<template>
  <el-container class="layout-container">
    <el-aside width="200px">
      <div class="logo">
        <h2>耗材管控平台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/home">
          <el-icon><Box /></el-icon>
          <span>耗材列表</span>
        </el-menu-item>
        <el-menu-item index="/records">
          <el-icon><Document /></el-icon>
          <span>领用记录</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.role === 'ADMIN'" index="/admin">
          <el-icon><Setting /></el-icon>
          <span>管理中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-content">
          <span class="page-title">{{ pageTitle }}</span>
          <div class="user-info">
            <el-icon><User /></el-icon>
            <span>{{ userStore.username }}</span>
            <span class="role-tag" :type="userStore.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ userStore.role === 'ADMIN' ? '管理员' : '实验员' }}
            </span>
            <el-button type="text" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '耗材管控平台')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.layout-container {
  height: 100%;
}

.el-aside {
  background-color: #545c64;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #434a50;
}

.logo h2 {
  font-size: 16px;
  color: #fff;
  margin: 0;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-tag {
  margin-left: 8px;
}

.el-main {
  background-color: #f0f2f5;
}
</style>
