<template>
  <el-dialog 
    title="数据处理插件流程配置" 
    :visible="visible" 
    @update:visible="$emit('update:visible', $event)"
    width="1000px" 
    top="5vh"
    :close-on-click-modal="false">
    
    <div v-if="mapping" style="display: flex; height: 600px;">
      <!-- 左侧插件库 -->
      <div style="width: 220px; border-right: 1px solid #DCDFE6; padding-right: 15px; overflow-y: auto;">
        <div style="font-weight: bold; margin-bottom: 10px; color: #303133;">
          <i class="el-icon-menu"></i> 插件库
        </div>
        
        <!-- 文本处理插件 -->
        <div style="margin-bottom: 15px;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 5px;">🧹 文本处理</div>
          <el-button 
            v-for="plugin in textPlugins" 
            :key="plugin.code"
            size="mini" 
            style="width: 100%; margin-bottom: 5px; text-align: left;"
            @click="addPlugin(plugin)">
            {{ plugin.name }}
          </el-button>
        </div>
        
        <!-- 数值处理插件 -->
        <div style="margin-bottom: 15px;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 5px;">🔢 数值处理</div>
          <el-button 
            v-for="plugin in numberPlugins" 
            :key="plugin.code"
            size="mini" 
            style="width: 100%; margin-bottom: 5px; text-align: left;"
            @click="addPlugin(plugin)">
            {{ plugin.name }}
          </el-button>
        </div>
        
        <!-- 编码转换插件 -->
        <div style="margin-bottom: 15px;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 5px;">🔐 编码转换</div>
          <el-button 
            v-for="plugin in encodingPlugins" 
            :key="plugin.code"
            size="mini" 
            type="info"
            style="width: 100%; margin-bottom: 5px; text-align: left;"
            @click="addPlugin(plugin)">
            {{ plugin.name }}
          </el-button>
        </div>
        
        <!-- 日期处理插件 -->
        <div style="margin-bottom: 15px;">
          <div style="font-size: 12px; color: #909399; margin-bottom: 5px;">📅 日期处理</div>
          <el-button 
            v-for="plugin in datePlugins" 
            :key="plugin.code"
            size="mini" 
            style="width: 100%; margin-bottom: 5px; text-align: left;"
            @click="addPlugin(plugin)">
            {{ plugin.name }}
          </el-button>
        </div>
      </div>
      
      <!-- 右侧流程画布 -->
      <div style="flex: 1; padding-left: 15px; overflow-y: auto;">
        <div style="margin-bottom: 15px;">
          <div style="font-weight: bold; margin-bottom: 10px; color: #303133;">
            <i class="el-icon-position"></i> 数据处理流程
          </div>
          
          <!-- 源字段 -->
          <div style="text-align: center; padding: 10px; background: #E6F7FF; border-radius: 4px; margin-bottom: 10px;">
            <i class="el-icon-upload"></i> 
            <strong>源字段: {{ mapping.sourceField }}</strong>
          </div>
          
          <!-- 流程连接线和插件列表 -->
          <div v-if="pluginChain.length === 0" style="text-align: center; padding: 50px; color: #909399; border: 2px dashed #DCDFE6; border-radius: 4px;">
            <i class="el-icon-info" style="font-size: 48px; margin-bottom: 10px; display: block;"></i>
            <div>暂无处理插件</div>
            <div style="font-size: 12px; margin-top: 5px;">请从左侧插件库中选择插件添加</div>
          </div>
          
          <div v-else>
            <div v-for="(plugin, index) in pluginChain" :key="index" style="margin-bottom: 10px;">
              <!-- 连接箭头 -->
              <div style="text-align: center; color: #909399; margin: 5px 0;">
                <i class="el-icon-bottom" style="font-size: 20px;"></i>
              </div>
              
              <!-- 插件卡片 -->
              <div 
                class="plugin-card"
                :style="{
                  border: '2px solid ' + getPluginColor(plugin.category),
                  borderLeft: '5px solid ' + getPluginColor(plugin.category)
                }">
                <div style="display: flex; align-items: center;">
                  <el-tag 
                    :type="getPluginTagType(plugin.category)" 
                    size="mini" 
                    style="margin-right: 10px;">
                    {{ index + 1 }}
                  </el-tag>
                  <div style="flex: 1;">
                    <div style="font-weight: bold; margin-bottom: 5px;">{{ plugin.name }}</div>
                    <div v-if="plugin.params && Object.keys(plugin.params).length > 0" style="font-size: 12px; color: #606266;">
                      <i class="el-icon-setting"></i>
                      <span v-for="(val, key) in plugin.params" :key="key" style="margin-left: 5px;">
                        {{ key }}: <strong>{{ val }}</strong>
                      </span>
                    </div>
                  </div>
                  <el-button-group size="mini">
                    <el-button 
                      icon="el-icon-arrow-up" 
                      :disabled="index === 0"
                      @click="movePlugin(index, -1)">
                    </el-button>
                    <el-button 
                      icon="el-icon-arrow-down" 
                      :disabled="index === pluginChain.length - 1"
                      @click="movePlugin(index, 1)">
                    </el-button>
                    <el-button 
                      icon="el-icon-setting"
                      @click="configPlugin(plugin, index)">
                    </el-button>
                    <el-button 
                      type="danger" 
                      icon="el-icon-delete"
                      @click="removePlugin(index)">
                    </el-button>
                  </el-button-group>
                </div>
              </div>
            </div>
            
            <!-- 最后的连接箭头 -->
            <div style="text-align: center; color: #909399; margin: 5px 0;">
              <i class="el-icon-bottom" style="font-size: 20px;"></i>
            </div>
          </div>
          
          <!-- 目标字段 -->
          <div style="text-align: center; padding: 10px; background: #FFF7E6; border-radius: 4px; margin-top: 10px;">
            <strong>→ {{ mapping.targetField }}</strong>
            <i class="el-icon-download"></i> 
          </div>
        </div>
      </div>
    </div>
    
    <span slot="footer">
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="handleSave">
        <i class="el-icon-check"></i> 保存配置
      </el-button>
    </span>
    
    <!-- 插件参数配置对话框 -->
    <el-dialog 
      :title="'配置: ' + (currentPlugin ? currentPlugin.name : '')" 
      :visible.sync="configDialogVisible"
      width="500px"
      append-to-body>
      <el-form v-if="currentPlugin" label-width="100px" size="small">
        <!-- 根据插件类型显示不同的配置项 -->
        
        <!-- 字符替换参数 -->
        <template v-if="currentPlugin.code === 'REPLACE'">
          <el-form-item label="查找字符串">
            <el-input v-model="currentPlugin.params.oldStr" placeholder="要替换的字符串" />
          </el-form-item>
          <el-form-item label="替换为">
            <el-input v-model="currentPlugin.params.newStr" placeholder="新字符串" />
          </el-form-item>
        </template>
        
        <!-- 字符串拆分参数 -->
        <template v-if="currentPlugin.code === 'SPLIT'">
          <el-form-item label="分隔符">
            <el-input v-model="currentPlugin.params.delimiter" placeholder="分隔符，如: ," />
          </el-form-item>
          <el-form-item label="索引">
            <el-input-number v-model="currentPlugin.params.index" :min="0" placeholder="取第几个，介0开始" />
          </el-form-item>
        </template>
        
        <!-- 文本拼接参数 -->
        <template v-if="currentPlugin.code === 'CONCAT'">
          <el-form-item label="拼接文本">
            <el-input v-model="currentPlugin.params.text" placeholder="要拼接的文本" />
          </el-form-item>
        </template>
        
        <!-- 前缀拼接参数 -->
        <template v-if="currentPlugin.code === 'CONCAT_PREFIX'">
          <el-form-item label="前缀文本">
            <el-input v-model="currentPlugin.params.prefix" placeholder="前缀" />
          </el-form-item>
        </template>
        
        <!-- 后缀拼接参数 -->
        <template v-if="currentPlugin.code === 'CONCAT_SUFFIX'">
          <el-form-item label="后缀文本">
            <el-input v-model="currentPlugin.params.suffix" placeholder="后缀" />
          </el-form-item>
        </template>
        
        <!-- 转小数参数 -->
        <template v-if="currentPlugin.code === 'TO_DECIMAL'">
          <el-form-item label="小数位数">
            <el-input-number v-model="currentPlugin.params.scale" :min="0" :max="10" />
          </el-form-item>
        </template>
        
        <!-- 加法运算参数 -->
        <template v-if="currentPlugin.code === 'ADD'">
          <el-form-item label="加数">
            <el-input-number v-model="currentPlugin.params.number" placeholder="要加的数值" />
          </el-form-item>
        </template>
        
        <!-- 乘法运算参数 -->
        <template v-if="currentPlugin.code === 'MULTIPLY'">
          <el-form-item label="乘数">
            <el-input-number v-model="currentPlugin.params.number" :min="0" placeholder="要乘的数值" />
          </el-form-item>
        </template>
        
        <!-- 数值精度参数 -->
        <template v-if="['ROUND', 'CEIL', 'FLOOR'].includes(currentPlugin.code)">
          <el-form-item label="小数位数">
            <el-input-number v-model="currentPlugin.params.precision" :min="0" :max="10" />
          </el-form-item>
        </template>
        
        <!-- 日期格式化参数 -->
        <template v-if="currentPlugin.code === 'DATE_FORMAT'">
          <el-form-item label="源格式">
            <el-select 
              v-model="currentPlugin.params.sourceFormat" 
              multiple
              filterable 
              allow-create 
              default-first-option
              placeholder="可多选，支持多种日期格式自动匹配"
              style="width: 100%">
              <el-option label="yyyy-MM-dd" value="yyyy-MM-dd">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27</span>
              </el-option>
              <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27 16:30:00</span>
              </el-option>
              <el-option label="yyyy-MM-dd'T'HH:mm:ss.SSS" value="yyyy-MM-dd'T'HH:mm:ss.SSS">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ'T'ＨＨ:ｍｍ:ｓｓ.ＳＳＳ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27T16:30:00.123</span>
              </el-option>
              <el-option label="yyyy/MM/dd" value="yyyy/MM/dd">
                <span>ｙｙｙｙ／ＭＭ／ｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025/11/27</span>
              </el-option>
              <el-option label="yyyy/MM/dd HH:mm:ss" value="yyyy/MM/dd HH:mm:ss">
                <span>ｙｙｙｙ／ＭＭ／ｄｄ ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025/11/27 16:30:00</span>
              </el-option>
              <el-option label="yyyyMMdd" value="yyyyMMdd">
                <span>ｙｙｙｙＭＭｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 20251127</span>
              </el-option>
              <el-option label="yyyyMMddHHmmss" value="yyyyMMddHHmmss">
                <span>ｙｙｙｙＭＭｄｄＨＨｍｍｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 20251127163000</span>
              </el-option>
              <el-option label="dd/MM/yyyy" value="dd/MM/yyyy">
                <span>ｄｄ／ＭＭ／ｙｙｙｙ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 27/11/2025</span>
              </el-option>
              <el-option label="MM/dd/yyyy" value="MM/dd/yyyy">
                <span>ＭＭ／ｄｄ／ｙｙｙｙ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 11/27/2025</span>
              </el-option>
              <el-option label="yyyy年MM月dd日" value="yyyy年MM月dd日">
                <span>ｙｙｙｙ年ＭＭ月ｄｄ日</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025年11月27日</span>
              </el-option>
              <el-option label="yyyy年MM月dd日 HH:mm:ss" value="yyyy年MM月dd日 HH:mm:ss">
                <span>ｙｙｙｙ年ＭＭ月ｄｄ日 ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025年11月27日 16:30:00</span>
              </el-option>
            </el-select>
            <div style="margin-top: 4px; color: #909399; font-size: 12px">
              <i class="el-icon-info"></i> 支持多选或自定义输入，系统会按顺序依次尝试解析，直到成功
            </div>
          </el-form-item>
          <el-form-item label="目标格式">
            <el-select 
              v-model="currentPlugin.params.targetFormat" 
              filterable 
              allow-create 
              default-first-option
              placeholder="选择或输入目标日期格式"
              style="width: 100%">
              <el-option label="yyyy-MM-dd" value="yyyy-MM-dd">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27</span>
              </el-option>
              <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27 16:30:00</span>
              </el-option>
              <el-option label="yyyy-MM-dd'T'HH:mm:ss.SSS" value="yyyy-MM-dd'T'HH:mm:ss.SSS">
                <span>ｙｙｙｙ－ＭＭ－ｄｄ'T'ＨＨ:ｍｍ:ｓｓ.ＳＳＳ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025-11-27T16:30:00.123</span>
              </el-option>
              <el-option label="yyyy/MM/dd" value="yyyy/MM/dd">
                <span>ｙｙｙｙ／ＭＭ／ｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025/11/27</span>
              </el-option>
              <el-option label="yyyy/MM/dd HH:mm:ss" value="yyyy/MM/dd HH:mm:ss">
                <span>ｙｙｙｙ／ＭＭ／ｄｄ ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025/11/27 16:30:00</span>
              </el-option>
              <el-option label="yyyyMMdd" value="yyyyMMdd">
                <span>ｙｙｙｙＭＭｄｄ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 20251127</span>
              </el-option>
              <el-option label="yyyyMMddHHmmss" value="yyyyMMddHHmmss">
                <span>ｙｙｙｙＭＭｄｄＨＨｍｍｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 20251127163000</span>
              </el-option>
              <el-option label="dd/MM/yyyy" value="dd/MM/yyyy">
                <span>ｄｄ／ＭＭ／ｙｙｙｙ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 27/11/2025</span>
              </el-option>
              <el-option label="MM/dd/yyyy" value="MM/dd/yyyy">
                <span>ＭＭ／ｄｄ／ｙｙｙｙ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 11/27/2025</span>
              </el-option>
              <el-option label="yyyy年MM月dd日" value="yyyy年MM月dd日">
                <span>ｙｙｙｙ年ＭＭ月ｄｄ日</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025年11月27日</span>
              </el-option>
              <el-option label="yyyy年MM月dd日 HH:mm:ss" value="yyyy年MM月dd日 HH:mm:ss">
                <span>ｙｙｙｙ年ＭＭ月ｄｄ日 ＨＨ:ｍｍ:ｓｓ</span>
                <span style="float: right; color: #8492a6; font-size: 12px">示例: 2025年11月27日 16:30:00</span>
              </el-option>
            </el-select>
            <div style="margin-top: 4px; color: #909399; font-size: 12px">
              <i class="el-icon-info"></i> 支持自定义输入，如：ｙｙｙｙ-ＭＭ-ｄｄ ＨＨ:ｍｍ:ｓｓ.ＳＳＳ
            </div>
          </el-form-item>
        </template>
        
        <!-- 默认值参数 -->
        <template v-if="currentPlugin.code === 'DEFAULT_VALUE'">
          <el-form-item label="默认值">
            <el-input v-model="currentPlugin.params.defaultValue" placeholder="为空时使用的默认值" />
          </el-form-item>
        </template>
      </el-form>
      
      <span slot="footer">
        <el-button @click="configDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePluginConfig">确定</el-button>
      </span>
    </el-dialog>
  </el-dialog>
