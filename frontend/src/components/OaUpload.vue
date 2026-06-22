<template>
  <el-upload
    ref="uploadRef"
    :action="action"
    :headers="headers"
    :data="extraData"
    :file-list="fileList"
    :limit="limit"
    :accept="accept"
    :multiple="multiple"
    :on-success="handleSuccess"
    :on-remove="handleRemove"
    :before-upload="beforeUpload"
    :on-error="handleError"
    :on-exceed="handleExceed"
    :show-file-list="showFileList"
  >
    <el-button :icon="Upload" type="primary">{{ buttonText }}</el-button>
    <template #tip>
      <div v-if="tip" class="el-upload__tip">{{ tip }}</div>
    </template>
  </el-upload>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage, genFileId, type UploadFile, type UploadInstance, type UploadProps, type UploadRawFile, type UploadUserFile } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import { getToken } from '@/utils/auth';
import type { FileRecord, Result } from '@/api/types';

const props = withDefaults(
  defineProps<{
    modelValue?: FileRecord[];
    businessType?: string;
    businessId?: number;
    limit?: number;
    accept?: string;
    multiple?: boolean;
    maxSizeMb?: number;
    tip?: string;
    showFileList?: boolean;
    buttonText?: string;
  }>(),
  {
    modelValue: () => [],
    businessType: '',
    businessId: undefined,
    limit: 5,
    accept: '',
    multiple: false,
    maxSizeMb: 20,
    tip: '',
    showFileList: true,
    buttonText: '上传文件'
  }
);

const emit = defineEmits<{
  (event: 'update:modelValue', value: FileRecord[]): void;
  (event: 'uploaded', value: FileRecord): void;
  (event: 'removed', value: UploadFile): void;
}>();

const action = '/api/files/upload';
const uploadRef = ref<UploadInstance>();
const fileList = ref<UploadUserFile[]>([]);

const headers = computed(() => {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
});

const extraData = computed(() => {
  const data: Record<string, string | number> = {};
  if (props.businessType) {
    data.businessType = props.businessType;
  }
  if (props.businessId !== undefined && props.businessId !== null) {
    data.businessId = props.businessId;
  }
  return data;
});

watch(
  () => props.modelValue,
  (value) => {
    fileList.value = value.map((file) => ({
      name: file.originalName,
      url: file.fileUrl,
      uid: file.id
    }));
  },
  { immediate: true }
);

const beforeUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const validSize = rawFile.size / 1024 / 1024 <= props.maxSizeMb;
  if (!validSize) {
    ElMessage.warning(`文件大小不能超过 ${props.maxSizeMb}MB`);
  }
  return validSize;
};

function normalizeResponse(response: Result<FileRecord> | FileRecord | unknown) {
  if (!response || typeof response !== 'object') {
    return null;
  }

  if ('code' in response) {
    const result = response as Result<FileRecord>;
    if (result.code !== 200 || !result.data) {
      ElMessage.error(result.message || '文件上传失败');
      return null;
    }
    return result.data;
  }
  return response as FileRecord;
}

function handleSuccess(response: Result<FileRecord> | FileRecord) {
  const file = normalizeResponse(response);
  if (!file) return;
  const next = props.limit === 1 ? [file] : [...props.modelValue, file];
  emit('update:modelValue', next);
  emit('uploaded', file);
  ElMessage.success(`${props.buttonText.replace(/^上传/, '') || '文件'}上传成功`);
}

function handleError() {
  ElMessage.error('文件上传失败，请稍后重试');
}

const handleExceed: UploadProps['onExceed'] = (files) => {
  if (props.limit === 1 && files.length > 0) {
    uploadRef.value?.clearFiles();
    const file = files[0] as UploadRawFile;
    file.uid = genFileId();
    uploadRef.value?.handleStart(file);
    uploadRef.value?.submit();
    return;
  }
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`);
};

function handleRemove(uploadFile: UploadFile) {
  const next = props.modelValue.filter((file) => file.id !== Number(uploadFile.uid));
  emit('update:modelValue', next);
  emit('removed', uploadFile);
}
</script>
