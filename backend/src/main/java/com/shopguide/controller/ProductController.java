package com.shopguide.controller;

import com.shopguide.common.PageBean;
import com.shopguide.common.Result;
import com.shopguide.entity.Product;
import com.shopguide.service.ProductService;
import com.shopguide.utils.AliyunOSSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * 商品接口：分页查询 / 图片上传
 * （新增、修改、上下架、删除等写操作已随前端功能删减移除；商品维护通过 AI 助手完成）
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private AliyunOSSUtil aliyunOSSUtil;

    /**
     * 分页查询：GET /product/page?page=1&pageSize=8&categoryId=2&name=xx&minPrice=1&maxPrice=100
     * 通过 product LEFT JOIN category 多表查询，返回结果带 categoryName（所属分类名称）
     */
    @GetMapping("/page")
    public Result<PageBean<Product>> page(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "8") int pageSize,
                                          @RequestParam(required = false) Integer categoryId,
                                          @RequestParam(required = false) String name,
                                          @RequestParam(required = false) BigDecimal minPrice,
                                          @RequestParam(required = false) BigDecimal maxPrice) {
        // 分页由 Service 层通过 MyBatis-Plus 分页插件完成，统一封装为 PageBean 返回
        PageBean<Product> pageBean = productService.pageQuery(page, pageSize, categoryId, name, minPrice, maxPrice);
        return Result.success(pageBean);
    }

    /**
     * 图片上传：POST /product/upload  (form-data: file)
     * 图片统一上传到阿里云 OSS，返回图片URL；OSS 未配置或上传失败则返回错误提示
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        try {
            String url = aliyunOSSUtil.upload(file);
            return Result.success("上传成功", url);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 配置缺失 / 文件为空：直接把原因返回给前端
            return Result.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }
}
