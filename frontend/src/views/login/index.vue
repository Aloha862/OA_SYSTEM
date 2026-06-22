<template>
  <main class="auth-page">
    <section class="auth-visual">
      <div class="auth-visual__overlay">
        <div class="auth-kicker">{{ t('auth.heroKicker') }}</div>
        <h1>{{ t('auth.heroTitle') }}</h1>
        <p>{{ t('auth.heroText') }}</p>

        <div class="auth-stats">
          <div>
            <strong>24h</strong>
            <span>{{ t('auth.statApproval') }}</span>
          </div>
          <div>
            <strong>360</strong>
            <span>{{ t('auth.statSchedule') }}</span>
          </div>
          <div>
            <strong>99%</strong>
            <span>{{ t('auth.statNotice') }}</span>
          </div>
        </div>
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
          <span>{{ t('app.workspace') }}</span>
          <h2>{{ t('auth.loginTitle') }}</h2>
          <p>{{ t('auth.loginSubtitle') }}</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item :label="t('auth.username')" prop="username">
            <el-input v-model.trim="form.username" :prefix-icon="User" :placeholder="t('auth.usernamePlaceholder')" />
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

          <div class="auth-options">
            <el-checkbox v-model="form.rememberMe">{{ t('auth.rememberMe') }}</el-checkbox>
            <span>{{ t('auth.forgotHint') }}</span>
          </div>

          <el-button class="auth-submit" type="primary" :loading="loading" @click="submit">
            {{ t('auth.loginButton') }}
          </el-button>
        </el-form>

        <div class="auth-card__foot">
          <RouterLink to="/register">{{ t('auth.goRegister') }}</RouterLink>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { Lock, User } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import LanguageSwitch from '@/components/LanguageSwitch.vue';
import { useUserStore } from '@/stores/user';
import { usePermissionStore } from '@/stores/permission';
import { useNotificationStore } from '@/stores/notification';
import { getRememberedUsername } from '@/utils/auth';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const permissionStore = usePermissionStore();
const notificationStore = useNotificationStore();
const { t } = useI18n();

const rememberedUsername = getRememberedUsername();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({
  username: rememberedUsername,
  password: '',
  rememberMe: Boolean(rememberedUsername)
});

const rules = computed<FormRules>(() => ({
  username: [{ required: true, message: t('auth.requiredUsername'), trigger: 'blur' }],
  password: [{ required: true, message: t('auth.requiredPassword'), trigger: 'blur' }]
}));

async function submit() {
  await formRef.value?.validate();
  loading.value = true;
  try {
    const result = await userStore.login({ username: form.username, password: form.password }, form.rememberMe);
    permissionStore.generateMenus(result.userInfo);
    notificationStore.fetchUnreadCount();
    ElMessage.success(t('auth.loginSuccess'));
    router.replace((route.query.redirect as string) || '/dashboard');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(420px, 0.72fr);
  min-height: 100vh;
  background: #f5f7fb;
}

.auth-visual {
  position: relative;
  display: flex;
  align-items: flex-end;
  min-height: 100vh;
  padding: 48px;
  background:
    linear-gradient(180deg, rgba(8, 20, 38, 0.12), rgba(8, 20, 38, 0.76)),
    url("/files/2026/06/seed-cover-20.jpg") center/cover;
  color: #ffffff;
}

.auth-visual__overlay {
  display: grid;
  gap: 18px;
  width: min(620px, 100%);
}

.auth-kicker {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 760;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-visual h1 {
  margin: 0;
  max-width: 760px;
  font-size: 42px;
  line-height: 1.16;
}

.auth-visual p {
  max-width: 560px;
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 16px;
  line-height: 1.8;
}

.auth-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  width: min(520px, 100%);
  margin-top: 8px;
}

.auth-stats div {
  min-height: 86px;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.auth-stats strong,
.auth-stats span {
  display: block;
}

.auth-stats strong {
  font-size: 24px;
}

.auth-stats span {
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.72);
}

.auth-panel {
  position: relative;
  display: grid;
  align-content: center;
  gap: 32px;
  min-height: 100vh;
  padding: 32px clamp(26px, 5vw, 72px);
}

.auth-panel__top {
  position: absolute;
  top: 28px;
  right: clamp(26px, 5vw, 72px);
  left: clamp(26px, 5vw, 72px);
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.auth-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: var(--oa-text);
}

.auth-brand span {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  background: var(--oa-primary);
  color: #ffffff;
  font-weight: 780;
}

.auth-brand strong {
  overflow: hidden;
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.auth-card {
  display: grid;
  gap: 24px;
  width: min(440px, 100%);
  margin: 0 auto;
  padding: 32px;
  border: 1px solid rgba(214, 222, 235, 0.96);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.1);
}

.auth-card__head span {
  color: var(--oa-accent);
  font-size: 12px;
  font-weight: 760;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-card__head h2 {
  margin: 8px 0 0;
  color: var(--oa-text);
  font-size: 28px;
  line-height: 1.2;
}

.auth-card__head p {
  margin: 9px 0 0;
  color: var(--oa-muted);
  line-height: 1.7;
}

.auth-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: -2px 0 16px;
  color: var(--oa-muted);
  font-size: 12px;
}

.auth-options span {
  text-align: right;
}

.auth-submit {
  width: 100%;
  height: 42px;
}

.auth-card__foot {
  padding-top: 4px;
  border-top: 1px solid var(--oa-border);
  text-align: center;
}

.auth-card__foot a {
  color: var(--oa-primary);
  font-weight: 650;
}

@media (max-width: 980px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-visual {
    min-height: 360px;
    padding: 90px 24px 28px;
  }

  .auth-visual h1 {
    font-size: 30px;
  }

  .auth-panel {
    position: static;
    min-height: auto;
    padding: 28px 16px 36px;
  }

  .auth-panel__top {
    top: 18px;
    right: 16px;
    left: 16px;
  }
}

@media (max-width: 560px) {
  .auth-stats,
  .auth-options {
    grid-template-columns: 1fr;
  }

  .auth-stats {
    display: none;
  }

  .auth-card {
    padding: 24px;
  }

  .auth-options {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .auth-options span {
    text-align: left;
  }
}
</style>
