import request from './request';

export interface DictTypeListVO {
  id: number; dictName: string; dictType: string; dataCount: number;
  status: number; remark: string; createTime: string;
  /** 后端由 dict_type 派生（`dict.type.*`），渲染走 `makeDictLabel`；派生不出时为 null */
  i18nKey?: string | null;
}
export interface DictDataVO {
  id: number; dictType: string; dictLabel: string; dictValue: string;
  cssClass: string; listClass: string; isDefault: number; sort: number;
  status: number; remark: string;
  /** 后端由 dict_type + dict_value 派生（`dict.{type}.{value}`），可为 null */
  i18nKey?: string | null;
}
interface PageResult<T> { list: T[]; total: number; pageNum: number; pageSize: number; pages: number; }

export const dictApi = {
  listTypes: (params: { pageNum: number; pageSize: number; dictName?: string; dictType?: string; status?: number }) =>
    request.get<any, { code: number; data: PageResult<DictTypeListVO> }>('/api/v1/dict/types', { params }),
  createType: (data: { dictName: string; dictType: string; status?: number; remark?: string }) =>
    request.post('/api/v1/dict/types', data),
  updateType: (id: number, data: { dictName: string; status?: number; remark?: string }) =>
    request.put(`/api/v1/dict/types/${id}`, data),
  removeType: (id: number) => request.delete(`/api/v1/dict/types/${id}`),
  listData: (dictType: string) =>
    request.get<any, { code: number; data: DictDataVO[] }>(`/api/v1/dict/types/${dictType}/data`),
  createData: (dictType: string, data: any) =>
    request.post(`/api/v1/dict/types/${dictType}/data`, data),
  updateData: (id: number, data: any) => request.put(`/api/v1/dict/data/${id}`, data),
  removeData: (id: number) => request.delete(`/api/v1/dict/data/${id}`),
  refreshCache: () => request.delete('/api/v1/dict/cache'),
};
