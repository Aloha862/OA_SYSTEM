<template>
  <el-select
    :model-value="modelValue"
    :placeholder="selectPlaceholder"
    :clearable="clearable"
    :multiple="multiple"
    :loading="dictStore.loadingMap[typeCode]"
    filterable
    @update:model-value="(value: unknown) => emit('update:modelValue', value)"
  >
    <el-option
      v-for="item in options"
      :key="item.id || item.dictValue"
      :label="item.dictLabel"
      :value="item.dictValue"
      :disabled="item.status === 0"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useDictStore } from '@/stores/dict';

const props = withDefaults(
  defineProps<{
    modelValue?: unknown;
    typeCode: string;
    placeholder?: string;
    clearable?: boolean;
    multiple?: boolean;
  }>(),
  {
    placeholder: '',
    clearable: true,
    multiple: false
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: unknown): void;
}>();

const dictStore = useDictStore();
const { t } = useI18n();
const options = computed(() => dictStore.getOptions(props.typeCode));
const selectPlaceholder = computed(() => props.placeholder || t('common.selectPlaceholder'));

onMounted(() => dictStore.fetchDict(props.typeCode));
watch(
  () => props.typeCode,
  (typeCode) => dictStore.fetchDict(typeCode)
);
</script>
