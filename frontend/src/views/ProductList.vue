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

    <!-- 搜索区：名称模糊查询 + 价格范围查询（热销商品页才有新增商品入口，分类页只做展示） -->
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
      <!-- 分类页（生活用品 / 电子产品）不提供新增商品功能 -->
      <el-button v-if="!isCategoryMode" type="primary" :icon="Plus" @click="openAddDialog">新增商品</el-button>
    </div>

    <!-- 商品模块：一行4个，一页8个（两行） -->
    <el-row :gutter="20" class="product-grid" v-loading="loading">
      <el-col :span="6" v-for="item in list" :key="item.id">
        <div class="product-card" :class="{ 'is-off': item.status === 0 }" @click="openAction(item)">
          <div class="img-wrap">
            <img :src="item.image || defaultImg" alt="商品图片" />
            <el-tag v-if="item.status === 0" class="off-tag" type="info" effect="dark">已下架</el-tag>
            <el-tag v-else class="off-tag hot-tag" effect="dark">热销中</el-tag>
            <!-- 所属分类：来自 product + category 多表查询结果 -->
            <el-tag v-if="item.categoryName" class="cate-tag" effect="dark" round>{{ item.categoryName }}</el-tag>
          </div>
          <div class="card-info">
            <div class="info-left">
              <div class="p-name" :title="item.name">{{ item.name }}</div>
              <div class="p-price">¥{{ item.price }}</div>
            </div>
            <!-- 名称/价格右侧的圆形加号：点击加入购物车 -->
            <button class="add-cart-btn" :disabled="item.status === 0"
                    :title="item.status === 0 ? '商品已下架，无法加入购物车' : '加入购物车'"
                    @click.stop="handleAddToCart(item)">
              <el-icon><Plus /></el-icon>
            </button>
          </div>
        </div>
      </el-col>
      <el-empty v-if="!loading && list.length === 0"
                :description="isCategoryMode ? `「${pageTitle}」分类下暂无商品` : '暂无商品，点击右上角“新增商品”添加'" class="empty" />
    </el-row>

    <!-- 分页查询条 -->
    <div class="pagination-bar">
      <el-pagination background layout="total, prev, pager, next, jumper" :total="total"
                     :page-size="query.pageSize" :current-page="query.page"
                     @current-change="handlePageChange" />
    </div>

    <!-- 新增 / 修改商品弹窗：商品名称、价格输入框 + 图片上传框 -->
    <el-dialog v-model="formDialogVisible" :title="dialogMode === 'add' ? '新增商品' : '修改商品'"
               width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择商品分类" clearable style="width: 200px">
            <el-option v-for="c in categoryState.list" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-upload class="uploader" drag action="#" :show-file-list="false" :auto-upload="false"
                     :on-change="handleFileChange" accept="image/*">
            <img v-if="form.image" :src="form.image" class="preview" />
            <div v-else class="uploader-empty">
              <el-icon class="uploader-icon"><Plus /></el-icon>
              <div class="uploader-text">将图片拖到此处，或<em>点击上传</em></div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ dialogMode === 'add' ? '确定' : '确认修改' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 点击商品卡片：下架/上架、修改、删除 选项 -->
    <el-dialog v-model="actionDialogVisible" :title="`商品操作 - ${currentProduct?.name || ''}`" width="420px">
      <div class="action-body" v-if="currentProduct">
        <img :src="currentProduct.image || defaultImg" class="action-img" />
        <div class="action-meta">
          <div class="p-name">{{ currentProduct.name }}</div>
          <div class="p-price">¥{{ currentProduct.price }}</div>
          <el-tag :type="currentProduct.status === 1 ? 'success' : 'info'">
            {{ currentProduct.status === 1 ? '上架中' : '已下架' }}
          </el-tag>
        </div>
      </div>
      <template #footer>
        <el-button :type="currentProduct?.status === 1 ? 'warning' : 'success'" @click="handleChangeStatus">
          {{ currentProduct?.status === 1 ? '下架商品' : '上架商品' }}
        </el-button>
        <el-button type="primary" plain @click="openEditDialog">修改商品</el-button>
        <el-button type="danger" @click="handleDelete">删除商品</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import { productPageApi, productAddApi, productUpdateApi, uploadApi, changeStatusApi, deleteProductApi } from '../api'
import { addToCart } from '../store/cart'
import { categoryState, loadCategories, categoryNameOf } from '../store/category'

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

/* ---------- 新增 / 修改商品弹窗 ---------- */
const formDialogVisible = ref(false)
const dialogMode = ref('add')          // add = 新增，edit = 修改
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ id: null, name: '', categoryId: null, price: undefined, image: '' })

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }]
}

