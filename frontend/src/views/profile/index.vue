<template>
  <div class="page profile-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">维护头像、联系方式和个人基础资料。</p>
      </div>
    </div>

    <section class="profile-grid">
      <aside class="panel panel-body profile-card">
        <el-avatar :size="78" :src="avatarSrc">{{ (form.realName || form.username || 'U').slice(0, 1) }}</el-avatar>
        <h2>{{ form.realName || '-' }}</h2>
        <p>{{ form.departmentName || '未设置部门' }} · {{ form.position || '未设置岗位' }}</p>
        <StatusTag :status="form.status ?? 1" mode="user" />
      </aside>

      <div class="panel panel-body">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <div class="form-grid">
            <el-form-item label="头像" class="full">
              <OaUpload
                v-model="avatarFiles"
                business-type="AVATAR"
                :limit="1"
                accept="image/*"
                :show-file-list="false"
                button-text="上传头像"
                @uploaded="setAvatar"
              />
            </el-form-item>
            <el-form-item label="登录账号">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="姓名" prop="realName">
              <el-input v-model.trim="form.realName" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model.trim="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model.trim="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="岗位">
              <el-input v-model.trim="form.position" placeholder="请输入岗位" />
            </el-form-item>
          </div>
        </el-form>
        <div class="profile-actions">
          <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">保存资料</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { usersApi } from '@/api/users';
import type { FileRecord, UserInfo } from '@/api/types';
import OaUpload from '@/components/OaUpload.vue';
import StatusTag from '@/components/StatusTag.vue';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const loading = ref(false);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const avatarFiles = ref<FileRecord[]>([]);
const form = reactive<Partial<UserInfo>>({});
const avatarVersion = ref(Date.now());

const avatarSrc = computed(() => {
  if (!form.avatar) return '';
  return `${form.avatar}${form.avatar.includes('?') ? '&' : '?'}v=${avatarVersion.value}`;
});

const rules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
};

async function load() {
  loading.value = true;
  try {
    const profile = await usersApi.profile();
    Object.assign(form, profile);
    avatarFiles.value = [];
    avatarVersion.value = Date.now();
  } finally {
    loading.value = false;
  }
}

async function setAvatar(file: FileRecord) {
  const avatar = file.fileUrl || '';
  if (!avatar) {
    ElMessage.warning('上传成功，但未获取到头像地址');
    return;
  }

  form.avatar = avatar;
  avatarFiles.value = [];
  avatarVersion.value = Date.now();

  try {
    const updated = await usersApi.updateProfile(profilePayload());
    Object.assign(form, updated);
    userStore.setUserInfo(updated);
    avatarVersion.value = Date.now();
    ElMessage.success('头像已更新');
  } catch {
    ElMessage.warning('头像已上传，请点击保存资料完成更新');
  }
}

async function submit() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    const updated = await usersApi.updateProfile(profilePayload());
    Object.assign(form, updated);
    userStore.setUserInfo(updated);
    avatarVersion.value = Date.now();
    ElMessage.success('个人资料已更新');
  } finally {
    submitting.value = false;
  }
}

function profilePayload() {
  return {
    realName: form.realName,
    gender: form.gender,
    phone: form.phone,
    email: form.email,
    avatar: form.avatar,
    position: form.position
  };
}

onMounted(load);
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
}

.profile-card {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 10px;
  text-align: center;
}

.profile-card h2 {
  margin: 8px 0 0;
  font-size: 20px;
}

.profile-card p {
  margin: 0;
  color: var(--oa-muted);
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
}

@media (max-width: 760px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
