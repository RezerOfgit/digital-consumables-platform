import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useUserStore } from '@/stores/user';
import router from '@/router';

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
});

request.interceptors.request.use(
  config => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

request.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败');
      if (res.code === 401) {
        const userStore = useUserStore();
        userStore.logout();
        router.push('/login');
      }
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  error => {
    let errorMsg = error.message || '网络错误';
    
    if (error.response) {
      errorMsg = `请求失败 (${error.response.status}): ${error.response.statusText}`;
      if (error.response.data && error.response.data.message) {
        errorMsg = error.response.data.message;
      }
    } else if (error.request) {
      errorMsg = '无法连接到服务器，请确保后端服务已启动';
    }
    
    ElMessage.error(errorMsg);
    return Promise.reject(error);
  }
);

export default request;
