<template>
  <div class="dict-mapping-container">
    <el-card>
      <!-- 搜索区 -->
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="映射名称">
          <el-input v-model="searchForm.mappingName" placeholder="请输入映射名称" clearable style="width: 200px"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button type="success" icon="el-icon-plus" @click="handleAdd">新增映射</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="mappingName" label="映射名称" min-width="150"></el-table-column>
        <el-table-column prop="mappingCode" label="映射编码" min-width="150"></el-table-column>
        <el-table-column label="映射类型" width="120">
          <template slot-scope="scope">
            <el-tag size="small">
              {{ getMappingTypeLabel(scope.row.mappingType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDictId" label="源数据源ID" width="120"></el-table-column>
        <el-table-column label="目标数据源ID" width="120">
          <template slot-scope="scope">
            <span v-if="scope.row.mappingType === 'SOURCE_TO_CUSTOM' || scope.row.mappingType === 'CUSTOM_TO_CUSTOM'" style="color: #909399">-</span>
            <span v-else>{{ scope.row.targetDictId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="defaultValue" label="默认值" width="100"></el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-setting" @click="handleViewDetail(scope.row)">配置类型映射</el-button>
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
        <el-form-item label="映射名称" prop="mappingName">
          <el-input v-model="form.mappingName" placeholder="如：系统A民族→系统B民族"></el-input>
        </el-form-item>
        <el-form-item label="映射编码" prop="mappingCode">
          <el-input v-model="form.mappingCode" placeholder="如：nation_a_to_b"></el-input>
        </el-form-item>
        <el-form-item label="映射类型" prop="mappingType">
          <el-radio-group v-model="form.mappingType" @change="handleMappingTypeChange">
            <el-radio label="SOURCE_TO_SOURCE">源数据源 → 目标数据源</el-radio>
            <el-radio label="SOURCE_TO_CUSTOM">源数据源 → 自定义目标</el-radio>
            <el-radio label="CUSTOM_TO_SOURCE">自定义源 → 目标数据源</el-radio>
            <el-radio label="CUSTOM_TO_CUSTOM">自定义源 → 自定义目标</el-radio>
          </el-radio-group>
          <div style="margin-top: 5px; color: #909399; font-size: 12px">
            {{ getMappingTypeHelp(form.mappingType) }}
          </div>
        </el-form-item>
        <el-form-item label="源数据源" prop="sourceDictId" v-if="form.mappingType === 'SOURCE_TO_SOURCE' || form.mappingType === 'SOURCE_TO_CUSTOM'">
          <el-select v-model="form.sourceDictId" placeholder="请选择源数据源" style="width: 100%">
            <el-option v-for="item in dictSourceList" :key="item.id" :label="item.sourceName" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="目标数据源" prop="targetDictId" v-if="form.mappingType === 'SOURCE_TO_SOURCE' || form.mappingType === 'CUSTOM_TO_SOURCE'">
          <el-select v-model="form.targetDictId" placeholder="请选择目标数据源" style="width: 100%">
            <el-option v-for="item in dictSourceList" :key="item.id" :label="item.sourceName" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="form.defaultValue" placeholder="未匹配时的默认值"></el-input>
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

    <!-- 配置映射项对话框 -->
    <el-dialog title="配置字典映射项" :visible.sync="itemsDialogVisible" width="1200px" @close="handleItemsDialogClose" :close-on-click-modal="false">
      <!-- 映射基本信息 -->
      <el-descriptions :column="3" border size="small" style="margin-bottom: 20px">
        <el-descriptions-item label="映射名称">{{ currentMapping.mappingName }}</el-descriptions-item>
        <el-descriptions-item label="映射类型">
          <el-tag size="small">
            {{ getMappingTypeLabel(currentMapping.mappingType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="默认值">{{ currentMapping.defaultValue || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 已配置的类型映射 -->
      <div v-if="typeGroups.length > 0" style="margin-bottom: 20px">
        <div style="margin-bottom: 10px; font-weight: bold">
          已配置的类型映射 <el-tag size="mini" style="margin-left: 10px">{{ typeGroups.length }} 个类型组</el-tag>
        </div>
        <el-collapse v-model="activeTypeGroups" accordion>
          <el-collapse-item 
            v-for="(group, index) in typeGroups" 
            :key="index" 
            :name="group.key">
            <template slot="title">
              <div style="width: 100%; display: flex; justify-content: space-between; align-items: center; padding-right: 20px">
                <div>
                  <i class="el-icon-folder-opened"></i>
                  <span style="margin-left: 5px; font-weight: bold">{{ getTypeLabel(group.sourceTypeValue, sourceTypeList) }}</span>
                  <span v-if="currentMapping.mappingType === 'SOURCE_TO_SOURCE'" style="color: #909399"> → {{ getTypeLabel(group.targetTypeValue, targetTypeList) }}</span>
                  <el-tag size="mini" style="margin-left: 10px">{{ group.items.length }} 项</el-tag>
                </div>
                <div @click.stop>
                  <el-button type="text" icon="el-icon-edit" size="small" @click="handleEditTypeGroup(group)">编辑</el-button>
                  <el-button type="text" icon="el-icon-delete" size="small" style="color: #F56C6C" @click="handleDeleteTypeGroup(group)">删除</el-button>
                </div>
              </div>
            </template>
            <el-table :data="group.items" border stripe size="small" max-height="250">
              <el-table-column type="index" label="序号" width="60"></el-table-column>
              <el-table-column prop="sourceKey" label="源字典编码" width="120"></el-table-column>
              <el-table-column prop="sourceLabel" label="源字典名称" width="120"></el-table-column>
              <el-table-column label="" width="40" align="center">
                <template><i class="el-icon-right"></i></template>
              </el-table-column>
              <el-table-column prop="targetKey" label="目标字典编码" width="120"></el-table-column>
              <el-table-column prop="targetLabel" label="目标字典名称" width="120"></el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>

      <el-divider></el-divider>

      <!-- 当前编辑的类型映射 -->
      <!-- 类型选择区 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="12" v-if="currentMappingType === 'SOURCE_TO_SOURCE' || currentMappingType === 'SOURCE_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #409EFF">
              <i class="el-icon-upload2"></i> 源字典类型
            </div>
            <el-select 
              v-model="sourceTypeValue" 
              placeholder="请选择源字典类型" 
              style="width: 100%"
              filterable
              clearable
              @change="handleSourceTypeChange"
              :loading="sourceTypeLoading">
              <el-option 
                v-for="item in sourceTypeList" 
                :key="item.value" 
                :label="item.label" 
                :value="item.value">
              </el-option>
            </el-select>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              已加载 {{ sourceItems.length }} 个字典项
            </div>
          </el-card>
        </el-col>
        <!-- 源自定义项：当源为CUSTOM且目标为数据源时展示 -->
        <el-col :span="12" v-if="false">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #409EFF">
              源自定义项
            </div>
            <div style="margin-bottom: 8px">
              <el-button size="mini" type="primary" @click="addSourceCustomItem">+ 添加源项</el-button>
            </div>
            <el-table :data="sourceCustomItems" size="mini" border max-height="200">
              <el-table-column type="index" label="序号" width="60"/>
              <el-table-column label="源编码" width="160">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.key" size="mini" placeholder="如：A001"/>
                </template>
              </el-table-column>
              <el-table-column label="源名称" width="200">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.value" size="mini" placeholder="如：管理员"/>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="mini" @click="removeSourceCustomItem(scope.$index)" style="color:#F56C6C">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12" v-if="currentMappingType === 'SOURCE_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_SOURCE'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #67C23A">
              <i class="el-icon-download"></i> 目标字典类型
            </div>
            <el-select 
              v-model="targetTypeValue" 
              placeholder="请选择目标字典类型" 
              style="width: 100%"
              filterable
              clearable
              @change="handleTargetTypeChange"
              :loading="targetTypeLoading">
              <el-option 
                v-for="item in targetTypeList" 
                :key="item.value" 
                :label="item.label" 
                :value="item.value">
              </el-option>
            </el-select>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              已加载 {{ targetItems.length }} 个字典项
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 自定义类型值输入（CUSTOM_TO_CUSTOM / SOURCE_TO_CUSTOM 部分） -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <!-- 源类型值（自定义）：当源为CUSTOM时 -->
        <el-col :span="12" v-if="currentMappingType === 'CUSTOM_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #409EFF">源字典类型（自定义）</div>
            <el-input v-model="sourceTypeValue" placeholder="请输入源字典类型编码" @input="loadTypeMappingItems"/>
            <el-input v-model="sourceTypeLabel" placeholder="请输入源字典类型名称" style="margin-top:8px"/>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">当前类型：{{ sourceTypeLabel || '-' }}（编码：{{ sourceTypeValue || '-' }}）</div>
          </el-card>
        </el-col>
        <!-- 目标类型值（自定义）：当目标为CUSTOM时 -->
        <el-col :span="12" v-if="currentMappingType === 'SOURCE_TO_CUSTOM' || currentMappingType === 'CUSTOM_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #67C23A">目标字典类型（自定义）</div>
            <el-input v-model="targetTypeValue" placeholder="请输入目标字典类型编码" @input="loadTypeMappingItems"/>
            <el-input v-model="targetTypeLabel" placeholder="请输入目标字典类型名称" style="margin-top:8px"/>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">当前类型：{{ targetTypeLabel || '-' }}（编码：{{ targetTypeValue || '-' }}）</div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 操作按钮 -->
      <div style="margin-bottom: 15px">
        <el-button type="primary" icon="el-icon-plus" size="small" @click="handleAddItem" :disabled="(!sourceTypeValue || !targetTypeValue) || ((currentMappingType === 'CUSTOM_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !sourceTypeLabel) || ((currentMappingType === 'SOURCE_TO_CUSTOM' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !targetTypeLabel)">添加映射项</el-button>
        <el-button 
          type="warning" 
          icon="el-icon-magic-stick" 
          size="small" 
          @click="handleAutoMatch" 
          :loading="autoMatchLoading" 
          :disabled="((!sourceTypeValue || !targetTypeValue) || (currentMappingType === 'SOURCE_TO_SOURCE' && !targetTypeValue)) || ((currentMappingType === 'CUSTOM_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !sourceTypeLabel) || ((currentMappingType === 'SOURCE_TO_CUSTOM' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !targetTypeLabel)">
          {{ currentMappingType === 'SOURCE_TO_SOURCE' ? '智能匹配' : '智能填充' }}
        </el-button>
        <el-button type="success" icon="el-icon-check" size="small" @click="handleSaveItems" :loading="saveItemsLoading" :disabled="(!sourceTypeValue || !targetTypeValue) || ((currentMappingType === 'CUSTOM_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !sourceTypeLabel) || ((currentMappingType === 'SOURCE_TO_CUSTOM' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !targetTypeLabel)">保存</el-button>
        <span style="margin-left: 10px; color: #E6A23C; font-size: 12px" v-if="!sourceTypeValue || !targetTypeValue || ((currentMappingType === 'CUSTOM_TO_SOURCE' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !sourceTypeLabel) || ((currentMappingType === 'SOURCE_TO_CUSTOM' || currentMappingType === 'CUSTOM_TO_CUSTOM') && !targetTypeLabel)">
          <i class="el-icon-warning"></i> 请先填写源/目标字典类型编码与名称
        </span>
        <span style="margin-left: 10px; color: #67C23A; font-size: 12px" v-else>
          <i class="el-icon-info"></i> 当前配置类型：<strong>{{ sourceTypeLabel || sourceTypeValue }}</strong>
          <template v-if="targetTypeValue"> → <strong>{{ targetTypeLabel || targetTypeValue }}</strong></template>
        </span>
      </div>
      
      <!-- 映射项表格 -->
      <el-table :data="mappingItems" border stripe max-height="400">
        <el-table-column type="index" label="序号" width="60"></el-table-column>
        <el-table-column label="源字典编码" width="150">
          <template slot-scope="scope">
            <el-input v-model="scope.row.sourceKey" size="small" placeholder="请输入编码"></el-input>
          </template>
        </el-table-column>
        <el-table-column label="源字典名称" width="150">
          <template slot-scope="scope">
            <el-input v-model="scope.row.sourceLabel" size="small" placeholder="请输入名称"></el-input>
          </template>
        </el-table-column>
        <el-table-column label="" width="50" align="center">
          <template>
            <i class="el-icon-right"></i>
          </template>
        </el-table-column>
        <el-table-column label="目标字典编码" width="150">
          <template slot-scope="scope">
            <el-input v-model="scope.row.targetKey" size="small" placeholder="请输入编码"></el-input>
          </template>
        </el-table-column>
        <el-table-column label="目标字典名称" width="150">
          <template slot-scope="scope">
            <el-input v-model="scope.row.targetLabel" size="small" placeholder="请输入名称"></el-input>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-delete" @click="handleDeleteItem(scope.$index)" style="color: #F56C6C">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DictMapping',
  data() {
    return {
      loading: false,
      submitLoading: false,
      autoMatchLoading: false,
      saveItemsLoading: false,
      searchForm: {
        mappingName: ''
      },
      tableData: [],
      pagination: {
        current: 1,
        size: 10,
        total: 0
      },
      dialogVisible: false,
      dialogTitle: '新增字典映射',
      form: {
        mappingName: '',
        mappingCode: '',
        mappingType: 'SOURCE_TO_SOURCE',
        sourceDictId: null,
        targetDictId: null,
        defaultValue: '',
        description: '',
        status: 1
      },
      rules: {
        mappingName: [{ required: true, message: '请输入映射名称', trigger: 'blur' }],
        mappingCode: [{ required: true, message: '请输入映射编码', trigger: 'blur' }],
        mappingType: [{ required: true, message: '请选择映射类型', trigger: 'change' }],
        sourceDictId: [{ required: true, message: '请选择源数据源', trigger: 'change' }]
      },
      dictSourceList: [],
      itemsDialogVisible: false,
      currentMappingId: null,
      currentMapping: {},
      currentMappingType: 'SOURCE_TO_SOURCE',
      currentSourceDictId: null,
      currentTargetDictId: null,
      mappingItems: [],
      
      // 类型分组
      typeGroups: [],
      activeTypeGroups: '',
      
      // 源字典类型和数据
      sourceTypeValue: '',
      sourceTypeList: [],
      sourceTypeLoading: false,
      sourceItems: [],
      // 目标字典类型和数据
      targetTypeValue: '',
      targetTypeList: [],
      targetTypeLoading: false,
      targetItems: [],
      sourceCustomItems: [],
      sourceTypeLabel: '',
      targetTypeLabel: '',
      typeLabelsMap: { source: {}, target: {} }
    }
  },
  created() {
    this.loadData()
    this.loadDictSources()
  },
  methods: {
    loadData() {
      this.loading = true
      this.$axios.get('/v1/dict-mapping/page', {
        params: {
          current: this.pagination.current,
          size: this.pagination.size,
          mappingName: this.searchForm.mappingName
        }
      }).then(res => {
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      }).finally(() => {
        this.loading = false
      })
    },
    loadDictSources() {
      this.$axios.get('/v1/dict-source/list').then(res => {
        this.dictSourceList = res.data
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
      this.dialogTitle = '新增字典映射'
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑字典映射'
      this.form = { ...row }
      this.dialogVisible = true
    },
    handleDialogClose() {
      this.$refs.form.resetFields()
      this.form = {
        mappingName: '',
        mappingCode: '',
        mappingType: 'SOURCE_TO_SOURCE',
        sourceDictId: null,
        targetDictId: null,
        defaultValue: '',
        description: '',
        status: 1
      }
    },
    handleMappingTypeChange(val) {
      if (val === 'SOURCE_TO_CUSTOM') {
        this.form.targetDictId = null
      } else if (val === 'CUSTOM_TO_SOURCE') {
        this.form.sourceDictId = null
      } else if (val === 'CUSTOM_TO_CUSTOM') {
        this.form.sourceDictId = null
        this.form.targetDictId = null
      }
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          // 验证源/目标数据源选择
          if ((this.form.mappingType === 'SOURCE_TO_SOURCE' || this.form.mappingType === 'SOURCE_TO_CUSTOM') && !this.form.sourceDictId) {
            this.$message.warning('请选择源数据源')
            return
          }
          if ((this.form.mappingType === 'SOURCE_TO_SOURCE' || this.form.mappingType === 'CUSTOM_TO_SOURCE') && !this.form.targetDictId) {
            this.$message.warning('请选择目标数据源')
            return
          }
          
          this.submitLoading = true
          const request = this.form.id
            ? this.$axios.put('/v1/dict-mapping', this.form)
            : this.$axios.post('/v1/dict-mapping', this.form)
          
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
      this.$confirm('确定删除该字典映射吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.$axios.delete(`/v1/dict-mapping/${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    },
    handleViewDetail(row) {
      this.$router.push({ name: 'DictMappingDetail', params: { id: row.id } })
    },
    handleConfigItems(row) {
      this.currentMappingId = row.id
      this.currentMapping = { ...row }
      this.currentMappingType = row.mappingType
      this.currentSourceDictId = row.sourceDictId
      this.currentTargetDictId = row.targetDictId
      
      // 解析typeLabels
      this.typeLabelsMap = { source: {}, target: {} }
      if (row.typeLabels) {
        try {
          this.typeLabelsMap = JSON.parse(row.typeLabels) || { source: {}, target: {} }
        } catch (e) {
          console.error('解析typeLabels失败:', e)
        }
      }
      
      if (row.sourceDictId) {
        this.loadSourceTypes(row.sourceDictId)
      }
      
      if ((row.mappingType === 'SOURCE_TO_SOURCE' || row.mappingType === 'CUSTOM_TO_SOURCE') && row.targetDictId) {
        this.loadTargetTypes(row.targetDictId)
      }
      
      // 加载所有类型分组
      this.loadAllTypeGroups()
      
      this.itemsDialogVisible = true
    },
    
    // 加载所有类型分组
    loadAllTypeGroups() {
      this.$axios.get(`/v1/dict-mapping/${this.currentMappingId}/items`).then(res => {
        const items = res.data || []
        
        // 按类型分组
        const groups = {}
        items.forEach(item => {
          const key = `${item.sourceTypeValue || 'null'}_${item.targetTypeValue || 'null'}`
          if (!groups[key]) {
            groups[key] = {
              key: key,
              sourceTypeValue: item.sourceTypeValue,
              targetTypeValue: item.targetTypeValue,
              items: []
            }
          }
          groups[key].items.push(item)
        })
        
        this.typeGroups = Object.values(groups)
      })
    },
    
    // 获取类型标签
    getTypeLabel(typeValue, typeList) {
      if (!typeValue || !typeList || typeList.length === 0) {
        return typeValue || '-'
      }
      const typeItem = typeList.find(t => String(t.value) === String(typeValue))
      return typeItem ? typeItem.label : typeValue
    },
    getMappingTypeLabel(type) {
      switch (type) {
        case 'SOURCE_TO_SOURCE': return '源数据源 → 目标数据源'
        case 'SOURCE_TO_CUSTOM': return '源数据源 → 自定义目标'
        case 'CUSTOM_TO_SOURCE': return '自定义源 → 目标数据源'
        case 'CUSTOM_TO_CUSTOM': return '自定义源 → 自定义目标'
        default: return type
      }
    },
    getMappingTypeHelp(type) {
      switch (type) {
        case 'SOURCE_TO_SOURCE': return '从数据源读取源/目标字典项'
        case 'SOURCE_TO_CUSTOM': return '源数据源 + 手动配置目标字典项'
        case 'CUSTOM_TO_SOURCE': return '自定义源字典项 + 目标端从数据源读取'
        case 'CUSTOM_TO_CUSTOM': return '两端均自定义字典项（支持批量导入）'
        default: return ''
      }
    },
    
    // 编辑类型分组
    handleEditTypeGroup(group) {
      this.sourceTypeValue = group.sourceTypeValue
      this.targetTypeValue = group.targetTypeValue
      
      // 自定义源场景：从typeLabelsMap中回填类型名称
      if (this.currentMappingType === 'CUSTOM_TO_SOURCE' || this.currentMappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.typeLabelsMap && this.typeLabelsMap.source && this.sourceTypeValue) {
          this.sourceTypeLabel = this.typeLabelsMap.source[this.sourceTypeValue] || ''
        } else {
          this.sourceTypeLabel = ''
        }
      }
      
      // 自定义目标场景：从typeLabelsMap中回填类型名称
      if (this.currentMappingType === 'SOURCE_TO_CUSTOM' || this.currentMappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.typeLabelsMap && this.typeLabelsMap.target && this.targetTypeValue) {
          this.targetTypeLabel = this.typeLabelsMap.target[this.targetTypeValue] || ''
        } else {
          this.targetTypeLabel = ''
        }
      }
      
      // 加载该类型的数据
      if (this.sourceTypeValue) {
        this.handleSourceTypeChange(this.sourceTypeValue)
      }
      if (this.targetTypeValue) {
        this.handleTargetTypeChange(this.targetTypeValue)
      }
    },
    
    // 删除类型分组
    handleDeleteTypeGroup(group) {
      this.$confirm(`确定删除类型映射“${this.getTypeLabel(group.sourceTypeValue, this.sourceTypeList)}”吗？这将删除该类型下的所有映射项。`, '提示', {
        type: 'warning'
      }).then(() => {
        const params = {
          sourceTypeValue: group.sourceTypeValue,
          targetTypeValue: group.targetTypeValue || ''
        }
        
        this.$axios.post(`/v1/dict-mapping/${this.currentMappingId}/items`, [], { params }).then(() => {
          this.$message.success('删除成功')
          this.loadAllTypeGroups()
        })
      })
    },
    loadMappingItems(mappingId) {
      // 加载映射项（按当前选中的类型过滤）
      const params = {}
      if (this.sourceTypeValue) {
        params.sourceTypeValue = this.sourceTypeValue
      }
      if (this.targetTypeValue) {
        params.targetTypeValue = this.targetTypeValue
      }
      
      this.$axios.get(`/v1/dict-mapping/${mappingId}/items`, { params }).then(res => {
        this.mappingItems = res.data || []
      })
    },
    handleItemsDialogClose() {
      this.currentMappingId = null
      this.currentMapping = {}
      this.currentMappingType = 'SOURCE_TO_SOURCE'
      this.currentSourceDictId = null
      this.currentTargetDictId = null
      this.mappingItems = []
      this.typeGroups = []
      this.activeTypeGroups = ''
      this.sourceTypeValue = ''
      this.sourceTypeList = []
      this.sourceItems = []
      this.targetTypeValue = ''
      this.targetTypeList = []
      this.targetItems = []
    },
    handleAddItem() {
      this.mappingItems.push({
        sourceKey: '',
        sourceLabel: '',
        targetKey: '',
        targetLabel: ''
      })
    },
    handleDeleteItem(index) {
      this.mappingItems.splice(index, 1)
    },
    addSourceCustomItem() {
      this.sourceCustomItems.push({ key: '', value: '' })
    },
    removeSourceCustomItem(index) {
      this.sourceCustomItems.splice(index, 1)
    },
    handleAutoMatch() {
      // 根据映射类型进行不同处理
      if (this.currentMappingType === 'SOURCE_TO_SOURCE') {
        // 必须选择源/目标类型
        if (!this.sourceTypeValue) {
          this.$message.warning('请先选择源字典类型')
          return
        }
        if (!this.targetTypeValue) {
          this.$message.warning('请先选择目标字典类型')
          return
        }
        // 后端智能匹配
        this.autoMatchLoading = true
        this.$axios.post(`/v1/dict-mapping/${this.currentMappingId}/auto-match`, null, {
          params: {
            sourceTypeValue: this.sourceTypeValue,
            targetTypeValue: this.targetTypeValue
          }
        }).then(res => {
          this.mappingItems = res.data || []
          this.$message.success(`智能匹配完成！匹配到 ${this.mappingItems.length} 个项`)
        }).finally(() => {
          this.autoMatchLoading = false
        })
      } else if (this.currentMappingType === 'CUSTOM_TO_SOURCE') {
        if (!this.targetTypeValue) {
          this.$message.warning('请先选择目标字典类型')
          return
        }
        if (!this.mappingItems || this.mappingItems.length === 0) {
          this.$message.warning('请先添加映射项')
          return
        }
        this.autoMatchLoading = true
        if (!this.targetItems || this.targetItems.length === 0) {
          this.$message.warning('目标字典类型下没有数据')
          this.autoMatchLoading = false
          return
        }
        const targetKeyMap = new Map(this.targetItems.map(i => [String(i.key || '').toLowerCase().trim(), i]))
        const targetLabelMap = new Map(this.targetItems.map(i => [String(i.value || '').toLowerCase().trim(), i]))
        this.mappingItems = this.mappingItems.map(src => {
          const k = String(src.sourceKey || '').toLowerCase().trim()
          const v = String(src.sourceLabel || '').toLowerCase().trim()
          const matched = targetKeyMap.get(k) || targetLabelMap.get(v)
          return {
            ...src,
            targetKey: matched ? matched.key : '',
            targetLabel: matched ? matched.value : ''
          }
        })
        const matchedCount = this.mappingItems.filter(m => m.targetKey).length
        this.$message.success(`智能匹配完成！匹配到 ${matchedCount} 项，未匹配项请手动填写`)
        this.autoMatchLoading = false
      } else {
        // SOURCE_TO_CUSTOM 或 CUSTOM_TO_CUSTOM：智能填充（目标留空）
        const srcList = this.currentMappingType === 'SOURCE_TO_CUSTOM' ? this.sourceItems : this.sourceCustomItems
        if (!srcList || srcList.length === 0) {
          this.$message.warning('源数据为空，请先选择类型或添加自定义项')
          return
        }
        this.mappingItems = srcList.map(item => ({
          mappingId: this.currentMappingId,
          sourceKey: item.key || '',
          sourceLabel: item.value || '',
          targetKey: '',
          targetLabel: ''
        }))
        this.$message.success(`智能填充完成！已生成 ${this.mappingItems.length} 条源项，请手动填写目标值`)
      }
    },
    handleSaveItems() {
      // 验证必须选择源类型
      if (!this.sourceTypeValue) {
        this.$message.warning('请先选择源字典类型')
        return
      }
      
      // 如果是数据源映射，验证必须选择目标类型
      if (this.currentMappingType === 'SOURCE_TO_SOURCE' && !this.targetTypeValue) {
        this.$message.warning('请先选择目标字典类型')
        return
      }
      
      this.saveItemsLoading = true
      
      // 更新typeLabelsMap：如果是自定义类型，保存类型名称
      if (this.currentMappingType === 'CUSTOM_TO_SOURCE' || this.currentMappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.sourceTypeValue && this.sourceTypeLabel) {
          this.typeLabelsMap.source[this.sourceTypeValue] = this.sourceTypeLabel
        }
      }
      if (this.currentMappingType === 'SOURCE_TO_CUSTOM' || this.currentMappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.targetTypeValue && this.targetTypeLabel) {
          this.typeLabelsMap.target[this.targetTypeValue] = this.targetTypeLabel
        }
      }
      
      // 构建请求参数
      const params = {
        sourceTypeValue: this.sourceTypeValue,
        targetTypeValue: this.targetTypeValue || '',
        sourceTypeLabel: this.sourceTypeLabel || '',
        targetTypeLabel: this.targetTypeLabel || ''
      }
      
      this.$axios.post(`/v1/dict-mapping/${this.currentMappingId}/items`, this.mappingItems, { params }).then(() => {
        this.$message.success(`保存成功！已保存 ${this.mappingItems.length} 条映射项（源类型：${this.sourceTypeValue}）`)
        // 不关闭对话框，用户可以继续配置其他类型
        // 重新加载类型分组显示
        this.loadAllTypeGroups()
      }).finally(() => {
        this.saveItemsLoading = false
      })
    },
    // 加载源字典类型列表
    loadSourceTypes(sourceDictId) {
      // 防止传入 null/"null"/非数字
      if (sourceDictId === null || sourceDictId === undefined || String(sourceDictId).toLowerCase() === 'null' || isNaN(Number(sourceDictId))) {
        this.sourceTypeList = []
        return Promise.resolve()
      }
      this.sourceTypeLoading = true
      return this.$axios.get(`/v1/dict-source/${sourceDictId}/types`).then(res => {
        this.sourceTypeList = res.data || []
      }).finally(() => {
        this.sourceTypeLoading = false
      })
    },
    // 加载目标字典类型列表
    loadTargetTypes(targetDictId) {
      // 防止传入 null/"null"/非数字
      if (targetDictId === null || targetDictId === undefined || String(targetDictId).toLowerCase() === 'null' || isNaN(Number(targetDictId))) {
        this.targetTypeList = []
        return Promise.resolve()
      }
      this.targetTypeLoading = true
      return this.$axios.get(`/v1/dict-source/${targetDictId}/types`).then(res => {
        this.targetTypeList = res.data || []
      }).finally(() => {
        this.targetTypeLoading = false
      })
    },
    // 源字典类型变化
    handleSourceTypeChange(typeValue) {
      if (!typeValue) {
        this.sourceItems = []
        return
      }
      // 仅当 currentSourceDictId 为有效数字时才请求数据源接口
      if (this.currentSourceDictId !== null && this.currentSourceDictId !== undefined && String(this.currentSourceDictId).toLowerCase() !== 'null' && !isNaN(Number(this.currentSourceDictId))) {
        this.$axios.get(`/v1/dict-source/${this.currentSourceDictId}/items`, { params: { typeValue: typeValue } }).then(res => {
          this.sourceItems = res.data || []
        })
      } else {
        this.sourceItems = []
      }
      // 加载该类型已保存的映射项
      this.loadMappingItems(this.currentMappingId)
    },
    // 目标字典类型变化
    handleTargetTypeChange(typeValue) {
      if (!typeValue) {
        this.targetItems = []
        return
      }
      // 仅当 currentTargetDictId 为有效数字时才请求数据源接口
      if (this.currentTargetDictId !== null && this.currentTargetDictId !== undefined && String(this.currentTargetDictId).toLowerCase() !== 'null' && !isNaN(Number(this.currentTargetDictId))) {
        this.$axios.get(`/v1/dict-source/${this.currentTargetDictId}/items`, { params: { typeValue: typeValue } }).then(res => {
          this.targetItems = res.data || []
        })
      } else {
        this.targetItems = []
      }
      // 加载该类型已保存的映射项
      this.loadMappingItems(this.currentMappingId)
    },
    // 选择源字典项
    handleSourceItemSelect(index, sourceKey) {
      const sourceItem = this.sourceItems.find(item => item.key === sourceKey)
      if (sourceItem) {
        this.mappingItems[index].sourceLabel = sourceItem.value
      }
    },
    // 选择目标字典项
    handleTargetItemSelect(index, targetKey) {
      const targetItem = this.targetItems.find(item => item.key === targetKey)
      if (targetItem) {
        this.mappingItems[index].targetLabel = targetItem.value
      }
    }
  }
}
</script>

<style scoped>
.dict-mapping-container {
  padding: 20px;
}
</style>
