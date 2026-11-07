package com.shopguide.controller;

import com.shopguide.common.Result;
import com.shopguide.service.StatisticService;
import com.shopguide.vo.TopProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 销售数据统计接口
 */
@RestController
@RequestMapping("/statistic")
public class StatisticController {

    @Autowired
    private StatisticService statisticService;

    /**
     * 销售数量排行（饼图 + 柱状图数据）：GET /statistic/top-sales?days=1|7&limit=5
     *
     * @param days  统计区间：1=当天，7=近7天（默认 1）
     * @param limit 取前 N 名（默认 5）
     */
    @GetMapping("/top-sales")
    public Result<List<TopProductVO>> topSales(@RequestParam(required = false) Integer days,
                                               @RequestParam(required = false) Integer limit) {
        return Result.success(statisticService.topSales(days, limit));
    }
}
