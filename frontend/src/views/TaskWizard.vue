<template>
  <div class="task-wizard-page">
    <el-card class="wizard-card" shadow="never">
      <div slot="header" class="wizard-header-section">
        <div class="header-title-area">
          <div class="title-icon-wrapper" :class="taskConfig.id ? 'edit-mode' : 'create-mode'">
            <i :class="taskConfig.id ? 'el-icon-edit' : 'el-icon-plus'"></i>
          </div>
          <div class="title-text-wrapper">
            <h2>{{ taskConfig.id ? '编辑任务' : '创建任务' }}</h2>
            <p v-if="taskConfig.taskName" class="subtitle">{{ taskConfig.taskName }}</p>
            <p v-else class="subtitle placeholder-subtitle">请先输入任务名称</p>
          </div>
        </div>
        
        <!-- 重新设计的步骤条 -->
        <div class="wizard-steps-modern">
          <div class="steps-container">
            <div 
              v-for="(step, index) in stepsData" 
              :key="index"
              class="step-item-modern"
              :class="{
                'is-completed': activeStep > index,
                'is-current': activeStep === index,
                'is-upcoming': activeStep < index
              }"
              @click="handleStepClick(index)">
              <!-- 步骤圆圈 -->
              <div class="step-circle">
                <div class="circle-bg"></div>
                <div class="circle-content">
                  <i v-if="activeStep > index" class="el-icon-check step-check"></i>
                  <i v-else :class="step.icon" class="step-icon"></i>
                </div>
                <div class="circle-ring"></div>
              </div>
              
              <!-- 步骤信息 -->
              <div class="step-info">
                <div class="step-label">STEP {{ index + 1 }}</div>
                <div class="step-title">{{ step.title }}</div>
              </div>
              
              <!-- 连接线 -->
              <div v-if="index < stepsData.length - 1" class="step-connector">
                <div class="connector-line"></div>
                <div class="connector-progress" :style="{ width: activeStep > index ? '100%' : '0%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤1: 基本信息 -->
      <div v-show="activeStep === 0" class="step-content">
        <!-- 合并的基本信息和同步配置卡片 -->
        <div class="form-section-card">
          <div class="section-header">
            <div class="section-icon">
              <i class="el-icon-setting"></i>
            </div>
            <div class="section-title">
              <h3>任务基础配置</h3>
              <p>配置任务的名称、编码、同步方式和描述信息</p>
            </div>
          </div>
          <div class="section-body">
            <el-form :model="taskConfig" label-width="110px" size="medium" class="modern-form">
              <!-- 基本信息区域 -->
              <div class="form-group">
                <div class="group-title">
                  <i class="el-icon-info"></i>
                  <span>基本信息</span>
                </div>
                
                <el-form-item label="任务名称" required class="form-item-enhanced">
                  <el-input 
                    v-model="taskConfig.taskName" 
                    placeholder="请输入任务名称" 
                    class="input-enhanced"
                    @input="handleTaskNameChange">
                    <i slot="prefix" class="el-icon-notebook-2"></i>
                  </el-input>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    建议使用有意义的名称，如: 用户数据同步
                  </div>
                </el-form-item>
                
                <el-form-item label="任务编码" required class="form-item-enhanced">
                  <el-input 
                    v-model="taskConfig.taskCode" 
                    placeholder="自动生成或手动输入"
                    class="input-enhanced">
                    <i slot="prefix" class="el-icon-link"></i>
                  </el-input>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    唯一标识，根据任务名称自动生成，也可手动修改
                  </div>
                </el-form-item>
                
                <el-form-item label="任务描述" class="form-item-enhanced">
                  <el-input 
                    v-model="taskConfig.description" 
                    type="textarea" 
                    :rows="3" 
                    placeholder="简要描述任务用途"
                    class="textarea-enhanced" />
                </el-form-item>
              </div>
              
              <!-- 分隔线 -->
              <el-divider class="section-divider"></el-divider>
              
              <!-- 同步配置区域 -->
              <div class="form-group">
                <div class="group-title">
                  <i class="el-icon-refresh"></i>
                  <span>同步配置</span>
                </div>
                
                <el-form-item label="同步模式" required class="form-item-enhanced">
                  <div class="mode-selector">
                    <div 
                      class="mode-card" 
                      :class="{ 'is-active': taskConfig.syncMode === 'FULL' }"
                      @click="taskConfig.syncMode = 'FULL'">
                      <div class="mode-icon">
                        <i class="el-icon-files"></i>
                      </div>
                      <div class="mode-info">
                        <h4>全量同步</h4>
                        <p>每次执行读取全部数据</p>
                      </div>
                      <div class="mode-check">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                    
                    <div 
                      class="mode-card" 
                      :class="{ 'is-active': taskConfig.syncMode === 'INCREMENTAL' }"
                      @click="taskConfig.syncMode = 'INCREMENTAL'">
                      <div class="mode-icon">
                        <i class="el-icon-sort"></i>
                      </div>
                      <div class="mode-info">
                        <h4>增量同步</h4>
                        <p>只同步新增或修改的数据</p>
                      </div>
                      <div class="mode-check">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                  </div>
                </el-form-item>
                
                <!-- 增量同步配置 -->
                <transition name="el-zoom-in-top">
                  <el-form-item v-if="taskConfig.syncMode === 'INCREMENTAL'" label="增量字段" required class="form-item-enhanced">
                    <el-input 
                      v-model="taskConfig.incrementalField" 
                      placeholder="请输入时间字段名，如: update_time、create_time"
                      class="input-enhanced">
                      <i slot="prefix" class="el-icon-time"></i>
                    </el-input>
                    <div class="info-box">
                      <div class="info-box-header">
                        <i class="el-icon-info"></i>
                        <strong>增量同步支持两种方式：</strong>
                      </div>
                      <ul class="info-list">
                        <li>
                          <span class="list-badge">推荐</span>
                          <strong>方式一：自动追加条件</strong>
                          <p>SQL中只写基础查询，系统自动在增量字段上追加时间条件</p>
                          <code class="code-example">SELECT * FROM users</code>
                          <span class="code-arrow">→</span>
                          <code class="code-result">WHERE update_time > '2025-11-24 10:30:00'</code>
                        </li>
                        <li>
                          <span class="list-badge advanced">高级</span>
                          <strong>方式二：手动控制</strong>
                          <p>SQL中使用 <code>{last_sync_time}</code> 占位符，适合复杂查询</p>
                          <code class="code-example">SELECT * FROM users WHERE status='ACTIVE' AND update_time > '{last_sync_time}'</code>
                        </li>
                        <li class="success-tip">
                          <i class="el-icon-success"></i>
                          首次执行自动全量同步，后续执行自动增量
                        </li>
                      </ul>
                    </div>
                  </el-form-item>
                </transition>
              </div>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 步骤2: 选择数据源 -->
      <div v-show="activeStep === 1" class="step-content">
        <!-- 源连接器选择卡片 -->
        <div class="form-section-card">
          <div class="section-header">
            <div class="section-icon">
              <i class="el-icon-upload2"></i>
            </div>
            <div class="section-title">
              <h3>源连接器配置</h3>
              <p>选择数据来源连接器并配置数据查询规则</p>
            </div>
          </div>
          <div class="section-body">
            <el-form :model="sourceConfig" label-width="110px" size="medium" class="modern-form">
              <!-- 连接器选择区域 -->
              <div class="form-group">
                <div class="group-title">
                  <i class="el-icon-connection"></i>
                  <span>连接器选择</span>
                </div>
                
                <el-form-item label="源连接器" required class="form-item-enhanced">
                  <el-select 
                    v-model="sourceConfig.connectorId" 
                    placeholder="请选择数据源连接器" 
                    class="input-enhanced"
                    @change="handleSourceConnectorChange">
                    <i slot="prefix" class="el-icon-database"></i>
                    <el-option 
                      v-for="item in connectorList" 
                      :key="item.id" 
                      :label="getConnectorLabel(item)" 
                      :value="item.id">
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <!-- Logo图标 -->
                          <img v-if="getConnectorLogo(item)" 
                            :src="getConnectorLogo(item)" 
                            style="width: 20px; height: 20px; object-fit: contain;" 
                            :alt="item.connectorName" />
                          <i v-else class="el-icon-connection" style="font-size: 20px; color: #9ca3af;"></i>
                          <span>{{ item.connectorName }}</span>
                        </div>
                        <el-tag size="mini" :type="item.connectorType === 'DATABASE' ? 'success' : 'warning'" effect="plain">
                          {{ getConnectorTypeLabel(item) }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    选择已配置的数据源连接器，或
                    <el-button type="text" @click="$router.push('/connector')" style="padding: 0; margin-left: 4px">新建连接器</el-button>
                  </div>
                </el-form-item>
              </div>
          
          <!-- 数据库类型数据源配置 -->
          <template v-if="sourceConnectorType === 'DATABASE'">
            <el-divider class="section-divider"></el-divider>
            
            <!-- 数据查询区域 -->
            <div class="form-group">
              <div class="group-title">
                <i class="el-icon-document"></i>
                <span>数据查询</span>
              </div>
              
              <el-form-item v-if="sourceConfig.connectorId" label="查询配置" required class="form-item-enhanced">
                <!-- 紧凑型快捷工具栏 -->
                <div class="compact-toolbar">
                  <el-button 
                    type="primary" 
                    size="mini" 
                    @click="handleLoadTables" 
                    :loading="loadingTables"
                    icon="el-icon-refresh">
                    加载表
                  </el-button>
                  <el-select 
                    v-model="selectedTable" 
                    placeholder="快捷选表" 
                    size="mini"
                    style="width: 200px" 
                    @change="handleTableSelect"
                    clearable
                    filterable>
                    <el-option 
                      v-for="table in sourceTableList" 
                      :key="table.tableName" 
                      :label="table.tableName" 
                      :value="table.tableName">
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span style="font-weight: 500; color: #303133;">{{ table.tableName }}</span>
                        <span v-if="table.tableComment" style="color: #909399; font-size: 12px; margin-left: 12px;">{{ table.tableComment }}</span>
                      </div>
                    </el-option>
                  </el-select>
                  <span class="toolbar-tip">选表后自动生成SQL</span>
                </div>
                
                <!-- SQL编辑器 - 增强版 -->
                <div class="sql-editor-enhanced">
                  <div class="editor-toolbar">
                    <div class="toolbar-left">
                      <span class="editor-label">
                        <i class="el-icon-edit"></i> SQL编辑器
                      </span>
                      <el-tooltip content="支持变量 {last_sync_time}" placement="top">
                        <el-tag size="mini" type="warning">{last_sync_time}</el-tag>
                      </el-tooltip>
                      <span v-if="sqlValidationError" class="sql-error-badge">
                        <i class="el-icon-warning"></i> {{ sqlValidationError }}
                      </span>
                      <span v-else-if="sourceConfig.sql && !sqlValidationError" class="sql-valid-badge">
                        <i class="el-icon-success"></i> 语法正常
                      </span>
                    </div>
                    <div class="toolbar-right">
                      <el-button-group size="mini">
                        <el-button @click="formatSql" icon="el-icon-sort">格式化</el-button>
                        <el-button
                          size="mini"
                          type="primary"
                          :loading="sqlValidationLoading"
                          @click="validateSourceSql">
                          SQL语法验证
                        </el-button>
                        <el-button @click="clearSql" icon="el-icon-delete">清空</el-button>
                      </el-button-group>
                    </div>
                  </div>
                  
                  <!-- SQL输入区 -->
                  <div class="sql-input-wrapper">
                    <div class="sql-editor-container">
                      <div class="sql-editor-background" v-html="highlightedSql"></div>
                      <textarea 
                        v-model="sourceConfig.sql"
                        class="sql-editor-input"
                        rows="6"
                        placeholder="请输入SQL查询语句，或使用上方快捷选表功能生成"
                        @input="updateHighlight"
                        @scroll="syncScroll"
                        ref="sqlTextarea">
                      </textarea>
                    </div>
                  </div>
                  
                  <!-- 底部操作栏 -->
                  <div class="editor-actions">
                    <div class="actions-left">
                      <span class="tip-text">
                        <i class="el-icon-info"></i>
                        示例: SELECT * FROM users WHERE id > 100
                      </span>
                    </div>
                    <div class="actions-right">
                      <el-input-number 
                        v-model="previewLimit" 
                        :min="1" 
                        :max="1000" 
                        size="small" 
                        controls-position="right"
                        style="width: 120px; margin-right: 10px;"
                        placeholder="条数">
                      </el-input-number>
                      <span style="margin-right: 10px; color: #909399; font-size: 13px;">条</span>
                      <el-button 
                        type="primary" 
                        size="small" 
                        @click="handlePreviewSource" 
                        :disabled="!sourceConfig.sql" 
                        :loading="sourcePreviewLoading"
                        icon="el-icon-download">
                        抽取数据
                      </el-button>
                      <span v-if="sourcePreviewData.length > 0" class="extract-badge">
                        <i class="el-icon-success"></i> {{ sourcePreviewData.length }} 条
                      </span>
                    </div>
                  </div>
                </div>
              </el-form-item>
            </div>
          </template>
          
          <!-- API类型数据源配置 -->
          <template v-if="sourceConnectorType === 'API'">
            
            <!-- 请求方式和URL -->
            <el-form-item label="请求配置" required>
              <div style="display: flex; margin-bottom: 12px">
                <el-select v-model="sourceConfig.apiMethod" style="width: 120px; margin-right: 8px">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="PATCH" value="PATCH" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
                <el-input v-model="sourceConfig.apiPath" placeholder="/api/users" style="flex: 1">
                  <template slot="prepend">{{ sourceConnectorBaseUrl }}</template>
                </el-input>
              </div>
              <div class="tip">完整URL: {{ sourceConnectorBaseUrl }}{{ sourceConfig.apiPath }}</div>
            </el-form-item>
            
            <!-- Postman风格标签页 -->
            <el-form-item label="请求配置">
              <el-tabs v-model="sourceApiActiveTab" type="border-card">
                <!-- Params标签页 -->
                <el-tab-pane label="Params" name="params">
                  <div style="margin-bottom: 8px">
                    <el-button size="mini" type="primary" @click="addSourceApiParam">+ 添加参数</el-button>
                  </div>
                  <el-table :data="sourceConfig.apiParams || []" size="mini" border max-height="250">
                    <el-table-column label="启用" width="60" align="center">
                      <template slot-scope="scope">
                        <el-checkbox v-model="scope.row.enabled" />
                      </template>
                    </el-table-column>
                    <el-table-column label="Key" width="180">
                      <template slot-scope="scope">
                        <el-input v-model="scope.row.key" placeholder="status" size="mini" />
                      </template>
                    </el-table-column>
                    <el-table-column label="Value" min-width="180">
                      <template slot-scope="scope">
                        <el-input v-model="scope.row.value" placeholder="active" size="mini" />
                      </template>
                    </el-table-column>
                    <el-table-column label="Description" min-width="150">
                      <template slot-scope="scope">
                        <el-input v-model="scope.row.description" placeholder="说明" size="mini" />
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="60" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" @click="removeSourceApiParam(scope.$index)" style="color: #F56C6C">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="tip" style="margin-top: 8px">Query参数会追加到URL: ?key1=value1&key2=value2</div>
                </el-tab-pane>
                
                <!-- Body标签页 -->
                <el-tab-pane label="Body" name="body" :disabled="sourceConfig.apiMethod === 'GET' || sourceConfig.apiMethod === 'DELETE'">
                  <el-radio-group v-model="sourceConfig.bodyType" size="small" style="margin-bottom: 12px" @change="handleSourceBodyTypeChange">
                    <el-radio-button label="none">none</el-radio-button>
                    <el-radio-button label="form-data">form-data</el-radio-button>
                    <el-radio-button label="json">raw (JSON)</el-radio-button>
                  </el-radio-group>
                  
                  <!-- form-data类型 -->
                  <div v-if="sourceConfig.bodyType === 'form-data'">
                    <div style="margin-bottom: 8px">
                      <el-button size="mini" type="primary" @click="addSourceFormDataParam">+ 添加字段</el-button>
                      <span style="margin-left: 12px; font-size: 12px; color: #909399">Content-Type: multipart/form-data</span>
                    </div>
                    <el-table :data="sourceConfig.formData || []" size="mini" border max-height="200">
                      <el-table-column label="启用" width="60" align="center">
                        <template slot-scope="scope">
                          <el-checkbox v-model="scope.row.enabled" />
                        </template>
                      </el-table-column>
                      <el-table-column label="Key" width="180">
                        <template slot-scope="scope">
                          <el-input v-model="scope.row.key" placeholder="username" size="mini" />
                        </template>
                      </el-table-column>
                      <el-table-column label="Value" min-width="200">
                        <template slot-scope="scope">
                          <el-input v-model="scope.row.value" placeholder="admin" size="mini" />
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="60" align="center">
                        <template slot-scope="scope">
                          <el-button type="text" size="mini" @click="removeSourceFormDataParam(scope.$index)" style="color: #F56C6C">删除</el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                  
                  <!-- JSON类型 -->
                  <div v-else-if="sourceConfig.bodyType === 'json'">
                    <div style="margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center">
                      <span style="font-size: 12px; color: #909399">Content-Type: application/json</span>
                      <div>
                        <el-button size="mini" type="success" @click="formatSourceJson">
                          <i class="el-icon-check"></i> 格式化
                        </el-button>
                        <el-button size="mini" type="warning" @click="validateSourceJson">
                          <i class="el-icon-circle-check"></i> 验证
                        </el-button>
                      </div>
                    </div>
                    <el-input 
                      v-model="sourceConfig.jsonBody" 
                      type="textarea" 
                      :rows="10" 
                      placeholder='{\n  "username": "admin",\n  "password": "123456"\n}'
                      :class="{'json-editor': true, 'json-error': sourceJsonError}"
                      @input="clearSourceJsonError" />
                    <div v-if="sourceJsonError" style="margin-top: 4px; color: #F56C6C; font-size: 12px">
                      <i class="el-icon-warning"></i> {{ sourceJsonError }}
                    </div>
                    <div v-else style="margin-top: 4px; color: #67C23A; font-size: 12px">
                      <i class="el-icon-success"></i> JSON格式正确
                    </div>
                  </div>
                  
                  <!-- none类型 -->
                  <div v-else style="padding: 20px; text-align: center; color: #909399">
                    This request does not have a body
                  </div>
                </el-tab-pane>
                
                <!-- Headers标签页 -->
                <el-tab-pane name="headers">
                  <span slot="label">
                    Headers
                    <el-badge :value="(sourceConfig.headers || []).filter(h => h.enabled).length" :hidden="!(sourceConfig.headers || []).filter(h => h.enabled).length" style="margin-left: 4px" />
                  </span>
                  <div style="margin-bottom: 8px">
                    <el-button size="mini" type="primary" @click="addSourceHeader">+ 添加Header</el-button>
                  </div>
                  <el-table :data="sourceConfig.headers || []" size="mini" border max-height="200">
                    <el-table-column label="启用" width="60" align="center">
                      <template slot-scope="scope">
                        <el-checkbox v-model="scope.row.enabled" />
                      </template>
                    </el-table-column>
                    <el-table-column label="Key" width="180">
                      <template slot-scope="scope">
                        <el-input v-model="scope.row.key" placeholder="Content-Type" size="mini" />
                      </template>
                    </el-table-column>
                    <el-table-column label="Value" min-width="200">
                      <template slot-scope="scope">
                        <el-input v-model="scope.row.value" placeholder="application/json" size="mini" />
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="60" align="center">
                      <template slot-scope="scope">
                        <el-button type="text" size="mini" @click="removeSourceHeader(scope.$index)" style="color: #F56C6C">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div class="tip" style="margin-top: 8px">自定义请求头(会覆盖连接器公共Headers)</div>
                </el-tab-pane>
              </el-tabs>
            </el-form-item>
            
            <!-- 分页配置 -->
            <el-form-item label="分页配置">
              <el-checkbox v-model="sourceConfig.enablePagination" style="margin-bottom: 10px">启用自动分页</el-checkbox>
              <div v-if="sourceConfig.enablePagination" style="border: 1px solid #DCDFE6; border-radius: 4px; padding: 12px; background: #F5F7FA">
                <el-row :gutter="10">
                  <el-col :span="12">
                    <div style="margin-bottom: 8px">
                      <label style="font-size: 12px; color: #606266">页码参数名:</label>
                      <el-input v-model="sourceConfig.pageParam" placeholder="page" size="small" />
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div style="margin-bottom: 8px">
                      <label style="font-size: 12px; color: #606266">每页数量参数名:</label>
                      <el-input v-model="sourceConfig.pageSizeParam" placeholder="pageSize" size="small" />
                    </div>
                  </el-col>
                </el-row>
                <el-row :gutter="10">
                  <el-col :span="8">
                    <div style="margin-bottom: 8px">
                      <label style="font-size: 12px; color: #606266">起始页:</label>
                      <el-input-number v-model="sourceConfig.startPage" :min="0" size="small" style="width: 100%" />
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div style="margin-bottom: 8px">
                      <label style="font-size: 12px; color: #606266">每页数量:</label>
                      <el-input-number v-model="sourceConfig.pageSize" :min="1" :max="1000" size="small" style="width: 100%" />
                    </div>
                  </el-col>
                  <el-col :span="8">
                    <div style="margin-bottom: 8px">
                      <label style="font-size: 12px; color: #606266">总数路径(可选):</label>
                      <el-input v-model="sourceConfig.totalPath" placeholder="data.total" size="small" />
                    </div>
                  </el-col>
                </el-row>
                <div class="tip">
                  <i class="el-icon-info"></i> ETL引擎执行时将自动循环获取所有分页数据
                </div>
              </div>
            </el-form-item>
            
            <el-form-item label="数据路径">
              <el-input v-model="sourceConfig.dataPath" placeholder="请输入JSON数据路径，如: data.list" style="width: 400px" />
              <div class="tip">如果API返回 {"data":{"list":[...]}}，则填写: data.list</div>
            </el-form-item>
            <el-form-item>
              <el-input-number 
                v-model="previewLimit" 
                :min="1" 
                :max="1000" 
                size="small" 
                controls-position="right"
                style="width: 120px; margin-right: 10px;"
                placeholder="条数">
              </el-input-number>
              <span style="margin-right: 10px; color: #909399; font-size: 13px;">条</span>
              <el-button type="primary" size="small" @click="handlePreviewApiSource" :loading="sourcePreviewLoading">
                <i class="el-icon-download"></i> 抽取数据
              </el-button>
              <span v-if="sourcePreviewData.length > 0" style="margin-left: 10px; color: #67C23A">
                <i class="el-icon-success"></i> 已抽取 {{ sourcePreviewData.length }} 条数据
              </span>
            </el-form-item>
            
            <!-- API数据预览 -->
            <el-form-item v-if="sourcePreviewData.length > 0" label="数据预览">
              <el-table :data="sourcePreviewData" border size="small" max-height="300">
                <el-table-column 
                  v-for="col in sourcePreviewColumns" 
                  :key="col" 
                  :prop="col" 
                  :label="col" 
                  min-width="120"
                  show-overflow-tooltip>
                  <template slot-scope="scope">
                    <span>{{ formatCellValue(scope.row[col]) }}</span>
                  </template>
                </el-table-column>
              </el-table>
              <div class="tip">共 {{ sourcePreviewData.length }} 条预览数据</div>
            </el-form-item>
          </template>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 步骤2.5: 辅助数据源(多表关联) -->
      <div v-show="activeStep === 2" class="auxiliary-datasource-step">
        <!-- 页面头部 -->
        <div class="step-header">
          <div class="header-title">
            <i class="el-icon-connection" style="font-size: 24px; color: #3b82f6; margin-right: 12px;"></i>
            <div>
              <h3 style="margin: 0; font-size: 18px; font-weight: 600; color: #1f2937;">多表关联配置</h3>
              <p style="margin: 4px 0 0; font-size: 13px; color: #6b7280;">配置辅助数据源，实现跨表数据关联查询</p>
            </div>
          </div>
        </div>

        <!-- 功能说明卡片 -->
        <div class="info-card">
          <div class="info-card-header">
            <i class="el-icon-info"></i>
            <span>功能说明</span>
          </div>
          <div class="info-card-body">
            <div class="info-item">
              <i class="el-icon-check"></i>
              <span>支持将主数据源与多个辅助数据源进行关联，类似 SQL 的 JOIN 操作</span>
            </div>
            <div class="info-item">
              <i class="el-icon-check"></i>
              <span>关联后的字段以<code class="inline-code">别名.字段名</code>的形式引用，如：<code class="inline-code">user.name</code>、<code class="inline-code">product.price</code></span>
            </div>
            <div class="info-item">
              <i class="el-icon-check"></i>
              <span>支持<strong>链式关联</strong>：<code class="inline-code highlight">A ← B ← C</code>，配置顺序很重要，后续辅助数据源可引用前面的字段</span>
            </div>
            <div class="info-item example">
              <i class="el-icon-star-off"></i>
              <span><strong>示例：</strong>订单关联用户 <code class="inline-code">orders.user_id = users.id</code>，用户再关联部门 <code class="inline-code">user.dept_id = dept.id</code></span>
            </div>
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <el-button type="primary" size="medium" @click="addAuxiliaryDatasource" class="add-button">
            <i class="el-icon-plus"></i>
            添加辅助数据源
          </el-button>
          <div class="status-info">
            <el-tag size="medium" :type="auxiliaryDatasources.length > 0 ? 'success' : 'info'" effect="plain">
              <i class="el-icon-document"></i>
              已配置 {{ auxiliaryDatasources.length }} 个数据源
            </el-tag>
            <el-tooltip v-if="auxiliaryDatasources.length > 1" placement="top" effect="light">
              <div slot="content" style="max-width: 300px;">
                <div style="font-weight: 600; margin-bottom: 8px; color: #3b82f6;">链式关联顺序说明：</div>
                <div style="line-height: 1.6;">
                  • 配置顺序决定关联顺序<br/>
                  • 第N个辅助数据源可以引用前面N-1个的字段<br/>
                  • 例如：第2个可以在"主表字段"中填写 <code style="background: #f0f9ff; padding: 2px 6px; border-radius: 3px;">user.dept_id</code>
                </div>
              </div>
              <i class="el-icon-question help-icon"></i>
            </el-tooltip>
          </div>
        </div>

        <!-- 数据源列表 -->
        <div v-if="auxiliaryDatasources.length > 0" class="datasource-list">
          <div v-for="(item, index) in auxiliaryDatasources" :key="index" class="datasource-card">
            <!-- 卡片头部 -->
            <div class="card-header">
              <div class="card-title">
                <el-tag size="small" type="primary" effect="dark" class="order-tag">{{ index + 1 }}</el-tag>
                <i class="el-icon-database" style="color: #3b82f6; margin: 0 8px;"></i>
                <span style="font-weight: 600; color: #1f2937;">辅助数据源</span>
              </div>
              <div class="card-actions">
                <el-tooltip content="上移" placement="top">
                  <el-button 
                    size="mini" 
                    circle
                    :disabled="index === 0"
                    @click="moveAuxDatasource(index, 'up')">
                    <i class="el-icon-arrow-up"></i>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="下移" placement="top">
                  <el-button 
                    size="mini" 
                    circle
                    :disabled="index === auxiliaryDatasources.length - 1"
                    @click="moveAuxDatasource(index, 'down')">
                    <i class="el-icon-arrow-down"></i>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button 
                    size="mini" 
                    circle
                    type="danger"
                    @click="removeAuxiliaryDatasource(index)">
                    <i class="el-icon-delete"></i>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <!-- 卡片内容 -->
            <div class="card-body">
              <!-- 第一行：别名 + 连接器 -->
              <div class="form-row">
                <div class="form-group" style="flex: 0 0 200px;">
                  <div class="form-label required">
                    <i class="el-icon-price-tag"></i>
                    别名
                  </div>
                  <el-input 
                    v-model="item.alias" 
                    placeholder="如：user" 
                    size="medium" 
                    class="enhanced-input">
                    <i slot="prefix" class="el-icon-edit" style="margin-left: 8px; color: #9ca3af;"></i>
                  </el-input>
                  <div class="form-tip">
                    <i class="el-icon-info"></i>
                    用于字段引用，如 user.name
                  </div>
                </div>
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-connection"></i>
                    连接器
                  </div>
                  <el-select 
                    v-model="item.connectorId" 
                    placeholder="选择连接器" 
                    size="medium" 
                    style="width: 100%" 
                    @change="handleAuxConnectorChange(item)"
                    class="enhanced-select">
                    <el-option 
                      v-for="conn in connectorList" 
                      :key="conn.id" 
                      :label="getConnectorLabel(conn)" 
                      :value="conn.id">
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <!-- Logo图标 -->
                          <img v-if="getConnectorLogo(conn)" 
                            :src="getConnectorLogo(conn)" 
                            style="width: 20px; height: 20px; object-fit: contain;" 
                            :alt="conn.connectorName" />
                          <i v-else class="el-icon-connection" style="font-size: 20px; color: #9ca3af;"></i>
                          <span style="font-weight: 500;">{{ conn.connectorName }}</span>
                        </div>
                        <el-tag size="mini" :type="conn.connectorType === 'DATABASE' ? 'success' : 'warning'" effect="plain">
                          {{ getConnectorTypeLabel(conn) }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                </div>
              </div>

              <!-- 第二行：数据配置 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-document"></i>
                    数据配置
                  </div>
                  <div v-if="item.connectorType === 'DATABASE'" class="sql-editor-wrapper">
                    <!-- SQL编辑器 - 辅助数据源版 -->
                    <div class="sql-editor-enhanced aux-sql-editor">
                      <div class="editor-toolbar">
                        <div class="toolbar-left">
                          <span class="editor-label">
                            <i class="el-icon-edit"></i> SQL编辑器
                          </span>
                          <span v-if="item._sqlError" class="sql-error-badge">
                            <i class="el-icon-warning"></i> {{ item._sqlError }}
                          </span>
                          <span v-else-if="item.config.sql && !item._sqlError" class="sql-valid-badge">
                            <i class="el-icon-success"></i> 语法正常
                          </span>
                        </div>
                        <div class="toolbar-right">
                          <el-button-group size="mini">
                            <el-button @click="formatAuxSql(index)" icon="el-icon-sort">格式化</el-button>
                           <el-button
                            size="mini"
                            type="primary"
                            :loading="sqlValidationLoading"
                            @click="validateAuxSql(index)">
                            SQL语法验证
                          </el-button>
                          <el-alert
                            v-if="sqlValidationError"
                            :title="sqlValidationError"
                            type="error"
                            show-icon
                            :closable="false"
                            style="margin-left: 10px; padding: 0 10px; height: 28px; line-height: 28px;" />
                            <el-button @click="clearAuxSql(index)" icon="el-icon-delete">清空</el-button>
                          </el-button-group>
                        </div>
                      </div>
                      
                      <!-- SQL输入区 -->
                      <div class="sql-input-wrapper">
                        <div class="sql-editor-container">
                          <div class="sql-editor-background" v-html="item._highlightedSql || ''"></div>
                          <textarea 
                            v-model="item.config.sql"
                            class="sql-editor-input"
                            rows="4"
                            placeholder="SELECT id, name, dept_id FROM t_user"
                            @input="updateAuxHighlight(index)"
                            @scroll="syncAuxScroll($event, index)"
                            :ref="'auxSqlTextarea' + index">
                          </textarea>
                        </div>
                      </div>
                      
                      <!-- 底部提示 -->
                      <div class="editor-actions">
                        <div class="actions-left">
                          <span class="tip-text">
                            <i class="el-icon-info"></i>
                            输入 SQL 查询语句，仅支持 SELECT 语句
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div v-else-if="item.connectorType === 'API'" class="api-config">
                    <el-input 
                      v-model="item.config.apiPath" 
                      placeholder="API 路径，如：/api/users" 
                      size="medium" 
                      style="margin-bottom: 10px;"
                      class="enhanced-input">
                      <template slot="prepend">GET</template>
                    </el-input>
                    <el-input 
                      v-model="item.config.dataPath" 
                      placeholder="数据路径，如：data" 
                      size="medium"
                      class="enhanced-input">
                      <i slot="prefix" class="el-icon-position" style="margin-left: 8px; color: #9ca3af;"></i>
                    </el-input>
                  </div>
                </div>
              </div>

              <!-- 第三行：关联条件 -->
              <div class="join-condition-section">
                <div class="section-title">
                  <i class="el-icon-link"></i>
                  关联条件
                </div>
                <div class="form-row">
                  <div class="form-group" style="flex: 1;">
                    <div class="form-label required">
                      主表字段
                    </div>
                    <el-autocomplete
                      v-model="item.joinCondition.mainField"
                      :fetch-suggestions="(query, cb) => queryAvailableFields(index, query, cb)"
                      placeholder="user_id 或 user.dept_id"
                      size="medium"
                      style="width: 100%"
                      class="enhanced-input"
                      :trigger-on-focus="true">
                      <i slot="prefix" class="el-icon-s-grid" style="margin-left: 8px; color: #9ca3af;"></i>
                      <template slot-scope="{ item }">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                          <span style="font-weight: 500;">{{ item.value }}</span>
                          <el-tag size="mini" type="info" effect="plain">{{ item.source }}</el-tag>
                        </div>
                      </template>
                    </el-autocomplete>
                    <div class="form-tip">
                      <i class="el-icon-info"></i>
                      支持引用已配置的辅助数据源字段
                    </div>
                  </div>
                  <div class="join-operator">
                    <div class="operator-icon">=</div>
                  </div>
                  <div class="form-group" style="flex: 1;">
                    <div class="form-label required">
                      辅助表字段
                    </div>
                    <el-input 
                      v-model="item.joinCondition.auxField" 
                      placeholder="id" 
                      size="medium"
                      class="enhanced-input">
                      <i slot="prefix" class="el-icon-s-grid" style="margin-left: 8px; color: #9ca3af;"></i>
                    </el-input>
                  </div>
                  <div class="form-group" style="flex: 0 0 140px;">
                    <div class="form-label required">
                      关联类型
                    </div>
                    <el-select 
                      v-model="item.joinType" 
                      size="medium" 
                      style="width: 100%"
                      class="enhanced-select">
                      <el-option label="LEFT JOIN" value="LEFT">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-back" style="margin-right: 6px; color: #3b82f6;"></i>
                          <span>LEFT</span>
                        </div>
                      </el-option>
                      <el-option label="INNER JOIN" value="INNER">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-finished" style="margin-right: 6px; color: #10b981;"></i>
                          <span>INNER</span>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-state">
          <div class="empty-icon">
            <i class="el-icon-folder-opened"></i>
          </div>
          <h4>暂无辅助数据源</h4>
          <p>点击"添加辅助数据源"按钮开始配置，或直接跳过该步骤</p>
          <el-button type="primary" size="medium" @click="addAuxiliaryDatasource" style="margin-top: 16px;">
            <i class="el-icon-plus"></i>
            添加第一个数据源
          </el-button>
        </div>
      </div>

      <!-- 步骤3: 选择目标 -->
      <div v-show="activeStep === 3" class="target-config-step">
        <!-- 页面头部 -->
        <div class="step-header">
          <div class="header-title">
            <i class="el-icon-download" style="font-size: 24px; color: #10b981; margin-right: 12px;"></i>
            <div>
              <h3 style="margin: 0; font-size: 18px; font-weight: 600; color: #1f2937;">目标连接器配置</h3>
              <p style="margin: 4px 0 0; font-size: 13px; color: #6b7280;">选择数据目标并配置写入规则</p>
            </div>
          </div>
        </div>

        <!-- 目标模式选择卡片 -->
        <div class="mode-selector-card">
          <div class="mode-selector-header">
            <i class="el-icon-s-operation"></i>
            <span>目标模式选择</span>
          </div>
          <div class="mode-selector-body">
            <div 
              class="mode-option" 
              :class="{'active': !multiTargetMode}"
              @click="multiTargetMode = false; handleTargetModeChange()">
              <div class="mode-icon">
                <i class="el-icon-files"></i>
              </div>
              <div class="mode-content">
                <h4>单目标模式</h4>
                <p>数据推送到一个目标表（兼容旧版）</p>
              </div>
              <div class="mode-check">
                <i class="el-icon-check"></i>
              </div>
            </div>
            <div 
              class="mode-option" 
              :class="{'active': multiTargetMode}"
              @click="multiTargetMode = true; handleTargetModeChange()">
              <div class="mode-icon">
                <i class="el-icon-document-copy"></i>
              </div>
              <div class="mode-content">
                <h4>多目标模式</h4>
                <p>数据推送到多个目标表（支持跨数据源）</p>
              </div>
              <div class="mode-check">
                <i class="el-icon-check"></i>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 单目标模式配置 -->
        <div v-if="!multiTargetMode" class="single-target-config">
          <!-- 连接器选择卡片 -->
          <div class="config-card">
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-connection"></i>
                <span>连接器选择</span>
              </div>
            </div>
            <div class="card-body">
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-download"></i>
                    目标连接器
                  </div>
                  <el-select 
                    v-model="targetConfig.connectorId" 
                    placeholder="请选择目标连接器" 
                    size="medium"
                    style="width: 100%" 
                    @change="handleTargetConnectorChange"
                    class="enhanced-select">
                    <el-option 
                      v-for="item in connectorList" 
                      :key="item.id" 
                      :label="getConnectorLabel(item)" 
                      :value="item.id">
                      <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <!-- Logo图标 -->
                          <img v-if="getConnectorLogo(item)" 
                            :src="getConnectorLogo(item)" 
                            style="width: 20px; height: 20px; object-fit: contain;" 
                            :alt="item.connectorName" />
                          <i v-else class="el-icon-connection" style="font-size: 20px; color: #9ca3af;"></i>
                          <span style="font-weight: 500;">{{ item.connectorName }}</span>
                        </div>
                        <el-tag size="mini" :type="item.connectorType === 'DATABASE' ? 'success' : 'warning'" effect="plain">
                          {{ getConnectorTypeLabel(item) }}
                        </el-tag>
                      </div>
                    </el-option>
                  </el-select>
                  <div class="form-tip">
                    <i class="el-icon-info"></i>
                    选择已配置的目标连接器，或
                    <el-button type="text" @click="$router.push('/connector')" style="padding: 0; margin-left: 4px">新建连接器</el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 数据库类型目标配置 -->
          <div v-if="targetConnectorType === 'DATABASE'" class="config-card">
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-document"></i>
                <span>数据库配置</span>
              </div>
            </div>
            <div class="card-body">
             <!-- 目标表 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-s-grid"></i>
                    目标表
                  </div>
                  <div style="display: flex; gap: 12px; align-items: center;">
                    <el-button 
                      type="primary" 
                      size="medium" 
                      @click="handleLoadTargetTables"
                      :disabled="!targetConfig.connectorId"
                      icon="el-icon-refresh">
                      加载表列表
                    </el-button>
                    <el-select 
                      v-model="targetConfig.tableName" 
                      placeholder="请选择目标表" 
                      size="medium"
                      style="flex: 1;" 
                      @change="handleTargetTableSelect"
                      clearable
                      filterable
                      class="enhanced-select">
                      <el-option 
                        v-for="table in targetTableList" 
                        :key="table.tableName" 
                        :label="table.tableName" 
                        :value="table.tableName">
                        <div style="display: flex; align-items: center; gap: 8px;">
                          <i class="el-icon-s-grid" style="color: #10b981;"></i>
                          <span style="font-weight: 500; color: #303133;">{{ table.tableName }}</span>
                          <span v-if="table.tableComment" style="color: #909399; font-size: 12px; margin-left: auto;">{{ table.tableComment }}</span>
                        </div>
                      </el-option>
                    </el-select>
                  </div>
                  <div class="form-tip">
                    <i class="el-icon-info"></i>
                    请先点击"加载表列表"按钮，然后从下拉框中选择目标表
                  </div>
                </div>
              </div>

              <!-- 写入模式 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-edit"></i>
                    写入模式
                  </div>
                  <div class="write-mode-selector">
                    <div 
                      class="write-mode-item" 
                      :class="{'active': targetConfig.writeMode === 'INSERT'}"
                      @click="targetConfig.writeMode = 'INSERT'">
                      <div class="mode-icon-sm">
                        <i class="el-icon-circle-plus"></i>
                      </div>
                      <div class="mode-info-sm">
                        <h5>INSERT</h5>
                        <p>仅插入新数据，重复数据会报错</p>
                      </div>
                      <div class="mode-check-sm">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                    <div 
                      class="write-mode-item" 
                      :class="{'active': targetConfig.writeMode === 'UPDATE'}"
                      @click="targetConfig.writeMode = 'UPDATE'">
                      <div class="mode-icon-sm">
                        <i class="el-icon-refresh"></i>
                      </div>
                      <div class="mode-info-sm">
                        <h5>UPDATE</h5>
                        <p>根据主键更新已存在的数据</p>
                      </div>
                      <div class="mode-check-sm">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                    <div 
                      class="write-mode-item" 
                      :class="{'active': targetConfig.writeMode === 'UPSERT'}"
                      @click="targetConfig.writeMode = 'UPSERT'">
                      <div class="mode-icon-sm">
                        <i class="el-icon-finished"></i>
                      </div>
                      <div class="mode-info-sm">
                        <h5>UPSERT</h5>
                        <p>存在则更新，不存在则插入</p>
                      </div>
                      <div class="mode-check-sm">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                  </div>
                  
                  <!-- INSERT模式性能优化提示 -->
                  <div v-if="targetConfig.writeMode === 'INSERT'" class="form-tip success" style="margin-top: 12px;">
                    <i class="el-icon-lightning"></i>
                    <strong>高性能模式已启用！</strong>INSERT模式将使用阶段4高性能加载器（LOAD DATA/COPY），性能提升30-150倍
                  </div>
                  
                  <!-- UPDATE/UPSERT模式优化提示 -->
                  <div v-else class="form-tip" style="margin-top: 12px;">
                    <i class="el-icon-info"></i>
                    {{ targetConfig.writeMode }}模式将使用阶段1-3优化（批量提交+并行写入+连接池），性能提升15-60倍
                  </div>
                </div>
              </div>

              <!-- 主键字段 -->
              <div v-if="targetConfig.writeMode !== 'INSERT'" class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-key"></i>
                    主键字段
                  </div>
                  <el-input 
                    v-model="targetConfig.primaryKey" 
                    placeholder="请输入主键字段，多个用逗号分隔" 
                    size="medium"
                    class="enhanced-input">
                    <i slot="prefix" class="el-icon-key" style="margin-left: 8px; color: #9ca3af;"></i>
                  </el-input>
                  <div class="form-tip">
                    <i class="el-icon-info"></i>
                    用于判断数据是否存在，如：<code class="inline-code">id</code> 或 <code class="inline-code">user_id,dept_id</code>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- API类型目标配置 -->
          <div v-else-if="targetConnectorType === 'API'" class="config-card">
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-link"></i>
                <span>API配置</span>
              </div>
            </div>
            <div class="card-body">
              <!-- 请求配置 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-link"></i>
                    请求配置
                  </div>
                  <div style="display: flex; gap: 12px; margin-bottom: 12px;">
                    <el-select v-model="targetConfig.apiMethod" size="medium" style="width: 140px;" class="enhanced-select">
                      <el-option label="POST" value="POST">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-upload2" style="margin-right: 6px; color: #10b981;"></i>
                          <span>POST</span>
                        </div>
                      </el-option>
                      <el-option label="PUT" value="PUT">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-edit" style="margin-right: 6px; color: #3b82f6;"></i>
                          <span>PUT</span>
                        </div>
                      </el-option>
                      <el-option label="PATCH" value="PATCH">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-edit-outline" style="margin-right: 6px; color: #f59e0b;"></i>
                          <span>PATCH</span>
                        </div>
                      </el-option>
                      <el-option label="DELETE" value="DELETE">
                        <div style="display: flex; align-items: center;">
                          <i class="el-icon-delete" style="margin-right: 6px; color: #ef4444;"></i>
                          <span>DELETE</span>
                        </div>
                      </el-option>
                    </el-select>
                    <el-input 
                      v-model="targetConfig.apiPath" 
                      placeholder="/api/users" 
                      size="medium"
                      style="flex: 1;"
                      class="enhanced-input">
                      <template slot="prepend">{{ targetConnectorBaseUrl }}</template>
                    </el-input>
                  </div>
                  <div class="form-tip">
                    <i class="el-icon-info"></i>
                    完整URL：<code class="inline-code">{{ targetConnectorBaseUrl }}{{ targetConfig.apiPath }}</code>
                  </div>
                </div>
              </div>

              <!-- Postman风格标签页 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <el-form :model="targetConfig" label-width="0">
                    <el-form-item>
                      <el-tabs v-model="targetApiActiveTab" type="border-card">
                        <!-- Params标签页 -->
                        <el-tab-pane label="Params" name="params">
                          <div style="margin-bottom: 8px">
                            <el-button size="mini" type="primary" @click="addTargetApiParam">+ 添加参数</el-button>
                          </div>
                          <el-table :data="targetConfig.apiParams || []" size="mini" border max-height="250">
                            <el-table-column label="启用" width="60" align="center">
                              <template slot-scope="scope">
                                <el-checkbox v-model="scope.row.enabled" />
                              </template>
                            </el-table-column>
                            <el-table-column label="Key" width="180">
                              <template slot-scope="scope">
                                <el-input v-model="scope.row.key" placeholder="status" size="mini" />
                              </template>
                            </el-table-column>
                            <el-table-column label="Value" min-width="180">
                              <template slot-scope="scope">
                                <el-input v-model="scope.row.value" placeholder="active" size="mini" />
                              </template>
                            </el-table-column>
                            <el-table-column label="Description" min-width="150">
                              <template slot-scope="scope">
                                <el-input v-model="scope.row.description" placeholder="说明" size="mini" />
                              </template>
                            </el-table-column>
                            <el-table-column label="操作" width="60" align="center">
                              <template slot-scope="scope">
                                <el-button type="text" size="mini" @click="removeTargetApiParam(scope.$index)" style="color: #F56C6C">删除</el-button>
                              </template>
                            </el-table-column>
                          </el-table>
                          <div class="tip" style="margin-top: 8px">Query参数会追加到URL: ?key1=value1&key2=value2</div>
                        </el-tab-pane>
                        
                        <!-- Headers标签页 -->
                        <el-tab-pane name="headers">
                          <span slot="label">
                            Headers
                            <el-badge :value="(targetConfig.headers || []).filter(h => h.enabled).length" :hidden="!(targetConfig.headers || []).filter(h => h.enabled).length" style="margin-left: 4px" />
                          </span>
                          <div style="margin-bottom: 8px">
                            <el-button size="mini" type="primary" @click="addTargetHeader">+ 添加Header</el-button>
                          </div>
                          <el-table :data="targetConfig.headers || []" size="mini" border max-height="200">
                            <el-table-column label="启用" width="60" align="center">
                              <template slot-scope="scope">
                                <el-checkbox v-model="scope.row.enabled" />
                              </template>
                            </el-table-column>
                            <el-table-column label="Key" width="180">
                              <template slot-scope="scope">
                                <el-input v-model="scope.row.key" placeholder="Content-Type" size="mini" />
                              </template>
                            </el-table-column>
                            <el-table-column label="Value" min-width="200">
                              <template slot-scope="scope">
                                <el-input v-model="scope.row.value" placeholder="application/json" size="mini" />
                              </template>
                            </el-table-column>
                            <el-table-column label="操作" width="60" align="center">
                              <template slot-scope="scope">
                                <el-button type="text" size="mini" @click="removeTargetHeader(scope.$index)" style="color: #F56C6C">删除</el-button>
                              </template>
                            </el-table-column>
                          </el-table>
                          <div class="tip" style="margin-top: 8px">自定义请求头(会覆盖连接器公共Headers)</div>
                        </el-tab-pane>
                      </el-tabs>
                    </el-form-item>
                  </el-form>
                </div>
              </div>
            </div>
          </div>
          
          <!-- API推送配置卡片 -->
          <div v-if="targetConnectorType === 'API'" class="config-card">
            <div class="card-header">
              <div class="card-title">
                <i class="el-icon-setting"></i>
                <span>推送配置</span>
              </div>
            </div>
            <div class="card-body">
              <!-- 推送模式 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="form-label required">
                    <i class="el-icon-upload"></i>
                    推送模式
                  </div>
                  <div class="write-mode-selector">
                    <div 
                      class="write-mode-item" 
                      :class="{'active': targetConfig.batchMode === 'batch'}"
                      @click="targetConfig.batchMode = 'batch'">
                      <div class="mode-icon-sm">
                        <i class="el-icon-document-copy"></i>
                      </div>
                      <div class="mode-info-sm">
                        <h5>批量推送</h5>
                        <p>将所有数据打包为数组一次性发送</p>
                      </div>
                      <div class="mode-check-sm">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                    <div 
                      class="write-mode-item" 
                      :class="{'active': targetConfig.batchMode === 'single'}"
                      @click="targetConfig.batchMode = 'single'">
                      <div class="mode-icon-sm">
                        <i class="el-icon-document"></i>
                      </div>
                      <div class="mode-info-sm">
                        <h5>逐条推送</h5>
                        <p>每条数据分别调用API</p>
                      </div>
                      <div class="mode-check-sm">
                        <i class="el-icon-check"></i>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 批量配置 -->
              <div v-if="targetConfig.batchMode === 'batch'" class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="config-panel">
                    <div class="panel-header">
                      <i class="el-icon-setting"></i>
                      <span>批量配置</span>
                    </div>
                    <div class="panel-body">
                      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div>
                          <label class="panel-label">批次大小：</label>
                          <el-input-number v-model="targetConfig.batchSize" :min="1" :max="1000" size="small" style="width: 100%" />
                          <div class="panel-tip">每批次发送的数据条数</div>
                        </div>
                        <div>
                          <label class="panel-label">数据包裹字段(可选)：</label>
                          <el-input v-model="targetConfig.wrapperField" placeholder="data" size="small" />
                          <div class="panel-tip">如: {"data":[...]},不填则直接发送数组</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 重试配置 -->
              <div class="form-row">
                <div class="form-group" style="flex: 1;">
                  <div class="config-panel">
                    <div class="panel-header">
                      <i class="el-icon-refresh"></i>
                      <span>重试配置</span>
                    </div>
                    <div class="panel-body">
                      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px;">
                        <div>
                          <label class="panel-label">失败重试次数：</label>
                          <el-input-number v-model="targetConfig.retryTimes" :min="0" :max="10" size="small" style="width: 100%" />
                        </div>
                        <div>
                          <label class="panel-label">重试间隔(毫秒)：</label>
                          <el-input-number v-model="targetConfig.retryInterval" :min="100" :max="10000" :step="100" size="small" style="width: 100%" />
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 多目标模式（新增） -->
        <div v-else>
          <div style="margin-bottom: 16px">
            <el-button type="primary" icon="el-icon-plus" @click="addTarget">
              添加目标表
            </el-button>
            <span style="margin-left: 12px; color: #909399; font-size: 13px">
              已添加 <strong style="color: #409EFF">{{ targets.length }}</strong> 个目标表
            </span>
          </div>
          
          <!-- 目标列表 -->
          <el-collapse v-model="activeTargetCollapseIndex" accordion>
            <el-collapse-item 
              v-for="(target, index) in targets" 
              :key="index"
              :name="index">
              
              <!-- 标题 -->
              <template slot="title">
                <div style="display: flex; align-items: center; width: 100%; padding: 0 12px">
                  <i class="el-icon-document" style="margin-right: 8px; color: #409EFF; font-size: 16px"></i>
                  <span style="font-weight: bold; flex: 1">
                    目标{{ index + 1 }}: {{ target.targetName || '未命名' }}
                  </span>
                  <el-button 
                    type="danger" 
                    size="mini" 
                    icon="el-icon-delete"
                    @click.stop="removeTarget(index)">
                    删除
                  </el-button>
                </div>
              </template>
              
              <!-- 配置内容 -->
              <div style="padding: 20px; background: #F5F7FA">
                <el-form label-width="120px">
                  <!-- 目标名称 -->
                  <el-form-item label="目标名称" required>
                    <el-input 
                      v-model="target.targetName" 
                      placeholder="如：用户主表、用户扩展表" 
                      style="width: 400px" />
                    <div class="tip">用于区分不同的目标表</div>
                  </el-form-item>
                  
                  <!-- 目标连接器 -->
                  <el-form-item label="目标连接器" required>
                    <el-select 
                      v-model="target.targetConnectorId" 
                      @change="handleMultiTargetConnectorChange(index)"
                      style="width: 400px"
                      filterable>
                      <el-option 
                        v-for="conn in connectorList.filter(c => c.connectorType === 'DATABASE')" 
                        :key="conn.id" 
                        :label="conn.connectorName + ' (' + (conn.host || '') + ':' + (conn.port || '') + ')'" 
                        :value="conn.id" />
                    </el-select>
                  </el-form-item>
                  
                  <!-- 目标表 -->
                  <el-form-item label="目标表名" required>
                    <el-input 
                      v-model="target.targetConfig.tableName" 
                      placeholder="请输入目标表名或从下拉选择" 
                      style="width: 400px" />
                    <div v-if="target.targetConnectorId" style="margin-top: 10px">
                      <el-button 
                        type="primary" 
                        size="small" 
                        :loading="target._loadingTables"
                        @click="handleLoadMultiTargetTables(index)">
                        <i class="el-icon-refresh"></i> 加载目标表列表
                      </el-button>
                      <el-select 
                        v-model="target.targetConfig.tableName" 
                        placeholder="请选择目标表" 
                        filterable
                        style="width: 300px; margin-left: 10px">
                        <el-option 
                          v-for="table in (target._tableList || [])" 
                          :key="table.tableName" 
                          :label="table.tableName" 
                          :value="table.tableName">
                          <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span style="font-weight: 500; color: #303133;">{{ table.tableName }}</span>
                            <span v-if="table.tableComment" style="color: #909399; font-size: 12px; margin-left: 12px;">{{ table.tableComment }}</span>
                          </div>
                        </el-option>
                      </el-select>
                    </div>
                    <div class="tip" v-if="target._tableList && target._tableList.length" style="margin-top: 5px">
                      也可直接输入新表名以在目标库创建
                    </div>
                  </el-form-item>
                  
                  <!-- 写入模式 -->
                  <el-form-item label="写入模式">
                    <el-select v-model="target.targetConfig.writeMode" style="width: 200px">
                      <el-option label="INSERT（插入）" value="INSERT" />
                      <el-option label="UPDATE（更新）" value="UPDATE" />
                      <el-option label="UPSERT（插入或更新）" value="UPSERT" />
                    </el-select>
                  </el-form-item>
                  
                  <!-- 批次大小 -->
                  <el-form-item label="批次大小">
                    <el-input-number 
                      v-model="target.targetConfig.batchSize" 
                      :min="100" 
                      :max="10000" 
                      :step="100" />
                    <span style="margin-left: 8px; color: #909399">条/批</span>
                  </el-form-item>
                  
                  <!-- 重试次数 -->
                  <el-form-item label="重试次数">
                    <el-input-number 
                      v-model="target.targetConfig.maxRetries" 
                      :min="0" 
                      :max="10" />
                  </el-form-item>
                  
                  <!-- 幂等键 -->
                  <el-form-item label="幂等键">
                    <el-input 
                      v-model="target.targetConfig.idempotentKey" 
                      placeholder="user_id" 
                      style="width: 200px" />
                    <div class="tip">用于去重，留空则不去重</div>
                  </el-form-item>
                </el-form>
              </div>
            </el-collapse-item>
          </el-collapse>
          
          <!-- 提示信息 -->
          <el-alert
            v-if="targets.length === 0"
            title="请至少添加一个目标表"
            type="warning"
            :closable="false"
            style="margin-top: 16px" />
        </div>
      </div>

      <!-- 步骤4: 数据转换 -->
      <div v-show="activeStep === 4" class="step-content-mapping">
        <!-- 多目标模式：添加预览标签页 -->
        <div v-if="multiTargetMode && targets.length > 0">
          <el-alert
            title="多目标字段映射配置"
            type="info"
            description="为每个目标表分别配置字段映射规则。每个目标表可以映射不同的字段。"
            :closable="false"
            style="margin-bottom: 20px">
          </el-alert>
          
          <!-- 添加预览标签页 -->
          <el-tabs v-model="transformTab" type="border-card" style="margin-bottom: 20px">
            <!-- 已抽取数据 -->
            <el-tab-pane label="已抽取数据" name="before">
              <div v-if="sourcePreviewData.length === 0" style="text-align: center; padding: 40px 0; color: #909399">
                <i class="el-icon-warning" style="font-size: 48px"></i>
                <div style="margin-top: 15px">暂无已抽取的数据</div>
                <div style="margin-top: 8px; font-size: 13px">请在“选择数据源”步骤点击“抽取数据”按钮</div>
                <el-button type="primary" size="small" @click="activeStep = 1" style="margin-top: 15px">
                  <i class="el-icon-back"></i> 返回数据源步骤
                </el-button>
              </div>
              <div v-else>
                <div style="margin-bottom: 10px; color: #606266">
                  <i class="el-icon-info"></i> 以下是从源端抽取到的原始数据（共 <strong style="color: #409EFF">{{ sourcePreviewData.length }}</strong> 条）
                </div>
                <el-table :data="sourcePreviewData" border size="small" max-height="400" stripe>
                  <el-table-column type="index" label="#" width="50" />
                  <el-table-column 
                    v-for="col in sourcePreviewColumns" 
                    :key="col" 
                    :prop="col" 
                    :label="col" 
                    min-width="120"
                    show-overflow-tooltip>
                    <template slot-scope="scope">
                      <span>{{ formatCellValue(scope.row[col]) }}</span>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
            
            <!-- 配置转换规则 -->
            <el-tab-pane label="配置转换规则" name="config">
              <div class="tip" style="margin-bottom: 15px; background: #fff3cd; padding: 12px; border-radius: 4px; color: #856404">
                <i class="el-icon-warning"></i> <strong>提示：</strong>配置完成后，可切换到“转换后数据预览”标签查看转换效果
              </div>
              
              <!-- 目标列表标签页 -->
              <el-tabs v-model="activeTargetTab" type="border-card">
                <el-tab-pane 
                  v-for="(target, index) in targets" 
                  :key="index"
                  :name="String(index)">
                  <span slot="label">
                    <i class="el-icon-document"></i> {{ target.targetName || '目标' + (index + 1) }}
                    <el-badge 
                      :value="getTargetMappingCount(index)" 
                      :hidden="getTargetMappingCount(index) === 0"
                      style="margin-left: 8px" />
                  </span>
                  
                  <!-- 目标表的字段映射配置 -->
                  <div style="padding: 16px">
                    <div style="margin-bottom: 16px">
                      <el-button type="success" size="small" icon="el-icon-magic-stick" @click="handleAutoMappingForTarget(index)">
                        智能映射
                      </el-button>
                      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddMappingForTarget(index)">
                        添加字段映射
                      </el-button>
                      <el-button type="warning" size="small" icon="el-icon-delete" @click="handleClearTargetMappings(index)">
                        清空映射
                      </el-button>
                      <span style="margin-left: 16px; color: #909399">
                        已配置 <strong style="color: #409EFF">{{ getTargetMappingCount(index) }}</strong> 个字段
                      </span>
                    </div>
                
                <!-- 字段映射表格 -->
                <el-table 
                  :data="getTargetMappings(index)" 
                  border 
                  stripe 
                  size="small"
                  style="width: 100%">
                  <el-table-column type="index" label="#" width="50" align="center" />
                  
                  <el-table-column label="源字段" min-width="150">
                    <template slot-scope="scope">
                      <el-autocomplete
                        v-model="scope.row.sourceField"
                        :fetch-suggestions="querySourceFields"
                        placeholder="请输入源字段名"
                        style="width: 100%"
                        size="small"
                        :trigger-on-focus="true"
                        @input="handleSourceFieldInput(scope.row)"
                        @select="handleSourceFieldSelect(scope.row, $event)"
                        @blur="handleSourceFieldBlur(scope.row)">
                        <template slot-scope="{ item }">
                          <div style="padding: 6px 10px; margin: 2px 0; border-radius: 4px; background-color: #fff; border: 1px solid #f0f2f5; box-sizing: border-box;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                              <span style="font-weight: 600; color: #303133; font-size: 13px;">{{ item.value }}</span>
                              <div style="display: flex; align-items: center; gap: 6px; font-size: 12px;">
                                <span v-if="item.nullable === false" style="color: #F56C6C;">★必填</span>
                                <span style="color: #909399;">
                                  {{ item.type || '字段' }}<span v-if="item.columnSize">({{ item.columnSize }})</span>
                                </span>
                              </div>
                            </div>
                            <div v-if="item.remarks" style="color: #909399; font-size: 12px; line-height: 1.4; max-height: 2.8em; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">
                              {{ item.remarks }}
                            </div>
                          </div>
                        </template>
                      </el-autocomplete>
                      <div v-if="scope.row.sourceType || scope.row.sourceNullable === false || scope.row.sourceRemarks || scope.row.sourceColumnSize" style="margin-top: 4px; display: flex; gap: 4px; flex-wrap: wrap">
                        <el-tag v-if="scope.row.sourceType" size="mini" type="info" effect="plain">
                          {{ scope.row.sourceType }}<span v-if="scope.row.sourceColumnSize">({{ scope.row.sourceColumnSize }})</span>
                        </el-tag>
                        <el-tag v-if="scope.row.sourceNullable === false" size="mini" type="danger" effect="plain">★必填</el-tag>
                        <el-tooltip v-if="scope.row.sourceRemarks" :content="scope.row.sourceRemarks" placement="top">
                          <el-tag size="mini" type="success" effect="plain">
                            <i class="el-icon-info"></i> {{ scope.row.sourceRemarks.length > 20 ? scope.row.sourceRemarks.substring(0, 20) + '...' : scope.row.sourceRemarks }}
                          </el-tag>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-table-column>
                  
                  <el-table-column width="50" align="center">
                    <template>
                      <i class="el-icon-right" style="font-size: 16px; color: #909399"></i>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="目标字段" min-width="150">
                    <template slot-scope="scope">
                      <el-autocomplete
                        v-model="scope.row.targetField"
                        :fetch-suggestions="(queryString, cb) => queryMultiTargetFields(index, queryString, cb)"
                        placeholder="请输入目标字段名"
                        style="width: 100%"
                        size="small"
                        :trigger-on-focus="true"
                        @input="handleTargetFieldInput(scope.row)"
                        @select="handleTargetFieldSelect(scope.row, $event)"
                        @blur="handleTargetFieldBlur(scope.row)">
                        <template slot-scope="{ item }">
                          <div style="padding: 6px 10px; margin: 2px 0; border-radius: 4px; background-color: #fff; border: 1px solid #f0f2f5; box-sizing: border-box;">
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                              <span style="font-weight: 600; color: #303133; font-size: 13px;">{{ item.value }}</span>
                              <div style="display: flex; align-items: center; gap: 6px; font-size: 12px;">
                                <span v-if="item.nullable === false" style="color: #F56C6C;">★必填</span>
                                <span style="color: #909399;">
                                  {{ item.type || '字段' }}<span v-if="item.columnSize">({{ item.columnSize }})</span>
                                </span>
                              </div>
                            </div>
                            <div v-if="item.remarks" style="color: #909399; font-size: 12px; line-height: 1.4; max-height: 2.8em; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">
                              {{ item.remarks }}
                            </div>
                          </div>
                        </template>
                      </el-autocomplete>
                      <div v-if="scope.row.targetType || scope.row.targetNullable === false || scope.row.targetRemarks || scope.row.targetColumnSize" style="margin-top: 4px; display: flex; gap: 4px; flex-wrap: wrap">
                        <el-tag v-if="scope.row.targetType" size="mini" type="info" effect="plain">
                          {{ scope.row.targetType }}<span v-if="scope.row.targetColumnSize">({{ scope.row.targetColumnSize }})</span>
                        </el-tag>
                        <el-tag v-if="scope.row.targetNullable === false" size="mini" type="danger" effect="plain">★必填</el-tag>
                        <el-tooltip v-if="scope.row.targetRemarks" :content="scope.row.targetRemarks" placement="top">
                          <el-tag size="mini" type="success" effect="plain">
                            <i class="el-icon-info"></i> {{ scope.row.targetRemarks.length > 20 ? scope.row.targetRemarks.substring(0, 20) + '...' : scope.row.targetRemarks }}
                          </el-tag>
                        </el-tooltip>
                      </div>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="数据处理流程" min-width="350">
                    <template slot-scope="scope">
                      <div v-if="!scope.row._processors || scope.row._processors.length === 0" style="color: #67C23A; font-size: 12px">
                        <i class="el-icon-success"></i> 直接映射
                      </div>
                      <div v-else style="display: flex; flex-wrap: wrap; gap: 6px; align-items: center; padding: 8px 0">
                        <div
                          v-for="(proc, idx) in scope.row._processors" 
                          :key="idx"
                          draggable
                          @dragstart="handleDragStart(scope.row, idx, $event)"
                          @dragover.prevent
                          @drop="handleDrop(scope.row, idx, $event)"
                          @dragenter="handleDragEnter($event)"
                          @dragleave="handleDragLeave($event)"
                          class="processor-item"
                          :class="{ 'dragging': draggedProcessorIndex === idx && draggedMappingRef === scope.row }"
                          style="position: relative; display: inline-flex; align-items: center; gap: 0; padding: 4px 10px 4px 0; margin: 4px 0; border-radius: 4px; transition: all 0.2s">
                          
                          <!-- 拖拽手柄 -->
                          <div class="drag-handle" style="cursor: move; padding: 0 6px; color: #909399; display: flex; align-items: center">
                            <i class="el-icon-rank" style="font-size: 14px"></i>
                          </div>
                          
                          <!-- 处理器标签 -->
                          <el-tooltip
                            :content="getProcessorDetailTextWithMapping(scope.row, proc)"
                            placement="top">
                            <el-tag 
                              :type="getProcessorTagType(proc.type)"
                              size="small"
                              style="cursor: pointer; margin: 0"
                              @click="configureProcessor(scope.row, proc, idx)">
                              <i :class="getProcessorIcon(proc.type)"></i>
                              {{ idx + 1 }}. {{ getProcessorTypeName(proc.type) }}{{ getProcessorSubtypeTextWithMapping(scope.row, proc) }}
                              <i v-if="isProcessorActuallyConfigured(scope.row, proc)" class="el-icon-circle-check" style="margin-left: 3px; color: #67C23A"></i>
                            </el-tag>
                          </el-tooltip>
                          
                          <!-- 删除按钮 -->
                          <el-button 
                            class="delete-btn"
                            type="danger" 
                            icon="el-icon-close" 
                            size="mini" 
                            circle
                            @click.stop="removeProcessor(scope.row, idx)"
                            style="position: absolute; top: -6px; right: -6px; width: 20px; height: 20px; padding: 0; font-size: 12px; opacity: 0; transition: opacity 0.2s; z-index: 10; border: 2px solid #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.12)"></el-button>
                        </div>
                      </div>
                    </template>
                  </el-table-column>
                  
                  <el-table-column label="操作" width="160" align="center" fixed="right">
                    <template slot-scope="scope">
                      <el-button 
                        size="mini" 
                        type="primary"
                        icon="el-icon-plus"
                        @click="showAddProcessorDialog(scope.row)">添加</el-button>
                      <el-button 
                        size="mini" 
                        type="danger" 
                        icon="el-icon-delete"
                        @click="handleDeleteTargetMapping(index, scope.$index)"></el-button>
                    </template>
                  </el-table-column>
                </el-table>
                
                <div v-if="getTargetMappingCount(index) === 0" 
                     style="text-align: center; padding: 40px 0; color: #909399">
                  <i class="el-icon-document" style="font-size: 48px"></i>
                  <div style="margin-top: 12px">暂无字段映射，请点击上方按钮添加</div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
        
        <!-- 转换后数据预览 -->
        <el-tab-pane label="转换后数据预览" name="after">
          <div v-if="mappings.length === 0" style="text-align: center; padding: 40px 0; color: #909399">
            <i class="el-icon-warning" style="font-size: 48px"></i>
            <div style="margin-top: 15px">暂无转换规则</div>
            <div style="margin-top: 8px; font-size: 13px">请在"配置转换规则"标签中添加字段映射</div>
            <el-button type="primary" size="small" @click="transformTab = 'config'" style="margin-top: 15px">
              <i class="el-icon-setting"></i> 前往配置
            </el-button>
          </div>
          <div v-else>
            <div class="tip" style="margin-bottom: 15px; background: #e7f4ff; padding: 12px; border-radius: 4px; color: #0056b3">
              <i class="el-icon-info"></i> <strong>提示：</strong>下方为每个目标表分别展示转换后的数据预览
            </div>
                    
            <!-- 按目标展示转换数据 -->
            <el-tabs v-model="activeTargetPreviewTab" type="border-card">
              <el-tab-pane 
                v-for="(target, index) in targets" 
                :key="index"
                :name="String(index)">
                <span slot="label">
                  <i class="el-icon-document"></i> {{ target.targetName || '目标' + (index + 1) }}
                  <el-badge 
                    :value="getTargetMappingCount(index)" 
                    :hidden="getTargetMappingCount(index) === 0"
                    style="margin-left: 8px" />
                </span>
                        
                <!-- 目标表的转换数据预览 -->
                <div style="padding: 16px">
                  <div v-if="getTargetMappingCount(index) === 0" style="text-align: center; padding: 40px 0; color: #909399">
                    <i class="el-icon-warning" style="font-size: 48px"></i>
                    <div style="margin-top: 15px">该目标暂无字段映射</div>
                    <div style="margin-top: 8px; font-size: 13px">请先在"配置转换规则"标签中为该目标添加字段映射</div>
                    <el-button type="primary" size="small" @click="transformTab = 'config'; activeTargetTab = String(index)" style="margin-top: 15px">
                      <i class="el-icon-setting"></i> 前往配置
                    </el-button>
                  </div>
                  <div v-else-if="!getTargetTransformedData(index) || getTargetTransformedData(index).length === 0" style="text-align: center; padding: 40px 0">
                    <div style="margin-bottom: 20px; color: #606266">
                      <i class="el-icon-info"></i> 点击下方按钮预览该目标的转换效果
                    </div>
                    <el-button type="primary" size="large" @click="handlePreviewTargetTransform(index)" :loading="transformPreviewLoading">
                      <i class="el-icon-view"></i> 预览转换效果
                    </el-button>
                    <div class="tip" style="margin-top: 15px">
                      <i class="el-icon-info"></i> 将对源数据应用该目标的转换规则，生成预览结果
                    </div>
                  </div>
                  <div v-else>
                    <div style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center">
                      <div style="color: #606266">
                        <i class="el-icon-success" style="color: #67C23A"></i> 
                        转换后数据（共 <strong style="color: #409EFF">{{ getTargetTransformedData(index).length }}</strong> 条，
                        字段 <strong style="color: #409EFF">{{ getTargetTransformedColumns(index).length }}</strong> 个）
                      </div>
                      <el-button type="primary" size="small" @click="handlePreviewTargetTransform(index)" :loading="transformPreviewLoading">
                        <i class="el-icon-refresh"></i> 重新预览
                      </el-button>
                    </div>
                    <el-table :data="getTargetTransformedData(index)" border size="small" max-height="400" stripe>
                      <el-table-column type="index" label="#" width="50" />
                      <el-table-column 
                        v-for="col in getTargetTransformedColumns(index)" 
                        :key="col" 
                        :prop="col" 
                        :label="col" 
                        min-width="120"
                        show-overflow-tooltip>
                        <template slot-scope="scope">
                          <span :style="getFieldChangeStyleForTarget(index, col, scope.$index)">{{ scope.row[col] }}</span>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-tab-pane>
          </el-tabs>
        </div>
        
        <!-- 单目标模式：现代化Tab标签页 -->
        <div v-else>
        <!-- 标签页导航 -->
        <div class="transform-tabs-nav">
          <div 
            class="tab-item"
            :class="{'active': transformTab === 'before'}"
            @click="transformTab = 'before'">
            <div class="tab-icon">
              <i class="el-icon-download"></i>
            </div>
            <div class="tab-info">
              <div class="tab-title">已抽取数据</div>
              <div class="tab-count">{{ sourcePreviewData.length || 0 }} 条</div>
            </div>
            <div v-if="sourcePreviewData.length > 0" class="tab-badge">
              <i class="el-icon-check"></i>
            </div>
          </div>
          
          <div 
            class="tab-item"
            :class="{'active': transformTab === 'config'}"
            @click="transformTab = 'config'">
            <div class="tab-icon">
              <i class="el-icon-setting"></i>
            </div>
            <div class="tab-info">
              <div class="tab-title">配置转换规则</div>
              <div class="tab-count">{{ mappings.length || 0 }} 个规则</div>
            </div>
            <div v-if="mappings.length > 0" class="tab-badge">
              <i class="el-icon-check"></i>
            </div>
          </div>
          
          <div 
            class="tab-item"
            :class="{'active': transformTab === 'after'}"
            @click="transformTab = 'after'">
            <div class="tab-icon">
              <i class="el-icon-view"></i>
            </div>
            <div class="tab-info">
              <div class="tab-title">转换后数据预览</div>
              <div class="tab-count">{{ transformedPreviewData.length || 0 }} 条</div>
            </div>
            <div v-if="transformedPreviewData.length > 0" class="tab-badge">
              <i class="el-icon-check"></i>
            </div>
          </div>
        </div>
        
        <!-- 内容区域 -->
        <div class="transform-content-area">
          <!-- 步骤1：已抽取数据 -->
          <div v-show="transformTab === 'before'" class="transform-step-content">
            <div v-if="sourcePreviewData.length === 0" style="text-align: center; padding: 40px 0; color: #909399">
              <i class="el-icon-warning" style="font-size: 48px"></i>
              <div style="margin-top: 15px">暂无已抽取的数据</div>
              <div style="margin-top: 8px; font-size: 13px">请在"选择数据源"步骤点击"抽取数据"按钮</div>
              <el-button type="primary" size="small" @click="activeStep = 1" style="margin-top: 15px">
                <i class="el-icon-back"></i> 返回数据源步骤
              </el-button>
            </div>
            <div v-else>
              <div style="margin-bottom: 10px; color: #606266">
                <i class="el-icon-info"></i> 以下是从源端抽取到的原始数据（共 <strong style="color: #409EFF">{{ sourcePreviewData.length }}</strong> 条）
              </div>
              <el-table :data="sourcePreviewData" border size="small" max-height="400" stripe>
                <el-table-column type="index" label="#" width="50" />
                <el-table-column 
                  v-for="col in sourcePreviewColumns" 
                  :key="col" 
                  :prop="col" 
                  :label="col" 
                  min-width="120"
                  show-overflow-tooltip>
                  <template slot-scope="scope">
                    <span>{{ formatCellValue(scope.row[col]) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          
          <!-- 步骤2：配置转换规则 -->
          <div v-show="transformTab === 'config'" class="transform-step-content">
            <div class="tip" style="margin-bottom: 15px; background: #fff3cd; padding: 12px; border-radius: 4px; color: #856404">
              <i class="el-icon-warning"></i> <strong>提示：</strong>配置完成后，可切换到"转换后数据预览"标签查看转换效果
            </div>
        <!-- 工具栏 -->
        <div style="background: #f5f7fa; padding: 15px; border-radius: 4px; margin-bottom: 20px">
          <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px">
            <div style="display: flex; gap: 8px; flex-wrap: wrap">
              <!-- 基础功能 -->
              <el-button type="success" size="small" icon="el-icon-magic-stick" @click="handleAutoMapping">
                智能映射
              </el-button>
              <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAddMapping">
                添加转换规则
              </el-button>
              
              <el-divider direction="vertical"></el-divider>
              
              <!-- 扩展功能3: 模板管理 -->
              <el-button type="primary" size="small" icon="el-icon-folder-add" plain @click="showTemplateSaveDialog">
                保存模板
              </el-button>
              <el-button type="primary" size="small" icon="el-icon-folder-opened" plain @click="showTemplateListDialog">
                应用模板
              </el-button>
              
              <el-divider direction="vertical"></el-divider>
              
              <el-button type="warning" size="small" icon="el-icon-delete" @click="handleClearMappings">
                清空全部
              </el-button>
            </div>
            <div style="color: #606266">
              <i class="el-icon-info"></i> 已配置 <strong style="color: #409EFF">{{ mappings.length }}</strong> 个转换规则
            </div>
          </div>
        </div>

        <!-- 转换规则列表（卡片式） -->
        <div v-if="mappings.length === 0" style="text-align: center; padding: 60px 0; color: #909399">
          <i class="el-icon-document" style="font-size: 64px"></i>
          <div style="margin-top: 15px; font-size: 14px">暂无转换规则，请点击上方按钮添加</div>
          <div style="margin-top: 8px; font-size: 12px">提示：可使用“智能映射”快速加载源字段，再手动指定目标字段</div>
        </div>

        <!-- 表格列表（紧凑模式） -->
        <div v-else>
          <el-table :data="mappings" border stripe style="width: 100%">
            <!-- 序号 -->
            <el-table-column type="index" label="#" width="50" align="center" />
            
            <!-- 源字段 -->
            <el-table-column label="源字段" min-width="180">
              <template slot-scope="scope">
                <el-autocomplete
                  v-model="scope.row.sourceField"
                  :fetch-suggestions="querySourceFields"
                  placeholder="请输入源字段名"
                  style="width: 100%"
                  size="small"
                  :trigger-on-focus="true"
                  @input="handleSourceFieldInput(scope.row)"
                  @select="handleSourceFieldSelect(scope.row, $event)"
                  @blur="handleSourceFieldBlur(scope.row)">
                  <template slot-scope="{ item }">
                    <div style="padding: 6px 10px; margin: 2px 0; border-radius: 4px; background-color: #fff; border: 1px solid #f0f2f5; box-sizing: border-box;">
                      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                        <span style="font-weight: 600; color: #303133; font-size: 13px;">{{ item.value }}</span>
                        <div style="display: flex; align-items: center; gap: 6px; font-size: 12px;">
                          <span v-if="item.nullable === false" style="color: #F56C6C;">★必填</span>
                          <span style="color: #909399;">
                            {{ item.type || '字段' }}<span v-if="item.columnSize">({{ item.columnSize }})</span>
                          </span>
                        </div>
                      </div>
                      <div v-if="item.remarks" style="color: #909399; font-size: 12px; line-height: 1.4; max-height: 2.8em; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">
                        {{ item.remarks }}
                      </div>
                    </div>
                  </template>
                </el-autocomplete>
                <div v-if="scope.row.sourceType || scope.row.sourceNullable === false || scope.row.sourceRemarks || scope.row.sourceColumnSize" style="margin-top: 4px; display: flex; gap: 4px; flex-wrap: wrap">
                  <el-tag v-if="scope.row.sourceType" size="mini" type="info" effect="plain">
                    {{ scope.row.sourceType }}<span v-if="scope.row.sourceColumnSize">({{ scope.row.sourceColumnSize }})</span>
                  </el-tag>
                  <el-tag v-if="scope.row.sourceNullable === false" size="mini" type="danger" effect="plain">★必填</el-tag>
                  <el-tooltip v-if="scope.row.sourceRemarks" :content="scope.row.sourceRemarks" placement="top">
                    <el-tag size="mini" type="success" effect="plain">
                      <i class="el-icon-info"></i> {{ scope.row.sourceRemarks.length > 20 ? scope.row.sourceRemarks.substring(0, 20) + '...' : scope.row.sourceRemarks }}
                    </el-tag>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            
            <!-- 箭头 -->
            <el-table-column width="50" align="center">
              <template>
                <i class="el-icon-right" style="font-size: 18px; color: #909399"></i>
              </template>
            </el-table-column>
            
            <!-- 目标字段 -->
            <el-table-column label="目标字段" min-width="180">
              <template slot-scope="scope">
                <el-autocomplete
                  v-model="scope.row.targetField"
                  :fetch-suggestions="queryTargetFields"
                  placeholder="请输入目标字段名"
                  style="width: 100%"
                  size="small"
                  :trigger-on-focus="true"
                  @input="handleTargetFieldInput(scope.row)"
                  @select="handleTargetFieldSelect(scope.row, $event)"
                  @blur="handleTargetFieldBlur(scope.row)">
                  <template slot-scope="{ item }">
                    <div style="padding: 6px 10px; margin: 2px 0; border-radius: 4px; background-color: #fff; border: 1px solid #f0f2f5; box-sizing: border-box;">
                      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 2px;">
                        <span style="font-weight: 600; color: #303133; font-size: 13px;">{{ item.value }}</span>
                        <div style="display: flex; align-items: center; gap: 6px; font-size: 12px;">
                          <span v-if="item.nullable === false" style="color: #F56C6C;">★必填</span>
                          <span style="color: #909399;">
                            {{ item.type || '字段' }}<span v-if="item.columnSize">({{ item.columnSize }})</span>
                          </span>
                        </div>
                      </div>
                      <div v-if="item.remarks" style="color: #909399; font-size: 12px; line-height: 1.4; max-height: 2.8em; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;">
                        {{ item.remarks }}
                      </div>
                    </div>
                  </template>
                </el-autocomplete>
                <div v-if="scope.row.targetType || scope.row.targetNullable === false || scope.row.targetRemarks || scope.row.targetColumnSize" style="margin-top: 4px; display: flex; gap: 4px; flex-wrap: wrap">
                  <el-tag v-if="scope.row.targetType" size="mini" type="info" effect="plain">
                    {{ scope.row.targetType }}<span v-if="scope.row.targetColumnSize">({{ scope.row.targetColumnSize }})</span>
                  </el-tag>
                  <el-tag v-if="scope.row.targetNullable === false" size="mini" type="danger" effect="plain">★必填</el-tag>
                  <el-tooltip v-if="scope.row.targetRemarks" :content="scope.row.targetRemarks" placement="top">
                    <el-tag size="mini" type="success" effect="plain">
                      <i class="el-icon-info"></i> {{ scope.row.targetRemarks.length > 20 ? scope.row.targetRemarks.substring(0, 20) + '...' : scope.row.targetRemarks }}
                    </el-tag>
                  </el-tooltip>
                </div>
              </template>
            </el-table-column>
            
            <!-- 数据处理流程 -->
            <el-table-column label="数据处理流程" min-width="350">
              <template slot-scope="scope">
                <div v-if="!scope.row._processors || scope.row._processors.length === 0" style="color: #67C23A; font-size: 12px">
                  <i class="el-icon-success"></i> 直接映射
                </div>
                <div v-else style="display: flex; flex-wrap: wrap; gap: 6px; align-items: center; padding: 8px 0">
                  <div
                    v-for="(proc, idx) in scope.row._processors" 
                    :key="idx"
                    draggable
                    @dragstart="handleDragStart(scope.row, idx, $event)"
                    @dragover.prevent
                    @drop="handleDrop(scope.row, idx, $event)"
                    @dragenter="handleDragEnter($event)"
                    @dragleave="handleDragLeave($event)"
                    class="processor-item"
                    :class="{ 'dragging': draggedProcessorIndex === idx && draggedMappingRef === scope.row }"
                    style="position: relative; display: inline-flex; align-items: center; gap: 0; padding: 4px 10px 4px 0; margin: 4px 0; border-radius: 4px; transition: all 0.2s">
                    
                    <!-- 拖拽手柄 -->
                    <div class="drag-handle" style="cursor: move; padding: 0 6px; color: #909399; display: flex; align-items: center">
                      <i class="el-icon-rank" style="font-size: 14px"></i>
                    </div>
                    
                    <!-- 处理器标签 -->
                    <el-tooltip
                      :content="getProcessorDetailTextWithMapping(scope.row, proc)"
                      placement="top">
                      <el-tag 
                        :type="getProcessorTagType(proc.type)"
                        size="small"
                        style="cursor: pointer; margin: 0"
                        @click="configureProcessor(scope.row, proc, idx)">
                        <i :class="getProcessorIcon(proc.type)"></i>
                        {{ idx + 1 }}. {{ getProcessorTypeName(proc.type) }}{{ getProcessorSubtypeTextWithMapping(scope.row, proc) }}
                        <i v-if="isProcessorActuallyConfigured(scope.row, proc)" class="el-icon-circle-check" style="margin-left: 3px; color: #67C23A"></i>
                      </el-tag>
                    </el-tooltip>
                    
                    <!-- 删除按钮 -->
                    <el-button 
                      class="delete-btn"
                      type="danger" 
                      icon="el-icon-close" 
                      size="mini" 
                      circle
                      @click.stop="removeProcessor(scope.row, idx)"
                      style="position: absolute; top: -6px; right: -6px; width: 20px; height: 20px; padding: 0; font-size: 12px; opacity: 0; transition: opacity 0.2s; z-index: 10; border: 2px solid #fff; box-shadow: 0 2px 4px rgba(0,0,0,0.12)"></el-button>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <!-- 操作 -->
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template slot-scope="scope">
                <el-button 
                  size="mini" 
                  type="primary"
                  icon="el-icon-plus"
                  @click="showAddProcessorDialog(scope.row)">添加</el-button>
                <el-button 
                  size="mini" 
                  type="danger" 
                  icon="el-icon-delete"
                  @click="handleDeleteMapping(scope.$index)"></el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
          </div>
          
          <!-- 步骤3：转换后数据预览 -->
          <div v-show="transformTab === 'after'" class="transform-step-content">
            <div v-if="mappings.length === 0" style="text-align: center; padding: 40px 0; color: #909399">
              <i class="el-icon-warning" style="font-size: 48px"></i>
              <div style="margin-top: 15px">暂无转换规则</div>
              <div style="margin-top: 8px; font-size: 13px">请在"配置转换规则"标签中添加字段映射</div>
              <el-button type="primary" size="small" @click="transformTab = 'config'" style="margin-top: 15px">
                <i class="el-icon-setting"></i> 前往配置
              </el-button>
            </div>
            <div v-else-if="transformedPreviewData.length === 0" style="text-align: center; padding: 40px 0">
              <div style="margin-bottom: 20px; color: #606266">
                <i class="el-icon-info"></i> 点击下方按钮预览转换后的数据效果
              </div>
              <el-button type="primary" size="large" @click="handlePreviewTransform" :loading="transformPreviewLoading">
                <i class="el-icon-view"></i> 预览转换效果
              </el-button>
              <div class="tip" style="margin-top: 15px">
                <i class="el-icon-info"></i> 将对转换前数据应用所有转换规则，生成预览结果
              </div>
            </div>
            <div v-else>
              <div style="margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center">
                <div style="color: #606266">
                  <i class="el-icon-success" style="color: #67C23A"></i> 
                  转换后数据（共 <strong style="color: #409EFF">{{ transformedPreviewData.length }}</strong> 条，
                  字段 <strong style="color: #409EFF">{{ transformedPreviewColumns.length }}</strong> 个）
                </div>
                <el-button type="primary" size="small" @click="handlePreviewTransform" :loading="transformPreviewLoading">
                  <i class="el-icon-refresh"></i> 重新预览
                </el-button>
              </div>
              <el-table :data="transformedPreviewData" border size="small" max-height="400" stripe>
                <el-table-column type="index" label="#" width="50" />
                <el-table-column 
                  v-for="col in transformedPreviewColumns" 
                  :key="col" 
                  :prop="col" 
                  :label="col" 
                  min-width="120"
                  show-overflow-tooltip>
                  <template slot-scope="scope">
                    <span :style="getFieldChangeStyle(col, scope.$index)">{{ scope.row[col] }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
        </div>
      </div>

      <!-- 步骤5: 调度配置 -->
      <div v-show="activeStep === 5" class="schedule-config-wrapper">
        <div class="schedule-config-container">
          <!-- 执行方式 -->
          <div class="config-section">
            <div class="section-header">
              <i class="el-icon-s-operation"></i>
              <span>执行方式</span>
            </div>
            <el-form :model="scheduleConfig" label-width="100px" size="medium">
              <el-form-item label="调度类型" required>
                <div class="schedule-type-cards">
                  <div 
                    class="type-card"
                    :class="{'active': scheduleConfig.scheduleType === 'MANUAL'}"
                    @click="scheduleConfig.scheduleType = 'MANUAL'">
                    <div class="card-icon">
                      <i class="el-icon-thumb"></i>
                    </div>
                    <div class="card-content">
                      <div class="card-title">手动执行</div>
                      <div class="card-desc">通过界面或API手动触发</div>
                    </div>
                  </div>
                  <div 
                    class="type-card"
                    :class="{'active': scheduleConfig.scheduleType === 'CRON'}"
                    @click="scheduleConfig.scheduleType = 'CRON'">
                    <div class="card-icon">
                      <i class="el-icon-alarm-clock"></i>
                    </div>
                    <div class="card-content">
                      <div class="card-title">定时调度</div>
                      <div class="card-desc">按Cron表达式或定时自动执行</div>
                    </div>
                  </div>
                </div>
              </el-form-item>

              <!-- Cron配置 -->
              <template v-if="scheduleConfig.scheduleType === 'CRON'">
                <el-form-item label="执行频率" required>
                  <div class="cron-template-list">
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 */5 * * * ?'}"
                      @click="handleCronTemplateChange('0 */5 * * * ?')">
                      <span class="cron-icon-wrapper icon-green">
                        <i class="el-icon-time"></i>
                      </span>
                      <span class="cron-label">每5分钟</span>
                      <el-tag size="mini" type="info">高频</el-tag>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 */30 * * * ?'}"
                      @click="handleCronTemplateChange('0 */30 * * * ?')">
                      <span class="cron-icon-wrapper icon-blue">
                        <i class="el-icon-time"></i>
                      </span>
                      <span class="cron-label">每30分钟</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 0 * * * ?'}"
                      @click="handleCronTemplateChange('0 0 * * * ?')">
                      <span class="cron-icon-wrapper icon-cyan">
                        <i class="el-icon-time"></i>
                      </span>
                      <span class="cron-label">每小时</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 0 2 * * ?'}"
                      @click="handleCronTemplateChange('0 0 2 * * ?')">
                      <span class="cron-icon-wrapper icon-orange">
                        <i class="el-icon-sunny"></i>
                      </span>
                      <span class="cron-label">每天凌晨2点</span>
                      <el-tag size="mini" type="success">推荐</el-tag>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 0 2 ? * MON'}"
                      @click="handleCronTemplateChange('0 0 2 ? * MON')">
                      <span class="cron-icon-wrapper icon-purple">
                        <i class="el-icon-date"></i>
                      </span>
                      <span class="cron-label">每周一凌晨2点</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 0 9 ? * MON-FRI'}"
                      @click="handleCronTemplateChange('0 0 9 ? * MON-FRI')">
                      <span class="cron-icon-wrapper icon-pink">
                        <i class="el-icon-office-building"></i>
                      </span>
                      <span class="cron-label">工作日上午9点</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === '0 0 2 1 * ?'}"
                      @click="handleCronTemplateChange('0 0 2 1 * ?')">
                      <span class="cron-icon-wrapper icon-red">
                        <i class="el-icon-date"></i>
                      </span>
                      <span class="cron-label">每月1号凌晨2点</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === 'visual'}"
                      @click="handleCronTemplateChange('visual')">
                      <span class="cron-icon-wrapper icon-indigo">
                        <i class="el-icon-setting"></i>
                      </span>
                      <span class="cron-label">可视化配置</span>
                    </div>
                    <div 
                      class="cron-item"
                      :class="{active: cronTemplate === 'custom'}"
                      @click="handleCronTemplateChange('custom')">
                      <span class="cron-icon-wrapper icon-gray">
                        <i class="el-icon-edit"></i>
                      </span>
                      <span class="cron-label">自定义</span>
                    </div>
                  </div>
                </el-form-item>
                
                <!-- 可视化配置面板 -->
                <el-form-item v-if="cronTemplate === 'visual'" label="定时规则">
                  <div style="border: 1px solid #e5e7eb; border-radius: 8px; overflow: hidden;">
                    <!-- 配置区 -->
                    <div style="background: #ffffff; padding: 24px;">
                      <el-row :gutter="20">
                        <!-- 周期配置 -->
                        <el-col :span="6">
                          <div style="margin-bottom: 8px; color: #374151; font-size: 13px; font-weight: 500;">
                            <i class="el-icon-date" style="margin-right: 4px;"></i>周期
                          </div>
                          <el-select v-model="cronVisual.period" @change="updateCronFromVisual" style="width: 100%" size="medium">
                            <el-option label="每天" value="day">
                              <span><i class="el-icon-sunrise"></i> 每天</span>
                            </el-option>
                            <el-option label="每周" value="week">
                              <span><i class="el-icon-date"></i> 每周</span>
                            </el-option>
                            <el-option label="每月" value="month">
                              <span><i class="el-icon-date"></i> 每月</span>
                            </el-option>
                            <el-option label="自定义" value="custom">
                              <span><i class="el-icon-setting"></i> 自定义</span>
                            </el-option>
                          </el-select>
                        </el-col>
                        
                        <!-- 星期选择（每周模式） -->
                        <el-col :span="6" v-if="cronVisual.period === 'week'">
                          <div style="margin-bottom: 8px; color: #374151; font-size: 13px; font-weight: 500;">
                            <i class="el-icon-calendar" style="margin-right: 4px;"></i>星期
                          </div>
                          <el-select v-model="cronVisual.weekDay" @change="updateCronFromVisual" style="width: 100%" size="medium">
                            <el-option label="周一" value="MON" />
                            <el-option label="周二" value="TUE" />
                            <el-option label="周三" value="WED" />
                            <el-option label="周四" value="THU" />
                            <el-option label="周五" value="FRI" />
                            <el-option label="周六" value="SAT" />
                            <el-option label="周日" value="SUN" />
                          </el-select>
                        </el-col>
                        
                        <!-- 日期选择（每月模式） -->
                        <el-col :span="6" v-if="cronVisual.period === 'month'">
                          <div style="margin-bottom: 8px; color: #374151; font-size: 13px; font-weight: 500;">
                            <i class="el-icon-date" style="margin-right: 4px;"></i>日期
                          </div>
                          <div style="display: flex; align-items: center;">
                            <el-input-number 
                              v-model="cronVisual.dayOfMonth" 
                              :min="1" 
                              :max="31" 
                              @change="updateCronFromVisual"
                              controls-position="right"
                              size="medium"
                              style="width: 100px" />
                            <span style="margin-left: 8px; color: #6b7280; font-size: 14px;">号</span>
                          </div>
                        </el-col>
                        
                        <!-- 时间选择（非自定义模式） -->
                        <el-col :span="6" v-if="cronVisual.period !== 'custom'">
                          <div style="margin-bottom: 8px; color: #374151; font-size: 13px; font-weight: 500;">
                            <i class="el-icon-time" style="margin-right: 4px;"></i>执行时间
                          </div>
                          <el-time-select
                            v-model="cronVisual.time"
                            :picker-options="{
                              start: '00:00',
                              step: '00:30',
                              end: '23:30'
                            }"
                            @change="updateCronFromVisual"
                            placeholder="选择时间"
                            size="medium"
                            style="width: 100%">
                          </el-time-select>
                        </el-col>
                        
                        <!-- 间隔配置（自定义模式） -->
                        <el-col :span="12" v-if="cronVisual.period === 'custom'">
                          <div style="margin-bottom: 8px; color: #374151; font-size: 13px; font-weight: 500;">
                            <i class="el-icon-refresh" style="margin-right: 4px;"></i>执行间隔
                          </div>
                          <div style="display: flex; align-items: center; gap: 8px;">
                            <span style="color: #6b7280; font-size: 14px;">每</span>
                            <el-input-number 
                              v-model="cronVisual.interval" 
                              :min="1" 
                              @change="updateCronFromVisual"
                              controls-position="right"
                              size="medium"
                              style="width: 100px" />
                            <el-select v-model="cronVisual.unit" @change="updateCronFromVisual" size="medium" style="width: 120px;">
                              <el-option label="分钟" value="minute">
                                <span><i class="el-icon-time"></i> 分钟</span>
                              </el-option>
                              <el-option label="小时" value="hour">
                                <span><i class="el-icon-time"></i> 小时</span>
                              </el-option>
                              <el-option label="天" value="day">
                                <span><i class="el-icon-date"></i> 天</span>
                              </el-option>
                            </el-select>
                            <span style="color: #6b7280; font-size: 14px;">执行一次</span>
                          </div>
                        </el-col>
                      </el-row>
                    </div>
                  </div>
                </el-form-item>

                <el-form-item label="Cron表达式">
                  <el-input 
                    v-model="scheduleConfig.cronExpression" 
                    placeholder="0 0 * * * ?"
                    :readonly="cronTemplate !== 'custom'">
                    <template slot="prepend"><i class="el-icon-edit-outline"></i></template>
                    <template slot="append">
                      <el-button 
                        icon="el-icon-refresh" 
                        @click="parseCronExpression"
                        :disabled="!scheduleConfig.cronExpression">解析</el-button>
                    </template>
                  </el-input>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    格式: 秒 分 时 日 月 周 (如: 0 0 2 * * ? 表示每天凌晨2点)
                  </div>
                  <!-- 执行计划 -->
                  <div v-if="cronDescription" style="margin-top: 8px; padding: 12px; background: #f5f5f5; border-radius: 4px; color: #555; font-size: 14px;">
                    <i class="el-icon-success" style="color: #10b981; margin-right: 8px;"></i>{{ cronDescription }}
                  </div>
                </el-form-item>
              </template>
            </el-form>
          </div>

          <!-- 重试策略 -->
          <div class="config-section">
            <div class="section-header">
              <i class="el-icon-refresh-right"></i>
              <span>重试策略</span>
            </div>
            <el-form :model="scheduleConfig" label-width="100px" size="medium">
              <el-form-item label="失败重试">
                <el-switch v-model="scheduleConfig.enableRetry" active-color="#10b981"></el-switch>
                <span style="margin-left: 12px; color: #6b7280">任务失败后自动重试</span>
              </el-form-item>

              <el-form-item v-if="scheduleConfig.enableRetry" label="最大重试次数">
                <el-input-number 
                  v-model="scheduleConfig.maxRetryTimes" 
                  :min="1" 
                  :max="10"
                  controls-position="right" />
                <span style="margin-left: 12px; color: #6b7280">次</span>
                <div class="field-tip warning">
                  <i class="el-icon-warning"></i>
                  建议设置3-5次，避免过度重试
                </div>
              </el-form-item>
            </el-form>
          </div>

          <!-- 执行策略 -->
          <div class="config-section">
            <div class="section-header">
              <i class="el-icon-s-data"></i>
              <span>执行策略</span>
            </div>
            <el-form :model="sourceConfig" label-width="100px" size="medium">
              <!-- 数据处理模式 -->
              <el-form-item label="数据处理模式">
                <div class="execution-mode-cards">
                  <div 
                    class="mode-card-compact"
                    :class="{'active': !sourceConfig.streamMode}"
                    @click="sourceConfig.streamMode = false">
                    <div class="mode-icon-small">
                      <i class="el-icon-s-grid"></i>
                    </div>
                    <div class="mode-content-compact">
                      <div class="mode-title-small">内存模式</div>
                      <div class="mode-desc-small">全量加载到内存处理，支持多表关联</div>
                    </div>
                    <div class="mode-check-small">
                      <i class="el-icon-check"></i>
                    </div>
                  </div>
                  
                  <div 
                    class="mode-card-compact"
                    :class="{'active': sourceConfig.streamMode}"
                    @click="sourceConfig.streamMode = true">
                    <div class="mode-icon-small">
                      <i class="el-icon-sort"></i>
                    </div>
                    <div class="mode-content-compact">
                      <div class="mode-title-small">流式模式</div>
                      <div class="mode-desc-small">边抽取边处理，内存占用低，适合大数据量</div>
                    </div>
                    <div class="mode-check-small">
                      <i class="el-icon-check"></i>
                    </div>
                  </div>
                </div>
                <div class="field-tip">
                  <i class="el-icon-info"></i>
                  <span v-if="!sourceConfig.streamMode">内存模式：数据全部加载到内存后处理，转换阶段无数据库连接占用</span>
                  <span v-else>流式模式：分批读取和处理数据，每批转换完立即写入，减少内存占用和连接占用时间</span>
                </div>
                <div v-if="sourceConfig.streamMode" class="field-tip warning">
                  <i class="el-icon-warning"></i>
                  流式模式不支持多表关联（辅助数据源），如检测到辅助数据源会自动切换为内存模式
                </div>
              </el-form-item>

              <!-- 批处理大小 -->
              <el-form-item label="批处理大小">
                <el-input-number 
                  v-model="sourceConfig.fetchBatchSize" 
                  :min="100" 
                  :max="10000"
                  :step="100"
                  controls-position="right" />
                <span style="margin-left: 12px; color: #6b7280">条/批次</span>
                <div class="field-tip">
                  <i class="el-icon-info"></i>
                  <span v-if="sourceConfig.streamMode">流式模式：每批读取和写入的数据条数（推荐5000-10000）</span>
                  <span v-else>内存模式：批量插入的大小（推荐5000-10000）</span>
                </div>
              </el-form-item>

              <!-- 性能优化配置 -->
              <el-form-item label="性能优化">
                <div class="performance-optimization-panel">
                  <div class="optimization-header">
                    <i class="el-icon-lightning"></i>
                    <span>4阶段智能性能优化已自动启用</span>
                    <el-tag type="success" size="mini" effect="plain">综合提升30-150倍</el-tag>
                  </div>
                  
                  <div class="optimization-stages">
                    <div class="stage-item">
                      <div class="stage-badge stage-1">阶段1</div>
                      <div class="stage-content">
                        <div class="stage-title">基础优化</div>
                        <div class="stage-desc">批量提交 + 大批次处理</div>
                        <div class="stage-performance">性能提升: 5-10倍</div>
                      </div>
                      <div class="stage-status"><i class="el-icon-check"></i></div>
                    </div>
                    
                    <div class="stage-item">
                      <div class="stage-badge stage-2">阶段2</div>
                      <div class="stage-content">
                        <div class="stage-title">并发优化</div>
                        <div class="stage-desc">并行写入(4线程) + JDBC参数优化</div>
                        <div class="stage-performance">性能提升: 10-30倍</div>
                      </div>
                      <div class="stage-status"><i class="el-icon-check"></i></div>
                    </div>
                    
                    <div class="stage-item">
                      <div class="stage-badge stage-3">阶段3</div>
                      <div class="stage-content">
                        <div class="stage-title">连接池优化</div>
                        <div class="stage-desc">HikariCP连接复用</div>
                        <div class="stage-performance">性能提升: 15-60倍</div>
                      </div>
                      <div class="stage-status"><i class="el-icon-check"></i></div>
                    </div>
                    
                    <div class="stage-item">
                      <div class="stage-badge stage-4">阶段4</div>
                      <div class="stage-content">
                        <div class="stage-title">高性能加载器</div>
                        <div class="stage-desc">LOAD DATA / COPY / 优化批量插入</div>
                        <div class="stage-performance">性能提升: 30-150倍</div>
                      </div>
                      <div class="stage-status"><i class="el-icon-check"></i></div>
                    </div>
                  </div>
                  
                  <div class="optimization-footer">
                    <i class="el-icon-info"></i>
                    <span>系统根据数据量和数据库类型自动选择最优策略，无需手动配置</span>
                  </div>
                </div>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>

      <!-- 步骤6: 推送后处理 -->
      <div v-show="activeStep === 6" class="postload-config-wrapper">
        <div class="postload-config-container">
          <!-- 功能说明卡片 -->
          <div class="info-banner">
            <div class="banner-icon">
              <i class="el-icon-info"></i>
            </div>
            <div class="banner-content">
              <div class="banner-title">推送后处理</div>
              <div class="banner-desc">在数据推送完成后执行特定操作，如更新源端数据状态为已完成，避免重复推送。</div>
            </div>
          </div>

          <!-- 状态回写开关 -->
          <div class="config-section">
            <div class="section-header">
              <i class="el-icon-finished"></i>
              <span>状态回写</span>
            </div>
            <el-form :model="postLoadConfig" label-width="100px" size="medium">
              <el-form-item label="启用状态回写">
                <el-switch v-model="postLoadConfig.statusUpdate.enabled" active-color="#10b981"></el-switch>
                <span style="margin-left: 12px; color: #6b7280">推送成功后自动更新源端数据状态</span>
              </el-form-item>
            </el-form>
          </div>

          <!-- 回写配置 -->
          <template v-if="postLoadConfig.statusUpdate.enabled">
            <!-- 回写方式选择 -->
            <div class="config-section">
              <div class="section-header">
                <i class="el-icon-s-operation"></i>
                <span>回写方式</span>
              </div>
              <el-form :model="postLoadConfig" label-width="100px" size="medium">
                <el-form-item label="回写方式" required>
                  <div class="update-type-cards">
                    <div 
                      class="type-card"
                      :class="{'active': postLoadConfig.statusUpdate.updateType === 'SQL'}"
                      @click="postLoadConfig.statusUpdate.updateType = 'SQL'">
                      <div class="card-icon">
                        <i class="el-icon-document"></i>
                      </div>
                      <div class="card-content">
                        <div class="card-title">SQL方式</div>
                        <div class="card-desc">通过UPDATE语句直接更新源端数据库</div>
                      </div>
                    </div>
                    <div 
                      class="type-card"
                      :class="{'active': postLoadConfig.statusUpdate.updateType === 'API'}"
                      @click="postLoadConfig.statusUpdate.updateType = 'API'">
                      <div class="card-icon">
                        <i class="el-icon-link"></i>
                      </div>
                      <div class="card-content">
                        <div class="card-title">API方式</div>
                        <div class="card-desc">通过HTTP请求回调其他系统</div>
                      </div>
                    </div>
                  </div>
                </el-form-item>
              </el-form>
            </div>

            <!-- SQL方式配置 -->
            <div v-if="postLoadConfig.statusUpdate.updateType === 'SQL'" class="config-section">
              <div class="section-header">
                <i class="el-icon-edit"></i>
                <span>SQL配置</span>
              </div>
              <el-form :model="postLoadConfig.statusUpdate" label-width="100px" size="medium">
                <el-form-item label="主键字段" required>
                  <el-input 
                    v-model="postLoadConfig.statusUpdate.keyField" 
                    placeholder="请输入主键字段名，如: id">
                    <template slot="prepend"><i class="el-icon-key"></i></template>
                  </el-input>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    主键字段必须在源端SQL的SELECT列表中
                  </div>
                </el-form-item>

                <el-form-item label="UPDATE语句" required>
                  <el-input 
                    v-model="postLoadConfig.statusUpdate.updateSql" 
                    type="textarea"
                    :rows="4"
                    placeholder="UPDATE users SET status = 'COMPLETED', sync_time = NOW() WHERE id IN ({ids})"
                    class="sql-editor" />
                  <div class="field-tip warning">
                    <i class="el-icon-warning"></i>
                    <strong>必须包含 {ids} 占位符</strong>，系统会自动替换为推送成功的记录ID列表
                  </div>
                </el-form-item>

                <el-form-item label="批处理大小">
                  <el-input-number 
                    v-model="postLoadConfig.statusUpdate.batchSize" 
                    :min="10" 
                    :max="1000" 
                    :step="10"
                    controls-position="right" />
                  <span style="margin-left: 12px; color: #6b7280">每次UPDATE的记录数（默认100）</span>
                </el-form-item>
              </el-form>
            </div>

            <!-- API方式配置 -->
            <div v-else-if="postLoadConfig.statusUpdate.updateType === 'API'" class="config-section">
              <div class="section-header">
                <i class="el-icon-link"></i>
                <span>API配置</span>
              </div>
              <el-form :model="postLoadConfig.statusUpdate" label-width="100px" size="medium">
                <el-form-item label="主键字段" required>
                  <el-input 
                    v-model="postLoadConfig.statusUpdate.keyField" 
                    placeholder="请输入主键字段名，如: id">
                    <template slot="prepend"><i class="el-icon-key"></i></template>
                  </el-input>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    主键字段必须在源端SQL的SELECT列表中
                  </div>
                </el-form-item>

                <el-form-item label="请求配置" required>
                  <div style="display: flex; gap: 8px">
                    <el-select v-model="postLoadConfig.statusUpdate.method" style="width: 120px">
                      <el-option label="POST" value="POST" />
                      <el-option label="PUT" value="PUT" />
                      <el-option label="PATCH" value="PATCH" />
                    </el-select>
                    <el-input 
                      v-model="postLoadConfig.statusUpdate.apiUrl" 
                      placeholder="http://internal-system/api/update-status"
                      style="flex: 1">
                      <template slot="prepend"><i class="el-icon-link"></i></template>
                    </el-input>
                  </div>
                  <div class="field-tip">
                    <i class="el-icon-info"></i>
                    完整URL地址（包含协议和域名）
                  </div>
                </el-form-item>

                <!-- 请求参数配置 -->
                <el-form-item label="请求参数">
                  <el-tabs v-model="postLoadApiActiveTab" type="border-card" class="api-config-tabs">
                    <!-- Params -->
                    <el-tab-pane label="Params" name="params">
                      <div class="tab-toolbar">
                        <el-button size="mini" type="primary" icon="el-icon-plus" @click="addPostLoadApiParam">添加参数</el-button>
                      </div>
                      <el-table :data="postLoadConfig.statusUpdate.params || []" size="mini" border max-height="200">
                        <el-table-column label="启用" width="60" align="center">
                          <template slot-scope="scope">
                            <el-checkbox v-model="scope.row.enabled" />
                          </template>
                        </el-table-column>
                        <el-table-column label="参数名" width="150">
                          <template slot-scope="scope">
                            <el-input v-model="scope.row.key" size="mini" placeholder="key" />
                          </template>
                        </el-table-column>
                        <el-table-column label="参数值">
                          <template slot-scope="scope">
                            <el-input v-model="scope.row.value" size="mini" placeholder="value" />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" align="center">
                          <template slot-scope="scope">
                            <el-button type="text" size="mini" @click="removePostLoadApiParam(scope.$index)" style="color: #f56c6c">
                              <i class="el-icon-delete"></i>
                            </el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </el-tab-pane>

                    <!-- Body -->
                    <el-tab-pane label="Body" name="body">
                      <div class="tab-toolbar">
                        <el-button-group>
                          <el-button 
                            :type="postLoadConfig.statusUpdate.bodyType === 'none' ? 'primary' : ''"
                            size="small"
                            @click="handlePostLoadBodyTypeChange('none')">none</el-button>
                          <el-button 
                            :type="postLoadConfig.statusUpdate.bodyType === 'form-data' ? 'primary' : ''"
                            size="small"
                            @click="handlePostLoadBodyTypeChange('form-data')">form-data</el-button>
                          <el-button 
                            :type="postLoadConfig.statusUpdate.bodyType === 'raw' ? 'primary' : ''"
                            size="small"
                            @click="handlePostLoadBodyTypeChange('raw')">raw (JSON)</el-button>
                        </el-button-group>
                      </div>

                      <!-- none -->
                      <template v-if="postLoadConfig.statusUpdate.bodyType === 'none'">
                        <el-empty description="无请求体" :image-size="60"></el-empty>
                      </template>

                      <!-- form-data -->
                      <template v-else-if="postLoadConfig.statusUpdate.bodyType === 'form-data'">
                        <div style="margin-top: 12px">
                          <el-button size="mini" type="primary" icon="el-icon-plus" @click="addPostLoadFormDataParam">添加字段</el-button>
                        </div>
                        <el-table :data="postLoadConfig.statusUpdate.formData || []" size="mini" border max-height="200" style="margin-top: 8px">
                          <el-table-column label="启用" width="60" align="center">
                            <template slot-scope="scope">
                              <el-checkbox v-model="scope.row.enabled" />
                            </template>
                          </el-table-column>
                          <el-table-column label="字段名" width="150">
                            <template slot-scope="scope">
                              <el-input v-model="scope.row.key" size="mini" placeholder="key" />
                            </template>
                          </el-table-column>
                          <el-table-column label="字段值">
                            <template slot-scope="scope">
                              <el-input v-model="scope.row.value" size="mini" placeholder="value" />
                            </template>
                          </el-table-column>
                          <el-table-column label="操作" width="80" align="center">
                            <template slot-scope="scope">
                              <el-button type="text" size="mini" @click="removePostLoadFormDataParam(scope.$index)" style="color: #f56c6c">
                                <i class="el-icon-delete"></i>
                              </el-button>
                            </template>
                          </el-table-column>
                        </el-table>
                        <div class="field-tip" style="margin-top: 8px">
                          <i class="el-icon-info"></i>
                          form-data会自动添加Content-Type: multipart/form-data
                        </div>
                      </template>

                      <!-- raw (JSON) -->
                      <template v-else-if="postLoadConfig.statusUpdate.bodyType === 'raw'">
                        <div class="json-toolbar">
                          <span style="font-size: 12px; color: #909399">
                            Content-Type: <span style="color: #606266">application/json</span>
                          </span>
                          <div>
                            <el-button size="mini" type="success" icon="el-icon-check" @click="formatPostLoadJson">格式化</el-button>
                            <el-button size="mini" type="warning" icon="el-icon-circle-check" @click="validatePostLoadJson">验证</el-button>
                          </div>
                        </div>
                        <el-input 
                          v-model="postLoadConfig.statusUpdate.jsonBody" 
                          type="textarea"
                          :rows="8"
                          placeholder='{&#x0a;  "id": 1&#x0a;}'
                          class="json-editor"
                          @input="clearPostLoadJsonStatus" />
                        <div style="margin-top: 8px; min-height: 20px">
                          <div v-if="postLoadJsonError" style="color: #F56C6C; font-size: 12px">
                            <i class="el-icon-circle-close"></i> {{ postLoadJsonError }}
                          </div>
                          <div v-else-if="postLoadJsonValid" style="color: #67C23A; font-size: 12px">
                            <i class="el-icon-circle-check"></i> JSON格式正确
                          </div>
                        </div>
                      </template>
                    </el-tab-pane>

                    <!-- Headers -->
                    <el-tab-pane name="headers">
                      <span slot="label">
                        Headers
                        <el-badge v-if="postLoadEnabledHeadersCount > 0" :value="postLoadEnabledHeadersCount" class="item" style="margin-left: 5px" />
                      </span>
                      <div class="tab-toolbar">
                        <el-button size="mini" type="primary" icon="el-icon-plus" @click="addPostLoadApiHeader">添加请求头</el-button>
                      </div>
                      <el-table :data="postLoadConfig.statusUpdate.headers || []" size="mini" border max-height="200">
                        <el-table-column label="启用" width="60" align="center">
                          <template slot-scope="scope">
                            <el-checkbox v-model="scope.row.enabled" />
                          </template>
                        </el-table-column>
                        <el-table-column label="Header名" width="180">
                          <template slot-scope="scope">
                            <el-input v-model="scope.row.key" size="mini" placeholder="Content-Type" />
                          </template>
                        </el-table-column>
                        <el-table-column label="Header值">
                          <template slot-scope="scope">
                            <el-input v-model="scope.row.value" size="mini" placeholder="application/json" />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" align="center">
                          <template slot-scope="scope">
                            <el-button type="text" size="mini" @click="removePostLoadApiHeader(scope.$index)" style="color: #f56c6c">
                              <i class="el-icon-delete"></i>
                            </el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </el-tab-pane>
                  </el-tabs>
                </el-form-item>
              </el-form>
            </div>
          </template>
        </div>
      </div>

      <!-- 步骤7: 完成 -->
      <div v-show="activeStep === 7" class="step-complete">
        <div class="complete-wrapper">
          <div class="complete-header">
            <i class="el-icon-success complete-icon"></i>
            <div class="complete-text">
              <h2>任务配置完成</h2>
              <p>请确认以下配置信息，确认无误后点击保存</p>
            </div>
          </div>

          <div class="complete-content">
            <div class="info-card">
              <div class="card-title">基本信息</div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">任务名称</span>
                  <span class="value">{{ taskConfig.taskName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">任务编码</span>
                  <span class="value">{{ taskConfig.taskCode }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">同步模式</span>
                  <span class="value">
                    <el-tag v-if="taskConfig.syncMode === 'FULL'" size="small">全量同步</el-tag>
                    <el-tag v-else size="small" type="success">增量同步</el-tag>
                  </span>
                </div>
                <div class="info-item">
                  <span class="label">调度类型</span>
                  <span class="value">
                    <el-tag v-if="scheduleConfig.scheduleType === 'MANUAL'" size="small">手动执行</el-tag>
                    <el-tag v-else size="small" type="warning">定时调度</el-tag>
                  </span>
                </div>
              </div>
            </div>

            <div class="info-card">
              <div class="card-title">连接器配置</div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">源连接器</span>
                  <span class="value">{{ getConnectorName(sourceConfig.connectorId) }}</span>
                </div>
                <div class="info-item">
                  <span class="label">目标连接器</span>
                  <span class="value">{{ getConnectorName(targetConfig.connectorId) }}</span>
                </div>
              </div>
              <div class="info-row">
                <div class="info-item">
                  <span class="label">字段映射</span>
                  <span class="value">共 {{ mappings.length }} 个字段</span>
                </div>
                <div class="info-item">
                  <span class="label">写入模式</span>
                  <span class="value">{{ targetConfig.writeMode }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="complete-footer">
            <el-button type="primary" size="large" @click="handleSaveTask">
              <i class="el-icon-check"></i> 保存并启用任务
            </el-button>
            <el-button size="large" @click="handleSaveDraft">
              <i class="el-icon-document"></i> 保存为草稿
            </el-button>
          </div>
        </div>
      </div>

      <!-- 底部按钮（固定在底部） -->
      <div class="wizard-footer-fixed" style="text-align: center; padding: 20px 0; border-top: 1px solid #ebeef5;">
        <el-button v-if="activeStep > 0" @click="activeStep--">上一步</el-button>
        <el-button v-if="activeStep < 7" type="primary" @click="handleNext">下一步</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </div>
    </el-card>

    <!-- 数据预览抽屉 -->
    <el-drawer
      title="数据预览"
      :visible.sync="showPreviewDialog"
      direction="rtl"
      size="85%"
      :before-close="handleDrawerClose">
      <div class="drawer-content">
        <!-- 数据统计信息 -->
        <div class="preview-stats">
          <el-tag size="medium" type="success">
            <i class="el-icon-document"></i> {{ sourcePreviewData.length }} 条数据
          </el-tag>
          <el-tag size="medium" type="primary" style="margin-left: 12px;">
            <i class="el-icon-menu"></i> {{ sourcePreviewColumns.length }} 列
          </el-tag>
        </div>
        
        <!-- 数据表格 -->
        <div class="preview-table-container">
          <el-table 
            :data="sourcePreviewData" 
            border 
            stripe
            size="small" 
            height="calc(100vh - 180px)"
            style="width: 100%">
            <el-table-column 
              type="index"
              label="序号"
              width="60"
              align="center"
              fixed="left">
            </el-table-column>
            <el-table-column 
              v-for="col in sourcePreviewColumns" 
              :key="col" 
              :prop="col" 
              :label="col" 
              :min-width="calculateColumnWidth(col, sourcePreviewData)"
              show-overflow-tooltip>
              <template slot-scope="scope">
                <span>{{ formatCellValue(scope.row[col]) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>

    <!-- 转换预览对话框 -->
    <el-dialog title="转换预览" :visible.sync="previewDialogVisible" width="600px">
      <el-form label-width="100px" size="medium">
        <el-form-item label="输入测试值">
          <el-input v-model="previewTestValue" placeholder="请输入测试值" />
        </el-form-item>
        <el-form-item label="转换类型">
          <el-tag>{{ previewMapping.transformType }}</el-tag>
        </el-form-item>
        <el-form-item label="转换规则">
          <div style="font-family: monospace; background: #f5f7fa; padding: 8px; border-radius: 4px">
            {{ getPreviewRule(previewMapping) }}
          </div>
        </el-form-item>
        <el-form-item label="输出结果">
          <el-alert
            v-if="previewResult !== null"
            :title="previewResult"
            :type="previewError ? 'error' : 'success'"
            :closable="false"
            show-icon>
          </el-alert>
          <div v-else style="color: #909399">点击“开始预览”查看结果</div>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="previewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executePreview" :loading="previewLoading">
          <i class="el-icon-video-play"></i> 开始预览
        </el-button>
      </span>
    </el-dialog>

    <!-- 模板保存对话框 -->
    <el-dialog title="保存为模板" :visible.sync="templateSaveDialogVisible" width="500px">
      <el-form :model="templateForm" label-width="100px" size="medium">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.templateName" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板编码" required>
          <el-input v-model="templateForm.templateCode" placeholder="请输入模板编码（英文）" />
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="3" placeholder="简要描述模板用途" />
        </el-form-item>
        <el-form-item label="场景标签">
          <el-input v-model="templateForm.tags" placeholder="用逗号分隔，如：用户,脱敏" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="templateSaveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </span>
    </el-dialog>

    <!-- 模板列表对话框 -->
    <el-dialog title="选择模板" :visible.sync="templateListDialogVisible" width="800px">
      <div style="margin-bottom: 15px">
        <el-input v-model="templateKeyword" placeholder="搜索模板名称、编码或标签" size="small" style="width: 300px">
          <i slot="prefix" class="el-icon-search"></i>
        </el-input>
        <el-button type="primary" size="small" @click="loadTemplateList" style="margin-left: 10px">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
      <el-table :data="templateList" border max-height="400">
        <el-table-column label="模板名称" prop="templateName" min-width="150" />
        <el-table-column label="模板编码" prop="templateCode" width="150" />
        <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="标签" width="150">
          <template slot-scope="scope">
            <el-tag v-for="tag in (scope.row.tags || '').split(',')"
:key="tag" size="mini" style="margin-right: 5px">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="使用次数" prop="useCount" width="90" align="center" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="applyTemplate(scope.row)">
              <i class="el-icon-check"></i> 应用
            </el-button>
            <el-button type="text" size="small" @click="deleteTemplate(scope.row.id)" style="color: #f56c6c">
              <i class="el-icon-delete"></i> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 15px; text-align: right">
        <el-pagination
          @size-change="handleTemplatePageSizeChange"
          @current-change="handleTemplatePageChange"
          :current-page="templatePage"
          :page-sizes="[10, 20, 50]"
          :page-size="templatePageSize"
          :total="templateTotal"
          layout="total, sizes, prev, pager, next, jumper">
        </el-pagination>
      </div>
      <span slot="footer">
        <el-button @click="templateListDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
    
    <!-- 数据清洗、校验和脱敏配置对话框 -->
    <el-dialog title="字段配置" :visible.sync="cleanseDialogVisible" width="700px">
      <!-- 数据处理流程说明 -->
      <el-alert 
        v-if="currentMapping"
        type="info" 
        :closable="false"
        style="margin-bottom: 15px">
        <div slot="title" style="line-height: 1.8">
          <strong>数据处理流程顺序:</strong> 
          <span style="color: #606266">{{ currentMapping.sourceField }}</span>
          <i class="el-icon-right" style="margin: 0 5px; color: #909399"></i>
          <el-tag size="mini" type="info">①空值处理</el-tag>
          <i class="el-icon-right" style="margin: 0 5px; color: #909399"></i>
          <el-tag size="mini" type="success">②数据清洗</el-tag>
          <i class="el-icon-right" style="margin: 0 5px; color: #909399"></i>
          <el-tag size="mini" type="warning">③数据转换</el-tag>
          <i class="el-icon-right" style="margin: 0 5px; color: #909399"></i>
          <el-tag size="mini" type="info">④数据脱敏</el-tag>
          <i class="el-icon-right" style="margin: 0 5px; color: #909399"></i>
          <span style="color: #606266">{{ currentMapping.targetField }}</span>
        </div>
      </el-alert>
      <el-tabs v-if="currentMapping" type="border-card">
        <!-- 数据清洗与转换 -->
        <el-tab-pane>
          <span slot="label"><i class="el-icon-s-tools"></i> ①②③ 清洗与转换</span>
          <el-form label-width="120px" size="medium">
            <el-form-item label="字段">
              <el-tag>{{ currentMapping.sourceField }} → {{ currentMapping.targetField }}</el-tag>
            </el-form-item>
            
            <el-divider><i class="el-icon-s-tools"></i> ① 空值处理</el-divider>
            <el-form-item label="空值策略">
              <el-select v-model="currentMapping.nullStrategy" placeholder="请选择策略" style="width: 100%">
                <el-option label="保持空值" value="KEEP">
                  <span>保持空值</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">不做处理</span>
                </el-option>
                <el-option label="使用默认值" value="DEFAULT">
                  <span>使用默认值</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">用默认值替换</span>
                </el-option>
                <el-option label="跳过该字段" value="SKIP">
                  <span>跳过该字段</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">不写入目标</span>
                </el-option>
                <el-option label="移除整行" value="REMOVE">
                  <span>移除整行</span>
                  <span style="float: right; color: #8492a6; font-size: 12px; color: #f56c6c">删除这条数据</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item v-if="currentMapping.nullStrategy === 'DEFAULT'" label="默认值">
              <el-input v-model="currentMapping.defaultValue" placeholder="请输入默认值，支持 {CURRENT_DATE} / {CURRENT_DATETIME}" />
            </el-form-item>
            
            <el-divider>
              <i class="el-icon-sort"></i> ③ 数据转换 
              <el-tag size="mini" type="warning" style="margin-left: 5px">{{ getTransformTypeLabel(currentMapping.transformType) }}</el-tag>
            </el-divider>
            <el-alert type="warning" :closable="false" style="margin-bottom: 10px">
              清洗后再转换！当前字段已配置转换类型，请在“字段映射”步骤中修改
            </el-alert>
            
            <el-divider><i class="el-icon-magic-stick"></i> ② 数据清洗</el-divider>
            <el-form-item>
              <div slot="label">
                清洗函数链
                <el-tooltip content="在转换之前清洗数据，去除空格、特殊字符等" placement="top">
                  <i class="el-icon-question" style="color: #909399"></i>
                </el-tooltip>
              </div>
              
              <!-- 插件式配置按钮 -->
              <el-button 
                type="primary" 
                icon="el-icon-s-operation"
                style="width: 100%; margin-bottom: 10px"
                @click="openPluginFlow(currentMapping)">
                插件流程配置
              </el-button>
              
              <!-- 函数列表 -->
              <div v-if="currentMapping._cleanseFunctions && currentMapping._cleanseFunctions.length > 0" 
                   style="margin-bottom: 10px; padding: 10px; background: #f5f7fa; border-radius: 4px">
                <div 
                  v-for="(func, index) in currentMapping._cleanseFunctions" 
                  :key="index"
                  style="margin-bottom: 8px; padding: 8px; background: white; border-radius: 4px; border-left: 3px solid #409EFF">
                  <div style="display: flex; align-items: center; margin-bottom: 5px">
                    <el-tag size="mini" type="info" style="margin-right: 8px">{{ index + 1 }}</el-tag>
                    <strong style="flex: 1">{{ getFunctionName(func.functionCode) }}</strong>
                    <el-button-group>
                      <el-button 
                        size="mini" 
                        icon="el-icon-arrow-up" 
                        :disabled="index === 0"
                        @click="moveFunctionUp(index)"></el-button>
                      <el-button 
                        size="mini" 
                        icon="el-icon-arrow-down" 
                        :disabled="index === currentMapping._cleanseFunctions.length - 1"
                        @click="moveFunctionDown(index)"></el-button>
                      <el-button 
                        size="mini" 
                        type="danger" 
                        icon="el-icon-delete"
                        @click="removeCleanseFunction(index)"></el-button>
                    </el-button-group>
                  </div>
                  <div v-if="func.params && Object.keys(func.params).length > 0" style="font-size: 12px; color: #909399">
                    <span v-for="(val, key) in func.params" :key="key" style="margin-right: 10px">
                      {{ key }}: {{ val }}
                    </span>
                  </div>
                </div>
              </div>
              <div v-else style="padding: 20px; text-align: center; color: #909399; background: #f5f7fa; border-radius: 4px">
                <i class="el-icon-info"></i> 暂无清洗函数，请点击下方按钮添加
              </div>
              
              <el-button 
                size="small" 
                type="primary" 
                icon="el-icon-plus"
                style="margin-top: 10px; width: 100%"
                @click="showAddCleanseFunctionDialog">
                添加清洗函数
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 数据脱敏 -->
        <el-tab-pane>
          <span slot="label"><i class="el-icon-view"></i> ④ 数据脱敏</span>
          <el-form label-width="120px" size="medium">
            <el-form-item label="字段">
              <el-tag>{{ currentMapping.targetField }}</el-tag>
            </el-form-item>
            
            <el-form-item label="脱敏类型">
              <el-select v-model="currentMapping._desensitizeType" placeholder="请选择" style="width: 100%" @change="updateDesensitizeConfig">
                <el-option label="不脱敏" value="NONE" />
                <el-option label="手机号" value="PHONE" />
                <el-option label="身份证号" value="ID_CARD" />
                <el-option label="银行卡号" value="BANK_CARD" />
                <el-option label="邮箱" value="EMAIL" />
                <el-option label="姓名" value="NAME" />
                <el-option label="地址" value="ADDRESS" />
                <el-option label="自定义" value="CUSTOM" />
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="currentMapping._desensitizeType && currentMapping._desensitizeType !== 'NONE'" label="脱敏策略">
              <el-radio-group v-model="currentMapping._desensitizeStrategy" @change="updateDesensitizeConfig">
                <el-radio label="FULL">完全脱敏</el-radio>
                <el-radio label="PARTIAL">部分脱敏</el-radio>
                <el-radio label="MASK">掩码替换</el-radio>
              </el-radio-group>
              <div class="tip">
                <span v-if="currentMapping._desensitizeStrategy === 'FULL'">全部替换为 *</span>
                <span v-else-if="currentMapping._desensitizeStrategy === 'PARTIAL'">保留部分字符，其余替换为 *</span>
                <span v-else-if="currentMapping._desensitizeStrategy === 'MASK'">使用指定字符掩盖</span>
              </div>
            </el-form-item>
            
            <!-- 自定义脱敏配置 -->
            <template v-if="currentMapping._desensitizeType === 'CUSTOM'">
              <el-divider><i class="el-icon-setting"></i> 自定义设置</el-divider>
              <el-form-item label="正则验证">
                <el-input v-model="currentMapping._desensitizePattern" placeholder="可选，用于验证格式" @input="updateDesensitizeConfig" />
              </el-form-item>
              <el-form-item label="掩码字符">
                <el-input v-model="currentMapping._desensitizeMaskChar" placeholder="默认: *" maxlength="1" style="width: 100px" @input="updateDesensitizeConfig" />
              </el-form-item>
              <el-form-item label="保留位数">
                <div style="display: flex; gap: 8px">
                  <el-input-number v-model="currentMapping._desensitizeKeepStart" placeholder="开始" style="flex: 1" :min="0" @change="updateDesensitizeConfig" />
                  <el-input-number v-model="currentMapping._desensitizeKeepEnd" placeholder="结束" style="flex: 1" :min="0" @change="updateDesensitizeConfig" />
                </div>
              </el-form-item>
            </template>
            
            <!-- 脱敏示例 -->
            <el-form-item v-if="currentMapping._desensitizeType && currentMapping._desensitizeType !== 'NONE'" label="示例">
              <el-alert :closable="false" type="info">
                <template v-if="currentMapping._desensitizeType === 'PHONE'">
                  原文: 13812345678 → 脱敏: {{ getDesensitizeExample('13812345678') }}
                </template>
                <template v-else-if="currentMapping._desensitizeType === 'ID_CARD'">
                  原文: 110101199001011234 → 脱敏: {{ getDesensitizeExample('110101199001011234') }}
                </template>
                <template v-else-if="currentMapping._desensitizeType === 'BANK_CARD'">
                  原文: 6222021234567890123 → 脱敏: {{ getDesensitizeExample('6222021234567890123') }}
                </template>
                <template v-else-if="currentMapping._desensitizeType === 'EMAIL'">
                  原文: zhangsan@example.com → 脱敏: {{ getDesensitizeExample('zhangsan@example.com') }}
                </template>
                <template v-else-if="currentMapping._desensitizeType === 'NAME'">
                  原文: 张三 → 脱敏: {{ getDesensitizeExample('张三') }}
                </template>
                <template v-else-if="currentMapping._desensitizeType === 'ADDRESS'">
                  原文: 北京市海淀区中关村 → 脱敏: {{ getDesensitizeExample('北京市海淀区中关村') }}
                </template>
                <template v-else>
                  请配置脱敏规则
                </template>
              </el-alert>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <span slot="footer">
        <el-button @click="cleanseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCleanseConfig">保存</el-button>
      </span>
    </el-dialog>
    
    <!-- 添加清洗函数对话框 -->
    <el-dialog title="选择清洗函数" :visible.sync="addCleanseFunctionDialogVisible" width="650px">
      <el-form label-width="100px" size="medium">
        <el-form-item label="函数分类">
          <el-radio-group v-model="selectedFunctionCategory" @change="onCategoryChange">
            <el-radio-button label="TEXT">📝 文本处理</el-radio-button>
            <el-radio-button label="NUMBER">🔢 数值处理</el-radio-button>
            <el-radio-button label="DATE">📅 日期处理</el-radio-button>
            <el-radio-button label="NULL">⁉️ 空值处理</el-radio-button>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="选择函数">
          <el-select 
            v-model="selectedFunctionCode" 
            placeholder="请选择函数" 
            style="width: 100%"
            @change="onFunctionSelect">
            <el-option
              v-for="func in filteredFunctions"
              :key="func.functionCode"
              :label="func.functionName"
              :value="func.functionCode">
              <span style="float: left">{{ func.functionName }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px">{{ func.description }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        
        <!-- 函数参数配置 -->
        <template v-if="selectedFunction && selectedFunction.params && selectedFunction.params.length > 0">
          <el-divider><i class="el-icon-setting"></i> 参数配置</el-divider>
          
          <el-form-item 
            v-for="param in selectedFunction.params" 
            :key="param.paramName"
            :label="param.paramLabel"
            :required="param.required">
            
            <!-- 字符串类型 -->
            <el-input 
              v-if="param.paramType === 'STRING'"
              v-model="functionParams[param.paramName]"
              :placeholder="param.placeholder || '请输入' + param.paramLabel" />
            
            <!-- 数值类型 -->
            <el-input-number 
              v-else-if="param.paramType === 'NUMBER'"
              v-model="functionParams[param.paramName]"
              :placeholder="param.placeholder || '请输入' + param.paramLabel"
              style="width: 100%" />
            
            <!-- 布尔类型 -->
            <el-switch 
              v-else-if="param.paramType === 'BOOLEAN'"
              v-model="functionParams[param.paramName]" />
            
            <!-- 枚举类型 -->
            <el-select 
              v-else-if="param.paramType === 'ENUM'"
              v-model="functionParams[param.paramName]"
              :placeholder="'请选择' + param.paramLabel"
              style="width: 100%">
              <el-option
                v-for="opt in param.enumOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value" />
            </el-select>
          </el-form-item>
          
          <!-- 函数预览 -->
          <el-form-item label="效果预览">
            <div style="display: flex; gap: 10px; align-items: center">
              <el-input 
                v-model="previewInputValue" 
                placeholder="输入测试值" 
                style="flex: 1"
                @input="previewCleanseFunction" />
              <el-tag v-if="previewOutputValue !== null" type="success">
                结果: {{ previewOutputValue }}
              </el-tag>
            </div>
          </el-form-item>
        </template>
      </el-form>
      
      <span slot="footer">
        <el-button @click="addCleanseFunctionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddCleanseFunction" :disabled="!selectedFunctionCode">添加</el-button>
      </span>
    </el-dialog>
    
    <!-- 数据血缘追踪对话框 -->
    <el-dialog title="数据血缘追踪" :visible.sync="lineageDialogVisible" width="900px">
      <el-alert
        title="血缘说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px">
        查看当前任务的数据血缘关系，跟踪字段级别的依赖和转换关系。
      </el-alert>
      
      <el-form label-width="120px" size="medium">
        <el-form-item label="数据源">
          <el-select v-model="lineageConfig.connectorId" placeholder="请选择数据源" style="width: 300px" @change="loadLineageTables">
            <el-option 
              v-for="item in connectorList" 
              :key="item.id" 
              :label="item.connectorName" 
              :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="表名">
          <el-select v-model="lineageConfig.tableName" placeholder="请选择表" style="width: 300px" @change="loadLineageFields" filterable>
            <el-option 
              v-for="table in lineageTableList" 
              :key="table" 
              :label="table" 
              :value="table" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="字段名">
          <el-select v-model="lineageConfig.fieldName" placeholder="请选择字段" style="width: 300px" filterable>
            <el-option 
              v-for="field in lineageFieldList" 
              :key="field" 
              :label="field" 
              :value="field" />
          </el-select>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="queryLineage" :loading="lineageLoading">
            <i class="el-icon-search"></i> 查询血缘
          </el-button>
          <el-button @click="exportLineageGraph" :disabled="!lineageGraphData">
            <i class="el-icon-download"></i> 导出图谱
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 血缘图谱展示 -->
      <div v-if="lineageGraphData" style="margin-top: 20px">
        <el-divider><i class="el-icon-share"></i> 血缘图谱</el-divider>
        
        <el-tabs>
          <el-tab-pane label="上游血缘">
            <el-table :data="lineageGraphData.upstream || []" border size="small" max-height="300">
              <el-table-column prop="sourceConnectorName" label="源连接器" width="150" />
              <el-table-column prop="sourceTable" label="源表" width="120" />
              <el-table-column prop="sourceField" label="源字段" width="120" />
              <el-table-column prop="targetConnectorName" label="目标连接器" width="150" />
              <el-table-column prop="targetTable" label="目标表" width="120" />
              <el-table-column prop="targetField" label="目标字段" width="120" />
              <el-table-column prop="transformRule" label="转换规则" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="下游血缘">
            <el-table :data="lineageGraphData.downstream || []" border size="small" max-height="300">
              <el-table-column prop="sourceConnectorName" label="源连接器" width="150" />
              <el-table-column prop="sourceTable" label="源表" width="120" />
              <el-table-column prop="sourceField" label="源字段" width="120" />
              <el-table-column prop="targetConnectorName" label="目标连接器" width="150" />
              <el-table-column prop="targetTable" label="目标表" width="120" />
              <el-table-column prop="targetField" label="目标字段" width="120" />
              <el-table-column prop="transformRule" label="转换规则" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
        
        <div style="margin-top: 15px; color: #606266">
          <i class="el-icon-info"></i> 上游记录：{{ (lineageGraphData.upstream || []).length }} 条，
          下游记录：{{ (lineageGraphData.downstream || []).length }} 条
        </div>
      </div>
      
      <span slot="footer">
        <el-button @click="lineageDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
    
    <!-- 数据对比对话框 -->
    <el-dialog title="数据对比" :visible.sync="compareDialogVisible" width="1000px">
      <el-alert
        title="对比说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px">
        对比源端和目标端数据的一致性，用于校验同步结果。
      </el-alert>
      
      <el-form label-width="120px" size="medium">
        <el-form-item label="源连接器">
          <el-select v-model="compareConfig.sourceConnectorId" placeholder="请选择源连接器" style="width: 300px">
            <el-option 
              v-for="item in connectorList" 
              :key="item.id" 
              :label="item.connectorName" 
              :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="源表/SQL">
          <el-input 
            v-model="compareConfig.sourceTableOrSql" 
            placeholder="请输入表名或SQL语句"
            type="textarea"
            :rows="2"
            style="width: 600px" />
        </el-form-item>
        
        <el-form-item label="目标连接器">
          <el-select v-model="compareConfig.targetConnectorId" placeholder="请选择目标连接器" style="width: 300px">
            <el-option 
              v-for="item in connectorList" 
              :key="item.id" 
              :label="item.connectorName" 
              :value="item.id" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="目标表/SQL">
          <el-input 
            v-model="compareConfig.targetTableOrSql" 
            placeholder="请输入表名或SQL语句"
            type="textarea"
            :rows="2"
            style="width: 600px" />
        </el-form-item>
        
        <el-form-item label="对比字段">
          <el-input 
            v-model="compareConfig.compareFields" 
            placeholder="多个字段用逗号分隔，如：id,name,amount（留空则对比所有字段）"
            style="width: 600px" />
        </el-form-item>
        
        <el-form-item label="主键字段">
          <el-input 
            v-model="compareConfig.keyFields" 
            placeholder="多个字段用逗号分隔，如：id"
            style="width: 600px" />
          <div style="margin-top: 5px; color: #909399; font-size: 12px">
            <i class="el-icon-info"></i> 用于匹配源端和目标端记录的唯一标识
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="executeCompare" :loading="compareLoading">
            <i class="el-icon-s-check"></i> 开始对比
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 对比结果展示 -->
      <div v-if="compareResult" style="margin-top: 20px">
        <el-divider><i class="el-icon-s-data"></i> 对比结果</el-divider>
        
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 32px; color: #67C23A; font-weight: bold">
                  {{ compareResult.matchCount || 0 }}
                </div>
                <div style="margin-top: 10px; color: #606266">匹配记录</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 32px; color: #E6A23C; font-weight: bold">
                  {{ compareResult.diffCount || 0 }}
                </div>
                <div style="margin-top: 10px; color: #606266">差异记录</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 32px; color: #F56C6C; font-weight: bold">
                  {{ compareResult.sourceOnlyCount || 0 }}
                </div>
                <div style="margin-top: 10px; color: #606266">源端独有</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 32px; color: #909399; font-weight: bold">
                  {{ compareResult.targetOnlyCount || 0 }}
                </div>
                <div style="margin-top: 10px; color: #606266">目标端独有</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 差异详情 -->
        <el-tabs v-if="compareResult.differences && compareResult.differences.length > 0">
          <el-tab-pane label="差异详情">
            <el-table :data="compareResult.differences.slice(0, 100)" border size="small" max-height="300">
              <el-table-column prop="key" label="记录标识" width="150" />
              <el-table-column prop="field" label="字段" width="120" />
              <el-table-column prop="sourceValue" label="源端值" show-overflow-tooltip />
              <el-table-column prop="targetValue" label="目标端值" show-overflow-tooltip />
            </el-table>
            <div style="margin-top: 10px; color: #909399; font-size: 12px">
              <i class="el-icon-info"></i> 最多显示100条差异记录
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <span slot="footer">
        <el-button @click="compareDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
    
    <!-- 插件流程配置对话框 -->
    <TransformPluginFlow
      :visible.sync="pluginFlowDialogVisible"
      :mapping="pluginFlowMapping"
      @save="handlePluginFlowSave"
    />
    
    <!-- 添加处理步骤对话框 -->
    <el-dialog
      title="添加处理步骤"
      :visible.sync="addProcessorDialogVisible"
      width="600px"
      :close-on-click-modal="false">
      <el-form :model="newProcessor" label-width="100px" size="medium">
        <el-form-item label="处理类型">
          <el-radio-group v-model="newProcessor.type" @change="handleProcessorTypeChange">
            <el-radio-button label="NULL_HANDLE" :disabled="isProcessorTypeDisabled('NULL_HANDLE')">
              <i class="el-icon-close"></i> 空值处理
            </el-radio-button>
            <el-radio-button label="CLEANSE" :disabled="isProcessorTypeDisabled('CLEANSE')">
              <i class="el-icon-brush"></i> 数据清洗
            </el-radio-button>
            <el-radio-button label="TRANSFORM" :disabled="isProcessorTypeDisabled('TRANSFORM')">
              <i class="el-icon-refresh"></i> 数据转换
            </el-radio-button>
          </el-radio-group>
          <div v-if="hasDisabledTypes" style="margin-top: 8px; color: #909399; font-size: 12px">
            <i class="el-icon-info"></i> 已添加的类型不可重复添加
          </div>
        </el-form-item>
        
        <!-- 空值处理配置 -->
        <template v-if="newProcessor.type === 'NULL_HANDLE'">
          <el-divider><i class="el-icon-setting"></i> 空值处理配置</el-divider>
          <el-form-item label="处理策略">
            <el-select v-model="newProcessor.config.strategy" style="width: 100%">
              <el-option label="保持空值 (KEEP)" value="KEEP">
                <span>保持空值</span>
                <span style="float: right; color: #8492a6; font-size: 12px">NULL</span>
              </el-option>
              <el-option label="设置默认值 (DEFAULT)" value="DEFAULT">
                <span>设置默认值</span>
                <span style="float: right; color: #8492a6; font-size: 12px">自定义值</span>
              </el-option>
              <el-option label="跳过该行 (SKIP)" value="SKIP">
                <span>跳过该行</span>
                <span style="float: right; color: #8492a6; font-size: 12px">不写入</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="newProcessor.config.strategy === 'DEFAULT'" label="默认值">
            <el-input v-model="newProcessor.config.defaultValue" placeholder="请输入默认值，支持 {CURRENT_DATE} / {CURRENT_DATETIME}" />
          </el-form-item>
          
          <el-alert type="info" :closable="false" style="margin-top: 15px">
            <div slot="title">
              <i class="el-icon-info"></i> 策略说明
            </div>
            <div v-if="newProcessor.config.strategy === 'KEEP'">当源字段为空值时，保持null写入目标字段</div>
            <div v-else-if="newProcessor.config.strategy === 'DEFAULT'">
              当源字段为空值时，写入指定的默认值；支持占位符：{CURRENT_DATE}=当前日期(yyyy-MM-dd)，{CURRENT_DATETIME}=当前时间(yyyy-MM-dd HH:mm:ss)
            </div>
            <div v-else-if="newProcessor.config.strategy === 'SKIP'">当源字段为空值时，跳过该条数据，不写入目标端</div>
          </el-alert>
        </template>
        
        <!-- 数据清洗配置 -->
        <template v-else-if="newProcessor.type === 'CLEANSE'">
          <el-divider><i class="el-icon-brush"></i> 数据清洗配置</el-divider>
          
          <el-alert type="info" :closable="false">
            <div slot="title">
              <i class="el-icon-info"></i> 配置说明
            </div>
            点击下方按钮打开插件流程配置界面，通过拖拽插件构建数据清洗链。<br>
            <strong>必须添加至少一个清洗插件</strong>，否则无法保存。
          </el-alert>
          
          <el-button
            type="primary"
            size="medium"
            icon="el-icon-setting"
            style="width: 100%; margin-top: 15px"
            @click="openPluginFlowForNewProcessor">
            <i class="el-icon-s-operation"></i> 打开插件流程配置
          </el-button>
          
          <!-- 显示已配置的插件数量 -->
          <div v-if="newProcessor.config.cleanseFunctions && newProcessor.config.cleanseFunctions.length > 0" 
               style="margin-top: 15px; padding: 10px; background: #F0F9FF; border-left: 4px solid #409EFF; border-radius: 4px">
            <i class="el-icon-success" style="color: #67C23A"></i>
            <strong>已配置 {{ newProcessor.config.cleanseFunctions.length }} 个清洗插件</strong>
          </div>
        </template>
        
        <!-- 数据转换配置 -->
        <template v-else-if="newProcessor.type === 'TRANSFORM'">
          <el-divider><i class="el-icon-refresh"></i> 数据转换配置</el-divider>
          <el-form-item label="转换类型">
            <el-radio-group v-model="newProcessor.config.transformType">
              <el-radio-button label="DIRECT">直接映射</el-radio-button>
              <el-radio-button label="DICT">字典映射</el-radio-button>
              <el-radio-button label="SCRIPT">脚本转换</el-radio-button>
              <el-radio-button label="CONSTANT">固定值</el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <!-- 直接映射 -->
          <template v-if="newProcessor.config.transformType === 'DIRECT'">
            <el-alert type="info" :closable="false" style="margin-top: 10px">
              <div slot="title">
                <i class="el-icon-info"></i> 直接映射说明
              </div>
              直接将源字段的值映射到目标字段，不做任何转换处理
            </el-alert>
          </template>
          
          <!-- 字典映射 -->
          <template v-else-if="newProcessor.config.transformType === 'DICT'">
            <el-form-item label="字典选择">
              <el-select v-model="newProcessor.config.dictMappingId" placeholder="请选择字典" style="width: 100%" @change="handleNewProcessorDictChange" filterable clearable>
                <el-option 
                  v-for="dict in dictList" 
                  :key="dict.id" 
                  :label="dict.mappingName" 
                  :value="dict.id">
                  <span style="float: left">{{ dict.mappingName }}</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">{{ dict.mappingCode }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item v-if="newProcessor.config._dictTypeList && newProcessor.config._dictTypeList.length > 0" label="字典类型">
              <el-select v-model="newProcessor.config.dictSourceTypeValue" placeholder="请选择字典类型" style="width: 100%" filterable clearable>
                <el-option 
                  v-for="(type, index) in newProcessor.config._dictTypeList" 
                  :key="index + '-' + type.value" 
                  :label="type.label" 
                  :value="type.value">
                  <span style="float: left">{{ type.label }}</span>
                  <span style="float: right; color: #8492a6; font-size: 12px">{{ type.value }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            
            <el-form-item label="输出模式">
              <el-radio-group v-model="newProcessor.config.dictOutputMode">
                <el-radio label="TARGET_KEY">目标编码</el-radio>
                <el-radio label="SOURCE_LABEL">源名称</el-radio>
                <el-radio label="TARGET_LABEL">目标名称</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="默认值">
              <el-input v-model="newProcessor.config.defaultValue" placeholder="未匹配时的默认值" />
            </el-form-item>
          </template>
          
          <!-- 脚本转换 -->
          <template v-else-if="newProcessor.config.transformType === 'SCRIPT'">
            <el-form-item label="转换脚本">
              <GroovyEditor 
                v-model="newProcessor.config.transformScript"
                :test-value="previewTestValue"
                placeholder="请输入Groovy脚本"
              />
              <div class="tip" style="margin-top: 8px">
                <i class="el-icon-info"></i> <strong>支持Groovy语法</strong>，可使用Java标准库和Groovy特性
              </div>
              <el-alert type="info" :closable="false" style="margin-top: 10px">
                <div slot="title">
                  <i class="el-icon-info"></i> 可用变量
                </div>
                <ul style="margin: 5px 0 0 0; padding-left: 20px; line-height: 1.8">
                  <li><code>value</code> - 当前字段值</li>
                  <li><code>row</code> - 当前数据行 (Map类型)，使用 row.get('字段名') 获取其他字段值</li>
                </ul>
              </el-alert>
            </el-form-item>
          </template>
          
          <!-- 固定值 -->
          <template v-else-if="newProcessor.config.transformType === 'CONSTANT'">
            <el-form-item label="固定值">
              <el-input v-model="newProcessor.config.constantValue" placeholder="请输入固定值" />
              <div class="tip" style="margin-top: 4px">
                <i class="el-icon-info"></i> 不读取源字段，直接设置为此固定值
              </div>
            </el-form-item>
          </template>
        </template>
      </el-form>
      
      <div slot="footer">
        <el-button @click="addProcessorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddProcessor">确定添加</el-button>
      </div>
    </el-dialog>
    
    <!-- 空值处理配置对话框 -->
    <el-dialog
      title="配置空值处理"
      :visible.sync="configNullHandleDialogVisible"
      width="500px"
      :close-on-click-modal="false">
      <el-form v-if="editingNullHandleProcessor" label-width="100px" size="medium">
        <el-form-item label="处理策略">
          <el-select v-model="editingNullHandleProcessor.config.strategy" style="width: 100%">
            <el-option label="保持空值 (KEEP)" value="KEEP">
              <span>保持空值</span>
              <span style="float: right; color: #8492a6; font-size: 12px">NULL</span>
            </el-option>
            <el-option label="设置默认值 (DEFAULT)" value="DEFAULT">
              <span>设置默认值</span>
              <span style="float: right; color: #8492a6; font-size: 12px">自定义值</span>
            </el-option>
            <el-option label="跳过该行 (SKIP)" value="SKIP">
              <span>跳过该行</span>
              <span style="float: right; color: #8492a6; font-size: 12px">不写入</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingNullHandleProcessor.config.strategy === 'DEFAULT'" label="默认值">
          <el-input v-model="editingNullHandleProcessor.config.defaultValue" placeholder="请输入默认值，支持 {CURRENT_DATE} / {CURRENT_DATETIME}" />
        </el-form-item>
        
        <el-alert type="info" :closable="false" style="margin-top: 15px">
          <div slot="title">
            <i class="el-icon-info"></i> 策略说明
          </div>
          <div v-if="editingNullHandleProcessor.config.strategy === 'KEEP'">当源字段为空值时，保持null写入目标字段</div>
          <div v-else-if="editingNullHandleProcessor.config.strategy === 'DEFAULT'">
            当源字段为空值时，写入指定的默认值；支持占位符：{CURRENT_DATE}=当前日期(yyyy-MM-dd)，{CURRENT_DATETIME}=当前时间(yyyy-MM-dd HH:mm:ss)
          </div>
          <div v-else-if="editingNullHandleProcessor.config.strategy === 'SKIP'">当源字段为空值时，跳过该条数据，不写入目标端</div>
        </el-alert>
      </el-form>
      
      <div slot="footer">
        <el-button @click="configNullHandleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveNullHandleConfig">保存</el-button>
      </div>
    </el-dialog>
    
    <!-- 高级配置对话框 -->
    <el-dialog
      title="数据转换配置"
      :visible.sync="advancedConfigDialogVisible"
      width="700px"
      :close-on-click-modal="false">
      <div v-if="advancedConfigMapping">
        <el-form label-width="120px" size="small">
          <el-form-item label="转换类型">
            <el-radio-group v-model="advancedConfigMapping.transformType" @change="handleTransformTypeChange(advancedConfigMapping)">
              <el-radio-button label="DIRECT">直接映射</el-radio-button>
              <el-radio-button label="DICT">字典映射</el-radio-button>
              <el-radio-button label="SCRIPT">脚本转换</el-radio-button>
              <el-radio-button label="CONSTANT">固定值</el-radio-button>
            </el-radio-group>
          </el-form-item>
          
          <el-divider></el-divider>
              
              <!-- 直接映射 -->
              <template v-if="advancedConfigMapping.transformType === 'DIRECT'">
                <el-alert type="info" :closable="false">
                  <div slot="title">
                    <i class="el-icon-info"></i> 直接映射说明
                  </div>
                  直接将源字段的值映射到目标字段，不做任何转换处理
                </el-alert>
              </template>
              
              <!-- 字典映射 -->
              <template v-else-if="advancedConfigMapping.transformType === 'DICT'">
                <el-form-item label="字典选择">
                  <el-select v-model="advancedConfigMapping.dictMappingId" placeholder="请选择字典" style="width: 100%" @change="handleDictMappingChange(advancedConfigMapping)" filterable clearable>
                    <el-option 
                      v-for="dict in dictList" 
                      :key="dict.id" 
                      :label="dict.mappingName" 
                      :value="dict.id">
                      <span style="float: left">{{ dict.mappingName }}</span>
                      <span style="float: right; color: #8492a6; font-size: 12px">{{ dict.mappingCode }}</span>
                    </el-option>
                  </el-select>
                </el-form-item>
                
                <el-form-item v-if="advancedConfigMapping._dictTypeList && advancedConfigMapping._dictTypeList.length > 0" label="字典类型">
                  <el-select v-model="advancedConfigMapping.dictSourceTypeValue" placeholder="请选择字典类型" style="width: 100%" filterable clearable>
                    <el-option 
                      v-for="(type, index) in advancedConfigMapping._dictTypeList" 
                      :key="index + '-' + type.value" 
                      :label="type.label" 
                      :value="type.value">
                      <span style="float: left">{{ type.label }}</span>
                      <span style="float: right; color: #8492a6; font-size: 12px">{{ type.value }}</span>
                    </el-option>
                  </el-select>
                </el-form-item>
                
                <el-form-item label="输出模式">
                  <el-radio-group v-model="advancedConfigMapping.dictOutputMode">
                    <el-radio label="TARGET_KEY">目标编码</el-radio>
                    <el-radio label="SOURCE_LABEL">源名称</el-radio>
                    <el-radio label="TARGET_LABEL">目标名称</el-radio>
                  </el-radio-group>
                  <div style="margin-top: 5px; color: #909399; font-size: 12px">
                    <span v-if="advancedConfigMapping.dictOutputMode === 'TARGET_KEY' || !advancedConfigMapping.dictOutputMode">输出目标字典的编码值（默认）</span>
                    <span v-else-if="advancedConfigMapping.dictOutputMode === 'SOURCE_LABEL'">输出源字典的名称值</span>
                    <span v-else-if="advancedConfigMapping.dictOutputMode === 'TARGET_LABEL'">输出目标字典的名称值</span>
                  </div>
                </el-form-item>
                
                <el-form-item label="默认值">
                  <el-input v-model="advancedConfigMapping.defaultValue" placeholder="未匹配时的默认值" />
                </el-form-item>
              </template>
              
              <!-- 脚本转换 -->
              <template v-else-if="advancedConfigMapping.transformType === 'SCRIPT'">
                <el-form-item label="转换脚本">
                  <GroovyEditor 
                    v-model="advancedConfigMapping.transformScript"
                    :test-value="previewTestValue"
                    placeholder="请输入Groovy脚本"
                  />
                  <div class="tip" style="margin-top: 8px">
                    <i class="el-icon-info"></i> <strong>支持Groovy语法</strong>，可使用Java标准库和Groovy特性
                  </div>
                  <el-alert type="info" :closable="false" style="margin-top: 10px">
                    <div slot="title">
                      <i class="el-icon-info"></i> 可用变量
                    </div>
                    <ul style="margin: 5px 0 0 0; padding-left: 20px; line-height: 1.8">
                      <li><code>value</code> - 当前字段值</li>
                      <li><code>row</code> - 当前数据行 (Map类型)，使用 row.get('字段名') 获取其他字段值</li>
                    </ul>
                  </el-alert>
                </el-form-item>
              </template>
              
              <!-- 固定值 -->
              <template v-else-if="advancedConfigMapping.transformType === 'CONSTANT'">
                <el-form-item label="固定值">
                  <el-input v-model="advancedConfigMapping.constantValue" placeholder="请输入固定值" />
                  <div class="tip" style="margin-top: 4px">
                    <i class="el-icon-info"></i> 不读取源字段，直接设置为此固定值
                  </div>
                </el-form-item>
              </template>
            </el-form>
          </div>
      
      <div slot="footer">
        <el-button @click="advancedConfigDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAdvancedConfig">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import TransformPluginFlow from '@/components/TransformPluginFlow.vue'

import GroovyEditor from '@/components/GroovyEditor.vue'

export default {
  components: {
    TransformPluginFlow,
    GroovyEditor
  },
  data() {
    return {
      sqlValidationLoading: false,
      sqlValidationError: '',
      activeStep: 0,
      
      // 步骤定义
      steps: [
        { title: '基本信息', icon: 'el-icon-edit', description: '配置任务基础信息' },
        { title: '选择源连接器', icon: 'el-icon-upload', description: '配置数据来源' },
        { title: '辅助数据源', icon: 'el-icon-coin', description: '配置多表关联' },
        { title: '选择目标连接器', icon: 'el-icon-download', description: '配置数据目标' },
        { title: '数据转换', icon: 'el-icon-sort', description: '配置字段映射' },
        { title: '调度配置', icon: 'el-icon-time', description: '设置执行计划' },
        { title: '推送后处理', icon: 'el-icon-s-operation', description: '配置后续操作' },
        { title: '完成', icon: 'el-icon-check', description: '完成配置' }
      ],
      
      // 步骤数据（用于新的步骤条）
      stepsData: [
        { title: '基本信息', icon: 'el-icon-edit-outline' },
        { title: '源连接器', icon: 'el-icon-upload2' },
        { title: '辅助数据', icon: 'el-icon-coin' },
        { title: '目标连接器', icon: 'el-icon-download' },
        { title: '数据转换', icon: 'el-icon-s-operation' },
        { title: '调度配置', icon: 'el-icon-time' },
        { title: '后续处理', icon: 'el-icon-s-tools' },
        { title: '完成', icon: 'el-icon-circle-check' }
      ],
      
      // 任务基本配置
      taskConfig: {
        id: null, // 编辑模式下的任务ID
        taskName: '',
        taskCode: '',
        syncMode: 'FULL',
        incrementalField: '', // 增量字段
        description: ''
      },
      
      // 上次自动生成的任务编码（用于判断是否需要重新生成）
      lastAutoGeneratedCode: '',
      
      // 源端配置
      sourceConfig: {
        connectorId: null,
        sql: '',
        tableName: '',
        // 性能优化配置
        streamMode: false,  // 流式模式，默认关闭
        fetchBatchSize: 5000,  // 批次大小，默认5000条（性能优化）
        // API配置
        apiPath: '',
        apiMethod: 'GET',
        dataPath: '',
        // API请求参数
        apiParams: [],
        // API Body配置
        bodyType: 'none',
        formData: [],
        jsonBody: '',
        // API Headers配置
        headers: [],
        // API分页配置
        enablePagination: false,
        pageParam: 'page',
        pageSizeParam: 'pageSize',
        startPage: 1,
        pageSize: 100,
        totalPath: ''
      },
      sourceTableList: [],
      sourcePreviewData: [],
      sourcePreviewColumns: [],
      sourceColumnInfos: [],  // 源表字段信息（包含类型）
      sourceApiActiveTab: 'params', // Postman风格标签页
      sourceJsonError: '', // JSON验证错误信息
      selectedTable: '', // 当前选中的表名
      sourcePreviewLoading: false, // 抽取数据加载状态
      loadingTables: false, // 加载表列表状态
      highlightedSql: '', // SQL语法高亮后的HTML
      sqlValidationError: '', // SQL验证错误信息
      showPreviewDialog: false, // 数据预览弹窗控制
      previewLimit: 100, // 抽取数据条数，默认100条
      activePreviewCollapse: ['preview'], // 预览折叠面板控制，默认展开
      
      // 辅助数据源配置（多表关联）
      auxiliaryDatasources: [],
      
      // 目标端配置
      targetConfig: {
        connectorId: null,
        tableName: '',
        writeMode: 'INSERT',
        primaryKey: '',
        // API配置
        apiPath: '',
        apiMethod: 'POST',
        batchMode: 'batch',
        // API请求参数
        apiParams: [],
        // API Headers配置
        headers: [],
        // API批量配置
        batchSize: 100,
        wrapperField: '',
        // API重试配置
        retryTimes: 3,
        retryInterval: 1000
      },
      targetTableList: [],
      targetColumnInfos: [],  // 目标表字段信息（包含类型）
      targetApiActiveTab: 'params', // Postman风格标签页
      
      // 字段映射
      mappings: [],
      
      // 转换预览数据
      transformTab: 'before', // 当前标签页: before/config/after
      transformedPreviewData: [], // 转换后的预览数据
      transformedPreviewColumns: [], // 转换后的字段列表
      transformPreviewLoading: false, // 转换预览加载状态
      
      // 调度配置
      scheduleConfig: {
        scheduleType: 'MANUAL',
        cronExpression: '',
        enableRetry: false,
        maxRetryTimes: 3
      },
      cronTemplate: '',
      cronDescription: '', // Cron表达式描述
      cronVisual: {
        period: 'day', // day, week, month, custom
        weekDay: 'MON',
        dayOfMonth: 1,
        time: '02:00',
        interval: 5,
        unit: 'minute' // minute, hour, day
      },
      
      // 推送后处理配置
      postLoadConfig: {
        statusUpdate: {
          enabled: false,
          updateType: 'SQL',
          keyField: 'id',
          updateSql: '',
          batchSize: 100,
          apiUrl: '',
          method: 'POST',
          params: [],
          headers: [],
          bodyType: 'none',
          formData: [],
          jsonBody: ''
        }
      },
      postLoadApiActiveTab: 'params', // API配置标签页
      postLoadJsonError: '', // JSON验证错误
      postLoadJsonValid: false, // JSON验证成功
      
      // 连接器列表
      connectorList: [],
      
      // 连接器Logo映射
      connectorLogoMap: {
        'MYSQL': require('@/assets/logos/mysql.svg'),
        'ORACLE': require('@/assets/logos/oracle.svg'),
        'POSTGRESQL': require('@/assets/logos/postgresql.svg'),
        'SQLSERVER': require('@/assets/logos/sqlserver.svg'),
        'KINGBASE': require('@/assets/logos/kingbase.png'),
        'DM': require('@/assets/logos/dm.png'),
        'API': require('@/assets/logos/api.svg')
      },
      
      // 字典列表
      dictList: [],
      
      // 扩展功能1: 转换预览
      previewDialogVisible: false,
      previewMapping: {},
      previewTestValue: '',
      previewResult: null,
      previewError: false,
      previewLoading: false,
      
      // 扩展功能3: 模板管理
      templateSaveDialogVisible: false,
      templateListDialogVisible: false,
      templateForm: {
        templateName: '',
        templateCode: '',
        description: '',
        tags: ''
      },
      templateList: [],
      templateKeyword: '',
      templatePage: 1,
      templatePageSize: 10,
      templateTotal: 0,
      
      // 扩展功能5: 虚拟滚动/分页
      mappingCurrentPage: 1,
      mappingPageSize: 20,  // 每页显示20条
      
      // 高级转换: 数据清洗配置
      cleanseDialogVisible: false,
      currentMapping: null,
      
      // 插件流程配置
      pluginFlowDialogVisible: false,
      pluginFlowMapping: null,
      
      // 高级配置对话框
      advancedConfigDialogVisible: false,
      advancedConfigMapping: null,
            
      // 添加处理器对话框
      addProcessorDialogVisible: false,
      currentMappingForProcessor: null,
      currentProcessorIndex: null,
      currentConfigProcessor: null, // 当前正在配置的处理器
      newProcessor: {
        type: 'NULL_HANDLE',
        config: {
          strategy: 'KEEP',
          defaultValue: '',
          functions: [],
          transformType: 'DIRECT',
          transformFunction: '',
          dictType: '',
          constantValue: ''
        }
      },
      
      // 空值处理配置对话框
      configNullHandleDialogVisible: false,
      editingNullHandleProcessor: null,
            
      // 拖拽相关
      draggedProcessorIndex: null,
      draggedMappingRef: null,
      
      // 清洗函数配置
      addCleanseFunctionDialogVisible: false,
      cleanseFunctionList: [], // 所有可用函数
      selectedFunctionCategory: 'TEXT', // 当前选中的分类
      selectedFunctionCode: null, // 当前选中的函数代码
      selectedFunction: null, // 当前选中的函数定义
      functionParams: {}, // 函数参数
      previewInputValue: '', // 预览输入值
      previewOutputValue: null, // 预览输出值
      isAddingToNewProcessor: false, // 是否正在添加到新处理器
      
      // 数据血缘追踪配置
      lineageDialogVisible: false,
      lineageConfig: {
        connectorId: null,
        tableName: '',
        fieldName: ''
      },
      lineageTableList: [],
      lineageFieldList: [],
      lineageGraphData: null,
      lineageLoading: false,
      
      // 数据对比配置
      compareDialogVisible: false,
      compareConfig: {
        sourceConnectorId: null,
        sourceTableOrSql: '',
        targetConnectorId: null,
        targetTableOrSql: '',
        compareFields: '',
        keyFields: ''
      },
      compareResult: null,
      compareLoading: false,
      
      // 草稿自动保存与变更检测
      draftKey: 'task_wizard_draft',
      hasUnsavedChanges: false,
      autoSaveTimer: null,
      lastSavedData: null,
      stepValidationErrors: {}, // 每个步骤的校验错误
      
      // ========== 多目标支持 ==========
      multiTargetMode: false, // 是否多目标模式
      targets: [], // 目标列表
      activeTargetCollapseIndex: [], // 折叠面板激活索引
      activeTargetTab: '0', // 字段映射标签页
      activeTargetPreviewTab: '0', // 转换预览标签页
      targetTransformedData: {} // 各目标的转换数据: { targetIndex: { data: [], columns: [] } }
    }
  },
  watch: {
    // 监听SQL变化，自动更新语法高亮
    'sourceConfig.sql': {
      handler(newVal) {
        this.updateHighlight()
      },
      immediate: false
    },
    
    // 切换转换标签页时，自动加载源/目标字段信息，确保下拉可展示类型、必填和描述
    transformTab(newVal) {
      if (newVal === 'config') {
        // 加载源字段信息
        if (this.sourceConnectorType === 'DATABASE' && this.sourceConfig.connectorId && (this.sourceConfig.tableName || this.sourceConfig.sql)) {
          // 只有在还没有加载过字段信息时才去请求，避免每次切换都打接口
          if (!this.sourceColumnInfos || this.sourceColumnInfos.length === 0) {
            let sourceTableName = this.sourceConfig.tableName
            if (!sourceTableName && this.sourceConfig.sql) {
              const sql = this.sourceConfig.sql.trim()
              const fromMatch = sql.match(/\bFROM\s+([`"\[]?\w+[`"\]]?\.)?([`"\[]?\w+[`"\]]?)/i)
              if (fromMatch) {
                sourceTableName = fromMatch[2].replace(/[`"\[\]]/g, '')
              }
            }
            if (sourceTableName) {
              this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${sourceTableName}/columns-with-type`)
                .then(res => {
                  this.sourceColumnInfos = res.data || []
                })
                .catch(err => {
                  console.warn('加载源表字段信息失败', err)
                })
            }
          }
        }
        
        // 加载目标字段信息（单目标模式）
        if (!this.multiTargetMode && this.targetConnectorType === 'DATABASE' && this.targetConfig.connectorId && this.targetConfig.tableName) {
          if (!this.targetColumnInfos || this.targetColumnInfos.length === 0) {
            this.loadTargetColumns()
          }
        }
      }
    }
  },
  
  computed: {
    // 扩展功能5: 虚拟滚动/分页 - 分页后的mappings
    paginatedMappings() {
      const start = (this.mappingCurrentPage - 1) * this.mappingPageSize
      const end = start + this.mappingPageSize
      return this.mappings.slice(start, end)
    },
    
    sourceConnectorType() {
      // 优先使用模板指定的连接器类型
      if (this.sourceConfig._templateConnectorType) {
        return this.sourceConfig._templateConnectorType
      }
      const connector = this.connectorList.find(c => c.id === this.sourceConfig.connectorId)
      return connector ? connector.connectorType : 'DATABASE'
    },
    sourceConnectorBaseUrl() {
      const connector = this.connectorList.find(c => c.id === this.sourceConfig.connectorId)
      if (!connector) return ''
      // API连接器: 组合http://host:port/baseUri
      if (connector.connectorType === 'API') {
        const protocol = connector.port === 443 ? 'https' : 'http'
        const host = connector.host || 'localhost'
        const port = connector.port || 80
        const baseUri = connector.url || ''
        return `${protocol}://${host}:${port}${baseUri}`
      }
      return connector.url || ''
    },
    targetConnectorType() {
      // 优先使用模板指定的连接器类型
      if (this.targetConfig._templateConnectorType) {
        return this.targetConfig._templateConnectorType
      }
      const connector = this.connectorList.find(c => c.id === this.targetConfig.connectorId)
      return connector ? connector.connectorType : 'DATABASE'
    },
    targetConnectorBaseUrl() {
      const connector = this.connectorList.find(c => c.id === this.targetConfig.connectorId)
      if (!connector) return ''
      // API连接器: 组合http://host:port/baseUri
      if (connector.connectorType === 'API') {
        const protocol = connector.port === 443 ? 'https' : 'http'
        const host = connector.host || 'localhost'
        const port = connector.port || 80
        const baseUri = connector.url || ''
        return `${protocol}://${host}:${port}${baseUri}`
      }
      return connector.url || ''
    },
    // 推送后处理启用的Headers数量
    postLoadEnabledHeadersCount() {
      if (!this.postLoadConfig.statusUpdate.headers) return 0
      return this.postLoadConfig.statusUpdate.headers.filter(h => h.enabled).length
    },
    
    // 根据分类过滤函数
    filteredFunctions() {
      return this.cleanseFunctionList.filter(func => func.category === this.selectedFunctionCategory)
    },
    
    // 是否有禁用的处理器类型
    hasDisabledTypes() {
      if (!this.currentMappingForProcessor || !this.currentMappingForProcessor._processors) {
        return false
      }
      return this.currentMappingForProcessor._processors.length > 0
    }
  },
  mounted() {
    this.loadConnectors()
    this.loadDictList()
    this.loadCleanseFunctions() // 加载清洗函数列表
    
    // 初始化SQL语法高亮
    this.$nextTick(() => {
      this.updateHighlight()
    })
    
    // 检查是否是编辑模式
    if (this.$route.query.taskId) {
      this.loadTaskForEdit(this.$route.query.taskId)
    } else {
      this.loadTemplateIfExists()
      // 加载草稿
      this.loadDraft()
    }
    
    // 启动自动保存（30秒一次）
    this.startAutoSave()
    
    // 监听数据变化
    this.$watch(
      () => JSON.stringify({
        taskConfig: this.taskConfig,
        sourceConfig: this.sourceConfig,
        targetConfig: this.targetConfig,
        mappings: this.mappings,
        scheduleConfig: this.scheduleConfig
      }),
      (newVal) => {
        if (this.lastSavedData && newVal !== this.lastSavedData) {
          this.hasUnsavedChanges = true
        }
      },
      { deep: true }
    )
  },
  
  beforeDestroy() {
    // 清理自动保存定时器
    if (this.autoSaveTimer) {
      clearInterval(this.autoSaveTimer)
    }
  },
  
  beforeRouteLeave(to, from, next) {
    // 检查是否有未保存的变更
    if (this.hasUnsavedChanges) {
      this.$confirm('您有未保存的变更，是否离开？', '提示', {
        confirmButtonText: '离开',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        next()
      }).catch(() => {
        next(false)
      })
    } else {
      next()
    }
  },
  methods: {
    // 从SQL中提取表名（简单解析，支持单表和连表查询）
    extractTableNameFromSql(sql) {
      if (!sql) return null
      
      try {
        // 移除注释
        let cleanSql = sql.replace(/--.*$/gm, '').replace(/\/\*[\s\S]*?\*\//g, '')
        
        // 提取FROM后的第一个表名（忽略JOIN的表）
        const fromMatch = cleanSql.match(/\bFROM\s+([\w.`"\[\]]+)/i)
        if (fromMatch) {
          let tableName = fromMatch[1]
          // 移除数据库名/schema前缀（如 db.table → table）
          if (tableName.includes('.')) {
            tableName = tableName.split('.').pop()
          }
          // 移除引号、方括号等
          tableName = tableName.replace(/[`"\[\]]/g, '').trim()
          console.log('从SQL中解析到表名:', tableName)
          return tableName
        }
      } catch (err) {
        console.warn('解析SQL表名失败', err)
      }
      
      return null
    },
    
    // 获取连接器Logo
    getConnectorLogo(item) {
      if (!item || !item.connectorType) return null
      
      if (item.connectorType === 'DATABASE' && item.dbType) {
        return this.connectorLogoMap[item.dbType] || null
      } else if (item.connectorType === 'API') {
        return this.connectorLogoMap['API']
      }
      return null
    },
    
    // 抽屉关闭处理
    handleDrawerClose(done) {
      done()
    },
    
    // ==================== 辅助数据源SQL编辑器功能 ====================
    // 高亮辅助数据源SQL
    updateAuxHighlight(index) {
      const item = this.auxiliaryDatasources[index]
      if (!item) return
      
      // 使用相同的高亮函数
      this.$set(item, '_highlightedSql', this.highlightSql(item.config.sql))
      
      // 清除错误提示
      if (item._sqlError) {
        this.$set(item, '_sqlError', '')
      }
    },
    
    // 同步滚动
    syncAuxScroll(event, index) {
      const textarea = event.target
      const container = textarea.parentElement
      const background = container.querySelector('.sql-editor-background')
      if (background) {
        background.scrollTop = textarea.scrollTop
        background.scrollLeft = textarea.scrollLeft
      }
    },
    
    // 格式化辅助数据源SQL（复用主数据源格式化逻辑）
    formatAuxSql(index) {
      const item = this.auxiliaryDatasources[index]
      if (!item || !item.config.sql) {
        this.$message.warning('请先输入SQL语句')
        return
      }
      
      try {
        let sql = item.config.sql.trim()
        
        // 保护字符串和注释
        const protectedStrings = []
        let protectedIndex = 0
        
        sql = sql.replace(/'([^']*)'/g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        sql = sql.replace(/"([^"]*)"/g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        sql = sql.replace(/--(.*)$/gm, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        sql = sql.replace(/\/\*([\s\S]*?)\*\//g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        // 格式化规则（与主数据源保持一致）
        const formatRules = [
          // 4词关键字
          { pattern: /\bNATURAL\s+LEFT\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL LEFT OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+RIGHT\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL RIGHT OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+FULL\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL FULL OUTER JOIN\n  ' },
          // 3词关键字
          { pattern: /\bLEFT\s+OUTER\s+JOIN\b/gi, replacement: '\nLEFT OUTER JOIN\n  ' },
          { pattern: /\bRIGHT\s+OUTER\s+JOIN\b/gi, replacement: '\nRIGHT OUTER JOIN\n  ' },
          { pattern: /\bFULL\s+OUTER\s+JOIN\b/gi, replacement: '\nFULL OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+LEFT\s+JOIN\b/gi, replacement: '\nNATURAL LEFT JOIN\n  ' },
          { pattern: /\bNATURAL\s+RIGHT\s+JOIN\b/gi, replacement: '\nNATURAL RIGHT JOIN\n  ' },
          { pattern: /\bNATURAL\s+FULL\s+JOIN\b/gi, replacement: '\nNATURAL FULL JOIN\n  ' },
          { pattern: /\bIS\s+NOT\s+NULL\b/gi, replacement: 'IS NOT NULL' },
          { pattern: /\bSTART\s+TRANSACTION\b/gi, replacement: 'START TRANSACTION' },
          { pattern: /\bBEGIN\s+TRANSACTION\b/gi, replacement: 'BEGIN TRANSACTION' },
          { pattern: /\bCOMMIT\s+TRANSACTION\b/gi, replacement: 'COMMIT TRANSACTION' },
          { pattern: /\bROLLBACK\s+TRANSACTION\b/gi, replacement: 'ROLLBACK TRANSACTION' },
          { pattern: /\bUNBOUNDED\s+PRECEDING\b/gi, replacement: 'UNBOUNDED PRECEDING' },
          { pattern: /\bUNBOUNDED\s+FOLLOWING\b/gi, replacement: 'UNBOUNDED FOLLOWING' },
          // 2词 - JOIN
          { pattern: /\bLEFT\s+JOIN\b/gi, replacement: '\nLEFT JOIN\n  ' },
          { pattern: /\bRIGHT\s+JOIN\b/gi, replacement: '\nRIGHT JOIN\n  ' },
          { pattern: /\bINNER\s+JOIN\b/gi, replacement: '\nINNER JOIN\n  ' },
          { pattern: /\bOUTER\s+JOIN\b/gi, replacement: '\nOUTER JOIN\n  ' },
          { pattern: /\bCROSS\s+JOIN\b/gi, replacement: '\nCROSS JOIN\n  ' },
          { pattern: /\bNATURAL\s+JOIN\b/gi, replacement: '\nNATURAL JOIN\n  ' },
          // 2词 - 聚合
          { pattern: /\bGROUP\s+BY\b/gi, replacement: '\nGROUP BY\n  ' },
          { pattern: /\bORDER\s+BY\b/gi, replacement: '\nORDER BY\n  ' },
          { pattern: /\bPARTITION\s+BY\b/gi, replacement: '\nPARTITION BY\n  ' },
          { pattern: /\bCLUSTER\s+BY\b/gi, replacement: '\nCLUSTER BY\n  ' },
          { pattern: /\bDISTRIBUTE\s+BY\b/gi, replacement: '\nDISTRIBUTE BY\n  ' },
          { pattern: /\bSORT\s+BY\b/gi, replacement: '\nSORT BY\n  ' },
          // 2词 - 集合
          { pattern: /\bUNION\s+ALL\b/gi, replacement: '\nUNION ALL\n' },
          { pattern: /\bUNION\s+DISTINCT\b/gi, replacement: '\nUNION DISTINCT\n' },
          { pattern: /\bEXCEPT\s+ALL\b/gi, replacement: '\nEXCEPT ALL\n' },
          { pattern: /\bEXCEPT\s+DISTINCT\b/gi, replacement: '\nEXCEPT DISTINCT\n' },
          { pattern: /\bINTERSECT\s+ALL\b/gi, replacement: '\nINTERSECT ALL\n' },
          { pattern: /\bINTERSECT\s+DISTINCT\b/gi, replacement: '\nINTERSECT DISTINCT\n' },
          // 2词 - DML
          { pattern: /\bINSERT\s+INTO\b/gi, replacement: 'INSERT INTO\n  ' },
          { pattern: /\bDELETE\s+FROM\b/gi, replacement: 'DELETE FROM ' },
          { pattern: /\bSELECT\s+DISTINCT\b/gi, replacement: 'SELECT DISTINCT\n  ' },
          // 2词 - DDL
          { pattern: /\bCREATE\s+TABLE\b/gi, replacement: 'CREATE TABLE ' },
          { pattern: /\bALTER\s+TABLE\b/gi, replacement: 'ALTER TABLE ' },
          { pattern: /\bDROP\s+TABLE\b/gi, replacement: 'DROP TABLE ' },
          { pattern: /\bTRUNCATE\s+TABLE\b/gi, replacement: 'TRUNCATE TABLE ' },
          { pattern: /\bCREATE\s+VIEW\b/gi, replacement: 'CREATE VIEW ' },
          { pattern: /\bDROP\s+VIEW\b/gi, replacement: 'DROP VIEW ' },
          { pattern: /\bCREATE\s+INDEX\b/gi, replacement: 'CREATE INDEX ' },
          { pattern: /\bDROP\s+INDEX\b/gi, replacement: 'DROP INDEX ' },
          { pattern: /\bCREATE\s+DATABASE\b/gi, replacement: 'CREATE DATABASE ' },
          { pattern: /\bDROP\s+DATABASE\b/gi, replacement: 'DROP DATABASE ' },
          // 2词 - 约束
          { pattern: /\bPRIMARY\s+KEY\b/gi, replacement: 'PRIMARY KEY' },
          { pattern: /\bFOREIGN\s+KEY\b/gi, replacement: 'FOREIGN KEY' },
          { pattern: /\bUNIQUE\s+KEY\b/gi, replacement: 'UNIQUE KEY' },
          { pattern: /\bNOT\s+NULL\b/gi, replacement: 'NOT NULL' },
          { pattern: /\bAUTO\s+INCREMENT\b/gi, replacement: 'AUTO_INCREMENT' },
          { pattern: /\bON\s+DELETE\b/gi, replacement: 'ON DELETE' },
          { pattern: /\bON\s+UPDATE\b/gi, replacement: 'ON UPDATE' },
          // 2词 - 窗口
          { pattern: /\bROWS\s+BETWEEN\b/gi, replacement: 'ROWS BETWEEN' },
          { pattern: /\bRANGE\s+BETWEEN\b/gi, replacement: 'RANGE BETWEEN' },
          { pattern: /\bCURRENT\s+ROW\b/gi, replacement: 'CURRENT ROW' },
          // 2词 - IS
          { pattern: /\bIS\s+NULL\b/gi, replacement: 'IS NULL' },
          { pattern: /\bIS\s+NOT\b/gi, replacement: 'IS NOT' },
          { pattern: /\bIS\s+TRUE\b/gi, replacement: 'IS TRUE' },
          { pattern: /\bIS\s+FALSE\b/gi, replacement: 'IS FALSE' },
          // 2词 - NOT
          { pattern: /\bNOT\s+IN\b/gi, replacement: 'NOT IN' },
          { pattern: /\bNOT\s+EXISTS\b/gi, replacement: 'NOT EXISTS' },
          { pattern: /\bNOT\s+LIKE\b/gi, replacement: 'NOT LIKE' },
          { pattern: /\bNOT\s+BETWEEN\b/gi, replacement: 'NOT BETWEEN' },
          // 2词 - WITH
          { pattern: /\bWITH\s+RECURSIVE\b/gi, replacement: 'WITH RECURSIVE ' },
          // 1词
          { pattern: /\bSELECT\b/gi, replacement: 'SELECT\n  ' },
          { pattern: /\bFROM\b/gi, replacement: '\nFROM\n  ' },
          { pattern: /\bWHERE\b/gi, replacement: '\nWHERE\n  ' },
          { pattern: /\bJOIN\b/gi, replacement: '\nJOIN\n  ' },
          { pattern: /\bON\b/gi, replacement: '\n  ON ' },
          { pattern: /\bUSING\b/gi, replacement: '\n  USING ' },
          { pattern: /\bHAVING\b/gi, replacement: '\nHAVING\n  ' },
          { pattern: /\bLIMIT\b/gi, replacement: '\nLIMIT ' },
          { pattern: /\bOFFSET\b/gi, replacement: '\nOFFSET ' },
          { pattern: /\bUNION\b/gi, replacement: '\nUNION\n' },
          { pattern: /\bINTERSECT\b/gi, replacement: '\nINTERSECT\n' },
          { pattern: /\bEXCEPT\b/gi, replacement: '\nEXCEPT\n' },
          { pattern: /\bAND\b/gi, replacement: '\n  AND ' },
          { pattern: /\bOR\b/gi, replacement: '\n  OR ' }
        ]
        
        // 统一关键字大写（与主数据源保持一致）
        const allKeywords = [
          'NATURAL LEFT OUTER JOIN', 'NATURAL RIGHT OUTER JOIN', 'NATURAL FULL OUTER JOIN',
          'LEFT OUTER JOIN', 'RIGHT OUTER JOIN', 'FULL OUTER JOIN',
          'NATURAL LEFT JOIN', 'NATURAL RIGHT JOIN', 'NATURAL FULL JOIN',
          'IS NOT NULL', 'START TRANSACTION', 'BEGIN TRANSACTION', 'COMMIT TRANSACTION', 'ROLLBACK TRANSACTION',
          'UNBOUNDED PRECEDING', 'UNBOUNDED FOLLOWING',
          'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'OUTER JOIN', 'CROSS JOIN', 'NATURAL JOIN',
          'GROUP BY', 'ORDER BY', 'PARTITION BY', 'CLUSTER BY', 'DISTRIBUTE BY', 'SORT BY',
          'UNION ALL', 'UNION DISTINCT', 'EXCEPT ALL', 'EXCEPT DISTINCT', 'INTERSECT ALL', 'INTERSECT DISTINCT',
          'INSERT INTO', 'DELETE FROM', 'SELECT DISTINCT', 'SELECT INTO',
          'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE', 'TRUNCATE TABLE',
          'CREATE VIEW', 'DROP VIEW', 'CREATE INDEX', 'DROP INDEX',
          'CREATE DATABASE', 'DROP DATABASE', 'CREATE SCHEMA', 'DROP SCHEMA',
          'PRIMARY KEY', 'FOREIGN KEY', 'UNIQUE KEY', 'CHECK CONSTRAINT',
          'NOT NULL', 'AUTO INCREMENT', 'ON DELETE', 'ON UPDATE', 'CASCADE DELETE', 'SET NULL',
          'ROWS BETWEEN', 'RANGE BETWEEN', 'CURRENT ROW',
          'IS NULL', 'IS NOT', 'IS TRUE', 'IS FALSE', 'IS UNKNOWN',
          'NOT IN', 'NOT EXISTS', 'NOT LIKE', 'NOT BETWEEN',
          'WITH RECURSIVE', 'WITH ORDINALITY',
          'FOR UPDATE', 'LOCK IN', 'SHARE MODE', 'NO WAIT', 'DEFAULT VALUE',
          'SELECT', 'DISTINCT', 'FROM', 'WHERE', 'JOIN', 'ON', 'USING',
          'HAVING', 'LIMIT', 'OFFSET', 'UNION', 'INTERSECT', 'EXCEPT',
          'INSERT', 'UPDATE', 'DELETE', 'MERGE', 'REPLACE',
          'CREATE', 'ALTER', 'DROP', 'TRUNCATE',
          'AND', 'OR', 'NOT', 'IN', 'EXISTS', 'BETWEEN', 'LIKE',
          'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
          'AS', 'ASC', 'DESC', 'NULL', 'IS', 'TRUE', 'FALSE'
        ]
        
        // 先压缩多余的空白字符（避免格式化不一致）
        sql = sql.replace(/\s+/g, ' ').trim()
        
        allKeywords.sort((a, b) => b.length - a.length).forEach(keyword => {
          // 注意：这里只匹配单个空格，避免匹配换行符
          const regex = new RegExp(`\\b${keyword.replace(/ /g, ' ')}\\b`, 'gi')
          sql = sql.replace(regex, keyword)
        })
        
        // 应用格式化
        formatRules.forEach(rule => {
          sql = sql.replace(rule.pattern, rule.replacement)
        })
        
        sql = sql.replace(/,(?!\s*\n)/g, ',\n  ')
        
        sql = sql
          .replace(/\n\s*\n+/g, '\n')
          .replace(/\n\s+\n/g, '\n')
          .replace(/^\s+/gm, (match) => {
            const indent = Math.floor(match.length / 2)
            return '  '.repeat(indent)
          })
          .trim()
        
        // 还原保护内容
        protectedStrings.reverse().forEach(item => {
          sql = sql.replace(item.placeholder, item.content)
        })
        
        item.config.sql = sql
        this.updateAuxHighlight(index)
        this.$message.success('SQL格式化成功')
      } catch (e) {
        console.error('SQL格式化错误:', e)
        this.$message.error('格式化失败：' + e.message)
      }
    },
    
       // 验证辅助数据源SQL
    validateAuxSql(index) {
      const item = this.auxiliaryDatasources[index]
      if (!item || !item.config || !item.config.sql) {
        this.$message.warning('请先输入SQL语句')
        return
      }

      const connectorId = item.connectorId
      const sql = item.config.sql.trim()

      if (!connectorId) {
        this.$message.error('请先选择辅助数据源')
        return
      }
      if (!sql) {
        this.$message.error('SQL不能为空')
        return
      }

      this.$set(item, '_sqlError', '')

      this.$axios.post('/v1/task/validate-sql', {
        connectorId,
        sql
      }).then(res => {
        // 拦截器已返回 response.data，res 就是后端的 Result 对象
        // Result.code = 200 表示成功，res.data 是实际数据
        if (res.data) {
          const data = res.data
          if (data.valid) {
            this.$set(item, '_sqlError', '')
            this.$message.success('SQL语法验证通过')
          } else {
            const msg = data.errorMessage || 'SQL语法验证失败'
            this.$set(item, '_sqlError', msg)
            this.$message.error(msg)
          }
        } else {
          const msg = res.message || 'SQL语法验证失败'
          this.$set(item, '_sqlError', msg)
          this.$message.error(msg)
        }
      }).catch(err => {
        const msg = err.message || 'SQL语法验证失败'
        this.$set(item, '_sqlError', msg)
        this.$message.error(msg)
      })
    },

        // 主数据源 SQL 语法校验
    validateSourceSql() {
      const connectorId = this.sourceConfig.connectorId
      const sql = (this.sourceConfig.sql || '').trim()

      if (!connectorId) {
        this.$message.error('请先选择源数据源')
        return
      }
      if (!sql) {
        this.$message.error('SQL不能为空')
        return
      }

      this.sqlValidationError = ''
      this.sqlValidationLoading = true

      this.$axios.post('/v1/task/validate-sql', {
        connectorId,
        sql
      }).then(res => {
        this.sqlValidationLoading = false
        // 拦截器已返回 response.data，res 就是后端的 Result 对象
        // Result.code = 200 表示成功，res.data 是实际数据
        if (res.data) {
          const data = res.data
          if (data.valid) {
            this.sqlValidationError = ''
            this.$message.success('SQL语法验证通过')
          } else {
            const msg = data.errorMessage || 'SQL语法验证失败'
            this.sqlValidationError = msg
            this.$message.error(msg)
          }
        } else {
          const msg = res.message || 'SQL语法验证失败'
          this.sqlValidationError = msg
          this.$message.error(msg)
        }
      }).catch(err => {
        this.sqlValidationLoading = false
        const msg = err.message || 'SQL语法验证失败'
        this.sqlValidationError = msg
        this.$message.error(msg)
      })
    },
    
    // 清空辅助数据源SQL
    clearAuxSql(index) {
      this.$confirm('确定要清空SQL内容吗？', '提示', {
        type: 'warning'
      }).then(() => {
        const item = this.auxiliaryDatasources[index]
        if (item) {
          item.config.sql = ''
          this.$set(item, '_highlightedSql', '')
          this.$set(item, '_sqlError', '')
          this.$message.success('已清空')
        }
      }).catch(() => {})
    },
    
    // 步骤点击处理
    handleStepClick(index) {
      // 可以根据需要添加验证逻辑，暂时允许自由跳转
      if (index >= 0 && index < this.stepsData.length) {
        this.activeStep = index
      }
    },
    
    // 根据任务名称自动生成任务编码
    handleTaskNameChange(value) {
      // 调试日志：确认任务名称变化
      console.log('[任务名称变化]', value)
      console.log('[当前taskConfig]', JSON.parse(JSON.stringify(this.taskConfig)))
      
      // 如果任务编码为空或者是之前自动生成的，才自动生成新的
      if (!this.taskConfig.taskCode || this.taskConfig.taskCode === this.lastAutoGeneratedCode) {
        // 中文转拼音 + 下划线格式
        const code = this.generateTaskCode(value)
        this.taskConfig.taskCode = code
        this.lastAutoGeneratedCode = code
        console.log('[自动生成任务编码]', code)
      }
    },
    
    // SQL语法高亮
    highlightSql(sql) {
      if (!sql) return ''
      
      // 如果已经包含HTML标签，说明已经高亮过，直接返回
      if (sql.includes('<span') || sql.includes('</span>')) {
        return sql
      }
      
      // 转义HTML字符
      let highlighted = sql
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      
      // 使用占位符保护已处理的内容
      const placeholders = []
      let placeholderIndex = 0
      
      // 1. 先处理注释（保存为占位符）
      highlighted = highlighted.replace(/--(.*)$/gm, (match) => {
        const placeholder = `__PLACEHOLDER_${placeholderIndex}__`
        placeholders[placeholderIndex] = `<span class="sql-comment">${match}</span>`
        placeholderIndex++
        return placeholder
      })
      
      highlighted = highlighted.replace(/\/\*([\s\S]*?)\*\//g, (match) => {
        const placeholder = `__PLACEHOLDER_${placeholderIndex}__`
        placeholders[placeholderIndex] = `<span class="sql-comment">${match}</span>`
        placeholderIndex++
        return placeholder
      })
      
      // 2. 处理字符串（保存为占位符）
      highlighted = highlighted.replace(/'([^']*)'/g, (match) => {
        const placeholder = `__PLACEHOLDER_${placeholderIndex}__`
        placeholders[placeholderIndex] = `<span class="sql-string">${match}</span>`
        placeholderIndex++
        return placeholder
      })
      
      highlighted = highlighted.replace(/"([^"]*)"/g, (match) => {
        const placeholder = `__PLACEHOLDER_${placeholderIndex}__`
        placeholders[placeholderIndex] = `<span class="sql-string">${match}</span>`
        placeholderIndex++
        return placeholder
      })
      
      // 3. 处理变量（保存为占位符）
      highlighted = highlighted.replace(/\{([^}]+)\}/g, (match) => {
        const placeholder = `__PLACEHOLDER_${placeholderIndex}__`
        placeholders[placeholderIndex] = `<span class="sql-variable">${match}</span>`
        placeholderIndex++
        return placeholder
      })
      
      // 4. 高亮SQL关键字（按长度排序，先匹配长关键字，避免重复替换）
      const keywords = [
        // 多词关键字（必须优先处理，按长度降序）
        // JOIN 类型（4词）
        'NATURAL LEFT OUTER JOIN', 'NATURAL RIGHT OUTER JOIN', 'NATURAL FULL OUTER JOIN',
        // JOIN 类型（3词）
        'LEFT OUTER JOIN', 'RIGHT OUTER JOIN', 'FULL OUTER JOIN', 'NATURAL LEFT JOIN', 'NATURAL RIGHT JOIN', 'NATURAL FULL JOIN',
        // JOIN 类型（2词）
        'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'OUTER JOIN', 'CROSS JOIN', 'NATURAL JOIN',
        // 聚合和分组（2词）
        'GROUP BY', 'ORDER BY', 'PARTITION BY', 'CLUSTER BY', 'DISTRIBUTE BY', 'SORT BY',
        // 集合操作（2词）
        'UNION ALL', 'UNION DISTINCT', 'EXCEPT ALL', 'EXCEPT DISTINCT', 'INTERSECT ALL', 'INTERSECT DISTINCT',
        // 逻辑运算（2词）
        'NOT IN', 'NOT EXISTS', 'NOT LIKE', 'NOT BETWEEN', 'NOT NULL',
        // IS 判断（2-3词）
        'IS NOT NULL', 'IS NOT', 'IS NULL', 'IS TRUE', 'IS FALSE', 'IS UNKNOWN',
        // DDL 约束（2词）
        'PRIMARY KEY', 'FOREIGN KEY', 'UNIQUE KEY', 'CHECK CONSTRAINT', 'DEFAULT VALUE',
        // DDL 操作（2词）
        'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE', 'TRUNCATE TABLE',
        'CREATE VIEW', 'DROP VIEW', 'CREATE INDEX', 'DROP INDEX',
        'CREATE DATABASE', 'DROP DATABASE', 'CREATE SCHEMA', 'DROP SCHEMA',
        // DML 操作（2词）
        'INSERT INTO', 'DELETE FROM', 'SELECT INTO', 'SELECT DISTINCT',
        // 窗口函数（2词）
        'ROWS BETWEEN', 'RANGE BETWEEN', 'ROWS UNBOUNDED', 'RANGE UNBOUNDED',
        'UNBOUNDED PRECEDING', 'UNBOUNDED FOLLOWING', 'CURRENT ROW',
        // 其他多词关键字（2词）
        'FOR UPDATE', 'LOCK IN', 'SHARE MODE', 'NO WAIT',
        'START TRANSACTION', 'BEGIN TRANSACTION', 'COMMIT TRANSACTION', 'ROLLBACK TRANSACTION',
        'AUTO INCREMENT', 'ON DELETE', 'ON UPDATE', 'CASCADE DELETE', 'SET NULL',
        'WITH RECURSIVE', 'WITH ORDINALITY',
        
        // DML/DDL 关键字（1词）
        'SELECT', 'FROM', 'WHERE', 'JOIN', 'ON', 'USING',
        'INSERT', 'UPDATE', 'DELETE', 'MERGE', 'REPLACE', 'UPSERT',
        'CREATE', 'ALTER', 'DROP', 'TRUNCATE', 'RENAME',
        'TABLE', 'VIEW', 'INDEX', 'DATABASE', 'SCHEMA', 'SEQUENCE',
        'CONSTRAINT', 'UNIQUE', 'CHECK', 'DEFAULT',
        // 聚合和分析（1词）
        'HAVING', 'DISTINCT', 'ALL', 'TOP', 'LIMIT', 'OFFSET', 'FETCH', 'FIRST', 'NEXT', 'ROWS', 'ONLY',
        'OVER', 'WINDOW',
        // 窗口和分析函数
        'ROW_NUMBER', 'RANK', 'DENSE_RANK', 'NTILE', 'LAG', 'LEAD', 'FIRST_VALUE', 'LAST_VALUE',
        'COUNT', 'SUM', 'AVG', 'MAX', 'MIN', 'STDDEV', 'VARIANCE',
        // 逻辑运算（1词）
        'AND', 'OR', 'NOT', 'XOR',
        'IN', 'EXISTS', 'BETWEEN', 'LIKE', 'ILIKE', 'RLIKE', 'REGEXP', 'SIMILAR',
        // 流程控制
        'CASE', 'WHEN', 'THEN', 'ELSE', 'END', 'ELSEIF',
        'IF', 'IFNULL', 'NULLIF', 'COALESCE',
        // 集合操作（1词）
        'UNION', 'INTERSECT', 'EXCEPT', 'MINUS',
        // JOIN 修饰符（单独出现时也需要高亮）
        'LEFT', 'RIGHT', 'INNER', 'OUTER', 'CROSS', 'NATURAL', 'FULL',
        // 其他关键字
        'AS', 'ASC', 'DESC', 'NULLS', 'LAST',
        'NULL', 'TRUE', 'FALSE', 'UNKNOWN',
        'SET', 'INTO', 'VALUES',
        'WITH', 'RECURSIVE', 'CTE',
        'BEGIN', 'COMMIT', 'ROLLBACK', 'SAVEPOINT',
        'GRANT', 'REVOKE', 'DENY',
        'CAST', 'CONVERT', 'EXTRACT', 'SUBSTRING', 'TRIM', 'UPPER', 'LOWER'
      ]
      
      // 使用单次正则替换，避免重复处理
      // 构建关键字的正则表达式（按长度降序）
      const keywordPattern = keywords
        .sort((a, b) => b.length - a.length)  // 按长度降序
        .map(k => k.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))  // 转义特殊字符
        .join('|')
      
      const keywordRegex = new RegExp(`\\b(${keywordPattern})\\b`, 'gi')
      highlighted = highlighted.replace(keywordRegex, (match) => {
        // 保护已经被替换的内容（通过检查是否在占位符中）
        if (match.startsWith('__PLACEHOLDER_')) {
          return match
        }
        return `<span class="sql-keyword">${match.toUpperCase()}</span>`
      })
      
      // 5. 高亮数字（整数、小数、科学计数法）
      highlighted = highlighted.replace(/\b(\d+\.\d+|\d+[eE][+-]?\d+|\d+)\b/g, '<span class="sql-number">$1</span>')
      
      // 6. 高亮函数调用（函数名后跟括号）
      highlighted = highlighted.replace(/\b([A-Z_][A-Z0-9_]*)\s*(?=\()/gi, '<span class="sql-function">$1</span>')
      
      // 7. 还原占位符
      for (let i = placeholderIndex - 1; i >= 0; i--) {
        highlighted = highlighted.replace(`__PLACEHOLDER_${i}__`, placeholders[i])
      }
      
      // 8. 添加换行符
      highlighted = highlighted.replace(/\n/g, '<br>')
      
      return highlighted
    },
    
    // 更新SQL高亮
    updateHighlight() {
      this.highlightedSql = this.highlightSql(this.sourceConfig.sql)
      // 输入时清除错误提示
      if (this.sqlValidationError) {
        this.sqlValidationError = ''
      }
    },
    
    // 格式化SQL
    formatSql() {
      if (!this.sourceConfig.sql) {
        this.$message.warning('请先输入SQL语句')
        return
      }
      
      try {
        let sql = this.sourceConfig.sql.trim()
        
        // 保护字符串和注释（避免格式化内部内容）
        const protectedStrings = []
        let protectedIndex = 0
        
        // 保护单引号字符串
        sql = sql.replace(/'([^']*)'/g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        // 保护双引号字符串
        sql = sql.replace(/"([^"]*)"/g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        // 保护单行注释
        sql = sql.replace(/--(.*)$/gm, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        // 保护多行注释
        sql = sql.replace(/\/\*([\s\S]*?)\*\//g, (match) => {
          const placeholder = `__STRING_${protectedIndex}__`
          protectedStrings.push({ placeholder, content: match })
          protectedIndex++
          return placeholder
        })
        
        // 定义格式化规则（按长度降序，避免短关键字破坏长关键字）
        const formatRules = [
          // 4词关键字
          { pattern: /\bNATURAL\s+LEFT\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL LEFT OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+RIGHT\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL RIGHT OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+FULL\s+OUTER\s+JOIN\b/gi, replacement: '\nNATURAL FULL OUTER JOIN\n  ' },
          
          // 3词关键字 - JOIN
          { pattern: /\bLEFT\s+OUTER\s+JOIN\b/gi, replacement: '\nLEFT OUTER JOIN\n  ' },
          { pattern: /\bRIGHT\s+OUTER\s+JOIN\b/gi, replacement: '\nRIGHT OUTER JOIN\n  ' },
          { pattern: /\bFULL\s+OUTER\s+JOIN\b/gi, replacement: '\nFULL OUTER JOIN\n  ' },
          { pattern: /\bNATURAL\s+LEFT\s+JOIN\b/gi, replacement: '\nNATURAL LEFT JOIN\n  ' },
          { pattern: /\bNATURAL\s+RIGHT\s+JOIN\b/gi, replacement: '\nNATURAL RIGHT JOIN\n  ' },
          { pattern: /\bNATURAL\s+FULL\s+JOIN\b/gi, replacement: '\nNATURAL FULL JOIN\n  ' },
          
          // 3词关键字 - 其他
          { pattern: /\bIS\s+NOT\s+NULL\b/gi, replacement: 'IS NOT NULL' },
          { pattern: /\bSTART\s+TRANSACTION\b/gi, replacement: 'START TRANSACTION' },
          { pattern: /\bBEGIN\s+TRANSACTION\b/gi, replacement: 'BEGIN TRANSACTION' },
          { pattern: /\bCOMMIT\s+TRANSACTION\b/gi, replacement: 'COMMIT TRANSACTION' },
          { pattern: /\bROLLBACK\s+TRANSACTION\b/gi, replacement: 'ROLLBACK TRANSACTION' },
          { pattern: /\bUNBOUNDED\s+PRECEDING\b/gi, replacement: 'UNBOUNDED PRECEDING' },
          { pattern: /\bUNBOUNDED\s+FOLLOWING\b/gi, replacement: 'UNBOUNDED FOLLOWING' },
          
          // 2词关键字 - JOIN
          { pattern: /\bLEFT\s+JOIN\b/gi, replacement: '\nLEFT JOIN\n  ' },
          { pattern: /\bRIGHT\s+JOIN\b/gi, replacement: '\nRIGHT JOIN\n  ' },
          { pattern: /\bINNER\s+JOIN\b/gi, replacement: '\nINNER JOIN\n  ' },
          { pattern: /\bOUTER\s+JOIN\b/gi, replacement: '\nOUTER JOIN\n  ' },
          { pattern: /\bCROSS\s+JOIN\b/gi, replacement: '\nCROSS JOIN\n  ' },
          { pattern: /\bNATURAL\s+JOIN\b/gi, replacement: '\nNATURAL JOIN\n  ' },
          
          // 2词关键字 - 聚合分组
          { pattern: /\bGROUP\s+BY\b/gi, replacement: '\nGROUP BY\n  ' },
          { pattern: /\bORDER\s+BY\b/gi, replacement: '\nORDER BY\n  ' },
          { pattern: /\bPARTITION\s+BY\b/gi, replacement: '\nPARTITION BY\n  ' },
          { pattern: /\bCLUSTER\s+BY\b/gi, replacement: '\nCLUSTER BY\n  ' },
          { pattern: /\bDISTRIBUTE\s+BY\b/gi, replacement: '\nDISTRIBUTE BY\n  ' },
          { pattern: /\bSORT\s+BY\b/gi, replacement: '\nSORT BY\n  ' },
          
          // 2词关键字 - 集合操作
          { pattern: /\bUNION\s+ALL\b/gi, replacement: '\nUNION ALL\n' },
          { pattern: /\bUNION\s+DISTINCT\b/gi, replacement: '\nUNION DISTINCT\n' },
          { pattern: /\bEXCEPT\s+ALL\b/gi, replacement: '\nEXCEPT ALL\n' },
          { pattern: /\bEXCEPT\s+DISTINCT\b/gi, replacement: '\nEXCEPT DISTINCT\n' },
          { pattern: /\bINTERSECT\s+ALL\b/gi, replacement: '\nINTERSECT ALL\n' },
          { pattern: /\bINTERSECT\s+DISTINCT\b/gi, replacement: '\nINTERSECT DISTINCT\n' },
          
          // 2词关键字 - DML
          { pattern: /\bINSERT\s+INTO\b/gi, replacement: 'INSERT INTO\n  ' },
          { pattern: /\bDELETE\s+FROM\b/gi, replacement: 'DELETE FROM ' },
          { pattern: /\bSELECT\s+DISTINCT\b/gi, replacement: 'SELECT DISTINCT\n  ' },
          
          // 2词关键字 - DDL
          { pattern: /\bCREATE\s+TABLE\b/gi, replacement: 'CREATE TABLE ' },
          { pattern: /\bALTER\s+TABLE\b/gi, replacement: 'ALTER TABLE ' },
          { pattern: /\bDROP\s+TABLE\b/gi, replacement: 'DROP TABLE ' },
          { pattern: /\bTRUNCATE\s+TABLE\b/gi, replacement: 'TRUNCATE TABLE ' },
          { pattern: /\bCREATE\s+VIEW\b/gi, replacement: 'CREATE VIEW ' },
          { pattern: /\bDROP\s+VIEW\b/gi, replacement: 'DROP VIEW ' },
          { pattern: /\bCREATE\s+INDEX\b/gi, replacement: 'CREATE INDEX ' },
          { pattern: /\bDROP\s+INDEX\b/gi, replacement: 'DROP INDEX ' },
          { pattern: /\bCREATE\s+DATABASE\b/gi, replacement: 'CREATE DATABASE ' },
          { pattern: /\bDROP\s+DATABASE\b/gi, replacement: 'DROP DATABASE ' },
          
          // 2词关键字 - 约束
          { pattern: /\bPRIMARY\s+KEY\b/gi, replacement: 'PRIMARY KEY' },
          { pattern: /\bFOREIGN\s+KEY\b/gi, replacement: 'FOREIGN KEY' },
          { pattern: /\bUNIQUE\s+KEY\b/gi, replacement: 'UNIQUE KEY' },
          { pattern: /\bNOT\s+NULL\b/gi, replacement: 'NOT NULL' },
          { pattern: /\bAUTO\s+INCREMENT\b/gi, replacement: 'AUTO_INCREMENT' },
          { pattern: /\bON\s+DELETE\b/gi, replacement: 'ON DELETE' },
          { pattern: /\bON\s+UPDATE\b/gi, replacement: 'ON UPDATE' },
          
          // 2词关键字 - 窗口函数
          { pattern: /\bROWS\s+BETWEEN\b/gi, replacement: 'ROWS BETWEEN' },
          { pattern: /\bRANGE\s+BETWEEN\b/gi, replacement: 'RANGE BETWEEN' },
          { pattern: /\bCURRENT\s+ROW\b/gi, replacement: 'CURRENT ROW' },
          
          // 2词关键字 - IS 判断
          { pattern: /\bIS\s+NULL\b/gi, replacement: 'IS NULL' },
          { pattern: /\bIS\s+NOT\b/gi, replacement: 'IS NOT' },
          { pattern: /\bIS\s+TRUE\b/gi, replacement: 'IS TRUE' },
          { pattern: /\bIS\s+FALSE\b/gi, replacement: 'IS FALSE' },
          
          // 2词关键字 - NOT 运算
          { pattern: /\bNOT\s+IN\b/gi, replacement: 'NOT IN' },
          { pattern: /\bNOT\s+EXISTS\b/gi, replacement: 'NOT EXISTS' },
          { pattern: /\bNOT\s+LIKE\b/gi, replacement: 'NOT LIKE' },
          { pattern: /\bNOT\s+BETWEEN\b/gi, replacement: 'NOT BETWEEN' },
          
          // 2词关键字 - WITH
          { pattern: /\bWITH\s+RECURSIVE\b/gi, replacement: 'WITH RECURSIVE ' },
          
          // 单词关键字
          { pattern: /\bSELECT\b/gi, replacement: 'SELECT\n  ' },
          { pattern: /\bFROM\b/gi, replacement: '\nFROM\n  ' },
          { pattern: /\bWHERE\b/gi, replacement: '\nWHERE\n  ' },
          { pattern: /\bJOIN\b/gi, replacement: '\nJOIN\n  ' },
          { pattern: /\bON\b/gi, replacement: '\n  ON ' },
          { pattern: /\bUSING\b/gi, replacement: '\n  USING ' },
          { pattern: /\bHAVING\b/gi, replacement: '\nHAVING\n  ' },
          { pattern: /\bLIMIT\b/gi, replacement: '\nLIMIT ' },
          { pattern: /\bOFFSET\b/gi, replacement: '\nOFFSET ' },
          { pattern: /\bUNION\b/gi, replacement: '\nUNION\n' },
          { pattern: /\bINTERSECT\b/gi, replacement: '\nINTERSECT\n' },
          { pattern: /\bEXCEPT\b/gi, replacement: '\nEXCEPT\n' },
          
          // 逻辑运算符（WHERE/HAVING 子句中）
          { pattern: /\bAND\b/gi, replacement: '\n  AND ' },
          { pattern: /\bOR\b/gi, replacement: '\n  OR ' }
        ]
        
        // 统一转大写（先处理关键字大写）
        const allKeywords = [
          // 4词关键字
          'NATURAL LEFT OUTER JOIN', 'NATURAL RIGHT OUTER JOIN', 'NATURAL FULL OUTER JOIN',
          // 3词关键字
          'LEFT OUTER JOIN', 'RIGHT OUTER JOIN', 'FULL OUTER JOIN',
          'NATURAL LEFT JOIN', 'NATURAL RIGHT JOIN', 'NATURAL FULL JOIN',
          'IS NOT NULL', 'START TRANSACTION', 'BEGIN TRANSACTION', 'COMMIT TRANSACTION', 'ROLLBACK TRANSACTION',
          'UNBOUNDED PRECEDING', 'UNBOUNDED FOLLOWING',
          // 2词关键字 - JOIN
          'LEFT JOIN', 'RIGHT JOIN', 'INNER JOIN', 'OUTER JOIN', 'CROSS JOIN', 'NATURAL JOIN',
          // 2词关键字 - 聚合
          'GROUP BY', 'ORDER BY', 'PARTITION BY', 'CLUSTER BY', 'DISTRIBUTE BY', 'SORT BY',
          // 2词关键字 - 集合
          'UNION ALL', 'UNION DISTINCT', 'EXCEPT ALL', 'EXCEPT DISTINCT', 'INTERSECT ALL', 'INTERSECT DISTINCT',
          // 2词关键字 - DML
          'INSERT INTO', 'DELETE FROM', 'SELECT DISTINCT', 'SELECT INTO',
          // 2词关键字 - DDL
          'CREATE TABLE', 'ALTER TABLE', 'DROP TABLE', 'TRUNCATE TABLE',
          'CREATE VIEW', 'DROP VIEW', 'CREATE INDEX', 'DROP INDEX',
          'CREATE DATABASE', 'DROP DATABASE', 'CREATE SCHEMA', 'DROP SCHEMA',
          // 2词关键字 - 约束
          'PRIMARY KEY', 'FOREIGN KEY', 'UNIQUE KEY', 'CHECK CONSTRAINT',
          'NOT NULL', 'AUTO INCREMENT', 'ON DELETE', 'ON UPDATE', 'CASCADE DELETE', 'SET NULL',
          // 2词关键字 - 窗口
          'ROWS BETWEEN', 'RANGE BETWEEN', 'CURRENT ROW',
          // 2词关键字 - IS
          'IS NULL', 'IS NOT', 'IS TRUE', 'IS FALSE', 'IS UNKNOWN',
          // 2词关键字 - NOT
          'NOT IN', 'NOT EXISTS', 'NOT LIKE', 'NOT BETWEEN',
          // 2词关键字 - WITH
          'WITH RECURSIVE', 'WITH ORDINALITY',
          // 2词关键字 - 其他
          'FOR UPDATE', 'LOCK IN', 'SHARE MODE', 'NO WAIT', 'DEFAULT VALUE',
          // 1词关键字
          'SELECT', 'DISTINCT', 'FROM', 'WHERE', 'JOIN', 'ON', 'USING',
          'HAVING', 'LIMIT', 'OFFSET', 'UNION', 'INTERSECT', 'EXCEPT',
          'INSERT', 'UPDATE', 'DELETE', 'MERGE', 'REPLACE',
          'CREATE', 'ALTER', 'DROP', 'TRUNCATE',
          'AND', 'OR', 'NOT', 'IN', 'EXISTS', 'BETWEEN', 'LIKE',
          'CASE', 'WHEN', 'THEN', 'ELSE', 'END',
          'AS', 'ASC', 'DESC', 'NULL', 'IS', 'TRUE', 'FALSE'
        ]
        
        // 先压缩多余的空白字符（避免格式化不一致）
        sql = sql.replace(/\s+/g, ' ').trim()
        
        // 按长度降序排列，避免短关键字破坏长关键字
        allKeywords.sort((a, b) => b.length - a.length).forEach(keyword => {
          // 注意：这里只匹配单个空格，避免匹配换行符
          const regex = new RegExp(`\\b${keyword.replace(/ /g, ' ')}\\b`, 'gi')
          sql = sql.replace(regex, keyword)
        })
        
        // 应用格式化规则
        formatRules.forEach(rule => {
          sql = sql.replace(rule.pattern, rule.replacement)
        })
        
        // 逗号后换行（字段列表）
        sql = sql.replace(/,(?!\s*\n)/g, ',\n  ')
        
        // 移除多余的空行和空格
        sql = sql
          .replace(/\n\s*\n+/g, '\n')  // 多个空行变成一个
          .replace(/\n\s+\n/g, '\n')    // 只有空格的行删除
          .replace(/^\s+/gm, (match) => {  // 保留缩进但统一为2空格
            const indent = Math.floor(match.length / 2)
            return '  '.repeat(indent)
          })
          .trim()
        
        // 还原保护的字符串和注释
        protectedStrings.reverse().forEach(item => {
          sql = sql.replace(item.placeholder, item.content)
        })
        
        this.sourceConfig.sql = sql
        this.updateHighlight()  // 更新高亮显示
        this.$message.success('SQL格式化成功')
      } catch (e) {
        console.error('SQL格式化错误:', e)
        this.$message.error('格式化失败：' + e.message)
      }
    },
    
    // 验证SQL语法
    validateSql() {
      if (!this.sourceConfig.sql) {
        this.$message.warning('请先输入SQL语句')
        return
      }
      
      const sql = this.sourceConfig.sql.trim()
      const sqlUpper = sql.toUpperCase()
      
      // 1. 基本语法检查
      if (!sqlUpper.startsWith('SELECT')) {
        this.sqlValidationError = '仅支持SELECT查询语句'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      if (!sqlUpper.includes('FROM')) {
        this.sqlValidationError = '缺少FROM子句'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 2. 检查WHERE子句完整性
      const whereIndex = sqlUpper.indexOf('WHERE')
      if (whereIndex !== -1) {
        // 获取WHERE后面的内容
        const afterWhere = sql.substring(whereIndex + 5).trim()
        
        // 检查WHERE后是否有内容
        if (!afterWhere || afterWhere.length === 0) {
          this.sqlValidationError = 'WHERE子句后缺少条件'
          this.$message.error(this.sqlValidationError)
          return
        }
        
        // 检查WHERE后面是否只是其他关键字
        const afterWhereUpper = afterWhere.toUpperCase()
        const nextKeywords = ['GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'UNION', 'JOIN']
        const startsWithKeyword = nextKeywords.some(keyword => afterWhereUpper.startsWith(keyword))
        if (startsWithKeyword) {
          this.sqlValidationError = 'WHERE子句后缺少条件表达式'
          this.$message.error(this.sqlValidationError)
          return
        }
      }
      
      // 3. 检查SELECT和FROM之间是否有字段
      const selectIndex = sqlUpper.indexOf('SELECT')
      const fromIndex = sqlUpper.indexOf('FROM')
      const betweenSelectFrom = sql.substring(selectIndex + 6, fromIndex).trim()
      if (!betweenSelectFrom) {
        this.sqlValidationError = 'SELECT后缺少字段列表'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 4. 检查FROM后是否有表名
      const afterFrom = sql.substring(fromIndex + 4).trim()
      if (!afterFrom) {
        this.sqlValidationError = 'FROM后缺少表名'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 提取FROM后的第一个词（表名）
      const tableNameMatch = afterFrom.match(/^([\w`"\[\]]+)/)
      if (!tableNameMatch) {
        this.sqlValidationError = 'FROM后的表名格式不正确'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 5. 检查括号匹配
      const openParens = (sqlUpper.match(/\(/g) || []).length
      const closeParens = (sqlUpper.match(/\)/g) || []).length
      if (openParens !== closeParens) {
        this.sqlValidationError = `括号不匹配（左括号${openParens}个，右括号${closeParens}个）`
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 6. 检查引号匹配
      const singleQuotes = (sql.match(/'/g) || []).length
      if (singleQuotes % 2 !== 0) {
        this.sqlValidationError = '单引号不匹配'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      const doubleQuotes = (sql.match(/"/g) || []).length
      if (doubleQuotes % 2 !== 0) {
        this.sqlValidationError = '双引号不匹配'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 7. 检查常见的语法错误
      // 检查是否有连续的逗号
      if (sql.includes(',,')) {
        this.sqlValidationError = '存在连续的逗号'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 检查是否以逗号结尾（在关键字前）
      const beforeFrom = sql.substring(0, fromIndex).trim()
      if (beforeFrom.endsWith(',')) {
        this.sqlValidationError = 'FROM前不应以逗号结尾'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 8. 检查JOIN语句完整性
      const joinMatch = sqlUpper.match(/\b(LEFT JOIN|RIGHT JOIN|INNER JOIN|JOIN)\b/g)
      if (joinMatch) {
        joinMatch.forEach(joinType => {
          const joinIndex = sqlUpper.indexOf(joinType)
          const afterJoin = sql.substring(joinIndex + joinType.length).trim()
          
          if (!afterJoin) {
            this.sqlValidationError = `${joinType}后缺少表名`
            this.$message.error(this.sqlValidationError)
            return
          }
          
          // 检查ON子句
          const onIndex = afterJoin.toUpperCase().indexOf('ON')
          if (onIndex !== -1) {
            const afterOn = afterJoin.substring(onIndex + 2).trim()
            if (!afterOn || afterOn.length < 3) {
              this.sqlValidationError = `${joinType}的ON子句缺少条件`
              this.$message.error(this.sqlValidationError)
              return
            }
          }
        })
        
        if (this.sqlValidationError) return
      }
      
      this.sqlValidationError = ''
      this.$message.success('SQL语法验证通过')
    },
    
    // 清空SQL
    clearSql() {
      this.$confirm('确定要清空SQL语句吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.sourceConfig.sql = ''
        this.sqlValidationError = ''
        this.$message.success('已清空')
      }).catch(() => {})
    },
    
    // 同步滚动
    syncScroll(event) {
      const textarea = event.target
      const background = textarea.previousElementSibling
      if (background) {
        background.scrollTop = textarea.scrollTop
        background.scrollLeft = textarea.scrollLeft
      }
    },
    
    // 表格行类名
    tableRowClassName({ rowIndex }) {
      return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
    },
    
    // 生成任务编码：拼音首字母 + 随机值，全大写
    generateTaskCode(taskName) {
      if (!taskName) return 'TASK_' + this.generateRandomCode()
      
      let prefix = ''
      let charCount = 0
      
      // 提取前4-6个有效字符
      for (let char of taskName) {
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
        prefix = 'TASK'
      }
      
      // 生成随机后缀：4位大写字母+数字
      const randomSuffix = this.generateRandomCode()
      
      return prefix + '_' + randomSuffix
    },
    
    // 获取中文字符的拼音首字母（基于Unicode编码范围）
    getChineseInitial(char) {
      const code = char.charCodeAt(0)
      
      // 基于Unicode编码范围判断拼音首字母
      // 这是一个简化的算法，根据汉字Unicode编码区间估算首字母
      if (code >= 0x4E00 && code <= 0x9FA5) {
        // 常用汉字区间，使用编码范围映射
        if (code >= 0x4E00 && code <= 0x4FFF) return String.fromCharCode(65 + (code % 26)) // A-Z
        if (code >= 0x5000 && code <= 0x51FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5200 && code <= 0x53FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5400 && code <= 0x55FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5600 && code <= 0x57FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5800 && code <= 0x59FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5A00 && code <= 0x5BFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5C00 && code <= 0x5DFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x5E00 && code <= 0x5FFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6000 && code <= 0x61FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6200 && code <= 0x63FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6400 && code <= 0x65FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6600 && code <= 0x67FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6800 && code <= 0x69FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6A00 && code <= 0x6BFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6C00 && code <= 0x6DFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x6E00 && code <= 0x6FFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7000 && code <= 0x71FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7200 && code <= 0x73FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7400 && code <= 0x75FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7600 && code <= 0x77FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7800 && code <= 0x79FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7A00 && code <= 0x7BFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7C00 && code <= 0x7DFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x7E00 && code <= 0x7FFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8000 && code <= 0x81FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8200 && code <= 0x83FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8400 && code <= 0x85FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8600 && code <= 0x87FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8800 && code <= 0x89FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8A00 && code <= 0x8BFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8C00 && code <= 0x8DFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x8E00 && code <= 0x8FFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9000 && code <= 0x91FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9200 && code <= 0x93FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9400 && code <= 0x95FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9600 && code <= 0x97FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9800 && code <= 0x99FF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9A00 && code <= 0x9BFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9C00 && code <= 0x9DFF) return String.fromCharCode(65 + (code % 26))
        if (code >= 0x9E00 && code <= 0x9FA5) return String.fromCharCode(65 + (code % 26))
      }
      
      // 默认返回基于编码的字母
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
    
    // 格式化单元格值（处理JSON对象/数组）
    formatCellValue(value) {
      // null 或 undefined
      if (value === null || value === undefined) {
        return '-'
      }
      
      // JSON对象或数组
      if (typeof value === 'object') {
        try {
          return JSON.stringify(value)
        } catch (e) {
          return '[Object]'
        }
      }
      
      // 布尔值
      if (typeof value === 'boolean') {
        return value ? 'true' : 'false'
      }
      
      // 普通值
      return String(value)
    },
    
    // 计算列宽（根据列名和内容自动调整）
    calculateColumnWidth(columnName, data) {
      if (!columnName || !data || data.length === 0) {
        return 150 // 默认宽度
      }
      
      // 计算列名宽度（中文字符算两个字符宽度）
      let headerWidth = 0
      for (let char of columnName) {
        headerWidth += /[\u4e00-\u9fa5]/.test(char) ? 16 : 8
      }
      headerWidth += 40 // 加上内边距和排序图标的空间
      
      // 计算内容宽度（取前5条数据的最大宽度）
      let maxContentWidth = 0
      const sampleSize = Math.min(5, data.length)
      for (let i = 0; i < sampleSize; i++) {
        const value = this.formatCellValue(data[i][columnName])
        const strValue = String(value)
        let contentWidth = 0
        for (let char of strValue) {
          contentWidth += /[\u4e00-\u9fa5]/.test(char) ? 16 : 8
        }
        maxContentWidth = Math.max(maxContentWidth, contentWidth)
      }
      maxContentWidth += 32 // 加上内边距
      
      // 取列名和内容的最大值，但不超过400px，不小于120px
      const calculatedWidth = Math.max(headerWidth, maxContentWidth)
      return Math.min(Math.max(calculatedWidth, 120), 400)
    },
    
    // 获取连接器显示标签（用于下拉框label）
    getConnectorLabel(connector) {
      const typeLabel = this.getConnectorTypeLabel(connector)
      return `${connector.connectorName} (${typeLabel})`
    },
    
    // 获取连接器类型标签
    getConnectorTypeLabel(connector) {
      if (connector.connectorType === 'DATABASE') {
        return connector.dbType || 'DATABASE'
      } else if (connector.connectorType === 'API') {
        return 'API'
      } else {
        return connector.connectorType
      }
    },
    
    loadConnectors() {
      this.$axios.get('/v1/connector/list').then(res => {
        this.connectorList = res.data
      })
    },
    
    loadDictList() {
      this.$axios.get('/v1/dict-mapping/list').then(res => {
        this.dictList = res.data
      }).catch(err => {
        console.error('加载字典列表失败', err)
      })
    },
    
    // 查询演示连接器的实际ID映射
    async loadDemoConnectorIds() {
      try {
        const res = await this.$axios.get('/v1/connector/list')
        const connectors = res.data || []
        
        const idMap = {}
        const demoNames = [
          'MySQL演示源库',
          'MySQL演示目标库', 
          'API演示源接口',
          'API演示目标接口'
        ]
        
        connectors.forEach(conn => {
          if (demoNames.includes(conn.connectorName)) {
            idMap[conn.connectorName] = conn.id
          }
        })
        
        console.log('演示连接器ID映射:', idMap)
        return idMap
      } catch (err) {
        console.error('查询演示连接器失败:', err)
        return {}
      }
    },
    
    // 根据模板中的连接器ID（1-4）获取对应的连接器名称
    getTemplateConnectorName(templateId) {
      const nameMap = {
        1: 'MySQL演示源库',
        2: 'MySQL演示目标库',
        3: 'API演示源接口',
        4: 'API演示目标接口'
      }
      return nameMap[templateId] || ''
    },
    
    async loadTemplateIfExists() {
      if (this.$route.query.template) {
        try {
          const templateConfig = JSON.parse(this.$route.query.template)
          
          // 查询演示连接器的实际ID映射
          const demoConnectorIdMap = await this.loadDemoConnectorIds()
          
          // 加载基本信息
          this.taskConfig.taskName = templateConfig.taskName
          this.taskConfig.taskCode = templateConfig.taskCode
          this.taskConfig.syncMode = templateConfig.syncMode
          this.taskConfig.description = templateConfig.description || ''
          
          // 加载增量字段（如果是增量同步）
          if (templateConfig.incrementalField) {
            this.taskConfig.incrementalField = templateConfig.incrementalField
          }
          
          // 加载源配置
          if (templateConfig.sourceConfig) {
            // 加载连接器ID（如果有）
            if (templateConfig.sourceConfig.connectorId) {
              // 如果是模板中的演示连接器ID（1-4），替换为实际ID
              const templateConnectorId = templateConfig.sourceConfig.connectorId
              if (templateConnectorId >= 1 && templateConnectorId <= 4) {
                const connectorName = this.getTemplateConnectorName(templateConnectorId)
                this.sourceConfig.connectorId = demoConnectorIdMap[connectorName] || templateConnectorId
              } else {
                this.sourceConfig.connectorId = templateConnectorId
              }
              
              // 根据配置内容判断连接器类型
              if (templateConfig.sourceConfig.apiPath || templateConfig.sourceConfig.apiMethod) {
                this.sourceConfig._templateConnectorType = 'API'
              } else {
                this.sourceConfig._templateConnectorType = 'DATABASE'
              }
            }
            
            // 数据库类型配置
            if (templateConfig.sourceConfig.sql) {
              this.sourceConfig.sql = templateConfig.sourceConfig.sql
            }
            if (templateConfig.sourceConfig.tableName) {
              this.sourceConfig.tableName = templateConfig.sourceConfig.tableName
            }
            
            // API类型配置
            if (templateConfig.sourceConfig.apiMethod) {
              this.sourceConfig.apiMethod = templateConfig.sourceConfig.apiMethod
            }
            if (templateConfig.sourceConfig.apiPath) {
              this.sourceConfig.apiPath = templateConfig.sourceConfig.apiPath
            }
            if (templateConfig.sourceConfig.dataPath) {
              this.sourceConfig.dataPath = templateConfig.sourceConfig.dataPath
            }
            if (templateConfig.sourceConfig.apiParams) {
              this.sourceConfig.apiParams = templateConfig.sourceConfig.apiParams
            }
            if (templateConfig.sourceConfig.headers) {
              this.sourceConfig.headers = templateConfig.sourceConfig.headers
            }
            if (typeof templateConfig.sourceConfig.enablePagination !== 'undefined') {
              this.sourceConfig.enablePagination = templateConfig.sourceConfig.enablePagination
            }
            if (templateConfig.sourceConfig.pageNumParam) {
              this.sourceConfig.pageNumParam = templateConfig.sourceConfig.pageNumParam
            }
            if (templateConfig.sourceConfig.pageSizeParam) {
              this.sourceConfig.pageSizeParam = templateConfig.sourceConfig.pageSizeParam
            }
            if (typeof templateConfig.sourceConfig.startPage !== 'undefined') {
              this.sourceConfig.startPage = templateConfig.sourceConfig.startPage
            }
            if (typeof templateConfig.sourceConfig.pageSize !== 'undefined') {
              this.sourceConfig.pageSize = templateConfig.sourceConfig.pageSize
            }
            if (typeof templateConfig.sourceConfig.maxPages !== 'undefined') {
              this.sourceConfig.maxPages = templateConfig.sourceConfig.maxPages
            }
            if (templateConfig.sourceConfig.totalPath) {
              this.sourceConfig.totalPath = templateConfig.sourceConfig.totalPath
            }
          }
          
          // 加载目标配置
          if (templateConfig.targetConfig) {
            // 加载连接器ID（如果有）
            if (templateConfig.targetConfig.connectorId) {
              // 如果是模板中的演示连接器ID（1-4），替换为实际ID
              const templateConnectorId = templateConfig.targetConfig.connectorId
              if (templateConnectorId >= 1 && templateConnectorId <= 4) {
                const connectorName = this.getTemplateConnectorName(templateConnectorId)
                this.targetConfig.connectorId = demoConnectorIdMap[connectorName] || templateConnectorId
              } else {
                this.targetConfig.connectorId = templateConnectorId
              }
              
              // 根据配置内容判断连接器类型
              if (templateConfig.targetConfig.apiPath || templateConfig.targetConfig.apiMethod) {
                this.targetConfig._templateConnectorType = 'API'
              } else {
                this.targetConfig._templateConnectorType = 'DATABASE'
              }
            }
            
            // 数据库类型配置
            if (templateConfig.targetConfig.tableName) {
              this.targetConfig.tableName = templateConfig.targetConfig.tableName
            }
            if (templateConfig.targetConfig.writeMode) {
              this.targetConfig.writeMode = templateConfig.targetConfig.writeMode
            }
            if (templateConfig.targetConfig.primaryKey) {
              this.targetConfig.primaryKey = templateConfig.targetConfig.primaryKey
            }
            
            // API类型配置
            if (templateConfig.targetConfig.apiMethod) {
              this.targetConfig.apiMethod = templateConfig.targetConfig.apiMethod
            }
            if (templateConfig.targetConfig.apiPath) {
              this.targetConfig.apiPath = templateConfig.targetConfig.apiPath
            }
            if (templateConfig.targetConfig.batchMode) {
              this.targetConfig.batchMode = templateConfig.targetConfig.batchMode
            }
            if (typeof templateConfig.targetConfig.batchSize !== 'undefined') {
              this.targetConfig.batchSize = templateConfig.targetConfig.batchSize
            }
            if (templateConfig.targetConfig.wrapperField) {
              this.targetConfig.wrapperField = templateConfig.targetConfig.wrapperField
            }
            if (typeof templateConfig.targetConfig.retryTimes !== 'undefined') {
              this.targetConfig.retryTimes = templateConfig.targetConfig.retryTimes
            }
            if (typeof templateConfig.targetConfig.retryInterval !== 'undefined') {
              this.targetConfig.retryInterval = templateConfig.targetConfig.retryInterval
            }
            if (templateConfig.targetConfig.headers) {
              this.targetConfig.headers = templateConfig.targetConfig.headers
            }
            if (templateConfig.targetConfig.apiParams) {
              this.targetConfig.apiParams = templateConfig.targetConfig.apiParams
            }
          }
          
          // 加载字段映射
          if (templateConfig.mappings && templateConfig.mappings.length > 0) {
            this.mappings = templateConfig.mappings.map(m => ({
              sourceField: m.sourceField || '',
              targetField: m.targetField || '',
              transformType: m.transformType || 'DIRECT',
              dictMappingId: m.dictMappingId || '',
              dictCode: m.dictCode || '',
              transformScript: m.transformScript || '',
              constantValue: m.constantValue || '',
              functionExpr: m.functionExpr || '',
              // 复制处理器链配置
              _processors: m._processors ? JSON.parse(JSON.stringify(m._processors)) : []
            }))
            
            // 加载字段类型和nullable信息
            this.loadFieldTypesForMappings()
          }
          
          // 加载调度配置
          this.scheduleConfig.scheduleType = templateConfig.scheduleType || 'MANUAL'
          this.scheduleConfig.cronExpression = templateConfig.cronExpression || ''
          if (typeof templateConfig.enableRetry !== 'undefined') {
            this.scheduleConfig.enableRetry = templateConfig.enableRetry
          }
          if (typeof templateConfig.maxRetryTimes !== 'undefined') {
            this.scheduleConfig.maxRetryTimes = templateConfig.maxRetryTimes
          }
          
          this.$message.success('模板加载成功！演示连接器、字段映射和处理流程已配置完毕')
        } catch (e) {
          console.error('模板加载失败:', e)
          this.$message.error('模板加载失败: ' + e.message)
        }
      }
    },
    
    // 加载任务进行编辑
    loadTaskForEdit(taskId) {
      // 判断是否为多目标任务，先查询详情
      this.$axios.get(`/v1/task/${taskId}/detail`).then(detailRes => {
        const data = detailRes.data
        const task = data.task
        const targets = data.targets || []
        const fieldMappings = data.fieldMappings || []
        
        // 判断是否多目标
        if (task.multiTarget === 1 && targets.length > 0) {
          // 多目标模式
          this.multiTargetMode = true
          
          // 加载目标列表
          this.targets = targets.map(t => {
            const targetConfig = typeof t.targetConfig === 'string' ? JSON.parse(t.targetConfig) : t.targetConfig
            return {
              id: t.id,
              targetConnectorId: t.targetConnectorId,
              targetName: t.targetName,
              targetConfig: targetConfig,
              sortOrder: t.sortOrder,
              status: t.status,
              // 添加前端辅助字段
              _tableList: [],
              _loadingTables: false,
              _columnInfos: []
            }
          })
          
          // 建立 targetId 映射: 数据库ID -> 数组索引
          const targetIdMap = new Map()
          targets.forEach((t, index) => {
            targetIdMap.set(t.id, index)
          })
          
          // 加载字段映射
          this.mappings = fieldMappings.map(m => {
            // 解析 processorChain 为 _processors
            let processors = []
            if (m.processorChain) {
              try {
                processors = JSON.parse(m.processorChain)
              } catch (e) {
                console.error('处理器链解析失败:', e)
              }
            }
            
            // 解析 cleanseFunctions 为 _cleanseFunctions
            let cleanseFunctions = []
            if (m.cleanseFunctions) {
              try {
                cleanseFunctions = JSON.parse(m.cleanseFunctions)
              } catch (e) {
                console.error('清洗函数解析失败:', e)
              }
            }
            
            // 将数据库targetId转换为数组索引
            const targetIndex = targetIdMap.get(m.targetId)
            if (targetIndex === undefined) {
              console.warn(`字段映射的targetId=${m.targetId}找不到对应的目标,默认为0`, m)
            }
            
            // 返回完整的映射对象
            return {
              targetId: targetIndex !== undefined ? targetIndex : 0,  // 使用数组索引
              sourceField: m.sourceField || '',
              targetField: m.targetField || '',
              transformType: m.transformType || 'DIRECT',
              dictMappingId: m.dictMappingId || null,
              dictSourceTypeValue: m.dictSourceTypeValue || '',
              dictCode: m.dictCode || '',
              dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
              transformScript: m.transformScript || '',
              transformFunction: m.transformFunction || '',
              defaultValue: m.defaultValue || '',
              constantValue: m.constantValue || '',
              nullStrategy: m.nullStrategy || 'KEEP',
              _processors: processors,
              _cleanseFunctions: cleanseFunctions,
              _dictTypeList: []  // 编辑时需要重新加载
            }
          })
        } else {
          // 单目标模式：使用原有逻辑
          this.multiTargetMode = false
          
          // 加载目标连接器
          this.targetConfig.connectorId = task.targetConnectorId
          
          // 解析目标配置
          if (task.targetConfig) {
            try {
              const targetConfigObj = typeof task.targetConfig === 'string' ? JSON.parse(task.targetConfig) : task.targetConfig
              Object.assign(this.targetConfig, targetConfigObj)
            } catch (e) {
              console.error('目标配置解析失败:', e)
            }
          }
          
          // 加载字段映射
          this.mappings = fieldMappings.map(m => {
            // 解析 processorChain 为 _processors
            if (m.processorChain) {
              try {
                m._processors = JSON.parse(m.processorChain)
              } catch (e) {
                console.error('处理器链解析失败:', e)
                m._processors = []
              }
            } else {
              m._processors = []
            }
            
            // 解析 cleanseFunctions 为 _cleanseFunctions
            if (m.cleanseFunctions) {
              try {
                m._cleanseFunctions = JSON.parse(m.cleanseFunctions)
              } catch (e) {
                console.error('清洗函数解析失败:', e)
                m._cleanseFunctions = []
              }
            } else {
              m._cleanseFunctions = []
            }
            
            return m
          })
        }
        
        // 加载基本信息（通用）
        this.taskConfig.id = task.id
        this.taskConfig.taskName = task.taskName
        this.taskConfig.taskCode = task.taskCode
        this.taskConfig.syncMode = task.syncMode
        this.taskConfig.description = task.description || ''
        
        // 加载源连接器
        this.sourceConfig.connectorId = task.sourceConnectorId
        
        // 解析源配置
        if (task.sourceConfig) {
          try {
            const sourceConfigObj = typeof task.sourceConfig === 'string' ? JSON.parse(task.sourceConfig) : task.sourceConfig
            Object.assign(this.sourceConfig, sourceConfigObj)
            
            // 同步tableName到selectedTable，确保下拉框回显
            if (sourceConfigObj.tableName) {
              this.selectedTable = sourceConfigObj.tableName
            }
          } catch (e) {
            console.error('源配置解析失败:', e)
          }
        }
        
        // 加载调度配置
        this.scheduleConfig.scheduleType = task.scheduleType || 'MANUAL'
        this.scheduleConfig.cronExpression = task.cronExpression || ''
        if (typeof task.enableRetry !== 'undefined') {
          this.scheduleConfig.enableRetry = task.enableRetry
        }
        if (typeof task.maxRetryTimes !== 'undefined') {
          this.scheduleConfig.maxRetryTimes = task.maxRetryTimes
        }
        
        // 加载字段类型和nullable信息
        this.$nextTick(() => {
          this.loadFieldTypesForMappings()
        })
        
        this.$message.success('任务加载成功，进入编辑模式')
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('任务加载失败: ' + errMsg)
        this.$router.push('/task')
      })
    },
    
    // 为编辑模式加载字段类型和nullable信息
    loadFieldTypesForMappings() {
      // 单目标模式:使用原有逻辑
      if (!this.multiTargetMode) {
        if (!this.sourceConfig.connectorId || !this.targetConfig.connectorId) {
          console.warn('缺少连接器ID，无法加载字段信息')
          return
        }
        
        // 获取源表名和目标表名
        let sourceTableName = this.sourceConfig.tableName
      
        // 如果源表名为空，尝试从SQL语句中提取
        if (!sourceTableName && this.sourceConfig.sql) {
          const sql = this.sourceConfig.sql.trim()
          // 匹配 FROM table_name 或 FROM schema.table_name
          const fromMatch = sql.match(/\bFROM\s+([`"\[]?\w+[`"\]]?\.)?([`"\[]?\w+[`"\]]?)/i)
          if (fromMatch) {
            sourceTableName = fromMatch[2].replace(/[`"\[\]]/g, '') // 移除引号和方括号
            console.log('从SQL提取的表名:', sourceTableName)
          }
        }
      
        const targetTableName = this.targetConfig.tableName
      
        if (!sourceTableName) {
          console.warn('无法确定源表名，跳过源字段信息加载')
        }
      
        if (!targetTableName) {
          console.warn('缺少目标表名，无法加载字段信息')
          return
        }
      
        // 并行请求源表和目标表的字段信息
        const promises = []
      
        // 只有当sourceTableName存在时才请求源表字段信息
        if (sourceTableName) {
          promises.push(
            this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${sourceTableName}/columns-with-type`).catch(err => {
              console.warn('获取源表字段信息失败', err)
              return { data: [] }
            })
          )
        } else {
          promises.push(Promise.resolve({ data: [] }))
        }
      
        promises.push(
          this.$axios.get(`/v1/dict-source/connector/${this.targetConfig.connectorId}/table/${targetTableName}/columns-with-type`).catch(err => {
            console.warn('获取目标表字段信息失败', err)
            return { data: [] }
          })
        )
      
        Promise.all(promises).then(([sourceRes, targetRes]) => {
          const sourceColumnInfos = sourceRes.data || []
          const targetColumnInfos = targetRes.data || []
        
          console.log('编辑模式加载字段信息:', { sourceTableName, sourceColumnInfos, targetTableName, targetColumnInfos })
        
          // 保存到data中
          this.sourceColumnInfos = sourceColumnInfos
          this.targetColumnInfos = targetColumnInfos
        
          // 建立字段信息映射
          const sourceFieldMap = new Map()
          sourceColumnInfos.forEach(col => {
            sourceFieldMap.set(col.columnName, {
              type: col.columnType,
              nullable: col.nullable,
              remarks: col.remarks,
              columnSize: col.columnSize
            })
          })
        
          const targetFieldMap = new Map()
          targetColumnInfos.forEach(col => {
            targetFieldMap.set(col.columnName, {
              type: col.columnType,
              nullable: col.nullable,
              remarks: col.remarks,
              columnSize: col.columnSize
            })
          })
        
          // 更新每个mapping的字段信息
          this.mappings.forEach(mapping => {
            // 源字段信息
            if (mapping.sourceField && sourceFieldMap.has(mapping.sourceField)) {
              const sourceInfo = sourceFieldMap.get(mapping.sourceField)
              this.$set(mapping, 'sourceType', sourceInfo.type)
              this.$set(mapping, 'sourceNullable', sourceInfo.nullable)
              this.$set(mapping, 'sourceRemarks', sourceInfo.remarks)
              this.$set(mapping, 'sourceColumnSize', sourceInfo.columnSize)
            }
          
            // 目标字段信息
            if (mapping.targetField && targetFieldMap.has(mapping.targetField)) {
              const targetInfo = targetFieldMap.get(mapping.targetField)
              this.$set(mapping, 'targetType', targetInfo.type)
              this.$set(mapping, 'targetNullable', targetInfo.nullable)
              this.$set(mapping, 'targetRemarks', targetInfo.remarks)
              this.$set(mapping, 'targetColumnSize', targetInfo.columnSize)
            }
          })
        
          console.log('字段类型和nullable信息已加载')
        }).catch(err => {
          console.error('加载字段信息失败', err)
        })
      } else {
        // 多目标模式:为每个目标加载字段信息
        if (!this.sourceConfig.connectorId) {
          console.warn('缺少源连接器ID，无法加载字段信息')
          return
        }
        
        // 获取源表名
        let sourceTableName = this.sourceConfig.tableName
        if (!sourceTableName && this.sourceConfig.sql) {
          const sql = this.sourceConfig.sql.trim()
          const fromMatch = sql.match(/\bFROM\s+([`"\[]?\w+[`"\]]?\.)?([`"\[]?\w+[`"\]]?)/i)
          if (fromMatch) {
            sourceTableName = fromMatch[2].replace(/[`"\[\]]/g, '')
          }
        }
        
        // 加载源表字段信息
        const sourcePromise = sourceTableName 
          ? this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${sourceTableName}/columns-with-type`).catch(() => ({ data: [] }))
          : Promise.resolve({ data: [] })
        
        sourcePromise.then(sourceRes => {
          const sourceColumnInfos = sourceRes.data || []
          this.sourceColumnInfos = sourceColumnInfos
          
          // 建立源字段映射
          const sourceFieldMap = new Map()
          sourceColumnInfos.forEach(col => {
            sourceFieldMap.set(col.columnName, {
              type: col.columnType,
              nullable: col.nullable,
              remarks: col.remarks,
              columnSize: col.columnSize
            })
          })
          
          // 为每个目标加载目标表字段信息
          const targetPromises = this.targets.map((target, index) => {
            if (!target.targetConnectorId || !target.targetConfig.tableName) {
              return Promise.resolve({ index, data: [] })
            }
            
            return this.$axios.get(
              `/v1/dict-source/connector/${target.targetConnectorId}/table/${target.targetConfig.tableName}/columns-with-type`
            ).then(res => ({
              index: index,
              data: res.data || []
            })).catch(() => ({
              index: index,
              data: []
            }))
          })
          
          Promise.all(targetPromises).then(results => {
            // 建立每个目标的字段映射
            const targetFieldMaps = []
            results.forEach(result => {
              const fieldMap = new Map()
              result.data.forEach(col => {
                fieldMap.set(col.columnName, {
                  type: col.columnType,
                  nullable: col.nullable,
                  remarks: col.remarks,
                  columnSize: col.columnSize
                })
              })
              targetFieldMaps[result.index] = fieldMap
            })
            
            // 更新每个mapping的字段信息
            this.mappings.forEach(mapping => {
              // 源字段信息
              if (mapping.sourceField && sourceFieldMap.has(mapping.sourceField)) {
                const sourceInfo = sourceFieldMap.get(mapping.sourceField)
                this.$set(mapping, 'sourceType', sourceInfo.type)
                this.$set(mapping, 'sourceNullable', sourceInfo.nullable)
                this.$set(mapping, 'sourceRemarks', sourceInfo.remarks)
                this.$set(mapping, 'sourceColumnSize', sourceInfo.columnSize)
              }
              
              // 目标字段信息(根据targetId获取对应目标的字段信息)
              if (mapping.targetId !== undefined && targetFieldMaps[mapping.targetId]) {
                const targetFieldMap = targetFieldMaps[mapping.targetId]
                if (mapping.targetField && targetFieldMap.has(mapping.targetField)) {
                  const targetInfo = targetFieldMap.get(mapping.targetField)
                  this.$set(mapping, 'targetType', targetInfo.type)
                  this.$set(mapping, 'targetNullable', targetInfo.nullable)
                  this.$set(mapping, 'targetRemarks', targetInfo.remarks)
                  this.$set(mapping, 'targetColumnSize', targetInfo.columnSize)
                }
              }
            })
            
            console.log('多目标模式:字段类型和nullable信息已加载')
          })
        })
      }
    },
    
    handleNext() {
      // 步骤1: 选择数据源 -> 强制抽取数据
      if (this.activeStep === 1) {
        // 检查是否已抽取数据
        if (this.sourcePreviewData.length === 0) {
          this.$confirm('检测到您还未抽取数据，是否立即抽取？', '提示', {
            confirmButtonText: '立即抽取',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(() => {
            // 执行抽取
            if (this.sourceConnectorType === 'DATABASE') {
              this.handlePreviewSource()
            } else if (this.sourceConnectorType === 'API') {
              this.handlePreviewApiSource()
            }
          }).catch(() => {
            this.$message.info('已取消')
          })
          return // 阻止跳转
        }
      }
      
      // 验证当前步骤
      if (!this.validateCurrentStep()) {
        return
      }
      this.activeStep++
    },
    
    validateStep(step) {
      // 保留旧方法供兼容，实际调用validateCurrentStep
      const oldStep = this.activeStep
      this.activeStep = step
      const result = this.validateCurrentStep()
      this.activeStep = oldStep
      return result
    },
    
    handleSourceConnectorChange(connectorId) {
      // 清除模板连接器类型标识（用户手动选择时）
      delete this.sourceConfig._templateConnectorType
      
      // 清空表列表和预览数据
      this.sourceTableList = []
      this.sourcePreviewData = []
      this.sourcePreviewColumns = []
      this.sourceColumnInfos = []  // 清空源表字段信息
      this.sourceConfig.sql = ''
      this.sourceConfig.tableName = ''
      this.sourceConfig.apiPath = ''
      this.sourceConfig.apiMethod = 'GET'
      this.sourceConfig.dataPath = ''
      this.sourceConfig.apiParams = []
      this.sourceConfig.enablePagination = false
      this.selectedTable = ''
    },
    
    handleLoadTables() {
      if (!this.sourceConfig.connectorId) {
        this.$message.warning('请先选择源连接器')
        return
      }
      
      this.loadingTables = true
      this.$axios.get(`/v1/task/connector/${this.sourceConfig.connectorId}/tables-with-comment`).then(res => {
        this.sourceTableList = res.data
        this.$message.success(`已加载 ${res.data.length} 个表，请在下拉框中选择`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '获取表列表失败'
        this.$message.error(errMsg)
      }).finally(() => {
        this.loadingTables = false
      })
    },
    
    handleTableSelect(tableName) {
      if (!tableName) {
        return
      }
      
      const sql = `SELECT * FROM ${tableName}`
      this.sourceConfig.sql = sql
      this.sourceConfig.tableName = tableName
      
      // 切换表时，清空之前的抽取结果和字段信息，避免数据与表不一致
      this.sourcePreviewData = []
      this.sourcePreviewColumns = []
      this.sourceColumnInfos = []  // 清空源表字段信息
      
      this.$message({
        type: 'success',
        message: `已生成SQL语句，您可以在下方编辑器中继续修改`,
        duration: 3000
      })
      
      // 自动滚动到SQL编辑器
      this.$nextTick(() => {
        const textarea = document.querySelector('textarea[placeholder*="请输入SQL查询语句"]')
        if (textarea) {
          textarea.focus()
          // 将光标移动到SQL末尾
          textarea.setSelectionRange(sql.length, sql.length)
        }
      })
    },
    
    handleTargetConnectorChange(connectorId) {
      // 清除模板连接器类型标识（用户手动选择时）
      delete this.targetConfig._templateConnectorType
      
      // 清空目标表列表与目标表名
      this.targetTableList = []
      this.targetColumnInfos = []  // 清空目标表字段信息
      this.targetConfig.tableName = ''
      this.targetConfig.apiPath = ''
      this.targetConfig.apiMethod = 'POST'
      this.targetConfig.batchMode = 'batch'
      this.targetConfig.batchSize = 100
      this.targetConfig.wrapperField = ''
      this.targetConfig.retryTimes = 3
      this.targetConfig.retryInterval = 1000
      
      const connector = this.connectorList.find(c => c.id === connectorId)
      if (connector && connector.connectorType === 'DATABASE') {
        this.$nextTick(() => {
          this.$message.info('可点击"加载目标表列表"查看可用表')
        })
      }
    },
    
    handleLoadTargetTables() {
      if (!this.targetConfig.connectorId) {
        this.$message.warning('请先选择目标连接器')
        return
      }
      this.$axios.get(`/v1/task/connector/${this.targetConfig.connectorId}/tables-with-comment`).then(res => {
        this.targetTableList = res.data
        this.$message.success(`已加载 ${res.data.length} 个目标表`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '获取目标表列表失败'
        this.$message.error(errMsg)
      })
    },
    
    handleTargetTableSelect(tableName) {
      this.targetConfig.tableName = tableName
      // 切换目标表时，清空目标表字段信息
      this.targetColumnInfos = []
    },
    
    handlePreviewSource() {
      if (!this.sourceConfig.connectorId) {
        this.$message.warning('请先选择源连接器')
        return
      }
      
      if (!this.sourceConfig.sql) {
        this.$message.warning('请先输入SQL查询语句')
        return
      }
      
      // 自动验证SQL语法
      const sql = this.sourceConfig.sql.trim()
      const sqlUpper = sql.toUpperCase()
      
      // 1. 基本语法检查
      if (!sqlUpper.startsWith('SELECT')) {
        this.sqlValidationError = '仅支持SELECT查询语句'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      if (!sqlUpper.includes('FROM')) {
        this.sqlValidationError = '缺少FROM子句'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 2. 检查WHERE子句完整性
      const whereIndex = sqlUpper.indexOf('WHERE')
      if (whereIndex !== -1) {
        const afterWhere = sql.substring(whereIndex + 5).trim()
        if (!afterWhere || afterWhere.length === 0) {
          this.sqlValidationError = 'WHERE子句后缺少条件'
          this.$message.error(this.sqlValidationError)
          return
        }
        
        const afterWhereUpper = afterWhere.toUpperCase()
        const nextKeywords = ['GROUP BY', 'ORDER BY', 'HAVING', 'LIMIT', 'UNION', 'JOIN']
        const startsWithKeyword = nextKeywords.some(keyword => afterWhereUpper.startsWith(keyword))
        if (startsWithKeyword) {
          this.sqlValidationError = 'WHERE子句后缺少条件表达式'
          this.$message.error(this.sqlValidationError)
          return
        }
      }
      
      // 3. 检查SELECT和FROM之间是否有字段
      const selectIndex = sqlUpper.indexOf('SELECT')
      const fromIndex = sqlUpper.indexOf('FROM')
      const betweenSelectFrom = sql.substring(selectIndex + 6, fromIndex).trim()
      if (!betweenSelectFrom) {
        this.sqlValidationError = 'SELECT后缺少字段列表'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 4. 检查FROM后是否有表名
      const afterFrom = sql.substring(fromIndex + 4).trim()
      if (!afterFrom) {
        this.sqlValidationError = 'FROM后缺少表名'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      const tableNameMatch = afterFrom.match(/^([\w`"\[\]]+)/)
      if (!tableNameMatch) {
        this.sqlValidationError = 'FROM后的表名格式不正确'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 5. 检查括号匹配
      const openParens = (sqlUpper.match(/\(/g) || []).length
      const closeParens = (sqlUpper.match(/\)/g) || []).length
      if (openParens !== closeParens) {
        this.sqlValidationError = `括号不匹配（左括号${openParens}个，右括号${closeParens}个）`
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 6. 检查引号匹配
      const singleQuotes = (sql.match(/'/g) || []).length
      if (singleQuotes % 2 !== 0) {
        this.sqlValidationError = '单引号不匹配'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      const doubleQuotes = (sql.match(/"/g) || []).length
      if (doubleQuotes % 2 !== 0) {
        this.sqlValidationError = '双引号不匹配'
        this.$message.error(this.sqlValidationError)
        return
      }
      
      // 验证通过，清除错误信息
      this.sqlValidationError = ''
      
      // 执行数据抽取
      const params = {
        connectorId: this.sourceConfig.connectorId,
        sql: this.sourceConfig.sql,
        limit: this.previewLimit,  // 使用用户设置的条数
        syncMode: this.taskConfig.syncMode,  // 增量/全量模式
        incrementalField: this.taskConfig.incrementalField  // 增量字段
      }
      
      this.sourcePreviewLoading = true
      
      this.$axios.post('/v1/task/preview', params).then(res => {
        this.sourcePreviewData = res.data.data
        this.sourcePreviewColumns = res.data.columns
        
        // 调试信息：查看数据和列
        console.log('=== 数据预览调试信息 ===');
        console.log('数据条数:', this.sourcePreviewData.length);
        console.log('列数:', this.sourcePreviewColumns.length);
        console.log('列名:', this.sourcePreviewColumns);
        console.log('第一条数据:', this.sourcePreviewData[0]);
        console.log('====================');
        
        // 抽取成功后，尝试加载字段详细信息
        if (this.sourceConfig.connectorId) {
          let tableName = this.sourceConfig.tableName
          
          // 如果没有选择表名，尝试从SQL中解析
          if (!tableName && this.sourceConfig.sql) {
            tableName = this.extractTableNameFromSql(this.sourceConfig.sql)
          }
          
          if (tableName) {
            this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${tableName}/columns-with-type`)
              .then(columnRes => {
                this.sourceColumnInfos = columnRes.data || []
                console.log('已加载源表字段信息:', this.sourceColumnInfos.length, '个字段')
              })
              .catch(err => {
                console.warn('加载源表字段信息失败', err)
              })
          } else {
            console.warn('无法获取表名，跳过字段信息加载')
          }
        }
        
        this.$message.success(`抽取成功! 共 ${res.data.total} 条数据`)
        
        // 打开预览弹窗
        this.showPreviewDialog = true
      }).catch(err => {
        // 获取详细错误信息
        let errMsg = '抽取失败'
        if (err.response && err.response.data) {
          if (err.response.data.message) {
            errMsg = err.response.data.message
          } else if (err.response.data.msg) {
            errMsg = err.response.data.msg
          }
        } else if (err.message) {
          errMsg = err.message
        }
        
        // 显示错误提示
        this.$message.error(errMsg)
        this.sourcePreviewData = []
        this.sourcePreviewColumns = []
      }).finally(() => {
        this.sourcePreviewLoading = false
      })
    },
    
    handlePreviewApiSource() {
      if (!this.sourceConfig.connectorId) {
        this.$message.warning('请先选择源连接器')
        return
      }
      
      if (!this.sourceConfig.apiPath) {
        this.$message.warning('请输入API路径')
        return
      }
      
      this.sourcePreviewLoading = true
      
      // 构建请求参数
      const enabledParams = (this.sourceConfig.apiParams || []).filter(p => p.enabled && p.key)
      const params = {}
      enabledParams.forEach(p => {
        params[p.key] = p.value || ''
      })
      
      // 构建请求头
      const enabledHeaders = (this.sourceConfig.headers || []).filter(h => h.enabled && h.key)
      const headers = {}
      enabledHeaders.forEach(h => {
        headers[h.key] = h.value || ''
      })
      
      // 构建请求体
      let requestBody = null
      if (this.sourceConfig.bodyType === 'json' && this.sourceConfig.jsonBody) {
        try {
          requestBody = JSON.parse(this.sourceConfig.jsonBody)
        } catch (e) {
          this.$message.error('JSON Body格式错误，请检查')
          this.sourcePreviewLoading = false
          return
        }
      } else if (this.sourceConfig.bodyType === 'form-data') {
        const enabledFormData = (this.sourceConfig.formData || []).filter(f => f.enabled && f.key)
        if (enabledFormData.length > 0) {
          requestBody = {}
          enabledFormData.forEach(f => {
            requestBody[f.key] = f.value || ''
          })
        }
      }
      
      const requestData = {
        connectorId: this.sourceConfig.connectorId,
        apiPath: this.sourceConfig.apiPath,
        apiMethod: this.sourceConfig.apiMethod,
        params: params,
        headers: headers,
        bodyType: this.sourceConfig.bodyType || 'none',
        requestBody: requestBody,
        dataPath: this.sourceConfig.dataPath,
        limit: this.previewLimit  // 使用用户设置的条数
      }
      
      const loading = this.$loading({
        lock: true,
        text: '正在预览API数据...',
        spinner: 'el-icon-loading'
      })
      
      this.$axios.post('/v1/task/preview-api', requestData).then(res => {
        this.sourcePreviewData = res.data.data
        this.sourcePreviewColumns = res.data.columns
        this.$message.success(`预览成功! 共 ${res.data.total} 条数据`)
        
        // 打开预览弹窗
        this.showPreviewDialog = true
      }).catch(err => {
        let errMsg = '预览失败'
        if (err.response && err.response.data) {
          if (err.response.data.message) {
            errMsg = err.response.data.message
          } else if (err.response.data.msg) {
            errMsg = err.response.data.msg
          }
        } else if (err.message) {
          errMsg = err.message
        }
        
        this.$message.error(errMsg)
        this.sourcePreviewData = []
        this.sourcePreviewColumns = []
      }).finally(() => {
        this.sourcePreviewLoading = false
        loading.close()  // 关闭全屏加载遮罩
      })
    },
    
    handleAutoMapping() {
      // 检查是否有预览数据
      if (this.sourcePreviewColumns.length > 0) {
        // 如果已经有sourceColumnInfos，直接使用
        // 否则先加载sourceColumnInfos，再执行映射
        if (!this.sourceColumnInfos || this.sourceColumnInfos.length === 0) {
          // 尝试从源表加载字段信息
          if (this.sourceConfig.connectorId) {
            let tableName = this.sourceConfig.tableName
            
            // 如果没有选择表名，尝试从SQL中解析
            if (!tableName && this.sourceConfig.sql) {
              tableName = this.extractTableNameFromSql(this.sourceConfig.sql)
            }
            
            if (tableName) {
              const loading = this.$loading({
                lock: true,
                text: '正在加载字段信息...',
                spinner: 'el-icon-loading'
              })
              
              this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${tableName}/columns-with-type`)
                .then(res => {
                  this.sourceColumnInfos = res.data || []
                  this.doAutoMapping(this.sourcePreviewColumns)
                })
                .catch(err => {
                  console.warn('加载源表字段信息失败，直接使用预览列', err)
                  this.doAutoMapping(this.sourcePreviewColumns)
                })
                .finally(() => {
                  loading.close()
                })
            } else {
              // 没有源表信息，直接使用预览数据的字段
              this.doAutoMapping(this.sourcePreviewColumns)
            }
          } else {
            // 没有连接器ID，直接使用预览数据的字段
            this.doAutoMapping(this.sourcePreviewColumns)
          }
        } else {
          // 已经有sourceColumnInfos，直接使用
          this.doAutoMapping(this.sourcePreviewColumns)
        }
        return
      }
      
      // 没有预览数据，尝试自动获取字段
      this.$confirm('检测到没有预览数据，是否自动获取源表字段信息？', '提示', {
        confirmButtonText: '自动获取',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.fetchSourceFieldsForMapping()
      }).catch(() => {
        this.$message.info('已取消，请先预览数据或手动添加映射')
      })
    },
    
    // 获取源表字段用于智能映射
    fetchSourceFieldsForMapping() {
      const sourceType = this.getSourceType()
      
      if (sourceType === 'DATABASE') {
        this.fetchDatabaseFields()
      } else if (sourceType === 'API') {
        this.$message.warning('API数据源请先点击"预览数据"获取字段信息')
      }
    },
    
    // 获取数据库字段
    fetchDatabaseFields() {
      if (!this.sourceConfig.connectorId) {
        this.$message.warning('请先选择源连接器')
        return
      }
      
      if (!this.sourceConfig.sql) {
        this.$message.warning('请先配置SQL查询语句')
        return
      }
      
      const loading = this.$loading({
        lock: true,
        text: '正在获取字段信息...',
        spinner: 'el-icon-loading'
      })
      
      const params = {
        connectorId: this.sourceConfig.connectorId,
        sql: this.sourceConfig.sql,
        limit: 1,  // 只需要字段信息，不需要大量数据
        syncMode: this.taskConfig.syncMode,  // 增量/全量模式
        incrementalField: this.taskConfig.incrementalField  // 增量字段
      }
      
      this.$axios.post('/v1/task/preview', params).then(res => {
        const columns = res.data.columns || []
        if (columns.length === 0) {
          this.$message.warning('未获取到字段信息')
          return
        }
        
        // 执行智能映射
        this.doAutoMapping(columns)
        this.$message.success(`已获取 ${columns.length} 个字段，智能映射完成！`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '获取字段失败'
        this.$message.error(errMsg)
      }).finally(() => {
        loading.close()
      })
    },
    
    // 执行智能映射
    doAutoMapping(sourceColumns) {
      if (!sourceColumns || sourceColumns.length === 0) {
        this.$message.warning('没有可用的源字段')
        return
      }
      
      // 尝试获取目标表字段进行智能匹配
      if (this.targetConnectorType === 'DATABASE' && this.targetConfig.connectorId && this.targetConfig.tableName) {
        this.fetchTargetFieldsAndMatch(sourceColumns)
      } else {
        // 目标不是数据库或未配置，先加载源表字段信息，然后只填充源字段
        // 检查是否已经有sourceColumnInfos
        if (this.sourceColumnInfos && this.sourceColumnInfos.length > 0) {
          // 已经有sourceColumnInfos，直接使用
          const sourceTypeMap = new Map()
          const sourceNullableMap = new Map()
          const sourceRemarksMap = new Map()
          const sourceColumnSizeMap = new Map()
          this.sourceColumnInfos.forEach(col => {
            sourceTypeMap.set(col.columnName, col.columnType)
            sourceNullableMap.set(col.columnName, col.nullable)
            sourceRemarksMap.set(col.columnName, col.remarks)
            sourceColumnSizeMap.set(col.columnName, col.columnSize)
          })
          
          this.mappings = sourceColumns.map(col => ({
            sourceField: col,
            targetField: '',
            sourceType: sourceTypeMap.get(col) || '',
            sourceNullable: sourceNullableMap.get(col),
            sourceRemarks: sourceRemarksMap.get(col) || '',
            sourceColumnSize: sourceColumnSizeMap.get(col),
            targetType: '',
            transformType: 'DIRECT',
            dictMappingId: null,
            dictSourceTypeValue: '',
            dictCode: '',
            transformScript: '',
            transformFunction: '',
            defaultValue: '',
            constantValue: '',
            _dictTypeList: [],
            nullStrategy: 'KEEP',
            _processors: [{
              type: 'TRANSFORM',
              order: 1,
              config: {
                transformType: 'DIRECT',
                dictMappingId: null,
                dictSourceTypeValue: '',
                dictOutputMode: 'TARGET_KEY',
                constantValue: '',
                defaultValue: ''
              }
            }]
          }))
          
          this.$message.success(`智能映射完成！已生成 ${this.mappings.length} 个字段映射，请手动指定目标字段`)
        } else if (this.sourceConfig.connectorId && this.sourceConfig.tableName) {
          // 如果有源表信息，先加载字段类型
          this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${this.sourceConfig.tableName}/columns-with-type`)
            .then(res => {
              const sourceColumnInfos = res.data || []
              this.sourceColumnInfos = sourceColumnInfos  // 保存到data中
              
              // 建立字段类型映射
              const sourceTypeMap = new Map()
              const sourceNullableMap = new Map()
              const sourceRemarksMap = new Map()
              const sourceColumnSizeMap = new Map()
              sourceColumnInfos.forEach(col => {
                sourceTypeMap.set(col.columnName, col.columnType)
                sourceNullableMap.set(col.columnName, col.nullable)
                sourceRemarksMap.set(col.columnName, col.remarks)
                sourceColumnSizeMap.set(col.columnName, col.columnSize)
              })
              
              this.mappings = sourceColumns.map(col => ({
                sourceField: col,
                targetField: '',  // 目标字段留空，由用户手动填写
                sourceType: sourceTypeMap.get(col) || '',
                sourceNullable: sourceNullableMap.get(col),
                sourceRemarks: sourceRemarksMap.get(col) || '',
                sourceColumnSize: sourceColumnSizeMap.get(col),
                targetType: '',
                transformType: 'DIRECT',
                dictMappingId: null,
                dictSourceTypeValue: '',
                dictCode: '',
                transformScript: '',
                transformFunction: '',
                defaultValue: '',
                constantValue: '',
                _dictTypeList: [],
                nullStrategy: 'KEEP',
                _processors: [{
                  type: 'TRANSFORM',
                  order: 1,
                  config: {
                    transformType: 'DIRECT',
                    dictMappingId: null,
                    dictSourceTypeValue: '',
                    dictOutputMode: 'TARGET_KEY',
                    constantValue: '',
                    defaultValue: ''
                  }
                }]
              }))
              
              this.$message.success(`智能映射完成！已生成 ${this.mappings.length} 个字段映射，请手动指定目标字段`)
            })
            .catch(err => {
              console.warn('获取源表字段信息失败，使用简化映射', err)
              // 如果获取失败，退回到简化映射
              this.mappings = sourceColumns.map(col => ({
                sourceField: col,
                targetField: '',
                transformType: 'DIRECT',
                dictMappingId: null,
                dictSourceTypeValue: '',
                dictCode: '',
                transformScript: '',
                transformFunction: '',
                defaultValue: '',
                constantValue: '',
                _dictTypeList: [],
                nullStrategy: 'KEEP',
                _processors: [{
                  type: 'TRANSFORM',
                  order: 1,
                  config: {
                    transformType: 'DIRECT',
                    dictMappingId: null,
                    dictSourceTypeValue: '',
                    dictOutputMode: 'TARGET_KEY',
                    constantValue: '',
                    defaultValue: ''
                  }
                }]
              }))
              this.$message.success(`智能映射完成！已生成 ${this.mappings.length} 个字段映射，请手动指定目标字段`)
            })
        } else {
          // 没有源表信息，使用简化映射
          this.mappings = sourceColumns.map(col => ({
            sourceField: col,
            targetField: '',  // 目标字段留空，由用户手动填写
            transformType: 'DIRECT',
            dictMappingId: null,
            dictSourceTypeValue: '',
            dictCode: '',
            transformScript: '',
            transformFunction: '',
            defaultValue: '',
            constantValue: '',
            _dictTypeList: [],
            nullStrategy: 'KEEP',
            _processors: [{
              type: 'TRANSFORM',
              order: 1,
              config: {
                transformType: 'DIRECT',
                dictMappingId: null,
                dictSourceTypeValue: '',
                dictOutputMode: 'TARGET_KEY',
                constantValue: '',
                defaultValue: ''
              }
            }]
          }))
          
          this.$message.success(`智能映射完成！已生成 ${this.mappings.length} 个字段映射，请手动指定目标字段`)
        }
      }
    },
    
    // 获取目标表字段并智能匹配
    fetchTargetFieldsAndMatch(sourceColumns) {
      const loading = this.$loading({
        lock: true,
        text: '正在获取表字段信息...',
        spinner: 'el-icon-loading'
      })
      
      // 并行请求源表和目标表的字段信息（包含类型）
      Promise.all([
        this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${this.sourceConfig.tableName || 'TEMP'}/columns-with-type`).catch(err => {
          console.warn('获取源表字段信息失败', err)
          return { data: [] }
        }),
        this.$axios.get(`/v1/dict-source/connector/${this.targetConfig.connectorId}/table/${this.targetConfig.tableName}/columns-with-type`)
      ]).then(([sourceRes, targetRes]) => {
        const sourceColumnInfos = sourceRes.data || []
        const targetColumnInfos = targetRes.data || []
        
        console.log('源表字段信息:', sourceColumnInfos)
        console.log('目标表字段信息:', targetColumnInfos)
        
        // 保存到data中，供后续使用
        this.sourceColumnInfos = sourceColumnInfos
        this.targetColumnInfos = targetColumnInfos
        
        if (targetColumnInfos.length === 0) {
          this.$message.warning('未获取到目标表字段，将只填充源字段')
          // 建立源字段类型映射
          const sourceTypeMap = new Map()
          sourceColumnInfos.forEach(col => {
            sourceTypeMap.set(col.columnName, col.columnType)
          })
          
          this.mappings = sourceColumns.map(col => {
            const sourceType = sourceTypeMap.get(col) || ''
            return {
              sourceField: col,
              targetField: '',
              sourceType: sourceType,
              targetType: '',
              transformType: 'DIRECT',
              dictMappingId: null,
              dictSourceTypeValue: '',
              dictCode: '',
              transformScript: '',
              transformFunction: '',
              defaultValue: '',
              constantValue: '',
              _dictTypeList: [],
              // 高级转换字段
              nullStrategy: 'KEEP',
              // 默认添加一个直接映射的数据转换处理器
              _processors: [{
                type: 'TRANSFORM',
                order: 1,
                config: {
                  transformType: 'DIRECT',
                  dictMappingId: null,
                  dictSourceTypeValue: '',
                  dictOutputMode: 'TARGET_KEY',
                  constantValue: '',
                  defaultValue: ''
                }
              }]
            }
          })
          return
        }
        
        // 建立字段类型映射
        const sourceTypeMap = new Map()
        const sourceNullableMap = new Map()
        const sourceRemarksMap = new Map()
        const sourceColumnSizeMap = new Map()
        sourceColumnInfos.forEach(col => {
          sourceTypeMap.set(col.columnName, col.columnType)
          sourceNullableMap.set(col.columnName, col.nullable)
          sourceRemarksMap.set(col.columnName, col.remarks)
          sourceColumnSizeMap.set(col.columnName, col.columnSize)
        })
        
        const targetTypeMap = new Map()
        const targetNullableMap = new Map()
        const targetRemarksMap = new Map()
        const targetColumnSizeMap = new Map()
        const targetFieldSet = new Set()
        targetColumnInfos.forEach(col => {
          targetTypeMap.set(col.columnName, col.columnType)
          targetNullableMap.set(col.columnName, col.nullable)
          targetRemarksMap.set(col.columnName, col.remarks)
          targetColumnSizeMap.set(col.columnName, col.columnSize)
          targetFieldSet.add(col.columnName)
        })
        
        // 智能匹配：以目标字段为基准，为每个目标字段寻找匹配的源字段
        let matchedCount = 0
        const sourceFieldSet = new Set(sourceColumns)
        
        this.mappings = targetColumnInfos.map(targetCol => {
          const targetField = targetCol.columnName
          // 尝试在源字段中找到同名字段
          const sourceField = sourceFieldSet.has(targetField) ? targetField : ''
          if (sourceField) matchedCount++
          
          const sourceType = sourceField ? (sourceTypeMap.get(sourceField) || '') : ''
          const targetType = targetTypeMap.get(targetField) || ''
          const sourceNullable = sourceField ? sourceNullableMap.get(sourceField) : undefined
          const targetNullable = targetNullableMap.get(targetField)
          const sourceRemarks = sourceField ? sourceRemarksMap.get(sourceField) : undefined
          const targetRemarks = targetRemarksMap.get(targetField)
          const sourceColumnSize = sourceField ? sourceColumnSizeMap.get(sourceField) : undefined
          const targetColumnSize = targetColumnSizeMap.get(targetField)
          
          console.log(`目标字段 ${targetField}: sourceField=${sourceField}, sourceType=${sourceType}, targetType=${targetType}`)
          
          return {
            sourceField: sourceField,
            targetField: targetField,
            sourceType: sourceType,
            targetType: targetType,
            sourceNullable: sourceNullable,
            targetNullable: targetNullable,
            sourceRemarks: sourceRemarks,
            targetRemarks: targetRemarks,
            sourceColumnSize: sourceColumnSize,
            targetColumnSize: targetColumnSize,
            transformType: 'DIRECT',
            dictMappingId: null,
            dictSourceTypeValue: '',
            dictCode: '',
            transformScript: '',
            transformFunction: '',
            defaultValue: '',
            constantValue: '',
            _dictTypeList: [],
            // 高级转换字段
            nullStrategy: 'KEEP',
            // 默认添加一个直接映射的数据转换处理器
            _processors: [{
              type: 'TRANSFORM',
              order: 1,
              config: {
                transformType: 'DIRECT',
                dictMappingId: null,
                dictSourceTypeValue: '',
                dictOutputMode: 'TARGET_KEY',
                constantValue: '',
                defaultValue: ''
              }
            }]
          }
        })
        
        this.$message.success(`智能映射完成！共 ${targetColumnInfos.length} 个目标字段，自动匹配 ${matchedCount} 个源字段`)
      }).catch(err => {
        console.error('获取表字段信息失败', err)
        const errMsg = (err.response && err.response.data && err.response.data.message) || '获取表字段信息失败'
        this.$message.warning(errMsg + '，将只填充源字段')
        
        this.mappings = sourceColumns.map(col => ({
          sourceField: col,
          targetField: '',
          sourceType: '',
          targetType: '',
          transformType: 'DIRECT',
          dictMappingId: null,
          dictSourceTypeValue: '',
          dictCode: '',
          transformScript: '',
          transformFunction: '',
          defaultValue: '',
          constantValue: '',
          _dictTypeList: [],
          // 高级转换字段
          conditionConfig: '',
          _conditionRules: [],
          aggregateConfig: '',
          nullStrategy: 'KEEP',
          filterRule: '',
          normalizeType: 'NONE',
          _filterType: 'NONE',
          _filterValue: '',
          _filterMin: 0,
          _filterMax: 100,
          // 默认添加一个直接映射的数据转换处理器
          _processors: [{
            type: 'TRANSFORM',
            order: 1,
            config: {
              transformType: 'DIRECT',
              dictMappingId: null,
              dictSourceTypeValue: '',
              dictOutputMode: 'TARGET_KEY',
              constantValue: '',
              defaultValue: ''
            }
          }]
        }))
      }).finally(() => {
        loading.close()
      })
    },
    
    handleAddMapping() {
      const newMapping = {
        sourceField: '',
        targetField: '',
        sourceType: '',  // 源字段类型
        targetType: '',  // 目标字段类型
        transformType: 'DIRECT',
        dictMappingId: null,
        dictSourceTypeValue: '',
        dictCode: '',
        transformScript: '',
        transformFunction: '',
        defaultValue: '',
        constantValue: '',
        _dictTypeList: [],  // 用于存储字典类型列表
        // 高级转换字段
        conditionConfig: '',
        aggregateConfig: '',
        nullStrategy: 'KEEP',
        // 默认添加一个直接映射的数据转换处理器
        _processors: [{
          type: 'TRANSFORM',
          order: 1,
          config: {
            transformType: 'DIRECT',
            dictMappingId: null,
            dictSourceTypeValue: '',
            dictOutputMode: 'TARGET_KEY',
            constantValue: '',
            defaultValue: ''
          }
        }]
      }
      this.mappings.push(newMapping)
    },
    
    handleDeleteMapping(index) {
      this.mappings.splice(index, 1)
    },
    
    handleClearMappings() {
      this.$confirm('确认清空所有映射吗?', '提示', { type: 'warning' }).then(() => {
        this.mappings = []
      })
    },
    
    handleTransformTypeChange(row) {
      // 清空其他转换配置
      row.dictCode = ''
      row.transformScript = ''
      row.transformFunction = ''
      row.constantValue = ''
      row.dictMappingId = null
      row.dictSourceTypeValue = ''
      row._dictTypeList = []
    },
    
    // 数据清洗: 显示配置对话框
    showCleanseConfig(mapping) {
      this.currentMapping = mapping
      
      // 初始化数据清洗配置
      if (!mapping.nullStrategy) {
        this.$set(mapping, 'nullStrategy', 'KEEP')
      }
      
      // 解析清洗函数链
      if (mapping.cleanseFunctions && mapping.cleanseFunctions.trim()) {
        try {
          this.$set(mapping, '_cleanseFunctions', JSON.parse(mapping.cleanseFunctions))
        } catch (e) {
          console.error('解析清洗函数链失败', e)
          this.$set(mapping, '_cleanseFunctions', [])
        }
      } else {
        this.$set(mapping, '_cleanseFunctions', [])
      }
      
      this.cleanseDialogVisible = true
    },
    
    // 数据清洗: 保存配置
    saveCleanseConfig() {
      // 更新cleanseFunctions字段
      if (this.currentMapping._cleanseFunctions && this.currentMapping._cleanseFunctions.length > 0) {
        this.currentMapping.cleanseFunctions = JSON.stringify(this.currentMapping._cleanseFunctions)
      } else {
        this.currentMapping.cleanseFunctions = ''
      }
      
      this.cleanseDialogVisible = false
      this.$message.success('数据清洗配置已保存')
    },
    
    // 加载清洗函数列表
    loadCleanseFunctions() {
      this.$axios.get('/v1/transform/cleanse/functions').then(res => {
        this.cleanseFunctionList = res.data || []
        console.log('已加载清洗函数列表:', this.cleanseFunctionList.length, '个函数')
        console.log('函数详情:', this.cleanseFunctionList)
      }).catch(err => {
        console.error('加载清洗函数列表失败', err)
        const errMsg = err.response && err.response.data && err.response.data.message 
          ? err.response.data.message 
          : err.message
        this.$message.error('加载清洗函数列表失败: ' + errMsg)
      })
    },
    
    // 显示添加清洗函数对话框
    showAddCleanseFunctionDialog() {
      console.log('打开添加清洗函数对话框')
      console.log('当前函数列表:', this.cleanseFunctionList)
      console.log('当前选中分类:', this.selectedFunctionCategory)
      console.log('过滤后的函数:', this.filteredFunctions)
      
      // 如果函数列表为空，提示用户
      if (this.cleanseFunctionList.length === 0) {
        this.$message.warning('正在加载函数列表，请稍后...')
        // 重新加载函数列表
        this.loadCleanseFunctions()
      }
      
      this.selectedFunctionCategory = 'TEXT'
      this.selectedFunctionCode = null
      this.selectedFunction = null
      this.functionParams = {}
      this.previewInputValue = ''
      this.previewOutputValue = null
      this.addCleanseFunctionDialogVisible = true
    },
    
    // 分类变化
    onCategoryChange() {
      this.selectedFunctionCode = null
      this.selectedFunction = null
      this.functionParams = {}
      this.previewInputValue = ''
      this.previewOutputValue = null
    },
    
    // 选择函数
    onFunctionSelect(functionCode) {
      this.selectedFunction = this.cleanseFunctionList.find(f => f.functionCode === functionCode)
      
      // 初始化参数
      this.functionParams = {}
      if (this.selectedFunction && this.selectedFunction.params) {
        this.selectedFunction.params.forEach(param => {
          if (param.defaultValue !== null && param.defaultValue !== undefined) {
            this.$set(this.functionParams, param.paramName, param.defaultValue)
          }
        })
      }
      
      // 清空预览
      this.previewInputValue = ''
      this.previewOutputValue = null
    },
    
    // 预览清洗函数效果
    previewCleanseFunction() {
      if (!this.selectedFunctionCode || !this.previewInputValue) {
        this.previewOutputValue = null
        return
      }
      
      this.$axios.post('/v1/transform/cleanse/preview', {
        functionCode: this.selectedFunctionCode,
        value: this.previewInputValue,
        params: this.functionParams
      }).then(res => {
        if (res.data.success) {
          this.previewOutputValue = res.data.result
        } else {
          this.$message.error('预览失败: ' + res.data.error)
        }
      }).catch(err => {
        console.error('预览失败', err)
      })
    },
    
    // 确认添加清洗函数
    confirmAddCleanseFunction() {
      if (!this.selectedFunctionCode) {
        this.$message.warning('请选择函数')
        return
      }
      
      // 检查必填参数
      if (this.selectedFunction && this.selectedFunction.params) {
        for (const param of this.selectedFunction.params) {
          if (param.required && !this.functionParams[param.paramName]) {
            this.$message.warning(`请填写必填参数: ${param.paramLabel}`)
            return
          }
        }
      }
      
      const functionData = {
        functionCode: this.selectedFunctionCode,
        params: { ...this.functionParams }
      }
      
      // 检查是否是添加到新处理器
      if (this.isAddingToNewProcessor) {
        // 添加到新处理器
        if (!this.newProcessor.config.cleanseFunctions) {
          this.$set(this.newProcessor.config, 'cleanseFunctions', [])
        }
        this.newProcessor.config.cleanseFunctions.push(functionData)
        this.isAddingToNewProcessor = false
      } else {
        // 添加到现有mapping
        if (!this.currentMapping._cleanseFunctions) {
          this.$set(this.currentMapping, '_cleanseFunctions', [])
        }
        this.currentMapping._cleanseFunctions.push(functionData)
      }
      
      this.addCleanseFunctionDialogVisible = false
      this.$message.success('清洗函数已添加')
    },
    
    // 移除清洗函数
    removeCleanseFunction(index) {
      this.currentMapping._cleanseFunctions.splice(index, 1)
      this.$message.success('已移除')
    },
    
    // 上移函数
    moveFunctionUp(index) {
      if (index === 0) return
      const temp = this.currentMapping._cleanseFunctions[index]
      this.$set(this.currentMapping._cleanseFunctions, index, this.currentMapping._cleanseFunctions[index - 1])
      this.$set(this.currentMapping._cleanseFunctions, index - 1, temp)
    },
    
    // 下移函数
    moveFunctionDown(index) {
      if (index === this.currentMapping._cleanseFunctions.length - 1) return
      const temp = this.currentMapping._cleanseFunctions[index]
      this.$set(this.currentMapping._cleanseFunctions, index, this.currentMapping._cleanseFunctions[index + 1])
      this.$set(this.currentMapping._cleanseFunctions, index + 1, temp)
    },
    
    // 获取函数名称
    getFunctionName(functionCode) {
      const func = this.cleanseFunctionList.find(f => f.functionCode === functionCode)
      return func ? func.functionName : functionCode
    },
    
    // 获取清洗函数名称（别名）
    getCleanseFunctionName(functionCode) {
      return this.getFunctionName(functionCode)
    },
    
    // 为新处理器添加清洗函数
    showAddCleanseToNewProcessor() {
      // 保存当前的添加流程状态
      this.isAddingToNewProcessor = true
      
      // 初始化cleanseFunctions数组
      if (!this.newProcessor.config.cleanseFunctions) {
        this.$set(this.newProcessor.config, 'cleanseFunctions', [])
      }
      
      // 打开添加清洗函数对话框
      this.showAddCleanseFunctionDialog()
    },
    
    // 删除新处理器的清洗函数
    removeNewProcessorCleanseFunction(index) {
      if (this.newProcessor.config.cleanseFunctions) {
        this.newProcessor.config.cleanseFunctions.splice(index, 1)
      }
    },
    
    // 显示验证配置对话框
    showValidationConfig(mapping) {
      this.advancedConfigMapping = mapping
      this.advancedActiveTab = 'validation'
      this.advancedConfigDialogVisible = true
    },
    
    // 显示转换配置对话框
    showTransformConfig(mapping) {
      this.advancedConfigMapping = mapping
      // 若为字典映射，确保加载字典类型列表
      if (mapping.transformType === 'DICT' && mapping.dictMappingId) {
        this.loadDictTypesForMapping(mapping)
      }
      this.advancedConfigDialogVisible = true
    },
    
    // 保存高级配置
    saveAdvancedConfig() {
      // 如果是通过处理器配置按钮打开的，需要将配置保存到processor.config中
      if (this.currentConfigProcessor && this.currentMappingForProcessor) {
        const mapping = this.currentMappingForProcessor
        const processor = this.currentConfigProcessor
        
        // 将mapping的配置同步到processor.config中
        processor.config.transformType = mapping.transformType
        processor.config.transformScript = mapping.transformScript
        processor.config.dictMappingId = mapping.dictMappingId
        processor.config.dictSourceTypeValue = mapping.dictSourceTypeValue
        processor.config.dictOutputMode = mapping.dictOutputMode
        processor.config.constantValue = mapping.constantValue
        processor.config.defaultValue = mapping.defaultValue
        
        this.currentConfigProcessor = null
        this.currentMappingForProcessor = null
      }
      
      this.advancedConfigDialogVisible = false
      this.$message.success('配置已保存')
    },
    
    // ========== 处理器管理方法 ==========
    
    // 显示添加处理器对话框
    showAddProcessorDialog(mapping) {
      this.currentMappingForProcessor = mapping
      
      // 重置新处理器配置
      // 选择第一个未被禁用的类型
      let defaultType = 'NULL_HANDLE'
      if (this.isProcessorTypeDisabled('NULL_HANDLE')) {
        if (!this.isProcessorTypeDisabled('CLEANSE')) {
          defaultType = 'CLEANSE'
        } else if (!this.isProcessorTypeDisabled('TRANSFORM')) {
          defaultType = 'TRANSFORM'
        }
      }
      
      this.newProcessor = {
        type: defaultType,
        config: {
          strategy: 'KEEP',
          defaultValue: '',
          functions: [],
          cleanseFunctions: [], // 清洗函数数组
          transformType: 'DIRECT',
          transformFunction: '',
          dictType: '',
          constantValue: '',
          dictMappingId: null,
          dictSourceTypeValue: '',
          dictOutputMode: 'TARGET_KEY',
          _dictTypeList: []
        }
      }
      this.addProcessorDialogVisible = true
    },
    
    // 判断处理器类型是否已禁用
    isProcessorTypeDisabled(type) {
      if (!this.currentMappingForProcessor || !this.currentMappingForProcessor._processors) {
        return false
      }
      // 检查是否已存在该类型的处理器
      return this.currentMappingForProcessor._processors.some(p => p.type === type)
    },
    
    // 处理器类型变化
    handleProcessorTypeChange() {
      // 重置配置
      this.newProcessor.config = {
        strategy: 'KEEP',
        defaultValue: '',
        functions: [],
        cleanseFunctions: [], // 清洗函数数组
        transformType: 'DIRECT',
        transformFunction: '',
        dictType: '',
        constantValue: '',
        dictMappingId: null,
        dictSourceTypeValue: '',
        dictOutputMode: 'TARGET_KEY',
        _dictTypeList: []
      }
    },
    
    // 处理新处理器的字典变化
    handleNewProcessorDictChange(dictMappingId) {
      if (!dictMappingId) {
        this.newProcessor.config._dictTypeList = []
        return
      }
      // 加载字典类型列表
      this.$axios.get(`/v1/dict-mapping/${dictMappingId}/types`).then(res => {
        const types = res.data || []
        const typeList = (types || []).map(t => ({
          value: String(t.value),
          label: t.label || String(t.value)
        }))
        this.$set(this.newProcessor.config, '_dictTypeList', typeList)
        // 如果只有一个类型，自动选中
        if (typeList.length === 1) {
          this.newProcessor.config.dictSourceTypeValue = typeList[0].value
        }
      }).catch(err => {
        this.$set(this.newProcessor.config, '_dictTypeList', [])
      })
    },

    // 加载映射的字典类型列表（用于编辑/配置时）
    loadDictTypesForMapping(mapping) {
      const mappingId = mapping.dictMappingId
      if (!mappingId) {
        this.$set(mapping, '_dictTypeList', [])
        return
      }
      this.$axios.get(`/v1/dict-mapping/${mappingId}/types`).then(res => {
        const types = res.data || []
        const typeList = (types || []).map(t => ({
          value: String(t.value),
          label: t.label || String(t.value)
        }))
        this.$set(mapping, '_dictTypeList', typeList)
        if (typeList.length === 1) {
          this.$set(mapping, 'dictSourceTypeValue', typeList[0].value)
        }
      }).catch(() => {
        this.$set(mapping, '_dictTypeList', [])
      })
    },
    
    // 为新处理器打开插件流程配置
    openPluginFlowForNewProcessor() {
      // 临时创建一个mapping对象用于插件流程配置
      const tempMapping = {
        sourceField: this.currentMappingForProcessor.sourceField || '源字段',
        targetField: this.currentMappingForProcessor.targetField || '目标字段',
        cleanseFunctions: this.newProcessor.config.cleanseFunctions && this.newProcessor.config.cleanseFunctions.length > 0 
          ? JSON.stringify(this.newProcessor.config.cleanseFunctions)
          : null,
        _isNewProcessor: true // 标记这是为新处理器配置
      }
      
      this.pluginFlowMapping = tempMapping
      this.pluginFlowDialogVisible = true
    },
    
   // 确认添加处理器
    confirmAddProcessor() {
      const mapping = this.currentMappingForProcessor
      if (!mapping) return
      
      console.log('验证处理器配置:', {
        type: this.newProcessor.type,
        strategy: this.newProcessor.config.strategy,
        defaultValue: this.newProcessor.config.defaultValue
      })
      
      // 验证配置完整性
      if (this.newProcessor.type === 'NULL_HANDLE') {
        // 空值处理：如果选择了DEFAULT策略，默认值为必填
        if (this.newProcessor.config.strategy === 'DEFAULT') {
          if (!this.newProcessor.config.defaultValue || this.newProcessor.config.defaultValue.trim() === '') {
            this.$message.warning('请填写默认值')
            return
          }
        }
      } else if (this.newProcessor.type === 'CLEANSE') {
        // 数据清洗：必须添加至少一个清洗函数
        if (!this.newProcessor.config.cleanseFunctions || this.newProcessor.config.cleanseFunctions.length === 0) {
          this.$message.warning('请至少添加一个清洗函数')
          return
        }
      } else if (this.newProcessor.type === 'TRANSFORM') {
        // 数据转换：验证转换类型配置
        if (this.newProcessor.config.transformType === 'SCRIPT') {
          // 脚本转换：必须填写脚本
          if (!this.newProcessor.config.transformScript || this.newProcessor.config.transformScript.trim() === '') {
            this.$message.warning('请填写转换脚本')
            return
          }
        } else if (this.newProcessor.config.transformType === 'DICT') {
          // 字典映射：必须选择字典
          if (!this.newProcessor.config.dictMappingId) {
            this.$message.warning('请选择字典')
            return
          }
        } else if (this.newProcessor.config.transformType === 'CONSTANT') {
          // 固定值：必须填写值
          if (this.newProcessor.config.constantValue === undefined || this.newProcessor.config.constantValue === null || this.newProcessor.config.constantValue === '') {
            this.$message.warning('请填写固定值')
            return
          }
        }
      }
      
      // 验证通过，执行添加
      this.doAddProcessor(mapping)
    },
    
    // 执行添加处理器
    doAddProcessor(mapping) {
      // 调试：打印配置信息
      console.log('添加处理器配置:', {
        type: this.newProcessor.type,
        config: JSON.parse(JSON.stringify(this.newProcessor.config))
      })
      
      // 初始化_processors数组
      if (!mapping._processors) {
        this.$set(mapping, '_processors', [])
      }
      
      // 如果是TRANSFORM类型，需要同步transformType到mapping中
      if (this.newProcessor.type === 'TRANSFORM') {
        // 将配置同步到mapping中，以便后续使用
        this.$set(mapping, 'transformType', this.newProcessor.config.transformType || 'DIRECT')
        if (this.newProcessor.config.transformType === 'DICT') {
          this.$set(mapping, 'dictMappingId', this.newProcessor.config.dictMappingId)
          this.$set(mapping, 'dictSourceTypeValue', this.newProcessor.config.dictSourceTypeValue || '')
          this.$set(mapping, 'dictOutputMode', this.newProcessor.config.dictOutputMode || 'TARGET_KEY')
          this.$set(mapping, 'defaultValue', this.newProcessor.config.defaultValue || '')
        } else if (this.newProcessor.config.transformType === 'CONSTANT') {
          this.$set(mapping, 'constantValue', this.newProcessor.config.constantValue || '')
        } else if (this.newProcessor.config.transformType === 'SCRIPT') {
          this.$set(mapping, 'transformScript', this.newProcessor.config.transformScript || '')
        }
      } else if (this.newProcessor.type === 'NULL_HANDLE') {
        // 空值处理同步到mapping
        this.$set(mapping, 'nullStrategy', this.newProcessor.config.strategy || 'KEEP')
        if (this.newProcessor.config.strategy === 'DEFAULT') {
          this.$set(mapping, 'defaultValue', this.newProcessor.config.defaultValue || '')
        }
      } else if (this.newProcessor.type === 'CLEANSE') {
        // 清洗函数同步到mapping
        if (this.newProcessor.config.cleanseFunctions && this.newProcessor.config.cleanseFunctions.length > 0) {
          this.$set(mapping, 'cleanseFunctions', JSON.stringify(this.newProcessor.config.cleanseFunctions))
        }
      }
      
      // 添加新处理器
      const newOrder = mapping._processors.length + 1
      mapping._processors.push({
        type: this.newProcessor.type,
        order: newOrder,
        config: JSON.parse(JSON.stringify(this.newProcessor.config))
      })
      
      this.addProcessorDialogVisible = false
      this.$message.success('处理步骤已添加')
    },
    
    // 配置处理器
    configureProcessor(mapping, processor, index) {
      this.currentProcessorIndex = index
      this.currentMappingForProcessor = mapping
      
      if (processor.type === 'NULL_HANDLE') {
        // 空值处理：打开配置对话框
        this.editingNullHandleProcessor = processor
        this.configNullHandleDialogVisible = true
      } else if (processor.type === 'CLEANSE') {
        // 保存当前正在配置的处理器
        this.currentConfigProcessor = processor
        
        // 创建临时mapping对象，将processor.config.cleanseFunctions转成JSON字符串
        const tempMapping = {
          sourceField: mapping.sourceField || '源字段',
          targetField: mapping.targetField || '目标字段',
          cleanseFunctions: processor.config.cleanseFunctions && processor.config.cleanseFunctions.length > 0
            ? JSON.stringify(processor.config.cleanseFunctions)
            : null,
          _isEditingProcessor: true // 标记这是编辑已有处理器
        }
        
        // 打开清洗插件流程配置对话框
        this.pluginFlowMapping = tempMapping
        this.pluginFlowDialogVisible = true
      } else if (processor.type === 'TRANSFORM') {
        // 为TRANSFORM处理器，需要针对当前处理器配置转换类型
        // 将处理器的config信息同步到mapping中，以便高级配置对话框使用
        if (processor.config && processor.config.transformType) {
          this.$set(mapping, 'transformType', processor.config.transformType)
          if (processor.config.transformType === 'DICT') {
            this.$set(mapping, 'dictMappingId', processor.config.dictMappingId)
            this.$set(mapping, 'dictSourceTypeValue', processor.config.dictSourceTypeValue || '')
            this.$set(mapping, 'dictOutputMode', processor.config.dictOutputMode || 'TARGET_KEY')
            this.$set(mapping, 'defaultValue', processor.config.defaultValue || '')
          } else if (processor.config.transformType === 'CONSTANT') {
            this.$set(mapping, 'constantValue', processor.config.constantValue || '')
          } else if (processor.config.transformType === 'SCRIPT') {
            this.$set(mapping, 'transformScript', processor.config.transformScript || '')
          }
        }
        this.currentConfigProcessor = processor
        this.advancedConfigMapping = mapping
        // 若为字典映射，加载字典类型列表以供选择
        if ((mapping.transformType || (processor.config && processor.config.transformType)) === 'DICT' && (mapping.dictMappingId || processor.config.dictMappingId)) {
          // 同步字典ID到mapping后加载类型
          if (processor.config && processor.config.dictMappingId) {
            this.$set(mapping, 'dictMappingId', processor.config.dictMappingId)
          }
          this.loadDictTypesForMapping(mapping)
        }
        this.advancedConfigDialogVisible = true
      }
    },
    
    // 保存空值处理配置
    saveNullHandleConfig() {
      if (!this.editingNullHandleProcessor) return
      
      const mapping = this.currentMappingForProcessor
      const processor = this.editingNullHandleProcessor
      
      // 验证：如果选择了DEFAULT策略，必须填写默认值
      if (processor.config.strategy === 'DEFAULT') {
        if (!processor.config.defaultValue || processor.config.defaultValue.trim() === '') {
          this.$message.warning('请填写默认值')
          return
        }
      }
      
      // 同步到mapping
      this.$set(mapping, 'nullStrategy', processor.config.strategy || 'KEEP')
      if (processor.config.strategy === 'DEFAULT') {
        this.$set(mapping, 'defaultValue', processor.config.defaultValue || '')
      }
      
      this.configNullHandleDialogVisible = false
      this.editingNullHandleProcessor = null
      this.$message.success('配置已保存')
    },
    
    // 删除处理器
    removeProcessor(mapping, index) {
      this.$confirm('确认删除该处理步骤？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        mapping._processors.splice(index, 1)
        // 重新排序order
        mapping._processors.forEach((p, i) => {
          p.order = i + 1
        })
        this.$message.success('已删除')
      }).catch(() => {})
    },
    
    // 拖拽开始
    handleDragStart(mapping, index, event) {
      this.draggedProcessorIndex = index
      this.draggedMappingRef = mapping
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/html', event.target.innerHTML)
    },
    
    // 拖拽进入
    handleDragEnter(event) {
      event.currentTarget.style.borderColor = '#409EFF'
      event.currentTarget.style.background = '#e6f7ff'
    },
    
    // 拖拽离开
    handleDragLeave(event) {
      event.currentTarget.style.borderColor = ''
      event.currentTarget.style.background = ''
    },
    
    // 拖拽放下
    handleDrop(mapping, dropIndex, event) {
      event.preventDefault()
      
      // 恢复样式
      event.currentTarget.style.borderColor = ''
      event.currentTarget.style.background = ''
      
      if (this.draggedMappingRef !== mapping || this.draggedProcessorIndex === dropIndex) {
        return
      }
      
      const dragIndex = this.draggedProcessorIndex
      const processors = mapping._processors
      
      // 移动元素
      const draggedItem = processors.splice(dragIndex, 1)[0]
      processors.splice(dropIndex, 0, draggedItem)
      
      // 重新排序order
      processors.forEach((p, i) => {
        p.order = i + 1
      })
      
      // 重置拖拽状态
      this.draggedProcessorIndex = null
      this.draggedMappingRef = null
      
      this.$message.success('顺序已调整')
    },
    
    // 获取处理器颜色
    getProcessorColor(type) {
      const colors = {
        'NULL_HANDLE': '#909399',
        'CLEANSE': '#67c23a',
        'TRANSFORM': '#409eff'
      }
      return colors[type] || '#909399'
    },
    
    // 获取处理器标签类型
    getProcessorTagType(type) {
      const tags = {
        'NULL_HANDLE': 'info',
        'CLEANSE': 'success',
        'TRANSFORM': 'primary'
      }
      return tags[type] || 'info'
    },
    
    // 获取处理器图标
    getProcessorIcon(type) {
      const icons = {
        'NULL_HANDLE': 'el-icon-close',
        'CLEANSE': 'el-icon-brush',
        'TRANSFORM': 'el-icon-refresh'
      }
      return icons[type] || 'el-icon-setting'
    },
    
    // 获取处理器类型名称
    getProcessorTypeName(type) {
      const names = {
        'NULL_HANDLE': '空值处理',
        'CLEANSE': '数据清洗',
        'TRANSFORM': '数据转换'
      }
      return names[type] || '未知类型'
    },
    
    // 获取处理器子类型文本（显示在标签上）
    getProcessorSubtypeText(processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE' && config && config.strategy) {
        const strategyMap = {
          'KEEP': '',
          'DEFAULT': '(默认值)',
          'SKIP': '(跳过)'
        }
        return strategyMap[config.strategy] || ''
      } else if (type === 'TRANSFORM' && config && config.transformType) {
        const typeMap = {
          'DIRECT': '(直接)',
          'DICT': '(字典)',
          'CONSTANT': '(固定值)'
        }
        return typeMap[config.transformType] || ''
      }
      
      return ''
    },
    
    // 获取处理器子类型文本（带mapping上下文）
    getProcessorSubtypeTextWithMapping(mapping, processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE' && config && config.strategy) {
        const strategyMap = {
          'KEEP': '',
          'DEFAULT': '(默认值)',
          'SKIP': '(跳过)'
        }
        return strategyMap[config.strategy] || ''
      } else if (type === 'CLEANSE') {
        // 显示清洗函数数量，优先从processor.config读取
        let functions = null
        
        // 优先从 processor.config.cleanseFunctions 读取
        if (config && config.cleanseFunctions && Array.isArray(config.cleanseFunctions)) {
          functions = config.cleanseFunctions
        } else if (mapping.cleanseFunctions) {
          // 其次从 mapping.cleanseFunctions 读取
          try {
            functions = JSON.parse(mapping.cleanseFunctions)
          } catch (e) {
            // ignore
          }
        }
        
        if (functions && Array.isArray(functions) && functions.length > 0) {
          return '(' + functions.length + '个函数)'
        }
        return ''
      } else if (type === 'TRANSFORM' && config && config.transformType) {
        const typeMap = {
          'DIRECT': '(直接)',
          'DICT': '(字典)',
          'CONSTANT': '(固定值)'
        }
        return typeMap[config.transformType] || ''
      }
      
      return ''
    },
    
    // 获取处理器详细文本（显示在tooltip中）
    getProcessorDetailText(processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE') {
        if (config.strategy === 'KEEP') {
          return '空值处理：保持空值'
        } else if (config.strategy === 'DEFAULT') {
          return '空值处理：设置为 ' + (config.defaultValue || '(未配置)')
        } else if (config.strategy === 'SKIP') {
          return '空值处理：跳过该行'
        }
        return '空值处理：未配置'
      } else if (type === 'CLEANSE') {
        return '数据清洗：点击查看清洗函数链'
      } else if (type === 'TRANSFORM') {
        if (config && config.transformType) {
          if (config.transformType === 'DIRECT') {
            return '数据转换：直接映射'
          } else if (config.transformType === 'DICT') {
            return '数据转换：字典映射' + (config.dictMappingId ? ' (ID:' + config.dictMappingId + ')' : '')
          } else if (config.transformType === 'SCRIPT') {
            return '数据转换：脚本转换'
          } else if (config.transformType === 'CONSTANT') {
            return '数据转换：固定值 ' + (config.constantValue || '(未配置)')
          }
          return '数据转换：未配置'
        }
        return '数据转换：点击配置转换类型'
      }
      
      return '未配置'
    },
    
    // 获取处理器详细文本（带mapping上下文）
    getProcessorDetailTextWithMapping(mapping, processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE') {
        if (config.strategy === 'KEEP') {
          return '空值处理：保持空值'
        } else if (config.strategy === 'DEFAULT') {
          return '空值处理：设置为 ' + (config.defaultValue || '(未配置)')
        } else if (config.strategy === 'SKIP') {
          return '空值处理：跳过该行'
        }
        return '空值处理：未配置'
      } else if (type === 'CLEANSE') {
        // 显示清洗函数详情，优先从processor.config读取
        let functions = null
        
        // 优先从 processor.config.cleanseFunctions 读取
        if (config && config.cleanseFunctions && Array.isArray(config.cleanseFunctions)) {
          functions = config.cleanseFunctions
        } else if (mapping.cleanseFunctions) {
          // 其次从 mapping.cleanseFunctions 读取
          try {
            functions = JSON.parse(mapping.cleanseFunctions)
          } catch (e) {
            // ignore
          }
        }
        
        if (functions && Array.isArray(functions) && functions.length > 0) {
          const functionNames = functions.map((f, idx) => {
            const funcDef = this.cleanseFunctionList.find(cf => cf.functionCode === f.functionCode)
            return (idx + 1) + '. ' + (funcDef ? funcDef.functionName : f.functionCode)
          }).join(', ')
          return '数据清洗 (' + functions.length + '个函数): ' + functionNames
        }
        return '数据清洗：未配置清洗函数，点击添加'
      } else if (type === 'TRANSFORM') {
        if (config && config.transformType) {
          if (config.transformType === 'DIRECT') {
            return '数据转换：直接映射'
          } else if (config.transformType === 'DICT') {
            return '数据转换：字典映射' + (config.dictMappingId ? ' (ID:' + config.dictMappingId + ')' : '')
          } else if (config.transformType === 'SCRIPT') {
            return '数据转换：脚本转换'
          } else if (config.transformType === 'CONSTANT') {
            return '数据转换：固定值 ' + (config.constantValue || '(未配置)')
          }
          return '数据转换：未配置'
        }
        return '数据转换：点击配置转换类型'
      }
      
      return '未配置'
    },
    
    // 获取处理器描述
    getProcessorDescription(processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE') {
        const strategyMap = {
          'KEEP': '保持空值',
          'DEFAULT': `设置为: ${config.defaultValue || '(未配置)'}`,
          'SKIP': '跳过该行'
        }
        return strategyMap[config.strategy] || '未配置'
      } else if (type === 'CLEANSE') {
        const count = (config.functions || []).length
        return count > 0 ? `${count}个清洗函数` : '未配置清洗函数'
      } else if (type === 'TRANSFORM') {
        const typeMap = {
          'DIRECT': '直接映射',
          'DICT': '字典映射',
          'SCRIPT': '脚本映射',
          'CONSTANT': '固定值'
        }
        return typeMap[config.transformType] || '未配置'
      }
      
      return '未配置'
    },
    
    // 实际获取处理器描述（需要mapping上下文）
    getProcessorActualDescription(mapping, processor) {
      const { type, config } = processor
      
      if (type === 'NULL_HANDLE') {
        const strategyMap = {
          'KEEP': '保持空值',
          'DEFAULT': `设置为: ${config.defaultValue || '(未配置)'}`,
          'SKIP': '跳过该行'
        }
        return strategyMap[config.strategy] || '未配置'
      } else if (type === 'CLEANSE') {
        // 从 mapping.cleanseFunctions 获取数量
        if (!mapping.cleanseFunctions) {
          return '未配置清洗函数'
        }
        try {
          const functions = JSON.parse(mapping.cleanseFunctions)
          const count = Array.isArray(functions) ? functions.length : 0
          return count > 0 ? `${count}个清洗函数` : '未配置清洗函数'
        } catch (e) {
          return '未配置清洗函数'
        }
      } else if (type === 'TRANSFORM') {
        // 从 processor.config.transformType 获取类型（每个处理器独立配置）
        const transformType = config.transformType || mapping.transformType || 'DIRECT'
        const typeMap = {
          'DIRECT': '直接映射',
          'DICT': '字典映射',
          'SCRIPT': '脚本映射',
          'CONSTANT': '固定值'
        }
        return typeMap[transformType] || '直接映射'
      }
      
      return '未配置'
    },
    
    // 判断处理器是否已配置
    isProcessorConfigured(processor) {
      const { type } = processor
      
      if (type === 'NULL_HANDLE') {
        // 空值处理默认就是已配置的（KEEP策略）
        return true
      } else if (type === 'CLEANSE') {
        // 清洗需要查看mapping的cleanseFunctions字段
        // 注意：这里需要通过currentMappingForProcessor来获取mapping
        // 但是由于是在模板中调用，我们需要传入mapping参数
        // 这里暂时返回true，在模板中直接检查
        return true
      } else if (type === 'TRANSFORM') {
        // 转换需要查看mapping的transformType
        return true
      }
      
      return false
    },
    
    // 实际检查处理器配置状态（需要mapping上下文）
    isProcessorActuallyConfigured(mapping, processor) {
      const { type } = processor
      
      if (type === 'NULL_HANDLE') {
        // 空值处理默认就是已配置的（KEEP策略）
        return true
      } else if (type === 'CLEANSE') {
        // 清洗需要检查mapping的cleanseFunctions字段
        if (!mapping.cleanseFunctions) {
          return false
        }
        try {
          const functions = JSON.parse(mapping.cleanseFunctions)
          return Array.isArray(functions) && functions.length > 0
        } catch (e) {
          return false
        }
      } else if (type === 'TRANSFORM') {
        // 转换需要检查mapping的transformType
        return mapping.transformType && mapping.transformType !== 'DIRECT'
      }
      
      return false
    },
    
    // 获取转换类型标签
    getTransformTypeTag(type) {
      const tags = {
        'DIRECT': '',
        'DICT': 'warning',
        'CONSTANT': 'info'
      }
      return tags[type] || ''
    },
    
    // 转换类型变化
    handleTransformTypeChange(mapping) {
      // 清空旧配置
      this.$set(mapping, '_funcPreset', '')
      this.$set(mapping, '_funcScale', 2)
      this.$set(mapping, '_funcNumber', 0)
      this.$set(mapping, '_funcPattern', 'yyyy-MM-dd')
      this.$set(mapping, '_funcDelimiter', ',')
      this.$set(mapping, '_funcIndex', 0)
      this.$set(mapping, '_funcText', '')
      this.$set(mapping, '_funcConcatText', '')
      this.$set(mapping, 'transformFunction', '')
      this.$set(mapping, 'transformScript', '')
      this.$set(mapping, 'constantValue', '')
      this.$set(mapping, 'dictMappingId', null)
      this.$set(mapping, 'dictSourceTypeValue', '')
      this.$set(mapping, 'defaultValue', '')
    },
    
    // ========== 插件流程配置 ==========
    
    // 打开插件流程配置
    openPluginFlow(mapping) {
      this.pluginFlowMapping = mapping
      this.pluginFlowDialogVisible = true
    },
    
    // 插件流程保存
    handlePluginFlowSave(mapping) {
      console.log('插件流程配置已保存:', mapping)
      
      // 检查是否是为新处理器配置
      if (mapping._isNewProcessor) {
        // 为新处理器配置，保存到 newProcessor.config.cleanseFunctions
        if (mapping.cleanseFunctions) {
          try {
            const functions = JSON.parse(mapping.cleanseFunctions)
            this.$set(this.newProcessor.config, 'cleanseFunctions', functions)
            this.$message.success('清洗插件链配置已保存，共 ' + functions.length + ' 个插件')
          } catch (e) {
            this.$message.error('配置保存失败')
          }
        } else {
          this.$set(this.newProcessor.config, 'cleanseFunctions', [])
          this.$message.warning('未配置任何插件')
        }
      } else if (this.currentConfigProcessor && this.currentConfigProcessor.type === 'CLEANSE') {
        // 为现有处理器配置，保存到 processor.config.cleanseFunctions
        if (mapping.cleanseFunctions) {
          try {
            const functions = JSON.parse(mapping.cleanseFunctions)
            this.$set(this.currentConfigProcessor.config, 'cleanseFunctions', functions)
            console.log('已同步到processor.config:', this.currentConfigProcessor.config)
            this.$message.success('清洗插件链配置已保存，共 ' + functions.length + ' 个插件')
          } catch (e) {
            this.$message.error('配置保存失败')
          }
        } else {
          this.$set(this.currentConfigProcessor.config, 'cleanseFunctions', [])
          this.$message.warning('未配置任何插件')
        }
        // 清空引用
        this.currentConfigProcessor = null
      } else if (mapping._isEditingProcessor) {
        // 兼容处理：如果标记为_isEditingProcessor，但currentConfigProcessor为空
        // 这是不应该发生的，但为了容错，给予提示
        this.$message.warning('配置保存异常，请重新配置')
      } else {
        // 为现有mapping配置（兼容旧逻辑）
        this.$message.success('清洗插件链配置已保存')
      }
    },
    
    // 数据脱敏: 更新脱敏配置
    updateDesensitizeConfig(mapping) {
      if (!mapping) {
        mapping = this.currentMapping
      }
      
      if (!mapping._desensitizeType || mapping._desensitizeType === 'NONE') {
        mapping.desensitizeConfig = ''
        return
      }
      
      const config = {
        type: mapping._desensitizeType,
        strategy: mapping._desensitizeStrategy || 'PARTIAL'
      }
      
      // 自定义脱敏配置
      if (mapping._desensitizeType === 'CUSTOM') {
        if (mapping._desensitizePattern) {
          config.pattern = mapping._desensitizePattern
        }
        if (mapping._desensitizeMaskChar) {
          config.maskChar = mapping._desensitizeMaskChar
        }
        config.keepStart = mapping._desensitizeKeepStart || 0
        config.keepEnd = mapping._desensitizeKeepEnd || 0
      }
      
      mapping.desensitizeConfig = JSON.stringify(config)
    },
    
    // 脱敏示例
    getDesensitizeExample(value) {
      if (!this.currentMapping || !this.currentMapping._desensitizeType || this.currentMapping._desensitizeType === 'NONE') {
        return value
      }
      
      const type = this.currentMapping._desensitizeType
      const strategy = this.currentMapping._desensitizeStrategy || 'PARTIAL'
      
      switch (type) {
        case 'PHONE':
          if (strategy === 'FULL') return '***********'
          if (strategy === 'PARTIAL' || strategy === 'MASK') return '138****1234'
          break
        case 'ID_CARD':
          if (strategy === 'FULL') return '******************'
          if (strategy === 'PARTIAL' || strategy === 'MASK') return '110101********1234'
          break
        case 'BANK_CARD':
          if (strategy === 'FULL') return '********************'
          if (strategy === 'PARTIAL' || strategy === 'MASK') return '6222 **** **** 1234'
          break
        case 'EMAIL':
          if (strategy === 'FULL') return '***@***.***'
          if (strategy === 'PARTIAL' || strategy === 'MASK') return 'u***@example.com'
          break
        case 'NAME':
          if (strategy === 'FULL') return '***'
          if (strategy === 'PARTIAL' || strategy === 'MASK') return '张**'
          break
        default:
          return value
      }
      return value
    },
    
    // 数据血缘: 显示对话框
    showLineageDialog() {
      this.lineageDialogVisible = true
      // 如果当前任务有源连接器，默认选中
      if (this.sourceConfig.connectorId) {
        this.lineageConfig.connectorId = this.sourceConfig.connectorId
        // 主动加载表列表
        this.$nextTick(() => {
          this.loadLineageTables()
        })
      }
    },
    
    // 数据血缘: 加载表列表
    loadLineageTables() {
      if (!this.lineageConfig.connectorId) return
      
      this.$axios.get(`/v1/task/connector/${this.lineageConfig.connectorId}/tables`).then(res => {
        this.lineageTableList = res.data || []
        if (this.lineageTableList.length > 0) {
          this.$message.success(`已加载 ${this.lineageTableList.length} 个表`)
        }
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('加载表列表失败: ' + errMsg)
      })
    },
    
    // 数据血缘: 加载字段列表
    loadLineageFields() {
      if (!this.lineageConfig.connectorId || !this.lineageConfig.tableName) return
      
      this.$axios.get(`/v1/dict-source/connector/${this.lineageConfig.connectorId}/table/${this.lineageConfig.tableName}/columns`).then(res => {
        this.lineageFieldList = res.data || []
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('加载字段列表失败: ' + errMsg)
      })
    },
    
    // 数据血缘: 查询血缘关系
    queryLineage() {
      if (!this.lineageConfig.connectorId || !this.lineageConfig.tableName || !this.lineageConfig.fieldName) {
        this.$message.warning('请选择数据源、表名和字段名')
        return
      }
      
      this.lineageLoading = true
      this.$axios.post('/v1/data-lineage/graph', {
        connectorId: this.lineageConfig.connectorId,
        tableName: this.lineageConfig.tableName,
        fieldName: this.lineageConfig.fieldName
      }).then(res => {
        this.lineageGraphData = res.data
        this.$message.success('血缘查询成功')
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('查询失败: ' + errMsg)
      }).finally(() => {
        this.lineageLoading = false
      })
    },
    
    // 数据血缘: 导出图谱
    exportLineageGraph() {
      if (!this.lineageGraphData) return
      
      const dataStr = JSON.stringify(this.lineageGraphData, null, 2)
      const blob = new Blob([dataStr], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `lineage_${this.lineageConfig.tableName}_${this.lineageConfig.fieldName}.json`
      link.click()
      URL.revokeObjectURL(url)
      this.$message.success('导出成功')
    },
    
    // 数据对比: 显示对话框
    showCompareDialog() {
      this.compareDialogVisible = true
      // 默认使用当前任务的源和目标连接器
      if (this.sourceConfig.connectorId) {
        this.compareConfig.sourceConnectorId = this.sourceConfig.connectorId
        if (this.sourceConfig.sql) {
          this.compareConfig.sourceTableOrSql = this.sourceConfig.sql
        } else if (this.sourceConfig.tableName) {
          this.compareConfig.sourceTableOrSql = this.sourceConfig.tableName
        }
      }
      if (this.targetConfig.connectorId) {
        this.compareConfig.targetConnectorId = this.targetConfig.connectorId
        if (this.targetConfig.tableName) {
          this.compareConfig.targetTableOrSql = this.targetConfig.tableName
        }
      }
    },
    
    // 数据对比: 执行对比
    executeCompare() {
      if (!this.compareConfig.sourceConnectorId || !this.compareConfig.targetConnectorId) {
        this.$message.warning('请选择源和目标连接器')
        return
      }
      if (!this.compareConfig.sourceTableOrSql || !this.compareConfig.targetTableOrSql) {
        this.$message.warning('请输入源和目标表名或SQL')
        return
      }
      if (!this.compareConfig.keyFields) {
        this.$message.warning('请输入主键字段')
        return
      }
      
      this.compareLoading = true
      this.$axios.post('/v1/data-compare/compare', {
        sourceConnectorId: this.compareConfig.sourceConnectorId,
        sourceTableOrSql: this.compareConfig.sourceTableOrSql,
        targetConnectorId: this.compareConfig.targetConnectorId,
        targetTableOrSql: this.compareConfig.targetTableOrSql,
        compareFields: this.compareConfig.compareFields ? this.compareConfig.compareFields.split(',').map(f => f.trim()) : null,
        keyFields: this.compareConfig.keyFields.split(',').map(f => f.trim())
      }).then(res => {
        this.compareResult = res.data
        this.$message.success('对比完成')
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('对比失败: ' + errMsg)
      }).finally(() => {
        this.compareLoading = false
      })
    },
    
    // 处理字典映射变化，加载字典类型列表
    handleDictMappingChange(row) {
      if (!row || !row.dictMappingId) {
        if (row) {
          row._dictTypeList = []
          row.dictSourceTypeValue = ''
        }
        return
      }
      this.$axios.get(`/v1/dict-mapping/${row.dictMappingId}/types`).then(res => {
        const types = res.data || []
        row._dictTypeList = (types || []).map(t => ({
          value: String(t.value),
          label: t.label || String(t.value)
        }))
        if (row._dictTypeList.length === 1) {
          row.dictSourceTypeValue = row._dictTypeList[0].value
        }
      }).catch(err => {
        console.error('获取字典类型失败', err)
        row._dictTypeList = []
      })
    },
    
    // 新增：添加处理步骤对话框中“字典选择”变化，加载类型
    handleNewProcessorDictChange(dictId) {
      if (!this.newProcessor || !this.newProcessor.config) return
      if (!dictId) {
        this.$set(this.newProcessor.config, '_dictTypeList', [])
        this.$set(this.newProcessor.config, 'dictSourceTypeValue', '')
        return
      }
      this.$axios.get(`/v1/dict-mapping/${dictId}/types`).then(res => {
        const types = res.data || []
        this.$set(this.newProcessor.config, '_dictTypeList', (types || []).map(t => ({
          value: String(t.value),
          label: t.label || String(t.value)
        })))
        if (this.newProcessor.config._dictTypeList.length === 1) {
          this.$set(this.newProcessor.config, 'dictSourceTypeValue', this.newProcessor.config._dictTypeList[0].value)
        }
      }).catch(err => {
        console.error('获取字典类型失败', err)
        this.$set(this.newProcessor.config, '_dictTypeList', [])
      })
    },
    
    // 判断是否需要类型转换
    needsTypeConversion(mapping) {
      if (!mapping.sourceType || !mapping.targetType) {
        return false
      }
      
      // 标准化类型名称（去除大小写和长度）
      const sourceBaseType = this.getBaseType(mapping.sourceType)
      const targetBaseType = this.getBaseType(mapping.targetType)
      
      // 相同基础类型不需要转换
      if (sourceBaseType === targetBaseType) {
        return false
      }
      
      // 兼容的类型组合不需要转换
      const compatibleTypes = [
        ['TINYINT', 'SMALLINT', 'INT', 'INTEGER', 'BIGINT'],
        ['FLOAT', 'DOUBLE', 'DECIMAL', 'NUMERIC'],
        ['CHAR', 'VARCHAR', 'TEXT', 'LONGTEXT'],
        ['DATE', 'DATETIME', 'TIMESTAMP']
      ]
      
      for (const group of compatibleTypes) {
        if (group.includes(sourceBaseType) && group.includes(targetBaseType)) {
          return false
        }
      }
      
      return true
    },
    
    // 获取基础类型（去除长度等参数）
    getBaseType(typeStr) {
      if (!typeStr) return ''
      // 去除括号及其内容，转大写
      return typeStr.split('(')[0].trim().toUpperCase()
    },
    
    // 新增：函数/脚本模板辅助方法
    applyFunctionPreset(row) {
      row._funcParam1 = ''
      row._funcParam2 = ''
      row._funcOld = ''
      row._funcNew = ''
      row._funcPattern = ''
      this.updateFunctionExpression(row)
    },
    updateFunctionExpression(row) {
      if (!row || !row._funcPreset) {
        row.transformFunction = ''
        return
      }
      // ✅ 只支持转换类函数（移除UPPER/LOWER/TRIM/SUBSTR/REPLACE/LPAD/RPAD）
      if (row._funcPreset === 'CEIL' || row._funcPreset === 'FLOOR' || row._funcPreset === 'ABS' || row._funcPreset === 'TO_INT' || row._funcPreset === 'TO_STRING' || row._funcPreset === 'LENGTH' || row._funcPreset === 'REVERSE' || row._funcPreset === 'URL_ENCODE' || row._funcPreset === 'URL_DECODE' || row._funcPreset === 'BASE64_ENCODE' || row._funcPreset === 'BASE64_DECODE' || row._funcPreset === 'MD5') {
        row.transformFunction = row._funcPreset
        return
      }
      if (row._funcPreset === 'ROUND') {
        var sc = (row._funcScale || 0)
        row.transformFunction = 'ROUND(' + sc + ')'
        return
      }
      if (row._funcPreset === 'TO_DECIMAL') {
        var sd = (row._funcScale || 0)
        row.transformFunction = 'TO_DECIMAL(' + sd + ')'
        return
      }
      if (row._funcPreset === 'DATE_FORMAT') {
        var pattern = row._funcPattern || 'yyyy-MM-dd'
        row.transformFunction = "DATE_FORMAT('" + pattern + "')"
        return
      }
      if (row._funcPreset === 'CONCAT_PREFIX' || row._funcPreset === 'CONCAT_SUFFIX') {
        var text = row._funcConcatText || ''
        row.transformFunction = row._funcPreset + "('" + text.replace(/'/g, "'") + "')"
        return
      }
      if (row._funcPreset === 'SPLIT') {
        var delim = row._funcDelimiter || ','
        var idx = row._funcIndex || 0
        row.transformFunction = "SPLIT('" + delim.replace(/'/g, "'") + "'," + idx + ")"
        return
      }
      if (row._funcPreset === 'CONCAT') {
        var txt = row._funcText || ''
        row.transformFunction = "CONCAT('" + txt.replace(/'/g, "'") + "')"
        return
      }
      if (row._funcPreset === 'COALESCE') {
        var defVal = row._funcText || ''
        row.transformFunction = "COALESCE('" + defVal.replace(/'/g, "'") + "')"
        return
      }
      if (row._funcPreset === 'ADD') {
        var num = row._funcNumber || 0
        row.transformFunction = 'ADD(' + num + ')'
        return
      }
      if (row._funcPreset === 'MULTIPLY') {
        var mul = row._funcNumber || 1
        row.transformFunction = 'MULTIPLY(' + mul + ')'
        return
      }
      row.transformFunction = ''
    },
    applyScriptTemplate(row) {
      var t = row._scriptTemplate
      if (!t) return
      // ✅ 只支持转换类脚本（移除UPPER/LOWER/TRIM/SUBSTR/REPLACE/LPAD/RPAD）
      if (t === 'ROUND') row.transformScript = "value == null ? null : new java.math.BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP).toString()"
      else if (t === 'CEIL') row.transformScript = "value == null ? null : Math.ceil(Double.parseDouble(value.toString()))"
      else if (t === 'FLOOR') row.transformScript = "value == null ? null : Math.floor(Double.parseDouble(value.toString()))"
      else if (t === 'ABS') row.transformScript = "value == null ? null : Math.abs(Double.parseDouble(value.toString()))"
      else if (t === 'TO_INT') row.transformScript = "value == null ? null : Integer.parseInt(value.toString())"
      else if (t === 'TO_DECIMAL') row.transformScript = "value == null ? null : new java.math.BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP)"
      else if (t === 'DATE_FORMAT') row.transformScript = "value instanceof java.util.Date ? new java.text.SimpleDateFormat('yyyy-MM-dd').format(value) : value"
      else if (t === 'SPLIT') row.transformScript = "value == null ? null : value.toString().split(',')[0]"
      else if (t === 'CONCAT') row.transformScript = "value == null ? '' : value.toString() + 'suffix'"
    },
    addDictPair(row) {
      if (!row._dictPairs) row._dictPairs = []
      row._dictPairs.push({ key: '', value: '' })
      this.updateDictScript(row)
    },
    removeDictPair(row, idx) {
      if (!row._dictPairs) return
      row._dictPairs.splice(idx, 1)
      this.updateDictScript(row)
    },
    updateDictScript(row) {
      var arr = row._dictPairs || []
      var obj = {}
      for (var i = 0; i < arr.length; i++) {
        var k = arr[i].key
        var v = arr[i].value
        if (k !== undefined && k !== null && ('' + k).length > 0) {
          obj['' + k] = v
        }
      }
      try {
        row.transformScript = JSON.stringify(obj)
      } catch (e) {
        row.transformScript = '{}'
      }
    },
    
    querySourceFields(queryString, cb) {
      // 智能判断：
      // 1. 如果已经抽取了数据，优先使用抽取后的列名（兼容连表查询和自定义SQL）
      // 2. 如果没有抽取数据，但有表字段信息，使用表字段信息（单表查询）
      
      if (this.sourcePreviewColumns && this.sourcePreviewColumns.length > 0) {
        // 尝试从 sourceColumnInfos 中获取字段的详细信息
        const fields = this.sourcePreviewColumns.map(col => {
          // 查找对应的字段信息
          const columnInfo = this.sourceColumnInfos && this.sourceColumnInfos.length > 0
            ? this.sourceColumnInfos.find(c => c.columnName === col)
            : null
          
          if (columnInfo) {
            // 如果找到了字段信息，返回完整信息
            return {
              value: col,
              type: columnInfo.columnType,
              nullable: columnInfo.nullable,
              remarks: columnInfo.remarks,
              columnSize: columnInfo.columnSize
            }
          } else {
            // 如果没有找到（可能是连表查询的其他表字段），只返回字段名
            return { value: col }
          }
        })
        cb(queryString ? fields.filter(f => f.value.includes(queryString)) : fields)
      } else if (this.sourceColumnInfos && this.sourceColumnInfos.length > 0) {
        // 回退到源表字段信息（单表查询且未抽取数据）
        const fields = this.sourceColumnInfos.map(col => ({
          value: col.columnName,
          type: col.columnType,
          nullable: col.nullable,
          remarks: col.remarks,
          columnSize: col.columnSize
        }))
        cb(queryString ? fields.filter(f => f.value.includes(queryString)) : fields)
      } else {
        cb([])
      }
    },
    
    queryTargetFields(queryString, cb) {
      // 如果有字段信息，展示类型、必填、长度和描述信息
      if (this.targetColumnInfos && this.targetColumnInfos.length > 0) {
        const fields = this.targetColumnInfos.map(col => ({
          value: col.columnName,
          type: col.columnType,
          nullable: col.nullable,
          remarks: col.remarks,
          columnSize: col.columnSize
        }))
        cb(queryString ? fields.filter(f => f.value.includes(queryString)) : fields)
      } else {
        // 如果没有，尝试加载
        this.loadTargetColumns()
        cb([])
      }
    },
    
    /**
     * 多目标模式下查询目标字段
     */
    queryMultiTargetFields(targetIndex, queryString, cb) {
      const target = this.targets[targetIndex]
      if (!target) {
        cb([])
        return
      }
      
      // 如果已经有字段信息缓存
      if (target._columnInfos && target._columnInfos.length > 0) {
        const fields = target._columnInfos.map(col => ({
          value: col.columnName,
          type: col.columnType,
          nullable: col.nullable,
          remarks: col.remarks,
          columnSize: col.columnSize
        }))
        cb(queryString ? fields.filter(f => f.value.includes(queryString)) : fields)
      } else {
        // 尝试加载目标表字段信息
        this.loadMultiTargetColumns(targetIndex)
        cb([])
      }
    },
    
    /**
     * 加载多目标模式下指定目标的字段信息
     */
    loadMultiTargetColumns(targetIndex) {
      const target = this.targets[targetIndex]
      if (!target || !target.targetConnectorId || !target.targetConfig.tableName) {
        return
      }
      
      this.$axios.get(`/v1/dict-source/connector/${target.targetConnectorId}/table/${target.targetConfig.tableName}/columns-with-type`)
        .then(res => {
          this.$set(target, '_columnInfos', res.data || [])
        })
        .catch(err => {
          console.error('加载目标表字段失败', err)
          this.$set(target, '_columnInfos', [])
        })
    },
    
    // 加载目标表字段信息
    loadTargetColumns() {
      if (!this.targetConfig.connectorId || !this.targetConfig.tableName) {
        return
      }
      
      this.$axios.get(`/v1/dict-source/connector/${this.targetConfig.connectorId}/table/${this.targetConfig.tableName}/columns-with-type`)
        .then(res => {
          this.targetColumnInfos = res.data || []
        })
        .catch(err => {
          console.error('加载目标表字段失败', err)
        })
    },
    
    // 处理源字段输入（用户修改字段名时清空类型）
    handleSourceFieldInput(mapping) {
      // 如果字段名变化了，清空类型信息，等待用户选择或失焦后重新查找
      if (mapping._lastSourceField !== mapping.sourceField) {
        mapping.sourceType = ''
        mapping._lastSourceField = mapping.sourceField
      }
    },
    
    // 处理源字段选择（下拉选择）
    handleSourceFieldSelect(mapping, item) {
      if (item && item.type) {
        mapping.sourceType = item.type
        mapping.sourceNullable = item.nullable
        mapping.sourceRemarks = item.remarks
        mapping.sourceColumnSize = item.columnSize
      } else {
        // 如果下拉项没有类型，尝试从 sourceColumnInfos 查找
        this.handleSourceFieldBlur(mapping)
      }
    },
    
    // 处理源字段失焦，查找字段类型
    handleSourceFieldBlur(mapping) {
      if (!mapping.sourceField || mapping.sourceType) {
        return
      }
      
      // 从 sourceColumnInfos 中查找
      if (this.sourceColumnInfos && this.sourceColumnInfos.length > 0) {
        const columnInfo = this.sourceColumnInfos.find(col => col.columnName === mapping.sourceField)
        if (columnInfo) {
          mapping.sourceType = columnInfo.columnType
          mapping.sourceNullable = columnInfo.nullable
          mapping.sourceRemarks = columnInfo.remarks
          mapping.sourceColumnSize = columnInfo.columnSize
        }
      }
    },
    
    // 处理目标字段输入（用户修改字段名时清空类型）
    handleTargetFieldInput(mapping) {
      // 如果字段名变化了，清空类型信息，等待用户选择或失焦后重新查找
      if (mapping._lastTargetField !== mapping.targetField) {
        mapping.targetType = ''
        mapping._lastTargetField = mapping.targetField
      }
    },
    
    // 处理目标字段选择（下拉选择）
    handleTargetFieldSelect(mapping, item) {
      if (item && item.type) {
        mapping.targetType = item.type
        mapping.targetNullable = item.nullable
        mapping.targetRemarks = item.remarks
        mapping.targetColumnSize = item.columnSize
      } else {
        // 如果下拉项没有类型，尝试从 targetColumnInfos 查找
        this.handleTargetFieldBlur(mapping)
      }
    },
    
    // 处理目标字段失焦，查找字段类型
    handleTargetFieldBlur(mapping) {
      if (!mapping.targetField || mapping.targetType) {
        return
      }
      
      // 单目标模式：从 targetColumnInfos 中查找
      if (!this.multiTargetMode) {
        if (this.targetColumnInfos && this.targetColumnInfos.length > 0) {
          const columnInfo = this.targetColumnInfos.find(col => col.columnName === mapping.targetField)
          if (columnInfo) {
            this.$set(mapping, 'targetType', columnInfo.columnType)
            this.$set(mapping, 'targetNullable', columnInfo.nullable)
            this.$set(mapping, 'targetRemarks', columnInfo.remarks)
            this.$set(mapping, 'targetColumnSize', columnInfo.columnSize)
          }
        }
      } else {
        // 多目标模式：根据 mapping.targetId 查找对应目标的 _columnInfos
        if (mapping.targetId !== undefined && mapping.targetId !== null) {
          const target = this.targets[mapping.targetId]
          if (target && target._columnInfos && target._columnInfos.length > 0) {
            const columnInfo = target._columnInfos.find(col => col.columnName === mapping.targetField)
            if (columnInfo) {
              this.$set(mapping, 'targetType', columnInfo.columnType)
              this.$set(mapping, 'targetNullable', columnInfo.nullable)
              this.$set(mapping, 'targetRemarks', columnInfo.remarks)
              this.$set(mapping, 'targetColumnSize', columnInfo.columnSize)
            }
          }
        }
      }
    },
    
    // 预览转换效果
    handlePreviewTransform() {
      if (this.sourcePreviewData.length === 0) {
        this.$message.warning('请先在"选择数据源"步骤预览源数据')
        this.transformTab = 'before'
        return
      }
      
      if (this.mappings.length === 0) {
        this.$message.warning('请先配置字段映射规则')
        this.transformTab = 'config'
        return
      }
      
      this.transformPreviewLoading = true
      
      // 构造转换请求参数
      const requestData = {
        sourceData: this.sourcePreviewData,
        mappings: this.mappings.map(m => {
          // 构建 processorChain
          let processorChain = null
          if (m._processors && m._processors.length > 0) {
            processorChain = JSON.stringify(m._processors)
          }
          
          return {
            sourceField: m.sourceField,
            targetField: m.targetField,
            transformType: m.transformType,
            transformScript: m.transformScript || null,
            transformFunction: m.transformFunction || null,
            dictMappingId: m.dictMappingId || null,
            dictSourceTypeValue: m.dictSourceTypeValue || '',
            dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
            constantValue: m.constantValue || null,
            defaultValue: m.defaultValue || null,
            cleanseFunctions: m.cleanseFunctions || null,
            nullStrategy: m.nullStrategy || 'KEEP',
            processorChain: processorChain
          }
        })
      }
      
      // 调用后端转换预览接口
      this.$axios.post('/v1/transform/preview', requestData).then(res => {
        this.transformedPreviewData = res.data.transformedData || []
        
        // 按照 mappings 配置的顺序提取字段列表
        this.transformedPreviewColumns = this.mappings.map(m => m.targetField).filter(f => f)
        
        // 自动切换到转换后预览标签
        this.transformTab = 'after'
        this.$message.success(`转换成功！共生成 ${this.transformedPreviewData.length} 条数据`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '转换预览失败'
        this.$message.error(errMsg)
        this.transformedPreviewData = []
        this.transformedPreviewColumns = []
      }).finally(() => {
        this.transformPreviewLoading = false
      })
    },
    
    // 获取字段变化样式（高亮转换后的字段）
    getFieldChangeStyle(fieldName, rowIndex) {
      // 检查这个字段是否经过了转换
      const mapping = this.mappings.find(m => m.targetField === fieldName)
      if (!mapping) return {}
      
      // 如果是直接映射，不高亮
      if (mapping.transformType === 'DIRECT') {
        return {}
      }
      
      // 其他转换类型，使用不同颜色标识
      const colorMap = {
        'FUNCTION': '#409EFF', // 蓝色
        'SCRIPT': '#E6A23C',   // 橙色
        'DICT': '#67C23A',     // 绿色
        'CONSTANT': '#909399'  // 灰色
      }
      
      return {
        color: colorMap[mapping.transformType] || '#606266',
        fontWeight: 'bold'
      }
    },
    
    handleCronTemplateChange(val) {
      this.cronTemplate = val  // 更新选中的模板
      if (val === 'visual') {
        // 选择可视化配置，初始化默认值
        this.updateCronFromVisual()
      } else if (val === 'custom') {
        // 自定义模式，不设置表达式
        return
      } else {
        // 选择了常用模板
        this.scheduleConfig.cronExpression = val
        this.parseCronExpression()
      }
    },
    
    // 从可视化配置生成Cron表达式
    updateCronFromVisual() {
      const { period, weekDay, dayOfMonth, time, interval, unit } = this.cronVisual
      const [hour, minute] = time ? time.split(':') : ['0', '0']
      
      let cron = ''
      
      if (period === 'day') {
        cron = `0 ${minute} ${hour} * * ?`
        this.cronDescription = `每天${time}执行`
      } else if (period === 'week') {
        cron = `0 ${minute} ${hour} ? * ${weekDay}`
        const weekDayMap = {
          'MON': '周一', 'TUE': '周二', 'WED': '周三', 
          'THU': '周四', 'FRI': '周五', 'SAT': '周六', 'SUN': '周日'
        }
        this.cronDescription = `每${weekDayMap[weekDay]}${time}执行`
      } else if (period === 'month') {
        cron = `0 ${minute} ${hour} ${dayOfMonth} * ?`
        this.cronDescription = `每月${dayOfMonth}号${time}执行`
      } else if (period === 'custom') {
        if (unit === 'minute') {
          cron = `0 */${interval} * * * ?`
          this.cronDescription = `每${interval}分钟执行一次`
        } else if (unit === 'hour') {
          cron = `0 0 */${interval} * * ?`
          this.cronDescription = `每${interval}小时执行一次`
        } else if (unit === 'day') {
          cron = `0 ${minute} ${hour} */${interval} * ?`
          this.cronDescription = `每${interval}天${time}执行`
        }
      }
      
      this.scheduleConfig.cronExpression = cron
    },
    
    // 解析Cron表达式
    parseCronExpression() {
      const cron = this.scheduleConfig.cronExpression
      if (!cron || !cron.trim()) {
        this.cronDescription = ''
        return
      }
      
      const parts = cron.trim().split(/\s+/)
      if (parts.length !== 6) {
        this.cronDescription = '请检查表达式格式（应为6个部分）'
        return
      }
      
      // 常见模式匹配
      const patterns = {
        '0 */5 * * * ?': '每5分钟执行一次',
        '0 */30 * * * ?': '每30分钟执行一次',
        '0 0 * * * ?': '每小时整点执行',
        '0 0 2 * * ?': '每天凌晨2:00执行',
        '0 0 2 ? * MON': '每周一凌晨2:00执行',
        '0 0 9 ? * MON-FRI': '工作日上午9:00执行',
        '0 0 2 1 * ?': '每月1号凌晨2:00执行'
      }
      
      if (patterns[cron]) {
        this.cronDescription = patterns[cron]
      } else {
        this.cronDescription = '自定义表达式'
      }
    },
    
    getConnectorName(id) {
      const connector = this.connectorList.find(c => c.id === id)
      return connector ? connector.connectorName : '-'
    },
    
    // ========== 多目标相关方法 ==========
    
    /**
     * 切换目标模式
     */
    handleTargetModeChange(isMulti) {
      if (isMulti) {
        // 切换到多目标模式
        if (this.targetConfig.connectorId) {
          // 如果有单目标配置，转换为第一个目标
          this.targets = [{
            id: null,
            targetConnectorId: this.targetConfig.connectorId,
            targetName: '目标表1',
            targetConfig: {
              tableName: this.targetConfig.tableName || '',
              writeMode: this.targetConfig.writeMode || 'INSERT',
              batchSize: 1000,
              maxRetries: 3,
              idempotentKey: ''
            },
            sortOrder: 0,
            status: 1
          }]
        } else {
          this.targets = []
        }
      } else {
        // 切换到单目标模式
        if (this.targets.length > 0) {
          const firstTarget = this.targets[0]
          this.targetConfig.connectorId = firstTarget.targetConnectorId
          this.targetConfig.tableName = firstTarget.targetConfig.tableName
          this.targetConfig.writeMode = firstTarget.targetConfig.writeMode
        }
      }
    },
    
    /**
     * 添加目标
     */
    addTarget() {
      const newTarget = {
        id: null,
        targetConnectorId: null,
        targetName: `目标表${this.targets.length + 1}`,
        targetConfig: {
          tableName: '',
          writeMode: 'INSERT',
          batchSize: 1000,
          maxRetries: 3,
          idempotentKey: ''
        },
        sortOrder: this.targets.length,
        status: 1
      }
      this.targets.push(newTarget)
      this.activeTargetCollapseIndex = [this.targets.length - 1]
    },
    
    /**
     * 删除目标
     */
    removeTarget(index) {
      this.$confirm(`确定删除目标【${this.targets[index].targetName}】吗？`, '提示', {
        type: 'warning'
      }).then(() => {
        this.targets.splice(index, 1)
        // 删除该目标的字段映射
        this.mappings = this.mappings.filter(m => m.targetId !== index)
        this.$message.success('已删除')
      }).catch(() => {})
    },
    
    /**
     * 多目标连接器变化
     */
    handleMultiTargetConnectorChange(index) {
      const target = this.targets[index]
      const connector = this.connectorList.find(c => c.id === target.targetConnectorId)
      if (connector) {
        this.$message.success(`已选择连接器：${connector.connectorName}`)
        // 清空表列表
        this.$set(target, '_tableList', [])
        this.$set(target, '_loadingTables', false)
      }
    },
    
    /**
     * 加载多目标表列表
     */
    handleLoadMultiTargetTables(index) {
      const target = this.targets[index]
      if (!target.targetConnectorId) {
        this.$message.warning('请先选择目标连接器')
        return
      }
      
      this.$set(target, '_loadingTables', true)
      this.$axios.get(`/v1/task/connector/${target.targetConnectorId}/tables-with-comment`).then(res => {
        this.$set(target, '_tableList', res.data || [])
        this.$message.success(`已加载 ${res.data.length} 个表，请在下拉框中选择`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '获取表列表失败'
        this.$message.error(errMsg)
        this.$set(target, '_tableList', [])
      }).finally(() => {
        this.$set(target, '_loadingTables', false)
      })
    },
    
    /**
     * 获取指定目标的字段映射
     */
    getTargetMappings(targetIndex) {
      return this.mappings.filter(m => m.targetId === targetIndex)
    },
    
    /**
     * 更新指定目标的字段映射
     */
    updateTargetMappings(targetIndex, mappings) {
      // 删除旧映射
      this.mappings = this.mappings.filter(m => m.targetId !== targetIndex)
      // 添加新映射
      mappings.forEach(m => {
        m.targetId = targetIndex
        this.mappings.push(m)
      })
    },
    
    /**
     * 获取目标映射数量
     */
    getTargetMappingCount(targetIndex) {
      return this.mappings.filter(m => m.targetId === targetIndex).length
    },
    
    /**
     * 为指定目标添加字段映射
     */
    handleAddMappingForTarget(targetIndex) {
      const newMapping = {
        targetId: targetIndex,
        sourceField: '',
        targetField: '',
        transformType: 'DIRECT',
        dictMappingId: null,
        dictSourceTypeValue: '',
        dictCode: '',
        transformScript: '',
        transformFunction: '',
        defaultValue: '',
        constantValue: '',
        _dictTypeList: [],
        nullStrategy: 'KEEP',
        // 默认添加一个直接映射的数据转换处理器
        _processors: [{
          type: 'TRANSFORM',
          order: 1,
          config: {
            transformType: 'DIRECT',
            dictMappingId: null,
            dictSourceTypeValue: '',
            dictOutputMode: 'TARGET_KEY',
            constantValue: '',
            defaultValue: ''
          }
        }]
      }
      this.mappings.push(newMapping)
    },
    
    /**
     * 智能映射（多目标）
     */
    handleAutoMappingForTarget(targetIndex) {
      if (this.sourcePreviewColumns.length === 0) {
        this.$message.warning('请先在步骤1中抽取数据')
        return
      }
      
      const target = this.targets[targetIndex]
      if (!target || !target.targetConnectorId || !target.targetConfig.tableName) {
        this.$message.warning('请先配置目标连接器和目标表')
        return
      }
      
      // 清空该目标的现有映射
      this.mappings = this.mappings.filter(m => m.targetId !== targetIndex)
      
      const loading = this.$loading({
        lock: true,
        text: '正在获取目标表字段信息...',
        spinner: 'el-icon-loading'
      })
      
      // 获取目标表字段信息
      this.$axios.get(`/v1/dict-source/connector/${target.targetConnectorId}/table/${target.targetConfig.tableName}/columns-with-type`)
        .then(res => {
          const targetColumnInfos = res.data || []
          
          if (targetColumnInfos.length === 0) {
            this.$message.warning('未获取到目标表字段')
            loading.close()
            return
          }
          
          // 以目标字段为基准，为每个目标字段寻找匹配的源字段
          const sourceFieldSet = new Set(this.sourcePreviewColumns)
          let matchedCount = 0
          
          targetColumnInfos.forEach(targetCol => {
            const targetField = targetCol.columnName
            const sourceField = sourceFieldSet.has(targetField) ? targetField : ''
            if (sourceField) matchedCount++
            
            this.mappings.push({
              targetId: targetIndex,
              sourceField: sourceField,
              targetField: targetField,
              targetType: targetCol.columnType,
              targetNullable: targetCol.nullable,
              targetRemarks: targetCol.remarks,
              transformType: 'DIRECT',
              dictMappingId: null,
              dictSourceTypeValue: '',
              dictCode: '',
              transformScript: '',
              transformFunction: '',
              defaultValue: '',
              constantValue: '',
              _dictTypeList: [],
              nullStrategy: 'KEEP',
              // 默认添加一个直接映射的数据转换处理器
              _processors: [{
                type: 'TRANSFORM',
                order: 1,
                config: {
                  transformType: 'DIRECT',
                  dictMappingId: null,
                  dictSourceTypeValue: '',
                  dictOutputMode: 'TARGET_KEY',
                  constantValue: '',
                  defaultValue: ''
                }
              }]
            })
          })
          
          loading.close()
          this.$message.success(`已为目标${targetIndex + 1}生成${targetColumnInfos.length}个字段映射，自动匹配${matchedCount}个源字段`)
          
          // 加载字段类型信息
          this.$nextTick(() => {
            this.loadFieldTypesForMultiTargetMappings(targetIndex)
          })
        })
        .catch(err => {
          loading.close()
          const errMsg = (err.response && err.response.data && err.response.data.message) || '获取目标表字段失败'
          this.$message.error(errMsg)
        })
    },
    
    /**
     * 加载多目标模式下的字段类型信息
     */
    loadFieldTypesForMultiTargetMappings(targetIndex) {
      const target = this.targets[targetIndex]
      if (!target) return
      
      // 获取源表名
      let sourceTableName = this.sourceConfig.tableName
      if (!sourceTableName && this.sourceConfig.sql) {
        const sql = this.sourceConfig.sql.trim()
        const fromMatch = sql.match(/\bFROM\s+([`"\[]?\w+[`"\]]?\.)?([`"\[]?\w+[`"\]]?)/i)
        if (fromMatch) {
          sourceTableName = fromMatch[2].replace(/[`"\[\]]/g, '')
          console.log('从 SQL 提取的源表名:', sourceTableName)
        }
      }
      
      // 加载源字段信息
      const sourcePromise = this.sourceConfig.connectorId && sourceTableName
        ? this.$axios.get(`/v1/dict-source/connector/${this.sourceConfig.connectorId}/table/${sourceTableName}/columns-with-type`)
            .then(res => {
              this.sourceColumnInfos = res.data || []
              return res
            })
            .catch(err => {
              console.warn('获取源表字段信息失败', err)
              return { data: [] }
            })
        : Promise.resolve({ data: [] })
      
      // 加载目标字段信息
      const targetPromise = target.targetConnectorId && target.targetConfig.tableName
        ? this.$axios.get(`/v1/dict-source/connector/${target.targetConnectorId}/table/${target.targetConfig.tableName}/columns-with-type`)
            .then(res => {
              this.$set(target, '_columnInfos', res.data || [])
              return res
            })
            .catch(err => {
              console.warn('获取目标表字段信息失败', err)
              return { data: [] }
            })
        : Promise.resolve({ data: [] })
      
      Promise.all([sourcePromise, targetPromise]).then(([sourceRes, targetRes]) => {
        const sourceColumnInfos = sourceRes.data || []
        const targetColumnInfos = targetRes.data || []
        
        // 建立字段映射
        const sourceFieldMap = new Map()
        sourceColumnInfos.forEach(col => {
          sourceFieldMap.set(col.columnName, {
            type: col.columnType,
            nullable: col.nullable,
            remarks: col.remarks,
            columnSize: col.columnSize
          })
        })
        
        const targetFieldMap = new Map()
        targetColumnInfos.forEach(col => {
          targetFieldMap.set(col.columnName, {
            type: col.columnType,
            nullable: col.nullable,
            remarks: col.remarks,
            columnSize: col.columnSize
          })
        })
        
        // 为该目标的所有mapping填充字段类型
        const targetMappings = this.mappings.filter(m => m.targetId === targetIndex)
        targetMappings.forEach(mapping => {
          // 填充源字段类型
          if (mapping.sourceField && sourceFieldMap.has(mapping.sourceField)) {
            const sourceInfo = sourceFieldMap.get(mapping.sourceField)
            this.$set(mapping, 'sourceType', sourceInfo.type)
            this.$set(mapping, 'sourceNullable', sourceInfo.nullable)
            this.$set(mapping, 'sourceRemarks', sourceInfo.remarks)
            this.$set(mapping, 'sourceColumnSize', sourceInfo.columnSize)
          }
          
          // 填充目标字段类型
          if (mapping.targetField && targetFieldMap.has(mapping.targetField)) {
            const targetInfo = targetFieldMap.get(mapping.targetField)
            this.$set(mapping, 'targetType', targetInfo.type)
            this.$set(mapping, 'targetNullable', targetInfo.nullable)
            this.$set(mapping, 'targetRemarks', targetInfo.remarks)
            this.$set(mapping, 'targetColumnSize', targetInfo.columnSize)
          }
        })
        
        console.log(`目标${targetIndex + 1}: 字段类型加载完成`, {
          sourceFields: sourceColumnInfos.length,
          targetFields: targetColumnInfos.length,
          mappings: targetMappings.length
        })
      }).catch(err => {
        console.error('加载字段类型失败', err)
      })
    },
    
    /**
     * 清空目标映射
     */
    handleClearTargetMappings(targetIndex) {
      this.$confirm('确定清空该目标的所有字段映射吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.mappings = this.mappings.filter(m => m.targetId !== targetIndex)
        this.$message.success('已清空')
      }).catch(() => {})
    },
    
    /**
     * 获取目标的转换数据
     */
    getTargetTransformedData(targetIndex) {
      const key = String(targetIndex)
      return this.targetTransformedData[key] ? this.targetTransformedData[key].data : []
    },
    
    /**
     * 获取目标的转换字段
     */
    getTargetTransformedColumns(targetIndex) {
      const key = String(targetIndex)
      return this.targetTransformedData[key] ? this.targetTransformedData[key].columns : []
    },
    
    /**
     * 预览指定目标的转换效果
     */
    handlePreviewTargetTransform(targetIndex) {
      if (this.sourcePreviewData.length === 0) {
        this.$message.warning('请先在"选择数据源"步骤预览源数据')
        this.transformTab = 'before'
        return
      }
      
      const targetMappings = this.getTargetMappings(targetIndex)
      if (targetMappings.length === 0) {
        this.$message.warning('请先为该目标配置字段映射规则')
        this.transformTab = 'config'
        this.activeTargetTab = String(targetIndex)
        return
      }
      
      this.transformPreviewLoading = true
      
      // 构造转换请求参数
      const requestData = {
        sourceData: this.sourcePreviewData,
        mappings: targetMappings.map(m => {
          // 构建 processorChain
          let processorChain = null
          if (m._processors && m._processors.length > 0) {
            processorChain = JSON.stringify(m._processors)
          }
          
          return {
            sourceField: m.sourceField,
            targetField: m.targetField,
            transformType: m.transformType,
            transformScript: m.transformScript || null,
            transformFunction: m.transformFunction || null,
            dictMappingId: m.dictMappingId || null,
            dictSourceTypeValue: m.dictSourceTypeValue || '',
            dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
            constantValue: m.constantValue || null,
            defaultValue: m.defaultValue || null,
            cleanseFunctions: m.cleanseFunctions || null,
            nullStrategy: m.nullStrategy || 'KEEP',
            processorChain: processorChain
          }
        })
      }
      
      // 调用后端转换预览接口
      this.$axios.post('/v1/transform/preview', requestData).then(res => {
        const transformedData = res.data.transformedData || []
        const columns = targetMappings.map(m => m.targetField).filter(f => f)
        
        // 存储该目标的转换数据
        this.$set(this.targetTransformedData, String(targetIndex), {
          data: transformedData,
          columns: columns
        })
        
        // 自动切换到转换后预览标签
        this.transformTab = 'after'
        this.activeTargetPreviewTab = String(targetIndex)
        
        const targetName = this.targets[targetIndex].targetName || `目标${targetIndex + 1}`
        this.$message.success(`【${targetName}】转换成功！共生成 ${transformedData.length} 条数据`)
      }).catch(err => {
        const errMsg = (err.response && err.response.data && err.response.data.message) || '转换预览失败'
        this.$message.error(errMsg)
        
        // 清空该目标的转换数据
        this.$set(this.targetTransformedData, String(targetIndex), {
          data: [],
          columns: []
        })
      }).finally(() => {
        this.transformPreviewLoading = false
      })
    },
    
    /**
     * 获取字段变化样式（多目标模式）
     */
    getFieldChangeStyleForTarget(targetIndex, fieldName, rowIndex) {
      const targetMappings = this.getTargetMappings(targetIndex)
      const mapping = targetMappings.find(m => m.targetField === fieldName)
      if (!mapping) return {}
      
      // 如果是直接映射，不高亮
      if (mapping.transformType === 'DIRECT') {
        return {}
      }
      
      // 其他转换类型，使用不同颜色标识
      const colorMap = {
        'FUNCTION': '#409EFF', // 蓝色
        'SCRIPT': '#E6A23C',   // 橙色
        'DICT': '#67C23A',     // 绿色
        'CONSTANT': '#909399'  // 灰色
      }
      
      return {
        color: colorMap[mapping.transformType] || '#606266',
        fontWeight: 'bold'
      }
    },
    
    /**
     * 删除目标映射
     */
    handleDeleteTargetMapping(targetIndex, mappingIndex) {
      const targetMappings = this.getTargetMappings(targetIndex)
      const mapping = targetMappings[mappingIndex]
      const index = this.mappings.indexOf(mapping)
      if (index > -1) {
        this.mappings.splice(index, 1)
      }
    },
    
    handleSaveTask() {
      // 根据连接器类型组装sourceConfig
      let sourceConfigData = {}
      if (this.sourceConnectorType === 'DATABASE') {
        // 获取表名：优先使用手动选择的tableName，其次从SQL中解析
        let tableName = this.sourceConfig.tableName
        if (!tableName && this.sourceConfig.sql) {
          tableName = this.extractTableNameFromSql(this.sourceConfig.sql)
        }
            
        sourceConfigData = { 
          sql: this.sourceConfig.sql,
          tableName: tableName,  // 保存表名，用于编辑时加载字段信息
          streamMode: this.sourceConfig.streamMode || false,  // 执行策略：流式模式
          fetchBatchSize: this.sourceConfig.fetchBatchSize || 5000  // 批处理大小（性能优化）
        }
      } else if (this.sourceConnectorType === 'API') {
        // 构建启用的参数
        const enabledParams = (this.sourceConfig.apiParams || []).filter(p => p.enabled && p.key)
        const params = {}
        enabledParams.forEach(p => {
          params[p.key] = p.value || ''
        })
        
        sourceConfigData = {
          apiPath: this.sourceConfig.apiPath,
          apiMethod: this.sourceConfig.apiMethod,
          params: params,
          dataPath: this.sourceConfig.dataPath
        }
        
        // 分页配置
        if (this.sourceConfig.enablePagination) {
          sourceConfigData.pagination = {
            enabled: true,
            pageParam: this.sourceConfig.pageParam,
            pageSizeParam: this.sourceConfig.pageSizeParam,
            startPage: this.sourceConfig.startPage,
            pageSize: this.sourceConfig.pageSize,
            totalPath: this.sourceConfig.totalPath
          }
        }
      }
      
      const loading = this.$loading({
        lock: true,
        text: '正在保存任务...',
        spinner: 'el-icon-loading'
      })
      
      // 判断是否多目标模式
      if (this.multiTargetMode) {
        // 多目标模式：使用新接口
        const taskData = {
          ...this.taskConfig,
          sourceConnectorId: this.sourceConfig.connectorId,
          sourceConfig: JSON.stringify(sourceConfigData),
          scheduleType: this.scheduleConfig.scheduleType,
          cronExpression: this.scheduleConfig.cronExpression,
          postLoadConfig: this.postLoadConfig.statusUpdate.enabled ? JSON.stringify(this.postLoadConfig) : null,
          multiTarget: 1,
          status: 1
        }
        
        // 准备targets数据
        const targetsData = this.targets.map((t, index) => ({
          id: t.id || null,
          targetName: t.targetName,
          targetConnectorId: t.targetConnectorId,
          targetConfig: JSON.stringify(t.targetConfig),
          sortOrder: index,
          status: 1
        }))
        
        // 准备fieldMappings数据
        const mappingsData = this.mappings.map(m => {
          let processorChain = null
          if (m._processors && m._processors.length > 0) {
            processorChain = JSON.stringify(m._processors)
          }
          
          // 固定值类型：将 constantValue 同步到 defaultValue 持久化
          let defaultValueToSave = m.defaultValue
          if (m.transformType === 'CONSTANT' && m.constantValue) {
            defaultValueToSave = m.constantValue
          }
          
          return {
            targetId: m.targetId || 0,
            sourceField: m.sourceField,
            targetField: m.targetField,
            transformType: m.transformType,
            dictMappingId: m.dictMappingId || null,
            dictSourceTypeValue: m.dictSourceTypeValue || '',
            dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
            transformScript: m.transformScript,
            transformFunction: m.transformFunction,
            defaultValue: defaultValueToSave,
            constantValue: m.constantValue,
            cleanseFunctions: m.cleanseFunctions || null,
            nullStrategy: m.nullStrategy || 'KEEP',
            processorChain: processorChain
          }
        })
        
        this.$axios.post('/v1/task/save-with-targets', {
          task: taskData,
          targets: targetsData,
          fieldMappings: mappingsData
        }).then(res => {
          const taskId = this.taskConfig.id || res.data
          
          // 如果是定时任务且启用，则自动启动调度
          if (this.scheduleConfig.scheduleType === 'CRON' && taskData.status === 1) {
            return this.$axios.post(`/v1/task/${taskId}/start`).then(() => {
              this.$message.success('任务保存成功，定时调度已启动!')
            }).catch(err => {
              console.warn('启动调度失败:', err)
              this.$message.warning('任务保存成功，但调度启动失败，请在任务列表中手动启动')
            })
          } else {
            this.$message.success('任务保存成功!')
          }
        }).then(() => {
          setTimeout(() => {
            this.$router.push('/task')
          }, 1000)
        }).catch(err => {
          const errMsg = (err.response && err.response.data && err.response.data.message) || '保存失败'
          this.$message.error(errMsg)
        }).finally(() => {
          loading.close()
        })
      } else {
        // 单目标模式：保持原有逻辑
        // 根据连接器类型组装targetConfig
        let targetConfigData = {}
        if (this.targetConnectorType === 'DATABASE') {
          targetConfigData = {
            tableName: this.targetConfig.tableName,
            writeMode: this.targetConfig.writeMode,
            primaryKey: this.targetConfig.primaryKey
          }
        } else if (this.targetConnectorType === 'API') {
          targetConfigData = {
            apiPath: this.targetConfig.apiPath,
            apiMethod: this.targetConfig.apiMethod,
            batchMode: this.targetConfig.batchMode
          }
          
          // 批量配置
          if (this.targetConfig.batchMode === 'batch') {
            targetConfigData.batchConfig = {
              batchSize: this.targetConfig.batchSize,
              wrapperField: this.targetConfig.wrapperField
            }
          }
          
          // 重试配置
          targetConfigData.retryConfig = {
            retryTimes: this.targetConfig.retryTimes,
            retryInterval: this.targetConfig.retryInterval
          }
        }
        
        const taskData = {
          ...this.taskConfig,
          sourceConnectorId: this.sourceConfig.connectorId,
          targetConnectorId: this.targetConfig.connectorId,
          sourceConfig: JSON.stringify(sourceConfigData),
          targetConfig: JSON.stringify(targetConfigData),
          scheduleType: this.scheduleConfig.scheduleType,
          cronExpression: this.scheduleConfig.cronExpression,
          postLoadConfig: this.postLoadConfig.statusUpdate.enabled ? JSON.stringify(this.postLoadConfig) : null,
          status: 1
        }
        
        // 编辑模式使用PUT，创建模式使用POST
        const isEditMode = !!this.taskConfig.id
        const apiCall = isEditMode 
          ? this.$axios.put('/v1/task', taskData)
          : this.$axios.post('/v1/task', taskData)
        
        apiCall.then(res => {
          const taskId = isEditMode ? this.taskConfig.id : res.data
          
          // 保存字段映射
          const mappingsData = this.mappings.map(m => {
            // 构建 processorChain（如果有处理器）
            let processorChain = null
            if (m._processors && m._processors.length > 0) {
              // 同步processorChain中TRANSFORM-CONSTANT处理器的constantValue到defaultValue
              m._processors.forEach(processor => {
                if (processor.type === 'TRANSFORM' && 
                    processor.config && 
                    processor.config.transformType === 'CONSTANT' && 
                    processor.config.constantValue) {
                  processor.config.defaultValue = processor.config.constantValue
                }
              })
              
              // 将处理器链转换为JSON字符串
              processorChain = JSON.stringify(m._processors)
            }
            
            // 固定值类型：将 constantValue 同步到 defaultValue 持久化
            let defaultValueToSave = m.defaultValue
            if (m.transformType === 'CONSTANT' && m.constantValue) {
              defaultValueToSave = m.constantValue
            }
            
            return {
              taskId: taskId,
              sourceField: m.sourceField,
              targetField: m.targetField,
              transformType: m.transformType,
              dictMappingId: m.dictMappingId || null,
              dictSourceTypeValue: m.dictSourceTypeValue || '',
              dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
              transformScript: m.transformScript,
              transformFunction: m.transformFunction,
              defaultValue: defaultValueToSave,
              constantValue: m.constantValue,
              cleanseFunctions: m.cleanseFunctions || null,
              nullStrategy: m.nullStrategy || 'KEEP',
              processorChain: processorChain
            }
          })
          
          return this.$axios.post(`/v1/task/${taskId}/mappings`, mappingsData).then(() => taskId)
        }).then(taskId => {
          // 保存辅助数据源
          if (this.auxiliaryDatasources.length > 0) {
            const auxData = this.auxiliaryDatasources.map(aux => ({
              taskId: taskId,
              connectorId: aux.connectorId,
              connectorType: aux.connectorType,
              alias: aux.alias,
              joinType: aux.joinType,
              joinCondition: JSON.stringify(aux.joinCondition),
              config: JSON.stringify(aux.config),
              enabled: aux.enabled || 1,
              sortOrder: aux.sortOrder || 0
            }))
            return this.$axios.post(`/v1/auxiliary-datasource/batch/${taskId}`, auxData).then(() => taskId)
          }
          return taskId
        }).then(taskId => {
          // 如果是定时任务且启用，则自动启动调度
          if (this.scheduleConfig.scheduleType === 'CRON') {
            return this.$axios.post(`/v1/task/${taskId}/start`).then(() => {
              const isEditMode = !!this.taskConfig.id
              this.$message.success(isEditMode ? '任务修改成功，定时调度已启动!' : '任务创建成功，定时调度已启动!')
            }).catch(err => {
              console.warn('启动调度失败:', err)
              const isEditMode = !!this.taskConfig.id
              this.$message.warning(isEditMode ? '任务修改成功，但调度启动失败，请在任务列表中手动启动' : '任务创建成功，但调度启动失败，请在任务列表中手动启动')
            })
          } else {
            const isEditMode = !!this.taskConfig.id
            this.$message.success(isEditMode ? '任务修改成功!' : '任务创建成功!')
          }
        }).then(() => {
          setTimeout(() => {
            this.$router.push('/task')
          }, 1000)
        }).catch(err => {
          const errMsg = (err.response && err.response.data && err.response.data.message) || '保存失败'
          this.$message.error(errMsg)
        }).finally(() => {
          loading.close()
        })
      }
    },
    
    handleSaveDraft() {
      // 根据连接器类型组装sourceConfig
      let sourceConfigData = {}
      if (this.sourceConnectorType === 'DATABASE') {
        // 获取表名：优先使用手动选择的tableName，其次从SQL中解析
        let tableName = this.sourceConfig.tableName
        if (!tableName && this.sourceConfig.sql) {
          tableName = this.extractTableNameFromSql(this.sourceConfig.sql)
        }
            
        sourceConfigData = { 
          sql: this.sourceConfig.sql,
          tableName: tableName,  // 保存表名，用于编辑时加载字段信息
          streamMode: this.sourceConfig.streamMode || false,  // 执行策略：流式模式
          fetchBatchSize: this.sourceConfig.fetchBatchSize || 5000  // 批处理大小（性能优化）
        }
      } else if (this.sourceConnectorType === 'API') {
        const enabledParams = (this.sourceConfig.apiParams || []).filter(p => p.enabled && p.key)
        const params = {}
        enabledParams.forEach(p => {
          params[p.key] = p.value || ''
        })
        
        sourceConfigData = {
          apiPath: this.sourceConfig.apiPath,
          apiMethod: this.sourceConfig.apiMethod,
          params: params,
          dataPath: this.sourceConfig.dataPath
        }
        
        if (this.sourceConfig.enablePagination) {
          sourceConfigData.pagination = {
            enabled: true,
            pageParam: this.sourceConfig.pageParam,
            pageSizeParam: this.sourceConfig.pageSizeParam,
            startPage: this.sourceConfig.startPage,
            pageSize: this.sourceConfig.pageSize,
            totalPath: this.sourceConfig.totalPath
          }
        }
      }
      
      const loading = this.$loading({
        lock: true,
        text: '正在保存草稿...',
        spinner: 'el-icon-loading'
      })
      
      // 判断是否多目标模式
      if (this.multiTargetMode) {
        // 多目标模式：使用新接口
        const taskData = {
          ...this.taskConfig,
          sourceConnectorId: this.sourceConfig.connectorId,
          sourceConfig: JSON.stringify(sourceConfigData),
          scheduleType: this.scheduleConfig.scheduleType,
          cronExpression: this.scheduleConfig.cronExpression,
          postLoadConfig: this.postLoadConfig.statusUpdate.enabled ? JSON.stringify(this.postLoadConfig) : null,
          multiTarget: 1,
          status: 0  // 草稿：不启用
        }
        
        // 准备targets数据（草稿允许不完整）
        const targetsData = this.targets.map((t, index) => ({
          id: t.id || null,
          targetName: t.targetName || '',  // 允许为空
          targetConnectorId: t.targetConnectorId || null,
          targetConfig: JSON.stringify(t.targetConfig || {}),
          sortOrder: index,
          status: 1
        }))
        
        // 准备fieldMappings数据
        const mappingsData = (this.mappings || []).map(m => {
          let processorChain = null
          if (m._processors && m._processors.length > 0) {
            // 同步processorChain中TRANSFORM-CONSTANT处理器的constantValue到defaultValue
            m._processors.forEach(processor => {
              if (processor.type === 'TRANSFORM' && 
                  processor.config && 
                  processor.config.transformType === 'CONSTANT' && 
                  processor.config.constantValue) {
                processor.config.defaultValue = processor.config.constantValue
              }
            })
            
            processorChain = JSON.stringify(m._processors)
          }
          
          // 固定值类型：将 constantValue 同步到 defaultValue 持久化
          let defaultValueToSave = m.defaultValue
          if (m.transformType === 'CONSTANT' && m.constantValue) {
            defaultValueToSave = m.constantValue
          }
          
          return {
            targetId: m.targetId || 0,
            sourceField: m.sourceField || '',
            targetField: m.targetField || '',
            transformType: m.transformType,
            dictMappingId: m.dictMappingId || null,
            dictSourceTypeValue: m.dictSourceTypeValue || '',
            dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
            transformScript: m.transformScript,
            transformFunction: m.transformFunction,
            defaultValue: defaultValueToSave,
            constantValue: m.constantValue,
            cleanseFunctions: m.cleanseFunctions || null,
            nullStrategy: m.nullStrategy || 'KEEP',
            processorChain: processorChain
          }
        })
        
        this.$axios.post('/v1/task/save-with-targets', {
          task: taskData,
          targets: targetsData,
          fieldMappings: mappingsData
        }).then(res => {
          const isEditMode = !!this.taskConfig.id
          this.$message.success(isEditMode ? '草稿修改成功！任务已禁用' : '草稿保存成功！任务已禁用')
          setTimeout(() => {
            this.$router.push('/task')
          }, 800)
        }).catch(err => {
          const errMsg = (err.response && err.response.data && err.response.data.message) || '草稿保存失败'
          this.$message.error(errMsg)
        }).finally(() => {
          loading.close()
        })
      } else {
        // 单目标模式：保持原有逻辑
        let targetConfigData = {}
        if (this.targetConnectorType === 'DATABASE') {
          targetConfigData = {
            tableName: this.targetConfig.tableName,
            writeMode: this.targetConfig.writeMode,
            primaryKey: this.targetConfig.primaryKey
          }
        } else if (this.targetConnectorType === 'API') {
          targetConfigData = {
            apiPath: this.targetConfig.apiPath,
            apiMethod: this.targetConfig.apiMethod,
            batchMode: this.targetConfig.batchMode
          }
          
          if (this.targetConfig.batchMode === 'batch') {
            targetConfigData.batchConfig = {
              batchSize: this.targetConfig.batchSize,
              wrapperField: this.targetConfig.wrapperField
            }
          }
          
          targetConfigData.retryConfig = {
            retryTimes: this.targetConfig.retryTimes,
            retryInterval: this.targetConfig.retryInterval
          }
        }
        
        const taskData = {
          ...this.taskConfig,
          sourceConnectorId: this.sourceConfig.connectorId,
          targetConnectorId: this.targetConfig.connectorId,
          sourceConfig: JSON.stringify(sourceConfigData),
          targetConfig: JSON.stringify(targetConfigData),
          scheduleType: this.scheduleConfig.scheduleType,
          cronExpression: this.scheduleConfig.cronExpression,
          postLoadConfig: this.postLoadConfig.statusUpdate.enabled ? JSON.stringify(this.postLoadConfig) : null,
          status: 0 // 草稿：不启用
        }

        // 编辑模式使用PUT，创建模式使用POST
        const isEditMode = !!this.taskConfig.id
        const apiCall = isEditMode 
          ? this.$axios.put('/v1/task', taskData)
          : this.$axios.post('/v1/task', taskData)

        // 保存任务（草稿）
        apiCall.then(res => {
          const taskId = isEditMode ? this.taskConfig.id : res.data
          // 如果已有字段映射，同步保存；无映射则跳过
          if (this.mappings && this.mappings.length > 0) {
            const mappingsData = this.mappings.map(m => {
              // 构建 processorChain（如果有处理器）
              let processorChain = null
              if (m._processors && m._processors.length > 0) {
                // 将处理器链转换为JSON字符串
                processorChain = JSON.stringify(m._processors)
              }
              
              return {
                taskId: taskId,
                sourceField: m.sourceField,
                targetField: m.targetField,
                transformType: m.transformType,
                dictMappingId: m.dictMappingId || null,
                dictSourceTypeValue: m.dictSourceTypeValue || '',
                dictOutputMode: m.dictOutputMode || 'TARGET_KEY',
                transformScript: m.transformScript,
                transformFunction: m.transformFunction,
                defaultValue: m.defaultValue,
                constantValue: m.constantValue,
                cleanseFunctions: m.cleanseFunctions || null,
                nullStrategy: m.nullStrategy || 'KEEP',
                processorChain: processorChain
              }
            })
            return this.$axios.post(`/v1/task/${taskId}/mappings`, mappingsData)
          }
        }).then(() => {
          const isEditMode = !!this.taskConfig.id
          this.$message.success(isEditMode ? '草稿修改成功！任务已禁用' : '草稿保存成功！任务已禁用')
          setTimeout(() => {
            this.$router.push('/task')
          }, 800)
        }).catch(err => {
          const errMsg = (err.response && err.response.data && err.response.data.message) || '草稿保存失败'
          this.$message.error(errMsg)
        }).finally(() => {
          loading.close()
        })
      }
    },
    
    // ========== 辅助数据源相关方法 ==========
    
    // 添加辅助数据源
    addAuxiliaryDatasource() {
      this.auxiliaryDatasources.push({
        alias: 'aux' + (this.auxiliaryDatasources.length + 1),
        connectorId: null,
        connectorType: 'DATABASE',
        joinType: 'LEFT',
        joinCondition: {
          mainField: '',
          auxField: ''
        },
        config: {
          sql: '',
          apiPath: '',
          apiMethod: 'GET',
          dataPath: ''
        },
        enabled: 1,
        sortOrder: this.auxiliaryDatasources.length
      })
    },
    
    // 删除辅助数据源
    removeAuxiliaryDatasource(index) {
      this.$confirm('确认删除该辅助数据源吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.auxiliaryDatasources.splice(index, 1)
        this.$message.success('删除成功')
      }).catch(() => {})
    },
    
    // 移动辅助数据源顺序
    moveAuxDatasource(index, direction) {
      const item = this.auxiliaryDatasources[index]
      this.auxiliaryDatasources.splice(index, 1)
      
      if (direction === 'up') {
        this.auxiliaryDatasources.splice(index - 1, 0, item)
      } else {
        this.auxiliaryDatasources.splice(index + 1, 0, item)
      }
      
      this.$message.success('调整成功')
    },
    
    // 辅助数据源连接器变化
    handleAuxConnectorChange(auxDs) {
      const connector = this.connectorList.find(c => c.id === auxDs.connectorId)
      if (connector) {
        auxDs.connectorType = connector.connectorType
      }
    },
    
    // 查询可用字段（用于辅助数据源关联条件的主表字段）
    queryAvailableFields(currentIndex, queryString, cb) {
      const fields = []
      
      // 1. 添加主数据源字段
      if (this.sourcePreviewColumns && this.sourcePreviewColumns.length > 0) {
        this.sourcePreviewColumns.forEach(col => {
          fields.push({
            value: col,
            source: '主数据源'
          })
        })
      }
      
      // 2. 添加之前配置的辅助数据源字段（支持链式关联）
      for (let i = 0; i < currentIndex; i++) {
        const aux = this.auxiliaryDatasources[i]
        if (aux.alias) {
          // 这里无法预先知道辅助数据源的具体字段，提供常用示例
          // 实际使用时用户需要根据辅助数据源的SQL/API结果填写
          fields.push({
            value: aux.alias + '.id',
            source: `辅助: ${aux.alias}（示例）`
          })
          fields.push({
            value: aux.alias + '.name',
            source: `辅助: ${aux.alias}（示例）`
          })
          fields.push({
            value: aux.alias + '.code',
            source: `辅助: ${aux.alias}（示例）`
          })
        }
      }
      
      // 3. 过滤查询
      const results = queryString 
        ? fields.filter(f => f.value.toLowerCase().includes(queryString.toLowerCase()))
        : fields
      
      cb(results)
    },
    
    handleCancel() {
      this.$confirm('确认取消吗? 已配置的内容将丢失', '提示', { type: 'warning' }).then(() => {
        this.$router.push('/task')
      })
    },
    
    // 添加源API参数
    addSourceApiParam() {
      if (!this.sourceConfig.apiParams) {
        this.sourceConfig.apiParams = []
      }
      this.sourceConfig.apiParams.push({ key: '', value: '', enabled: true })
    },
    
    // 删除源API参数
    removeSourceApiParam(index) {
      this.sourceConfig.apiParams.splice(index, 1)
    },
    
    // 添加源API Form-Data参数
    addSourceFormDataParam() {
      if (!this.sourceConfig.formData) {
        this.sourceConfig.formData = []
      }
      this.sourceConfig.formData.push({ key: '', value: '', enabled: true })
    },
    
    // 删除源API Form-Data参数
    removeSourceFormDataParam(index) {
      this.sourceConfig.formData.splice(index, 1)
    },
    
    // 添加源API Header
    addSourceHeader() {
      if (!this.sourceConfig.headers) {
        this.sourceConfig.headers = []
      }
      this.sourceConfig.headers.push({ key: '', value: '', enabled: true })
    },
    
    // 删除源API Header
    removeSourceHeader(index) {
      this.sourceConfig.headers.splice(index, 1)
    },
    
    // 处理源API Body类型变化
    handleSourceBodyTypeChange(newType) {
      // 移除旧的Content-Type
      if (!this.sourceConfig.headers) {
        this.sourceConfig.headers = []
      }
      const contentTypeIndex = this.sourceConfig.headers.findIndex(h => h.key === 'Content-Type')
      if (contentTypeIndex > -1) {
        this.sourceConfig.headers.splice(contentTypeIndex, 1)
      }
      
      // 根据新类型添加Content-Type
      if (newType === 'json') {
        this.sourceConfig.headers.push({ 
          key: 'Content-Type', 
          value: 'application/json', 
          enabled: true 
        })
      } else if (newType === 'form-data') {
        this.sourceConfig.headers.push({ 
          key: 'Content-Type', 
          value: 'multipart/form-data', 
          enabled: true 
        })
      }
    },
    
    // 添加目标API参数
    addTargetApiParam() {
      if (!this.targetConfig.apiParams) {
        this.targetConfig.apiParams = []
      }
      this.targetConfig.apiParams.push({ key: '', value: '', enabled: true, description: '' })
    },
    
    // 删除目标API参数
    removeTargetApiParam(index) {
      this.targetConfig.apiParams.splice(index, 1)
    },
    
    // 添加目标API Header
    addTargetHeader() {
      if (!this.targetConfig.headers) {
        this.targetConfig.headers = []
      }
      this.targetConfig.headers.push({ key: '', value: '', enabled: true })
    },
    
    // 删除目标API Header
    removeTargetHeader(index) {
      this.targetConfig.headers.splice(index, 1)
    },
    
    // 格式化源API的JSON Body
    formatSourceJson() {
      if (!this.sourceConfig.jsonBody || !this.sourceConfig.jsonBody.trim()) {
        this.$message.warning('JSON内容为空')
        return
      }
      try {
        const jsonObj = JSON.parse(this.sourceConfig.jsonBody)
        this.sourceConfig.jsonBody = JSON.stringify(jsonObj, null, 2)
        this.sourceJsonError = ''
        this.$message.success('格式化成功')
      } catch (e) {
        this.sourceJsonError = '格式化失败: ' + e.message
        this.$message.error('JSON格式错误，无法格式化')
      }
    },
    
    // 验证源API的JSON Body
    validateSourceJson() {
      if (!this.sourceConfig.jsonBody || !this.sourceConfig.jsonBody.trim()) {
        this.$message.info('JSON内容为空')
        this.sourceJsonError = ''
        return
      }
      try {
        JSON.parse(this.sourceConfig.jsonBody)
        this.sourceJsonError = ''
        this.$message.success('JSON格式正确')
      } catch (e) {
        this.sourceJsonError = 'JSON格式错误: ' + e.message
        this.$message.error('JSON格式验证失败')
      }
    },
    
    // 清除源API的JSON错误提示
    clearSourceJsonError() {
      if (this.sourceJsonError) {
        this.sourceJsonError = ''
      }
    },
    
    // ========== 推送后处理API配置方法 ==========
    
    // 添加推送后处理API参数
    addPostLoadApiParam() {
      if (!this.postLoadConfig.statusUpdate.params) {
        this.$set(this.postLoadConfig.statusUpdate, 'params', [])
      }
      this.postLoadConfig.statusUpdate.params.push({ key: '', value: '', enabled: true })
    },
    
    // 删除推送后处理API参数
    removePostLoadApiParam(index) {
      this.postLoadConfig.statusUpdate.params.splice(index, 1)
    },
    
    // 添加推送后处理API Header
    addPostLoadApiHeader() {
      if (!this.postLoadConfig.statusUpdate.headers) {
        this.$set(this.postLoadConfig.statusUpdate, 'headers', [])
      }
      this.postLoadConfig.statusUpdate.headers.push({ key: '', value: '', enabled: true })
    },
    
    // 删除推送后处理API Header
    removePostLoadApiHeader(index) {
      this.postLoadConfig.statusUpdate.headers.splice(index, 1)
    },
    
    // 添加推送后处理Form-Data参数
    addPostLoadFormDataParam() {
      if (!this.postLoadConfig.statusUpdate.formData) {
        this.$set(this.postLoadConfig.statusUpdate, 'formData', [])
      }
      this.postLoadConfig.statusUpdate.formData.push({ key: '', value: '', enabled: true })
    },
    
    // 删除推送后处理Form-Data参数
    removePostLoadFormDataParam(index) {
      this.postLoadConfig.statusUpdate.formData.splice(index, 1)
    },
    
    // 处理推送后处理Body类型变化
    handlePostLoadBodyTypeChange(newType) {
      console.log('[PostLoad] Body类型切换:', newType, '当前类型:', this.postLoadConfig.statusUpdate.bodyType)
      
      // 如果是同一类型，也要执行一次（解决首次点击不生效的问题）
      this.postLoadConfig.statusUpdate.bodyType = newType
      
      // 确保headers数组存在
      if (!this.postLoadConfig.statusUpdate.headers) {
        this.$set(this.postLoadConfig.statusUpdate, 'headers', [])
      }
      
      console.log('[PostLoad] 当前headers:', JSON.parse(JSON.stringify(this.postLoadConfig.statusUpdate.headers)))
      
      // 移除旧的Content-Type
      const contentTypeIndex = this.postLoadConfig.statusUpdate.headers.findIndex(h => h.key === 'Content-Type')
      if (contentTypeIndex > -1) {
        this.postLoadConfig.statusUpdate.headers.splice(contentTypeIndex, 1)
        console.log('[PostLoad] 已移除旧的Content-Type')
      }
      
      // 根据新类型添加Content-Type
      if (newType === 'raw') {
        this.postLoadConfig.statusUpdate.headers.push({ 
          key: 'Content-Type', 
          value: 'application/json', 
          enabled: true 
        })
        console.log('[PostLoad] 已添加application/json')
      } else if (newType === 'form-data') {
        this.postLoadConfig.statusUpdate.headers.push({ 
          key: 'Content-Type', 
          value: 'multipart/form-data', 
          enabled: true 
        })
        console.log('[PostLoad] 已添加multipart/form-data')
      }
      
      // 强制更新
      this.$forceUpdate()
      
      console.log('[PostLoad] 最终headers:', JSON.parse(JSON.stringify(this.postLoadConfig.statusUpdate.headers)))
      console.log('[PostLoad] Headers数量:', this.postLoadEnabledHeadersCount)
    },
    
    // 清除推送后处理JSON状态
    clearPostLoadJsonStatus() {
      this.postLoadJsonValid = false
      this.postLoadJsonError = ''
    },
    
    // 验证推送后处理JSON
    validatePostLoadJson() {
      this.postLoadJsonValid = false
      this.postLoadJsonError = ''
      
      if (!this.postLoadConfig.statusUpdate.jsonBody || !this.postLoadConfig.statusUpdate.jsonBody.trim()) {
        this.$message.info('JSON内容为空')
        return
      }
      
      try {
        JSON.parse(this.postLoadConfig.statusUpdate.jsonBody)
        this.postLoadJsonValid = true
        this.$message.success('JSON格式正确')
      } catch (e) {
        this.postLoadJsonError = 'JSON格式错误: ' + e.message
        this.$message.error('JSON格式验证失败')
      }
    },
    
    // 格式化推送后处理JSON
    formatPostLoadJson() {
      this.postLoadJsonValid = false
      this.postLoadJsonError = ''
      
      if (!this.postLoadConfig.statusUpdate.jsonBody || !this.postLoadConfig.statusUpdate.jsonBody.trim()) {
        this.$message.warning('JSON内容为空')
        return
      }
      
      try {
        const jsonObj = JSON.parse(this.postLoadConfig.statusUpdate.jsonBody)
        this.postLoadConfig.statusUpdate.jsonBody = JSON.stringify(jsonObj, null, 2)
        this.postLoadJsonValid = true
        this.$message.success('格式化成功')
      } catch (e) {
        this.postLoadJsonError = '格式化失败: ' + e.message
        this.$message.error('JSON格式错误，无法格式化')
      }
    },
    
    // ==========  扩展功能方法 ==========
    
    // 扩展功能1: 转换预览
    showPreviewDialog(mapping) {
      this.previewMapping = mapping
      this.previewTestValue = ''
      this.previewResult = null
      this.previewError = false
      this.previewDialogVisible = true
    },
    
    getPreviewRule(mapping) {
      const type = mapping.transformType
      if (type === 'DIRECT') return '直接映射'
      if (type === 'CONSTANT') return mapping.constantValue || '-'
      if (type === 'DICT') return `字典ID: ${mapping.dictMappingId || '-'}`
      return '-'
    },
    
    // 获取转换类型显示标签
    getTransformTypeLabel(type) {
      const labels = {
        'DIRECT': '直接映射',
        'DICT': '字典映射',
        'CONSTANT': '固定值'
      }
      return labels[type] || type || '未配置'
    },
    
    async executePreview() {
      if (!this.previewTestValue) {
        this.$message.warning('请输入测试值')
        return
      }
      
      this.previewLoading = true
      this.previewError = false
      
      try {
        const res = await this.$axios.post('/api/transform/preview', {
          transformType: this.previewMapping.transformType,
          testValue: this.previewTestValue,
          transformFunction: this.previewMapping.transformFunction,
          transformScript: this.previewMapping.transformScript,
          dictMappingId: this.previewMapping.dictMappingId,
          defaultValue: this.previewMapping.defaultValue,
          constantValue: this.previewMapping.constantValue
        })
        
        this.previewResult = res.data || ''
        this.$message.success('预览成功')
      } catch (err) {
        this.previewError = true
        this.previewResult = (err.response && err.response.data && err.response.data.message) || err.message || '预览失败'
        this.$message.error('预览失败')
      } finally {
        this.previewLoading = false
      }
    },
    
    // 扩展功能4: 表达式验证（已废弃，FUNCTION类型不再使用）
    async validateExpression(mapping) {
      // FUNCTION类型已废弃，所有函数处理应该在cleanseFunctions中配置
      return
    },
    
    // 扩展功能2: Excel导入
    async handleExcelImport(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      
      try {
        const res = await this.$axios.post('/api/transform/import-excel', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        
        this.mappings = res.data
        this.$message.success(`导入成功！共 ${res.data.length} 条规则`)
      } catch (err) {
        this.$message.error('导入失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    // Excel导出
    async handleExcelExport() {
      if (this.mappings.length === 0) {
        this.$message.warning('暂无可导出的转换规则')
        return
      }
      
      try {
        const res = await this.$axios.post('/api/transform/export-excel', this.mappings, {
          responseType: 'blob'
        })
        
        // 下载文件
        const blob = new Blob([res.data], { 
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
        })
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = `field-mappings-${Date.now()}.xlsx`
        link.click()
        
        this.$message.success('导出成功')
      } catch (err) {
        this.$message.error('导出失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    // 下载Excel模板
    async handleDownloadTemplate() {
      try {
        const res = await this.$axios.get('/api/transform/download-template', {
          responseType: 'blob'
        })
        
        const blob = new Blob([res.data], { 
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' 
        })
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = 'field-mapping-template.xlsx'
        link.click()
        
        this.$message.success('模板下载成功')
      } catch (err) {
        this.$message.error('下载失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    // 扩展功能3: 模板保存
    showTemplateSaveDialog() {
      if (this.mappings.length === 0) {
        this.$message.warning('请先配置转换规则')
        return
      }
      
      this.templateForm = {
        templateName: '',
        templateCode: '',
        description: '',
        tags: ''
      }
      this.templateSaveDialogVisible = true
    },
    
    async saveTemplate() {
      if (!this.templateForm.templateName || !this.templateForm.templateCode) {
        this.$message.warning('请填写模板名称和编码')
        return
      }
      
      try {
        await this.$axios.post('/api/transform/template/save', {
          templateName: this.templateForm.templateName,
          templateCode: this.templateForm.templateCode,
          description: this.templateForm.description,
          tags: this.templateForm.tags,
          mappingsJson: JSON.stringify(this.mappings)
        })
        
        this.$message.success('模板保存成功')
        this.templateSaveDialogVisible = false
      } catch (err) {
        this.$message.error('保存失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    // 显示模板列表
    showTemplateListDialog() {
      this.templateListDialogVisible = true
      this.loadTemplateList()
    },
    
    async loadTemplateList() {
      try {
        const res = await this.$axios.get('/api/transform/template/list', {
          params: {
            page: this.templatePage,
            pageSize: this.templatePageSize,
            keyword: this.templateKeyword
          }
        })
        
        this.templateList = res.data.records
        this.templateTotal = res.data.total
      } catch (err) {
        this.$message.error('加载模板列表失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    handleTemplatePageChange(page) {
      this.templatePage = page
      this.loadTemplateList()
    },
    
    handleTemplatePageSizeChange(size) {
      this.templatePageSize = size
      this.templatePage = 1
      this.loadTemplateList()
    },
    
    async applyTemplate(template) {
      try {
        // 增加使用次数
        await this.$axios.post(`/api/transform/template/${template.id}/apply`)
        
        // 应用模板
        const mappings = JSON.parse(template.mappingsJson)
        this.mappings = mappings
        
        this.$message.success(`已应用模板：${template.templateName}，共 ${mappings.length} 条规则`)
        this.templateListDialogVisible = false
      } catch (err) {
        this.$message.error('应用模板失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
      }
    },
    
    async deleteTemplate(id) {
      try {
        await this.$confirm('确认删除该模板？', '提示', {
          type: 'warning'
        })
        
        await this.$axios.delete(`/api/transform/template/${id}`)
        this.$message.success('删除成功')
        this.loadTemplateList()
      } catch (err) {
        if (err !== 'cancel') {
          this.$message.error('删除失败: ' + ((err.response && err.response.data && err.response.data.message) || err.message))
        }
      }
    },
    
    // 扩展功能5: 虚拟滚动/分页 - 辅助方法
    getActualMappingIndex(pageIndex) {
      return (this.mappingCurrentPage - 1) * this.mappingPageSize + pageIndex
    },
    
    // ========== 草稿自动保存功能 ==========
    
    // 启动自动保存（30秒一次）
    startAutoSave() {
      this.autoSaveTimer = setInterval(() => {
        if (this.hasUnsavedChanges) {
          this.saveDraft()
        }
      }, 30000) // 30秒
    },
    
    // 保存草稿
    saveDraft() {
      const draftData = {
        taskConfig: this.taskConfig,
        sourceConfig: this.sourceConfig,
        targetConfig: this.targetConfig,
        mappings: this.mappings,
        scheduleConfig: this.scheduleConfig,
        activeStep: this.activeStep,
        timestamp: new Date().getTime()
      }
      
      try {
        localStorage.setItem(this.draftKey, JSON.stringify(draftData))
        this.lastSavedData = JSON.stringify(draftData)
        this.hasUnsavedChanges = false
        console.log('草稿已自动保存')
      } catch (e) {
        console.error('草稿保存失败', e)
      }
    },
    
    // 加载草稿
    loadDraft() {
      try {
        const draftStr = localStorage.getItem(this.draftKey)
        if (draftStr) {
          const draftData = JSON.parse(draftStr)
          
          // 检查草稿是否过期（7天）
          const now = new Date().getTime()
          const draftAge = now - (draftData.timestamp || 0)
          const maxAge = 7 * 24 * 60 * 60 * 1000 // 7天
          
          if (draftAge > maxAge) {
            this.clearDraft()
            return
          }
          
          // 提示用户恢复草稿
          this.$confirm('检测到未保存的草稿，是否恢复？', '提示', {
            confirmButtonText: '恢复',
            cancelButtonText: '放弃',
            type: 'info'
          }).then(() => {
            this.taskConfig = draftData.taskConfig || this.taskConfig
            this.sourceConfig = draftData.sourceConfig || this.sourceConfig
            this.targetConfig = draftData.targetConfig || this.targetConfig
            this.mappings = draftData.mappings || this.mappings
            this.scheduleConfig = draftData.scheduleConfig || this.scheduleConfig
            this.activeStep = draftData.activeStep || 0
            
            this.lastSavedData = draftStr
            this.$message.success('草稿已恢复')
          }).catch(() => {
            this.clearDraft()
          })
        }
      } catch (e) {
        console.error('草稿加载失败', e)
      }
    },
    
    // 清除草稿
    clearDraft() {
      try {
        localStorage.removeItem(this.draftKey)
        this.hasUnsavedChanges = false
        this.lastSavedData = null
      } catch (e) {
        console.error('草稿清除失败', e)
      }
    },
    
    // ========== 步骤校验功能 ==========
    
    // 验证当前步骤
    validateCurrentStep() {
      const errors = []
      
      switch (this.activeStep) {
        case 0: // 基本信息
          if (!this.taskConfig.taskName) {
            errors.push('请输入任务名称')
          }
          if (!this.taskConfig.taskCode) {
            errors.push('请输入任务编码')
          }
          if (this.taskConfig.syncMode === 'INCREMENTAL' && !this.taskConfig.incrementalField) {
            errors.push('增量模式需配置增量字段')
          }
          break
          
        case 1: // 源连接器
          if (!this.sourceConfig.connectorId) {
            errors.push('请选择源连接器')
          }
          if (this.sourceConnectorType === 'DATABASE' && !this.sourceConfig.sql) {
            errors.push('请输入SQL查询语句')
          }
          if (this.sourceConnectorType === 'API' && !this.sourceConfig.apiPath) {
            errors.push('请输入API路径')
          }
          if (this.sourcePreviewData.length === 0) {
            errors.push('请先抽取数据预览')
          }
          break
          
        case 3: // 目标连接器
          if (this.multiTargetMode) {
            // 多目标模式验证
            if (this.targets.length === 0) {
              errors.push('请至少添加一个目标表')
            } else {
              // 验证每个目标
              this.targets.forEach((target, index) => {
                if (!target.targetConnectorId) {
                  errors.push(`目标${index + 1}: 请选择目标连接器`)
                }
                if (!target.targetConfig.tableName) {
                  errors.push(`目标${index + 1}: 请输入目标表名`)
                }
              })
            }
          } else {
            // 单目标模式验证
            if (!this.targetConfig.connectorId) {
              errors.push('请选择目标连接器')
            }
            if (this.targetConnectorType === 'DATABASE' && !this.targetConfig.tableName) {
              errors.push('请输入目标表名')
            }
            if (this.targetConnectorType === 'API' && !this.targetConfig.apiPath) {
              errors.push('请输入API路径')
            }
          }
          break
          
        case 4: // 数据转换
          if (this.mappings.length === 0) {
            errors.push('请至少配置一个字段映射')
          } else if (this.multiTargetMode) {
            // 多目标模式：验证每个目标都有字段映射
            this.targets.forEach((target, index) => {
              const targetMappingCount = this.getTargetMappingCount(index)
              if (targetMappingCount === 0) {
                errors.push(`目标${index + 1}(「${target.targetName}」): 请配置字段映射`)
              }
            })
          }
          break
          
        case 5: // 调度配置
          if (this.scheduleConfig.scheduleType === 'CRON' && !this.scheduleConfig.cronExpression) {
            errors.push('请输入Cron表达式')
          }
          break
      }
      
      this.$set(this.stepValidationErrors, this.activeStep, errors)
      
      if (errors.length > 0) {
        this.$message.error(errors.join('；'))
        return false
      }
      
      return true
    },
    
    // 获取步骤错误信息
    getStepError(step) {
      return this.stepValidationErrors[step] || []
    },
    
    // 检查步骤是否有错误
    hasStepError(step) {
      const errors = this.stepValidationErrors[step]
      return errors && errors.length > 0
    }
  }
}
</script>

<style scoped>
/* 防止横向滚动条 */
::v-deep body,
::v-deep html {
  overflow-x: hidden !important;
}

/* ========== 现代化向导页样式 ========== */
.task-wizard-page {
  padding: 20px;
  overflow-x: hidden;
}

.wizard-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  position: relative;
  height: calc(100vh - 80px);
}

/* card body 布局 */
::v-deep .wizard-card .el-card__body {
  height: calc(100% - 200px);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
}

/* 滚动条美化 */
::v-deep .wizard-card .el-card__body::-webkit-scrollbar {
  width: 8px;
}

::v-deep .wizard-card .el-card__body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

::v-deep .wizard-card .el-card__body::-webkit-scrollbar-thumb {
  background: #3b82f6;
  border-radius: 4px;
}

::v-deep .wizard-card .el-card__body::-webkit-scrollbar-thumb:hover {
  background: #2563eb;
}

/* 头部区域 */
.wizard-header-section {
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  padding: 0 !important;
}

.header-title-area {
  display: flex;
  align-items: center;
  padding: 24px 32px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.title-icon-wrapper {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.title-icon-wrapper.create-mode {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
}

.title-icon-wrapper.edit-mode {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.title-text-wrapper h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.3;
}

.title-text-wrapper .subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
  font-weight: 400;
}

.title-text-wrapper .subtitle.placeholder-subtitle {
  color: #9ca3af;
  font-style: italic;
}

/* 步骤条样式 */
.modern-steps {
  padding: 28px 32px 24px;
}

/* ========== 重新设计的步骤条 ========== */
.wizard-steps-modern {
  padding: 32px 40px 28px;
  background: white;
}

.steps-container {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  max-width: 1200px;
  margin: 0 auto;
}

.step-item-modern {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s;
}

/* 步骤圆圈 */
.step-circle {
  width: 56px;
  height: 56px;
  position: relative;
  margin-bottom: 12px;
  z-index: 2;
}

.circle-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.circle-content {
  position: absolute;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #9ca3af;
  z-index: 1;
  transition: all 0.4s;
}

.circle-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 3px solid transparent;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 已完成状态 */
.step-item-modern.is-completed .circle-bg {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  box-shadow: 0 8px 16px rgba(16, 185, 129, 0.25);
}

.step-item-modern.is-completed .circle-content {
  color: white;
}

.step-item-modern.is-completed .step-check {
  font-size: 28px;
  font-weight: bold;
  animation: checkBounce 0.5s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

@keyframes checkBounce {
  0% { transform: scale(0); opacity: 0; }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

/* 当前步骤状态 */
.step-item-modern.is-current .circle-bg {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35),
              0 0 0 0 rgba(59, 130, 246, 0.4);
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.step-item-modern.is-current .circle-content {
  color: white;
}

.step-item-modern.is-current .circle-ring {
  border-color: rgba(59, 130, 246, 0.2);
  transform: scale(1.15);
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35),
                0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 8px 20px rgba(59, 130, 246, 0.35),
                0 0 0 10px rgba(59, 130, 246, 0);
  }
}

/* 待完成状态 */
.step-item-modern.is-upcoming .circle-bg {
  background: linear-gradient(135deg, #ffffff 0%, #f9fafb 100%);
  border: 2px solid #e5e7eb;
}

.step-item-modern.is-upcoming .circle-content {
  color: #d1d5db;
}

/* 悬停效果 */
.step-item-modern:hover:not(.is-current) .circle-bg {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
}

.step-item-modern:hover:not(.is-current) .circle-ring {
  border-color: rgba(59, 130, 246, 0.3);
  transform: scale(1.1);
}

/* 步骤信息 */
.step-info {
  text-align: center;
  transition: all 0.3s;
}

.step-label {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  color: #9ca3af;
  margin-bottom: 4px;
  transition: all 0.3s;
}

.step-item-modern.is-current .step-label,
.step-item-modern.is-completed .step-label {
  color: #3b82f6;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  line-height: 1.4;
  transition: all 0.3s;
}

.step-item-modern.is-current .step-title {
  color: #1f2937;
  font-size: 15px;
}

.step-item-modern.is-completed .step-title {
  color: #374151;
}

/* 连接线 */
.step-connector {
  position: absolute;
  top: 28px;
  left: calc(50% + 28px);
  right: calc(-50% + 28px);
  height: 3px;
  z-index: 1;
}

.connector-line {
  position: absolute;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, #e5e7eb 0%, #f3f4f6 50%, #e5e7eb 100%);
  border-radius: 2px;
}

.connector-progress {
  position: absolute;
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  border-radius: 2px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.5);
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .wizard-steps-modern {
    padding: 24px 20px;
  }
  
  .step-circle {
    width: 48px;
    height: 48px;
  }
  
  .step-label {
    font-size: 10px;
  }
  
  .step-title {
    font-size: 13px;
  }
}

/* 原有步骤条样式（保留以防其他地方使用） */
.modern-steps-old {
  padding: 28px 32px 24px;
}

/* 覆盖 Element UI 步骤条默认样式 */
::v-deep .modern-steps.el-steps {
  background: transparent;
}

::v-deep .modern-steps .el-step__head {
  border-color: #e5e7eb;
}

::v-deep .modern-steps .el-step__head.is-process {
  color: #3b82f6;
  border-color: #3b82f6;
}

::v-deep .modern-steps .el-step__head.is-finish {
  color: #10b981;
  border-color: #10b981;
}

::v-deep .modern-steps .el-step__head.is-wait {
  color: #d1d5db;
  border-color: #e5e7eb;
}

::v-deep .modern-steps .el-step__head.is-process .el-step__icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  font-weight: 600;
}

::v-deep .modern-steps .el-step__head.is-finish .el-step__icon {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-color: #10b981;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.2);
}

::v-deep .modern-steps .el-step__head.is-wait .el-step__icon {
  background: #f9fafb;
  border-color: #e5e7eb;
  color: #9ca3af;
}

::v-deep .modern-steps .el-step__title {
  font-size: 14px;
  font-weight: 500;
  margin-top: 8px;
}

::v-deep .modern-steps .el-step__title.is-process {
  color: #1f2937;
  font-weight: 600;
}

::v-deep .modern-steps .el-step__title.is-finish {
  color: #10b981;
}

::v-deep .modern-steps .el-step__title.is-wait {
  color: #9ca3af;
}

::v-deep .modern-steps .el-step__line {
  background-color: #e5e7eb;
}

::v-deep .modern-steps .el-step__line-inner {
  border-width: 2px !important;
}

::v-deep .modern-steps .el-step.is-horizontal .el-step__line {
  top: 14px;
  height: 2px;
}

/* 步骤内容区域 */
.step-content {
  padding: 20px;
  padding-bottom: 120px;
}

/* 其他步骤容器也需要相同样式 */
.step-content-mapping,
.schedule-config-wrapper,
.postload-config-wrapper,
.step-complete {
  padding: 20px;
  padding-bottom: 120px;
}

/* 固定底部按钮 */
.wizard-footer-fixed {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: white;
  z-index: 100;
  border-top: 1px solid #ebeef5;
}

/* 表单优化 */
::v-deep .step-content .el-form-item__label {
  font-weight: 500;
  color: #374151;
}

::v-deep .step-content .el-input__inner,
::v-deep .step-content .el-textarea__inner {
  border-radius: 6px;
  border-color: #e5e7eb;
  transition: all 0.2s;
}

::v-deep .step-content .el-input__inner:focus,
::v-deep .step-content .el-textarea__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

::v-deep .step-content .el-button {
  border-radius: 6px;
  font-weight: 500;
  transition: all 0.2s;
}

::v-deep .step-content .el-button--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border: none;
}

::v-deep .step-content .el-button--primary:hover {
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
  transform: translateY(-1px);
}

/* ========== 卡片式表单区域 ========== */
.form-section-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #e8e8e8;
  margin-bottom: 24px;
  overflow: hidden;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.form-section-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: #d1d5db;
}

/* 卡片头部 */
.section-header {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, #f9fafb 0%, #ffffff 100%);
  border-bottom: 1px solid #f0f0f0;
}

.section-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: #ffffff !important;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.25);
}

.section-icon i {
  color: #ffffff !important;
  opacity: 1 !important;
  font-weight: 900 !important;
  -webkit-text-stroke: 0.5px white;
}

.section-title h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.3;
}

.section-title p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
  font-weight: 400;
}

/* 卡片主体 */
.section-body {
  padding: 28px 24px;
}

/* 现代化表单 */
.modern-form {
  max-width: 800px;
  margin: 0 auto;  /* 水平居中 */
}

.form-item-enhanced {
  margin-bottom: 28px;
}

::v-deep .form-item-enhanced .el-form-item__label {
  font-weight: 500;
  color: #374151;
  font-size: 14px;
  line-height: 40px;
}

/* 增强输入框 */
.input-enhanced {
  width: 100%;
  max-width: 600px;
}

::v-deep .input-enhanced .el-input__inner {
  height: 44px;
  line-height: 44px;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  padding-left: 42px;
  font-size: 14px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

::v-deep .input-enhanced .el-input__inner:hover {
  border-color: #3b82f6;
}

::v-deep .input-enhanced .el-input__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

::v-deep .input-enhanced .el-input__prefix {
  left: 12px;
  font-size: 18px;
  color: #9ca3af;
  transition: color 0.3s;
}

::v-deep .input-enhanced.is-focus .el-input__prefix,
::v-deep .input-enhanced:hover .el-input__prefix {
  color: #3b82f6;
}

/* 增强文本域 */
::v-deep .textarea-enhanced .el-textarea__inner {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  font-size: 14px;
  padding: 12px 14px;
  line-height: 1.6;
  transition: all 0.3s;
}

::v-deep .textarea-enhanced .el-textarea__inner:hover {
  border-color: #3b82f6;
}

::v-deep .textarea-enhanced .el-textarea__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.1);
}

/* 字段提示 */
.field-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.6;
}

.field-tip i {
  color: #9ca3af;
  font-size: 14px;
  margin-top: 2px;
}

/* 模式选择器 */
.mode-selector {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 700px;
}

.mode-card {
  position: relative;
  padding: 20px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  display: flex;
  align-items: center;
  gap: 14px;
}

.mode-card:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.02) 0%, rgba(59, 130, 246, 0.05) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.12);
}

.mode-card.is-active {
  border-color: #3b82f6;
  border-width: 2px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(59, 130, 246, 0.08) 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.mode-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  transition: all 0.3s;
}

.mode-card.is-active .mode-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.mode-info {
  flex: 1;
}

.mode-info h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.mode-info p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.mode-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: transparent;
  flex-shrink: 0;
  transition: all 0.3s;
}

.mode-card.is-active .mode-check {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: white;
}

/* 信息提示框 */
.info-box {
  margin-top: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 10px;
  padding: 16px;
}

.info-box-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #0369a1;
  font-size: 14px;
}

.info-box-header i {
  font-size: 16px;
}

.info-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.info-list > li {
  padding: 14px;
  background: white;
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid #e0f2fe;
  transition: all 0.2s;
}

.info-list > li:last-child {
  margin-bottom: 0;
}

.info-list > li:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.info-list > li strong {
  display: block;
  color: #1f2937;
  font-size: 14px;
  margin-bottom: 6px;
}

.info-list > li p {
  color: #6b7280;
  font-size: 13px;
  margin: 6px 0 8px;
  line-height: 1.6;
}

.list-badge {
  display: inline-block;
  padding: 2px 10px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  font-size: 11px;
  border-radius: 4px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-right: 8px;
}

.list-badge.advanced {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
}

.code-example,
.code-result {
  display: inline-block;
  background: #1e293b;
  color: #e2e8f0;
  padding: 6px 12px;
  border-radius: 6px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  margin: 4px 0;
  border: 1px solid #334155;
}

.code-result {
  background: #0f172a;
  color: #10b981;
}

.code-arrow {
  margin: 0 8px;
  color: #3b82f6;
  font-weight: bold;
}

.success-tip {
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%) !important;
  border-color: #6ee7b7 !important;
  color: #047857 !important;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px !important;
  font-weight: 500 !important;
}

.success-tip i {
  font-size: 16px;
  color: #10b981;
}

/* 表单分组 */
.form-group {
  margin-bottom: 0;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}

.group-title i {
  font-size: 18px;
  color: #3b82f6;
}

.section-divider {
  margin: 32px 0;
}

::v-deep .section-divider .el-divider__text {
  background: white;
  padding: 0 20px;
  color: #9ca3af;
  font-weight: 500;
}

/* 紧凑工具栏 */
.compact-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 12px;
  border: 1px solid #e9ecef;
}

.toolbar-tip {
  font-size: 12px;
  color: #6b7280;
  margin-left: auto;
}

/* SQL编辑器增强版 */
.sql-editor-enhanced {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
  background: white;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-bottom: 1px solid #e5e7eb;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.editor-label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 6px;
}

.editor-label i {
  color: #3b82f6;
}

.sql-error-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #fee2e2;
  color: #dc2626;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.sql-valid-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: #d1fae5;
  color: #059669;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.sql-input-wrapper {
  padding: 0;
}

.editor-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.actions-left .tip-text {
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
}

.actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.extract-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #d1fae5;
  color: #059669;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

/* ==================== 数据预览卡片 ==================== */

/* 外层区域 - 真正占满100%屏幕宽度 */
.data-preview-card-section {
  position: relative;
  left: 50%;
  right: 50%;
  margin-left: -50vw;
  margin-right: -50vw;
  width: 100vw;
  max-width: 100vw;
  margin-top: 24px;
  padding: 0;
  box-sizing: border-box;
  overflow: hidden;
}

/* 预览卡片容器 */
.preview-card-container {
  border: 1px solid #e5e7eb;
  border-left: none;    /* 移除左边框 */
  border-right: none;   /* 移除右边框 */
  border-radius: 0;     /* 移除圆角 */
  overflow: hidden;
  background: white;
  width: 100%;
}

/* 卡片头部栏 */
.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.header-left-section {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-left-section i {
  font-size: 18px;
  color: #3b82f6;
}

.header-title-text {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.header-right-section {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}

.info-badge {
  color: #3b82f6;
  font-weight: 500;
}

.info-divider {
  color: #d1d5db;
}

/* 卡片主体内容 */
.card-body-content {
  padding: 20px 24px;
  overflow-x: auto;
}

/* 滚动条美化 */
.card-body-content::-webkit-scrollbar {
  height: 8px;
}

.card-body-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.card-body-content::-webkit-scrollbar-thumb {
  background: #3b82f6;
  border-radius: 4px;
}

.card-body-content::-webkit-scrollbar-thumb:hover {
  background: #2563eb;
}

/* 旧样式删除（保留以便备份） */
.compact-preview-section {
  margin-top: 20px !important;
  padding: 0 !important;
  background: white;
}

.compact-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  margin-bottom: 16px;
  border-bottom: 2px solid #e5e7eb;
}

.preview-title {
  font-size: 14px;
  font-weight: 600;
  color: #0c4a6e;
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-title i {
  font-size: 16px;
}

.preview-info {
  display: flex;
  gap: 8px;
}

.compact-preview-table-wrapper {
  position: relative;
  left: 50%;
  right: 50%;
  margin-left: -50vw !important;
  margin-right: -50vw !important;
  width: 100vw !important;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 0 20px 20px 20px;
  background: #f8f9fa;
  border-top: 2px solid #3b82f6;
  border-bottom: 2px solid #e5e7eb;
}

.table-inner-wrapper {
  display: inline-block;
  min-width: 100%;
}

.table-inner-wrapper .el-table {
  width: auto !important;
}

.compact-preview-table-wrapper::-webkit-scrollbar {
  height: 8px;
}

.compact-preview-table-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.compact-preview-table-wrapper::-webkit-scrollbar-thumb {
  background: #3b82f6;
  border-radius: 4px;
}

.compact-preview-table-wrapper::-webkit-scrollbar-thumb:hover {
  background: #2563eb;
}

/* 原有工具栏样式 */
.toolbar-box {
  background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f5 100%);
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 16px;
}

.toolbar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 2px solid #dee2e6;
}

.toolbar-header i {
  font-size: 18px;
  color: #3b82f6;
}

.toolbar-content {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

/* 编辑器样式 */
.editor-box {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: white;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-bottom: 1px solid #e5e7eb;
}

.editor-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.editor-title i {
  font-size: 16px;
  color: #3b82f6;
}

.editor-tips {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.editor-tips code {
  padding: 2px 6px;
  background: #fef3c7;
  color: #d97706;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.sql-editor {
  border: none;
}

/* SQL编辑器容器：双层结构实现语法高亮 */
.sql-editor-container {
  position: relative;
  width: 100%;
  min-height: 160px;
  font-family: 'Courier New', Consolas, Monaco, 'Lucida Console', monospace;
  font-size: 14px;
  line-height: 1.8;
  overflow: hidden;
}

.sql-editor-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 14px 16px;
  background: #fafafa;
  pointer-events: none;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  z-index: 1;
  line-height: 1.8;
  font-family: 'Courier New', Consolas, Monaco, 'Lucida Console', monospace;
  font-size: 14px;
}

.sql-editor-input {
  position: relative;
  width: 100%;
  min-height: 160px;
  padding: 14px 16px;
  background: transparent;
  color: transparent;
  caret-color: #3b82f6;
  border: none;
  outline: none;
  resize: vertical;
  overflow: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  z-index: 2;
  font-family: inherit;
  font-size: inherit;
  line-height: 1.8;
}

.sql-editor-input:focus {
  background: rgba(255, 255, 255, 0.5);
}

.sql-editor-input::placeholder {
  color: #9ca3af;
  font-style: italic;
}

/* SQL语法高亮颜色 - 增强对比 */
::v-deep .sql-keyword {
  color: #1d4ed8;
  font-weight: 700;
  text-transform: uppercase;
}

::v-deep .sql-string {
  color: #047857;
  font-weight: 500;
}

::v-deep .sql-number {
  color: #dc2626;
  font-weight: 600;
}

::v-deep .sql-variable {
  color: #ea580c;
  font-weight: 700;
  background: #fed7aa;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #fdba74;
}

::v-deep .sql-comment {
  color: #16a34a;  /* 绿色注释 */
  font-style: italic;
}

::v-deep .sql-function {
  color: #7c3aed;  /* 紫色函数名 */
  font-weight: 600;
}

::v-deep .sql-editor .el-textarea__inner {
  border: none;
  border-radius: 0;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 16px;
  background: #fafafa;
}

::v-deep .sql-editor .el-textarea__inner:focus {
  background: white;
}

.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

/* 预览样式 */
.preview-box {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

/* 独立数据预览区域 */
.data-preview-section {
  margin-top: 32px;
  padding: 0;
}

.preview-section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  margin-bottom: 16px;
  border-bottom: 2px solid #e5e7eb;
}

.preview-section-header i:first-child {
  font-size: 20px;
  color: #3b82f6;
}

.preview-section-header > span {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
}

.preview-count {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.preview-count i {
  font-size: 14px;
}

/* 宽预览样式 */
.preview-box-wide {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background: white;
}

.preview-scroll-container {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}

/* 滚动条样式 */
.preview-scroll-container::-webkit-scrollbar {
  height: 8px;
}

.preview-scroll-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.preview-scroll-container::-webkit-scrollbar-thumb {
  background: #3b82f6;
  border-radius: 4px;
}

.preview-scroll-container::-webkit-scrollbar-thumb:hover {
  background: #2563eb;
}

/* 全宽预览项 */
.preview-item-full {
  margin-left: -110px;
  margin-right: -40px;
  padding: 0 40px;
}

.preview-item-full ::v-deep .el-form-item__label {
  float: none;
  display: block;
  text-align: left;
  width: 100% !important;
  margin-bottom: 16px;
  padding-left: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.preview-item-full ::v-deep .el-form-item__content {
  margin-left: 0 !important;
  width: 100%;
}

.preview-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
  font-size: 13px;
  color: #6b7280;
}

.preview-footer i {
  color: #3b82f6;
}

/* 成功徽章 */
.success-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-left: 12px;
  padding: 4px 12px;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.success-badge i {
  font-size: 14px;
}

/* 现代化按钮 */
.btn-modern {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-modern:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(59, 130, 246, 0.25);
}

/* 现代化表格 */
.modern-table {
  border-radius: 8px;
  overflow: hidden;
}

::v-deep .modern-table th {
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  color: #374151;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 10px;
}

::v-deep .modern-table td {
  padding: 10px;
  font-size: 13px;
}

::v-deep .modern-table .el-table__body tr:hover > td {
  background: #f0f9ff !important;
}

/* 表格斑马纹 */
::v-deep .modern-table .even-row {
  background: #fafafa;
}

::v-deep .modern-table .odd-row {
  background: white;
}

::v-deep .modern-table .even-row:hover,
::v-deep .modern-table .odd-row:hover {
  background: #f0f9ff !important;
}

/* 原有样式 */
.tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
  margin-top: 5px;
}

.el-step__title {
  font-size: 14px;
}

::v-deep .el-step__head.is-finish {
  color: #67C23A;
  border-color: #67C23A;
}

::v-deep .el-step__title.is-finish {
  color: #67C23A;
}

/* JSON编辑器样式 */
.json-editor ::v-deep textarea {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace !important;
  font-size: 13px !important;
  line-height: 1.6 !important;
  background-color: #2d2d2d !important;
  color: #a9b7c6 !important;
  border: 1px solid #3c3f41 !important;
  border-radius: 4px !important;
  padding: 12px !important;
}

.json-editor ::v-deep textarea::placeholder {
  color: #6a737d !important;
}

.json-editor.json-error ::v-deep textarea {
  border-color: #F56C6C;
  background-color: #fef0f0;
  color: #F56C6C;
}

/* 数据转换卡片样式 */
.transform-card {
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.transform-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transform: translateY(-2px);
}

/* 代码样式 */
code {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
}

/* 处理器项样式 */
.processor-item {
  background: #f5f7fa;
  border: 1px solid transparent;
}

.processor-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.processor-item:hover .delete-btn {
  opacity: 1 !important;
}

.processor-item.dragging {
  opacity: 0.5;
  background: #e6f7ff;
  border-color: #91d5ff;
}

.processor-item .drag-handle {
  opacity: 0.6;
  transition: opacity 0.2s;
}

.processor-item:hover .drag-handle {
  opacity: 1;
  color: #409EFF;
}

/* ==================== 数据预览弹窗样式 ==================== */
.preview-dialog ::v-deep .el-dialog__body {
  padding: 20px;
}

.preview-dialog ::v-deep .el-dialog__footer {
  text-align: center;
  padding: 15px 20px;
  border-top: 1px solid #e5e7eb;
}

.preview-dialog .dialog-footer .el-button {
  min-width: 100px;
}

/* ==================== 数据预览抽屉样式 ==================== */
.drawer-content {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-stats {
  padding: 20px 24px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-table-container {
  flex: 1;
  padding: 20px 24px;
  overflow: auto;
}

/* 抽屉标题样式 */
::v-deep .el-drawer__header {
  padding: 20px 24px;
  margin-bottom: 0;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

::v-deep .el-drawer__title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

::v-deep .el-drawer__body {
  padding: 0;
}

/* ==================== 辅助数据源步骤样式 ==================== */
.auxiliary-datasource-step {
  padding: 24px;
  padding-bottom: 150px;
  background: #f9fafb;
  min-height: calc(100vh - 300px);
}

/* 步骤头部 */
.step-header {
  margin-bottom: 24px;
}

.header-title {
  display: flex;
  align-items: center;
}

/* 功能说明卡片 */
.info-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

.info-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 16px;
}

.info-card-header i {
  font-size: 18px;
}

.info-card-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #1f2937;
  line-height: 1.6;
}

.info-item i {
  color: #10b981;
  font-size: 16px;
  margin-top: 2px;
  flex-shrink: 0;
}

.info-item.example {
  background: rgba(255, 255, 255, 0.6);
  padding: 12px;
  border-radius: 8px;
  margin-top: 6px;
}

.info-item.example i {
  color: #f59e0b;
}

.inline-code {
  background: rgba(59, 130, 246, 0.1);
  color: #1e40af;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  font-weight: 500;
}

.inline-code.highlight {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  font-weight: 600;
}

/* 操作栏 */
.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.add-button {
  font-weight: 500;
  padding: 10px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.2);
  transition: all 0.3s;
}

.add-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.status-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.help-icon {
  color: #3b82f6;
  font-size: 18px;
  cursor: pointer;
  transition: all 0.3s;
}

.help-icon:hover {
  color: #2563eb;
  transform: scale(1.1);
}

/* 数据源列表 */
.datasource-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 数据源卡片 */
.datasource-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s;
  border: 1px solid #e5e7eb;
}

.datasource-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 1px solid #e5e7eb;
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 15px;
}

.order-tag {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  border-radius: 6px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.card-body {
  padding: 24px;
}

/* 表单布局 */
.form-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-group {
  flex: 1;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 8px;
}

.form-label.required::after {
  content: '*';
  color: #f56c6c;
  margin-left: 2px;
}

.form-label i {
  color: #6b7280;
  font-size: 14px;
}

.form-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 6px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.5;
}

.form-tip i {
  font-size: 13px;
}

.form-tip.success {
  color: #065f46;
  background: #f0fdf4;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #10b981;
  font-weight: 500;
}

.form-tip.success i {
  color: #10b981;
  font-size: 14px;
}

/* 增强输入框 */
.enhanced-input ::v-deep .el-input__inner {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 0.3s;
}

.enhanced-input ::v-deep .el-input__inner:hover {
  border-color: #3b82f6;
}

.enhanced-input ::v-deep .el-input__inner:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.enhanced-select ::v-deep .el-input__inner {
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 0.3s;
}

.enhanced-select ::v-deep .el-input__inner:hover {
  border-color: #3b82f6;
}

/* SQL 编辑器 */
.sql-editor-wrapper {
  position: relative;
}

.sql-input ::v-deep textarea {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  border-radius: 8px;
  border: 1.5px solid #e5e7eb;
  transition: all 0.3s;
}

.sql-input ::v-deep textarea:hover {
  border-color: #3b82f6;
}

.sql-input ::v-deep textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

/* 辅助数据源SQL编辑器特殊样式 */
.aux-sql-editor {
  margin-top: 0;
}

.aux-sql-editor .sql-editor-input {
  min-height: 100px !important;
  height: auto !important;
}

.aux-sql-editor .editor-toolbar {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 8px 8px 0 0;
}

.aux-sql-editor .sql-editor-container {
  border: 1px solid #e5e7eb;
  border-top: none;
  border-radius: 0 0 8px 8px;
}

/* API 配置 */
.api-config ::v-deep .el-input-group__prepend {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border: none;
  font-weight: 600;
}

/* 关联条件区域 */
.join-condition-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px dashed #e5e7eb;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 16px;
}

.section-title i {
  color: #3b82f6;
  font-size: 16px;
}

.join-operator {
  flex: 0 0 60px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 8px;
}

.operator-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  font-size: 20px;
  font-weight: 700;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-icon {
  font-size: 72px;
  color: #d1d5db;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.empty-state h4 {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
  line-height: 1.6;
}

/* ==================== 目标连接器步骤样式 ==================== */
.target-config-step {
  padding: 24px;
  padding-bottom: 150px;
  background: #f9fafb;
  min-height: calc(100vh - 300px);
}

/* 模式选择器卡片 */
.mode-selector-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
}

.mode-selector-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-bottom: 1px solid #e5e7eb;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
}

.mode-selector-header i {
  color: #3b82f6;
  font-size: 18px;
}

.mode-selector-body {
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.mode-option {
  position: relative;
  padding: 20px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  display: flex;
  align-items: center;
  gap: 14px;
}

.mode-option:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.02) 0%, rgba(59, 130, 246, 0.05) 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.12);
}

.mode-option.active {
  border-color: #3b82f6;
  border-width: 2px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(59, 130, 246, 0.08) 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.mode-option .mode-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  transition: all 0.3s;
}

.mode-option.active .mode-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.mode-option .mode-content {
  flex: 1;
}

.mode-option .mode-content h4 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.4;
}

.mode-option .mode-content p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.5;
}

.mode-option .mode-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: transparent;
  flex-shrink: 0;
  transition: all 0.3s;
}

.mode-option.active .mode-check {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: white;
}

/* 单目标配置 */
.single-target-config {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 配置卡片 */
.config-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  border: 1px solid #e5e7eb;
  transition: all 0.3s;
}

.config-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

/* 写入模式选择器 */
.write-mode-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.write-mode-item {
  position: relative;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
}

.write-mode-item:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.02) 0%, rgba(59, 130, 246, 0.05) 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.12);
}

.write-mode-item.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, rgba(59, 130, 246, 0.08) 100%);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
}

.mode-icon-sm {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #3b82f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  transition: all 0.3s;
}

.write-mode-item.active .mode-icon-sm {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.mode-info-sm {
  flex: 1;
}

.mode-info-sm h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.mode-info-sm p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.mode-check-sm {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: transparent;
  transition: all 0.3s;
  position: absolute;
  top: 8px;
  right: 8px;
}

.write-mode-item.active .mode-check-sm {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  border-color: #3b82f6;
  color: white;
}

/* 配置面板 */
.config-panel {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  border-radius: 10px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-bottom: 1px solid #bae6fd;
  font-size: 14px;
  font-weight: 600;
  color: #0369a1;
}

.panel-header i {
  font-size: 16px;
}

.panel-body {
  padding: 16px;
}

.panel-label {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.panel-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

/* ========== 现代化Tab标签页样式 ========== */
.transform-tabs-nav {
  display: flex;
  gap: 0;
  background: #fafbfc;
  border: 1px solid #e5e7eb;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  overflow: hidden;
  margin-bottom: 0;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border-right: 1px solid #e5e7eb;
}

.tab-item:last-child {
  border-right: none;
}

.tab-item::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-item:hover {
  background: rgba(255, 255, 255, 0.5);
}

.tab-item.active {
  background: white;
}

.tab-item.active::before {
  background: linear-gradient(90deg, #3b82f6 0%, #60a5fa 100%);
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.4);
}

.tab-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-radius: 8px;
  font-size: 18px;
  color: #6b7280;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.tab-item:hover .tab-icon {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
}

.tab-item.active .tab-icon {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #3b82f6;
  box-shadow: 0 2px 6px rgba(59, 130, 246, 0.2);
}

.tab-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.tab-title {
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  transition: all 0.3s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-item.active .tab-title {
  color: #1f2937;
  font-weight: 600;
}

.tab-count {
  font-size: 12px;
  color: #9ca3af;
  transition: all 0.3s;
}

.tab-item.active .tab-count {
  color: #3b82f6;
  font-weight: 500;
}

.tab-badge {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3), 0 0 0 2px rgba(16, 185, 129, 0.1);
}

.transform-content-area {
  background: white;
  padding: 24px;
  border: 1px solid #e5e7eb;
  border-radius: 0 0 8px 8px;
  min-height: 400px;
}

.transform-step-content {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ========== 调度配置页面样式 ========== */
.schedule-config-wrapper {
  padding: 20px;
  padding-bottom: 150px;
}

.schedule-config-container {
  max-width: 900px;
  margin: 0 auto;
}

.config-section {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 20px;
}

.config-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f3f4f6;
}

.section-header i {
  font-size: 18px;
  color: #3b82f6;
}

.schedule-type-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.type-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fafbfc;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.type-card:hover {
  border-color: #cbd5e1;
  background: #f9fafb;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.type-card.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.card-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 8px;
  font-size: 24px;
  color: #6b7280;
  flex-shrink: 0;
  transition: all 0.3s;
}

.type-card.active .card-icon {
  background: #3b82f6;
  color: white;
}

.card-content {
  flex: 1;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.card-desc {
  font-size: 13px;
  color: #6b7280;
  line-height: 1.4;
}

.field-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
}

.field-tip i {
  color: #3b82f6;
  font-size: 14px;
}

.field-tip.warning {
  background: #fffbeb;
}

.field-tip.warning i {
  color: #f59e0b;
}

/* ========== 推送后处理页面样式 ========== */
.postload-config-wrapper {
  padding: 20px;
  padding-bottom: 150px;
}

.postload-config-container {
  max-width: 900px;
  margin: 0 auto;
}

.info-banner {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  margin-bottom: 20px;
}

.banner-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border-radius: 8px;
  font-size: 20px;
  color: #3b82f6;
  flex-shrink: 0;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 4px;
}

.banner-desc {
  font-size: 13px;
  color: #1e3a8a;
  line-height: 1.5;
}

.update-type-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.sql-editor,
.json-editor {
  font-family: 'Courier New', Consolas, monospace;
}

.sql-editor ::v-deep textarea {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.json-editor ::v-deep textarea {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.api-config-tabs {
  box-shadow: none;
}

.api-config-tabs ::v-deep .el-tabs__header {
  background: #fafbfc;
  border-bottom: 1px solid #e5e7eb;
}

.api-config-tabs ::v-deep .el-tabs__content {
  padding: 16px;
}

.tab-toolbar {
  margin-bottom: 12px;
}

.json-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

/* ========== 完成页面样式 ========== */
.step-complete {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 500px;
  padding: 40px 20px;
  padding-bottom: 150px;
}

.complete-wrapper {
  width: 100%;
  max-width: 900px;
  transform: scale(0.85);
  transform-origin: center;
}

.complete-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 30px;
}

.complete-icon {
  font-size: 64px;
  color: #67c23a;
  flex-shrink: 0;
}

.complete-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.complete-text p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.complete-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 30px;
}

.info-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.info-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 12px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-item .label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  min-width: 80px;
}

.info-item .value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  flex: 1;
}

.complete-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
  border-top: 1px solid #e5e7eb;
}

/* Cron模板列表样式 */
.cron-template-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.cron-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  color: #374151;
}

.cron-item:hover {
  border-color: #d1d5db;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.cron-item.active {
  border-color: #10b981;
  background: #f0fdf4;
  color: #047857;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.15);
}

.cron-item .cron-icon-wrapper {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;
  transition: all 0.25s ease;
  position: relative;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.cron-item:hover .cron-icon-wrapper {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.2);
}

/* 图标渐变背景 */
.icon-green {
  background: linear-gradient(135deg, #34d399 0%, #10b981 100%);
}

.icon-blue {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}

.icon-cyan {
  background: linear-gradient(135deg, #22d3ee 0%, #06b6d4 100%);
}

.icon-orange {
  background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
}

.icon-purple {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
}

.icon-pink {
  background: linear-gradient(135deg, #f472b6 0%, #ec4899 100%);
}

.icon-red {
  background: linear-gradient(135deg, #f87171 0%, #ef4444 100%);
}

.icon-indigo {
  background: linear-gradient(135deg, #818cf8 0%, #6366f1 100%);
}

.icon-gray {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
}

.cron-item .cron-label {
  font-weight: 500;
  white-space: nowrap;
}

/* 性能优化配置样式 */
.performance-config-box {
  margin-top: 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #f8fafc 100%);
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  padding: 16px;
}

.performance-config-box .config-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
}

.performance-config-box .config-header i:first-child {
  color: #3b82f6;
  font-size: 16px;
}

.performance-config-box .config-content {
  margin: 0;
}

.performance-config-box .config-item {
  height: 100%;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.performance-config-box .config-item:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.performance-config-box .config-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  margin-bottom: 8px;
}

.performance-config-box .config-desc {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.performance-config-box .config-desc i {
  font-size: 12px;
  color: #93c5fd;
}

/* 执行策略配置 - 紧凑型卡片样式 */
.execution-mode-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

/* 性能优化面板 */
.performance-optimization-panel {
  background: linear-gradient(135deg, #f0fdf4 0%, #f9fafb 100%);
  border: 2px solid #86efac;
  border-radius: 12px;
  padding: 20px;
}

.optimization-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #d1fae5;
}

.optimization-header i {
  font-size: 20px;
  color: #10b981;
}

.optimization-header span {
  font-size: 15px;
  font-weight: 600;
  color: #065f46;
  flex: 1;
}

.optimization-stages {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #d1fae5;
  border-radius: 8px;
  transition: all 0.3s;
}

.stage-item:hover {
  border-color: #10b981;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.1);
  transform: translateY(-2px);
}

.stage-badge {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.stage-badge.stage-1 {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
}

.stage-badge.stage-2 {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
}

.stage-badge.stage-3 {
  background: linear-gradient(135deg, #ec4899 0%, #db2777 100%);
}

.stage-badge.stage-4 {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
}

.stage-content {
  flex: 1;
  min-width: 0;
}

.stage-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.stage-desc {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 6px;
  line-height: 1.3;
}

.stage-performance {
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
}

.stage-status {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #10b981;
  flex-shrink: 0;
}

.stage-status i {
  font-size: 14px;
  color: white;
}

.optimization-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f0fdf4;
  border-radius: 8px;
  font-size: 12px;
  color: #065f46;
}

.optimization-footer i {
  font-size: 14px;
  color: #10b981;
}

.field-tip.success {
  background: #f0fdf4;
  border-left: 3px solid #10b981;
}

.field-tip.success i {
  color: #10b981;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .optimization-stages {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .execution-mode-cards {
    grid-template-columns: 1fr;
  }
}

.mode-card-compact {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.mode-card-compact:hover {
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
}

.mode-card-compact.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.2);
}

.mode-icon-small {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  transition: all 0.3s;
  flex-shrink: 0;
}

.mode-card-compact.active .mode-icon-small {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.mode-icon-small i {
  font-size: 20px;
  color: #6b7280;
  transition: all 0.3s;
}

.mode-card-compact.active .mode-icon-small i {
  color: white;
}

.mode-content-compact {
  flex: 1;
  min-width: 0;
}

.mode-title-small {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
  transition: all 0.3s;
}

.mode-card-compact.active .mode-title-small {
  color: #1e40af;
}

.mode-desc-small {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
  transition: all 0.3s;
}

.mode-card-compact.active .mode-desc-small {
  color: #3b82f6;
}

.mode-check-small {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: 2px solid #d1d5db;
  background: white;
  transition: all 0.3s;
  flex-shrink: 0;
}

.mode-card-compact.active .mode-check-small {
  border-color: #3b82f6;
  background: #3b82f6;
}

.mode-check-small i {
  font-size: 14px;
  color: transparent;
  transition: all 0.3s;
}

.mode-card-compact.active .mode-check-small i {
  color: white;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .execution-mode-cards {
    grid-template-columns: 1fr;
  }
}
</style>

.processor-item:hover .drag-handle {
  opacity: 1;
  color: #409EFF;
}
</style>

.processor-item:hover .drag-handle {
  opacity: 1;
  color: #409EFF;
}
</style>
  opacity: 1;
  color: #409EFF;
}
</style>
