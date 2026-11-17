package com.shopguide.service;

import com.shopguide.vo.TopProductVO;

import java.util.List;

/**
 * 销售数据统计服务
 */
public interface StatisticService {

    /**
     * 指定时间区间（近 N 天，含当天）内，已支付商品按销售数量排行前 limit 名
     *
     * @param days  统计天数：1 = 当天，7 = 近 7 天
     * @param limit 取前几条，默认 5
     */
    List<TopProductVO> topSales(Integer days, Integer limit);
}
