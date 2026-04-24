<template>
  <div>
    <div style="margin-bottom: 16px">
      <el-button type="primary" @click="openAdd(1)">新增科目</el-button>
    </div>

    <el-table :data="treeData" border row-key="id" default-expand-all v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="level" label="层级" width="100">
        <template #default="{ row }">
          {{ levelText[row.level] }}
        </template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="启用" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.enabled" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button v-if="row.level < 3" type="primary" link size="small" @click="openAdd(row.level + 1, row.id)">
            新增子项
          </el-button>
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？删除后不可恢复" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
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
import { getSubjectTree, addSubject, updateSubject, deleteSubject } from '../api/admin'

const loading = ref(false)
const treeData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const levelText = { 1: '科目', 2: '模块', 3: '知识点' }

const form = reactive({ id: null, name: '', parentId: 0, level: 1, sortOrder: 0 })

const dialogTitle = computed(() => isEdit.value ? '编辑' : `新增${levelText[form.level] || ''}`)

function openAdd(level, parentId) {
  isEdit.value = false
  form.id = null
  form.name = ''
  form.parentId = parentId || 0
  form.level = level
  form.sortOrder = 0
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, { id: row.id, name: row.name, level: row.level, sortOrder: row.sortOrder, enabled: row.enabled })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('名称不能为空')
    return
  }
  if (isEdit.value) {
    await updateSubject(form.id, { name: form.name, sortOrder: form.sortOrder })
  } else {
    await addSubject({ name: form.name, parentId: form.parentId, level: form.level, sortOrder: form.sortOrder })
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

async function handleToggle(row) {
  await updateSubject(row.id, { enabled: row.enabled })
}

async function handleDelete(id) {
  await deleteSubject(id)
  ElMessage.success('删除成功')
  loadData()
}

async function loadData() {
  loading.value = true
  try {
    const res = await getSubjectTree()
    treeData.value = res.data || []
  } finally {
    loading.value = false
  }
}

import { computed } from 'vue'
onMounted(loadData)
</script>
