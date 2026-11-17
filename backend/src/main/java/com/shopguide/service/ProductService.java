package com.shopguide.service;

import com.shopguide.common.PageBean;
import com.shopguide.entity.Product;

import java.math.BigDecimal;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 分页查询：商品名称模糊查询 + 价格范围查询 + 所属分类过滤
     * 采用 product LEFT JOIN category 的多表关联查询，返回结果带分类名称
     *
     * @param categoryId 所属分类ID，为 null 表示不限分类（热销商品页）
     */
    PageBean<Product> pageQuery(int page, int pageSize, Integer categoryId, String name,
                                BigDecimal minPrice, BigDecimal maxPrice);
}
