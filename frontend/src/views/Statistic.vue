<template>
  <div class="stat-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>首页</el-breadcrumb-item>
        <el-breadcrumb-item>销售数据统计</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="stat-panel" v-loading="loading">
      <!-- 顶部：时间区间选择（当天 / 近7天） -->
      <div class="panel-header">
        <div class="range-filter">
          <span class="filter-label">统计区间</span>
          <el-radio-group v-model="days" @change="loadData">
            <el-radio-button :value="1">当天</el-radio-button>
            <el-radio-button :value="7">近7天</el-radio-button>
          </el-radio-group>
          <span class="panel-sub">{{ rangeText }}已支付商品的销售数量，取前 5 名</span>
        </div>
        <span class="panel-total">销量合计：<b>{{ totalQty }}</b> 件　销售额：<b>¥{{ totalAmount }}</b></span>
      </div>

      <!-- 无数据 -->
      <el-empty v-if="!loading && rows.length === 0" description="该时间区间内暂无已支付订单数据" />

      <!-- 饼图 + 柱状图 -->
      <el-row v-else :gutter="20" class="chart-row">
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">销售数量占比（饼图）</div>
            <div ref="pieRef" class="chart"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <div class="chart-title">销售数量排行 TOP5（柱状图）</div>
            <div ref="barRef" class="chart"></div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { topSalesApi } from '../api'

// 统计区间：1 = 当天，7 = 近7天
const days = ref(1)
const rows = ref([])
const loading = ref(false)

const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

// 主题色（与平台橙色系一致）
const COLORS = ['#ff6a3c', '#f5a623', '#4a90e2', '#52c41a', '#9b59b6']

const rangeText = computed(() => (days.value === 1 ? '当天' : '近7天'))
const totalQty = computed(() => rows.value.reduce((s, r) => s + Number(r.totalQuantity || 0), 0))
const totalAmount = computed(() =>
  rows.value.reduce((s, r) => s + Number(r.totalAmount || 0), 0).toFixed(2)
)

/** 加载统计数据（days 由顶部单选决定） */
const loadData = async () => {
  loading.value = true
  try {
    const res = await topSalesApi({ days: days.value, limit: 5 })
    rows.value = res.data || []
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
  await nextTick()
  renderCharts()
}

/** 渲染饼图与柱状图 */
const renderCharts = () => {
  // 无数据时模板渲染的是 el-empty，图表容器不存在
  if (!pieRef.value || !barRef.value) {
    pieChart && pieChart.dispose()
    barChart && barChart.dispose()
    pieChart = barChart = null
    return
  }

  pieChart = pieChart || echarts.init(pieRef.value)
  barChart = barChart || echarts.init(barRef.value)

  const names = rows.value.map(r => r.productName)
  const quantities = rows.value.map(r => Number(r.totalQuantity))
  const amounts = rows.value.map(r => Number(r.totalAmount))

  // 饼图：各商品销售数量占比
  pieChart.setOption({
    color: COLORS,
    tooltip: {
      trigger: 'item',
      formatter: p => `${p.name}<br/>销售数量：${p.value} 件（${p.percent}%）`
    },
    legend: { bottom: 0, type: 'scroll', itemWidth: 12, itemHeight: 12, textStyle: { color: '#666' } },
    series: [
      {
        name: '销售数量',
        type: 'pie',
        radius: ['40%', '66%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
        label: { formatter: '{b}\n{c} 件', fontSize: 12, color: '#555' },
        labelLine: { length: 10, length2: 10 },
        data: rows.value.map(r => ({ name: r.productName, value: Number(r.totalQuantity) }))
      }
    ]
  }, true)

  // 柱状图：销售数量排行
  barChart.setOption({
    color: COLORS,
    grid: { top: 30, left: 16, right: 24, bottom: 56, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: params => {
        const i = params[0].dataIndex
        return `${names[i]}<br/>销售数量：${quantities[i]} 件<br/>销售额：¥${amounts[i].toFixed(2)}`
      }
    },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { interval: 0, width: 76, overflow: 'truncate', color: '#666' },
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#e5e5e5' } }
    },
    yAxis: {
      type: 'value',
      name: '件',
      nameTextStyle: { color: '#999' },
      splitLine: { lineStyle: { color: '#f3f3f3' } },
      axisLabel: { color: '#666' }
    },
    series: [
      {
        name: '销售数量',
        type: 'bar',
        barWidth: '46%',
        data: quantities.map((v, i) => ({
          value: v,
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: COLORS[i % COLORS.length] },
              { offset: 1, color: '#ffd9c6' }
            ])
          }
        })),
        label: { show: true, position: 'top', color: '#f5541f', fontWeight: 'bold' }
      }
    ]
  }, true)
}

// 窗口尺寸变化时自适应
const handleResize = () => {
  pieChart && pieChart.resize()
  barChart && barChart.resize()
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', handleResize)
  // AI 助手改动已支付订单后刷新统计
  window.addEventListener('sg:data-changed', loadData)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('sg:data-changed', loadData)
  pieChart && pieChart.dispose()
  barChart && barChart.dispose()
  pieChart = barChart = null
})
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 14px;
}

.stat-panel {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px 24px;
  min-height: 420px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f2f2;
}

.range-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #666;
}

.panel-sub {
  font-size: 13px;
  color: #999;
}

.panel-total {
  font-size: 13px;
  color: #666;
}

.panel-total b {
  font-size: 17px;
  color: var(--sg-primary-deep);
}

.chart-row {
  margin-top: 18px;
}

.chart-card {
  background: #fcfcfc;
  border: 1px solid #f2f2f2;
  border-radius: 10px;
  padding: 14px 16px 6px;
}

.chart-title {
  font-size: 14px;
  font-weight: bold;
  color: #333;
  margin-bottom: 6px;
}

.chart {
  width: 100%;
  height: 360px;
}
</style>
