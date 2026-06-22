import { ref } from 'vue';
import { defineStore } from 'pinia';
import { LOCALE_KEY, setI18nLocale, type SupportedLocale } from '@/i18n';

function resolveInitialLocale(): SupportedLocale {
  const stored = localStorage.getItem(LOCALE_KEY);
  if (stored === 'zh-CN' || stored === 'en-US') return stored;
  return navigator.language?.toLowerCase().startsWith('en') ? 'en-US' : 'zh-CN';
}

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<SupportedLocale>(resolveInitialLocale());

  function setLocale(value: SupportedLocale) {
    locale.value = value;
    localStorage.setItem(LOCALE_KEY, value);
    setI18nLocale(value);
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en-US' : 'zh-CN');
  }

  setI18nLocale(locale.value);

  return {
    locale,
    setLocale,
    toggleLocale
  };
});
