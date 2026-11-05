import { reactive } from 'vue'
import { categoryListApi } from '../api'

/**
 * 商品分类状态（全局共享）
 * - 导航栏的商品分类子菜单
 * - 商品新增/修改弹窗里的「商品分类」下拉框
 * 两处共用同一份数据，登录后只请求一次接口
 */
export const categoryState = reactive({
  list: [],
  loaded: false
})

/**
 * 加载分类列表（默认走缓存，force=true 强制刷新）
 */
export async function loadCategories(force = false) {
  if (categoryState.loaded && !force) {
    return categoryState.list
  }
  const res = await categoryListApi()
  categoryState.list = res.data || []
  categoryState.loaded = true
  return categoryState.list
}

/**
 * 根据分类ID取名称（找不到返回空串）
 */
export function categoryNameOf(id) {
  const hit = categoryState.list.find(c => c.id === Number(id))
  return hit ? hit.name : ''
}
