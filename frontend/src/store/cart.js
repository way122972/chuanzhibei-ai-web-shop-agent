import { computed, reactive } from 'vue'
import {
  cartListApi,
  cartAddApi,
  cartUpdateApi,
  cartDeleteApi,
  cartClearApi,
  cartCheckoutApi
} from '../api'

/**
 * 购物车状态管理（数据来自后端 cart 表，按登录用户隔离）
 * 后端通过 JWT 解析当前登录用户（ThreadLocal），所以前端不需要传 userId
 */
export const cartState = reactive({
  items: [],
  loaded: false
})

/**
 * 从后端加载当前登录用户的购物车
 */
export async function loadCart() {
  const res = await cartListApi()
  cartState.items = res.data || []
  cartState.loaded = true
}

/**
 * 加入购物车
 */
export async function addToCart(product, quantity = 1) {
  await cartAddApi({ productId: product.id, quantity })
  await loadCart()
}

/**
 * 修改数量
 */
export async function updateQuantity(id, quantity) {
  await cartUpdateApi({ id, quantity })
  await loadCart()
}

/**
 * 移除商品
 */
export async function removeFromCart(id) {
  await cartDeleteApi(id)
  await loadCart()
}

/**
 * 清空购物车
 */
export async function clearCart() {
  await cartClearApi()
  await loadCart()
}

/**
 * 结算：把前端购物车总价传给后端，后端校验余额后扣款
 * 返回后端结果（data 含 paidAmount / balance / itemCount）
 */
export async function checkout() {
  const totalPrice = Number(cartTotal.value)
  const res = await cartCheckoutApi({ totalPrice })
  await loadCart()
  return res
}

/** 购物车商品总件数（用于左侧导航角标） */
export const cartCount = computed(() => cartState.items.reduce((sum, i) => sum + i.quantity, 0))

/** 购物车合计金额 */
export const cartTotal = computed(() =>
  cartState.items.reduce((sum, i) => sum + i.quantity * Number(i.price), 0).toFixed(2)
)
