<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2>AI 模型配置</h2>
      <el-button type="primary" @click="openDialog()">新增配置</el-button>
    </div>

    <el-table :data="configs" v-loading="loading" style="width: 100%">
      <el-table-column prop="name" label="配置名称" width="140" />
      <el-table-column prop="provider" label="协议" width="100">
        <template #default="{ row }">
          <el-tag :type="row.provider === 'anthropic' ? 'warning' : 'success'">{{ row.provider }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="model" label="模型" width="180" />
      <el-table-column prop="baseUrl" label="Base URL" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isActive" type="primary">当前使用</el-tag>
          <span v-else style="color: #999">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button v-if="!row.isActive" type="success" size="small" @click="setActive(row.id)">设为当前</el-button>
          <el-button type="danger" size="small" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="配置名称">
          <el-input v-model="form.name" placeholder="例如：DeepSeek 官方" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-..." />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="Model">
          <el-input v-model="form.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="form.provider" placeholder="请选择协议类型" style="width: 100%">
            <el-option label="OpenAI" value="openai" />
            <el-option label="Anthropic" value="anthropic" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为当前">
          <el-switch v-model="form.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLlmConfigs, addLlmConfig, updateLlmConfig, deleteLlmConfig, setActiveLlmConfig } from '../api/admin.js'

const configs = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  name: '',
  apiKey: '',
  baseUrl: '',
  model: '',
  provider: 'openai',
  isActive: false,
})

function resetForm() {
  form.name = ''
  form.apiKey = ''
  form.baseUrl = ''
  form.model = ''
  form.provider = 'openai'
  form.isActive = false
}

function openDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.name = row.name
    form.apiKey = row.apiKey
    form.baseUrl = row.baseUrl
    form.model = row.model
    form.provider = row.provider
    form.isActive = row.isActive
  } else {
    isEdit.value = false
    editId.value = null
    resetForm()
  }
  dialogVisible.value = true
}

async function load() {
  loading.value = true
  try {
    const res = await getLlmConfigs()
    configs.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function save() {
  const payload = {
    name: form.name,
    apiKey: form.apiKey,
    baseUrl: form.baseUrl,
    model: form.model,
    provider: form.provider,
    isActive: form.isActive,
  }
  if (isEdit.value) {
    await updateLlmConfig(editId.value, payload)
  } else {
    await addLlmConfig(payload)
  }
  if (form.isActive && isEdit.value) {
    await setActiveLlmConfig(editId.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function setActive(id) {
  await ElMessageBox.confirm('确定切换为该配置？', '提示', { type: 'warning' })
  await setActiveLlmConfig(id)
  ElMessage.success('已切换')
  await load()
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该配置？', '提示', { type: 'warning' })
  await deleteLlmConfig(id)
  ElMessage.success('删除成功')
  await load()
}

onMounted(load)
</script>
