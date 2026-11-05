<template>
  <div class="cart-page">
    <!-- 面包屑 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>首页</el-breadcrumb-item>
        <el-breadcrumb-item>购物车</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="cart-panel" v-loading="loading">
      <div class="panel-header">
        <span class="panel-title">我的购物车</span>
        <span class="panel-sub">当前用户：{{ username }}　共 {{ cartCount }} 件商品</span>
        <span class="balance">
          账户余额：<b :class="{ 'low': isLowBalance }">¥{{ money ?? '--' }}</b>
        </span>
      </div>

      <!-- 空购物车 -->
      <el-empty v-if="cartState.items.length === 0" description="购物车还是空的，快去挑选商品吧">
        <el-button type="primary" @click="$router.push('/product')">去逛逛</el-button>
      </el-empty>

      <!-- 购物车列表（数据来自后端 cart 表，仅当前登录用户） -->
      <template v-else>
        <el-table :data="cartState.items" class="cart-table">
          <el-table-column label="商品" min-width="320">
            <template #default="{ row }">
              <div class="goods-cell">
                <img v-if="row.image" :src="row.image" class="goods-img" alt="" />
                <div v-else class="goods-img placeholder">🛒</div>
                <span class="goods-name" :title="row.productName">{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="130">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number :model-value="row.quantity" :min="1" :max="99" size="small"
                               @change="val => handleQuantityChange(row, val)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="130">
            <template #default="{ row }">
              <span class="subtotal">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleRemove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 底部结算栏 -->
        <div class="settle-bar">
          <el-button link type="danger" @click="handleClear">清空购物车</el-button>
          <div class="settle-right">
            <span class="total-label">合计（{{ cartCount }} 件）：</span>
            <span class="total-price">¥{{ cartTotal }}</span>
            <el-button type="primary" size="large" :loading="submitting" @click="handleCheckout">提交购物车（结算）</el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartState, cartCount, cartTotal, loadCart, updateQuantity, removeFromCart, clearCart, checkout } from '../store/cart'
import { userInfoApi } from '../api'

const username = computed(() => localStorage.getItem('username') || '未知用户')
const loading = ref(false)
const submitting = ref(false)
const money = ref(null)

const isLowBalance = computed(() => money.value !== null && Number(money.value) < Number(cartTotal.value))

// 加载当前用户余额
const loadMoney = async () => {
  try {
    const res = await userInfoApi()
    money.value = res.data.money
  } catch (e) {
    /* 拦截器已提示 */
  }
}

const init = async () => {
  loading.value = true
  try {
    await Promise.all([loadCart(), loadMoney()])
  } finally {
    loading.value = false
  }
}

// 数量变化：直接提交后端
const handleQuantityChange = async (row, val) => {
  if (!val) return
  try {
    await updateQuantity(row.id, val)
  } catch (e) {
    await loadCart()
  }
}

const handleRemove = row => {
  ElMessageBox.confirm(`确定从购物车移除「${row.productName}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await removeFromCart(row.id)
      ElMessage.success('已移除')
    })
    .catch(() => {})
}

const handleClear = () => {
  ElMessageBox.confirm('确定要清空购物车吗？', '提示', { type: 'warning' })
    .then(async () => {
      await clearCart()
      ElMessage.success('购物车已清空')
    })
    .catch(() => {})
}

// 提交购物车（结算）：后端校验余额，足够则扣款并改为已支付，否则提示余额不足
const handleCheckout = () => {
  ElMessageBox.confirm(
    `共 ${cartCount.value} 件商品，合计 ¥${cartTotal.value}，确认提交并支付？`,
    '确认结算',
    { confirmButtonText: '确认支付', cancelButtonText: '再想想', type: 'info' }
  )
    .then(async () => {
      submitting.value = true
      try {
        const res = await checkout()
        money.value = res.data.balance
        ElMessage.success(`购买成功！支付 ¥${res.data.paidAmount}，余额 ¥${res.data.balance}`)
      } catch (e) {
        // 余额不足等业务错误由请求拦截器统一弹出提示，这里刷新余额与购物车
        await loadMoney()
        await loadCart()
      } finally {
        submitting.value = false
      }
    })
    .catch(() => {})
}

onMounted(() => {
  init()
  // AI 助手改动购物车（加入/清空）后自动刷新
  window.addEventListener('sg:data-changed', init)
})

onBeforeUnmount(() => {
  window.removeEventListener('sg:data-changed', init)
})
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 14px;
}

.cart-panel {
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px 24px;
  min-height: 400px;
}

.panel-header {
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f2f2f2;
  margin-bottom: 6px;
}

.panel-title {
  font-size: 18px;
  font-weight: bold;
}

.panel-sub {
  font-size: 13px;
  color: #999;
}

.balance {
  margin-left: auto;
  font-size: 13px;
  color: #666;
}

.balance b {
  font-size: 17px;
  color: #67c23a;
}

.balance b.low {
  color: #f56c6c;
}

.cart-table {
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

.settle-bar {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed #eee;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.settle-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-label {
  font-size: 14px;
  color: #666;
}

.total-price {
  font-size: 24px;
  font-weight: bold;
  color: var(--sg-primary-deep);
  margin-right: 10px;
}
</style>
