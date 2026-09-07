<template>
  <div id="app">
    <el-container style="height: 100vh">
      <!-- 左侧菜单 -->
      <el-aside width="240px" class="sidebar-container">
        <!-- Logo区域 -->
        <div class="logo-container">
          <div class="logo-icon">
            <i class="el-icon-s-platform"></i>
          </div>
          <div class="logo-text">
            <div class="logo-title">数据对接平台</div>
            <div class="logo-subtitle">Data Push Manager</div>
          </div>
        </div>
        
        <!-- 导航菜单 -->
        <el-menu
          :default-active="activeMenuIndex"
          background-color="transparent"
          text-color="#4b5563"
          active-text-color="#3b82f6"
          router
          class="sidebar-menu">
          <el-menu-item index="/monitor">
            <i class="el-icon-pie-chart"></i>
            <span slot="title">监控仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/connector">
            <i class="el-icon-connection"></i>
            <span slot="title">连接器管理</span>
          </el-menu-item>
          <el-menu-item index="/task">
            <i class="el-icon-s-operation"></i>
            <span slot="title">任务管理</span>
          </el-menu-item>
          <el-submenu index="/dict">
            <template slot="title">
              <i class="el-icon-set-up"></i>
              <span>字典管理</span>
            </template>
            <el-menu-item index="/dict/source">字典数据源</el-menu-item>
            <el-menu-item index="/dict/mapping">字典映射</el-menu-item>
          </el-submenu>
          <el-menu-item index="/log">
            <i class="el-icon-document"></i>
            <span slot="title">执行日志</span>
          </el-menu-item>
          <el-menu-item index="/alert">
            <i class="el-icon-warning"></i>
            <span slot="title">告警管理</span>
          </el-menu-item>
        </el-menu>
        
        <!-- 底部信息 -->
        <div class="sidebar-footer">
          <div class="version-info">
            <i class="el-icon-info"></i> v1.0.0
          </div>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-container>
        <!-- 顶部导航栏 -->
        <el-header height="60px" class="main-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-button type="text" icon="el-icon-question" @click="showHelp">帮助</el-button>
            <el-button type="text" icon="el-icon-setting">设置</el-button>
          </div>
        </el-header>
        
        <!-- 主内容 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App',
  computed: {
    activeMenuIndex() {
      const path = this.$route.path
      // 如果是任务向导页面，激活任务管理菜单
      if (path.startsWith('/task/')) {
        return '/task'
      }
      // 如果是字典相关页面，保持子菜单激活
      if (path.startsWith('/dict/')) {
        return path
      }
      // 其他页面直接返回路径
      return path
    },
    currentPageTitle() {
      const route = this.$route
      const titles = {
        '/monitor': '监控仪表盘',
        '/connector': '连接器管理',
        '/task': '任务管理',
        '/task/wizard': '任务配置',
        '/dict/source': '字典数据源',
        '/dict/mapping': '字典映射',
        '/log': '执行日志',
        '/alert': '告警管理'
      }
      return titles[route.path] || '首页'
    }
  },
  methods: {
    showHelp() {
      const helpContent = `
        <div style="text-align: left">
          <h3>常见问题</h3>
          <p>1. 如何创建连接器？</p>
          <p style="color: #909399; font-size: 13px; padding-left: 20px">进入<strong>连接器管理</strong>页面，点击<strong>新增</strong>按钮</p>
          <p>2. 如何配置同步任务？</p>
          <p style="color: #909399; font-size: 13px; padding-left: 20px">进入<strong>任务管理</strong>页面，使用<strong>向导式配置</strong></p>
          <p>3. 如何查看执行日志？</p>
          <p style="color: #909399; font-size: 13px; padding-left: 20px">进入<strong>执行日志</strong>页面，选择任务查看详细日志</p>
        </div>
      `
      this.$alert(helpContent, '帮助文档', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '我知道了'
      })
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
}

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

/* Logo区域样式 */
.sidebar-container {
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.05);
  border-right: 1px solid #e8e8e8;
  position: relative;
}

