<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background: #f5f5f5">
    <el-card style="width: 400px">
      <template #header>
        <div style="text-align: center; font-size: 20px; font-weight: bold">智考管理后台</div>
      </template>
      <el-form :model="form" @submit.prevent="handleSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
            {{ isRegister ? '注册' : '登录' }}
          </el-button>
        </el-form-item>
        <el-form-item style="margin-bottom: 0">
          <el-button link type="primary" style="width: 100%" @click="isRegister = !isRegister">
            {{ isRegister ? '已有账号？去登录' : '没有账号？去注册' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const loading = ref(false)
const isRegister = ref(false)
const form = reactive({ username: '', password: '' })

async function handleSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await api.post('/auth/register', {
        username: form.username,
        password: form.password,
        nickname: form.username,
      })
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
    } else {
      const res = await api.post('/auth/login', form)
      const token = res.data?.token || res.token
      if (!token) {
        ElMessage.error('登录响应中没有token')
        return
      }
      localStorage.setItem('token', token)
      ElMessage.success('登录成功')
      router.push('/')
    }
  } catch {
    ElMessage.error(isRegister.value ? '注册失败' : '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
