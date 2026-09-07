<template>
  <div class="groovy-editor-wrapper">
    <!-- 标题栏 -->
    <div class="editor-header">
      <div class="header-left">
        <i class="el-icon-edit-outline"></i>
        <span class="header-title">转换脚本</span>
        <span class="header-subtitle">Groovy Script Editor</span>
      </div>
      <div class="header-actions">
        <el-tooltip content="格式化代码 (Shift+Ctrl+F)" placement="top">
          <el-button size="mini" icon="el-icon-document-copy" @click="formatCode">格式化</el-button>
        </el-tooltip>
        <el-tooltip content="清空编辑器" placement="top">
          <el-button size="mini" icon="el-icon-delete" @click="clearCode">清空</el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 代码编辑器区域 -->
    <div class="editor-main">
      <!-- 状态指示器 -->
      <transition name="fade">
        <div class="status-indicator" v-if="errorMessage || previewResult !== null">
          <el-tag v-if="errorMessage" type="danger" size="mini" effect="plain">
            <i class="el-icon-warning-outline"></i> 执行失败
          </el-tag>
          <el-tag v-else-if="previewResult !== null" type="success" size="mini" effect="plain">
            <i class="el-icon-success"></i> 执行成功
          </el-tag>
        </div>
      </transition>

      <!-- CodeMirror 编辑器 -->
      <div class="codemirror-wrapper">
        <codemirror
          ref="cmEditor"
          v-model="code"
          :options="editorOptions"
          @ready="onEditorReady"
          @input="onCodeChange"
        />
      </div>

      <!-- 编辑器底部信息栏 -->
      <div class="editor-footer">
        <div class="footer-left">
          <span class="footer-item">
            <i class="el-icon-document"></i>
            Groovy
          </span>
          <span class="footer-item">
            <i class="el-icon-info"></i>
            使用 value 和 row 变量访问数据
          </span>
        </div>
        <div class="footer-right">
          <span class="footer-item" v-if="code">
            {{ code.split('\n').length }} 行
          </span>
        </div>
      </div>
    </div>

    <!-- 预览执行区域 -->
    <div class="preview-section">
      <div class="preview-header">
        <div class="preview-title">
          <i class="el-icon-video-play"></i>
          <span>运行预览</span>
        </div>
        <div class="preview-actions">
          <el-button 
            v-if="!showInputs"
            type="text" 
            size="small" 
            @click="showInputs = true">
            <i class="el-icon-setting"></i> 配置参数
          </el-button>
          <el-button 
            type="primary" 
            size="small" 
            @click="executePreview" 
            :loading="executing"
            plain>
            <i class="el-icon-caret-right"></i> 执行
          </el-button>
        </div>
      </div>
      
      <div class="preview-body">
        <!-- 参数配置区 -->
        <transition name="slide-down">
          <div v-if="showInputs" class="params-config">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="param-item">
                  <div class="param-label">
                    <i class="el-icon-edit"></i>
                    <span>测试值 (value)</span>
                  </div>
                  <el-input 
                    v-model="previewParams.value" 
                    placeholder="例如：张三"
                    size="small"
                    clearable
                  />
                </div>
              </el-col>
              <el-col :span="12">
                <div class="param-item">
                  <div class="param-label">
                    <i class="el-icon-s-grid"></i>
                    <span>数据行 (row JSON)</span>
                  </div>
                  <el-input 
                    v-model="previewParams.rowJson" 
                    placeholder='{"name":"张三","age":25}'
                    size="small"
                    clearable
                  />
                </div>
              </el-col>
            </el-row>
            <div class="params-footer">
              <el-button type="text" size="mini" @click="showInputs = false">
                <i class="el-icon-arrow-up"></i> 收起参数
              </el-button>
            </div>
          </div>
        </transition>
        
        <!-- 执行结果区 -->
        <transition name="slide-down">
          <div v-if="previewResult !== null || errorMessage" class="result-area">
            <!-- 失败结果 -->
            <div v-if="errorMessage" class="result-box result-error">
              <div class="result-icon">
                <i class="el-icon-circle-close"></i>
              </div>
              <div class="result-content">
                <div class="result-label">错误</div>
                <div class="result-text">{{ errorMessage }}</div>
              </div>
            </div>
            
            <!-- 成功结果 -->
            <div v-else class="result-box result-success">
              <div class="result-icon">
                <i class="el-icon-circle-check"></i>
              </div>
              <div class="result-content">
                <div class="result-label">结果</div>
                <div class="result-value">{{ previewResult }}</div>
              </div>
              <el-button 
                type="text" 
                size="small" 
                @click="copyResult"
                icon="el-icon-document-copy"
                class="copy-btn">
                复制
              </el-button>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script>
