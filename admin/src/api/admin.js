import api from './index'

export function getQuestions(params) {
  return api.get('/admin/questions', { params })
}

export function getQuestion(id) {
  return api.get(`/admin/questions/${id}`)
}

export function updateQuestion(id, data) {
  return api.put(`/admin/questions/${id}`, data)
}

export function deleteQuestion(id) {
  return api.delete(`/admin/questions/${id}`)
}

export function importQuestions(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/admin/questions/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getSubjectTree() {
  return api.get('/admin/subjects')
}

export function addSubject(data) {
  return api.post('/admin/subjects', data)
}

export function updateSubject(id, data) {
  return api.put(`/admin/subjects/${id}`, data)
}

export function deleteSubject(id) {
  return api.delete(`/admin/subjects/${id}`)
}

export function getErrorTypes() {
  return api.get('/admin/error-types')
}

export function addErrorType(data) {
  return api.post('/admin/error-types', data)
}

export function updateErrorType(id, data) {
  return api.put(`/admin/error-types/${id}`, data)
}

export function deleteErrorType(id) {
  return api.delete(`/admin/error-types/${id}`)
}

export function getSystemConfigs() {
  return api.get('/admin/system-config')
}

export function addSystemConfig(data) {
  return api.post('/admin/system-config', data)
}

export function updateSystemConfig(id, data) {
  return api.put(`/admin/system-config/${id}`, data)
}

export function getLlmConfigs() {
  return api.get('/admin/llm-config')
}

export function addLlmConfig(data) {
  return api.post('/admin/llm-config', data)
}

export function updateLlmConfig(id, data) {
  return api.put(`/admin/llm-config/${id}`, data)
}

export function deleteLlmConfig(id) {
  return api.delete(`/admin/llm-config/${id}`)
}

export function setActiveLlmConfig(id) {
  return api.post(`/admin/llm-config/${id}/set-active`)
}
