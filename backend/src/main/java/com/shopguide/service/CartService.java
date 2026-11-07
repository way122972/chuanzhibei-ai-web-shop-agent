package com.shopguide.service;

import com.shopguide.common.Result;
import com.shopguide.entity.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 购物车服务接口（所有操作均基于 ThreadLocal 中的当前登录用户）
 */
public interface CartService {

    /**
     * 查询当前用户的购物车（未支付商品）
     */
    List<Cart> listByCurrentUser();

    /**
     * 按状态查询当前用户的订单（cart 表同时承载订单数据）
     * status：0-未支付 1-已支付
     */
    List<Cart> listByStatus(Integer status);

    /**
     * 加入购物车（同一商品累加数量）
     */
    boolean add(Integer productId, Integer quantity);

    /**
     * 修改购物车商品数量
     */
    boolean updateQuantity(Integer id, Integer quantity);

    /**
     * 删除购物车中的某条商品
     */
    boolean remove(Integer id);

    /**
     * 清空当前用户的购物车（未支付部分）
     */
    void clear();

    /**
     * 结算：校验余额 -> 扣款 -> 购物车记录状态改为「已支付」（数据保留）
     */
    Result<Map<String, Object>> checkout(BigDecimal totalPrice);
}
