<template>
  <div class="admin-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="耗材管理" name="material">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>耗材列表</span>
              <el-button type="primary" @click="showAddMaterial = true">
                <el-icon><Plus /></el-icon>
                新增耗材
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
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="分类管理" name="category">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>分类列表</span>
              <el-button type="primary" @click="showAddCategory = true">
                <el-icon><Plus /></el-icon>
                新增分类
              </el-button>
            </div>
          </template>
          <el-table :data="categoryList" stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="分类名称" />
            <el-table-column prop="sort" label="排序" width="100" />
            <el-table-column label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <!-- ====== 审批管理（改动部分） ====== -->
      <el-tab-pane label="审批管理" name="approve">
        <el-card>
          <template #header>
            <span>待审批记录</span>
          </template>
          <el-table
            :data="pendingRecords"
            stripe
            style="width: 100%"
            :row-class-name="tableRowClassName"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="materialId" label="耗材ID" width="100" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="quantity" label="数量" width="100" />
            <el-table-column prop="status" label="状态" width="160">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning">待审批</el-tag>
                <el-tag v-else-if="row.status === 3" type="danger" effect="dark">
                  高危待审批
                </el-tag>
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
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="openApproveDialog(row)">
                  审批
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <!-- ====== 审批管理（改动结束） ====== -->
    </el-tabs>

    <el-dialog v-model="showAddMaterial" title="新增耗材" width="500px">
      <el-form :model="materialForm" label-width="100px">
        <el-form-item label="分类ID">
          <el-input-number v-model="materialForm.categoryId" :min="1" />
        </el-form-item>
        <el-form-item label="耗材名称">
          <el-input v-model="materialForm.name" placeholder="请输入耗材名称" />
        </el-form-item>
        <el-form-item label="规格型号">
          <el-input v-model="materialForm.specification" placeholder="请输入规格型号" />
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="materialForm.unit" placeholder="请输入计量单位" />
        </el-form-item>
        <el-form-item label="库存数量">
          <el-input-number v-model="materialForm.stock" :min="0" />
        </el-form-item>
        <el-form-item label="危险等级">
          <el-select v-model="materialForm.dangerLevel" placeholder="请选择">
            <el-option :label="getDangerLevelText(0)" :value="0" />
            <el-option :label="getDangerLevelText(1)" :value="1" />
            <el-option :label="getDangerLevelText(2)" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="存储条件">
          <el-input v-model="materialForm.storageCondition" placeholder="请输入存储条件" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMaterial = false">取消</el-button>
        <el-button type="primary" :loading="addMaterialLoading" @click="submitAddMaterial">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddCategory" title="新增分类" width="500px">
      <el-form :model="categoryForm" label-width="100px">
        <el-form-item label="分类名称">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddCategory = false">取消</el-button>
        <el-button type="primary" :loading="addCategoryLoading" @click="submitAddCategory">提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="showApproveDialog" title="审批领用" width="450px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.status">
            <el-radio :value="1">同意</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="approveForm.reply"
            type="textarea"
            :rows="3"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue';
import { getMaterialList, addMaterial, getCategoryList, addCategory, approveRecord, getPendingRecords } from '@/api';
import { ElMessage } from 'element-plus';

const activeTab = ref('material');
const materialList = ref([]);
const categoryList = ref([]);
const pendingRecords = ref([]);
const showAddMaterial = ref(false);
const showAddCategory = ref(false);
const addMaterialLoading = ref(false);
const addCategoryLoading = ref(false);

const materialForm = reactive({
  categoryId: 1,
  name: '',
  specification: '',
  unit: '',
  stock: 0,
  dangerLevel: 0,
  storageCondition: ''
});

const categoryForm = reactive({
  name: '',
  sort: 0
});

const getDangerLevelType = (level) => {
  const types = ['', 'info', 'danger'];
  return types[level] || 'info';
};

const getDangerLevelText = (level) => {
  const texts = ['普通', '低危', '高危'];
  return texts[level] || '普通';
};

// ====== 新增：AI 高危行标红 ======
const tableRowClassName = ({ row }) => {
  if (row.status === 3) {
    return 'ai-high-risk-row';
  }
  return '';
};

// ====== 新增：拆分备注，AI 风控部分标红 ======
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

const fetchMaterials = async () => {
  try {
    const res = await getMaterialList();
    materialList.value = res.data;
  } catch (error) {
    console.error('Failed to fetch materials:', error);
  }
};

const fetchCategories = async () => {
  try {
    const res = await getCategoryList();
    categoryList.value = res.data;
  } catch (error) {
    console.error('Failed to fetch categories:', error);
  }
};

const fetchPendingRecords = async () => {
  try {
    const res = await getPendingRecords();
    pendingRecords.value = res.data;
  } catch (error) {
    console.error('Failed to fetch pending records:', error);
  }
};

const submitAddMaterial = async () => {
  if (!materialForm.name || !materialForm.unit) {
    ElMessage.warning('请填写必填项');
    return;
  }
  addMaterialLoading.value = true;
  try {
    await addMaterial(materialForm);
    ElMessage.success('耗材入库成功');
    showAddMaterial.value = false;
    Object.assign(materialForm, { categoryId: 1, name: '', specification: '', unit: '', stock: 0, dangerLevel: 0, storageCondition: '' });
    fetchMaterials();
  } catch (error) {
    console.error(error);
  } finally {
    addMaterialLoading.value = false;
  }
};

const submitAddCategory = async () => {
  if (!categoryForm.name) {
    ElMessage.warning('请填写分类名称');
    return;
  }
  addCategoryLoading.value = true;
  try {
    await addCategory(categoryForm);
    ElMessage.success('分类添加成功');
    showAddCategory.value = false;
    Object.assign(categoryForm, { name: '', sort: 0 });
    fetchCategories();
  } catch (error) {
    console.error(error);
  } finally {
    addCategoryLoading.value = false;
  }
};

const showApproveDialog = ref(false);
const approveForm = reactive({
  recordId: null,
  status: 1,
  reply: ''
});

const openApproveDialog = (row) => {
  approveForm.recordId = row.id;
  approveForm.status = 1;
  approveForm.reply = '';
  showApproveDialog.value = true;
};

const submitApprove = async () => {
  if (!approveForm.reply.trim()) {
    ElMessage.warning('请输入审批意见');
    return;
  }
  try {
    await approveRecord({
      recordId: approveForm.recordId,
      status: approveForm.status,
      reply: approveForm.reply
    });
    ElMessage.success('审批成功');
    showApproveDialog.value = false;
    fetchPendingRecords();
    fetchMaterials();
  } catch (error) {
    console.error(error);
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

watch(activeTab, (newTab) => {
  if (newTab === 'approve') {
    fetchPendingRecords();
  }
});

onMounted(() => {
  fetchMaterials();
  fetchCategories();
});
</script>

<style scoped>
.admin-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* AI 高危行整行标红 */
:deep(.ai-high-risk-row) {
  background-color: #fef0f0 !important;
}
:deep(.ai-high-risk-row:hover > td) {
  background-color: #fde2e2 !important;
}
</style>
