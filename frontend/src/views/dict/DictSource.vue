<template>
  <div class="dict-source-container">
    <el-card>
      <!-- 搜索区 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="数据源名称">
          <el-input v-model="searchForm.sourceName" placeholder="请输入数据源名称" clearable style="width: 200px"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="sourceName" label="数据源名称" min-width="150"></el-table-column>
        <el-table-column prop="sourceCode" label="数据源编码" min-width="150"></el-table-column>
        <el-table-column prop="connectorId" label="连接器ID" width="100"></el-table-column>
        <el-table-column prop="tableName" label="字典表名" min-width="120"></el-table-column>
        <el-table-column prop="keyField" label="键字段" width="100"></el-table-column>
        <el-table-column prop="valueField" label="值字段" width="100"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-view" @click="handlePreview(scope.row)">预览</el-button>
            <el-button type="text" icon="el-icon-connection" @click="handleTest(scope.row)">测试</el-button>
            <el-button type="text" icon="el-icon-edit" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" style="color: #F56C6C">删除</el-button>
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
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; text-align: right">
      </el-pagination>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose">
      <el-form :model="form" :rules="rules" ref="form" label-width="120px">
        <el-form-item label="数据源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="如：系统A-民族字典" @input="handleSourceNameChange"></el-input>
        </el-form-item>
        <el-form-item label="数据源编码" prop="sourceCode">
          <el-input v-model="form.sourceCode" placeholder="自动生成或手动输入"></el-input>
          <div style="color: #909399; font-size: 12px; margin-top: 4px">唯一标识，根据数据源名称自动生成，也可手动修改</div>
        </el-form-item>
        <el-form-item label="连接器" prop="connectorId">
          <el-select v-model="form.connectorId" placeholder="请选择连接器" style="width: 100%" @change="handleConnectorChange">
            <el-option v-for="item in connectorList" :key="item.id" :label="item.connectorName" :value="item.id">
              <div style="display: flex; align-items: center; gap: 8px;">
                <!-- Logo图标 -->
                <img v-if="item.connectorType === 'DATABASE' && item.dbType === 'MYSQL'" 
                  :src="require('@/assets/logos/mysql.svg')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="MySQL" />
                <img v-else-if="item.connectorType === 'DATABASE' && item.dbType === 'ORACLE'" 
                  :src="require('@/assets/logos/oracle.svg')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="Oracle" />
                <img v-else-if="item.connectorType === 'DATABASE' && item.dbType === 'POSTGRESQL'" 
                  :src="require('@/assets/logos/postgresql.svg')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="PostgreSQL" />
                <img v-else-if="item.connectorType === 'DATABASE' && item.dbType === 'SQLSERVER'" 
                  :src="require('@/assets/logos/sqlserver.svg')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="SQLServer" />
                <img v-else-if="item.connectorType === 'DATABASE' && item.dbType === 'KINGBASE'" 
                  :src="require('@/assets/logos/kingbase.png')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="KingBase" />
                <img v-else-if="item.connectorType === 'DATABASE' && item.dbType === 'DM'" 
                  :src="require('@/assets/logos/dm.png')" 
                  style="width: 24px; height: 24px; object-fit: contain; border-radius: 4px;" 
                  alt="DM" />
                <img v-else-if="item.connectorType === 'API'" 
                  :src="require('@/assets/logos/api.svg')" 
                  style="width: 18px; height: 18px; object-fit: contain;" 
                  alt="API" />
                <i v-else class="el-icon-connection" style="font-size: 18px; color: #9ca3af;"></i>
                <span>{{ item.connectorName }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="字典表名" prop="tableName">
          <el-select v-model="form.tableName" placeholder="请先选择连接器" style="width: 100%" @change="handleTableChange" :loading="tableLoading" :disabled="!form.connectorId">
            <el-option v-for="table in tableList" :key="table" :label="table" :value="table"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="键字段名" prop="keyField">
          <el-select v-model="form.keyField" placeholder="请先选择字典表" style="width: 100%" :loading="columnLoading" :disabled="!form.tableName">
            <el-option v-for="column in columnList" :key="column" :label="column" :value="column"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="值字段名" prop="valueField">
          <el-select v-model="form.valueField" placeholder="请先选择字典表" style="width: 100%" :loading="columnLoading" :disabled="!form.tableName">
            <el-option v-for="column in columnList" :key="column" :label="column" :value="column"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型字段名">
          <el-select v-model="form.typeField" placeholder="可选，如：dict_type" clearable style="width: 100%" :loading="columnLoading" :disabled="!form.tableName">
            <el-option v-for="column in columnList" :key="column" :label="column" :value="column"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型名称字段">
          <el-select v-model="form.typeLabelField" placeholder="可选，如：type_name" clearable style="width: 100%" :loading="columnLoading" :disabled="!form.tableName">
            <el-option v-for="column in columnList" :key="column" :label="column" :value="column"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="类型值">
          <el-input v-model="form.typeValue" placeholder="可选，如：NATION"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3"></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </span>
    </el-dialog>

    <!-- 预览对话框 -->
    <el-dialog title="字典数据预览" :visible.sync="previewVisible" width="900px">
      <!-- 查询表单 -->
      <el-form :inline="true" :model="previewSearchForm" style="margin-bottom: 15px">
        <el-form-item label="字典类型" v-if="previewTypeList.length > 0">
          <el-select v-model="previewSearchForm.typeValue" placeholder="请选择字典类型" clearable style="width: 180px" @change="handlePreviewSearch">
            <el-option v-for="item in previewTypeList" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="字典名称">
          <el-input v-model="previewSearchForm.keyword" placeholder="请输入字典名称" clearable style="width: 200px" @keyup.enter.native="handlePreviewSearch"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handlePreviewSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handlePreviewReset">重置</el-button>
        </el-form-item>
      </el-form>
      
      <!-- 数据表格 -->
      <el-table :data="filteredPreviewData" border stripe max-height="400" v-loading="previewLoading">
        <el-table-column v-for="(col, index) in previewColumns" :key="index" :prop="col" :label="col" min-width="120">
          <template slot-scope="scope">
            <span v-if="col === currentKeyField" style="color: #409EFF; font-weight: bold">{{ scope.row[col] }}</span>
            <span v-else-if="col === currentValueField" style="color: #67C23A">{{ scope.row[col] }}</span>
            <span v-else-if="col === currentTypeField" style="color: #E6A23C">{{ scope.row[col] }}</span>
            <span v-else-if="col === currentTypeLabelField" style="color: #F56C6C; font-weight: bold">{{ scope.row[col] }}</span>
            <span v-else>{{ scope.row[col] }}</span>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 统计信息 -->
      <div style="margin-top: 15px; color: #909399; font-size: 14px">
        <i class="el-icon-info"></i> 
        共 {{ filteredPreviewData.length }} 条数据
        <span style="margin-left: 20px">
          <span style="color: #409EFF">■</span> 键字段: {{ currentKeyField }}
        </span>
        <span style="margin-left: 15px">
          <span style="color: #67C23A">■</span> 值字段: {{ currentValueField }}
        </span>
        <span v-if="previewTypeList.length > 0" style="margin-left: 15px">
          <span style="color: #E6A23C">■</span> 类型字段: {{ currentTypeField }}
        </span>
        <span v-if="currentTypeLabelField" style="margin-left: 15px">
          <span style="color: #F56C6C">■</span> 类型名称: {{ currentTypeLabelField }}
        </span>
      </div>
      
      <span slot="footer">
        <el-button @click="previewVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DictSource',
  data() {
    return {
      loading: false,
      submitLoading: false,
      searchForm: {
        sourceName: ''
      },
      tableData: [],
      pagination: {
        current: 1,
        size: 10,
        total: 0
      },
      dialogVisible: false,
      dialogTitle: '新增字典数据源',
      form: {
        sourceName: '',
        sourceCode: '',
        connectorId: null,
        tableName: '',
        keyField: '',
        valueField: '',
        typeField: '',
        typeLabelField: '',
        typeValue: '',
        description: '',
        status: 1
      },
      rules: {
        sourceName: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
        sourceCode: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
        connectorId: [{ required: true, message: '请选择连接器', trigger: 'change' }],
        tableName: [{ required: true, message: '请输入字典表名', trigger: 'blur' }],
        keyField: [{ required: true, message: '请输入键字段名', trigger: 'blur' }],
        valueField: [{ required: true, message: '请输入值字段名', trigger: 'blur' }]
      },
      connectorList: [],
      tableList: [],
      columnList: [],
      tableLoading: false,
      columnLoading: false,
      previewVisible: false,
      previewData: [],
      previewColumns: [],
      previewLoading: false,
      previewSearchForm: {
        typeValue: '',
        keyword: ''
      },
      previewTypeList: [],
      currentKeyField: '',
      currentValueField: '',
      currentTypeField: '',
      currentTypeLabelField: '',
      
      // 上次自动生成的编码（用于判断是否需要重新生成）
      lastAutoGeneratedCode: ''
    }
  },
  created() {
    this.loadData()
    this.loadConnectors()
  },
  computed: {
    // 过滤后的预览数据
    filteredPreviewData() {
      let data = this.previewData
      
      // 按类型过滤
      if (this.previewSearchForm.typeValue && this.currentTypeField) {
        data = data.filter(row => {
          const rowTypeValue = String(row[this.currentTypeField])
          const searchTypeValue = String(this.previewSearchForm.typeValue)
          return rowTypeValue === searchTypeValue
        })
      }
      
      // 按关键词过滤
      if (this.previewSearchForm.keyword) {
        const keyword = this.previewSearchForm.keyword.toLowerCase()
        data = data.filter(row => {
          // 在值字段中搜索
          const valueStr = String(row[this.currentValueField] || '').toLowerCase()
          return valueStr.includes(keyword)
        })
      }
      
      return data
    }
  },
  methods: {
    // 根据数据源名称自动生成编码
    handleSourceNameChange(value) {
      // 如果编码为空或者是之前自动生成的，才自动生成新的
      if (!this.form.sourceCode || this.form.sourceCode === this.lastAutoGeneratedCode) {
        const code = this.generateSourceCode(value)
        this.form.sourceCode = code
        this.lastAutoGeneratedCode = code
      }
    },
    
    // 生成数据源编码：拼音首字母 + 随机值，全大写
    generateSourceCode(sourceName) {
      if (!sourceName) return 'DS_' + this.generateRandomCode()
      
      let prefix = ''
      let charCount = 0
      
      // 提取前4-6个有效字符
      for (let char of sourceName) {
        if (charCount >= 6) break
        
        if (/[a-zA-Z]/.test(char)) {
          // 英文字母直接转大写
          prefix += char.toUpperCase()
          charCount++
        } else if (/[\u4e00-\u9fa5]/.test(char)) {
          // 中文字符：使用拼音首字母算法
          const initial = this.getChineseInitial(char)
          prefix += initial
          charCount++
        } else if (/[0-9]/.test(char)) {
          // 数字直接保留
          prefix += char
          charCount++
        }
        // 其他特殊字符忽略
      }
      
      // 如果前缀为空，使用默认前缀
      if (!prefix) {
        prefix = 'DS'
      }
      
      // 生成随机后缀：4位大写字母+数字
      const randomSuffix = this.generateRandomCode()
      
      return prefix + '_' + randomSuffix
    },
    
    // 获取中文字符的拼音首字母（基于Unicode编码范围）
    getChineseInitial(char) {
      const code = char.charCodeAt(0)
      // 简化算法：根据 Unicode 编码转换为 A-Z
      return String.fromCharCode(65 + (code % 26))
    },
    
    // 生成随机编码：4位大写字母+数字组合
    generateRandomCode() {
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
      let result = ''
      for (let i = 0; i < 4; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      return result
    },
    
    loadData() {
      this.loading = true
      this.$axios.get('/v1/dict-source/page', {
        params: {
          current: this.pagination.current,
          size: this.pagination.size,
          sourceName: this.searchForm.sourceName
        }
      }).then(res => {
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    loadConnectors() {
      this.$axios.get('/v1/connector/list').then(res => {
        this.connectorList = res.data
      })
    },
    handleSearch() {
      this.pagination.current = 1
      this.loadData()
    },
    handleSizeChange(val) {
      this.pagination.size = val
      this.loadData()
    },
    handleCurrentChange(val) {
      this.pagination.current = val
      this.loadData()
    },
    handleAdd() {
      this.dialogTitle = '新增字典数据源'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑字典数据源'
      this.form = { ...row }
      this.dialogVisible = true
      
      // 编辑时加载表和字段
      if (this.form.connectorId) {
        this.loadTables(this.form.connectorId)
      }
      if (this.form.connectorId && this.form.tableName) {
        this.loadColumns(this.form.connectorId, this.form.tableName)
      }
    },
    handleDialogClose() {
      this.$refs.form.resetFields()
      this.form = {
        sourceName: '',
        sourceCode: '',
        connectorId: null,
        tableName: '',
        keyField: '',
        valueField: '',
        typeField: '',
        typeLabelField: '',
        typeValue: '',
        description: '',
        status: 1
      }
      this.tableList = []
      this.columnList = []
      this.lastAutoGeneratedCode = '' // 重置自动生成的编码
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.submitLoading = true
          const request = this.form.id
            ? this.$axios.put('/v1/dict-source', this.form)
            : this.$axios.post('/v1/dict-source', this.form)
          
          request.then(() => {
            this.$message.success(this.form.id ? '更新成功' : '新增成功')
            this.dialogVisible = false
            this.loadData()
          }).finally(() => {
            this.submitLoading = false
          })
        }
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除该字典数据源吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$axios.delete(`/v1/dict-source/${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    },
    handleTest(row) {
      const loading = this.$loading({ text: '正在测试连接...' })
      this.$axios.post(`/v1/dict-source/${row.id}/test`).then(res => {
        if (res.data.success) {
          this.$message.success(res.data.message)
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        loading.close()
      })
    },
    handlePreview(row) {
      this.previewLoading = true
      this.previewVisible = true
      
      // 记录当前字段配置
      this.currentKeyField = row.keyField
      this.currentValueField = row.valueField
      this.currentTypeField = row.typeField || ''
      this.currentTypeLabelField = row.typeLabelField || ''
      
      // 重置搜索条件
      this.previewSearchForm = {
        typeValue: '',
        keyword: ''
      }
      this.previewTypeList = []
      
      // 并行加载预览数据和类型列表
      Promise.all([
        this.$axios.get(`/v1/dict-source/${row.id}/preview`, { params: { limit: 100 } }),
        this.currentTypeField ? this.$axios.get(`/v1/dict-source/${row.id}/types`) : Promise.resolve({ data: [] })
      ]).then(([previewRes, typesRes]) => {
        // 处理预览数据
        this.previewData = previewRes.data.data || []
        if (this.previewData.length > 0) {
          this.previewColumns = Object.keys(this.previewData[0])
        }
        
        // 处理类型列表（从后端直接获取完整的类型列表）
        if (typesRes.data && typesRes.data.length > 0) {
          this.previewTypeList = typesRes.data
        }
      }).catch(err => {
        const errorMsg = err.response && err.response.data && err.response.data.message 
          ? err.response.data.message 
          : err.message || '加载数据失败'
        this.$message.error('加载数据失败: ' + errorMsg)
      }).finally(() => {
        this.previewLoading = false
      })
    },
    handlePreviewSearch() {
      // 搜索由computed属性自动处理
    },
    handlePreviewReset() {
      this.previewSearchForm = {
        typeValue: '',
        keyword: ''
      }
    },
    handleConnectorChange(connectorId) {
      // 清空表名和字段
      this.form.tableName = ''
      this.form.keyField = ''
      this.form.valueField = ''
      this.form.typeField = ''
      this.form.typeLabelField = ''
      this.tableList = []
      this.columnList = []
      
      // 加载表列表
      if (connectorId) {
        this.loadTables(connectorId)
      }
    },
    handleTableChange(tableName) {
      // 清空字段
      this.form.keyField = ''
      this.form.valueField = ''
      this.form.typeField = ''
      this.form.typeLabelField = ''
      this.columnList = []
      
      // 加载字段列表
      if (tableName && this.form.connectorId) {
        this.loadColumns(this.form.connectorId, tableName)
      }
    },
    loadTables(connectorId) {
      this.tableLoading = true
      this.$axios.get(`/v1/dict-source/connector/${connectorId}/tables`).then(res => {
        this.tableList = res.data || []
        if (this.tableList.length === 0) {
          this.$message.warning('该连接器下没有找到表')
        }
      }).catch(() => {
        this.$message.error('加载表列表失败')
      }).finally(() => {
        this.tableLoading = false
      })
    },
    loadColumns(connectorId, tableName) {
      this.columnLoading = true
      this.$axios.get(`/v1/dict-source/connector/${connectorId}/table/${tableName}/columns`).then(res => {
        this.columnList = res.data || []
        if (this.columnList.length === 0) {
          this.$message.warning('该表下没有找到字段')
        }
      }).catch(() => {
        this.$message.error('加载字段列表失败')
      }).finally(() => {
        this.columnLoading = false
      })
    }
  }
}
</script>

<style scoped>
.dict-source-container {
  padding: 20px;
}
</style>
