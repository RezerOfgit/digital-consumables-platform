<template>
  <div class="home-container">
    <el-card>
      <template #header>
        <div class="card-header">
        <span>耗材列表</span>
        <el-button type="primary" @click="showBatchApply = true">
          <el-icon><ShoppingCart /></el-icon>
          批量领用
        </el-button>
        </div>
      </template>
      <el-table :data="materialList" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="specification" label="规格" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="stock" label="库存" width="100">
          <template #default="{ row }">
            <el-tag :type="row.stock === 0 ? 'danger' : row.stock < 10 ? 'warning' : 'success'">
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dangerLevel" label="危险等级" width="120">
          <template #default="{ row }">
            <el-tag :type="getDangerLevelType(row.dangerLevel)">
              {{ getDangerLevelText(row.dangerLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storageCondition" label="存储条件" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleApply(row)" :disabled="row.stock === 0">
              领用
            </el-button>
            <el-button type="success" size="small" @click="addToBatch(row)" :disabled="row.stock === 0">
              加入批量
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showApplyDialog" title="领用耗材" width="500px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="耗材名称">
          <el-input v-model="currentMaterial.name" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-tag :type="currentMaterial.stock === 0 ? 'danger' : 'success'">
            {{ currentMaterial.stock }} {{ currentMaterial.unit }}
          </el-tag>
        </el-form-item>
        <el-form-item label="申请人">
          <el-input v-model="applyForm.applicant" placeholder="请输入申请人姓名" />
        </el-form-item>
        <el-form-item label="领用数量">
          <el-input-number v-model="applyForm.quantity" :min="1" :max="currentMaterial.stock" />
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="applyForm.remark" type="textarea" :rows="3" placeholder="请输入用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="submitApply">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showBatchApply" title="批量领用" width="600px">
      <el-form :model="batchApplyForm" label-width="100px">
        <el-form-item label="申请人">
          <el-input v-model="batchApplyForm.applicant" placeholder="请输入申请人姓名" />
        </el-form-item>
        <el-form-item label="用途说明">
          <el-input v-model="batchApplyForm.remark" type="textarea" :rows="2" placeholder="请输入用途说明" />
        </el-form-item>
        <el-form-item label="耗材明细">
          <el-table :data="batchItems" style="width: 100%">
            <el-table-column prop="name" label="耗材名称" />
            <el-table-column prop="quantity" label="数量" width="180">
              <template #default="{ row, $index }">
                <el-input-number v-model="row.quantity" :min="1" :max="row.maxStock" />
                <el-button type="danger" size="small" @click="removeBatchItem($index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchApply = false">取消</el-button>
        <el-button type="primary" :loading="batchApplyLoading" @click="submitBatchApply" :disabled="batchItems.length === 0">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { getMaterialList, applyMaterial, applyBatchMaterial } from '@/api';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';

const userStore = useUserStore();

const materialList = ref([]);
const showApplyDialog = ref(false);
const showBatchApply = ref(false);
const applyLoading = ref(false);
const batchApplyLoading = ref(false);
const currentMaterial = ref({});
const batchItems = ref([]);

const applyForm = reactive({
  materialId: null,
  applicant: '',
  quantity: 1,
  remark: ''
});

const batchApplyForm = reactive({
  applicant: '',
  remark: '',
  items: []
});

const getDangerLevelType = (level) => {
  const types = ['', 'info', 'danger'];
  return types[level] || 'info';
};

const getDangerLevelText = (level) => {
  const texts = ['普通', '低危', '高危'];
  return texts[level] || '普通';
};

const fetchMaterials = async () => {
  try {
    const res = await getMaterialList();
    materialList.value = res.data;
  } catch (error) {
    console.error('Failed to fetch materials:', error);
  }
};

const handleApply = (row) => {
  currentMaterial.value = row;
  applyForm.materialId = row.id;
  applyForm.applicant = userStore.username;
  applyForm.quantity = 1;
  applyForm.remark = '';
  showApplyDialog.value = true;
};

const submitApply = async () => {
  if (!applyForm.applicant) {
    ElMessage.warning('请输入申请人姓名');
    return;
  }
  applyLoading.value = true;
  try {
    await applyMaterial(applyForm);
    ElMessage.success('申请已提交，已进入风控与审批流程');
    showApplyDialog.value = false;
    fetchMaterials();
  } catch (error) {
    console.error(error);
  } finally {
    applyLoading.value = false;
  }
};

const addToBatch = (row) => {
  const existing = batchItems.value.find(item => item.materialId === row.id);
  if (existing) {
    ElMessage.warning('该耗材已在批量列表中');
    return;
  }
  batchItems.value.push({
    materialId: row.id,
    name: row.name,
    quantity: 1,
    maxStock: row.stock
  });
  ElMessage.success('已加入批量列表');
};

const removeBatchItem = (index) => {
  batchItems.value.splice(index, 1);
};

const submitBatchApply = async () => {
  if (!batchApplyForm.applicant) {
    ElMessage.warning('请输入申请人姓名');
    return;
  }
  if (batchItems.value.length === 0) {
    ElMessage.warning('请至少选择一种耗材');
    return;
  }
  batchApplyLoading.value = true;
  try {
    const data = {
      applicant: batchApplyForm.applicant,
      remark: batchApplyForm.remark,
      items: batchItems.value.map(item => ({
        materialId: item.materialId,
        quantity: item.quantity
      }))
    };
    await applyBatchMaterial(data);
    ElMessage.success('批量申请已提交');
    showBatchApply.value = false;
    batchItems.value = [];
    fetchMaterials();
  } catch (error) {
    console.error(error);
  } finally {
    batchApplyLoading.value = false;
  }
};

onMounted(() => {
  fetchMaterials();
});
</script>

<style scoped>
.home-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
