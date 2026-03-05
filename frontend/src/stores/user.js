import { defineStore } from 'pinia'
import { authApi } from '../services/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.user?.username || '',
  },
  
  actions: {
    async login(username, password) {
      const response = await authApi.login({ username, password })
      const { token, username: name, email, expiresIn } = response.data
      
      this.token = token
      this.user = { username: name, email }
      
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify({ username: name, email }))
      
      return response.data
    },
    
    async register(username, email, password) {
      const response = await authApi.register({ username, email, password })
      return response.data
    },
    
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    },
  },
})
