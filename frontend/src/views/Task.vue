<template>
  <div class="task-container">
    <el-card class="page-card">
      <div slot="header" class="card-header-custom">
        <div class="header-left">
          <i class="el-icon-s-operation" style="margin-right: 8px; font-size: 18px; color: var(--primary-color);"></i>
          <span>任务管理</span>
        </div>
        <div class="header-actions">
          <el-button type="success" size="small" icon="el-icon-magic-stick" @click="handleTemplate">从模板创建</el-button>
          <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">向导创建</el-button>
        </div>
      </div>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-form :inline="true" size="small">
          <el-form-item label="任务名称">
            <el-input v-model="queryParams.taskName" placeholder="请输入" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="同步模式">
            <el-select v-model="queryParams.syncMode" placeholder="请选择" clearable style="width: 120px">
              <el-option label="全量" value="FULL" />
              <el-option label="增量" value="INCREMENTAL" />
            </el-select>
          </el-form-item>
          <el-form-item label="调度类型">
            <el-select v-model="queryParams.scheduleType" placeholder="请选择" clearable style="width: 120px">
              <el-option label="手动" value="MANUAL" />
              <el-option label="定时" value="CRON" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadData">查询</el-button>
            <el-button icon="el-icon-refresh" @click="handleResetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe class="data-table">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="taskName" label="任务名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="taskCode" label="任务编码" width="140" show-overflow-tooltip />
        <el-table-column label="数据流向" width="200">
          <template>
            <el-tag size="mini" type="success">源</el-tag>
            <i class="el-icon-right" style="margin: 0 5px"></i>
            <el-tag size="mini" type="warning">目标</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="syncMode" label="同步模式" width="90">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.syncMode === 'FULL'" type="primary" size="small">全量</el-tag>
            <el-tag v-else type="success" size="small">增量</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scheduleType" label="调度类型" width="90">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.scheduleType === 'MANUAL'" type="info" size="small">手动</el-tag>
            <el-tag v-else type="warning" size="small">定时</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 1" type="success" size="small">启用</el-tag>
            <el-tag v-else type="danger" size="small">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后执行" width="155" />
        <el-table-column label="操作" width="360" fixed="right">
          <template slot-scope="scope">
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <!-- 主要操作按钮 -->
              <el-button 
                size="mini" 
                type="success" 
                icon="el-icon-video-play" 
                @click="handleExecute(scope.row)" 
                :disabled="scope.row.status === 0">
                执行
              </el-button>
              
              <el-button 
                size="mini" 
                :type="scope.row.status === 0 ? 'success' : 'warning'" 
                icon="el-icon-switch-button" 
                @click="handleToggleStatus(scope.row)">
                {{ scope.row.status === 0 ? '启用' : '禁用' }}
              </el-button>
              
              <el-button 
                size="mini" 
                type="primary" 
                icon="el-icon-edit" 
                @click="handleEdit(scope.row)">
                编辑
              </el-button>
              
              <!-- 更多操作下拉菜单 -->
              <el-dropdown trigger="click" @command="handleCommand($event, scope.row)">
                <el-button size="mini" type="info">
                  更多<i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="copy" icon="el-icon-document-copy">
                    复制任务
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" icon="el-icon-delete" divided style="color: #f56c6c">
                    删除任务
                  </el-dropdown-item>
                  <el-dropdown-item command="log" icon="el-icon-document" divided>
                    执行日志
                  </el-dropdown-item>
                  <el-dropdown-item 
                    command="lineage" 
                    icon="el-icon-share"
                    :disabled="!scope.row.lastSyncTime">
                    数据血缘
                  </el-dropdown-item>
                  <el-dropdown-item 
                    command="compare" 
                    icon="el-icon-s-check"
                    :disabled="!scope.row.lastSyncTime">
                    数据对比
                  </el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        @current-change="handleCurrentChange"
        :current-page="queryParams.current"
        :page-size="queryParams.size"
        layout="total, prev, pager, next"
        :total="total"
        style="margin-top: 20px; text-align: right">
      </el-pagination>
    </el-card>

    <!-- 模板选择对话框 -->
    <task-template ref="taskTemplate" />

    <!-- 复制任务对话框 -->
    <el-dialog title="复制任务" :visible.sync="copyDialogVisible" width="500px">
      <el-form :model="copyForm" ref="copyForm" label-width="100px" size="small">
        <el-form-item label="原任务名称">
          <el-input v-model="copyForm.originalTaskName" disabled />
        </el-form-item>
        <el-form-item label="新任务名称" required>
          <el-input 
            v-model="copyForm.newTaskName" 
            placeholder="请输入新任务名称" 
            maxlength="100"
            show-word-limit />
        </el-form-item>
        <el-alert
          title="提示：复制后的任务将包含原任务的所有配置（源、目标、字段映射等），但默认为禁用状态"
          type="info"
          :closable="false"
          style="margin-top: 10px">
        </el-alert>
      </el-form>
      <div slot="footer">
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCopy" :loading="copyLoading">确定复制</el-button>
      </div>
    </el-dialog>

    <!-- 任务配置对话框 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="700px">
      <el-form :model="form" ref="form" label-width="120px" size="small">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.taskName" />
        </el-form-item>
        <el-form-item label="任务编码" required>
          <el-input v-model="form.taskCode" />
        </el-form-item>
        <el-form-item label="源连接器" required>
          <el-select v-model="form.sourceConnectorId" placeholder="请选择">
            <el-option v-for="item in connectorList" :key="item.id" :label="item.connectorName" :value="item.id">
              <div style="display: flex; align-items: center; gap: 8px;">
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
        <el-form-item label="源端SQL" required>
          <el-input v-model="form.sourceConfig" type="textarea" :rows="3" placeholder='{"sql": "SELECT * FROM table"}' />
        </el-form-item>
        <el-form-item label="目标连接器" required>
          <el-select v-model="form.targetConnectorId" placeholder="请选择">
            <el-option v-for="item in connectorList" :key="item.id" :label="item.connectorName" :value="item.id">
              <div style="display: flex; align-items: center; gap: 8px;">
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
        <el-form-item label="目标端配置" required>
          <el-input v-model="form.targetConfig" type="textarea" :rows="3" placeholder='{"tableName": "table", "writeMode": "INSERT"}' />
        </el-form-item>
        <el-form-item label="同步模式">
          <el-radio-group v-model="form.syncMode">
            <el-radio label="FULL">全量</el-radio>
            <el-radio label="INCREMENTAL">增量</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="调度类型">
          <el-radio-group v-model="form.scheduleType">
            <el-radio label="MANUAL">手动</el-radio>
            <el-radio label="CRON">定时</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- Cron配置 -->
        <template v-if="form.scheduleType === 'CRON'">
          <el-form-item label="执行频率">
            <el-select 
              v-model="cronTemplate" 
              placeholder="选择常用模板或自定义" 
              style="width: 100%"
              @change="handleCronTemplateChange">
              <el-option label="每5分钟" value="0 */5 * * * ?">
                <span><i class="el-icon-time"></i> 每5分钟</span>
              </el-option>
              <el-option label="每30分钟" value="0 */30 * * * ?">
                <span><i class="el-icon-time"></i> 每30分钟</span>
              </el-option>
              <el-option label="每小时" value="0 0 * * * ?">
                <span><i class="el-icon-time"></i> 每小时</span>
              </el-option>
              <el-option label="每天凌晨2点" value="0 0 2 * * ?">
                <span><i class="el-icon-sunrise"></i> 每天凌晨2点</span>
              </el-option>
              <el-option label="每周一凌晨2点" value="0 0 2 ? * MON">
                <span><i class="el-icon-date"></i> 每周一凌晨2点</span>
              </el-option>
              <el-option label="工作日上午9点" value="0 0 9 ? * MON-FRI">
                <span><i class="el-icon-office-building"></i> 工作日上午9点</span>
              </el-option>
              <el-option label="每月1号凌晨2点" value="0 0 2 1 * ?">
                <span><i class="el-icon-date"></i> 每月1号凌晨2点</span>
              </el-option>
              <el-option label="可视化配置" value="visual">
                <span><i class="el-icon-edit"></i> 可视化配置</span>
              </el-option>
              <el-option label="手写表达式" value="custom">
                <span><i class="el-icon-edit-outline"></i> 手写表达式</span>
              </el-option>
            </el-select>
          </el-form-item>
          
          <!-- 可视化配置 -->
          <el-form-item v-if="cronTemplate === 'visual'" label="定时规则">
            <div style="background: #f5f7fa; padding: 15px; border-radius: 4px;">
              <el-form :inline="true" size="small" label-width="60px">
                <el-form-item label="周期">
                  <el-select v-model="cronVisual.period" @change="updateCronFromVisual" style="width: 150px">
                    <el-option label="每天" value="day" />
                    <el-option label="每周" value="week" />
                    <el-option label="每月" value="month" />
                    <el-option label="自定义" value="custom" />
                  </el-select>
                </el-form-item>
                
                <el-form-item v-if="cronVisual.period === 'week'" label="星期">
                  <el-select v-model="cronVisual.weekDay" @change="updateCronFromVisual" style="width: 120px">
                    <el-option label="周一" value="MON" />
                    <el-option label="周二" value="TUE" />
                    <el-option label="周三" value="WED" />
                    <el-option label="周四" value="THU" />
                    <el-option label="周五" value="FRI" />
                    <el-option label="周六" value="SAT" />
                    <el-option label="周日" value="SUN" />
                  </el-select>
                </el-form-item>
                
                <el-form-item v-if="cronVisual.period === 'month'" label="日期">
                  <el-input-number 
                    v-model="cronVisual.dayOfMonth" 
                    :min="1" 
                    :max="31" 
                    @change="updateCronFromVisual"
                    style="width: 100px" />
                  <span style="margin-left: 5px;">号</span>
                </el-form-item>
                
                <el-form-item label="时间">
                  <el-time-select
                    v-model="cronVisual.time"
                    :picker-options="{
                      start: '00:00',
                      step: '00:30',
                      end: '23:30'
                    }"
                    @change="updateCronFromVisual"
                    placeholder="选择时间"
                    style="width: 120px">
                  </el-time-select>
                </el-form-item>
                
                <el-form-item v-if="cronVisual.period === 'custom'" label="间隔">
                  <el-input-number 
                    v-model="cronVisual.interval" 
                    :min="1" 
                    @change="updateCronFromVisual"
                    style="width: 100px" />
                  <el-select v-model="cronVisual.unit" @change="updateCronFromVisual" style="width: 100px; margin-left: 5px;">
                    <el-option label="分钟" value="minute" />
                    <el-option label="小时" value="hour" />
                    <el-option label="天" value="day" />
                  </el-select>
                </el-form-item>
              </el-form>
            </div>
          </el-form-item>
          
          <el-form-item label="Cron表达式">
            <el-input 
              v-model="form.cronExpression" 
              placeholder="0 0 * * * ?"
              :readonly="cronTemplate !== 'custom'">
              <template slot="append">
                <el-button 
                  icon="el-icon-refresh" 
                  @click="parseCronExpression"
                  :disabled="!form.cronExpression">解析</el-button>
              </template>
            </el-input>
            <div style="margin-top: 5px; font-size: 12px; color: #909399;">
              <i class="el-icon-info"></i> 
              格式: 秒 分 时 日 月 周
              <span v-if="cronDescription" style="color: #67c23a; margin-left: 10px;">
                <i class="el-icon-success"></i> {{ cronDescription }}
              </span>
            </div>
          </el-form-item>
        </template>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 字段映射对话框 -->
    <el-dialog title="字段映射配置" :visible.sync="mappingDialogVisible" width="800px">
      <el-button type="primary" size="small" @click="handleAddMapping" style="margin-bottom: 10px">添加映射</el-button>
      <el-table :data="mappings" border>
        <el-table-column label="源字段">
          <template slot-scope="scope">
            <el-input v-model="scope.row.sourceField" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="目标字段">
          <template slot-scope="scope">
            <el-input v-model="scope.row.targetField" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="转换类型">
          <template slot-scope="scope">
            <el-select v-model="scope.row.transformType" size="small">
              <el-option label="直接映射" value="DIRECT" />
              <el-option label="脚本转换" value="SCRIPT" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template slot-scope="scope">
            <el-button type="danger" size="mini" @click="handleDeleteMapping(scope.$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button @click="mappingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMappings">保存</el-button>
      </div>
    </el-dialog>

    <!-- 数据血缘追踪对话框 -->
    <el-dialog title="数据血缘追踪" :visible.sync="lineageDialogVisible" width="1000px">
      <el-alert
        title="血缘说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px">
        查看当前任务的数据血缘关系，跟踪字段级别的依赖和转换关系。
      </el-alert>
      
      <!-- 快捷查询区 -->
      <el-card shadow="never" style="margin-bottom: 15px; background: #f5f7fa">
        <div slot="header" style="padding: 10px 0">
          <i class="el-icon-search"></i>
          <span style="margin-left: 5px; font-weight: bold">快捷查询</span>
        </div>
        
        <el-form :inline="true" size="medium">
          <el-form-item label="搜索">
            <el-autocomplete
              v-model="lineageSearchInput"
              :fetch-suggestions="searchLineageFields"
              placeholder="输入表名.字段名，如：user.name"
              style="width: 300px"
              clearable
              @select="handleLineageSearchSelect">
              <template slot-scope="{ item }">
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <span>
                    <i class="el-icon-tickets"></i>
                    <strong>{{ item.tableName }}</strong>.<span style="color: #409EFF">{{ item.fieldName }}</span>
                  </span>
                  <el-tag size="mini" type="info">{{ item.connectorName }}</el-tag>
                </div>
              </template>
            </el-autocomplete>
          </el-form-item>
          
          <el-form-item>
            <el-button-group>
              <el-button 
                v-for="item in recentLineageQueries.slice(0, 3)"
                :key="item.key"
                size="small"
                @click="loadRecentQuery(item)">
                <i class="el-icon-time"></i> {{ item.tableName }}.{{ item.fieldName.join(',') }}
              </el-button>
            </el-button-group>
          </el-form-item>
        </el-form>
      </el-card>
      
      <!-- 详细查询表单 -->
      <el-collapse v-model="lineageAdvancedSearch" style="margin-bottom: 15px">
        <el-collapse-item name="advanced">
          <template slot="title">
            <i class="el-icon-setting"></i>
            <span style="margin-left: 5px">高级查询</span>
          </template>
          
          <el-form label-width="120px" size="medium">
            <el-form-item label="数据源">
              <el-select v-model="lineageConfig.connectorId" placeholder="请选择数据源" style="width: 300px" @change="loadLineageTables" clearable>
                <el-option 
                  v-for="item in connectorList" 
                  :key="item.id" 
                  :label="item.connectorName" 
                  :value="item.id">
                  <div style="display: flex; align-items: center; gap: 8px;">
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
            
            <el-form-item label="表名">
              <el-select v-model="lineageConfig.tableName" placeholder="请选择表" style="width: 300px" @change="loadLineageFields" filterable clearable>
                <el-option 
                  v-for="table in lineageTableList" 
                  :key="table" 
                  :label="table" 
                  :value="table" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="字段名">
              <el-select 
                v-model="lineageConfig.fieldName" 
                placeholder="请选择字段（可多选）" 
                style="width: 300px" 
                filterable
                multiple
                collapse-tags
                clearable>
                <el-option 
                  v-for="field in lineageFieldList" 
                  :key="field.columnName" 
                  :label="field.columnName" 
                  :value="field.columnName" />
              </el-select>
              <div class="tip" style="margin-top: 5px">
                <i class="el-icon-info"></i> 支持多选，一次查询多个字段的血缘关系
              </div>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
      
      <div style="margin-bottom: 15px">
        <el-button type="primary" @click="queryLineage" :loading="lineageLoading" icon="el-icon-search">查询血缘</el-button>
        <el-button @click="clearLineageQuery" icon="el-icon-refresh-left">清空条件</el-button>
      </div>
      
      <!-- 血缘结果展示 -->
      <div v-if="lineageGraphData" style="margin-top: 20px">
        <el-divider><i class="el-icon-share"></i> 血缘关系</el-divider>
        <!-- 如果没有节点和边，显示友好提示 -->
        <el-empty 
          v-if="!lineageGraphData.edges || lineageGraphData.edges.length === 0" 
          description="该字段暂无血缘关系"
          :image-size="100">
          <template slot="description">
            <div style="color: #909399">
              <p style="font-size: 14px; margin: 10px 0">该字段暂无血缘关系</p>
              <el-divider></el-divider>
              <div style="text-align: left; padding: 0 40px">
                <p style="margin: 5px 0"><i class="el-icon-warning" style="color: #E6A23C"></i> <strong>可能原因：</strong></p>
                <ul style="list-style: none; padding-left: 20px; margin: 5px 0">
                  <li style="margin: 5px 0">• 该字段所在的任务还未执行过</li>
                  <li style="margin: 5px 0">• 该字段未在任务的字段映射中配置</li>
                  <li style="margin: 5px 0">• 该字段仅存在于数据表中，但没有ETL任务使用</li>
                </ul>
                <p style="margin: 10px 0 5px 0"><i class="el-icon-info" style="color: #409EFF"></i> <strong>建议操作：</strong></p>
                <ul style="list-style: none; padding-left: 20px; margin: 5px 0">
                  <li style="margin: 5px 0">• 确认相关任务是否已经执行，如果没有，请先执行任务</li>
                  <li style="margin: 5px 0">• 检查任务的字段映射配置，确保该字段已被映射</li>
                  <li style="margin: 5px 0">• 尝试查询其他相关字段的血缘关系</li>
                </ul>
              </div>
            </div>
          </template>
        </el-empty>
        
        <!-- 有数据时展示 -->
        <div v-else>
          <el-alert
            type="success"
            :closable="false"
            style="margin-bottom: 15px">
            <template slot="title">
              <i class="el-icon-success"></i> 找到 <strong>{{ lineageGraphData.edges.length }}</strong> 条血缘关系，
              共 <strong>{{ lineageGraphData.nodes.length }}</strong> 个节点
            </template>
          </el-alert>
          
          <!-- 视图切换和过滤器 -->
          <el-card shadow="never" style="margin-bottom: 15px">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-radio-group v-model="lineageViewMode" size="small">
                  <el-radio-button label="table">
                    <i class="el-icon-tickets"></i> 表格视图
                  </el-radio-button>
                  <el-radio-button label="graph">
                    <i class="el-icon-share"></i> 拓扑图
                  </el-radio-button>
                </el-radio-group>
              </el-col>
              <el-col :span="12" style="text-align: right">
                <el-select v-model="lineageFilterType" size="small" style="width: 150px" placeholder="过滤转换类型" clearable>
                  <el-option label="全部" value=""></el-option>
                  <el-option label="直接映射" value="DIRECT"></el-option>
                  <el-option label="函数转换" value="FUNCTION"></el-option>
                  <el-option label="字典转换" value="DICT"></el-option>
                  <el-option label="脚本转换" value="SCRIPT"></el-option>
                  <el-option label="条件转换" value="CONDITION"></el-option>
                </el-select>
                <el-input 
                  v-model="lineageSearchKeyword" 
                  size="small" 
                  placeholder="搜索字段名" 
                  style="width: 150px; margin-left: 10px"
                  clearable>
                  <i slot="prefix" class="el-icon-search"></i>
                </el-input>
              </el-col>
            </el-row>
          </el-card>
          
          <!-- 表格视图 -->
          <div v-show="lineageViewMode === 'table'">
          <el-tabs>
            <el-tab-pane>
              <span slot="label">
                <i class="el-icon-top"></i> 上游血缘 ({{ getFilteredUpstreamEdges().length }})
              </span>
              <el-table :data="getFilteredUpstreamEdges()" border size="small" max-height="300" :row-class-name="tableRowClassName">
                <el-table-column label="源连接器" width="120">
                  <template slot-scope="scope">
                    {{ getConnectorName(getNodeById(scope.row.source).connectorId) }}
                  </template>
                </el-table-column>
                <el-table-column label="源表" width="150" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <el-tag size="mini" type="info">{{ getNodeById(scope.row.source).table }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="源字段" width="120" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <strong>{{ getNodeById(scope.row.source).label }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="转换规则" min-width="200" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="getTransformTypeColor(scope.row.transformType)">{{ scope.row.transformType || 'DIRECT' }}</el-tag>
                    <span v-if="scope.row.transformRule" style="margin-left: 8px; color: #606266">{{ scope.row.transformRule }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="目标字段" width="120">
                  <template slot-scope="scope">
                    <strong style="color: #67C23A">{{ getNodeById(scope.row.target).label }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="任务" width="80">
                  <template slot-scope="scope">
                    <el-tag size="mini" type="warning">#{{ scope.row.taskId }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template slot-scope="scope">
                    <el-button 
                      type="text" 
                      size="mini" 
                      @click="drillDownLineage(getNodeById(scope.row.source))">
                      <i class="el-icon-d-arrow-left"></i> 上针
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            
            <el-tab-pane>
              <span slot="label">
                <i class="el-icon-bottom"></i> 下游血缘 ({{ getFilteredDownstreamEdges().length }})
              </span>
              <el-table :data="getFilteredDownstreamEdges()" border size="small" max-height="300" :row-class-name="tableRowClassName">
                <el-table-column label="源字段" width="120">
                  <template slot-scope="scope">
                    <strong style="color: #409EFF">{{ getNodeById(scope.row.source).label }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="转换规则" min-width="200" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <el-tag size="mini" :type="getTransformTypeColor(scope.row.transformType)">{{ scope.row.transformType || 'DIRECT' }}</el-tag>
                    <span v-if="scope.row.transformRule" style="margin-left: 8px; color: #606266">{{ scope.row.transformRule }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="目标连接器" width="120">
                  <template slot-scope="scope">
                    {{ getConnectorName(getNodeById(scope.row.target).connectorId) }}
                  </template>
                </el-table-column>
                <el-table-column label="目标表" width="150" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <el-tag size="mini" type="success">{{ getNodeById(scope.row.target).table }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="目标字段" width="120" show-overflow-tooltip>
                  <template slot-scope="scope">
                    <strong>{{ getNodeById(scope.row.target).label }}</strong>
                  </template>
                </el-table-column>
                <el-table-column label="任务" width="80">
                  <template slot-scope="scope">
                    <el-tag size="mini" type="warning">#{{ scope.row.taskId }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" fixed="right">
                  <template slot-scope="scope">
                    <el-button 
                      type="text" 
                      size="mini" 
                      @click="drillDownLineage(getNodeById(scope.row.target))">
                      <i class="el-icon-d-arrow-right"></i> 下针
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
          </div>
          
          <!-- 拓扑图视图 -->
          <div v-show="lineageViewMode === 'graph'" style="margin-top: 15px">
            <div id="lineageGraph" style="width: 100%; height: 400px; border: 1px solid #dcdfe6; border-radius: 4px; background: #fff"></div>
            <div style="margin-top: 10px; text-align: center; color: #909399">
              <i class="el-icon-info"></i> 使用鼠标拖动移动节点，滚轮缩放
            </div>
          </div>
          
          <div style="margin-top: 15px; color: #606266; background: #f5f7fa; padding: 12px; border-radius: 4px">
            <i class="el-icon-info"></i> 
            <strong>当前查询字段：</strong> 
            <el-tag size="mini">{{ lineageConfig.tableName }}</el-tag>
            <span style="margin-left: 5px">
              <el-tag 
                v-for="(field, index) in lineageConfig.fieldName" 
                :key="index" 
                size="mini" 
                type="success" 
                style="margin-left: 5px">
                {{ field }}
              </el-tag>
            </span>
          </div>
        </div>
      </div>
      
      <span slot="footer">
        <el-button @click="lineageDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>

    <!-- 数据对比对话框 -->
    <el-dialog title="数据对比" :visible.sync="compareDialogVisible" width="1100px">
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: 20px">
        <template slot="title">
          <i class="el-icon-info"></i> <strong>对比说明</strong>：对比源端和目标端数据的一致性，用于校验同步结果
        </template>
        <div style="margin-top: 8px; font-size: 13px">
          <div>• <strong>主键字段</strong>：用于匹配源端和目标端记录（必填）</div>
          <div>• <strong>对比字段</strong>：指定要对比的字段，空表示对比所有字段</div>
          <div>• 支持直接输入<strong>表名</strong>或自定义<strong>SQL查询</strong></div>
        </div>
      </el-alert>
      
      <el-form label-width="120px" size="medium">
        <!-- 源端配置 -->
        <el-divider content-position="left">
          <i class="el-icon-upload2" style="color: #409EFF"></i> 源端配置
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源连接器" required>
              <el-select 
                v-model="compareConfig.sourceConnectorId" 
                placeholder="请选择" 
                style="width: 100%"
                @change="handleSourceConnectorChange">
                <el-option 
                  v-for="item in connectorList" 
                  :key="item.id" 
                  :label="item.connectorName" 
                  :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源表名" required>
              <el-autocomplete
                v-model="compareConfig.sourceTableOrSql"
                :fetch-suggestions="querySourceTables"
                placeholder="输入表名或SQL语句"
                style="width: 100%"
                clearable>
                <template slot-scope="{ item }">
                  <i class="el-icon-tickets"></i> {{ item.value }}
                </template>
              </el-autocomplete>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 目标端配置 -->
        <el-divider content-position="left">
          <i class="el-icon-download" style="color: #67C23A"></i> 目标端配置
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目标连接器" required>
              <el-select 
                v-model="compareConfig.targetConnectorId" 
                placeholder="请选择" 
                style="width: 100%"
                @change="handleTargetConnectorChange">
                <el-option 
                  v-for="item in connectorList" 
                  :key="item.id" 
                  :label="item.connectorName" 
                  :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标表名" required>
              <el-autocomplete
                v-model="compareConfig.targetTableOrSql"
                :fetch-suggestions="queryTargetTables"
                placeholder="输入表名或SQL语句"
                style="width: 100%"
                clearable>
                <template slot-scope="{ item }">
                  <i class="el-icon-tickets"></i> {{ item.value }}
                </template>
              </el-autocomplete>
            </el-form-item>
          </el-col>
        </el-row>
        
        <!-- 对比配置 -->
        <el-divider content-position="left">
          <i class="el-icon-s-operation" style="color: #E6A23C"></i> 对比配置
        </el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主键字段" required>
              <el-select
                v-model="compareConfig.keyFields"
                placeholder="选择主键字段（可多选）"
                style="width: 100%"
                multiple
                filterable
                allow-create
                default-first-option
                collapse-tags>
                <el-option
                  v-for="field in compareKeyFieldOptions"
                  :key="field"
                  :label="field"
                  :value="field" />
              </el-select>
              <div class="tip" style="margin-top: 5px">
                <i class="el-icon-info"></i> 示例：id 或 order_id, line_num
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对比字段">
              <el-select
                v-model="compareConfig.compareFields"
                placeholder="选择对比字段（空表示全部）"
                style="width: 100%"
                multiple
                filterable
                allow-create
                default-first-option
                collapse-tags
                clearable>
                <el-option
                  v-for="field in compareFieldOptions"
                  :key="field"
                  :label="field"
                  :value="field" />
              </el-select>
              <div class="tip" style="margin-top: 5px">
                <i class="el-icon-info"></i> 空表示对比所有字段
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item>
          <el-button type="primary" @click="executeCompare" :loading="compareLoading">
            <i class="el-icon-s-check"></i> 执行对比
          </el-button>
          <el-button @click="loadTaskConfigToCompare" v-if="currentTaskId">
            <i class="el-icon-refresh-left"></i> 使用任务配置
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 对比结果展示 -->
      <div v-if="compareResult" style="margin-top: 20px">
        <el-divider><i class="el-icon-s-check"></i> 对比结果</el-divider>
        
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">源端记录数</div>
                <div style="font-size: 24px; font-weight: bold; color: #409EFF; margin-top: 8px">{{ compareResult.sourceCount }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">目标端记录数</div>
                <div style="font-size: 24px; font-weight: bold; color: #409EFF; margin-top: 8px">{{ compareResult.targetCount }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" style="cursor: pointer" @click.native="showMatchDetails">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">
                  匹配记录数
                  <i class="el-icon-search" style="margin-left: 4px"></i>
                </div>
                <div style="font-size: 24px; font-weight: bold; color: #67C23A; margin-top: 8px">{{ compareResult.matchCount }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover" style="cursor: pointer" @click.native="showDiffDetails">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">
                  差异记录数
                  <i class="el-icon-search" style="margin-left: 4px"></i>
                </div>
                <div style="font-size: 24px; font-weight: bold; color: #F56C6C; margin-top: 8px">{{ compareResult.diffCount }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-row :gutter="20" style="margin-bottom: 20px">
          <el-col :span="12">
            <el-card shadow="hover" style="cursor: pointer" @click.native="showSourceOnlyDetails">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">
                  仅源端存在
                  <i class="el-icon-search" style="margin-left: 4px"></i>
                </div>
                <div style="font-size: 20px; font-weight: bold; color: #E6A23C; margin-top: 8px">{{ compareResult.sourceOnlyCount }}</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover" style="cursor: pointer" @click.native="showTargetOnlyDetails">
              <div style="text-align: center">
                <div style="font-size: 14px; color: #909399">
                  仅目标端存在
                  <i class="el-icon-search" style="margin-left: 4px"></i>
                </div>
                <div style="font-size: 20px; font-weight: bold; color: #E6A23C; margin-top: 8px">{{ compareResult.targetOnlyCount }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      
      <span slot="footer">
        <el-button @click="compareDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
    
    <!-- 对比详情对话框 -->
    <el-dialog
      :title="compareDetailTitle"
      :visible.sync="compareDetailDialogVisible"
      width="90%"
      top="5vh">
      
      <!-- 表格视图 -->
      <el-tabs v-model="detailViewMode" type="border-card">
        <el-tab-pane label="表格视图" name="table">
          <el-table :data="compareDetailData" border size="small" max-height="500">
            <el-table-column type="index" label="#" width="50" fixed />
            <el-table-column prop="key" label="主键" width="150" show-overflow-tooltip fixed />
            
            <!-- 差异类型列（仅在diff类型时显示） -->
            <el-table-column v-if="compareDetailType === 'diff'" label="差异字段" width="150" fixed>
              <template slot-scope="scope">
                <el-tag
                  v-for="field in scope.row.diffFields"
                  :key="field"
                  type="warning"
                  size="mini"
                  style="margin: 2px">
                  {{ field }}
                </el-tag>
                <span v-if="!scope.row.diffFields || scope.row.diffFields.length === 0">无</span>
              </template>
            </el-table-column>
            
            <!-- 动态字段列 -->
            <el-table-column
              v-for="field in detailTableColumns"
              :key="field"
              :label="field"
              :prop="field"
              min-width="120"
              show-overflow-tooltip>
              <template slot-scope="scope">
                <div style="display: flex; gap: 10px; align-items: center">
                  <!-- 源端数据 -->
                  <div v-if="compareDetailType !== 'targetOnly'" style="flex: 1">
                    <el-tag v-if="scope.row.sourceData && scope.row.sourceData[field] !== undefined" size="small" type="info">
                      {{ formatValue(scope.row.sourceData[field]) }}
                    </el-tag>
                    <span v-else style="color: #C0C4CC">-</span>
                  </div>
                  
                  <!-- 对比图标 -->
                  <i v-if="compareDetailType === 'diff' && isFieldDiffInRow(scope.row, field)" 
                     class="el-icon-warning" 
                     style="color: #F56C6C"></i>
                  <i v-else-if="compareDetailType !== 'sourceOnly' && compareDetailType !== 'targetOnly'" 
                     class="el-icon-check" 
                     style="color: #67C23A"></i>
                  
                  <!-- 目标端数据 -->
                  <div v-if="compareDetailType !== 'sourceOnly'" style="flex: 1">
                    <el-tag v-if="scope.row.targetData && scope.row.targetData[field] !== undefined" 
                            size="small" 
                            :type="isFieldDiffInRow(scope.row, field) ? 'danger' : 'success'">
                      {{ formatValue(scope.row.targetData[field]) }}
                    </el-tag>
                    <span v-else style="color: #C0C4CC">-</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            
            <!-- 操作列 -->
            <el-table-column label="操作" width="100" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="showRowDetail(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        
        <!-- JSON视图 -->
        <el-tab-pane label="JSON视图" name="json">
          <el-table :data="compareDetailData" border size="small" max-height="500">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="key" label="主键" width="150" show-overflow-tooltip />
            
            <!-- 差异类型列（仅在diff类型时显示） -->
            <el-table-column v-if="compareDetailType === 'diff'" label="差异字段" width="200">
              <template slot-scope="scope">
                <el-tag
                  v-for="field in scope.row.diffFields"
                  :key="field"
                  type="warning"
                  size="mini"
                  style="margin: 2px">
                  {{ field }}
                </el-tag>
                <span v-if="!scope.row.diffFields || scope.row.diffFields.length === 0">无</span>
              </template>
            </el-table-column>
            
            <!-- 源端数据列 -->
            <el-table-column v-if="compareDetailType !== 'targetOnly'" label="源端数据" show-overflow-tooltip>
              <template slot-scope="scope">
                <pre v-if="scope.row.sourceData" style="margin: 0; font-size: 12px; white-space: pre-wrap">{{ formatJson(scope.row.sourceData) }}</pre>
                <span v-else style="color: #909399">无数据</span>
              </template>
            </el-table-column>
            
            <!-- 目标端数据列 -->
            <el-table-column v-if="compareDetailType !== 'sourceOnly'" label="目标端数据" show-overflow-tooltip>
              <template slot-scope="scope">
                <pre v-if="scope.row.targetData" style="margin: 0; font-size: 12px; white-space: pre-wrap">{{ formatJson(scope.row.targetData) }}</pre>
                <span v-else style="color: #909399">无数据</span>
              </template>
            </el-table-column>
            
            <!-- 操作列 -->
            <el-table-column label="操作" width="100" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="showRowDetail(scope.row)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      
      <div slot="footer">
        <el-button @click="exportDetailData" type="primary" icon="el-icon-download">导出数据</el-button>
        <el-button @click="compareDetailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
    
    <!-- 单条记录详情对话框 -->
    <el-dialog
      title="记录详情"
      :visible.sync="rowDetailDialogVisible"
      width="60%">
      
      <el-row :gutter="20">
        <el-col :span="currentRowDetail.sourceData && currentRowDetail.targetData ? 12 : 24">
          <el-card shadow="never" v-if="currentRowDetail.sourceData">
            <div slot="header">
              <i class="el-icon-upload2" style="color: #409EFF"></i>
              <span style="margin-left: 8px">源端数据</span>
            </div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item
                v-for="(value, key) in currentRowDetail.sourceData"
                :key="key"
                :label="key">
                {{ value }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card shadow="never" v-else>
            <el-empty description="源端无此记录" :image-size="80"></el-empty>
          </el-card>
        </el-col>
        
        <el-col :span="currentRowDetail.sourceData && currentRowDetail.targetData ? 12 : 24">
          <el-card shadow="never" v-if="currentRowDetail.targetData">
            <div slot="header">
              <i class="el-icon-download" style="color: #67C23A"></i>
              <span style="margin-left: 8px">目标端数据</span>
            </div>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item
                v-for="(value, key) in currentRowDetail.targetData"
                :key="key"
                :label="key"
                :label-class-name="isFieldDiff(key) ? 'diff-field-label' : ''">
                <span :class="isFieldDiff(key) ? 'diff-field-value' : ''">{{ value }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
          <el-card shadow="never" v-else>
            <el-empty description="目标端无此记录" :image-size="80"></el-empty>
          </el-card>
        </el-col>
      </el-row>
      
      <div slot="footer">
        <el-button @click="rowDetailDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import TaskTemplate from './TaskTemplate.vue'

export default {
  components: {
    TaskTemplate
  },
  data() {
    return {
      queryParams: { 
        current: 1, 
        size: 10, 
        taskName: '',
        syncMode: '',
        scheduleType: ''
      },
      tableData: [],
      total: 0,
      dialogVisible: false,
      dialogTitle: '新增任务',
      form: {
        taskName: '',
        taskCode: '',
        sourceConnectorId: null,
        targetConnectorId: null,
        sourceConfig: '{"sql": "SELECT * FROM table"}',
        targetConfig: '{"tableName": "table", "writeMode": "INSERT"}',
        syncMode: 'FULL',
        scheduleType: 'MANUAL',
        cronExpression: '',
        description: '',
        status: 1
      },
      
      // Cron配置相关
      cronTemplate: '', // 当前选择的模板
      cronDescription: '', // Cron表达式描述
      cronVisual: {
        period: 'day', // day, week, month, custom
        weekDay: 'MON',
        dayOfMonth: 1,
        time: '02:00',
        interval: 5,
        unit: 'minute' // minute, hour, day
      },
      connectorList: [],
      mappingDialogVisible: false,
      currentTaskId: null,
      mappings: [],
      
      // 复制任务相关
      copyDialogVisible: false,
      copyLoading: false,
      copyForm: {
        taskId: null,
        originalTaskName: '',
        newTaskName: ''
      },
      
      // 数据血缘相关
      lineageDialogVisible: false,
      lineageLoading: false,
      lineageConfig: {
        connectorId: null,
        tableName: '',
        fieldName: [] // 改为数组支持多选
      },
      lineageTableList: [],
      lineageFieldList: [],
      lineageGraphData: null,
      // 血缘界面增强
      lineageSearchInput: '', // 快捷搜索输入
      lineageAdvancedSearch: [], // 高级搜索折叠面板
      recentLineageQueries: [], // 最近查询记录
      lineageViewMode: 'table', // 'table' 或 'graph'
      lineageFilterType: '', // 过滤转换类型
      lineageSearchKeyword: '', // 搜索关键词
      lineageGraphInstance: null, // 拓扑图实例
      
      // 数据对比相关
      compareDialogVisible: false,
      compareLoading: false,
      compareConfig: {
        sourceConnectorId: null,
        sourceTableOrSql: '',
        targetConnectorId: null,
        targetTableOrSql: '',
        keyFields: [], // 改为数组支持多选
        compareFields: [] // 改为数组支持多选
      },
      compareResult: null,
      compareSourceTables: [], // 源表列表
      compareTargetTables: [], // 目标表列表
      compareKeyFieldOptions: [], // 主键字段选项
      compareFieldOptions: [], // 对比字段选项
      
      // 对比详情相关
      compareDetailDialogVisible: false,
      compareDetailTitle: '',
      compareDetailType: '', // 'match', 'diff', 'sourceOnly', 'targetOnly'
      compareDetailData: [],
      detailViewMode: 'table', // 'table' 或 'json'
      detailTableColumns: [], // 动态表格列
      
      // 记录详情相关
      rowDetailDialogVisible: false,
      currentRowDetail: {
        sourceData: {},
        targetData: null,
        diffFields: []
      }
    }
  },
  mounted() {
    this.loadData()
    this.loadConnectors()
    
    // 加载最近的血缘查询记录
    try {
      const stored = localStorage.getItem('recentLineageQueries')
      if (stored) {
        this.recentLineageQueries = JSON.parse(stored)
      }
    } catch (e) {
      console.error('加载查询记录失败', e)
    }
  },
  watch: {
    // 监视视图模式切换
    lineageViewMode(newMode) {
      if (newMode === 'graph' && this.lineageGraphData && this.lineageGraphData.edges && this.lineageGraphData.edges.length > 0) {
        this.$nextTick(() => {
          this.renderLineageGraph()
        })
      }
    },
      
    // 监听Cron表达式变化，自动解析
    'form.cronExpression'(newVal) {
      if (newVal && this.cronTemplate === 'custom') {
        // 只有在自定义模式下才自动解析
        this.parseCronExpression()
      }
    }
  },
  methods: {
    loadData() {
      this.$axios.get('/v1/task/page', { params: this.queryParams }).then(res => {
        this.tableData = res.data.records
        this.total = res.data.total
      })
    },
    loadConnectors() {
      this.$axios.get('/v1/connector/list').then(res => {
        this.connectorList = res.data
      })
    },
    handleCurrentChange(val) {
      this.queryParams.current = val
      this.loadData()
    },
    handleResetQuery() {
      this.queryParams = {
        current: 1,
        size: 10,
        taskName: '',
        syncMode: '',
        scheduleType: ''
      }
      this.loadData()
    },
    handleTemplate() {
      console.log('handleTemplate called')
      console.log('taskTemplate ref:', this.$refs.taskTemplate)
      if (this.$refs.taskTemplate) {
        this.$refs.taskTemplate.show()
      } else {
        this.$message.error('模板组件未加载,请刷新页面重试')
      }
    },
    handleAdd() {
      this.$router.push('/task/wizard')
    },
    handleEdit(row) {
      // 跳转到任务向导页面，传递任务ID进行编辑
      this.$router.push({
        path: '/task/wizard',
        query: { taskId: row.id }
      })
    },
    handleSubmit() {
      const api = this.form.id ? this.$axios.put('/v1/task', this.form) : this.$axios.post('/v1/task', this.form)
      api.then(() => {
        this.$message.success(this.form.id ? '修改成功' : '新增成功')
        this.dialogVisible = false
        this.loadData()
      })
    },
    handleDelete(row) {
      this.$confirm('确认删除？', '提示', { type: 'warning' }).then(() => {
        this.$axios.delete(`/v1/task/${row.id}`).then(() => {
          this.$message.success('删除成功')
          this.loadData()
        })
      })
    },
    
    // 下拉菜单命令处理
    handleCommand(command, row) {
      switch (command) {
        case 'copy':
          this.handleCopy(row)
          break
        case 'delete':
          this.handleDelete(row)
          break
        case 'log':
          this.handleViewLog(row)
          break
        case 'lineage':
          this.handleLineage(row)
          break
        case 'compare':
          this.handleCompare(row)
          break
      }
    },
    
    // Cron配置相关方法
    handleCronTemplateChange(value) {
      if (value === 'visual' || value === 'custom') {
        // 可视化配置或自定义，不自动设置表达式
        return
      }
      // 选择了常用模板，直接设置表达式
      this.form.cronExpression = value
      this.parseCronExpression()
    },
    
    // 从可视化配置生成Cron表达式
    updateCronFromVisual() {
      const { period, weekDay, dayOfMonth, time, interval, unit } = this.cronVisual
      
      // 解析时间 HH:mm
      const [hour, minute] = time ? time.split(':') : ['0', '0']
      
      let cron = ''
      
      if (period === 'day') {
        // 每天的指定时间
        cron = `0 ${minute} ${hour} * * ?`
        this.cronDescription = `每天${time}执行`
      } else if (period === 'week') {
        // 每周的指定时间
        cron = `0 ${minute} ${hour} ? * ${weekDay}`
        const weekDayMap = {
          'MON': '周一', 'TUE': '周二', 'WED': '周三', 
          'THU': '周四', 'FRI': '周五', 'SAT': '周六', 'SUN': '周日'
        }
        this.cronDescription = `每${weekDayMap[weekDay]}${time}执行`
      } else if (period === 'month') {
        // 每月的指定日期和时间
        cron = `0 ${minute} ${hour} ${dayOfMonth} * ?`
        this.cronDescription = `每月${dayOfMonth}号${time}执行`
      } else if (period === 'custom') {
        // 自定义间隔
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
      
      this.form.cronExpression = cron
    },
    
    // 解析Cron表达式
    parseCronExpression() {
      const cron = this.form.cronExpression
      if (!cron || !cron.trim()) {
        this.cronDescription = ''
        return
      }
      
      // 简单解析常见格式
      const parts = cron.trim().split(/\s+/)
      if (parts.length !== 6) {
        this.cronDescription = '请检查表达式格式（应为6个部分）'
        return
      }
      
      const [second, minute, hour, day, month, week] = parts
      
      // 常见模式匹配
      if (cron === '0 */5 * * * ?') {
        this.cronDescription = '每5分钟执行一次'
      } else if (cron === '0 */30 * * * ?') {
        this.cronDescription = '每30分钟执行一次'
      } else if (cron === '0 0 * * * ?') {
        this.cronDescription = '每小时整点执行'
      } else if (cron === '0 0 2 * * ?') {
        this.cronDescription = '每天凌晨2:00执行'
      } else if (cron === '0 0 2 ? * MON') {
        this.cronDescription = '每周一凌晨2:00执行'
      } else if (cron === '0 0 9 ? * MON-FRI') {
        this.cronDescription = '工作日上午9:00执行'
      } else if (cron === '0 0 2 1 * ?') {
        this.cronDescription = '每月1号凌晨2:00执行'
      } else {
        // 通用解析
        let desc = []
        
        // 分钟
        if (minute.includes('*/')) {
          desc.push(`每${minute.replace('*/', '')}分钟`)
        } else if (minute !== '*' && minute !== '0') {
          desc.push(`${minute}分`)
        }
        
        // 小时
        if (hour.includes('*/')) {
          desc.push(`每${hour.replace('*/', '')}小时`)
        } else if (hour !== '*') {
          desc.push(`${hour}点`)
        }
        
        // 日
        if (day !== '*' && day !== '?') {
          if (day.includes('*/')) {
            desc.push(`每${day.replace('*/', '')}天`)
          } else {
            desc.push(`${day}号`)
          }
        }
        
        // 月
        if (month !== '*') {
          desc.push(`${month}月`)
        }
        
        // 周
        if (week !== '*' && week !== '?') {
          const weekMap = {
            'MON': '周一', 'TUE': '周二', 'WED': '周三',
            'THU': '周四', 'FRI': '周五', 'SAT': '周六', 'SUN': '周日',
            '1': '周日', '2': '周一', '3': '周二', '4': '周三',
            '5': '周四', '6': '周五', '7': '周六'
          }
          if (week.includes('-')) {
            desc.push(`${weekMap[week.split('-')[0]]}至${weekMap[week.split('-')[1]]}`)
          } else {
            desc.push(weekMap[week] || week)
          }
        }
        
        this.cronDescription = desc.length > 0 ? desc.join(' ') + ' 执行' : '自定义表达式'
      }
    },
    
    // 复制任务
    handleCopy(row) {
      this.copyForm.taskId = row.id
      this.copyForm.originalTaskName = row.taskName
      this.copyForm.newTaskName = row.taskName + '_副本'
      this.copyDialogVisible = true
    },
    
    handleConfirmCopy() {
      if (!this.copyForm.newTaskName || this.copyForm.newTaskName.trim() === '') {
        this.$message.warning('请输入新任务名称')
        return
      }
      
      this.copyLoading = true
      this.$axios.post(`/v1/task/${this.copyForm.taskId}/copy`, {
        taskName: this.copyForm.newTaskName.trim()
      }).then(res => {
        this.$message.success('复制成功！新任务ID: ' + res.data)
        this.copyDialogVisible = false
        this.loadData()
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message 
          ? err.response.data.message 
          : '复制失败'
        this.$message.error(errMsg)
      }).finally(() => {
        this.copyLoading = false
      })
    },
    handleExecute(row) {
      this.$confirm('确认执行该任务吗？', '提示', { type: 'info' }).then(() => {
        this.$axios.post(`/v1/task/${row.id}/execute`).then(() => {
          this.$message.success('任务已提交执行')
        })
      })
    },
    handleToggleStatus(row) {
      const newStatus = row.status === 0 ? 1 : 0
      const payload = { ...row, status: newStatus }
      this.$axios.put('/v1/task', payload).then(() => {
        // 如果是启用操作且为定时任务,则启动调度
        if (newStatus === 1 && row.scheduleType === 'CRON') {
          return this.$axios.post(`/v1/task/${row.id}/start`).then(() => {
            this.$message.success('已启用,定时调度已启动')
            this.loadData()
          }).catch(err => {
            console.warn('启动调度失败:', err)
            this.$message.warning('已启用,但调度启动失败,请手动启动')
            this.loadData()
          })
        } else if (newStatus === 0 && row.scheduleType === 'CRON') {
          // 如果是禁用操作且为定时任务,则停止调度
          return this.$axios.post(`/v1/task/${row.id}/stop`).then(() => {
            this.$message.success('已禁用,定时调度已停止')
            this.loadData()
          }).catch(err => {
            console.warn('停止调度失败:', err)
            this.$message.success('已禁用')
            this.loadData()
          })
        } else {
          this.$message.success(newStatus === 1 ? '已启用' : '已禁用')
          this.loadData()
        }
      })
    },
    handleConfig(row) {
      this.currentTaskId = row.id
      this.$axios.get(`/v1/task/${row.id}/mappings`).then(res => {
        this.mappings = res.data.length > 0 ? res.data : []
        this.mappingDialogVisible = true
      })
    },
    handleAddMapping() {
      this.mappings.push({ sourceField: '', targetField: '', transformType: 'DIRECT' })
    },
    handleDeleteMapping(index) {
      this.mappings.splice(index, 1)
    },
    handleSaveMappings() {
      this.$axios.post(`/v1/task/${this.currentTaskId}/mappings`, this.mappings).then(() => {
        this.$message.success('保存成功')
        this.mappingDialogVisible = false
      })
    },
    
    // 执行日志
    handleViewLog(row) {
      this.$router.push({
        path: '/log',
        query: { taskId: row.id, taskName: row.taskName }
      })
    },
    
    // 数据血缘
    handleLineage(row) {
      this.currentTaskId = row.id
      this.lineageDialogVisible = true
      
      // 默认加载源连接器的表
      if (row.sourceConnectorId) {
        this.lineageConfig.connectorId = row.sourceConnectorId
        this.loadLineageTables()
      }
    },
    
    loadLineageTables() {
      if (!this.lineageConfig.connectorId) return
      
      this.$axios.get(`/v1/task/connector/${this.lineageConfig.connectorId}/tables`).then(res => {
        this.lineageTableList = res.data || []
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('加载表列表失败: ' + errMsg)
      })
    },
    
    loadLineageFields() {
      if (!this.lineageConfig.connectorId || !this.lineageConfig.tableName) return
      
      this.$axios.get(`/v1/dict-source/connector/${this.lineageConfig.connectorId}/table/${this.lineageConfig.tableName}/columns-with-type`).then(res => {
        this.lineageFieldList = res.data || []
      }).catch(err => {
        const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
        this.$message.error('加载字段列表失败: ' + errMsg)
      })
    },
    
    queryLineage() {
      if (!this.lineageConfig.connectorId || !this.lineageConfig.tableName || !this.lineageConfig.fieldName || this.lineageConfig.fieldName.length === 0) {
        this.$message.warning('请选择数据源、表名和字段名')
        return
      }
      
      this.lineageLoading = true
      
      // 如果只选了一个字段，直接查询
      if (this.lineageConfig.fieldName.length === 1) {
        this.$axios.post('/v1/data-lineage/graph', {
          connectorId: this.lineageConfig.connectorId,
          tableName: this.lineageConfig.tableName,
          fieldName: this.lineageConfig.fieldName[0]
        }).then(res => {
          this.lineageGraphData = res.data
          this.handleLineageQueryResult()
        }).catch(err => {
          const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
          this.$message.error('查询失败: ' + errMsg)
        }).finally(() => {
          this.lineageLoading = false
        })
      } else {
        // 多字段查询，合并结果
        const promises = this.lineageConfig.fieldName.map(fieldName => {
          return this.$axios.post('/v1/data-lineage/graph', {
            connectorId: this.lineageConfig.connectorId,
            tableName: this.lineageConfig.tableName,
            fieldName: fieldName
          })
        })
        
        Promise.all(promises).then(results => {
          // 合并所有结果
          const mergedNodes = []
          const mergedEdges = []
          const nodeIds = new Set()
          const edgeKeys = new Set()
          
          results.forEach(res => {
            const data = res.data
            // 合并节点
            if (data.nodes) {
              data.nodes.forEach(node => {
                if (!nodeIds.has(node.id)) {
                  nodeIds.add(node.id)
                  mergedNodes.push(node)
                }
              })
            }
            // 合并边
            if (data.edges) {
              data.edges.forEach(edge => {
                const edgeKey = `${edge.source}-${edge.target}`
                if (!edgeKeys.has(edgeKey)) {
                  edgeKeys.add(edgeKey)
                  mergedEdges.push(edge)
                }
              })
            }
          })
          
          this.lineageGraphData = {
            nodes: mergedNodes,
            edges: mergedEdges,
            currentNode: null // 多字段时不设置当前节点
          }
          
          this.handleLineageQueryResult()
        }).catch(err => {
          const errMsg = err.response && err.response.data && err.response.data.message ? err.response.data.message : err.message
          this.$message.error('查询失败: ' + errMsg)
        }).finally(() => {
          this.lineageLoading = false
        })
      }
    },
    
    // 处理血缘查询结果
    handleLineageQueryResult() {
      if (!this.lineageGraphData.edges || this.lineageGraphData.edges.length === 0) {
        this.$message.warning({
          message: '未找到该字段的血缘关系，请确认：1. 任务是否已经执行 2. 该字段是否在字段映射中配置',
          duration: 5000
        })
      } else {
        const edgeCount = this.lineageGraphData.edges.length
        const nodeCount = this.lineageGraphData.nodes.length
        this.$message.success(`血缘查询成功，找到 ${edgeCount} 条血缘关系，共 ${nodeCount} 个节点`)
        
        // 保存到最近查询记录
        this.saveRecentLineageQuery()
        
        // 如果是拓扑图模式，渲染图形
        if (this.lineageViewMode === 'graph') {
          this.$nextTick(() => {
            this.renderLineageGraph()
          })
        }
      }
    },
    
    // 保存最近查询记录
    saveRecentLineageQuery() {
      const query = {
        key: `${this.lineageConfig.connectorId}_${this.lineageConfig.tableName}_${this.lineageConfig.fieldName.join(',')}`,
        connectorId: this.lineageConfig.connectorId,
        tableName: this.lineageConfig.tableName,
        fieldName: [...this.lineageConfig.fieldName]
      }
      
      // 去重
      this.recentLineageQueries = this.recentLineageQueries.filter(q => q.key !== query.key)
      // 添加到头部
      this.recentLineageQueries.unshift(query)
      // 只保留最近 5 条
      if (this.recentLineageQueries.length > 5) {
        this.recentLineageQueries = this.recentLineageQueries.slice(0, 5)
      }
      
      // 保存到 localStorage
      try {
        localStorage.setItem('recentLineageQueries', JSON.stringify(this.recentLineageQueries))
      } catch (e) {
        console.error('保存查询记录失败', e)
      }
    },
    
    // 加载最近查询
    loadRecentQuery(item) {
      this.lineageConfig.connectorId = item.connectorId
      this.lineageConfig.tableName = item.tableName
      this.lineageConfig.fieldName = [...item.fieldName]
      
      // 加载表列表和字段列表
      this.loadLineageTables()
      this.loadLineageFields()
      
      // 自动查询
      this.$nextTick(() => {
        this.queryLineage()
      })
    },
    
    // 快捷搜索字段
    searchLineageFields(queryString, cb) {
      // 这里可以调用后端接口搜索所有表的字段
      // 为了简单，这里只介绍架构
      if (!queryString || queryString.length < 2) {
        cb([])
        return
      }
      
      // TODO: 调用后端搜索接口
      // 暂时使用当前字段列表
      const results = this.lineageFieldList
        .filter(f => f.columnName.toLowerCase().includes(queryString.toLowerCase()))
        .map(f => ({
          tableName: this.lineageConfig.tableName,
          fieldName: f.columnName,
          connectorName: this.getConnectorName(this.lineageConfig.connectorId)
        }))
      
      cb(results)
    },
    
    // 快捷搜索选中
    handleLineageSearchSelect(item) {
      this.lineageConfig.tableName = item.tableName
      this.lineageConfig.fieldName = [item.fieldName]
      this.loadLineageFields()
      
      // 自动查询
      this.$nextTick(() => {
        this.queryLineage()
      })
    },
    
    // 清空查询条件
    clearLineageQuery() {
      this.lineageConfig = {
        connectorId: null,
        tableName: '',
        fieldName: []
      }
      this.lineageTableList = []
      this.lineageFieldList = []
      this.lineageGraphData = null
      this.lineageSearchInput = ''
    },
    
    // 下针查询
    drillDownLineage(node) {
      if (!node || !node.connectorId || !node.table || !node.label) {
        this.$message.warning('无效的节点信息')
        return
      }
      
      // 设置查询条件
      this.lineageConfig.connectorId = node.connectorId
      this.lineageConfig.tableName = node.table
      this.lineageConfig.fieldName = [node.label]
      
      // 加载表列表和字段列表
      this.loadLineageTables()
      this.loadLineageFields()
      
      // 查询
      this.$nextTick(() => {
        this.queryLineage()
      })
    },
    
    // 过滤上游血缘
    getFilteredUpstreamEdges() {
      const upstream = this.getUpstreamEdges()
      return this.filterLineageEdges(upstream)
    },
    
    // 过滤下游血缘
    getFilteredDownstreamEdges() {
      const downstream = this.getDownstreamEdges()
      return this.filterLineageEdges(downstream)
    },
    
    // 过滤血缘边
    filterLineageEdges(edges) {
      let filtered = edges
      
      // 按转换类型过滤
      if (this.lineageFilterType) {
        filtered = filtered.filter(edge => {
          const type = edge.transformType || 'DIRECT'
          return type === this.lineageFilterType
        })
      }
      
      // 按关键词搜索
      if (this.lineageSearchKeyword) {
        const keyword = this.lineageSearchKeyword.toLowerCase()
        filtered = filtered.filter(edge => {
          const sourceNode = this.getNodeById(edge.source)
          const targetNode = this.getNodeById(edge.target)
          return (
            (sourceNode && sourceNode.label && sourceNode.label.toLowerCase().includes(keyword)) ||
            (targetNode && targetNode.label && targetNode.label.toLowerCase().includes(keyword)) ||
            (edge.transformRule && edge.transformRule.toLowerCase().includes(keyword))
          )
        })
      }
      
      return filtered
    },
    
    // 表格行类名
    tableRowClassName({ row }) {
      // 可以根据转换类型添加不同样式
      return ''
    },
    
    // 渲染血缘拓扑图
    renderLineageGraph() {
      // 检查是否有 ECharts，如果没有则提示
      if (typeof echarts === 'undefined') {
        this.$message.warning('请先引入 ECharts 库')
        this.lineageViewMode = 'table'
        return
      }
      
      const container = document.getElementById('lineageGraph')
      if (!container) return
      
      // 销毁旧实例
      if (this.lineageGraphInstance) {
        this.lineageGraphInstance.dispose()
      }
      
      // 创建新实例
      this.lineageGraphInstance = echarts.init(container)
      
      // 构造图数据
      const nodes = this.lineageGraphData.nodes.map((node, index) => ({
        id: node.id,
        name: `${node.table}.${node.label}`,
        symbolSize: 50,
        x: (index % 5) * 150,
        y: Math.floor(index / 5) * 100,
        itemStyle: {
          color: node.nodeType === 'SOURCE' ? '#409EFF' : '#67C23A'
        },
        label: {
          show: true,
          fontSize: 11
        }
      }))
      
      const links = this.lineageGraphData.edges.map(edge => ({
        source: edge.source,
        target: edge.target,
        label: {
          show: true,
          formatter: edge.transformType || 'DIRECT',
          fontSize: 10
        },
        lineStyle: {
          curveness: 0.3
        }
      }))
      
      // 配置选项
      const option = {
        tooltip: {
          formatter: function(params) {
            if (params.dataType === 'edge') {
              return `${params.data.label.formatter}<br/>${params.data.transformRule || ''}`
            }
            return params.name
          }
        },
        series: [{
          type: 'graph',
          layout: 'force',
          data: nodes,
          links: links,
          roam: true,
          draggable: true,
          force: {
            repulsion: 200,
            edgeLength: 150
          },
          emphasis: {
            focus: 'adjacency',
            lineStyle: {
              width: 3
            }
          }
        }]
      }
      
      this.lineageGraphInstance.setOption(option)
      
      // 点击节点事件
      this.lineageGraphInstance.on('click', (params) => {
        if (params.dataType === 'node') {
          const node = this.lineageGraphData.nodes.find(n => n.id === params.data.id)
          if (node) {
            this.drillDownLineage(node)
          }
        }
      })
    },
    
    // 数据对比
    handleCompare(row) {
      this.currentTaskId = row.id
      this.compareDialogVisible = true
      
      // 清空之前的对比结果，避免显示旧数据
      this.compareResult = null
      
      // 预填充任务的源和目标连接器
      this.compareConfig.sourceConnectorId = row.sourceConnectorId
      this.compareConfig.targetConnectorId = row.targetConnectorId
      
      // 加载表列表
      this.loadSourceTables()
      this.loadTargetTables()
      
      // 尝试从任务配置中提取表名
      try {
        if (row.sourceConfig) {
          const sourceConfig = JSON.parse(row.sourceConfig)
          this.compareConfig.sourceTableOrSql = sourceConfig.tableName || sourceConfig.sql || ''
        }
        if (row.targetConfig) {
          const targetConfig = JSON.parse(row.targetConfig)
          this.compareConfig.targetTableOrSql = targetConfig.tableName || ''
        }
      } catch (e) {
        console.error('解析任务配置失败', e)
      }
      
      // 加载字段映射作为字段选项
      this.$axios.get(`/v1/task/${row.id}/mappings`).then(res => {
        const mappings = res.data || []
        if (mappings.length > 0) {
          // 提取所有源字段和目标字段
          const sourceFields = [...new Set(mappings.map(m => m.sourceField).filter(f => f))]
          const targetFields = [...new Set(mappings.map(m => m.targetField).filter(f => f))]
          
          // 设置字段选项
          this.compareKeyFieldOptions = sourceFields
          this.compareFieldOptions = [...sourceFields, ...targetFields.filter(f => !sourceFields.includes(f))]
        }
      }).catch(err => {
        console.error('加载字段映射失败', err)
      })
    },
    
    // 加载源表列表
    loadSourceTables() {
      if (!this.compareConfig.sourceConnectorId) return
      this.$axios.get(`/v1/task/connector/${this.compareConfig.sourceConnectorId}/tables`).then(res => {
        this.compareSourceTables = res.data || []
      }).catch(() => {
        this.compareSourceTables = []
      })
    },
    
    // 加载目标表列表
    loadTargetTables() {
      if (!this.compareConfig.targetConnectorId) return
      this.$axios.get(`/v1/task/connector/${this.compareConfig.targetConnectorId}/tables`).then(res => {
        this.compareTargetTables = res.data || []
      }).catch(() => {
        this.compareTargetTables = []
      })
    },
    
    // 源连接器变化
    handleSourceConnectorChange() {
      this.compareConfig.sourceTableOrSql = ''
      this.loadSourceTables()
    },
    
    // 目标连接器变化
    handleTargetConnectorChange() {
      this.compareConfig.targetTableOrSql = ''
      this.loadTargetTables()
    },
    
    // 查询源表（自动补全）
    querySourceTables(queryString, cb) {
      const results = queryString
        ? this.compareSourceTables.filter(t => t.toLowerCase().includes(queryString.toLowerCase()))
        : this.compareSourceTables
      cb(results.map(t => ({ value: t })))
    },
    
    // 查询目标表（自动补全）
    queryTargetTables(queryString, cb) {
      const results = queryString
        ? this.compareTargetTables.filter(t => t.toLowerCase().includes(queryString.toLowerCase()))
        : this.compareTargetTables
      cb(results.map(t => ({ value: t })))
    },
    
    // 使用任务配置填充
    loadTaskConfigToCompare() {
      this.$axios.get(`/v1/task/${this.currentTaskId}`).then(res => {
        const task = res.data
        this.compareConfig.sourceConnectorId = task.sourceConnectorId
        this.compareConfig.targetConnectorId = task.targetConnectorId
        
        try {
          if (task.sourceConfig) {
            const sourceConfig = JSON.parse(task.sourceConfig)
            this.compareConfig.sourceTableOrSql = sourceConfig.tableName || sourceConfig.sql || ''
          }
          if (task.targetConfig) {
            const targetConfig = JSON.parse(task.targetConfig)
            this.compareConfig.targetTableOrSql = targetConfig.tableName || ''
          }
        } catch (e) {
          this.$message.error('解析任务配置失败')
        }
        
        // 加载表列表
        this.loadSourceTables()
        this.loadTargetTables()
        
        // 加载字段映射作为参考
        this.$axios.get(`/v1/task/${this.currentTaskId}/mappings`).then(mappingRes => {
          const mappings = mappingRes.data || []
          if (mappings.length > 0) {
            // 提取所有源字段和目标字段
            const sourceFields = [...new Set(mappings.map(m => m.sourceField))]
            const targetFields = [...new Set(mappings.map(m => m.targetField))]
            
            // 设置字段选项
            this.compareKeyFieldOptions = sourceFields
            this.compareFieldOptions = [...sourceFields, ...targetFields.filter(f => !sourceFields.includes(f))]
            
            this.$message.success('已加载任务配置，请选择主键字段和对比字段')
          }
        })
      })
    },
    
    executeCompare() {
      if (!this.compareConfig.sourceConnectorId || !this.compareConfig.targetConnectorId) {
        this.$message.warning('请选择源和目标连接器')
        return
      }
      if (!this.compareConfig.sourceTableOrSql || !this.compareConfig.targetTableOrSql) {
        this.$message.warning('请输入源和目标表名或SQL')
        return
      }
      if (!this.compareConfig.keyFields || this.compareConfig.keyFields.length === 0) {
        this.$message.warning('请选择主键字段')
        return
      }
      
      this.compareLoading = true
      
      // 处理字段：如果是数组直接使用，否则解析字符串
      const keyFieldsArray = Array.isArray(this.compareConfig.keyFields) 
        ? this.compareConfig.keyFields 
        : this.compareConfig.keyFields.split(',').map(f => f.trim())
        
      const compareFieldsArray = Array.isArray(this.compareConfig.compareFields)
        ? this.compareConfig.compareFields
        : (this.compareConfig.compareFields ? this.compareConfig.compareFields.split(',').map(f => f.trim()) : [])
      
      this.$axios.post('/v1/data-compare/compare', {
        sourceConnectorId: this.compareConfig.sourceConnectorId,
        sourceTableOrSql: this.compareConfig.sourceTableOrSql,
        targetConnectorId: this.compareConfig.targetConnectorId,
        targetTableOrSql: this.compareConfig.targetTableOrSql,
        keyFields: keyFieldsArray,
        compareFields: compareFieldsArray.length > 0 ? compareFieldsArray : null
      }).then(res => {
        this.compareResult = res.data
        this.$message.success('对比完成')
      }).catch(err => {
        // 清空结果，防止显示错误的空数据
        this.compareResult = null
        
        // 解析错误信息
        let errMsg = '未知错误'
        if (err.response && err.response.data) {
          if (err.response.data.message) {
            errMsg = err.response.data.message
          } else if (typeof err.response.data === 'string') {
            errMsg = err.response.data
          }
        } else if (err.message) {
          errMsg = err.message
        }
        
        // 根据错误类型提供友好的提示
        if (errMsg.includes('Access denied')) {
          this.$message.error('数据库连接失败：用户名或密码错误，请检查连接器配置')
        } else if (errMsg.includes('Unknown database')) {
          this.$message.error('数据库不存在，请检查数据库名称是否正确')
        } else if (errMsg.includes('Table') && errMsg.includes("doesn't exist")) {
          this.$message.error('表不存在，请检查表名是否正确')
        } else if (errMsg.includes('Connection refused')) {
          this.$message.error('无法连接到数据库，请检查数据库服务是否启动')
        } else {
          this.$message.error('对比失败: ' + errMsg)
        }
      }).finally(() => {
        this.compareLoading = false
      })
    },
    
    // 获取节点标签(从 nodeId 中解析: connectorId:tableName:fieldName)
    getNodeLabel(nodeId, type) {
      if (!nodeId) return ''
      const parts = nodeId.split(':')
      if (type === 'table') {
        return parts[1] || ''
      } else if (type === 'field') {
        return parts[2] || ''
      }
      return nodeId
    },
    
    // 根据节点ID获取节点对象
    getNodeById(nodeId) {
      if (!this.lineageGraphData || !this.lineageGraphData.nodes) return {}
      const node = this.lineageGraphData.nodes.find(n => n.id === nodeId)
      return node || {}
    },
    
    // 获取连接器名称
    getConnectorName(connectorId) {
      if (!connectorId) return ''
      const connector = this.connectorList.find(c => c.id === connectorId)
      return connector ? connector.connectorName : `连接器#${connectorId}`
    },
    
    // 获取上游边（指向当前字段的边，即当前字段作为目标）
    getUpstreamEdges() {
      if (!this.lineageGraphData || !this.lineageGraphData.edges) return []
      const currentNodeId = this.lineageGraphData.currentNode
      const selectedFields = Array.isArray(this.lineageConfig.fieldName) ? this.lineageConfig.fieldName : [this.lineageConfig.fieldName]
      
      // 查找所有target包含当前字段的边
      return this.lineageGraphData.edges.filter(e => {
        if (e.target === currentNodeId) return true
        const targetNode = this.getNodeById(e.target)
        return targetNode && 
               selectedFields.includes(targetNode.label) && 
               targetNode.table === this.lineageConfig.tableName
      })
    },
    
    // 获取下游边（从当前字段出发的边，即当前字段作为源）
    getDownstreamEdges() {
      if (!this.lineageGraphData || !this.lineageGraphData.edges) return []
      const currentNodeId = this.lineageGraphData.currentNode
      const selectedFields = Array.isArray(this.lineageConfig.fieldName) ? this.lineageConfig.fieldName : [this.lineageConfig.fieldName]
      
      // 查找所有source包含当前字段的边
      return this.lineageGraphData.edges.filter(e => {
        if (e.source === currentNodeId) return true
        const sourceNode = this.getNodeById(e.source)
        return sourceNode && 
               selectedFields.includes(sourceNode.label) && 
               sourceNode.table === this.lineageConfig.tableName
      })
    },
    
    // 获取转换类型颜色
    getTransformTypeColor(transformType) {
      const colorMap = {
        'DIRECT': '',
        'FUNCTION': 'warning',
        'DICT': 'success',
        'SCRIPT': 'danger',
        'CONDITION': 'info'
      }
      return colorMap[transformType] || ''
    },
    
    // 显示匹配记录详情
    showMatchDetails() {
      if (!this.compareResult || !this.compareResult.matchCount) {
        this.$message.info('暂无匹配记录')
        return
      }
      
      // 如果后端已经返回了matchDetail数据，直接使用
      if (this.compareResult.matchDetail && this.compareResult.matchDetail.length > 0) {
        this.compareDetailTitle = `匹配记录详情 (共 ${this.compareResult.matchCount} 条)`
        this.compareDetailType = 'match'
        this.compareDetailData = this.compareResult.matchDetail.map(match => ({
          key: match.key,
          diffFields: [],  // 匹配记录没有差异字段
          sourceData: match.sourceRow || {},
          targetData: match.targetRow || {}
        }))
        // 提取所有字段名作为表格列
        this.extractTableColumns()
        this.detailViewMode = 'table'
        this.compareDetailDialogVisible = true
      } else {
        this.$message.warning('后端未返回匹配明细数据，请检查后端接口')
      }
    },
    
    // 显示差异记录详情
    showDiffDetails() {
      if (!this.compareResult || !this.compareResult.diffCount) {
        this.$message.info('暂无差异记录')
        return
      }
      
      // 如果后端已经返回了differences数据，直接使用
      if (this.compareResult.differences && this.compareResult.differences.length > 0) {
        this.compareDetailTitle = `差异记录详情 (共 ${this.compareResult.diffCount} 条)`
        this.compareDetailType = 'diff'
        // 过滤出真正的差异记录（不包括SOURCE_ONLY和TARGET_ONLY）
        const diffOnly = this.compareResult.differences.filter(d => d.type === 'DIFF')
        this.compareDetailData = diffOnly.map(diff => ({
          key: diff.key,
          diffFields: diff.diffFields ? Object.keys(diff.diffFields) : [],
          sourceData: diff.sourceRow || {},
          targetData: diff.targetRow || {}
        }))
        this.extractTableColumns()
        this.detailViewMode = 'table'
        this.compareDetailDialogVisible = true
      } else {
        this.$message.warning('后端未返回差异明细数据，请检查后端接口')
      }
    },
    
    // 显示仅源端存在记录详情
    showSourceOnlyDetails() {
      if (!this.compareResult || !this.compareResult.sourceOnlyCount) {
        this.$message.info('暂无仅源端存在的记录')
        return
      }
      
      // 如果后端已经返回了sourceOnlyDetail数据，直接使用
      if (this.compareResult.sourceOnlyDetail && this.compareResult.sourceOnlyDetail.length > 0) {
        this.compareDetailTitle = `仅源端存在记录 (共 ${this.compareResult.sourceOnlyCount} 条)`
        this.compareDetailType = 'sourceOnly'
        this.compareDetailData = this.compareResult.sourceOnlyDetail.map(record => ({
          key: record.key,
          diffFields: [],
          sourceData: record.sourceRow || {},
          targetData: null  // 目标端无数据
        }))
        this.extractTableColumns()
        this.detailViewMode = 'table'
        this.compareDetailDialogVisible = true
      } else {
        this.$message.warning('后端未返回明细数据，请检查后端接口')
      }
    },
    
    // 显示仅目标端存在记录详情
    showTargetOnlyDetails() {
      if (!this.compareResult || !this.compareResult.targetOnlyCount) {
        this.$message.info('暂无仅目标端存在的记录')
        return
      }
      
      // 如果后端已经返回了targetOnlyDetail数据，直接使用
      if (this.compareResult.targetOnlyDetail && this.compareResult.targetOnlyDetail.length > 0) {
        this.compareDetailTitle = `仅目标端存在记录 (共 ${this.compareResult.targetOnlyCount} 条)`
        this.compareDetailType = 'targetOnly'
        this.compareDetailData = this.compareResult.targetOnlyDetail.map(record => ({
          key: record.key,
          diffFields: [],
          sourceData: null,  // 源端无数据
          targetData: record.targetRow || {}
        }))
        this.extractTableColumns()
        this.detailViewMode = 'table'
        this.compareDetailDialogVisible = true
      } else {
        this.$message.warning('后端未返回明细数据，请检查后端接口')
      }
    },
    
    // 显示单条记录详情
    showRowDetail(row) {
      this.currentRowDetail = {
        sourceData: row.sourceData || {},
        targetData: row.targetData || null,
        diffFields: row.diffFields || []
      }
      this.rowDetailDialogVisible = true
    },
    
    // 判断字段是否是差异字段
    isFieldDiff(fieldName) {
      return this.currentRowDetail.diffFields.includes(fieldName)
    },
    
    // 格式化JSON显示
    formatJson(data) {
      if (!data) return '-'
      if (typeof data === 'object') {
        return JSON.stringify(data, null, 2)
      }
      return data
    },
    
    // 格式化字段值显示
    formatValue(value) {
      if (value === null || value === undefined) return '-'
      if (typeof value === 'object') {
        return JSON.stringify(value)
      }
      // 限制长度，太长的字符串截断
      const str = String(value)
      return str.length > 50 ? str.substring(0, 50) + '...' : str
    },
    
    // 提取表格列（从数据中获取所有字段名）
    extractTableColumns() {
      const columnsSet = new Set()
      
      this.compareDetailData.forEach(row => {
        if (row.sourceData) {
          Object.keys(row.sourceData).forEach(key => columnsSet.add(key))
        }
        if (row.targetData) {
          Object.keys(row.targetData).forEach(key => columnsSet.add(key))
        }
      })
      
      this.detailTableColumns = Array.from(columnsSet)
    },
    
    // 判断字段在当前行是否为差异字段
    isFieldDiffInRow(row, fieldName) {
      return row.diffFields && row.diffFields.includes(fieldName)
    },
    
    // 导出详情数据
    exportDetailData() {
      if (!this.compareDetailData || this.compareDetailData.length === 0) {
        this.$message.warning('暂无数据可导出')
        return
      }
      
      // 生成CSV数据
      let headers = ['主键']
      
      // 根据类型添加不同的列
      if (this.compareDetailType === 'diff') {
        headers.push('差异字段')
      }
      if (this.compareDetailType !== 'targetOnly') {
        headers.push('源端数据')
      }
      if (this.compareDetailType !== 'sourceOnly') {
        headers.push('目标端数据')
      }
      
      const rows = this.compareDetailData.map(row => {
        const rowData = [row.key]
        
        if (this.compareDetailType === 'diff') {
          rowData.push((row.diffFields || []).join(', '))
        }
        if (this.compareDetailType !== 'targetOnly') {
          rowData.push(JSON.stringify(row.sourceData || {}))
        }
        if (this.compareDetailType !== 'sourceOnly') {
          rowData.push(JSON.stringify(row.targetData || {}))
        }
        
        return rowData
      })
      
      let csvContent = headers.join(',') + '\n'
      rows.forEach(row => {
        csvContent += row.map(cell => `"${cell}"`).join(',') + '\n'
      })
      
      // 下载文件
      const blob = new Blob(["\ufeff" + csvContent], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      
      // 根据类型生成文件名
      const typeNames = {
        'match': '匹配记录',
        'diff': '差异记录',
        'sourceOnly': '仅源端存在',
        'targetOnly': '仅目标端存在'
      }
      const typeName = typeNames[this.compareDetailType] || '对比结果'
      link.download = `${typeName}_${new Date().getTime()}.csv`
      link.click()
      
      this.$message.success('导出成功')
    }
  }
}
</script>

<style scoped>
.diff-field-label {
  background-color: #FEF0F0 !important;
  color: #F56C6C !important;
  font-weight: bold;
}

.diff-field-value {
  color: #F56C6C;
  font-weight: bold;
}

/* 页面整体样式 */
.task-container {
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
</style>
