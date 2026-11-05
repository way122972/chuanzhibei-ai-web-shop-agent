package com.shopguide.controller;

import com.shopguide.common.Result;
import com.shopguide.entity.Category;
import com.shopguide.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品分类接口
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 全部分类（导航栏 + 商品表单下拉框）：GET /category/list
     */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    /**
     * 分类详情：GET /category/{id}
     */
    @GetMapping("/{id}")
    public Result<Category> detail(@PathVariable Integer id) {
        Category category = categoryService.getById(id);
        return category == null ? Result.error("分类不存在") : Result.success(category);
    }
}
