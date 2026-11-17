package com.shopguide.service;

import com.shopguide.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 *
 * 分类下的商品查询走 ProductService#pageQuery(categoryId, ...)，
 * 由 ProductMapper 中的多表关联 SQL 完成 product + category 的联合查询。
 */
public interface CategoryService {

    /**
     * 查询全部分类（按 sort、id 排序），供导航栏与商品表单的下拉框使用
     */
    List<Category> listAll();

    /**
     * 按 id 查分类（导航栏面包屑展示分类名）
     */
    Category getById(Integer id);
}
