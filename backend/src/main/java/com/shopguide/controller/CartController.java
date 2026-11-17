package com.shopguide.controller;

import com.shopguide.common.Result;
import com.shopguide.entity.Cart;
import com.shopguide.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 购物车接口
 * 当前登录用户由 JwtInterceptor 解析 token 后写入 ThreadLocal（UserContext），
 * 因此这里的每个接口都只操作「当前登录用户」的购物车数据。
 */
@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 当前用户的订单/购物车列表：GET /cart/list?status=0|1
     * status 不传时默认 0（未支付，即购物车内容）；
     * 传 1 查询已支付订单（订单状态页面使用），数据均按当前登录用户隔离。
     */
    @GetMapping("/list")
    public Result<List<Cart>> list(@RequestParam(required = false) Integer status) {
        if (status == null) {
            return Result.success(cartService.listByCurrentUser());
        }
        return Result.success(cartService.listByStatus(status));
    }

    /**
     * 加入购物车：POST /cart/add  body: {productId, quantity}
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Integer> body) {
        Integer productId = body.get("productId");
        Integer quantity = body.get("quantity");
        boolean ok = cartService.add(productId, quantity);
        return ok ? Result.success("已加入购物车") : Result.error("加入购物车失败，商品可能已下架");
    }

    /**
     * 修改数量：PUT /cart/quantity  body: {id, quantity}
     */
    @PutMapping("/quantity")
    public Result<String> updateQuantity(@RequestBody Map<String, Integer> body) {
        boolean ok = cartService.updateQuantity(body.get("id"), body.get("quantity"));
        return ok ? Result.success("修改成功") : Result.error("修改失败");
    }

    /**
     * 删除购物车商品：DELETE /cart/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        boolean ok = cartService.remove(id);
        return ok ? Result.success("已移除") : Result.error("删除失败");
    }

    /**
     * 清空购物车：DELETE /cart/clear
     */
    @DeleteMapping("/clear")
    public Result<String> clear() {
        cartService.clear();
        return Result.success("购物车已清空");
    }

    /**
     * 结算：POST /cart/checkout  body: {totalPrice}
     * 余额足够则扣款并把购物车状态改为已支付，否则提示余额不足
     */
    @PostMapping("/checkout")
    public Result<Map<String, Object>> checkout(@RequestBody(required = false) Map<String, BigDecimal> body) {
        BigDecimal totalPrice = body == null ? null : body.get("totalPrice");
        return cartService.checkout(totalPrice);
    }
}
