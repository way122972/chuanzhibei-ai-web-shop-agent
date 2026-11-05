package com.shopguide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("product")
public class Product implements Serializable {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 商品名称 */
    private String name;
    /** 商品价格 */
    private BigDecimal price;
    /** 商品图片URL（阿里云OSS地址） */
    private String image;
    /** 所属分类ID（关联 category.id） */
    private Integer categoryId;
    /** 所属分类名称：多表关联查询时填充，数据库无此列 */
    @TableField(exist = false)
    private String categoryName;
    /** 商品状态：1-上架 0-下架 */
    private Integer status;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
