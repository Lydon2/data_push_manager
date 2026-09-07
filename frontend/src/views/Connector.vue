<template>
  <div class="connector-container">
    <el-card class="page-card">
      <div slot="header" class="card-header-custom">
        <div class="header-left">
          <i class="el-icon-connection" style="margin-right: 8px; font-size: 18px; color: var(--success-color);"></i>
          <span>连接器管理</span>
        </div>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增连接器</el-button>
      </div>

      <!-- 搜索区 -->
      <div class="search-section">
        <el-form :inline="true" size="small">
          <el-form-item label="连接器名称">
            <el-input v-model="queryParams.connectorName" placeholder="请输入连接器名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="连接器类型">
            <el-select v-model="queryParams.connectorType" placeholder="请选择" clearable style="width: 150px">
              <el-option label="数据库" value="DATABASE" />
              <el-option label="API" value="API" />
              <el-option label="文件" value="FILE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
          </el-form-item>
          <el-form-item style="float: right">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="card">
                <i class="el-icon-menu"></i> 卡片视图
              </el-radio-button>
              <el-radio-button label="table">
                <i class="el-icon-s-grid"></i> 表格视图
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'card'" class="card-view">
        <div class="card-grid">
          <div v-for="item in tableData" :key="item.id" class="card-grid-item">
            <div class="connector-card" :class="'connector-card-' + item.connectorType.toLowerCase()">
              <!-- 卡片头部 -->
              <div class="card-header-new">
                <!-- Logo 和类型标签 -->
                <div class="card-header-top">
                  <div class="logo-wrapper">
                    <!-- 数据库Logo -->
                    <template v-if="item.connectorType === 'DATABASE'">
                      <img v-if="item.dbType === 'MYSQL'" 
                        :src="require('@/assets/logos/mysql.svg')" 
                        class="db-logo" 
                        alt="MySQL" />
                      <img v-else-if="item.dbType === 'ORACLE'" 
                        :src="require('@/assets/logos/oracle.svg')" 
                        class="db-logo" 
                        alt="Oracle" />
                      <img v-else-if="item.dbType === 'POSTGRESQL'" 
                        :src="require('@/assets/logos/postgresql.svg')" 
                        class="db-logo" 
                        alt="PostgreSQL" />
                      <img v-else-if="item.dbType === 'SQLSERVER'" 
                        :src="require('@/assets/logos/sqlserver.svg')" 
                        class="db-logo" 
                        alt="SQL Server" />
                      <img v-else-if="item.dbType === 'KINGBASE'" 
                        :src="require('@/assets/logos/kingbase.png')" 
                        class="db-logo" 
                        alt="KingBase" />
                      <img v-else-if="item.dbType === 'DM'" 
                        :src="require('@/assets/logos/dm.png')" 
                        style="width: 60px; height: 60px; object-fit: contain; border-radius: 8px;" 
                        alt="DM" />
                      <i v-else class="el-icon-coin card-icon" style="color: var(--success-color)"></i>
                    </template>
                    <!-- API Logo -->
                    <template v-else-if="item.connectorType === 'API'">
                      <img :src="require('@/assets/logos/api.svg')" class="db-logo" alt="API" />
                    </template>
                    <!-- 文件 Logo -->
                    <template v-else>
                      <i class="el-icon-document card-icon" style="color: var(--info-color)"></i>
                    </template>
                  </div>
                  
                  <div class="type-badge" :class="'badge-' + item.connectorType.toLowerCase()">
                    <span v-if="item.connectorType === 'DATABASE'">数据库</span>
                    <span v-else-if="item.connectorType === 'API'">API</span>
                    <span v-else>文件</span>
                  </div>
                </div>
                
                <!-- 连接器名称 -->
                <div class="card-title">
                  <h3>{{ item.connectorName }}</h3>
                  <el-tag :type="item.status === 1 ? 'success' : 'danger'" size="mini" class="status-tag">
                    {{ item.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </div>
              </div>
              
              <!-- 卡片内容 -->
              <div class="card-body-new">
                <!-- 数据库类型 -->
                <div v-if="item.connectorType === 'DATABASE'" class="info-list">
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-coin"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">数据库类型</div>
                      <div class="info-value">{{ item.dbType }}</div>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-monitor"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">主机地址</div>
                      <div class="info-value">{{ item.host }}</div>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-connection"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">端口</div>
                      <div class="info-value">{{ item.port }}</div>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-document"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">数据库名</div>
                      <div class="info-value">{{ item.databaseName || '-' }}</div>
                    </div>
                  </div>
                </div>
                
                <!-- API类型 -->
                <div v-else-if="item.connectorType === 'API'" class="info-list">
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-monitor"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">主机地址</div>
                      <div class="info-value">{{ item.host || 'localhost' }}</div>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-connection"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">端口</div>
                      <div class="info-value">{{ item.port || 80 }}</div>
                    </div>
                  </div>
                  <div class="info-row">
                    <div class="info-icon">
                      <i class="el-icon-link"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">BaseURI</div>
                      <div class="info-value">{{ item.url || '/' }}</div>
                    </div>
                  </div>
                  <div class="info-row full-width">
                    <div class="info-icon">
                      <i class="el-icon-position"></i>
                    </div>
                    <div class="info-content">
                      <div class="info-label">完整地址</div>
                      <el-tooltip :content="buildApiUrl(item)" placement="top">
                        <div class="info-value url-text">{{ buildApiUrl(item) }}</div>
                      </el-tooltip>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 卡片底部操作 -->
              <div class="card-footer-new">
                <el-button size="small" type="primary" icon="el-icon-connection" 
                  :loading="testLoadingMap[item.id]" 
                  @click="handleTest(item)" 
                  plain>测试</el-button>
                <el-button size="small" type="warning" icon="el-icon-edit" 
                  @click="handleEdit(item)" 
                  plain>编辑</el-button>
                <el-button size="small" type="danger" icon="el-icon-delete" 
                  @click="handleDelete(item)" 
                  plain>删除</el-button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <el-empty v-if="tableData.length === 0" description="暂无连接器数据"></el-empty>
      </div>

      <!-- 表格视图 -->
      <div v-else>
        <el-table :data="tableData" border stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="connectorName" label="连接器名称" min-width="150" />
          <el-table-column prop="connectorType" label="类型" width="100">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.connectorType === 'DATABASE'" type="success">数据库</el-tag>
              <el-tag v-else-if="scope.row.connectorType === 'API'" type="warning">API</el-tag>
              <el-tag v-else type="info">文件</el-tag>
            </template>
          </el-table-column>
          
          <!-- 数据库专属列 -->
          <template v-if="hasDatabase">
            <el-table-column label="数据库类型" width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.connectorType === 'DATABASE'">{{ scope.row.dbType }}</span>
                <span v-else style="color: #C0C4CC; font-size: 12px">N/A</span>
              </template>
            </el-table-column>
            <el-table-column label="数据库名" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.connectorType === 'DATABASE'">{{ scope.row.databaseName || '-' }}</span>
                <span v-else style="color: #C0C4CC; font-size: 12px">N/A</span>
              </template>
            </el-table-column>
          </template>
          
          <el-table-column label="主机地址" min-width="150">
            <template slot-scope="scope">
              <span v-if="scope.row.host">{{ scope.row.host }}</span>
              <span v-else style="color: #C0C4CC">-</span>
            </template>
          </el-table-column>
          
          <el-table-column label="端口" width="80">
            <template slot-scope="scope">
              <span v-if="scope.row.port">{{ scope.row.port }}</span>
              <span v-else style="color: #C0C4CC">-</span>
            </template>
          </el-table-column>
          
          <!-- API专属列 -->
          <template v-if="hasAPI">
            <el-table-column label="BaseURI" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.connectorType === 'API'">{{ scope.row.url || '/' }}</span>
                <span v-else style="color: #C0C4CC; font-size: 12px">N/A</span>
              </template>
            </el-table-column>
          </template>
          
          <el-table-column prop="status" label="状态" width="80">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status === 1" type="success" size="small">启用</el-tag>
              <el-tag v-else type="danger" size="small">禁用</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="260" fixed="right">
            <template slot-scope="scope">
              <el-button size="mini" type="primary" :loading="testLoadingMap[scope.row.id]" :disabled="testLoadingMap[scope.row.id]" @click="handleTest(scope.row)">测试连接</el-button>
              <el-button size="mini" type="warning" @click="handleEdit(scope.row)">编辑</el-button>
              <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="queryParams.current"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="queryParams.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        style="margin-top: 20px; text-align: right">
      </el-pagination>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose">
      <el-form :model="form" :rules="rules" ref="form" label-width="120px" size="small">
        <el-form-item label="连接器名称" prop="connectorName">
          <el-input v-model="form.connectorName" placeholder="请输入连接器名称" />
        </el-form-item>
        <el-form-item label="连接器类型" prop="connectorType">
          <el-select v-model="form.connectorType" placeholder="请选择" @change="handleTypeChange">
            <el-option label="数据库" value="DATABASE" />
            <el-option label="API" value="API" />
          </el-select>
        </el-form-item>
        <template v-if="form.connectorType === 'DATABASE'">
          <el-form-item label="数据库类型" prop="dbType">
            <el-select v-model="form.dbType" placeholder="请选择">
              <el-option label="MySQL" value="MYSQL">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/mysql.svg')" style="width: 18px; height: 18px; object-fit: contain;" alt="MySQL" />
                  <span>MySQL</span>
                </div>
              </el-option>
              <el-option label="Oracle" value="ORACLE">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/oracle.svg')" style="width: 18px; height: 18px; object-fit: contain;" alt="Oracle" />
                  <span>Oracle</span>
                </div>
              </el-option>
              <el-option label="PostgreSQL" value="POSTGRESQL">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/postgresql.svg')" style="width: 18px; height: 18px; object-fit: contain;" alt="PostgreSQL" />
                  <span>PostgreSQL</span>
                </div>
              </el-option>
              <el-option label="SQL Server" value="SQLSERVER">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/sqlserver.svg')" style="width: 18px; height: 18px; object-fit: contain;" alt="SQL Server" />
                  <span>SQL Server</span>
                </div>
              </el-option>
              <el-option label="金仓" value="KINGBASE">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/kingbase.png')" style="width: 18px; height: 18px; object-fit: contain;" alt="KingBase" />
                  <span>金仓</span>
                </div>
              </el-option>
              <el-option label="达梦" value="DM">
                <div style="display: flex; align-items: center; gap: 8px;">
                  <img :src="require('@/assets/logos/dm.png')" style="width: 24px; height: 24px; object-fit: contain; border-radius: 4px;" alt="DM" />
                  <span>达梦</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="主机地址" prop="host">
            <el-input v-model="form.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input v-model.number="form.port" placeholder="请输入端口" />
          </el-form-item>
          <el-form-item label="数据库名" prop="databaseName">
            <el-input v-model="form.databaseName" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="form.password" 
              type="password" 
              :placeholder="form.id ? '不修改则保持原密码' : '请输入密码'" 
              show-password
              autocomplete="new-password"
            />
          </el-form-item>
        </template>
        <template v-else-if="form.connectorType === 'API'">
          <el-form-item label="IP地址" prop="host">
            <el-input v-model="form.host" placeholder="127.0.0.1 或 api.example.com" />
            <div class="form-tip">服务器IP地址或域名</div>
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input v-model.number="form.port" placeholder="80" type="number" />
            <div class="form-tip">API服务端口，如: 80, 443, 8080</div>
          </el-form-item>
          <el-form-item label="BaseURI" prop="url">
            <el-input v-model="form.url" placeholder="/api/v1" />
            <div class="form-tip">基础路径，如: /api/v1 或 /，完整地址示例: http://{{ form.host }}:{{ form.port }}{{ form.url }}</div>
          </el-form-item>
        </template>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Connector',
  data() {
    return {
      viewMode: 'card', // 视图模式: card 或 table
      queryParams: {
        current: 1,
        size: 10,
        connectorName: '',
        connectorType: ''
      },
      tableData: [],
      total: 0,
      testLoadingMap: {},
      dialogVisible: false,
      dialogTitle: '新增连接器',
      form: {
        connectorName: '',
        connectorType: 'DATABASE',
        dbType: 'MYSQL',
        host: '',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
        url: '',
        authType: 'NONE',
        authConfig: '',
        customHeaders: [],
        customParams: [],
        // 测试请求相关字段
        testMethod: 'GET',
        testPath: '',
        testActiveTab: 'params',
        testParams: [],
        testBodyType: 'none',
        testFormData: [],
        testJsonBody: '',
        testHeaders: [],
        description: '',
        status: 1
      },
      rules: {
        connectorName: [{ required: true, message: '请输入连接器名称', trigger: 'blur' }],
        connectorType: [{ required: true, message: '请选择连接器类型', trigger: 'change' }],
        dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
        host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
        databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }]
        // 密码校验已移除，编辑时密码为 ******，不需要必填校验
      }
    }
  },
  computed: {
    // 判断当前表格数据中是否有数据库类型连接器
    hasDatabase() {
      return this.tableData.some(item => item.connectorType === 'DATABASE')
    },
    // 判断当前表格数据中是否有API类型连接器
    hasAPI() {
      return this.tableData.some(item => item.connectorType === 'API')
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // 构建API完整地址
    buildApiUrl(row) {
      if (row.connectorType !== 'API') return ''
      const protocol = row.port === 443 ? 'https' : 'http'
      const host = row.host || 'localhost'
      const port = row.port || 80
      const baseUri = row.url || ''
      return `${protocol}://${host}:${port}${baseUri}`
    },
    
    loadData() {
      this.$axios.get('/v1/connector/page', { params: this.queryParams }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total
      })
    },
    handleQuery() {
      this.queryParams.current = 1
      this.loadData()
    },
    handleReset() {
      this.queryParams = { current: 1, size: 10, connectorName: '', connectorType: '' }
      this.loadData()
    },
    handleSizeChange(val) {
      this.queryParams.size = val
      this.loadData()
    },
    handleCurrentChange(val) {
      this.queryParams.current = val
      this.loadData()
    },
    handleAdd() {
      this.dialogTitle = '新增连接器'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑连接器'
      this.$axios.get(`/v1/connector/${row.id}`).then(res => {
        this.form = res.data
        // 解析API认证配置
        if (this.form.connectorType === 'API' && this.form.authConfig) {
          try {
            const authConfig = JSON.parse(this.form.authConfig)
            if (this.form.authType === 'BASIC') {
              this.form.authUsername = authConfig.username || ''
              this.form.authPassword = authConfig.password || ''
            } else if (this.form.authType === 'BEARER') {
              this.form.authToken = authConfig.token || ''
            } else if (this.form.authType === 'API_KEY') {
              this.form.authApiKey = authConfig.apiKey || ''
              this.form.authHeaderName = authConfig.headerName || 'X-API-Key'
            }
          } catch (e) {
            console.error('解析authConfig失败', e)
          }
        }
        // 解析extraConfig中的headers和params
        if (this.form.connectorType === 'API' && this.form.extraConfig) {
          try {
            const extraConfig = JSON.parse(this.form.extraConfig)
            this.form.customHeaders = extraConfig.headers ? Object.keys(extraConfig.headers).map(key => ({
              key: key,
              value: extraConfig.headers[key]
            })) : []
            this.form.customParams = extraConfig.params ? Object.keys(extraConfig.params).map(key => ({
              key: key,
              value: extraConfig.params[key]
            })) : []
          } catch (e) {
            console.error('解析extraConfig失败', e)
            this.form.customHeaders = []
            this.form.customParams = []
          }
        }
        this.dialogVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除该连接器吗？', '提示', { type: 'warning' }).then(() => {
        this.$axios.delete(`/v1/connector/${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    },
    handleTest(row) {
      this.$set(this.testLoadingMap, row.id, true)
      this.$axios.post(`/v1/connector/test/${row.id}`).then(() => {
        this.$message.success('连接测试成功')
      }).catch(() => {
        this.$message.error('连接测试失败')
      }).finally(() => {
        this.$set(this.testLoadingMap, row.id, false)
      })
    },
    handleTypeChange() {
      if (this.form.connectorType === 'DATABASE') {
        this.form.port = 3306
      } else if (this.form.connectorType === 'API') {
        this.form.authType = 'NONE'
      }
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          // 仅数据库类型需要检查密码
          if (this.form.connectorType === 'DATABASE' && !this.form.id && !this.form.password) {
            this.$message.error('请输入密码')
            return
          }
          
          // API类型需要组装authConfig和extraConfig
          if (this.form.connectorType === 'API') {
            const authConfig = {}
            if (this.form.authType === 'BASIC') {
              authConfig.username = this.form.authUsername || ''
              authConfig.password = this.form.authPassword || ''
            } else if (this.form.authType === 'BEARER') {
              authConfig.token = this.form.authToken || ''
            } else if (this.form.authType === 'API_KEY') {
              authConfig.apiKey = this.form.authApiKey || ''
              authConfig.headerName = this.form.authHeaderName || 'X-API-Key'
            }
            this.form.authConfig = JSON.stringify(authConfig)
            
            // 组装headers和params到extraConfig
            const extraConfig = {
              headers: {},
              params: {}
            }
            if (this.form.customHeaders && this.form.customHeaders.length > 0) {
              this.form.customHeaders.forEach(h => {
                if (h.key && h.key.trim()) {
                  extraConfig.headers[h.key.trim()] = h.value || ''
                }
              })
            }
            if (this.form.customParams && this.form.customParams.length > 0) {
              this.form.customParams.forEach(p => {
                if (p.key && p.key.trim()) {
                  extraConfig.params[p.key.trim()] = p.value || ''
                }
              })
            }
            this.form.extraConfig = JSON.stringify(extraConfig)
          }
          
          const api = this.form.id ? this.$axios.put('/v1/connector', this.form) : this.$axios.post('/v1/connector', this.form)
          api.then(() => {
            this.$message.success(this.form.id ? '修改成功' : '新增成功')
            this.dialogVisible = false
            this.loadData()
          }).catch(err => {
            const errMsg = (err.response && err.response.data && err.response.data.message) || '操作失败'
            this.$message.error(errMsg)
          })
        }
      })
    },
    handleDialogClose() {
      this.$refs.form.resetFields()
      this.form = { 
        connectorName: '', 
        connectorType: 'DATABASE', 
        dbType: 'MYSQL', 
        host: '', 
        port: 3306, 
        databaseName: '', 
        username: '', 
        password: '', 
        url: '',
        authType: 'NONE',
        authConfig: '',
        customHeaders: [],
        customParams: [],
        // 重置测试请求字段
        testMethod: 'GET',
        testPath: '',
        testActiveTab: 'params',
        testParams: [],
        testBodyType: 'none',
        testFormData: [],
        testJsonBody: '',
        testHeaders: [],
        description: '', 
        status: 1 
      }
    },
    addHeader() {
      if (!this.form.customHeaders) {
        this.form.customHeaders = []
      }
      this.form.customHeaders.push({ key: '', value: '' })
    },
    removeHeader(index) {
      this.form.customHeaders.splice(index, 1)
    },
    addParam() {
      if (!this.form.customParams) {
        this.form.customParams = []
      }
      this.form.customParams.push({ key: '', value: '' })
    },
    removeParam(index) {
      this.form.customParams.splice(index, 1)
    },
    // 测试请求相关方法
    addTestParam() {
      if (!this.form.testParams) {
        this.form.testParams = []
      }
      this.form.testParams.push({ key: '', value: '', description: '', enabled: true })
    },
    removeTestParam(index) {
      this.form.testParams.splice(index, 1)
    },
    addFormDataParam() {
      if (!this.form.testFormData) {
        this.form.testFormData = []
      }
      this.form.testFormData.push({ key: '', value: '', enabled: true })
    },
    removeFormDataParam(index) {
      this.form.testFormData.splice(index, 1)
    },
    addTestHeader() {
      if (!this.form.testHeaders) {
        this.form.testHeaders = []
      }
      this.form.testHeaders.push({ key: '', value: '', enabled: true })
    },
    removeTestHeader(index) {
      this.form.testHeaders.splice(index, 1)
    },
    handleBodyTypeChange(newType) {
      // 根据Body类型自动设置Content-Type
      if (!this.form.testHeaders) {
        this.form.testHeaders = []
      }
      
      // 移除旧的Content-Type
      const contentTypeIndex = this.form.testHeaders.findIndex(h => h.key === 'Content-Type')
      if (contentTypeIndex > -1) {
        this.form.testHeaders.splice(contentTypeIndex, 1)
      }
      
      // 根据新类型添加Content-Type
      if (newType === 'json') {
        this.form.testHeaders.push({ 
          key: 'Content-Type', 
          value: 'application/json', 
          enabled: true 
        })
      } else if (newType === 'form-data') {
        this.form.testHeaders.push({ 
          key: 'Content-Type', 
          value: 'multipart/form-data', 
          enabled: true 
        })
      }
    },
    handleTestRequest() {
      if (!this.form.url) {
        this.$message.warning('请先输入API基地址')
        return
      }
      
      // 构建完整URL
      let url = this.form.url
      if (this.form.testPath) {
        if (!url.endsWith('/') && !this.form.testPath.startsWith('/')) {
          url += '/'
        }
        url += this.form.testPath
      }
      
      // 添加启用的Params
      const enabledParams = (this.form.testParams || []).filter(p => p.enabled && p.key)
      if (enabledParams.length > 0) {
        const queryString = enabledParams.map(p => `${encodeURIComponent(p.key)}=${encodeURIComponent(p.value || '')}`).join('&')
        url += (url.includes('?') ? '&' : '?') + queryString
      }
      
      // 构建 Headers
      const headers = {}
      // 认证Headers
      if (this.form.authType === 'BASIC' && this.form.authUsername) {
        const auth = btoa(`${this.form.authUsername}:${this.form.authPassword || ''}`)
        headers['Authorization'] = `Basic ${auth}`
      } else if (this.form.authType === 'BEARER' && this.form.authToken) {
        headers['Authorization'] = `Bearer ${this.form.authToken}`
      } else if (this.form.authType === 'API_KEY' && this.form.authApiKey) {
        const headerName = this.form.authHeaderName || 'X-API-Key'
        headers[headerName] = this.form.authApiKey
      }
      // 自定义Headers
      const enabledHeaders = (this.form.testHeaders || []).filter(h => h.enabled && h.key)
      enabledHeaders.forEach(h => {
        headers[h.key] = h.value || ''
      })
      
      // 构建 Body
      let body = null
      if (this.form.testMethod !== 'GET') {
        if (this.form.testBodyType === 'json' && this.form.testJsonBody) {
          try {
            body = JSON.parse(this.form.testJsonBody)
          } catch (e) {
            this.$message.error('JSON格式错误: ' + e.message)
            return
          }
        } else if (this.form.testBodyType === 'form-data') {
          const formData = {}
          const enabledFormData = (this.form.testFormData || []).filter(f => f.enabled && f.key)
          enabledFormData.forEach(f => {
            formData[f.key] = f.value || ''
          })
          body = formData
        }
      }
      
      // 发送请求
      const loading = this.$loading({
        lock: true,
        text: '发送请求中...',
        spinner: 'el-icon-loading'
      })
      
      const method = this.form.testMethod.toLowerCase()
      const config = { headers }
      
      let request
      if (method === 'get') {
        request = this.$axios.get(url, config)
      } else if (method === 'post') {
        request = this.$axios.post(url, body, config)
      } else if (method === 'put') {
        request = this.$axios.put(url, body, config)
      } else if (method === 'patch') {
        request = this.$axios.patch(url, body, config)
      } else if (method === 'delete') {
        request = this.$axios.delete(url, config)
      }
      
      request.then(res => {
        loading.close()
        this.$alert(
          `<pre style="max-height: 400px; overflow: auto">${JSON.stringify(res, null, 2)}</pre>`,
          '响应结果',
          {
            dangerouslyUseHTMLString: true,
            customClass: 'test-response-dialog'
          }
        )
      }).catch(err => {
        loading.close()
        const errMsg = (err.response && err.response.data) 
          ? JSON.stringify(err.response.data, null, 2) 
          : err.message
        this.$alert(
          `<pre style="max-height: 400px; overflow: auto; color: #F56C6C">${errMsg}</pre>`,
          '请求失败',
          {
            dangerouslyUseHTMLString: true,
            type: 'error'
          }
        )
      })
    }
  }
}
</script>

<style scoped>
.connector-container {
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

.form-tip {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-top: 4px;
}

/* 卡片视图样式 */
.card-view {
  padding: 0 var(--spacing-lg) var(--spacing-lg);
}

/* 卡片网格布局 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  align-items: start;
}

/* 卡片网格项 */
.card-grid-item {
  height: 100%;
}

/* 全新卡片设计 */
.connector-card {
  background: var(--bg-white);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.connector-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 16px;
  opacity: 0;
  transition: opacity 0.4s;
  pointer-events: none;
}

.connector-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 20px rgba(0, 0, 0, 0.12);
}

.connector-card:hover::after {
  opacity: 1;
}

/* 数据库类型卡片 */
.connector-card-database {
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(135deg, #10b981, #059669) border-box;
}

.connector-card-database::after {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(5, 150, 105, 0.05));
}

.connector-card-database:hover {
  box-shadow: 0 20px 40px rgba(16, 185, 129, 0.3);
}

/* API类型卡片 */
.connector-card-api {
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(135deg, #f59e0b, #d97706) border-box;
}

.connector-card-api::after {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.1), rgba(217, 119, 6, 0.05));
}

.connector-card-api:hover {
  box-shadow: 0 20px 40px rgba(245, 158, 11, 0.3);
}

/* 文件类型卡片 */
.connector-card-file {
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(135deg, #6366f1, #4f46e5) border-box;
}

.connector-card-file::after {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(79, 70, 229, 0.05));
}

.connector-card-file:hover {
  box-shadow: 0 20px 40px rgba(99, 102, 241, 0.3);
}

/* 卡片头部 */
.card-header-new {
  padding: 20px;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.02), transparent);
  position: relative;
  flex-shrink: 0;
}

.card-header-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.logo-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-white);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.connector-card:hover .logo-wrapper {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.15);
}

.db-logo {
  width: 40px;
  height: 40px;
  object-fit: contain;
}

.card-icon {
  font-size: 32px;
}

.type-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.badge-database {
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
}

.badge-api {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: white;
}

.badge-file {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: white;
}

.connector-card:hover .type-badge {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 12px;
}

.status-tag {
  flex-shrink: 0;
}

/* 卡片主体 */
.card-body-new {
  flex: 1;
  padding: 0 20px 16px;
  display: flex;
  flex-direction: column;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-row {
  display: flex;
  align-items: center;
  padding: 10px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
  transition: all 0.2s;
}

.info-row:hover {
  background: rgba(59, 130, 246, 0.08);
  transform: translateX(4px);
}

.info-row.full-width {
  width: 100%;
}

.info-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  flex-shrink: 0;
  background: var(--bg-white);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.info-icon i {
  font-size: 16px;
  color: var(--primary-color);
}

.info-content {
  flex: 1;
  min-width: 0;
}

.info-label {
  font-size: 11px;
  color: var(--text-secondary);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.url-text {
  cursor: pointer;
  color: var(--primary-color);
}

/* 卡片底部 */
.card-footer-new {
  padding: 16px 20px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.03), transparent);
  display: flex;
  gap: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

.card-footer-new .el-button {
  flex: 1;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.3s;
}

.card-footer-new .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .card-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  }
}

@media (max-width: 768px) {
  .card-view {
    padding: 0 var(--spacing-sm) var(--spacing-sm);
  }
  
  .card-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .logo-wrapper {
    width: 48px;
    height: 48px;
  }
  
  .db-logo {
    width: 32px;
    height: 32px;
  }
  
  .card-icon {
    font-size: 28px;
  }
  
  .card-title h3 {
    font-size: 16px;
  }
  
  .info-row {
    padding: 8px;
  }
  
  .info-icon {
    width: 28px;
    height: 28px;
  }
}

/* 测试响应对话框样式 */
::v-deep .test-response-dialog {
  width: 60%;
}

::v-deep .test-response-dialog .el-message-box__message {
  max-height: 500px;
  overflow: auto;
}
</style>
