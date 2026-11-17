import request from './request'

/**
 * 登录：POST /user/login
 */
export function loginApi(data) {
  return request.post('/user/login', data)
}

/**
 * 商品分页查询（名称模糊 + 价格范围）：GET /product/page
 */
export function productPageApi(params) {
  return request.get('/product/page', { params })
}

/**
 * 图片上传（阿里云OSS）：POST /product/upload
 * AI 助手「上传图片分析」依赖本接口
 */
export function uploadApi(formData) {
  return request.post('/product/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/* ==================== 商品分类 ==================== */

/**
 * 全部分类（导航栏 + 商品表单下拉框）：GET /category/list
 */
export function categoryListApi() {
  return request.get('/category/list')
}

/**
 * 分类详情：GET /category/{id}
 */
export function categoryDetailApi(id) {
  return request.get(`/category/${id}`)
}

/* ==================== 用户 ==================== */

/**
 * 当前登录用户信息（含余额）：GET /user/info
 */
export function userInfoApi() {
  return request.get('/user/info')
}

/* ==================== 购物车 ==================== */

/**
 * 当前用户的购物车/订单列表：GET /cart/list?status=0|1
 * 不传 status 返回未支付（购物车），传 1 返回已支付订单
 */
export function cartListApi(params) {
  return request.get('/cart/list', { params })
}

/**
 * 加入购物车：POST /cart/add
 */
export function cartAddApi(data) {
  return request.post('/cart/add', data)
}

/**
 * 修改购物车数量：PUT /cart/quantity
 */
export function cartUpdateApi(data) {
  return request.put('/cart/quantity', data)
}

/**
 * 删除购物车商品：DELETE /cart/delete/{id}
 */
export function cartDeleteApi(id) {
  return request.delete(`/cart/delete/${id}`)
}

/**
 * 清空购物车：DELETE /cart/clear
 */
export function cartClearApi() {
  return request.delete('/cart/clear')
}

/**
 * 结算（校验余额并扣款）：POST /cart/checkout
 */
export function cartCheckoutApi(data) {
  return request.post('/cart/checkout', data)
}
