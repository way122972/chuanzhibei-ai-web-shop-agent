package com.shopguide.mapper;

import com.shopguide.vo.TopProductVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售数据统计 Mapper
 * 继承 BaseMapper 不需要实体表，这里只承载统计用的自定义查询；
 * SQL 统一写在 resources/mapper/StatisticMapper.xml。
 */
public interface StatisticMapper {

    /**
     * 指定时间区间内「已支付」商品按销售数量排行
     *
     * @param startTime 统计起始时间（含）
     * @param limit     取前 N 名
     */
    List<TopProductVO> selectTopSales(@Param("startTime") LocalDateTime startTime,
                                      @Param("limit") Integer limit);
}