.logo-container {
  display: flex;
  align-items: center;
  padding: 24px 20px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  margin-bottom: 0;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
}

.logo-icon {
  width: 42px;
  height: 42px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.logo-text {
  flex: 1;
}

.logo-title {
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 4px;
  letter-spacing: 0.5px;
}

.logo-subtitle {
  font-size: 11px;
  opacity: 0.9;
  letter-spacing: 1px;
  font-weight: 300;
}

/* 侧边栏菜单样式 */
.sidebar-menu {
  border-right: none;
  background: transparent !important;
  padding: 12px 0;
}

.sidebar-menu .el-menu-item {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 4px 12px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  color: #4b5563 !important;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.sidebar-menu .el-menu-item i {
  color: #6b7280;
  font-size: 18px;
  margin-right: 10px;
  transition: all 0.3s;
}

.sidebar-menu .el-menu-item:hover {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, rgba(59, 130, 246, 0.04) 100%) !important;
  color: #3b82f6 !important;
}

.sidebar-menu .el-menu-item:hover i {
  color: #3b82f6;
  transform: scale(1.1);
}

.sidebar-menu .el-menu-item.is-active {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.12) 0%, rgba(59, 130, 246, 0.06) 100%) !important;
  color: #3b82f6 !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.sidebar-menu .el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 0 2px 2px 0;
}

.sidebar-menu .el-menu-item.is-active i {
  color: #3b82f6;
}

.sidebar-menu .el-submenu__title {
  margin: 4px 12px;
  border-radius: 8px;
  height: 48px;
  line-height: 48px;
  color: #4b5563 !important;
  font-weight: 500;
  transition: all 0.3s;
}

.sidebar-menu .el-submenu__title i {
  color: #6b7280;
  font-size: 18px;
  transition: all 0.3s;
}

.sidebar-menu .el-submenu__title:hover {
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.08) 0%, rgba(59, 130, 246, 0.04) 100%) !important;
  color: #3b82f6 !important;
}

.sidebar-menu .el-submenu__title:hover i {
  color: #3b82f6;
}

.sidebar-menu .el-submenu.is-active .el-submenu__title {
  color: #3b82f6 !important;
  font-weight: 600;
}

.sidebar-menu .el-submenu.is-active .el-submenu__title i {
  color: #3b82f6;
}

.sidebar-menu .el-menu {
  background: rgba(0, 0, 0, 0.02) !important;
}

.sidebar-menu .el-menu .el-menu-item {
  color: #6b7280 !important;
  font-size: 13px;
  height: 42px;
  line-height: 42px;
  padding-left: 54px !important;
}

.sidebar-menu .el-menu .el-menu-item:hover {
  color: #3b82f6 !important;
  background: rgba(59, 130, 246, 0.08) !important;
}

.sidebar-menu .el-menu .el-menu-item.is-active {
  color: #3b82f6 !important;
  font-weight: 600;
  background: rgba(59, 130, 246, 0.12) !important;
}

/* 侧边栏底部 */
.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  background: linear-gradient(180deg, transparent 0%, rgba(0, 0, 0, 0.02) 100%);
}

.version-info {
  color: #9ca3af;
  font-size: 12px;
  text-align: center;
  font-weight: 500;
}

/* 顶部导航栏 */
.main-header {
  background: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  z-index: 10;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  flex: 1;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right .el-button {
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.header-right .el-button:hover {
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.08);
}

/* 主内容区 */
.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  min-height: calc(100vh - 60px);
}

/* 面包屑样式 */
.el-breadcrumb {
  font-size: 14px;
  line-height: 1;
}

.el-breadcrumb__item .el-breadcrumb__inner {
  color: #9ca3af;
  font-weight: 500;
  transition: color 0.2s;
}

.el-breadcrumb__item .el-breadcrumb__inner:hover {
  color: #3b82f6;
}

.el-breadcrumb__item:last-child .el-breadcrumb__inner {
  color: #1f2937;
  font-weight: 600;
}
</style>
