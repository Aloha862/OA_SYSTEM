<template>
  <el-container class="oa-layout">
    <Sidebar :collapsed="collapsed" />
    <el-container class="oa-layout__main">
      <Header :collapsed="collapsed" @toggle="collapsed = !collapsed" />
      <el-main class="oa-layout__content">
        <RouterView v-slot="{ Component }">
          <Transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </Transition>
        </RouterView>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import Header from './components/Header.vue';
import Sidebar from './components/Sidebar.vue';

const collapsed = ref(false);
</script>

<style scoped>
.oa-layout {
  display: flex;
  min-height: 100vh;
  background:
    radial-gradient(circle at 84% 0%, rgba(15, 159, 143, 0.08), transparent 28%),
    var(--oa-bg);
}

.oa-layout__main {
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 100vh;
  background:
    linear-gradient(180deg, #fbfdff 0, var(--oa-bg) 240px),
    var(--oa-bg);
}

.oa-layout__content {
  min-height: calc(100vh - 58px);
  padding: 22px 24px 32px;
  overflow: auto;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 900px) {
  .oa-layout__content {
    padding: 14px 10px 24px;
  }
}
</style>
