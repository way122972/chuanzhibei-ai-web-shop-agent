<template>
  <div class="order-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>首页</el-breadcrumb-item>
        <el-breadcrumb-item>订单状态</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="order-panel" v-loading="loading">
      <!-- 顶部左侧：订单状态选择（未支付 / 已支付） -->
      <div class="panel-header">
        <div class="status-filter">
          <span class="filter-label">订单状态</span>
          <el-select v-model="status" style="width: 140px" @change="handleStatusChange">
            <el-option label="未支付" :value="0" />
            <el-option label="已支付" :value="1" />
          </el-select>
          <span class="panel-sub">当前用户：{{ username }}　共 {{ orders.length }} 条订单</span>
        </div>
        <span class="panel-total">
          合计金额：<b>¥{{ totalAmount }}</b>
        </span>
      </div>

      <!-- 无订单 -->
      <el-empty v-if="!loading && orders.length === 0"
                :description="status === 0 ? '暂无未支付订单，去购物车逛逛吧' : '暂无已支付订单'">
        <el-button type="primary" @click="$router.push(status === 0 ? '/cart' : '/product')">
          {{ status === 0 ? '去购物车' : '去逛逛' }}
        </el-button>
      </el-empty>

      <!-- 订单列表（数据来自后端 cart 表，按当前登录用户 + 状态查询，每条订单一一行） -->
      <el-table v-else :data="orders" class="order-table">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="goods-cell">
              <img v-if="row.image" :src="row.image" class="goods-img" alt="" />
              <div v-else class="goods-img placeholder">🛒</div>
              <span class="goods-name" :title="row.productName">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="数量" width="90" align="center">
          <template #default="{ row }">x{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{ row }">
            <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" effect="light">
              {{ row.status === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="180">
          <template #default="{ row }">
            <span class="order-time">{{ row.createTime }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <!-- 未支付：可删除（同时从购物车移除）；已支付：再来一单（重新加入购物车） -->
            <el-button v-if="row.status === 0" link type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-else link type="primary" @click="handleBuyAgain(row)">再来一单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartListApi, cartDeleteApi, cartAddApi } from '../api'
import { loadCart } from '../store/cart'

const username = computed(() => localStorage.getItem('username') || '未知用户')
const loading = ref(false)
const orders = ref([])
// 当前查询的状态：0-未支付 1-已支付
const status = ref(0)

// 列表合计金额
const totalAmount = computed(() =>
  orders.value.reduce((sum, o) => sum + Number(o.price) * o.quantity, 0).toFixed(2)
)

// 按状态查询当前登录用户的订单
const loadOrders = async () => {
  loading.value = true
  try {
    const res = await cartListApi({ status: status.value })
    orders.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  loadOrders()
}

// 未支付订单删除：该记录即购物车记录，删除后购物车中同一商品同步消失
const handleDelete = row => {
  ElMessageBox.confirm(
    `确定删除未支付订单「${row.productName}」吗？删除后购物车中的该商品也会被一并移除。`,
    '提示',
    { type: 'warning' }
  )
    .then(async () => {
      await cartDeleteApi(row.id)
      ElMessage.success('订单已删除，购物车中的该商品已同步移除')
      await Promise.all([loadOrders(), loadCart()])
    })
    .catch(() => {})
}

// 已支付订单「再来一单」：按原商品与数量重新加入购物车
const handleBuyAgain = async row => {
  try {
    await cartAddApi({ productId: row.productId, quantity: row.quantity })
    ElMessage.success(`「${row.productName}」已重新加入购物车`)
    await loadCart()
  } catch (e) {
    /* 商品已下架/删除的提示由请求拦截器统一处理 */
  }
}

onMounted(() => {
  loadOrders()
  // AI 助手改动购物车/订单后自动刷新
  window.addEventListener('sg:data-changed', loadOrders)
})

onBeforeUnmount(() => {
  window.removeEventListener('sg:data-changed', loadOrders)
})
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 14px;
}

.order-panel {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px 24px;
  min-height: 400px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f2f2;
  margin-bottom: 6px;
}

/* 顶部左侧：订单状态选择 */
.status-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-label {
  font-size: 14px;
  color: #666;
}

.panel-sub {
  font-size: 13px;
  color: #999;
  margin-left: 6px;
}

.panel-total {
  font-size: 13px;
  color: #666;
}

.panel-total b {
  font-size: 18px;
  color: var(--sg-primary-deep);
}

.order-table {
  width: 100%;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-img {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--sg-primary-light);
  flex-shrink: 0;
}

.goods-img.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.goods-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtotal {
  color: var(--sg-primary-deep);
  font-weight: bold;
}

.order-time {
  font-size: 13px;
  color: #888;
}
</style>
