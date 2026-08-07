package com.precision.rbac.dict.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.dict.dto.*;
import com.precision.rbac.dict.vo.*;

import java.util.List;

public interface DictService {
    PageResult<DictTypeListVO> listTypes(DictTypeQueryDTO query);
    DictTypeVO createType(DictTypeCreateDTO dto);
    void updateType(Long id, DictTypeUpdateDTO dto);
    void removeType(Long id);
    List<DictDataVO> listDataByType(String dictType);
    DictDataVO createData(String dictType, DictDataCreateDTO dto);
    void updateData(Long id, DictDataUpdateDTO dto);
    void removeData(Long id);
    void refreshCache();
}
