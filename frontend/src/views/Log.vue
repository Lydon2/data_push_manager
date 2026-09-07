<template>
  <div class="log-container">
    <el-card class="page-card">
      <div slot="header" class="card-header-custom">
        <div class="header-left">
          <i class="el-icon-document" style="margin-right: 8px; font-size: 18px; color: var(--primary-color);"></i>
          <span>执行日志</span>
        </div>
        <el-tag type="info" size="small"><i class="el-icon-tickets"></i> 共 {{ total }} 条记录</el-tag>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-form :inline="true" size="small">
          <el-form-item label="任务名称">
            <el-input v-model="queryParams.taskName" placeholder="请输入任务名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="执行状态">
            <el-select v-model="queryParams.executeStatus" placeholder="请选择" clearable style="width: 150px">
              <el-option label="执行中" value="RUNNING" />
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAILED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="executeStatus" label="执行状态" width="110">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.executeStatus === 'RUNNING'" type="warning" size="small">
              <i class="el-icon-loading"></i> 执行中
            </el-tag>
            <el-tag v-else-if="scope.row.executeStatus === 'SUCCESS'" type="success" size="small">
              <i class="el-icon-circle-check"></i> 成功
            </el-tag>
            <el-tag v-else type="danger" size="small">
              <i class="el-icon-circle-close"></i> 失败
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerType" label="触发类型" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.triggerType === 'MANUAL'" type="info" size="small">手动</el-tag>
            <el-tag v-else type="warning" size="small">定时</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总记录数" width="100" align="right">
          <template slot-scope="scope">
            <span class="number-highlight">{{ scope.row.totalCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功数" width="100" align="right">
          <template slot-scope="scope">
            <span class="number-success">{{ scope.row.successCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长" width="100" align="right">
          <template slot-scope="scope">
            <span class="duration-text">{{ formatDuration(scope.row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" icon="el-icon-view" @click="handleViewLog(scope.row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.current"
        :page-size="queryParams.size"
        layout="total, prev, pager, next, jumper"
        :total="total"
        class="pagination-custom">
      </el-pagination>
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog title="执行日志详情" :visible.sync="logDialogVisible" width="800px" class="log-detail-dialog">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="任务名称">{{ logDetail.taskName }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">
          <el-tag v-if="logDetail.executeStatus === 'SUCCESS'" type="success">  成功</el-tag>
          <el-tag v-else-if="logDetail.executeStatus === 'FAILED'" type="danger">失败</el-tag>
          <el-tag v-else type="warning">执行中</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="总记录数"><span class="number-highlight">{{ logDetail.totalCount }}</span></el-descriptions-item>
        <el-descriptions-item label="成功记录数"><span class="number-success">{{ logDetail.successCount }}</span></el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ logDetail.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ logDetail.endTime }}</el-descriptions-item>
        <el-descriptions-item label="执行时长"><span class="duration-text">{{ formatDuration(logDetail.duration) }}</span></el-descriptions-item>
        <el-descriptions-item label="触发类型">{{ logDetail.triggerType }}</el-descriptions-item>
      </el-descriptions>
      <el-divider><i class="el-icon-warning"></i> 错误信息</el-divider>
      <div v-if="logDetail.errorMessage" class="error-message-box">
        <el-input :value="logDetail.errorMessage" type="textarea" :rows="3" readonly />
      </div>
      <div v-else class="no-error">
        <i class="el-icon-circle-check"></i> 无错误信息
      </div>
      <el-divider><i class="el-icon-document"></i> 执行日志</el-divider>
      <div class="log-content">
        <el-input :value="logDetail.executeLog" type="textarea" :rows="15" readonly />
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  data() {
    return {
      queryParams: { current: 1, size: 10, taskName: '', executeStatus: '' },
      tableData: [],
      total: 0,
      logDialogVisible: false,
      logDetail: {}
    }
  },
  mounted() {
    // 如果有query参数，自动填充并查询
    if (this.$route.query.taskName) {
      this.queryParams.taskName = this.$route.query.taskName
    }
    this.loadData()
  },
  methods: {
    loadData() {
      this.$axios.get('/v1/log/page', { params: this.queryParams }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total
      })
    },
    handleCurrentChange(val) {
      this.queryParams.current = val
      this.loadData()
    },
    handleReset() {
      this.queryParams = {
        current: 1,
        size: 10,
        taskName: '',
        executeStatus: ''
      }
      this.loadData()
    },
    handleViewLog(row) {
      this.$axios.get(`/v1/log/${row.id}`).then(res => {
        this.logDetail = res.data
        this.logDialogVisible = true
      })
    },
    calculateDuration(startTime, endTime) {
      if (!startTime || !endTime) return '-'
      const start = new Date(startTime)
      const end = new Date(endTime)
      const duration = end - start
      if (duration < 1000) return duration + 'ms'
      if (duration < 60000) return (duration / 1000).toFixed(2) + 's'
      return (duration / 60000).toFixed(2) + 'min'
    },
    formatDuration(ms) {
      if (!ms) return '-'
      if (ms < 1000) return ms + 'ms'
      if (ms < 60000) return (ms / 1000).toFixed(2) + 's'
      return (ms / 60000).toFixed(2) + 'min'
    }
  }
}
</script>

<style scoped>
.log-container {
  padding: var(--spacing-lg);
  background: var(--bg-gray-50);
}

.page-card {
  border: 1px solid var(--border-color);
}

.search-section {
  background: var(--bg-white);
  border: 1px solid var(--border-light);
}

.number-success {
  color: var(--success-color);
  font-weight: 600;
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
}

.duration-text {
  color: var(--warning-color);
  font-weight: 500;
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.error-message-box {
  background: var(--danger-light);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--danger-color);
}

.no-error {
  text-align: center;
  padding: var(--spacing-lg);
  color: var(--success-color);
  font-size: 13px;
}

.no-error i {
  font-size: 20px;
  margin-right: var(--spacing-xs);
}

.log-content {
  background: var(--bg-gray-50);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
}

/* 对话框 */
.log-detail-dialog >>> .el-dialog__header {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
}

.log-detail-dialog >>> .el-dialog__title {
  color: var(--text-primary);
  font-weight: 600;
}

.log-detail-dialog >>> .el-divider__text {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 13px;
}
</style>