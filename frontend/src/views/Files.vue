<template>
  <div class="files-container">
    <!-- 顶部导航 -->
    <div class="header">
      <div class="header-left">
        <el-icon :size="28"><FolderOpened /></el-icon>
        <h1>文件下载服务</h1>
      </div>
      <div class="header-right">
        <span class="user-info">
          <el-icon><User /></el-icon>
          {{ userStore.username }}
        </span>
        <el-button @click="handleLogout" size="small">退出</el-button>
      </div>
    </div>
    
    <!-- 路径面包屑 -->
    <div class="path-bar">
      <el-icon><Location /></el-icon>
      <span class="path-text">{{ currentPath }}</span>
    </div>
    
    <!-- 文件列表 -->
    <el-card class="file-card">
      <el-table :data="files" style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name">
              <el-icon :size="20" :class="{ 'icon-folder': row.directory }">
                <Folder v-if="row.directory" />
                <Document v-else />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="formattedSize" label="大小" width="100" />
        
        <el-table-column label="修改时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastModified) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button 
              v-if="row.directory" 
              type="primary" 
              size="small"
              @click="enterDirectory(row.path)"
            >
              进入
            </el-button>
            <el-button 
              v-else 
              type="success" 
              size="small"
              @click="downloadFile(row.path)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 空状态 -->
      <el-empty v-if="!loading && files.length === 0" description="此目录为空" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { fileApi } from '../services/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const files = ref([])
const currentPath = ref('')
const workspaceDir = ref('')

onMounted(() => {
  loadFiles()
})

const loadFiles = async (path = '') => {
  loading.value = true
  try {
    const response = await fileApi.list(path)
    if (response.data.success) {
      files.value = response.data.files
      currentPath.value = response.data.currentPath
      workspaceDir.value = response.data.workspaceDir
    }
  } catch (error) {
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

const enterDirectory = (path) => {
  loadFiles(path)
}

const goParent = () => {
  // 获取父目录路径
  const parts = currentPath.value.split('/')
  parts.pop()
  const parentPath = parts.join('/') || workspaceDir.value
  loadFiles(parentPath)
}

const downloadFile = (filePath) => {
  const link = document.createElement('a')
  link.href = fileApi.download(filePath)
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const formatDate = (timestamp) => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.files-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.path-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
  color: #606266;
  font-family: monospace;
  font-size: 13px;
}

.file-card {
  margin: 20px 24px;
  border-radius: 8px;
}

.file-card :deep(.el-card__body) {
  padding: 0;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-folder {
  color: #f7ba2a;
}
</style>