// 打开新增弹窗：仅热销商品页可用（分类页只做展示，已隐藏入口，这里再兜底一次）
const openAddDialog = () => {
  if (isCategoryMode.value) return
  dialogMode.value = 'add'
  form.id = null
  form.name = ''
  form.categoryId = null
  form.price = undefined
  form.image = ''
  formDialogVisible.value = true
}

// 打开修改弹窗：回显当前商品的原信息（含所属分类）
const openEditDialog = () => {
  const p = currentProduct.value
  if (!p) return
  dialogMode.value = 'edit'
  form.id = p.id
  form.name = p.name
  form.categoryId = p.categoryId ? Number(p.categoryId) : null
  form.price = Number(p.price)
  form.image = p.image || ''
  actionDialogVisible.value = false    // 先关掉操作框，再弹出修改框
  formDialogVisible.value = true
}

// 选择图片后先上传（OSS），拿回URL再提交表单
const handleFileChange = async file => {
  if (!file.raw) return
  const fd = new FormData()
  fd.append('file', file.raw)
  try {
    const res = await uploadApi(fd)
    form.image = res.data
    ElMessage.success('图片上传成功')
  } catch (e) { /* 拦截器已提示 */ }
}

// 提交：新增走 add 接口，修改走 update 接口
const handleSubmit = () => {
  formRef.value.validate(async valid => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (dialogMode.value === 'add') {
        await productAddApi({
          name: form.name, categoryId: form.categoryId, price: form.price, image: form.image, status: 1
        })
        ElMessage.success('新增商品成功')
      } else {
        await productUpdateApi({
          id: form.id, name: form.name, categoryId: form.categoryId, price: form.price, image: form.image
        })
        ElMessage.success('修改商品成功')
      }
      formDialogVisible.value = false
      handleSearch()
    } finally {
      submitLoading.value = false
    }
  })
}

/* ---------- 加入购物车（写入后端 cart 表） ---------- */
const handleAddToCart = async item => {
  if (item.status === 0) {
    ElMessage.warning('该商品已下架，无法加入购物车')
    return
  }
  try {
    await addToCart(item)
    ElMessage.success(`「${item.name}」已加入购物车`)
  } catch (e) {
    /* 失败提示已由请求拦截器统一处理 */
  }
}

/* ---------- 卡片操作：下架 / 删除 ---------- */
const actionDialogVisible = ref(false)
const currentProduct = ref(null)

const openAction = item => {
  currentProduct.value = item
  actionDialogVisible.value = true
}

// 下架/上架：改变商品状态
const handleChangeStatus = () => {
  const p = currentProduct.value
  const action = p.status === 1 ? '下架' : '上架'
  ElMessageBox.confirm(`确定要${action}商品「${p.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await changeStatusApi(p.id)
      ElMessage.success(`${action}成功`)
      actionDialogVisible.value = false
      loadProducts()
    })
    .catch(() => {})
}

// 删除：从表中删除
const handleDelete = () => {
  const p = currentProduct.value
  ElMessageBox.confirm(`确定要删除商品「${p.name}」吗？删除后不可恢复！`, '危险操作', { type: 'error' })
    .then(async () => {
      await deleteProductApi(p.id)
      ElMessage.success('删除成功')
      actionDialogVisible.value = false
      // 若当前页删除后为空则回退一页
      if (list.value.length === 1 && query.page > 1) query.page--
      loadProducts()
    })
    .catch(() => {})
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
  cursor: pointer;
  transition: all 0.25s;
  margin-bottom: 20px;
  border: 1px solid #f2f2f2;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 24px rgba(255, 106, 60, 0.18);
  border-color: #ffb696;
}

.product-card.is-off {
  opacity: 0.6;
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

/* 上传框 */
.uploader {
  width: 100%;
}

.uploader :deep(.el-upload-dragger) {
  width: 100%;
  height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.preview {
  width: 100%;
  height: 130px;
  object-fit: cover;
  border-radius: 6px;
}

.uploader-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.uploader-icon {
  font-size: 32px;
  color: #ff6a3c;
}

.uploader-text {
  font-size: 13px;
  color: #888;
}

/* 操作弹窗 */
.action-body {
  display: flex;
  gap: 16px;
  align-items: center;
}

.action-img {
  width: 110px;
  height: 110px;
  object-fit: cover;
  border-radius: 8px;
  background: var(--sg-primary-light);
}

.action-meta .p-price {
  margin: 6px 0 8px;
}
</style>
