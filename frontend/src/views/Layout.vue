<template>
  <el-container class="layout">
    <!-- 顶部栏：左侧项目名称，右侧当前用户 + 退出登录 -->
    <el-header class="header">
      <div class="header-left">
        <span class="logo">🛍️</span>
        <span class="title">智能电商导购平台</span>
      </div>
      <div class="header-right">
        <!-- 圆形 AI 助手按钮：点击在右侧展开对话面板 -->
        <button class="ai-entry" :class="{ active: aiVisible }" title="AI 助手" @click="aiVisible = !aiVisible">
          <el-icon><MagicStick /></el-icon>
          <span class="ai-entry-text">AI</span>
        </button>
        <el-dropdown>
          <span class="user-box">
            <el-icon><UserFilled /></el-icon>
            {{ username }}
            <el-icon class="arrow"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="danger" link class="logout-btn" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>退出登录
        </el-button>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- 左侧导航栏 -->
      <el-aside width="200px" class="aside">
        <div class="aside-title">电商管理</div>
        <el-menu :default-active="activeMenu" router background-color="#ffffff" text-color="#555"
                 active-text-color="#f5541f">
          <el-menu-item index="/product">
            <el-icon><Goods /></el-icon>
            <span>热销商品</span>
          </el-menu-item>
          <!-- 商品分类导航：分类数据来自后端 category 表（生活用品 / 电子产品……） -->
          <el-sub-menu index="category">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>商品分类</span>
            </template>
            <el-menu-item v-for="c in categoryState.list" :key="c.id" :index="`/category/${c.id}`">
              <el-icon><PriceTag /></el-icon>
              <span>{{ c.name }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 购物车导航入口：点击进入当前用户的购物车 -->
          <el-menu-item index="/cart">
            <el-icon><ShoppingCart /></el-icon>
            <span>购物车</span>
            <span v-if="cartCount > 0" class="menu-cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
          </el-menu-item>
          <!-- 订单状态：按未支付 / 已支付查询当前登录用户的订单 -->
          <el-menu-item index="/order">
            <el-icon><Tickets /></el-icon>
            <span>订单状态</span>
          </el-menu-item>
          <!-- 销售数据统计：当天 / 近7天 的销售数量 TOP5 饼图 + 柱状图 -->
          <el-menu-item index="/statistic">
            <el-icon><DataAnalysis /></el-icon>
            <span>销售数据统计</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 右侧 AI 助手对话面板 -->
    <AiAssistant v-model:visible="aiVisible" />
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartCount, cartState, loadCart } from '../store/cart'
import { categoryState, loadCategories } from '../store/category'
import AiAssistant from '../components/AiAssistant.vue'

const route = useRoute()
const router = useRouter()

const username = computed(() => localStorage.getItem('username') || '未知用户')
const activeMenu = computed(() => route.path)

// AI 助手面板开关
const aiVisible = ref(false)

// AI 修改过数据后，刷新顶部购物车角标
const handleDataChanged = () => {
  loadCart().catch(() => {})
}

// 登录后从后端加载当前用户的购物车（后端按 JWT 中的用户ID隔离数据）
onMounted(() => {
  loadCart().catch(() => {})
  // 商品分类导航：一次加载，供侧边栏子菜单与商品表单共用
  loadCategories().catch(() => {})
  window.addEventListener('sg:data-changed', handleDataChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('sg:data-changed', handleDataChanged)
})

// 退出登录：清除令牌，返回登录页面
const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      // JWT 无状态鉴权：退出登录只需清除本地令牌，后续请求不再携带即视为未登录
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      cartState.items = []
      ElMessage.success('已退出登录')
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.header {
  height: var(--sg-header-height);
  background: linear-gradient(90deg, #ff6a3c 0%, #f5541f 100%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 84, 31, 0.35);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  font-size: 28px;
  margin-right: 10px;
}

.title {
  font-size: 22px;
  font-weight: bold;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 顶部圆形 AI 助手按钮 */
.ai-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 34px;
  padding: 0 12px 0 10px;
  margin-right: 16px;
  border-radius: 17px;
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  backdrop-filter: blur(2px);
}

.ai-entry:hover {
  background: #fff;
  color: #f5541f;
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.15);
}

.ai-entry.active {
  background: #fff;
  color: #f5541f;
}

.ai-entry .el-icon {
  font-size: 16px;
}

.ai-entry-text {
  letter-spacing: 1px;
}

.user-box {
  display: flex;
  align-items: center;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  margin-right: 20px;
}

.user-box .arrow {
  margin-left: 4px;
}

.logout-btn {
  color: #fff;
  font-size: 14px;
}

.aside {
  background: #fff;
  border-right: 1px solid #f0f0f0;
}

.aside-title {
  padding: 18px 20px 10px;
  font-size: 13px;
  color: #aaa;
  letter-spacing: 2px;
}

.aside :deep(.el-menu) {
  border-right: none;
}

.aside :deep(.el-menu-item.is-active) {
  background: var(--sg-primary-light);
}

.main {
  background: var(--sg-bg);
  padding: 20px;
  overflow-y: auto;
}

/* 左侧导航：购物车数量角标 */
.menu-cart-badge {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ff6a3c, #f5541f);
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
}

</style>
