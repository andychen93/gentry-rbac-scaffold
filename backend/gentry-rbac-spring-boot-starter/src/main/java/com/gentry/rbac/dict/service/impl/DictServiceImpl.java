package com.gentry.rbac.dict.service.impl;

import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.core.i18n.DictI18nKeyResolver;
import com.gentry.core.security.UserContext;
import com.gentry.rbac.dict.dto.*;
import com.gentry.rbac.dict.cache.DictCacheManager;
import com.gentry.rbac.dict.entity.DictData;
import com.gentry.rbac.dict.entity.DictType;
import com.gentry.rbac.dict.mapper.DictDataMapper;
import com.gentry.rbac.dict.mapper.DictTypeMapper;
import com.gentry.rbac.dict.service.DictService;
import com.gentry.rbac.dict.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictServiceImpl implements DictService {

    private static final Logger log = LoggerFactory.getLogger(DictServiceImpl.class);
    private final DictTypeMapper dictTypeMapper;
    private final DictDataMapper dictDataMapper;
    /** L1 Caffeine + L2 Redis + 跨节点失效广播，见 DictCacheManager */
    private final DictCacheManager cache;

    public DictServiceImpl(DictTypeMapper dictTypeMapper,
                           DictDataMapper dictDataMapper,
                           DictCacheManager cache) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
        this.cache = cache;
    }

    @Override
    public PageResult<DictTypeListVO> listTypes(DictTypeQueryDTO query) {
        Long tenantId = UserContext.getTenantId();
        long total = dictTypeMapper.selectCount(query, tenantId);
        List<DictTypeListVO> list = total > 0 ? dictTypeMapper.selectList(query, tenantId) : List.of();
        // DictTypeListVO 由 XML 的 resultType 直接映射，不经组装方法，i18nKey 只能在这里补
        list.forEach(vo -> vo.setI18nKey(DictI18nKeyResolver.resolveType(vo.getDictType())));
        return new PageResult<>(list, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional
    public DictTypeVO createType(DictTypeCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        if (dictTypeMapper.countByDictType(tenantId, dto.getDictType()) > 0) {
            throw new BizException(ErrorCode.DICT_TYPE_EXISTS);
        }
        DictType entity = new DictType();
        entity.setDictName(dto.getDictName());
        entity.setDictType(dto.getDictType());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());
        dictTypeMapper.insert(entity);
        return toTypeVO(entity);
    }

    @Override
    @Transactional
    public void updateType(Long id, DictTypeUpdateDTO dto) {
        DictType existing = dictTypeMapper.selectOneById(id);
        if (existing == null) throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.type.not.found");
        DictType entity = new DictType();
        entity.setId(id);
        entity.setDictName(dto.getDictName());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());
        dictTypeMapper.update(entity);
    }

    @Override
    @Transactional
    public void removeType(Long id) {
        DictType existing = dictTypeMapper.selectOneById(id);
        if (existing == null) throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.type.not.found");
        Long tenantId = UserContext.getTenantId();
        dictTypeMapper.logicDeleteById(id);
        dictDataMapper.logicDeleteByDictType(tenantId, existing.getDictType());
        invalidateCache(tenantId, existing.getDictType());
    }

    @Override
    public List<DictDataVO> listDataByType(String dictType) {
        Long tenantId = UserContext.getTenantId();
        List<DictDataVO> cached = cache.get(tenantId, dictType);
        if (cached != null) return cached;

        List<DictData> dataList = dictDataMapper.selectByDictType(tenantId, dictType);
        List<DictDataVO> voList = dataList.stream().map(this::toDataVO).collect(Collectors.toList());
        cache.put(tenantId, dictType, voList);
        return voList;
    }

    @Override
    @Transactional
    public DictDataVO createData(String dictType, DictDataCreateDTO dto) {
        Long tenantId = UserContext.getTenantId();
        // 校验字典类型存在且启用
        DictType typeEntity = dictTypeMapper.selectByDictType(tenantId, dictType);
        if (typeEntity == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.type.not.found");
        }
        if (typeEntity.getStatus() != null && typeEntity.getStatus() != 1) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.type.disabled");
        }
        if (dictDataMapper.countByDictValue(tenantId, dictType, dto.getDictValue()) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.value.exists");
        }
        DictData entity = new DictData();
        entity.setDictType(dictType);
        entity.setDictLabel(dto.getDictLabel());
        entity.setDictValue(dto.getDictValue());
        entity.setCssClass(dto.getCssClass());
        entity.setListClass(dto.getListClass());
        entity.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : 0);
        entity.setSort(dto.getSort() != null ? dto.getSort() : 0);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        entity.setRemark(dto.getRemark());
        dictDataMapper.insert(entity);
        invalidateCache(tenantId, dictType);
        return toDataVO(entity);
    }

    @Override
    @Transactional
    public void updateData(Long id, DictDataUpdateDTO dto) {
        DictData existing = dictDataMapper.selectOneById(id);
        if (existing == null) throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.data.not.found");
        DictData entity = new DictData();
        entity.setId(id);
        entity.setDictLabel(dto.getDictLabel());
        entity.setCssClass(dto.getCssClass());
        entity.setListClass(dto.getListClass());
        entity.setIsDefault(dto.getIsDefault());
        entity.setSort(dto.getSort());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());
        dictDataMapper.update(entity);
        invalidateCache(UserContext.getTenantId(), existing.getDictType());
    }

    @Override
    @Transactional
    public void removeData(Long id) {
        DictData existing = dictDataMapper.selectOneById(id);
        if (existing == null) throw new BizException(ErrorCode.PARAM_ERROR, "error.dict.data.not.found");
        dictDataMapper.logicDeleteById(id);
        invalidateCache(UserContext.getTenantId(), existing.getDictType());
    }

    @Override
    public void refreshCache() {
        // 清 L1 + 删 L2 + 广播，其它节点收到广播后清各自的 L1
        cache.invalidateAll();
    }

    private void invalidateCache(Long tenantId, String dictType) {
        cache.invalidate(tenantId, dictType);
    }

    private DictTypeVO toTypeVO(DictType entity) {
        DictTypeVO vo = new DictTypeVO();
        vo.setId(entity.getId());
        vo.setDictName(entity.getDictName());
        vo.setI18nKey(DictI18nKeyResolver.resolveType(entity.getDictType()));
        vo.setDictType(entity.getDictType());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private DictDataVO toDataVO(DictData entity) {
        DictDataVO vo = new DictDataVO();
        vo.setId(entity.getId());
        vo.setDictType(entity.getDictType());
        vo.setDictLabel(entity.getDictLabel());
        vo.setI18nKey(DictI18nKeyResolver.resolveData(entity.getDictType(), entity.getDictValue()));
        vo.setDictValue(entity.getDictValue());
        vo.setCssClass(entity.getCssClass());
        vo.setListClass(entity.getListClass());
        vo.setIsDefault(entity.getIsDefault());
        vo.setSort(entity.getSort());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
