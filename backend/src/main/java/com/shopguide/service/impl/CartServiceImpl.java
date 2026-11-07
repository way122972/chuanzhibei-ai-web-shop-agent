package com.shopguide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shopguide.common.Result;
import com.shopguide.entity.Cart;
import com.shopguide.entity.Product;
import com.shopguide.entity.User;
import com.shopguide.mapper.CartMapper;
import com.shopguide.mapper.ProductMapper;
import com.shopguide.mapper.UserMapper;
import com.shopguide.service.CartService;
import com.shopguide.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 * 当前登录用户统一从 UserContext（ThreadLocal）获取，保证每个用户只能操作自己的购物车
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Cart> listByCurrentUser() {
        // 购物车 = 当前用户状态为「未支付」的记录
        return listByStatus(Cart.STATUS_UNPAID);
    }

    @Override
    public List<Cart> listByStatus(Integer status) {
        Integer userId = UserContext.getRequiredUserId();
        // 只允许 0/1 两种状态，非法值按「未支付」处理
        int st = (status != null && status == Cart.STATUS_PAID) ? Cart.STATUS_PAID : Cart.STATUS_UNPAID;
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId)
                .eq(Cart::getStatus, st)
                .orderByDesc(Cart::getCreateTime)
                .orderByDesc(Cart::getId);
        return cartMapper.selectList(qw);
    }

    @Override
    public boolean add(Integer productId, Integer quantity) {
        Integer userId = UserContext.getRequiredUserId();
        if (productId == null) {
            return false;
        }
        int qty = (quantity == null || quantity < 1) ? 1 : quantity;

        Product product = productMapper.selectById(productId);
        // 商品不存在或已下架不能加入购物车
        if (product == null || (product.getStatus() != null && product.getStatus() == 0)) {
            return false;
        }

        // 同一用户、同一商品、未支付 -> 数量累加
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
                .eq(Cart::getStatus, Cart.STATUS_UNPAID);
        Cart exist = cartMapper.selectOne(qw);
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + qty);
            exist.setUpdateTime(LocalDateTime.now());
            return cartMapper.updateById(exist) > 0;
        }

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(product.getId());
        cart.setProductName(product.getName());
        cart.setPrice(product.getPrice());
        cart.setImage(product.getImage());
        cart.setQuantity(qty);
        cart.setStatus(Cart.STATUS_UNPAID);   // 新加入购物车为未支付
        cart.setCreateTime(LocalDateTime.now());
        cart.setUpdateTime(LocalDateTime.now());
        return cartMapper.insert(cart) > 0;
    }

    @Override
    public boolean updateQuantity(Integer id, Integer quantity) {
        if (id == null || quantity == null || quantity < 1) {
            return false;
        }
        // 只允许修改属于当前登录用户的记录
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getId, id).eq(Cart::getUserId, UserContext.getRequiredUserId());
        Cart cart = cartMapper.selectOne(qw);
        if (cart == null) {
            return false;
        }
        cart.setQuantity(quantity);
        cart.setUpdateTime(LocalDateTime.now());
        return cartMapper.updateById(cart) > 0;
    }

    @Override
    public boolean remove(Integer id) {
        // 只允许删除「未支付」的记录：删除后购物车中同一商品同步消失；已支付订单作为历史保留
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getId, id)
                .eq(Cart::getUserId, UserContext.getRequiredUserId())
                .eq(Cart::getStatus, Cart.STATUS_UNPAID);
        return cartMapper.delete(qw) > 0;
    }

    @Override
    public void clear() {
        LambdaQueryWrapper<Cart> qw = new LambdaQueryWrapper<>();
        qw.eq(Cart::getUserId, UserContext.getRequiredUserId())
                .eq(Cart::getStatus, Cart.STATUS_UNPAID);
        cartMapper.delete(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> checkout(BigDecimal totalPrice) {
        Integer userId = UserContext.getRequiredUserId();

        // 1. 根据当前登录用户查询购物车中的未支付商品
        List<Cart> unpaidList = listByCurrentUser();
        if (unpaidList.isEmpty()) {
            return Result.error("购物车中没有待结算的商品");
        }

        // 2. 计算服务端总价，并与前端传来的购物车总价核对
        BigDecimal serverTotal = unpaidList.stream()
                .map(c -> c.getPrice().multiply(BigDecimal.valueOf(c.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPrice != null && totalPrice.compareTo(serverTotal) != 0) {
            return Result.error("购物车金额已变动，请刷新后重试");
        }

        // 3. 查询当前用户余额，判断是否足够
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        BigDecimal money = user.getMoney() == null ? BigDecimal.ZERO : user.getMoney();
        if (money.compareTo(serverTotal) < 0) {
            return Result.error("余额不足！当前余额 ¥" + money + "，需支付 ¥" + serverTotal);
        }

        // 4. 余额足够：扣减余额
        BigDecimal balance = money.subtract(serverTotal);
        LambdaUpdateWrapper<User> userUpdate = new LambdaUpdateWrapper<>();
        userUpdate.eq(User::getId, userId).set(User::getMoney, balance);
        userMapper.update(null, userUpdate);

        // 5. 结算成功：购物车记录状态由「未支付(0)」改为「已支付(1)」，数据保留可追溯
        List<Integer> ids = unpaidList.stream().map(Cart::getId).collect(Collectors.toList());
        LambdaUpdateWrapper<Cart> cartUpdate = new LambdaUpdateWrapper<>();
        cartUpdate.in(Cart::getId, ids)
                .set(Cart::getStatus, Cart.STATUS_PAID)
                .set(Cart::getUpdateTime, LocalDateTime.now());
        cartMapper.update(null, cartUpdate);

        // 6. 返回结算结果
        Map<String, Object> data = new HashMap<>();
        data.put("paidAmount", serverTotal);
        data.put("balance", balance);
        data.put("itemCount", unpaidList.stream().mapToInt(Cart::getQuantity).sum());
        return Result.success("购买成功", data);
    }
}
