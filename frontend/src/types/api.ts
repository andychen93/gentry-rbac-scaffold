export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

export interface PageQuery {
  pageNum: number;
  pageSize: number;
  [key: string]: unknown;
}

/** 统一响应（拦截器已剥一层 axios response，业务层拿到此对象） */
export interface ApiResult<T> {
  code: number;
  data: T;
  message?: string;
}
