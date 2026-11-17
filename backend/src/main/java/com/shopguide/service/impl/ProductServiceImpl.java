package com.shopguide.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shopguide.common.PageBean;
import com.shopguide.entity.Product;
import com.shopguide.mapper.ProductMapper;
import com.shopguide.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品服务实现类
 * 分页查询基于 MyBatis-Plus 分页插件（MybatisPlusConfig 中注册的 PaginationInnerInterceptor）
 * 商品列表统一走 ProductMapper#selectPageWithCategory（product + category 多表关联）
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Override
    public PageBean<Product> pageQuery(int page, int pageSize, Integer categoryId, String name,
                                       BigDecimal minPrice, BigDecimal maxPrice) {
        // 多表关联查询：product LEFT JOIN category，SQL 写在 ProductMapper#selectPageWithCategory
        // 分页插件自动拼接 LIMIT 并执行 count
        Page<Product> pageParam = new Page<>(page, pageSize);
        IPage<Product> result = productMapper.selectPageWithCategory(pageParam, categoryId,
                StringUtils.hasText(name) ? name.trim() : null, minPrice, maxPrice);

        // 转换为通用分页封装类 PageBean 返回
        return PageBean.of(result);
    }
}
