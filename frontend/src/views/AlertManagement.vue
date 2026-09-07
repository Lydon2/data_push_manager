<template>
  <div class="alert-management">
    <el-card class="page-card">
      <div slot="header" class="card-header-custom">
        <div class="header-left">
          <i class="el-icon-bell" style="margin-right: 8px; font-size: 18px; color: var(--warning-color);"></i>
          <span>告警管理</span>
        </div>
        <div class="header-stats">
          <el-tag type="danger" size="small">
            <i class="el-icon-warning"></i> 待处理: {{ pendingCount }}
          </el-tag>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-form :inline="true" :model="searchForm" size="small">
          <el-form-item label="告警级别">
            <el-select v-model="searchForm.alertLevel" placeholder="请选择" clearable style="width: 150px;">
              <el-option label="提示" value="INFO"></el-option>
              <el-option label="警告" value="WARNING"></el-option>
              <el-option label="错误" value="ERROR"></el-option>
              <el-option label="严重" value="CRITICAL"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态">
            <el-select v-model="searchForm.alertStatus" placeholder="请选择" clearable style="width: 150px;">
              <el-option label="待处理" value="PENDING"></el-option>
              <el-option label="已通知" value="NOTIFIED"></el-option>
              <el-option label="已处理" value="HANDLED"></el-option>
              <el-option label="已忽略" value="IGNORED"></el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadAlertList">查询</el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 告警列表 -->
      <el-table :data="alertList" border stripe class="data-table">
        <el-table-column prop="alertTitle" label="告警标题" min-width="200" show-overflow-tooltip>
          <template slot-scope="scope">
            <div class="alert-title">
              <i :class="getLevelIcon(scope.row.alertLevel)" :style="{color: getLevelColor(scope.row.alertLevel)}"></i>
              {{ scope.row.alertTitle }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="alertLevel" label="级别" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getLevelType(scope.row.alertLevel)" size="small">
              {{ getLevelText(scope.row.alertLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="taskName" label="关联任务" width="150" show-overflow-tooltip></el-table-column>
        
        <el-table-column prop="alertTime" label="告警时间" width="160"></el-table-column>
        
        <el-table-column prop="alertStatus" label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.alertStatus)" size="small">
              <i :class="getStatusIcon(scope.row.alertStatus)"></i>
              {{ getStatusText(scope.row.alertStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" icon="el-icon-view" @click="handleViewDetail(scope.row)" plain>查看</el-button>
            <el-button v-if="scope.row.alertStatus !== 'HANDLED'" type="success" size="mini" icon="el-icon-check" @click="handleAlert(scope.row)" plain>处理</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.current"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        class="pagination-custom">
      </el-pagination>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog title="告警详情" :visible.sync="detailDialogVisible" width="600px" class="alert-detail-dialog">
      <el-descriptions :column="1" border v-if="currentAlert">
        <el-descriptions-item label="告警标题">{{ currentAlert.alertTitle }}</el-descriptions-item>
        <el-descriptions-item label="告警级别">
          <el-tag :type="getLevelType(currentAlert.alertLevel)" size="small">
            {{ getLevelText(currentAlert.alertLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="告警类型">{{ currentAlert.alertType }}</el-descriptions-item>
        <el-descriptions-item label="关联任务">{{ currentAlert.taskName }}</el-descriptions-item>
        <el-descriptions-item label="告警时间">{{ currentAlert.alertTime }}</el-descriptions-item>
        <el-descriptions-item label="告警内容">
          <div class="alert-content-box">
            <pre>{{ currentAlert.alertContent }}</pre>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="发送渠道">{{ currentAlert.sendChannels }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentAlert.alertStatus)" size="small">
            {{ getStatusText(currentAlert.alertStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.handleUser" label="处理人">{{ currentAlert.handleUser }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.handleTime" label="处理时间">{{ currentAlert.handleTime }}</el-descriptions-item>
        <el-descriptions-item v-if="currentAlert.handleRemark" label="处理备注">{{ currentAlert.handleRemark }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 处理对话框 -->
    <el-dialog title="处理告警" :visible.sync="handleDialogVisible" width="500px" class="handle-dialog">
      <el-form :model="handleForm" label-width="80px" size="small">
        <el-form-item label="处理人" required>
          <el-input v-model="handleForm.handleUser" placeholder="请输入处理人"></el-input>
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input type="textarea" v-model="handleForm.handleRemark" :rows="4" placeholder="请输入处理备注"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle" icon="el-icon-check">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'AlertManagement',
  data() {
    return {
      searchForm: {
        alertLevel: '',
        alertStatus: ''
      },
      alertList: [],
      pagination: {
        current: 1,
        size: 10,
        total: 0
      },
      detailDialogVisible: false,
      handleDialogVisible: false,
      currentAlert: null,
      handleForm: {
        handleUser: '',
        handleRemark: ''
      }
    }
  },
  computed: {
    pendingCount() {
      return this.alertList.filter(item => item.alertStatus === 'PENDING').length
    }
  },
  mounted() {
    this.loadAlertList()
  },
  methods: {
    loadAlertList() {
      const params = {
        current: this.pagination.current,
        size: this.pagination.size,
        alertLevel: this.searchForm.alertLevel,
        alertStatus: this.searchForm.alertStatus
      }
      
      this.$axios.get('/v1/monitor/alerts', { params }).then(res => {
        this.alertList = res.data.records
        this.pagination.total = res.data.total
      }).catch(err => {
        this.$message.error('获取告警列表失败')
        console.error(err)
      })
    },
    
    handleReset() {
      this.searchForm = {
        alertLevel: '',
        alertStatus: ''
      }
      this.pagination.current = 1
      this.loadAlertList()
    },
    
    handleSizeChange(size) {
      this.pagination.size = size
      this.loadAlertList()
    },
    
    handleCurrentChange(current) {
      this.pagination.current = current
      this.loadAlertList()
    },
    
    handleViewDetail(row) {
      this.currentAlert = row
      this.detailDialogVisible = true
    },
    
    handleAlert(row) {
      this.currentAlert = row
      this.handleForm = {
        handleUser: '',
        handleRemark: ''
      }
      this.handleDialogVisible = true
    },
    
    submitHandle() {
      if (!this.handleForm.handleUser) {
        this.$message.warning('请输入处理人')
        return
      }
      
      const params = {
        handleUser: this.handleForm.handleUser,
        handleRemark: this.handleForm.handleRemark
      }
      
      this.$axios.post(`/v1/monitor/alerts/${this.currentAlert.id}/handle`, null, { params }).then(() => {
        this.$message.success('处理成功')
        this.handleDialogVisible = false
        this.loadAlertList()
      }).catch(err => {
        this.$message.error('处理失败')
        console.error(err)
      })
    },
    
    getLevelType(level) {
      const map = {
        'INFO': 'info',
        'WARNING': 'warning',
        'ERROR': 'danger',
        'CRITICAL': 'danger'
      }
      return map[level] || 'info'
    },
    
    getLevelText(level) {
      const map = {
        'INFO': '提示',
        'WARNING': '警告',
        'ERROR': '错误',
        'CRITICAL': '严重'
      }
      return map[level] || level
    },
    
    getStatusType(status) {
      const map = {
        'PENDING': 'warning',
        'NOTIFIED': 'primary',
        'HANDLED': 'success',
        'IGNORED': 'info'
      }
      return map[status] || 'info'
    },
    
    getStatusText(status) {
      const map = {
        'PENDING': '待处理',
        'NOTIFIED': '已通知',
        'HANDLED': '已处理',
        'IGNORED': '已忽略'
      }
      return map[status] || status
    },
    getLevelIcon(level) {
      const map = {
        'INFO': 'el-icon-info',
        'WARNING': 'el-icon-warning',
        'ERROR': 'el-icon-error',
        'CRITICAL': 'el-icon-warning-outline'
      }
      return map[level] || 'el-icon-info'
    },
    getLevelColor(level) {
      const map = {
        'INFO': '#909399',
        'WARNING': '#E6A23C',
        'ERROR': '#F56C6C',
        'CRITICAL': '#F56C6C'
      }
      return map[level] || '#909399'
    },
    getStatusIcon(status) {
      const map = {
        'PENDING': 'el-icon-time',
        'NOTIFIED': 'el-icon-message',
        'HANDLED': 'el-icon-circle-check',
        'IGNORED': 'el-icon-remove'
      }
      return map[status] || 'el-icon-info'
    }
  }
}
</script>

<style scoped>
.alert-management {
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

.alert-title {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.alert-title i {
  font-size: 14px;
}

.alert-content-box {
  background: var(--bg-gray-50);
  padding: var(--spacing-md);
  border-radius: var(--radius-sm);
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--border-light);
}

.alert-content-box pre {
  white-space: pre-wrap;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-primary);
}

/* 对话框 */
.alert-detail-dialog >>> .el-dialog__header,
.handle-dialog >>> .el-dialog__header {
  background: var(--bg-white);
  border-bottom: 1px solid var(--border-light);
}

.alert-detail-dialog >>> .el-dialog__title,
.handle-dialog >>> .el-dialog__title {
  color: var(--text-primary);
  font-weight: 600;
}
</style>
