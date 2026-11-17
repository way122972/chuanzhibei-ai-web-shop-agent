package com.shopguide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 购物车实体（每个用户的数据通过 user_id 隔离）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cart")
public class Cart implements Serializable {
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Integer id;
    /** 所属用户ID */
    private Integer userId;
    /** 商品ID */
    private Integer productId;
    /** 商品名称快照 */
    private String productName;
    /** 商品单价快照 */
    private BigDecimal price;
    /** 商品图片快照 */
    private String image;
    /** 购买数量 */
    private Integer quantity;
    /** 状态：0-未支付 1-已支付 */
    private Integer status;
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /** 商品状态常量：未支付 */
    public static final int STATUS_UNPAID = 0;
    /** 商品状态常量：已支付 */
    public static final int STATUS_PAID = 1;
}
