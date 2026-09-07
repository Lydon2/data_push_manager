<template>
  <div class="monitor-dashboard">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="header-icon">
          <i class="el-icon-data-analysis"></i>
        </div>
        <div class="header-info">
          <h1>监控仪表盘</h1>
          <p>实时监控任务执行状态与数据同步情况</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button icon="el-icon-refresh" size="small" @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card metric-primary">
          <div class="metric-icon">
            <div class="icon-circle">
              <i class="el-icon-s-data"></i>
            </div>
          </div>
          <div class="metric-info">
            <div class="metric-number">{{ dashboardData.totalTasks || 0 }}</div>
            <div class="metric-title">任务总数</div>
            <div class="metric-desc">Total Tasks</div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card metric-success">
          <div class="metric-icon">
            <div class="icon-circle">
              <i class="el-icon-circle-check"></i>
            </div>
          </div>
          <div class="metric-info">
            <div class="metric-number">{{ dashboardData.todaySuccessCount || 0 }}</div>
            <div class="metric-title">今日成功</div>
            <div class="metric-desc">Success Today</div>
          </div>
          <div class="metric-badge success" v-if="dashboardData.todaySuccessRate > 0">
            <i class="el-icon-top"></i> {{ dashboardData.todaySuccessRate }}%
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card metric-danger">
          <div class="metric-icon">
            <div class="icon-circle">
              <i class="el-icon-circle-close"></i>
            </div>
          </div>
          <div class="metric-info">
            <div class="metric-number">{{ dashboardData.todayFailCount || 0 }}</div>
            <div class="metric-title">今日失败</div>
            <div class="metric-desc">Failed Today</div>
          </div>
          <div class="metric-alert" v-if="dashboardData.todayFailCount > 0">
            <i class="el-icon-warning"></i>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card metric-warning clickable" @click="goToAlertPage">
          <div class="metric-icon">
            <div class="icon-circle">
              <i class="el-icon-bell"></i>
            </div>
          </div>
          <div class="metric-info">
            <div class="metric-number">{{ dashboardData.unreadAlerts || 0 }}</div>
            <div class="metric-title">待处理告警</div>
            <div class="metric-desc">Pending Alerts</div>
          </div>
          <div class="metric-pulse" v-if="dashboardData.unreadAlerts > 0"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 第二行统计 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6">
        <div class="info-card">
          <div class="info-icon">
            <i class="el-icon-upload2"></i>
          </div>
          <div class="info-content">
            <div class="info-value">{{ formatNumber(dashboardData.todayDataCount) }}</div>
            <div class="info-label">今日数据量</div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="info-card">
          <div class="info-icon running">
            <i class="el-icon-loading"></i>
          </div>
          <div class="info-content">
            <div class="info-value">{{ dashboardData.runningTasks || 0 }}</div>
            <div class="info-label">运行中</div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="info-card">
          <div class="info-icon">
            <i class="el-icon-pie-chart"></i>
          </div>
          <div class="info-content">
            <div class="info-value">{{ dashboardData.todaySuccessRate || 0 }}%</div>
            <div class="info-label">成功率</div>
          </div>
        </div>
      </el-col>
      
      <el-col :xs="12" :sm="6">
        <div class="info-card">
          <div class="info-icon">
            <i class="el-icon-timer"></i>
          </div>
          <div class="info-content">
            <div class="info-value">{{ formatDuration(dashboardData.avgDuration) }}</div>
            <div class="info-label">平均时长</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表和列表 -->
    <el-row :gutter="20">
      <!-- 执行趋势图 -->
      <el-col :xs="24" :md="16">
        <div class="chart-card">
          <div class="card-header">
            <div class="header-title">
              <i class="el-icon-data-line"></i>
              <span>最近7天执行趋势</span>
            </div>
          </div>
          <div class="card-body">
            <div id="trendChart" style="height: 320px;"></div>
          </div>
        </div>
      </el-col>
      
      <!-- 最近执行任务 -->
      <el-col :xs="24" :md="8">
        <div class="chart-card">
          <div class="card-header">
            <div class="header-title">
              <i class="el-icon-time"></i>
              <span>最近执行任务</span>
            </div>
          </div>
          <div class="card-body">
            <div class="recent-tasks">
              <div v-for="task in recentTasks" :key="task.id" class="task-item">
                <div class="task-status-indicator" :class="'status-' + task.executeStatus.toLowerCase()"></div>
                <div class="task-content">
                  <div class="task-header">
                    <span class="task-name">{{ task.taskName }}</span>
                    <el-tag :type="getStatusType(task.executeStatus)" size="mini" effect="plain">
                      {{ getStatusText(task.executeStatus) }}
                    </el-tag>
                  </div>
                  <div class="task-time">
                    <i class="el-icon-time"></i>
                    {{ task.startTime }}
                  </div>
                </div>
              </div>
              <div v-if="recentTasks.length === 0" class="empty-state">
                <i class="el-icon-document"></i>
                <p>暂无执行记录</p>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 性能监控 -->
    <div class="chart-card">
      <div class="card-header">
        <div class="header-title">
          <i class="el-icon-monitor"></i>
          <span>性能监控 - 连接池状态</span>
        </div>
        <el-button size="mini" icon="el-icon-refresh" @click="loadPoolStats">刷新</el-button>
      </div>
      <div class="card-body">
        <el-alert v-if="poolStats.length === 0" type="info" :closable="false" style="margin-bottom: 16px;">
          <template slot="title">
            <div style="display: flex; align-items: center; gap: 8px;">
              <i class="el-icon-info"></i>
              <span>暂无活跃连接池。连接池会在任务执行时自动创建，请先执行一个任务。</span>
            </div>
          </template>
        </el-alert>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="pool in poolStats" :key="pool.connectorId">
            <div class="pool-card">
              <div class="pool-header">
                <div class="pool-name">{{ pool.connectorName }}</div>
                <el-tag size="mini" :type="getDbTypeColor(pool.dbType)">{{ pool.dbType }}</el-tag>
              </div>
              <div class="pool-stats">{{ pool.stats }}</div>
              <div class="pool-optimization" v-if="pool.optimizations">
                <div class="opt-item">
                  <span class="opt-label">加载器:</span>
                  <span class="opt-value">{{ pool.optimizations.loader }}</span>
                </div>
                <div class="opt-item">
                  <span class="opt-label">性能:</span>
                  <span class="opt-value opt-highlight">{{ pool.optimizations.performance }}</span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
        <div v-if="poolStats.length === 0" class="empty-state" style="padding: 40px 20px;">
          <i class="el-icon-connection"></i>
          <p>执行任务后将显示连接池状态</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'