import { codemirror } from 'vue-codemirror'
import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/groovy/groovy.js'
import 'codemirror/addon/edit/closebrackets.js'
import 'codemirror/addon/edit/matchbrackets.js'
import 'codemirror/addon/selection/active-line.js'
import 'codemirror/addon/hint/show-hint.js'
import 'codemirror/addon/hint/show-hint.css'
import 'codemirror/addon/lint/lint.js'
import 'codemirror/addon/lint/lint.css'

export default {
  name: 'GroovyEditor',
  components: {
    codemirror
  },
  props: {
    value: {
      type: String,
      default: ''
    },
    height: {
      type: String,
      default: '300px'
    },
    theme: {
      type: String,
      default: 'eclipse' // monokai 或 eclipse
    },
    readonly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      code: this.value,
      showInputs: false, // 控制参数输入区显社
      previewParams: {
        value: '',
        rowJson: ''
      },
      previewResult: null,
      errorMessage: '',
      executing: false,
      editorOptions: {
        mode: 'text/x-groovy',
        theme: 'default',
        lineNumbers: true,
        line: true,
        styleActiveLine: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        indentUnit: 4,
        tabSize: 4,
        indentWithTabs: false,
        readOnly: this.readonly,
        extraKeys: {
          'Ctrl-Space': 'autocomplete',
          'Ctrl-/': 'toggleComment',
          'Shift-Ctrl-F': (cm) => {
            this.formatCode()
          }
        }
      }
    }
  },
  watch: {
    value(newVal) {
      if (newVal !== this.code) {
        this.code = newVal
      }
    }
  },
  mounted() {
    // 设置编辑器高度
    this.$nextTick(() => {
      const cm = this.$refs.cmEditor.codemirror
      cm.setSize(null, this.height)
    })
  },
  methods: {
    onEditorReady(cm) {
      // 编辑器准备完成
      cm.setSize(null, this.height)
      
      // 添加自定义语法高亮增强
      this.enhanceSyntaxHighlighting(cm)
    },
    
    // 增强语法高亮 - 识别类名
    enhanceSyntaxHighlighting(cm) {
      // 常见的Java/Groovy类名列表
      const classNames = new Set(['String', 'Integer', 'Long', 'Double', 'Float', 'Boolean', 
                         'Math', 'System', 'List', 'Map', 'Set', 'Date', 'Pattern',
                         'StringBuilder', 'StringBuffer', 'Object', 'Class', 'Thread'])
      
      // 自定义 token 标记逻辑
      const markClassNames = () => {
        const lineCount = cm.lineCount()
        for (let i = 0; i < lineCount; i++) {
          const lineTokens = cm.getLineTokens(i)
          lineTokens.forEach((token, index) => {
            if (token.type === 'variable' && classNames.has(token.string)) {
              // 找到类名，添加特殊标记
              const from = { line: i, ch: token.start }
              const to = { line: i, ch: token.end }
              cm.markText(from, to, {
                className: 'cm-classname',
                atomic: false
              })
            }
          })
        }
      }
      
      // 初始化时标记
      setTimeout(markClassNames, 100)
      
      // 内容变化时重新标记
      cm.on('change', () => {
        setTimeout(markClassNames, 100)
      })
    },
    
    onCodeChange(newCode) {
      this.code = newCode
      this.$emit('input', newCode)
      this.$emit('change', newCode)
      this.errorMessage = ''
      this.previewResult = null
    },

    // 格式化代码
    formatCode() {
      if (!this.code || !this.code.trim()) {
        this.$message.warning('代码为空，无需格式化')
        return
      }

      try {
        const cm = this.$refs.cmEditor.codemirror
        const formatted = this.formatGroovyCode(this.code)
        cm.setValue(formatted)
        this.$message.success('代码格式化成功')
      } catch (e) {
        this.$message.error('格式化失败: ' + e.message)
      }
    },

    // 简单的 Groovy 代码格式化
    formatGroovyCode(code) {
      let lines = code.split('\n')
      let indentLevel = 0
      let formatted = []

      lines.forEach(line => {
        let trimmed = line.trim()
        if (!trimmed) {
          formatted.push('')
          return
        }

        // 减少缩进：} 或 ]
        if (trimmed.startsWith('}') || trimmed.startsWith(']')) {
          indentLevel = Math.max(0, indentLevel - 1)
        }

        // 添加缩进
        formatted.push('    '.repeat(indentLevel) + trimmed)

        // 增加缩进：{ 或 [
        if (trimmed.endsWith('{') || trimmed.endsWith('[')) {
          indentLevel++
        } else if (trimmed.endsWith('}') || trimmed.endsWith(']')) {
          // } 或 ] 已经在开头处理过了
        }
      })

      return formatted.join('\n')
    },

    // 执行预览
    async executePreview() {
      if (!this.code || !this.code.trim()) {
        this.$message.warning('脚本代码为空')
        return
      }

      this.executing = true
      this.errorMessage = ''
      this.previewResult = null

      try {
        // 解析 row JSON
        let row = {}
        if (this.previewParams.rowJson && this.previewParams.rowJson.trim()) {
          try {
            row = JSON.parse(this.previewParams.rowJson)
          } catch (e) {
            this.errorMessage = 'row JSON 格式错误: ' + e.message
            this.executing = false
            return
          }
        }

        // 调用后端预览接口
        const response = await this.$axios.post('/v1/transform/script/preview', {
          script: this.code,
          value: this.previewParams.value || '',
          row: row
        })

        // axios 响应拦截器已经处理了 response.data
        if (response && response.data) {
          const result = response.data
          if (result.success) {
            this.previewResult = result.result
            this.$message.success('执行成功')
          } else {
            this.errorMessage = result.error || '执行失败'
          }
        } else {
          this.errorMessage = '执行失败：响应数据格式错误'
        }
      } catch (error) {
        console.error('脚本执行错误:', error)
        const errorMsg = (error.response && error.response.data && error.response.data.message) 
                      || error.message 
                      || '执行失败'
        this.errorMessage = errorMsg
      } finally {
        this.executing = false
      }
    },

    // 清空代码
    clearCode() {
      this.$confirm('确定要清空代码吗？', '提示', {
        type: 'warning'
      }).then(() => {
        const cm = this.$refs.cmEditor.codemirror
        cm.setValue('')
        this.$message.success('已清空')
      }).catch(() => {})
    },

    // 复制结果
    copyResult() {
      const text = String(this.previewResult)
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$message.success('已复制到剪贴板')
        }).catch(() => {
          this.fallbackCopy(text)
        })
      } else {
        this.fallbackCopy(text)
      }
    },

    // 降级复制方法
    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message.success('已复制到剪贴板')
      } catch (err) {
        this.$message.error('复制失败，请手动复制')
      }
      document.body.removeChild(textarea)
    },

    // 获取结果类型
    getResultType(value) {
      if (value === null) return 'null'
      if (value === undefined) return 'undefined'
      const type = typeof value
      if (type === 'object') {
        if (Array.isArray(value)) return 'Array'
        return 'Object'
      }
      return type.charAt(0).toUpperCase() + type.slice(1)
    },

    // 获取结果长度
    getResultLength(value) {
      if (value === null || value === undefined) return 0
      if (typeof value === 'string') return value.length
      if (Array.isArray(value)) return value.length
      if (typeof value === 'object') return Object.keys(value).length
      return String(value).length
    },

    // 获取代码
    getCode() {
      return this.code
    },

    // 设置代码
    setCode(code) {
      const cm = this.$refs.cmEditor.codemirror
      cm.setValue(code || '')
    }
  }
}
</script>

