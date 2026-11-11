<script setup>
import { onMounted, ref } from 'vue'

const notes = ref([])
const content = ref('')
const health = ref(null)
const error = ref('')
const loading = ref(false)

async function request(url, options) {
  const res = await fetch(url, options)
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`${res.status} ${text}`)
  }
  return res.status === 204 ? null : res.json()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    health.value = await request('/api/health')
    notes.value = await request('/api/notes')
  } catch (e) {
    error.value = String(e.message || e)
  } finally {
    loading.value = false
  }
}

async function addNote() {
  const text = content.value.trim()
  if (!text) return
  try {
    await request('/api/notes', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: text })
    })
    content.value = ''
    await load()
  } catch (e) {
    error.value = String(e.message || e)
  }
}

async function removeNote(id) {
  try {
    await request(`/api/notes/${id}`, { method: 'DELETE' })
    await load()
  } catch (e) {
    error.value = String(e.message || e)
  }
}

onMounted(load)
</script>

<template>
  <div class="shell">
    <div class="card">
      <h1>Demo Deploy</h1>
      <p class="sub">Spring Boot + Vue + MySQL 部署练习</p>

      <div class="row">
        <input
          v-model="content"
          maxlength="500"
          placeholder="写点什么，回车或点添加"
          @keyup.enter="addNote"
        />
        <button :disabled="!content.trim()" @click="addNote">添加</button>
      </div>

      <div v-if="error" class="status bad">
        <span>接口异常：{{ error }}</span>
      </div>
      <div v-else-if="health" class="status">
        <span>后端：{{ health.status }}</span>
        <span>记录数：{{ health.notes }}</span>
        <span>服务器时间：{{ health.time }}</span>
      </div>
      <div v-else class="status">
        <span>{{ loading ? '加载中…' : '等待后端响应' }}</span>
      </div>

      <ul v-if="notes.length" class="notes">
        <li v-for="note in notes" :key="note.id">
          <div>
            {{ note.content }}
            <small>#{{ note.id }} · {{ note.createdAt }}</small>
          </div>
          <button class="del" @click="removeNote(note.id)">删除</button>
        </li>
      </ul>
      <p v-else class="empty">还没有记录，先添加一条。</p>
    </div>
  </div>
</template>
