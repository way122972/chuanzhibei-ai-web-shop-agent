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
 * 商品接口：分页查询 / 新增 / 修改 / 图片上传 / 上下架 / 删除
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
     * 新增商品：POST /product/add  body: {name, price, image, status}
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return Result.error("商品名称不能为空");
        }
        if (product.getPrice() == null) {
            return Result.error("商品价格不能为空");
        }
        boolean ok = productService.add(product);
        return ok ? Result.success("新增商品成功") : Result.error("新增商品失败");
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

    /**
     * 修改商品：PUT /product/update  body: {id, name, price, image}
     * 只更新名称、价格、图片，商品状态（上下架）保持原样
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Product product) {
        if (product.getId() == null) {
            return Result.error("商品ID不能为空");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return Result.error("商品名称不能为空");
        }
        if (product.getPrice() == null) {
            return Result.error("商品价格不能为空");
        }
        boolean ok = productService.update(product);
        return ok ? Result.success("修改商品成功") : Result.error("商品不存在或修改失败");
    }

    /**
     * 下架/上架（状态取反）：PUT /product/status/{id}
     */
    @PutMapping("/status/{id}")
    public Result<String> changeStatus(@PathVariable Integer id) {
        boolean ok = productService.changeStatus(id);
        return ok ? Result.success("状态修改成功") : Result.error("商品不存在");
    }

    /**
     * 删除商品：DELETE /product/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        boolean ok = productService.delete(id);
        return ok ? Result.success("删除成功") : Result.error("商品不存在");
    }
}
