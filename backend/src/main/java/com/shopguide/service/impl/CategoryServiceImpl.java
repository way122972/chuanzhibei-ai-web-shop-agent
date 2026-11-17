package com.shopguide.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shopguide.entity.Category;
import com.shopguide.mapper.CategoryMapper;
import com.shopguide.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品分类服务实现
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> listAll() {
        LambdaQueryWrapper<Category> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(Category::getSort).orderByAsc(Category::getId);
        return categoryMapper.selectList(qw);
    }

    @Override
    public Category getById(Integer id) {
        if (id == null) {
            return null;
        }
        return categoryMapper.selectById(id);
    }
}
