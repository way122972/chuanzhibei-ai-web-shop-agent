package com.shopguide.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 销售排行 VO（统计用）
 * 由 cart 表中「已支付」记录按商品聚合得到：
 *   totalQuantity = 销售数量合计，totalAmount = 销售额合计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductVO implements Serializable {

    /** 商品ID */
    private Integer productId;

    /** 商品名称（下单时的快照名称） */
    private String productName;

    /** 销售数量合计 */
    private Long totalQuantity;

    /** 销售额合计 */
    private BigDecimal totalAmount;
}
