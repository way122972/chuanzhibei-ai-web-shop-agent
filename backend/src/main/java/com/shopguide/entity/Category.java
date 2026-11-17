package com.shopguide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品分类实体（生活用品、电子产品……）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("category")
public class Category implements Serializable {
    /** 分类主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 分类名称 */
    private String name;
    /** 排序号（越小越靠前） */
    private Integer sort;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
