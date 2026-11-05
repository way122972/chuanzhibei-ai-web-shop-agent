import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/product',
    children: [
      {
        path: 'product',
        name: 'Product',
        component: () => import('../views/ProductList.vue'),
        meta: { title: '热销商品' }
      },
      {
        path: 'category/:id',
        name: 'CategoryProducts',
        component: () => import('../views/ProductList.vue'),
        meta: { title: '商品分类' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/Cart.vue'),
        meta: { title: '购物车' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('../views/OrderList.vue'),
        meta: { title: '订单状态' }
      },
      {
        path: 'statistic',
        name: 'Statistic',
        component: () => import('../views/Statistic.vue'),
        meta: { title: '销售数据统计' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录强制跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
