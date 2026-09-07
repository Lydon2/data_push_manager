<template>
  <div class="dict-mapping-detail-container">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/" style="margin-bottom: 20px">
      <el-breadcrumb-item :to="{ path: '/dict/mapping' }">字典映射管理</el-breadcrumb-item>
      <el-breadcrumb-item>{{ mappingInfo.mappingName || '映射详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-card>
      <!-- 映射基本信息 -->
      <div slot="header" style="display: flex; justify-content: space-between; align-items: center">
        <div>
          <span style="font-weight: bold; font-size: 16px">{{ mappingInfo.mappingName }}</span>
          <el-tag size="small" style="margin-left: 10px">
            {{ getMappingTypeLabel(mappingInfo.mappingType) }}
          </el-tag>
          <el-tag :type="mappingInfo.status === 1 ? 'success' : 'danger'" size="small" style="margin-left: 5px">
            {{ mappingInfo.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </div>
        <div>
          <el-button type="primary" icon="el-icon-plus" size="small" @click="handleAddType">新增类型映射</el-button>
          <el-button type="default" icon="el-icon-back" size="small" @click="$router.go(-1)">返回</el-button>
        </div>
      </div>

      <!-- 映射信息摘要 -->
      <el-descriptions :column="3" border style="margin-bottom: 20px">
        <el-descriptions-item label="映射编码">{{ mappingInfo.mappingCode }}</el-descriptions-item>
        <el-descriptions-item label="源数据源">{{ sourceDictName }}</el-descriptions-item>
        <el-descriptions-item label="目标数据源">
          <span v-if="mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM'" style="color: #909399">-（自定义）</span>
          <span v-else>{{ targetDictName }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="默认值">{{ mappingInfo.defaultValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ mappingInfo.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ mappingInfo.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="3">{{ mappingInfo.description || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 类型映射列表 -->
      <div style="margin-bottom: 10px">
        <span style="font-weight: bold; font-size: 14px">已配置的类型映射</span>
        <span style="margin-left: 10px; color: #909399; font-size: 12px">
          共 {{ typeGroups.length }} 个类型组，{{ totalItems }} 条映射项
        </span>
      </div>

      <!-- 类型分组展示 -->
      <el-collapse v-model="activeTypes" accordion>
        <el-collapse-item 
          v-for="(group, index) in typeGroups" 
          :key="index" 
          :name="group.key">
          <template slot="title">
            <div style="width: 100%; display: flex; justify-content: space-between; align-items: center; padding-right: 20px">
              <div>
                <i class="el-icon-folder-opened"></i>
                <span style="margin-left: 5px; font-weight: bold">{{ group.sourceTypeLabel || group.sourceTypeValue }}</span>
                <span v-if="group.targetTypeLabel || group.targetTypeValue" style="color: #909399"> → {{ group.targetTypeLabel || group.targetTypeValue }}</span>
                <el-tag size="mini" style="margin-left: 10px">{{ group.items.length }} 项</el-tag>
              </div>
              <div @click.stop>
                <el-button type="text" icon="el-icon-edit" size="small" @click="handleEditType(group)">编辑</el-button>
                <el-button type="text" icon="el-icon-delete" size="small" style="color: #F56C6C" @click="handleDeleteType(group)">删除</el-button>
              </div>
            </div>
          </template>
          
          <!-- 映射项表格 -->
          <el-table :data="group.items" border stripe size="small" max-height="300">
            <el-table-column type="index" label="序号" width="60"></el-table-column>
            <el-table-column prop="sourceKey" label="源字典编码" width="150"></el-table-column>
            <el-table-column prop="sourceLabel" label="源字典名称" width="150"></el-table-column>
            <el-table-column label="" width="50" align="center">
              <template>
                <i class="el-icon-right"></i>
              </template>
            </el-table-column>
            <el-table-column prop="targetKey" label="目标字典编码" width="150"></el-table-column>
            <el-table-column prop="targetLabel" label="目标字典名称" width="150"></el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="160"></el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <!-- 空状态 -->
      <el-empty v-if="typeGroups.length === 0" description="暂无类型映射配置，请点击新增类型映射开始配置"></el-empty>
    </el-card>

    <!-- 新增/编辑类型映射对话框 -->
    <el-dialog 
      :title="dialogTitle" 
      :visible.sync="dialogVisible" 
      width="1000px" 
      @close="handleDialogClose"
      :close-on-click-modal="false">
      
      <!-- 类型选择区 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="12" v-if="mappingInfo.mappingType === 'SOURCE_TO_SOURCE' || mappingInfo.mappingType === 'SOURCE_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #409EFF">
              <i class="el-icon-upload2"></i> 源字典类型
            </div>
            <el-select 
              v-model="currentSourceTypeValue" 
              placeholder="请选择源字典类型" 
              style="width: 100%"
              filterable
              clearable
              @change="handleSourceTypeChange"
              :loading="sourceTypeLoading">
              <el-option 
                v-for="(item, index) in sourceTypeList" 
                :key="'source-' + index + '-' + item.value" 
                :label="item.label" 
                :value="String(item.value)">
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
            <div slot="header" style="font-weight: bold; color: #409EFF">源自定义项</div>
            <div style="margin-bottom: 8px">
              <el-button size="mini" type="primary" @click="addSourceCustomItem">+ 添加源项</el-button>
              <el-button size="mini" type="warning" @click="openSourceImportDialog" style="margin-left:8px">批量导入</el-button>
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
        <!-- 源字典类型（自定义）：当源为CUSTOM时展示文本输入 -->
        <el-col :span="12" v-if="mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' || mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #409EFF">源字典类型（自定义）</div>
            <el-input v-model="currentSourceTypeValue" placeholder="请输入源字典类型编码" @input="handleCustomSourceTypeInput"/>
            <el-input v-model="customSourceTypeLabel" placeholder="请输入源字典类型名称" style="margin-top:8px" @input="handleCustomSourceTypeLabelInput"/>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              当前类型：{{ customSourceTypeLabel || '-' }}（编码：{{ currentSourceTypeValue || '-' }}）
            </div>
          </el-card>
        </el-col>
        <!-- 目标字典类型（自定义）：当目标为CUSTOM时展示文本输入 -->
        <el-col :span="12" v-if="mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #67C23A">目标字典类型（自定义）</div>
            <el-input v-model="currentTargetTypeValue" placeholder="请输入目标字典类型编码" @input="handleCustomTargetTypeInput"/>
            <el-input v-model="customTargetTypeLabel" placeholder="请输入目标字典类型名称" style="margin-top:8px" @input="handleCustomTargetTypeLabelInput"/>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              当前类型：{{ customTargetTypeLabel || '-' }}（编码：{{ currentTargetTypeValue || '-' }}）
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" v-if="mappingInfo.mappingType === 'SOURCE_TO_SOURCE' || mappingInfo.mappingType === 'CUSTOM_TO_SOURCE'">
          <el-card shadow="never" style="background-color: #f5f7fa">
            <div slot="header" style="font-weight: bold; color: #67C23A">
              <i class="el-icon-download"></i> 目标字典类型
            </div>
            <el-select 
              v-model="currentTargetTypeValue" 
              placeholder="请选择目标字典类型" 
              style="width: 100%"
              filterable
              clearable
              @change="handleTargetTypeChange"
              :loading="targetTypeLoading">
              <el-option 
                v-for="(item, index) in targetTypeList" 
                :key="'target-' + index + '-' + item.value" 
                :label="item.label" 
                :value="String(item.value)">
              </el-option>
            </el-select>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              已加载 {{ targetItems.length }} 个字典项
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 操作按钮 -->
      <div style="margin-bottom: 15px">
        <el-button type="primary" icon="el-icon-plus" size="small" @click="handleAddItem" :disabled="(mappingInfo.mappingType === 'SOURCE_TO_SOURCE' || mappingInfo.mappingType === 'SOURCE_TO_CUSTOM') && !currentSourceTypeValue">添加映射项</el-button>
        <el-button 
          type="warning" 
          icon="el-icon-magic-stick" 
          size="small" 
          @click="handleAutoMatch" 
          :loading="autoMatchLoading" 
          :disabled="(mappingInfo.mappingType === 'SOURCE_TO_SOURCE' && (!currentSourceTypeValue || !currentTargetTypeValue)) || (mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' && !currentSourceTypeValue) || (mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' && (!currentTargetTypeValue || editingItems.length === 0))">
          {{ getAutoButtonLabel(mappingInfo.mappingType) }}
        </el-button>
        <el-button 
          type="danger" 
          icon="el-icon-delete" 
          size="small" 
          @click="handleClearAllItems"
          :disabled="editingItems.length === 0">
          清空全部
        </el-button>
        <span style="margin-left: 10px; color: #67C23A; font-size: 12px" v-if="currentSourceTypeValue">
          <i class="el-icon-info"></i> 当前配置类型：<strong>{{ currentSourceTypeValue }}</strong>
          <template v-if="mappingInfo.mappingType === 'SOURCE_TO_SOURCE' && currentTargetTypeValue"> → <strong>{{ currentTargetTypeValue }}</strong></template>
        </span>
      </div>
      
      <!-- 映射项表格 -->
      <el-table :data="editingItems" border stripe max-height="400">
        <el-table-column label="序号" width="60" align="center">
          <template slot-scope="scope">
            {{ scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column label="源字典名称" width="180">
          <template slot-scope="scope">
            <el-autocomplete
              v-model="scope.row.sourceLabel"
              :fetch-suggestions="querySourceNames"
              size="small"
              placeholder="选择或输入名称"
              @select="handleSelectSourceName(scope.row, $event)">
              <template slot-scope="{ item }">
                <div style="display:flex;justify-content:space-between;">
                  <span>{{ item.value }}</span>
                  <span style="color:#909399">{{ item.key }}</span>
                </div>
              </template>
            </el-autocomplete>
          </template>
        </el-table-column>
        <el-table-column label="源字典编码" width="180">
          <template slot-scope="scope">
            <el-autocomplete
              v-model="scope.row.sourceKey"
              :fetch-suggestions="querySourceKeys"
              size="small"
              placeholder="选择或输入编码"
              @select="handleSelectSourceKey(scope.row, $event)">
              <template slot-scope="{ item }">
                <div style="display:flex;justify-content:space-between;">
                  <span>{{ item.value }}</span>
                  <span style="color:#909399">{{ item.label }}</span>
                </div>
              </template>
            </el-autocomplete>
          </template>
        </el-table-column>
        <el-table-column label="" width="50" align="center">
          <template>
            <i class="el-icon-right"></i>
          </template>
        </el-table-column>
        <el-table-column label="目标字典名称" width="180">
          <template slot-scope="scope">
            <el-autocomplete
              v-model="scope.row.targetLabel"
              :fetch-suggestions="queryTargetNames"
              size="small"
              placeholder="选择或输入名称"
              @select="handleSelectTargetName(scope.row, $event)">
              <template slot-scope="{ item }">
                <div style="display:flex;justify-content:space-between;">
                  <span>{{ item.value }}</span>
                  <span style="color:#909399">{{ item.key }}</span>
                </div>
              </template>
            </el-autocomplete>
          </template>
        </el-table-column>
        <el-table-column label="目标字典编码" width="180">
          <template slot-scope="scope">
            <el-autocomplete
              v-model="scope.row.targetKey"
              :fetch-suggestions="queryTargetKeys"
              size="small"
              placeholder="选择或输入编码"
              @select="handleSelectTargetKey(scope.row, $event)">
              <template slot-scope="{ item }">
                <div style="display:flex;justify-content:space-between;">
                  <span>{{ item.value }}</span>
                  <span style="color:#909399">{{ item.label }}</span>
                </div>
              </template>
            </el-autocomplete>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" icon="el-icon-delete" @click="handleDeleteItem(scope.$index)" style="color: #F56C6C">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saveLoading" :disabled="(!currentSourceTypeValue || !currentTargetTypeValue) || ((mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' || mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') && !customSourceTypeLabel) || ((mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') && !customTargetTypeLabel)">保存</el-button>
      </span>
    </el-dialog>

    <!-- 源自定义项批量导入弹窗 -->
    <el-dialog title="批量导入源自定义项" :visible.sync="sourceImportDialogVisible" width="600px" @close="sourceImportDialogVisible=false">
      <div style="margin-bottom:8px;color:#909399">粘贴每行格式：key,value，例如：<code>A001,管理员</code></div>
      <el-input type="textarea" v-model="sourceImportText" :rows="8" placeholder="每行：编码,名称"></el-input>
      <span slot="footer">
        <el-button @click="sourceImportDialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleImportSourceItems">导入</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'DictMappingDetail',
  data() {
    return {
      mappingId: null,
      mappingInfo: {},
      sourceDictName: '',
      targetDictName: '',
      typeGroups: [],
      totalItems: 0,
      activeTypes: '',
      
      // 对话框
      dialogVisible: false,
      dialogTitle: '新增类型映射',
      editingGroup: null,
      editingItems: [],
      
      // 类型和数据
      currentSourceTypeValue: '',
      currentTargetTypeValue: '',
      sourceTypeList: [],
      targetTypeList: [],
      sourceTypeLoading: false,
      targetTypeLoading: false,
      sourceItems: [],
      targetItems: [],
      
      // 加载状态
      autoMatchLoading: false,
      saveLoading: false,
      sourceCustomItems: [],
      sourceImportDialogVisible: false,
      sourceImportText: '',
      customSourceTypeLabel: '',
      customTargetTypeLabel: '',
      typeLabelsMap: { source: {}, target: {} }
    }
  },
  created() {
    this.mappingId = this.$route.params.id
    if (this.mappingId) {
      this.loadMappingInfo()
    }
  },
  methods: {
    // 加载映射基本信息
    loadMappingInfo() {
      this.$axios.get(`/v1/dict-mapping/${this.mappingId}`).then(res => {
        this.mappingInfo = res.data
        // 解析源自定义项
        if (this.mappingInfo && this.mappingInfo.sourceCustomItems) {
          try { this.sourceCustomItems = JSON.parse(this.mappingInfo.sourceCustomItems) || [] } catch(e) { this.sourceCustomItems = [] }
        }
        this.loadDictSourceNames()
        const promises = []
        // 解析类型标签映射
        if (this.mappingInfo && this.mappingInfo.typeLabels) {
          try { this.typeLabelsMap = JSON.parse(this.mappingInfo.typeLabels) || { source: {}, target: {} } } catch(e) { this.typeLabelsMap = { source: {}, target: {} } }
        }

        if (this.mappingInfo.mappingType === 'SOURCE_TO_SOURCE' || this.mappingInfo.mappingType === 'SOURCE_TO_CUSTOM') {
          promises.push(this.loadSourceTypes())
        }
        if (this.mappingInfo.mappingType === 'SOURCE_TO_SOURCE' || this.mappingInfo.mappingType === 'CUSTOM_TO_SOURCE') {
          promises.push(this.loadTargetTypes())
        }
        Promise.all(promises).then(() => {
          this.loadAllTypeGroups()
        })
      })
    },
    
    // 加载数据源名称
    loadDictSourceNames() {
      if (this.mappingInfo.sourceDictId) {
        this.$axios.get(`/v1/dict-source/${this.mappingInfo.sourceDictId}`).then(res => {
          this.sourceDictName = res.data.sourceName
        })
      }
      if (this.mappingInfo.targetDictId) {
        this.$axios.get(`/v1/dict-source/${this.mappingInfo.targetDictId}`).then(res => {
          this.targetDictName = res.data.sourceName
        })
      }
    },
    
    // 加载所有类型分组
    loadAllTypeGroups() {
      this.$axios.get(`/v1/dict-mapping/${this.mappingId}/items`).then(res => {
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
              sourceTypeLabel: null,
              targetTypeLabel: null,
              items: []
            }
          }
          groups[key].items.push(item)
        })
        
        this.typeGroups = Object.values(groups)
        this.totalItems = items.length
        
        // 加载类型名称
        this.loadTypeLabels()
      })
    },
    
    // 加载类型名称
    loadTypeLabels() {
      // 为每个类型组加载对应的类型名称
      this.typeGroups.forEach(group => {
        // 查找源类型名称（优先使用自定义标签映射）
        if (group.sourceTypeValue) {
          const customLabel = (this.typeLabelsMap && this.typeLabelsMap.source) ? this.typeLabelsMap.source[String(group.sourceTypeValue)] : null
          if (customLabel) {
            group.sourceTypeLabel = customLabel
          } else if (this.sourceTypeList.length > 0) {
            const sourceType = this.sourceTypeList.find(t => String(t.value) === String(group.sourceTypeValue))
            if (sourceType) { group.sourceTypeLabel = sourceType.label }
          }
        }
        
        // 查找目标类型名称（优先使用自定义标签映射）
        if (group.targetTypeValue) {
          const customLabelT = (this.typeLabelsMap && this.typeLabelsMap.target) ? this.typeLabelsMap.target[String(group.targetTypeValue)] : null
          if (customLabelT) {
            group.targetTypeLabel = customLabelT
          } else if (this.targetTypeList.length > 0) {
            const targetType = this.targetTypeList.find(t => String(t.value) === String(group.targetTypeValue))
            if (targetType) { group.targetTypeLabel = targetType.label }
          }
        }
      })
      
      // 强制更新视图
      this.$forceUpdate()
    },
    
    // 加载源字典类型列表
    loadSourceTypes() {
      // 仅当有有效的 sourceDictId 时才加载
      if (!this.mappingInfo || this.mappingInfo.sourceDictId === null || this.mappingInfo.sourceDictId === undefined || String(this.mappingInfo.sourceDictId).toLowerCase() === 'null' || isNaN(Number(this.mappingInfo.sourceDictId))) {
        this.sourceTypeList = []
        return Promise.resolve()
      }
      this.sourceTypeLoading = true
      return this.$axios.get(`/v1/dict-source/${this.mappingInfo.sourceDictId}/types`).then(res => {
        this.sourceTypeList = (res.data || []).map(item => ({ ...item, value: String(item.value) }))
      }).finally(() => { this.sourceTypeLoading = false })
    },
    
    // 加载目标字典类型列表
    loadTargetTypes() {
      // 仅当有有效的 targetDictId 时才加载
      if (!this.mappingInfo || this.mappingInfo.targetDictId === null || this.mappingInfo.targetDictId === undefined || String(this.mappingInfo.targetDictId).toLowerCase() === 'null' || isNaN(Number(this.mappingInfo.targetDictId))) {
        this.targetTypeList = []
        return Promise.resolve()
      }
      this.targetTypeLoading = true
      return this.$axios.get(`/v1/dict-source/${this.mappingInfo.targetDictId}/types`).then(res => {
        this.targetTypeList = (res.data || []).map(item => ({ ...item, value: String(item.value) }))
      }).finally(() => { this.targetTypeLoading = false })
    },
    
    // 新增类型映射
    handleAddType() {
      this.dialogTitle = '新增类型映射'
      this.editingGroup = null
      this.currentSourceTypeValue = ''
      this.currentTargetTypeValue = ''
      this.editingItems = []
      this.dialogVisible = true
    },
    
    // 编辑类型映射
    handleEditType(group) {
      console.log('编辑类型映射:', group)
      console.log('源类型列表:', this.sourceTypeList)
      console.log('目标类型列表:', this.targetTypeList)
      
      this.dialogTitle = '编辑类型映射'
      this.editingGroup = group
      
      // 确保类型值的类型一致（转为字符串）
      this.currentSourceTypeValue = String(group.sourceTypeValue)
      this.currentTargetTypeValue = group.targetTypeValue ? String(group.targetTypeValue) : ''
      
      // 自定义源场景：从typeLabelsMap中回填类型名称
      if (this.mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.typeLabelsMap && this.typeLabelsMap.source && this.currentSourceTypeValue) {
          this.customSourceTypeLabel = this.typeLabelsMap.source[this.currentSourceTypeValue] || ''
        } else {
          this.customSourceTypeLabel = group.sourceTypeLabel || ''
        }
      }
      
      // 自定义目标场景：从typeLabelsMap中回填类型名称
      if (this.mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') {
        if (this.typeLabelsMap && this.typeLabelsMap.target && this.currentTargetTypeValue) {
          this.customTargetTypeLabel = this.typeLabelsMap.target[this.currentTargetTypeValue] || ''
        } else {
          this.customTargetTypeLabel = group.targetTypeLabel || ''
        }
      }
      
      console.log('设置的源类型值:', this.currentSourceTypeValue, typeof this.currentSourceTypeValue)
      console.log('设置的源类型名称:', this.customSourceTypeLabel)
      console.log('设置的目标类型值:', this.currentTargetTypeValue, typeof this.currentTargetTypeValue)
      console.log('设置的目标类型名称:', this.customTargetTypeLabel)
      
      // 触发加载字典项
      if (this.currentSourceTypeValue) {
        this.handleSourceTypeChange(this.currentSourceTypeValue)
      }
      if (this.currentTargetTypeValue) {
        this.handleTargetTypeChange(this.currentTargetTypeValue)
      }
      
      this.dialogVisible = true
    },
    
    // 删除类型映射
    handleDeleteType(group) {
      this.$confirm(`确定删除类型映射"${group.sourceTypeLabel || group.sourceTypeValue}"吗？这将删除该类型下的所有映射项。`, '提示', {
        type: 'warning'
      }).then(() => {
        const params = {
          sourceTypeValue: group.sourceTypeValue,
          targetTypeValue: group.targetTypeValue || ''
        }
        
        this.$axios.post(`/v1/dict-mapping/${this.mappingId}/items`, [], { params }).then(() => {
          this.$message.success('删除成功')
          this.loadAllTypeGroups()
        })
      })
    },
    
    // 源字典类型变化
    handleSourceTypeChange(typeValue) {
      if (!typeValue) {
        this.sourceItems = []
        return
      }
      // 仅当 sourceDictId 为有效数字时才请求数据源项
      if (this.mappingInfo && this.mappingInfo.sourceDictId !== null && this.mappingInfo.sourceDictId !== undefined && String(this.mappingInfo.sourceDictId).toLowerCase() !== 'null' && !isNaN(Number(this.mappingInfo.sourceDictId))) {
        this.$axios.get(`/v1/dict-source/${this.mappingInfo.sourceDictId}/items`, { params: { typeValue: typeValue } }).then(res => {
          this.sourceItems = res.data || []
        })
      } else {
        this.sourceItems = []
      }
      // 加载该类型已保存的映射项
      this.loadTypeMappingItems()
    },
    
    // 目标字典类型变化
    handleTargetTypeChange(typeValue) {
      if (!typeValue) {
        this.targetItems = []
        return
      }
      // 仅当 targetDictId 为有效数字时才请求数据源项
      if (this.mappingInfo && this.mappingInfo.targetDictId !== null && this.mappingInfo.targetDictId !== undefined && String(this.mappingInfo.targetDictId).toLowerCase() !== 'null' && !isNaN(Number(this.mappingInfo.targetDictId))) {
        this.$axios.get(`/v1/dict-source/${this.mappingInfo.targetDictId}/items`, { params: { typeValue: typeValue } }).then(res => {
          this.targetItems = res.data || []
        })
      } else {
        this.targetItems = []
      }
      // 加载该类型已保存的映射项
      this.loadTypeMappingItems()
    },
    
    // 加载特定类型的映射项
    loadTypeMappingItems() {
      const params = {}
      if (this.currentSourceTypeValue) {
        params.sourceTypeValue = this.currentSourceTypeValue
      }
      if (this.currentTargetTypeValue) {
        params.targetTypeValue = this.currentTargetTypeValue
      }
      
      this.$axios.get(`/v1/dict-mapping/${this.mappingId}/items`, { params }).then(res => {
        this.editingItems = res.data || []
      })
    },
    
    // 查询建议：源名称
    querySourceNames(queryString, cb) {
      const list = (this.sourceItems || []).map(i => ({ value: i.value, key: i.key }))
      cb(queryString ? list.filter(it => (it.value || '').includes(queryString)) : list)
    },
    // 查询建议：源编码
    querySourceKeys(queryString, cb) {
      const list = (this.sourceItems || []).map(i => ({ value: i.key, label: i.value }))
      cb(queryString ? list.filter(it => (it.value || '').includes(queryString)) : list)
    },
    // 查询建议：目标名称
    queryTargetNames(queryString, cb) {
      const list = (this.targetItems || []).map(i => ({ value: i.value, key: i.key }))
      cb(queryString ? list.filter(it => (it.value || '').includes(queryString)) : list)
    },
    // 查询建议：目标编码
    queryTargetKeys(queryString, cb) {
      const list = (this.targetItems || []).map(i => ({ value: i.key, label: i.value }))
      cb(queryString ? list.filter(it => (it.value || '').includes(queryString)) : list)
    },
    // 选择处理：源名称
    handleSelectSourceName(row, item) {
      row.sourceLabel = item.value
      row.sourceKey = item.key
    },
    // 选择处理：源编码
    handleSelectSourceKey(row, item) {
      row.sourceKey = item.value
      row.sourceLabel = item.label
    },
    // 选择处理：目标名称
    handleSelectTargetName(row, item) {
      row.targetLabel = item.value
      row.targetKey = item.key
    },
    // 选择处理：目标编码
    handleSelectTargetKey(row, item) {
      row.targetKey = item.value
      row.targetLabel = item.label
    },
    
    // 添加映射项
    handleAddItem() {
      this.editingItems.push({
        sourceKey: '',
        sourceLabel: '',
        targetKey: '',
        targetLabel: ''
      })
    },
    
    // 删除映射项
    handleDeleteItem(index) {
      this.editingItems.splice(index, 1)
    },
    
    // 清空全部映射项
    handleClearAllItems() {
      this.editingItems = []
    },
    
    handleCustomSourceTypeInput() { this.loadTypeMappingItems() },
    handleCustomTargetTypeInput() { this.loadTypeMappingItems() },
    handleCustomSourceTypeLabelInput() { /* 仅用于显示，不参与后端 */ },
    handleCustomTargetTypeLabelInput() { /* 仅用于显示，不参与后端 */ },
    
    handleAutoMatch() {
      if (this.mappingInfo.mappingType === 'SOURCE_TO_SOURCE') {
        if (!this.currentSourceTypeValue) { this.$message.warning('请先选择源字典类型'); return }
        if (!this.currentTargetTypeValue) { this.$message.warning('请先选择目标字典类型'); return }
        this.autoMatchLoading = true
        this.$axios.post(`/v1/dict-mapping/${this.mappingId}/auto-match`, null, {
          params: { sourceTypeValue: this.currentSourceTypeValue, targetTypeValue: this.currentTargetTypeValue }
        }).then(res => {
          let items = res.data || []
          const existSet = new Set(items.map(it => `${it.targetKey || ''}|${it.targetLabel || ''}`));
          (this.targetItems || []).forEach(t => {
            const key = `${t.key || ''}|${t.value || ''}`
            if (!existSet.has(key)) {
              items.push({ sourceKey: '', sourceLabel: '', targetKey: t.key, targetLabel: t.value })
            }
          })
          this.editingItems = items
          this.$message.success(`智能匹配完成！匹配到 ${this.editingItems.length} 个项`)
        }).finally(() => { this.autoMatchLoading = false })
      } else if (this.mappingInfo.mappingType === 'CUSTOM_TO_SOURCE') {
        if (!this.currentTargetTypeValue) { this.$message.warning('请先选择目标字典类型'); return }
        if (this.editingItems.length === 0) { this.$message.warning('请先添加映射项'); return }
        const targetKeyMap = new Map(this.targetItems.map(i => [String(i.key || '').toLowerCase().trim(), i]))
        const targetLabelMap = new Map(this.targetItems.map(i => [String(i.value || '').toLowerCase().trim(), i]))
        this.editingItems = this.editingItems.map(src => {
          const k = String(src.sourceKey || '').toLowerCase().trim()
          const v = String(src.sourceLabel || '').toLowerCase().trim()
          const matched = targetKeyMap.get(k) || targetLabelMap.get(v)
          return { ...src, targetKey: matched ? matched.key : '', targetLabel: matched ? matched.value : '' }
        })
        const matchedCount = this.editingItems.filter(m => m.targetKey).length
        this.$message.success(`智能匹配完成！匹配到 ${matchedCount} 项，未匹配项请手动填写`)
      } else {
        // SOURCE_TO_CUSTOM 或 CUSTOM_TO_CUSTOM：智能填充
        const srcList = this.mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' ? this.sourceItems : this.sourceCustomItems
        if (!srcList || srcList.length === 0) { this.$message.warning('源数据为空，请先选择类型或添加自定义项'); return }
        this.editingItems = srcList.map(item => ({ sourceKey: item.key || '', sourceLabel: item.value || '', targetKey: '', targetLabel: '' }))
        this.$message.success(`智能填充完成！已生成 ${this.editingItems.length} 条源项`) 
      }
    },
    
    handleSave() {
      const requireSourceType = true
      if (!this.currentSourceTypeValue) { this.$message.warning('请先填写源字典类型编码'); return }
      const requireTargetType = true
      if (!this.currentTargetTypeValue) { this.$message.warning('请先填写目标字典类型编码'); return }
      if ((this.mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') && !this.customSourceTypeLabel) { this.$message.warning('请填写源字典类型名称'); return }
      if ((this.mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') && !this.customTargetTypeLabel) { this.$message.warning('请填写目标字典类型名称'); return }
      this.saveLoading = true
      const params = { sourceTypeValue: this.currentSourceTypeValue, targetTypeValue: this.currentTargetTypeValue }
      // 先保存映射项
      this.$axios.post(`/v1/dict-mapping/${this.mappingId}/items`, this.editingItems, { params }).then(() => {
        // 更新类型标签映射（自定义场景）
        let labels = this.typeLabelsMap || { source: {}, target: {} }
        if (this.mappingInfo.mappingType === 'CUSTOM_TO_SOURCE' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') {
          labels.source[String(this.currentSourceTypeValue)] = this.customSourceTypeLabel || ''
        }
        if (this.mappingInfo.mappingType === 'SOURCE_TO_CUSTOM' || this.mappingInfo.mappingType === 'CUSTOM_TO_CUSTOM') {
          labels.target[String(this.currentTargetTypeValue)] = this.customTargetTypeLabel || ''
        }
        const payload = { ...this.mappingInfo, typeLabels: JSON.stringify(labels) }
        return this.$axios.put('/v1/dict-mapping', payload)
      }).then(() => {
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.loadAllTypeGroups()
      }).finally(() => { this.saveLoading = false })
    },
    
    // 关闭对话框
    handleDialogClose() {
      this.currentSourceTypeValue = ''
      this.currentTargetTypeValue = ''
      this.editingItems = []
      this.sourceItems = []
      this.targetItems = []
      this.sourceCustomItems = []
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
    getAutoButtonLabel(type) {
      return type === 'SOURCE_TO_SOURCE' ? '智能匹配' : (type === 'CUSTOM_TO_SOURCE' ? '智能匹配' : '智能填充')
    },
    addSourceCustomItem() { this.sourceCustomItems.push({ key: '', value: '' }) },
    removeSourceCustomItem(index) { this.sourceCustomItems.splice(index, 1) },
    openSourceImportDialog() { this.sourceImportDialogVisible = true },
    handleImportSourceItems() {
      const lines = (this.sourceImportText || '').split(/\r?\n/).map(l => l.trim()).filter(l => l)
      const items = []
      lines.forEach(l => {
        const parts = l.split(',')
        if (parts.length >= 2) { items.push({ key: parts[0].trim(), value: parts[1].trim() }) }
      })
      if (items.length === 0) { this.$message.warning('请按 key,value 每行格式粘贴'); return }
      this.sourceCustomItems = items
      this.sourceImportDialogVisible = false
      this.sourceImportText = ''
      this.$message.success(`已导入 ${items.length} 个自定义源项`)
    }
  }
}
</script>

<style scoped>
.dict-mapping-detail-container {
  padding: 20px;
}
</style>
