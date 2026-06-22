const TOKEN_KEY = 'oa_token';
const REMEMBERED_USERNAME_KEY = 'oa_remembered_username';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY) || '';
}

export function setToken(token: string, remember = false) {
  clearToken();
  const storage = remember ? localStorage : sessionStorage;
  storage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
}

export function getRememberedUsername() {
  return localStorage.getItem(REMEMBERED_USERNAME_KEY) || '';
}

export function setRememberedUsername(username: string) {
  localStorage.setItem(REMEMBERED_USERNAME_KEY, username);
}

export function clearRememberedUsername() {
  localStorage.removeItem(REMEMBERED_USERNAME_KEY);
}