</template>

<script>
export default {
  name: 'TransformPluginFlow',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    mapping: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      pluginChain: [],
      configDialogVisible: false,
      currentPlugin: null,
      currentPluginIndex: -1,
      
      // 文本处理插件
      textPlugins: [
        { code: 'TRIM', name: '去除空格', category: 'TEXT', params: {} },
        { code: 'UPPER', name: '转大写', category: 'TEXT', params: {} },
        { code: 'LOWER', name: '转小写', category: 'TEXT', params: {} },
        { code: 'REPLACE', name: '字符替换', category: 'TEXT', params: { oldStr: '', newStr: '' } },
        { code: 'SPLIT', name: '字符串拆分', category: 'TEXT', params: { delimiter: ',', index: 0 } },
        { code: 'CONCAT', name: '文本拼接', category: 'TEXT', params: { text: '' } },
        { code: 'CONCAT_PREFIX', name: '前缀拼接', category: 'TEXT', params: { prefix: '' } },
        { code: 'CONCAT_SUFFIX', name: '后缀拼接', category: 'TEXT', params: { suffix: '' } },
        { code: 'LENGTH', name: '获取长度', category: 'TEXT', params: {} },
        { code: 'REVERSE', name: '字符反转', category: 'TEXT', params: {} },
        { code: 'TO_STRING', name: '转字符串', category: 'TEXT', params: {} }
      ],
      
      // 数值处理插件
      numberPlugins: [
        { code: 'TO_INT', name: '转整数', category: 'NUMBER', params: {} },
        { code: 'TO_DECIMAL', name: '转小数', category: 'NUMBER', params: { scale: 2 } },
        { code: 'ROUND', name: '四舍五入', category: 'NUMBER', params: { precision: 2 } },
        { code: 'CEIL', name: '向上取整', category: 'NUMBER', params: { precision: 0 } },
        { code: 'FLOOR', name: '向下取整', category: 'NUMBER', params: { precision: 0 } },
        { code: 'ABS', name: '绝对值', category: 'NUMBER', params: {} },
        { code: 'ADD', name: '加法运算', category: 'NUMBER', params: { number: 0 } },
        { code: 'MULTIPLY', name: '乘法运算', category: 'NUMBER', params: { number: 1 } }
      ],
      
      // 编码转换插件
      encodingPlugins: [
        { code: 'URL_ENCODE', name: 'URL编码', category: 'ENCODING', params: {} },
        { code: 'URL_DECODE', name: 'URL解码', category: 'ENCODING', params: {} },
        { code: 'BASE64_ENCODE', name: 'Base64编码', category: 'ENCODING', params: {} },
        { code: 'BASE64_DECODE', name: 'Base64解码', category: 'ENCODING', params: {} },
        { code: 'MD5', name: 'MD5哈希', category: 'ENCODING', params: {} }
      ],
      
      // 日期处理插件
      datePlugins: [
        { code: 'DATE_FORMAT', name: '日期格式化', category: 'DATE', params: { sourceFormat: ['yyyy-MM-dd'], targetFormat: 'yyyy/MM/dd' } }
      ]
    }
  },
  watch: {
    mapping: {
      immediate: true,
      handler(newVal) {
        if (newVal) {
          this.initializePluginChain()
        }
      }
    }
  },
  methods: {
    initializePluginChain() {
      this.pluginChain = []
      
      // 从mapping中恢复插件链
      if (this.mapping.cleanseFunctions) {
        try {
          const functions = JSON.parse(this.mapping.cleanseFunctions)
          this.pluginChain = functions.map(func => {
            // 查找对应的插件定义
            const allPlugins = [
              ...this.textPlugins,
              ...this.numberPlugins,
              ...this.encodingPlugins,
              ...this.datePlugins
            ]
            const pluginDef = allPlugins.find(p => p.code === func.functionCode)
            if (pluginDef) {
              // 深拷贝插件定义
              const plugin = JSON.parse(JSON.stringify(pluginDef))
              
              // 合并参数，但需要特殊处理日期格式化的sourceFormat
              if (func.params) {
                plugin.params = { ...func.params }
                
                // 兼容处理：旧数据的sourceFormat是字符串，新数据是数组
                if (func.functionCode === 'DATE_FORMAT' && plugin.params.sourceFormat) {
                  if (typeof plugin.params.sourceFormat === 'string') {
                    // 字符串转数组（按逗号或端线分割）
                    plugin.params.sourceFormat = plugin.params.sourceFormat
                      .split(/[,|\|]/)
                      .map(s => s.trim())
                      .filter(s => s)
                  }
                  // 如果已经是数组，直接使用
                }
              }
              
              return plugin
            }
            return null
          }).filter(p => p !== null)
        } catch (e) {
          console.error('解析插件链失败:', e)
        }
      }
    },
    
    addPlugin(plugin) {
      // 深拷贝插件
      const newPlugin = JSON.parse(JSON.stringify(plugin))
      this.pluginChain.push(newPlugin)
      
      // 如果有参数需要配置，立即打开配置对话框
      if (Object.keys(newPlugin.params).length > 0) {
        this.configPlugin(newPlugin, this.pluginChain.length - 1)
      }
    },
    
    movePlugin(index, direction) {
      const newIndex = index + direction
      if (newIndex < 0 || newIndex >= this.pluginChain.length) return
      
      const temp = this.pluginChain[index]
      this.$set(this.pluginChain, index, this.pluginChain[newIndex])
      this.$set(this.pluginChain, newIndex, temp)
    },
    
    removePlugin(index) {
      this.$confirm('确定要移除此插件吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.pluginChain.splice(index, 1)
        this.$message.success('已移除')
      }).catch(() => {})
    },
    
    configPlugin(plugin, index) {
      this.currentPlugin = plugin
      this.currentPluginIndex = index
      this.configDialogVisible = true
    },
    
    savePluginConfig() {
      this.configDialogVisible = false
      this.$message.success('配置已更新')
    },
    
    getPluginColor(category) {
      const colors = {
        'TEXT': '#67C23A',
        'NUMBER': '#409EFF',
        'ENCODING': '#909399',
        'DATE': '#E6A23C'
      }
      return colors[category] || '#DCDFE6'
    },
    
    getPluginTagType(category) {
      const types = {
        'TEXT': 'success',
        'NUMBER': 'primary',
        'ENCODING': 'info',
        'DATE': 'warning'
      }
      return types[category] || 'info'
    },
    
    handleSave() {
      // 保存插件链到 mapping
      const cleanseFunctions = this.pluginChain.map(plugin => ({
        functionCode: plugin.code,
        params: plugin.params
      }))
      
      this.mapping.cleanseFunctions = JSON.stringify(cleanseFunctions)
      
      this.$emit('save', this.mapping)
      this.$emit('update:visible', false)
      this.$message.success('配置已保存')
    }
  }
}
</script>

<style scoped>
.plugin-card {
  padding: 12px;
  background: white;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.plugin-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}
</style>
