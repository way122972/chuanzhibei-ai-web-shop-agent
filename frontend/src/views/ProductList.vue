<template>
  <div class="product-page">
    <!-- 面包屑：热销商品 / 分类查询页 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item>首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="isCategoryMode">商品分类</el-breadcrumb-item>
        <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 搜索区：名称模糊查询 + 价格范围查询 -->
    <div class="toolbar">
      <div class="search-area">
        <el-input v-model="query.name" placeholder="商品名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        <span class="range-label">价格区间</span>
        <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价" style="width: 110px" />
        <span class="range-sep">—</span>
        <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价" style="width: 110px" />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 商品模块：一行4个，一页8个（两行） -->
    <el-row :gutter="20" class="product-grid" v-loading="loading">
      <el-col :span="6" v-for="item in list" :key="item.id">
        <div class="product-card">
          <div class="img-wrap">
            <img :src="item.image || defaultImg" alt="商品图片" />
            <el-tag class="off-tag hot-tag" effect="dark">热销中</el-tag>
            <!-- 所属分类：来自 product + category 多表查询结果 -->
            <el-tag v-if="item.categoryName" class="cate-tag" effect="dark" round>{{ item.categoryName }}</el-tag>
          </div>
          <div class="card-info">
            <div class="info-left">
              <div class="p-name" :title="item.name">{{ item.name }}</div>
              <div class="p-price">¥{{ item.price }}</div>
            </div>
            <!-- 名称/价格右侧的圆形加号：点击加入购物车 -->
            <button class="add-cart-btn" title="加入购物车"
                    @click.stop="handleAddToCart(item)">
              <el-icon><Plus /></el-icon>
            </button>
          </div>
        </div>
      </el-col>
      <el-empty v-if="!loading && list.length === 0"
                :description="isCategoryMode ? `「${pageTitle}」分类下暂无商品` : '暂无商品'" class="empty" />
    </el-row>

    <!-- 分页查询条 -->
    <div class="pagination-bar">
      <el-pagination background layout="total, prev, pager, next, jumper" :total="total"
                     :page-size="query.pageSize" :current-page="query.page"
                     @current-change="handlePageChange" />
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { productPageApi } from '../api'
import { addToCart } from '../store/cart'
import { loadCategories, categoryNameOf } from '../store/category'

const route = useRoute()

// 该页面同时承载「热销商品 /product」与「商品分类 /category/:id」两种入口
const isCategoryMode = computed(() => !!route.params.id)
const pageTitle = computed(() =>
  isCategoryMode.value ? (categoryNameOf(route.params.id) || '商品分类') : '热销商品'
)

const defaultImg = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect width="100%" height="100%" fill="#fff3ec"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="40" fill="#ffb696">🛒</text></svg>'
)

const loading = ref(false)
const list = ref([])
const total = ref(0)

// 分页查询条件：名称模糊 + 价格范围（categoryId 仅在分类页内部使用，不展示下拉框）
const query = reactive({
  page: 1,
  pageSize: 8,        // 一页8个，两行，每行4个
  categoryId: undefined,
  name: '',
  minPrice: undefined,
  maxPrice: undefined
})

const loadProducts = async () => {
  loading.value = true
  try {
    const res = await productPageApi(query)
    list.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

// 从路由同步分类条件：分类页固定查该分类，热销商品页不带分类
const syncCategoryFromRoute = () => {
  query.categoryId = route.params.id ? Number(route.params.id) : undefined
  query.page = 1
}

const handleSearch = () => {
  query.page = 1
  loadProducts()
}

const handleReset = () => {
  query.name = ''
  query.minPrice = undefined
  query.maxPrice = undefined
  query.categoryId = route.params.id ? Number(route.params.id) : undefined
  handleSearch()
}

const handlePageChange = page => {
  query.page = page
  loadProducts()
}

// 切换左侧导航的分类时，复用同一个组件实例，需要监听路由变化重新查询
watch(() => route.params.id, () => {
  syncCategoryFromRoute()
  loadProducts()
})

/* ---------- 加入购物车（写入后端 cart 表） ---------- */
const handleAddToCart = async item => {
  try {
    await addToCart(item)
    ElMessage.success(`「${item.name}」已加入购物车`)
  } catch (e) {
    /* 失败提示已由请求拦截器统一处理 */
  }
}

onMounted(async () => {
  // 面包屑分类名依赖分类列表，先加载再查询商品
  await loadCategories().catch(() => {})
  syncCategoryFromRoute()
  loadProducts()
  // AI 助手增删改商品后，自动刷新当前列表
  window.addEventListener('sg:data-changed', handleDataChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('sg:data-changed', handleDataChanged)
})

// AI 改动数据后刷新商品列表（分类可能新增/改动）
const handleDataChanged = () => {
  loadCategories().catch(() => {})
  loadProducts()
}
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 14px 18px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.search-area {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.range-label {
  font-size: 14px;
  color: #666;
  margin-left: 6px;
}

.range-sep {
  color: #999;
}

/* 商品模块：一行4个，间距20 */
.product-grid {
  min-height: 300px;
}

.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.25s;
  margin-bottom: 20px;
  border: 1px solid #f2f2f2;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(255, 106, 60, 0.18);
  border-color: #ffb696;
}

.img-wrap {
  position: relative;
  height: 170px;
  background: var(--sg-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
}

.img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.off-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

/* 卡片右上角：所属分类（多表查询得到的 categoryName） */
.cate-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.45);
  border: none;
  backdrop-filter: blur(2px);
}

.hot-tag {
  background: linear-gradient(90deg, #ff6a3c, #f5541f);
  border: none;
}

.card-info {
  padding: 10px 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.info-left {
  flex: 1;
  min-width: 0;
}

/* 圆形加号按钮：加入购物车 */
.add-cart-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #ffd9c6;
  background: var(--sg-primary-light);
  color: var(--sg-primary-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.add-cart-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff6a3c, #f5541f);
  border-color: #f5541f;
  color: #fff;
  transform: scale(1.12);
  box-shadow: 0 4px 10px rgba(245, 84, 31, 0.35);
}

.add-cart-btn:active:not(:disabled) {
  transform: scale(0.94);
}

.add-cart-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
  background: #f5f5f5;
  border-color: #e4e7ed;
  color: #bbb;
}

.p-name {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.p-price {
  margin-top: 6px;
  font-size: 18px;
  font-weight: bold;
  color: var(--sg-primary-deep);
}

.empty {
  width: 100%;
  padding-top: 40px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}
</style>
