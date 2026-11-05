import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  // 默认连接 8081 后端；如需临时切换可在启动时设置 VITE_API_BASE
  baseURL: import.meta.env.VITE_API_BASE || 'http://localhost:8081',
  timeout: 10000
})

// 请求拦截器：自动携带 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

// 响应拦截器：统一处理业务结果
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 1) {
      ElMessage.error(res.msg || '操作失败')
      return Promise.reject(res)
    }
    return res
  },
  error => {
    // 401 未登录 / token 失效 -> 回到登录页
    if (error.response && error.response.status === 401) {
      ElMessage.error('登录已失效，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      router.push('/login')
    } else {
      ElMessage.error('网络请求异常，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
