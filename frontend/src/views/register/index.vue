<template>
  <main class="auth-page register-page">
    <section class="auth-visual">
      <div class="auth-visual__overlay">
        <div class="auth-kicker">{{ t('auth.secure') }}</div>
        <h1>{{ t('auth.registerTitle') }}</h1>
        <p>{{ t('auth.secureText') }}</p>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-panel__top">
        <RouterLink class="auth-brand" to="/login">
          <span>OA</span>
          <strong>{{ t('app.name') }}</strong>
        </RouterLink>
        <LanguageSwitch :circle="false" />
      </div>

      <div class="auth-card">
        <div class="auth-card__head">
          <span>{{ t('auth.secure') }}</span>
          <h2>{{ submitted ? t('auth.pendingTitle') : t('auth.registerTitle') }}</h2>
          <p>{{ submitted ? t('auth.pendingText') : t('auth.registerSubtitle') }}</p>
        </div>

        <div v-if="submitted" class="register-success">
          <el-icon><CircleCheck /></el-icon>
          <strong>{{ t('auth.registerSuccess') }}</strong>
          <RouterLink to="/login">{{ t('auth.goLogin') }}</RouterLink>
        </div>

        <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <div class="register-grid">
            <el-form-item :label="t('auth.username')" prop="username">
              <el-input v-model.trim="form.username" :prefix-icon="User" :placeholder="t('auth.usernamePlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('auth.realName')" prop="realName">
              <el-input v-model.trim="form.realName" :prefix-icon="Postcard" :placeholder="t('auth.realNamePlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('auth.phone')" prop="phone">
              <el-input v-model.trim="form.phone" :prefix-icon="Iphone" :placeholder="t('auth.phonePlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('auth.email')" prop="email">
              <el-input v-model.trim="form.email" :prefix-icon="Message" :placeholder="t('auth.emailPlaceholder')" />
            </el-form-item>
            <el-form-item :label="t('auth.password')" prop="password">
              <el-input
                v-model="form.password"
                :prefix-icon="Lock"
                type="password"
                show-password
                :placeholder="t('auth.passwordPlaceholder')"
              />
            </el-form-item>
            <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                :prefix-icon="Lock"
                type="password"
                show-password
                :placeholder="t('auth.confirmPasswordPlaceholder')"
              />
            </el-form-item>
          </div>

          <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">
            {{ t('auth.registerButton') }}
          </el-button>
        </el-form>

        <div v-if="!submitted" class="auth-card__foot">
          <RouterLink to="/login">{{ t('auth.goLogin') }}</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RouterLink } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { CircleCheck, Iphone, Lock, Message, Postcard, User } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import LanguageSwitch from '@/components/LanguageSwitch.vue';
import { authApi } from '@/api/auth';

const { t } = useI18n();
const formRef = ref<FormInstance>();
const loading = ref(false);
const submitted = ref(false);

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: ''
});

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error(t('auth.requiredConfirmPassword')));
    return;
  }
  if (value !== form.password) {
    callback(new Error(t('auth.passwordMismatch')));
    return;
  }
  callback();
};

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('auth.requiredUsername'), trigger: 'blur' }],
  realName: [{ required: true, message: t('auth.requiredRealName'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.requiredPassword'), trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  email: [{ type: 'email', message: t('auth.emailPlaceholder'), trigger: 'blur' }]
}));

async function submit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    await authApi.register({
      username: form.username,
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      password: form.password,
      confirmPassword: form.confirmPassword
    });
    ElMessage.success(t('auth.registerSuccess'));
    submitted.value = true;
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.register-page .auth-card {
  width: min(560px, 100%);
}

.register-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.register-success {
  display: grid;
  justify-items: center;
  gap: 14px;
  padding: 28px 12px 8px;
  text-align: center;
}

.register-success .el-icon {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  background: var(--oa-success-soft);
  color: var(--oa-success);
  font-size: 30px;
}

.register-success a {
  color: var(--oa-primary);
  font-weight: 650;
}

@media (max-width: 560px) {
  .register-grid {
    grid-template-columns: 1fr;
  }
}
</style>
