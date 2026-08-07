/// <reference types="vite/client" />

/** 前端环境变量声明。新增 VITE_* 变量时在此补类型，并同步 .env.example */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