<style scoped>
/* ========== 全局容器 ========== */
.groovy-editor-wrapper {
  background: #ffffff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

/* ========== 标题栏区域 ========== */
.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fafbfc;
  border-bottom: 2px solid #e8eaed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left i {
  font-size: 20px;
  color: #5e6c84;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #172b4d;
  letter-spacing: -0.2px;
}

.header-subtitle {
  font-size: 12px;
  color: #8993a4;
  font-weight: 400;
  margin-left: 8px;
  padding-left: 8px;
  border-left: 1px solid #dfe1e6;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-actions .el-button {
  border: 1px solid #dfe1e6;
  background: #ffffff;
  color: #5e6c84;
  font-weight: 500;
  transition: all 0.2s ease;
}

.header-actions .el-button:hover {
  background: #f4f5f7;
  border-color: #c1c7d0;
  color: #172b4d;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

/* ========== 主编辑器区域 ========== */
.editor-main {
  position: relative;
  background: #ffffff;
}

.status-indicator {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
}

.codemirror-wrapper {
  background: #ffffff;
  padding: 0;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e8eaed;
}

/* ========== CodeMirror 样式优化 ========== */
::v-deep .CodeMirror {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
  height: auto;
  background: #ffffff;
  color: #172b4d;
}

::v-deep .CodeMirror-scroll {
  min-height: 280px;
  max-height: 450px;
  padding: 16px 0;
}

::v-deep .CodeMirror-gutters {
  background: #f7f8fa;
  border-right: 2px solid #e8eaed;
  padding: 0 4px;
}

::v-deep .CodeMirror-linenumber {
  color: #8993a4;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 500;
}

::v-deep .CodeMirror-activeline-background {
  background: rgba(94, 108, 132, 0.05);
}

::v-deep .CodeMirror-cursor {
  border-left: 2px solid #0052cc;
}

::v-deep .CodeMirror-selected {
  background: rgba(0, 82, 204, 0.1);
}

/* 语法高亮优化 - IntelliJ IDEA Light 主题 */
::v-deep .cm-keyword {
  color: #0033b3;  /* 深蓝色 - 关键字 */
  font-weight: 600;
}

::v-deep .cm-def {
  color: #00627a;  /* 蓝绿色 - 函数定义名 */
  font-weight: 600;
}

::v-deep .cm-variable {
  color: #000000;  /* 黑色 - 普通变量 */
  font-weight: 400;
}

::v-deep .cm-variable-2 {
  color: #660e7a;  /* 紫色 - 局部变量/参数 */
  font-weight: 400;
}

::v-deep .cm-property {
  color: #871094;  /* 紫红色 - 方法/属性调用 */
  font-weight: 600;
}

::v-deep .cm-operator {
  color: #000000;  /* 黑色 - 运算符 */
  font-weight: 400;
}

::v-deep .cm-string {
  color: #067d17;  /* 绿色 - 字符串 */
}

::v-deep .cm-string-2 {
  color: #067d17;  /* 绿色 - 字符串 */
}

::v-deep .cm-number {
  color: #1750eb;  /* 蓝色 - 数字 */
  font-weight: 400;
}

::v-deep .cm-atom {
  color: #0033b3;  /* 深蓝色 - 常量 (true, false, null) */
  font-weight: 600;
}

::v-deep .cm-comment {
  color: #629755;  /* 绿色 - 注释 (IDEA 风格) */
  font-style: italic;
}

::v-deep .cm-meta {
  color: #bbb529;  /* 黄色 - 元数据/注解 */
}

::v-deep .cm-builtin {
  color: #20999d;  /* 青绿色 - 内置类型 (String, Integer, Math) */
  font-weight: 600;
}

::v-deep .cm-bracket {
  color: #000000;  /* 黑色 - 括号 */
  font-weight: 400;
}

::v-deep .cm-tag {
  color: #0033b3;  /* 深蓝色 - 标签 */
}

::v-deep .cm-attribute {
  color: #174ad4;  /* 蓝色 - 属性 */
}

::v-deep .cm-qualifier {
  color: #20999d;  /* 青绿色 - 限定符/类名 */
  font-weight: 600;
}

::v-deep .cm-type {
  color: #20999d;  /* 青绿色 - 类名 (Math, String 等) */
  font-weight: 600;
}

::v-deep .cm-error {
  color: #ff0000;
  background: #ffcccc;
  text-decoration: underline wavy;
}

/* 特殊处理：强制标记常见类名 */
::v-deep .cm-classname {
  color: #20999d !important;  /* 青绿色 - 类名 */
  font-weight: 600 !important;
}

/* 为 String, Math 等关键字添加额外样式 */
::v-deep .cm-variable:first-child {
  /* 如果变量是首字母大写，很可能是类名 */
}

::v-deep .CodeMirror-matchingbracket {
  background: #c8e1ff;
  border: 1px solid #005cc5;
  font-weight: bold;
}

/* ========== 编辑器底部信息栏 ========== */
.editor-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 20px;
  background: #f7f8fa;
  border-top: 1px solid #e8eaed;
  font-size: 12px;
  color: #5e6c84;
}

