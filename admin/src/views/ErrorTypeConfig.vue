<template>
  <div>
    <div style="margin-bottom: 16px">
      <el-button type="primary" @click="openAdd">新增错因</el-button>
    </div>

    <el-table :data="tableData" border v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="错因名称" />
      <el-table-column prop="sortOrder" label="排序" width="100" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑错因' : '新增错因'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorTypes, addErrorType, updateErrorType, deleteErrorType } from '../api/admin'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, name: '', sortOrder: 0 })

function openAdd() {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.sortOrder = 0
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, name: row.name, sortOrder: row.sortOrder })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  if (isEdit.value) {
    await updateErrorType(form.id, { name: form.name, sortOrder: form.sortOrder })
  } else {
    await addErrorType({ name: form.name, sortOrder: form.sortOrder, enabled: true })
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleToggle(row) {
  await updateErrorType(row.id, { enabled: row.enabled })
}

async function handleDelete(id) {
  await deleteErrorType(id)
  ElMessage.success('删除成功')
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getErrorTypes()
    tableData.value = res.data || []
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
