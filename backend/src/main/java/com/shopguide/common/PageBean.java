package com.shopguide.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页结果封装类
 * 任何需要分页返回的场景都可以直接调用：
 *   1. PageBean.of(mybatisPlusPage)  —— 由 MyBatis-Plus 分页插件查询结果直接转换
 *   2. new PageBean<>(total, page, pageSize, records) —— 手动组装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean<T> implements Serializable {

    /** 总记录数 */
    private Long total;
    /** 当前页码 */
    private Integer page;
    /** 每页条数 */
    private Integer pageSize;
    /** 当前页数据列表 */
    private List<T> records;

    /**
     * 由 MyBatis-Plus 的分页对象（Page / 自定义关联查询的 IPage）转换为 PageBean
     */
    public static <T> PageBean<T> of(IPage<T> p) {
        return new PageBean<>(p.getTotal(), (int) p.getCurrent(), (int) p.getSize(), p.getRecords());
    }
}
