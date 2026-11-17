package com.shopguide.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shopguide.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 商品 Mapper
 *
 * 注意：接口中不放任何手写 SQL（不使用 @Select/@Update 注解拼接）。
 * 所有自定义 SQL 统一写在 resources/mapper/ProductMapper.xml 中，通过 namespace + id 对应：
 *   - selectPageWithCategory -> ProductMapper.xml 中的同 id 的 &lt;select&gt;
 * 该查询为 product + category 的多表关联（LEFT JOIN），并带分类 / 名称 / 价格区间的动态条件；
 * 分页由 MyBatis-Plus 的 PaginationInnerInterceptor 自动完成 count + LIMIT。
 */
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 多表关联分页查询：可按分类、名称模糊、价格范围过滤
     * SQL 见 ProductMapper.xml#selectPageWithCategory
     */
    IPage<Product> selectPageWithCategory(IPage<Product> page,
                                          @Param("categoryId") Integer categoryId,
                                          @Param("name") String name,
                                          @Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice);
}
