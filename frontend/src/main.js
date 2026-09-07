import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import axios from 'axios'
import './styles/common.css' // 导入全局样式

Vue.use(ElementUI)

// 配置axios - 从 window.API_CONFIG 读取后端地址（打包后可直接修改 public/config.js）
axios.defaults.baseURL = (window.API_CONFIG && window.API_CONFIG.baseURL) || 'http://localhost:8080/api'
axios.defaults.timeout = 10000

// 请求拦截器
axios.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElementUI.Message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    ElementUI.Message.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

Vue.prototype.$axios = axios
Vue.config.productionTip = false

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