.footer-left,
.footer-right {
  display: flex;
  gap: 16px;
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.footer-item i {
  font-size: 14px;
  color: #8993a4;
}

/* ========== 预览执行区域 ========== */
.preview-section {
  background: #fafbfc;
  border-top: 2px solid #e8eaed;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #172b4d;
}

.preview-title i {
  font-size: 16px;
  color: #0052cc;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-body {
  padding: 16px 20px;
}

/* ========== 参数配置区 ========== */
.params-config {
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 6px;
  padding: 16px;
  margin-bottom: 16px;
}

.param-item {
  margin-bottom: 0;
}

.param-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #5e6c84;
}

.param-label i {
  font-size: 14px;
  color: #8993a4;
}

.params-footer {
  text-align: right;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8eaed;
}

/* ========== 结果显示区 ========== */
.result-area {
  margin-top: 0;
}

.result-box {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.result-error {
  background: #fef0f0;
  box-shadow: 0 2px 12px rgba(245, 108, 108, 0.15);
}

.result-success {
  background: #f0f9ff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.15);
}

/* 图标 */
.result-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 22px;
}

.result-error .result-icon {
  background: #f56c6c;
  color: #ffffff;
}

.result-success .result-icon {
  background: #409eff;
  color: #ffffff;
}

/* 内容 */
.result-content {
  flex: 1;
  min-width: 0;
}

.result-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  font-weight: 500;
}

.result-text {
  font-size: 14px;
  color: #f56c6c;
  line-height: 1.6;
  word-break: break-all;
}

.result-value {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  line-height: 1.6;
  word-break: break-all;
}

/* 复制按钮 */
.copy-btn {
  flex-shrink: 0;
  color: #606266;
}

.copy-btn:hover {
  color: #409eff;
}

/* ========== 动画效果 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.slide-down-enter-active {
  animation: slideDown 0.3s ease;
}

.slide-down-leave-active {
  animation: slideUp 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-10px);
  }
}
</style>
