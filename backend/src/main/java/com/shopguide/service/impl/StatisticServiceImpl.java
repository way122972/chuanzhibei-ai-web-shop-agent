package com.shopguide.service.impl;

import com.shopguide.mapper.StatisticMapper;
import com.shopguide.service.StatisticService;
import com.shopguide.vo.TopProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售数据统计实现
 * 时间区间口径：按「支付时间」（cart.update_time）统计，
 * 近 7 天 = 含今天在内的 7 个自然日（从 6 天前的 00:00 开始）。
 */
@Service
public class StatisticServiceImpl implements StatisticService {

    /** 默认取前 5 名 */
    private static final int DEFAULT_LIMIT = 5;

    @Autowired
    private StatisticMapper statisticMapper;

    @Override
    public List<TopProductVO> topSales(Integer days, Integer limit) {
        int day = (days == null || days < 1) ? 1 : Math.min(days, 365);
        int top = (limit == null || limit < 1) ? DEFAULT_LIMIT : limit;

        LocalDateTime startTime = LocalDate.now().minusDays(day - 1L).atStartOfDay();
        return statisticMapper.selectTopSales(startTime, top);
    }
}
