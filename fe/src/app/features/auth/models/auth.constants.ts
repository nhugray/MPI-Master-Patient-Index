export const AUTH_CONSTANTS = {
  TOKEN_KEY: 'access_token',
  REFRESH_TOKEN_KEY: 'refresh_token',
  USER_KEY: 'current_user',
  TOKEN_EXPIRY_KEY: 'token_expiry',
  REMEMBER_ME_KEY: 'remember_me_email',

  API_PREFIX: '/api/v1/auth',
  LOGIN_ENDPOINT: '/api/v1/auth/login',
  LOGOUT_ENDPOINT: '/api/v1/auth/logout',
  REFRESH_ENDPOINT: '/api/v1/auth/refresh',
  ME_ENDPOINT: '/api/v1/auth/me',

  DEFAULT_TOKEN_EXPIRY: 900000,
  REFRESH_TOKEN_EXPIRY: 604800000
};

export enum StorageType {
  LOCAL = 'localStorage',
  SESSION = 'sessionStorage'
}
