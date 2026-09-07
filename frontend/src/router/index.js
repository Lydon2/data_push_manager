import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/monitor'
  },
  {
    path: '/connector',
    name: 'Connector',
    component: () => import('../views/Connector.vue')
  },
  {
    path: '/task',
    name: 'Task',
    component: () => import('../views/Task.vue')
  },
  {
    path: '/task/wizard',
    name: 'TaskWizard',
    component: () => import('../views/TaskWizard.vue')
  },
  {
    path: '/log',
    name: 'Log',
    component: () => import('../views/Log.vue')
  },
  {
    path: '/dict/source',
    name: 'DictSource',
    component: () => import('../views/dict/DictSource.vue')
  },
  {
    path: '/dict/mapping',
    name: 'DictMapping',
    component: () => import('../views/dict/DictMapping.vue')
  },
  {
    path: '/dict/mapping/:id',
    name: 'DictMappingDetail',
    component: () => import('../views/dict/DictMappingDetail.vue')
  },
  {
    path: '/monitor',
    name: 'MonitorDashboard',
    component: () => import('../views/MonitorDashboard.vue')
  },
  {
    path: '/alert',
    name: 'AlertManagement',
    component: () => import('../views/AlertManagement.vue')
  },
  {
    path: '/debug/lineage',
    name: 'DebugLineage',
    component: () => import('../views/DebugLineage.vue')
  },
  
]

const router = new VueRouter({
  mode: 'hash',
  base: './',
  routes
})

export default router