export default {
  name: 'MonitorDashboard',
  data() {
    return {
      dashboardData: {},
      recentTasks: [],
      trendChart: null,
      poolStats: []
    }
  },
  mounted() {
    this.loadDashboardData()
    this.loadRecentTasks()
    this.loadExecuteTrend()
    this.loadPoolStats()
    
    // 定时刷新（每30秒）
    this.refreshTimer = setInterval(() => {
      this.loadDashboardData()
      this.loadRecentTasks()
      this.loadPoolStats()
    }, 30000)
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
    if (this.trendChart) {
      this.trendChart.dispose()
    }
  },
  methods: {
    refreshAll() {
      this.loadDashboardData()
      this.loadRecentTasks()
      this.loadExecuteTrend()
      this.loadPoolStats()
      this.$message.success('刷新成功')
    },
    
    loadDashboardData() {
      this.$axios.get('/v1/monitor/dashboard').then(res => {
        this.dashboardData = res.data
      }).catch(err => {
        console.error('获取仪表盘数据失败', err)
      })
    },
    
    loadRecentTasks() {
      this.$axios.get('/v1/monitor/recent-tasks', {
        params: { limit: 10 }
      }).then(res => {
        this.recentTasks = res.data
      }).catch(err => {
        console.error('获取最近任务失败', err)
      })
    },
    
    loadExecuteTrend() {
      this.$axios.get('/v1/monitor/execute-trend').then(res => {
        this.renderTrendChart(res.data)
      }).catch(err => {
        console.error('获取执行趋势失败', err)
      })
    },
    
    renderTrendChart(data) {
      const chartDom = document.getElementById('trendChart')
      if (!chartDom) return
      
      if (this.trendChart) {
        this.trendChart.dispose()
      }
      
      this.trendChart = echarts.init(chartDom)
      
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['成功', '失败']
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: data.dates
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '成功',
            type: 'line',
            smooth: true,
            itemStyle: { color: '#67C23A' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
                { offset: 1, color: 'rgba(103, 194, 58, 0)' }
              ])
            },
            data: data.successCounts
          },
          {
            name: '失败',
            type: 'line',
            smooth: true,
            itemStyle: { color: '#F56C6C' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(245, 108, 108, 0.3)' },
                { offset: 1, color: 'rgba(245, 108, 108, 0)' }
              ])
            },
            data: data.failCounts
          }
        ]
      }
      
      this.trendChart.setOption(option)
    },
    
    formatNumber(num) {
      if (!num) return '0'
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
      }
      return num.toString()
    },
    
    formatDuration(ms) {
      if (!ms) return '0s'
      if (ms < 1000) {
        return ms + 'ms'
      }
      return (ms / 1000).toFixed(1) + 's'
    },
    
    getStatusType(status) {
      const map = {
        'SUCCESS': 'success',
        'FAILED': 'danger',
        'RUNNING': 'warning'
      }
      return map[status] || 'info'
    },
    
    getStatusText(status) {
      const map = {
        'SUCCESS': '成功',
        'FAILED': '失败',
        'RUNNING': '运行中'
      }
      return map[status] || status
    },
    
    loadPoolStats() {
      this.$axios.get('/v1/performance/pool-stats').then(res => {
        this.poolStats = res.data || []
      }).catch(err => {
        console.error('获取连接池状态失败', err)
      })
    },
    
    getDbTypeColor(dbType) {
      const colorMap = {
        'MYSQL': 'success',
        'POSTGRESQL': 'primary',
        'ORACLE': 'warning',
        'SQLSERVER': 'info',
        'KINGBASE': 'primary',
        'DM': 'warning'
      }
      return colorMap[dbType.toUpperCase()] || 'info'
    },
    
    goToAlertPage() {
      this.$router.push('/alert')
    }
  }
}
</script>

