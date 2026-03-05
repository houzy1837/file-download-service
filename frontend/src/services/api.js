import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

const api = axios.create({
  baseURL: API_BASE,
  timeout: 30000,
})

// 请求拦截器：添加 JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：处理 token 过期
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// 认证相关 API
export const authApi = {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  getCurrentUser: () => api.get('/auth/me'),
}

// 文件相关 API
export const fileApi = {
  list: (path = '') => api.get('/files/list', { params: { path } }),
  download: (file) => `${API_BASE}/files/download?file=${encodeURIComponent(file)}`,
}

export default api
