<template>
  <el-dialog title="从模板创建任务" :visible.sync="visible" width="900px" @close="handleClose">
    <div style="margin-bottom: 20px">
      <el-input 
        v-model="searchKeyword" 
        placeholder="搜索模板..." 
        prefix-icon="el-icon-search"
        clearable
        style="width: 300px" />
    </div>

    <el-row :gutter="20">
      <el-col :span="8" v-for="template in filteredTemplates" :key="template.id">
        <el-card shadow="hover" class="template-card" @click.native="handleSelectTemplate(template)">
          <div class="template-icon">
            <i :class="template.icon" style="font-size: 36px; color: #409EFF"></i>
          </div>
          <div class="template-title">{{ template.name }}</div>
          <div class="template-desc">{{ template.description }}</div>
          <div class="template-tags">
            <el-tag size="mini" type="primary">{{ template.syncMode === 'FULL' ? '全量' : '增量' }}</el-tag>
            <el-tag size="mini" type="success" style="margin-left: 5px">{{ template.scheduleType === 'MANUAL' ? '手动' : '定时' }}</el-tag>
            <el-tag v-if="template.connectorType" size="mini" :type="getConnectorTypeTagType(template.connectorType)" style="margin-left: 5px">
              {{ getConnectorTypeLabel(template.connectorType) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div slot="footer">
      <el-button @click="visible = false">取消</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  data() {
    return {
      visible: false,
      searchKeyword: '',
      templates: [
        {
          id: 1,
          name: 'MySQL → MySQL 全量同步',
          description: '两个MySQL数据库之间的全量数据同步，适合定时全量更新场景',
          icon: 'el-icon-data-board',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          config: {
            taskName: 'MySQL全量同步任务',
            taskCode: 'mysql_full_sync',
            description: '每天凌晨２点执行全量数据同步，从源数据库users表同步到目标数据库target_users表',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 0 2 * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: 'SELECT id, username, email, phone, status, create_time FROM users',
              tableName: 'users'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_users',
              writeMode: 'INSERT'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'id', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'SKIP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'username', 
                targetField: 'username', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: 'unknown' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} },
                        { functionCode: 'LOWER', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'email', 
                targetField: 'email', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} },
                        { functionCode: 'LOWER', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'phone', 
                targetField: 'phone', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'status', 
                targetField: 'status', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: '0' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'create_time', 
                targetField: 'create_time', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              }
            ]
          }
        },
        {
          id: 2,
          name: 'MySQL → MySQL 增量同步',
          description: '基于时间戳字段的增量数据同步，只同步新增或修改的数据',
          icon: 'el-icon-refresh',
          syncMode: 'INCREMENTAL',
          scheduleType: 'CRON',
          config: {
            taskName: 'MySQL增量同步任务',
            taskCode: 'mysql_incremental_sync',
            description: '每10分钟执行一次，基于update_time字段增量同步，使用UPSERT模式避免数据重复',
            syncMode: 'INCREMENTAL',
            incrementalField: 'update_time',  // 增量字段
            scheduleType: 'CRON',
            cronExpression: '0 */10 * * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, username, email, phone, update_time FROM users WHERE update_time > '{last_sync_time}'",
              tableName: 'users'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_users',
              writeMode: 'UPSERT',
              primaryKey: 'id'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'id', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'SKIP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'username', 
                targetField: 'username', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: 'unknown' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'email', 
                targetField: 'email', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'phone', 
                targetField: 'phone', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'update_time', 
                targetField: 'update_time', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              }
            ]
          }
        },
        {
          id: 3,
          name: '手动执行单次同步',
          description: '手动触发的一次性数据同步任务，适合临时数据迁移',
          icon: 'el-icon-video-play',
          syncMode: 'FULL',
          scheduleType: 'MANUAL',
          config: {
            taskName: '手动同步任务',
            taskCode: 'manual_sync',
            description: '手动执行的数据迁移任务，适合临时性的数据导入导出场景',
            syncMode: 'FULL',
            scheduleType: 'MANUAL',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, product_name, price, category, status FROM products WHERE status = 1",
              tableName: 'products'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_products',
              writeMode: 'INSERT'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'product_id', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'SKIP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'product_name', 
                targetField: 'name', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: '未知商品' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'price', 
                targetField: 'price', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: '0' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'category', 
                targetField: 'category', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'status', 
                targetField: 'status', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              }
            ]
          }
        },
        {
          id: 4,
          name: '用户数据同步',
          description: '用户表数据同步模板，包含常用字段映射',
          icon: 'el-icon-user',
          syncMode: 'INCREMENTAL',
          scheduleType: 'CRON',
          config: {
            taskName: '用户数据同步',
            taskCode: 'user_data_sync',
            description: '同步用户表数据，包含id/用户名/邮箱/电话/创建时间/更新时间字段',
            syncMode: 'INCREMENTAL',
            incrementalField: 'update_time',  // 增量字段
            scheduleType: 'CRON',
            cronExpression: '0 */30 * * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, username, email, phone, create_time, update_time FROM users WHERE update_time > '{last_sync_time}'",
              tableName: 'users'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_users',
              writeMode: 'UPSERT',
              primaryKey: 'id'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'user_id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'username', 
                targetField: 'user_name', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: 'unknown' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'email', 
                targetField: 'email', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }, { functionCode: 'LOWER', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'phone', 
                targetField: 'mobile', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'create_time', 
                targetField: 'create_time', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'update_time', 
                targetField: 'update_time', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        },
        {
          id: 5,
          name: '订单数据同步',
          description: '订单表数据同步模板，支持状态字典映射',
          icon: 'el-icon-shopping-cart-2',
          syncMode: 'INCREMENTAL',
          scheduleType: 'CRON',
          config: {
            taskName: '订单数据同步',
            taskCode: 'order_data_sync',
            description: '同步订单数据，包含id/订单号/用户ID/金额/状态/创建时间，状态字段支持字典映射',
            syncMode: 'INCREMENTAL',
            incrementalField: 'create_time',  // 增量字段
            scheduleType: 'CRON',
            cronExpression: '0 */5 * * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, order_no, user_id, amount, status, create_time FROM orders WHERE create_time > '{last_sync_time}'",
              tableName: 'orders'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_orders',
              writeMode: 'UPSERT',
              primaryKey: 'id'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'order_id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'order_no', 
                targetField: 'order_number', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }, { functionCode: 'UPPER', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'user_id', 
                targetField: 'user_id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'amount', 
                targetField: 'order_amount', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'status', 
                targetField: 'order_status', 
                transformType: 'DICT',
                dictCode: 'order_status_mapping',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DICT' } }
                ]
              },
              { 
                sourceField: 'create_time', 
                targetField: 'create_time', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        },
        {
          id: 6,
          name: '每日凌晨全量同步',
          description: '每日凌晨２点执行的全量数据同步',
          icon: 'el-icon-time',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          config: {
            taskName: '每日凌晨全量同步',
            taskCode: 'daily_full_sync',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 0 2 * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: 'SELECT report_date, total_sales, order_count, user_count FROM daily_report',
              tableName: 'daily_report'
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'target_daily_report',
              writeMode: 'INSERT'
            },
            mappings: [
              { 
                sourceField: 'report_date', 
                targetField: 'date', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'total_sales', 
                targetField: 'sales_amount', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'order_count', 
                targetField: 'orders', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'user_count', 
                targetField: 'active_users', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        },
        {
          id: 7,
          name: 'API数据采集 → 数据库',
          description: '从第三方API获取数据，自动分页采集，存入数据库',
          icon: 'el-icon-download',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          connectorType: 'API_TO_DB',
          config: {
            taskName: 'API数据采集任务',
            taskCode: 'api_to_db_sync',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 */30 * * * ?',
            sourceConfig: {
              connectorId: 3,  // API演示源接口
              apiMethod: 'GET',
              apiPath: '/users',
              dataPath: '',
              apiParams: [
                { enabled: true, key: 'status', value: 'active' }
              ],
              headers: [
                { enabled: true, key: 'Accept', value: 'application/json' }
              ],
              enablePagination: true,
              pageParam: 'page',
              pageSizeParam: 'size',
              startPage: 1,
              pageSize: 50,
              totalPath: ''
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'api_users',
              writeMode: 'UPSERT',
              primaryKey: 'id'
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'user_id', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'SKIP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'name', 
                targetField: 'user_name', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: 'unknown' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'email', 
                targetField: 'email', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} },
                        { functionCode: 'LOWER', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'phone', 
                targetField: 'mobile', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              }
            ]
          }
        },
        {
          id: 8,
          name: '数据库 → API批量推送',
          description: '将数据库数据批量推送到第三方API，支持重试',
          icon: 'el-icon-upload2',
          syncMode: 'INCREMENTAL',
          scheduleType: 'CRON',
          connectorType: 'DB_TO_API',
          config: {
            taskName: '数据库到API推送',
            taskCode: 'db_to_api_push',
            syncMode: 'INCREMENTAL',
            incrementalField: 'update_time',  // 增量字段
            scheduleType: 'CRON',
            cronExpression: '0 */10 * * * ?',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, name, email, phone FROM users WHERE update_time > '{last_sync_time}'",
              tableName: 'users'
            },
            targetConfig: {
              connectorId: 4,  // API演示目标接口
              apiMethod: 'POST',
              apiPath: '/api/users/batch',
              batchMode: 'batch',
              batchSize: 100,
              wrapperField: 'data',
              retryTimes: 3,
              retryInterval: 1000,
              headers: [
                { enabled: true, key: 'Content-Type', value: 'application/json' }
              ]
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'userId', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'SKIP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'name', 
                targetField: 'userName', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'DEFAULT', defaultValue: 'unknown' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'email', 
                targetField: 'email', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'CLEANSE',
                    order: 2,
                    config: {
                      cleanseFunctions: [
                        { functionCode: 'TRIM', params: {} },
                        { functionCode: 'LOWER', params: {} }
                      ]
                    }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 3,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              },
              { 
                sourceField: 'phone', 
                targetField: 'mobile', 
                transformType: 'DIRECT',
                _processors: [
                  {
                    type: 'NULL_HANDLE',
                    order: 1,
                    config: { strategy: 'KEEP' }
                  },
                  {
                    type: 'TRANSFORM',
                    order: 2,
                    config: { transformType: 'DIRECT' }
                  }
                ]
              }
            ]
          }
        },
        {
          id: 9,
          name: 'API实时数据同步',
          description: '高频API调用，每5分钟获取最新数据',
          icon: 'el-icon-refresh-right',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          connectorType: 'API_TO_DB',
          config: {
            taskName: 'API实时数据同步',
            taskCode: 'api_realtime_sync',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 */5 * * * ?',
            sourceConfig: {
              connectorId: 3,  // API演示源接口
              apiMethod: 'GET',
              apiPath: '/posts',
              dataPath: '',
              apiParams: [
                { enabled: true, key: 'limit', value: '100' }
              ],
              headers: [
                { enabled: true, key: 'Accept', value: 'application/json' }
              ],
              enablePagination: false
            },
            targetConfig: {
              connectorId: 2,  // MySQL演示目标库
              tableName: 'api_orders',
              writeMode: 'UPSERT',
              primaryKey: 'order_id'
            },
            mappings: [
              { 
                sourceField: 'orderId', 
                targetField: 'order_id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'orderNo', 
                targetField: 'order_number', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'userId', 
                targetField: 'user_id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'amount', 
                targetField: 'total_amount', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'status', 
                targetField: 'order_status', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'createTime', 
                targetField: 'create_time', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        },
        {
          id: 10,
          name: '数据库 → API逐条推送',
          description: '每条数据分别调用API，适合单条处理场景',
          icon: 'el-icon-s-promotion',
          syncMode: 'INCREMENTAL',
          scheduleType: 'MANUAL',
          connectorType: 'DB_TO_API',
          config: {
            taskName: '逐条推送任务',
            taskCode: 'db_to_api_single',
            syncMode: 'INCREMENTAL',
            incrementalField: 'status',  // 增量字段（根据状态筛选）
            scheduleType: 'MANUAL',
            sourceConfig: {
              connectorId: 1,  // MySQL演示源库
              sql: "SELECT id, product_name, price FROM products WHERE status = 'new'",
              tableName: 'products'
            },
            targetConfig: {
              connectorId: 4,  // API演示目标接口
              apiMethod: 'POST',
              apiPath: '/api/product/create',
              batchMode: 'single',
              retryTimes: 2,
              retryInterval: 500,
              headers: [
                { enabled: true, key: 'Content-Type', value: 'application/json' }
              ]
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'productId', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'product_name', 
                targetField: 'name', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '未知商品' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'price', 
                targetField: 'price', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        },
        {
          id: 11,
          name: 'API → API 数据中转',
          description: '从源API获取数据，转换后推送到目标API',
          icon: 'el-icon-sort',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          connectorType: 'API_TO_API',
          config: {
            taskName: 'API数据中转任务',
            taskCode: 'api_to_api_relay',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 */15 * * * ?',
            sourceConfig: {
              connectorId: 3,  // API演示源接口
              apiMethod: 'GET',
              apiPath: '/users',
              dataPath: 'data.items',
              apiParams: [
                { enabled: true, key: 'category', value: 'products' }
              ],
              headers: [
                { enabled: true, key: 'Accept', value: 'application/json' }
              ],
              enablePagination: true,
              pageParam: 'page',
              pageSizeParam: 'limit',
              startPage: 1,
              pageSize: 100,
              totalPath: 'data.total'
            },
            targetConfig: {
              connectorId: 4,  // API演示目标接口
              apiMethod: 'POST',
              apiPath: '/posts',
              batchMode: 'batch',
              batchSize: 50,
              wrapperField: 'items',
              retryTimes: 3,
              retryInterval: 2000,
              headers: [
                { enabled: true, key: 'Content-Type', value: 'application/json' },
                { enabled: true, key: 'X-Source', value: 'data-sync-platform' }
              ]
            },
            mappings: [
              { 
                sourceField: 'id', 
                targetField: 'sourceId', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'name', 
                targetField: 'productName', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: 'unknown' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'price', 
                targetField: 'price', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'status', 
                targetField: 'state', 
                transformType: 'DICT',
                dictCode: 'product_status_mapping',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: '0' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DICT' } }
                ]
              }
            ]
          }
        },
        {
          id: 12,
          name: 'API → API 实时同步',
          description: '高频API到API同步，适合实时数据交换',
          icon: 'el-icon-connection',
          syncMode: 'FULL',
          scheduleType: 'CRON',
          connectorType: 'API_TO_API',
          config: {
            taskName: 'API实时数据同步',
            taskCode: 'api_to_api_realtime',
            syncMode: 'FULL',
            scheduleType: 'CRON',
            cronExpression: '0 */5 * * * ?',
            sourceConfig: {
              connectorId: 3,  // API演示源接口
              apiMethod: 'GET',
              apiPath: '/posts',
              dataPath: '',
              apiParams: [
                { enabled: true, key: 'limit', value: '50' }
              ],
              headers: [
                { enabled: true, key: 'Accept', value: 'application/json' }
              ],
              enablePagination: false
            },
            targetConfig: {
              connectorId: 4,  // API演示目标接口
              apiMethod: 'POST',
              apiPath: '/posts',
              batchMode: 'batch',
              batchSize: 50,
              wrapperField: 'data',
              retryTimes: 5,
              retryInterval: 1000,
              headers: [
                { enabled: true, key: 'Content-Type', value: 'application/json' }
              ]
            },
            mappings: [
              { 
                sourceField: 'eventId', 
                targetField: 'id', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'SKIP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'eventType', 
                targetField: 'type', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'DEFAULT', defaultValue: 'unknown' } },
                  { type: 'CLEANSE', order: 2, config: { cleanseFunctions: [{ functionCode: 'TRIM', params: {} }, { functionCode: 'UPPER', params: {} }] } },
                  { type: 'TRANSFORM', order: 3, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'timestamp', 
                targetField: 'occurTime', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              },
              { 
                sourceField: 'payload', 
                targetField: 'data', 
                transformType: 'DIRECT',
                _processors: [
                  { type: 'NULL_HANDLE', order: 1, config: { strategy: 'KEEP' } },
                  { type: 'TRANSFORM', order: 2, config: { transformType: 'DIRECT' } }
                ]
              }
            ]
          }
        }
      ]
    }
  },
  computed: {
    filteredTemplates() {
      if (!this.searchKeyword) {
        return this.templates
      }
      const keyword = this.searchKeyword.toLowerCase()
      return this.templates.filter(t => 
        t.name.toLowerCase().includes(keyword) || 
        t.description.toLowerCase().includes(keyword)
      )
    }
  },
  methods: {
    show() {
      this.visible = true
    },
    handleClose() {
      this.searchKeyword = ''
    },
    getConnectorTypeLabel(connectorType) {
      const labels = {
        'API_TO_DB': 'API→DB',
        'DB_TO_API': 'DB→API',
        'API_TO_API': 'API→API'
      }
      return labels[connectorType] || 'DB→DB'
    },
    getConnectorTypeTagType(connectorType) {
      const types = {
        'API_TO_DB': 'warning',
        'DB_TO_API': 'danger',
        'API_TO_API': 'info'
      }
      return types[connectorType] || ''
    },
    handleSelectTemplate(template) {
      this.$confirm(`确认使用模板"${template.name}"创建任务吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        // 将模板配置传递给向导页面
        this.$router.push({
          path: '/task/wizard',
          query: {
            templateId: template.id,
            template: JSON.stringify(template.config)
          }
        })
        this.visible = false
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.template-card {
  cursor: pointer;
  margin-bottom: 20px;
  text-align: center;
  transition: all 0.3s;
  min-height: 200px;
}

.template-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.template-icon {
  margin: 20px 0;
}

.template-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
}

.template-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 15px;
  min-height: 40px;
}

.template-tags {
  margin-top: 10px;
}
</style>
