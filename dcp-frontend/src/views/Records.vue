<template>
  <div class="records-container">
    <el-card>
      <template #header>
        <span>领用记录</span>
      </template>
      <el-table :data="records" stripe style="width: 100%" :row-class-name="tableRowClassName">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="materialId" label="耗材ID" width="100" />
        <el-table-column prop="applicant" label="申请人" width="120" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="status" label="状态" width="160">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0" type="warning">待审批</el-tag>
            <el-tag v-else-if="row.status === 1" type="success">已通过</el-tag>
            <el-tag v-else-if="row.status === 2" type="danger">已驳回</el-tag>
            <el-tag v-else-if="row.status === 3" type="danger" effect="dark">高危待审批</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip min-width="300">
          <template #default="{ row }">
            <span v-if="row.remark && row.remark.includes('[AI 风控]')">
              <span
                v-for="(part, index) in splitRemark(row.remark)"
                :key="index"
                :style="part.isAi ? 'color: #F56C6C;' : ''"
              >{{ part.text }}</span>
            </span>
            <span v-else>{{ row.remark }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column v-if="userStore.role === 'ADMIN'" label="操作" width="150">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 3"
              type="primary"
              size="small"
              @click="handleApprove(row)"
            >
              审批
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showApproveDialog" title="审批领用" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="申请人">
          <el-input v-model="currentRecord.applicant" disabled />
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model="currentRecord.quantity" disabled />
        </el-form-item>
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.status">
            <el-radio :label="1">同意</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.reply" type="textarea" :rows="3" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button type="primary" :loading="approveLoading" @click="submitApprove">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useUserStore } from '@/stores/user';
import { getRecordList, approveRecord } from '@/api';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();

const records = ref([]);
const showApproveDialog = ref(false);
const approveLoading = ref(false);
const currentRecord = ref({});

const approveForm = reactive({
  recordId: null,
  status: 1,
  reply: ''
});

const getStatusType = (status) => {
  const types = ['warning', 'success', 'danger', 'info'];
  return types[status] || 'info';
};

const getStatusText = (status) => {
  const texts = ['待审批', '已通过', '已驳回', '高危待审批'];
  return texts[status] || '未知';
};

const fetchRecords = async () => {
  try {
    const res = await getRecordList();
    records.value = res.data;
  } catch (error) {
    console.error('Failed to fetch records:', error);
  }
};

const handleApprove = (row) => {
  currentRecord.value = row;
  approveForm.recordId = row.id;
  approveForm.status = 1;
  approveForm.reply = '';
  showApproveDialog.value = true;
};

const submitApprove = async () => {
  if (!approveForm.reply) {
    ElMessage.warning('请输入审批意见');
    return;
  }
  approveLoading.value = true;
  try {
    await approveRecord(approveForm);
    ElMessage.success('审批成功');
    showApproveDialog.value = false;
    fetchRecords();
  } catch (error) {
    console.error(error);
  } finally {
    approveLoading.value = false;
  }
};

const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};

const splitRemark = (remark) => {
  if (!remark) return [];
  const parts = remark.split(/($$AI 风控$$[^\|]*)/g);
  return parts
    .filter(p => p !== '')
    .map(p => ({
      text: p,
      isAi: p.includes('[AI 风控]')
    }));
};

const tableRowClassName = ({ row }) => {
  if (row.status === 3) {
    return 'ai-high-risk-row';
  }
  return '';
};

onMounted(() => {
  fetchRecords();
});
</script>

<style scoped>
.records-container {
  padding: 0;
}

:deep(.ai-high-risk-row) {
  background-color: #fef0f0 !important;
}
:deep(.ai-high-risk-row:hover > td) {
  background-color: #fde2e2 !important;
}
</style>