<style scoped>
.monitor-dashboard {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* ========== 页面头部 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 28px 32px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border-left: 4px solid #3b82f6;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  width: 56px;
  height: 56px;
  background: #eff6ff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon i {
  font-size: 28px;
  color: #3b82f6;
}

.header-info h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}

.header-info p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 400;
}

.header-actions .el-button {
  background: white;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.header-actions .el-button:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #1f2937;
}

/* ========== 统计卡片 ========== */
.stats-row {
  margin-bottom: 20px;
}

/* 主要指标卡片（第一行） */
.metric-card {
  position: relative;
  background: white;
  border-radius: 16px;
  padding: 28px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  min-height: 160px;
  display: flex;
  flex-direction: column;
}

.metric-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  transition: height 0.3s;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
  border-color: transparent;
}

.metric-card:hover::before {
  height: 4px;
}

.metric-card.clickable {
  cursor: pointer;
}

.metric-card.clickable:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
}

.metric-primary::before {
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
}

.metric-success::before {
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
}

.metric-danger::before {
  background: linear-gradient(90deg, #ef4444 0%, #dc2626 100%);
}

.metric-warning::before {
  background: linear-gradient(90deg, #f59e0b 0%, #d97706 100%);
}

.metric-icon {
  margin-bottom: 16px;
}

.icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.metric-primary .icon-circle {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.metric-primary .icon-circle i {
  color: #3b82f6;
  font-size: 28px;
}

.metric-success .icon-circle {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.metric-success .icon-circle i {
  color: #10b981;
  font-size: 28px;
}

.metric-danger .icon-circle {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
}

.metric-danger .icon-circle i {
  color: #ef4444;
  font-size: 28px;
}

.metric-warning .icon-circle {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}

.metric-warning .icon-circle i {
  color: #f59e0b;
  font-size: 28px;
}

.metric-card:hover .icon-circle {
  transform: scale(1.08) rotate(5deg);
}

.metric-info {
  flex: 1;
}

.metric-number {
  font-size: 40px;
  font-weight: 700;
  color: #111827;
  line-height: 1;
  margin-bottom: 10px;
  letter-spacing: -1px;
}

.metric-title {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 4px;
}

.metric-desc {
  font-size: 12px;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.metric-badge {
  position: absolute;
  top: 24px;
  right: 24px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.metric-badge.success {
  background: #d1fae5;
  color: #059669;
}

.metric-badge i {
  font-size: 14px;
}

.metric-alert {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fef2f2;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: shake 0.5s ease-in-out infinite alternate;
}

.metric-alert i {
  color: #ef4444;
  font-size: 18px;
}

@keyframes shake {
  0% { transform: rotate(-5deg); }
  100% { transform: rotate(5deg); }
}

.metric-pulse {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #f59e0b;
  animation: pulse-dot 2s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(245, 158, 11, 0);
  }
}

/* 次要信息卡片（第二行） */
.info-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #f3f4f6;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.info-card:hover {
  border-color: #e5e7eb;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.info-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.3s;
}

.info-icon i {
  font-size: 22px;
  color: #6b7280;
}

.info-icon.running {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.info-icon.running i {
  color: #3b82f6;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.info-card:hover .info-icon {
  transform: scale(1.1);
}

.info-content {
  flex: 1;
  min-width: 0;
}

.info-value {
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.2;
  margin-bottom: 4px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ========== 图表卡片 ========== */
.chart-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: all 0.3s;
  margin-bottom: 20px;
}

.chart-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-color: #d1d5db;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafbfc;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.header-title i {
  font-size: 18px;
  color: #3b82f6;
}

.card-body {
  padding: 24px;
}

/* ========== 最近任务列表 ========== */
.recent-tasks {
  max-height: 400px;
  overflow-y: auto;
}

.task-item {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  background: #f9fafb;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s;
}

.task-item:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.task-item:last-child {
  margin-bottom: 0;
}

.task-status-indicator {
  width: 4px;
  border-radius: 4px;
  flex-shrink: 0;
}

.status-success {
  background: #10b981;
}

.status-failed {
  background: #ef4444;
}

.status-running {
  background: #f59e0b;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.task-content {
  flex: 1;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.task-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #6b7280;
}

.task-time i {
  font-size: 14px;
}

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-state i {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
  display: block;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

/* ========== 性能监控 - 连接池卡片 ========== */
.pool-card {
  background: #f9fafb;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  transition: all 0.3s;
  margin-bottom: 16px;
}

.pool-card:hover {
  background: white;
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
  transform: translateY(-2px);
}

.pool-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.pool-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 8px;
}

.pool-stats {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 12px;
  font-family: 'Courier New', monospace;
}

.pool-optimization {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.opt-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.opt-label {
  color: #9ca3af;
  font-weight: 500;
}

.opt-value {
  color: #4b5563;
  font-weight: 600;
}

.opt-highlight {
  color: #10b981;
  font-weight: 700;
}

.card-header .el-button {
  background: white;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.card-header .el-button:hover {
  background: #f9fafb;
  border-color: #3b82f6;
  color: #3b82f6;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .monitor-dashboard {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }
  
  .header-content {
    width: 100%;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .header-actions .el-button {
    width: 100%;
  }
  
  .stat-card {
    height: auto;
    min-height: 100px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
  margin-right: 8px;
}

.pool-stats {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 12px;
  font-family: 'Courier New', monospace;
}

.pool-optimization {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.opt-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.opt-label {
  color: #9ca3af;
  font-weight: 500;
}

.opt-value {
  color: #4b5563;
  font-weight: 600;
}

.opt-highlight {
  color: #10b981;
  font-weight: 700;
}

.card-header .el-button {
  background: white;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.card-header .el-button:hover {
  background: #f9fafb;
  border-color: #3b82f6;
  color: #3b82f6;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .monitor-dashboard {
    padding: 16px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }
  
  .header-content {
    width: 100%;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .header-actions .el-button {
    width: 100%;
  }
  
  .stat-card {
    height: auto;
    min-height: 100px;
  }
  
  .stat-value {
    font-size: 24px;
  }
}
</style>
