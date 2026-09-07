<template>
  <div style="padding: 20px">
    <el-card>
      <div slot="header">
        <h3>数据血缘调试工具</h3>
      </div>
      
      <el-button type="primary" @click="loadAllLineage" :loading="loading">查询所有血缘记录</el-button>
      
      <div v-if="lineageData.length > 0" style="margin-top: 20px">
        <el-alert type="success" :closable="false" style="margin-bottom: 15px">
          找到 <strong>{{ lineageData.length }}</strong> 条血缘记录
        </el-alert>
        
        <el-table :data="lineageData" border size="small" max-height="600">
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="taskId" label="任务ID" width="80" />
          <el-table-column prop="sourceConnectorId" label="源连接器ID" width="100" />
          <el-table-column prop="sourceTable" label="源表" width="150" show-overflow-tooltip />
          <el-table-column prop="sourceField" label="源字段" width="120" show-overflow-tooltip />
          <el-table-column prop="targetConnectorId" label="目标连接器ID" width="120" />
          <el-table-column prop="targetTable" label="目标表" width="150" show-overflow-tooltip />
          <el-table-column prop="targetField" label="目标字段" width="120" show-overflow-tooltip />
          <el-table-column prop="transformType" label="转换类型" width="100" />
          <el-table-column prop="transformRule" label="转换规则" show-overflow-tooltip />
        </el-table>
      </div>
      
      <el-empty v-else-if="!loading" description="暂无血缘数据" />
    </el-card>
  </div>
</template>

<script>
export default {
  data() {
    return {
      loading: false,
      lineageData: []
    }
  },
  mounted() {
    this.loadAllLineage()
  },
  methods: {
    loadAllLineage() {
      this.loading = true
      this.$axios.get('/v1/data-lineage/all').then(res => {
        this.lineageData = res.data || []
        this.$message.success(`加载成功，共 ${this.lineageData.length} 条记录`)
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('加载失败: ' + errMsg)
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
