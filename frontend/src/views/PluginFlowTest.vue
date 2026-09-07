<template>
  <div style="padding: 20px;">
    <el-page-header @back="$router.go(-1)" content="插件流程配置测试">
    </el-page-header>
    
    <el-card style="margin-top: 20px;">
      <div slot="header">
        <span>测试字段映射配置</span>
      </div>
      
      <el-form label-width="120px">
        <el-form-item label="源字段">
          <el-input v-model="testMapping.sourceField" />
        </el-form-item>
        <el-form-item label="目标字段">
          <el-input v-model="testMapping.targetField" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="openPluginFlow">
            <i class="el-icon-s-operation"></i> 打开插件流程配置
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 显示当前配置 -->
      <el-divider>当前配置</el-divider>
      <div v-if="testMapping.cleanseFunctions" style="padding: 15px; background: #F5F7FA; border-radius: 4px;">
        <pre style="margin: 0; font-size: 12px;">{{ formatJson(testMapping.cleanseFunctions) }}</pre>
      </div>
      <div v-else style="padding: 15px; text-align: center; color: #909399; background: #F5F7FA; border-radius: 4px;">
        暂无配置
      </div>
    </el-card>
    
    <!-- 插件流程配置对话框 -->
    <TransformPluginFlow
      :visible.sync="dialogVisible"
      :mapping="testMapping"
      @save="handleSave"
    />
  </div>
</template>

<script>
import TransformPluginFlow from '@/components/TransformPluginFlow.vue'

export default {
  name: 'PluginFlowTest',
  components: {
    TransformPluginFlow
  },
  data() {
    return {
      dialogVisible: false,
      testMapping: {
        sourceField: 'user_name',
        targetField: 'userName',
        transformType: 'PLUGIN_CHAIN',
        cleanseFunctions: ''
      }
    }
  },
  methods: {
    openPluginFlow() {
      this.dialogVisible = true
    },
    
    handleSave(mapping) {
      this.testMapping = Object.assign({}, mapping)
      this.$message.success('配置已保存！')
      console.log('保存的配置:', mapping)
    },
    
    formatJson(jsonStr) {
      try {
        return JSON.stringify(JSON.parse(jsonStr), null, 2)
      } catch (e) {
        return jsonStr
      }
    }
  }
}
</script>
