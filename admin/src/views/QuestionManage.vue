<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <div style="display: flex; gap: 12px; align-items: center">
        <el-select v-model="query.subject" placeholder="选择科目" clearable style="width: 150px" @change="onSubjectChange">
          <el-option v-for="s in subjects" :key="s.name" :label="s.name" :value="s.name" />
        </el-select>
        <el-select v-model="query.module" placeholder="选择模块" clearable style="width: 150px">
          <el-option v-for="m in modules" :key="m.name" :label="m.name" :value="m.name" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
      <el-button type="success" @click="triggerImport">导入Excel</el-button>
      <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display: none" @change="handleImport" />
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="subject" label="科目" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="content" label="题干" show-overflow-tooltip />
      <el-table-column prop="type" label="题型" width="80" />
      <el-table-column prop="difficulty" label="难度" width="70">
        <template #default="{ row }">
          <el-rate v-model="row.difficulty" disabled size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
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

    <el-pagination
      v-if="total > 0"
      style="margin-top: 16px; justify-content: flex-end"
      :current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="(p) => { query.page = p; loadData() }"
    />

    <el-dialog v-model="editVisible" title="编辑题目" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="科目">
          <el-input v-model="editForm.subject" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="editForm.module" />
        </el-form-item>
        <el-form-item label="题干">
          <el-input v-model="editForm.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="editForm.answer" />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="editForm.analysis" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="editForm.difficulty" />
        </el-form-item>
        <el-form-item label="考频">
          <el-select v-model="editForm.frequency">
            <el-option label="高频" value="HIGH" />
            <el-option label="中频" value="MEDIUM" />
            <el-option label="低频" value="LOW" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestions, updateQuestion, deleteQuestion, importQuestions, getSubjectTree } from '../api/admin'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const subjects = ref([])
const editVisible = ref(false)
const editForm = reactive({})
const fileInput = ref(null)

const query = reactive({ page: 1, size: 20, subject: '', module: '' })

const modules = computed(() => {
  const found = subjects.value.find((s) => s.name === query.subject)
  return found?.children || []
})

function onSubjectChange() {
  query.module = ''
}

async function loadData() {
  loading.value = true
  try {
    const res = await getQuestions(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadSubjects() {
  const res = await getSubjectTree()
  subjects.value = res.data || []
}

function openEdit(row) {
  Object.assign(editForm, row)
  editVisible.value = true
}

async function handleUpdate() {
  await updateQuestion(editForm.id, editForm)
  ElMessage.success('更新成功')
  editVisible.value = false
  loadData()
}

async function handleDelete(id) {
  await deleteQuestion(id)
  ElMessage.success('删除成功')
  loadData()
}

function triggerImport() {
  fileInput.value.click()
}

async function handleImport(e) {
  const file = e.target.files[0]
  if (!file) return
  const res = await importQuestions(file)
  ElMessage.success(`导入完成：成功${res.data.success}条，失败${res.data.fail}条`)
  fileInput.value.value = ''
  loadData()
}

onMounted(() => {
  loadSubjects()
  loadData()
})
</script>
