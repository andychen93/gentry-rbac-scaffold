package com.precision.rbac.dict.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.dict.entity.DictData;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    List<DictData> selectByDictType(@Param("tenantId") Long tenantId, @Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict_data WHERE tenant_id = #{tenantId} AND dict_type = #{dictType} AND dict_value = #{dictValue} AND deleted = 0")
    int countByDictValue(@Param("tenantId") Long tenantId, @Param("dictType") String dictType, @Param("dictValue") String dictValue);

    @Update("UPDATE sys_dict_data SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int logicDeleteById(Long id);

    @Update("UPDATE sys_dict_data SET deleted = 1, update_time = NOW() WHERE tenant_id = #{tenantId} AND dict_type = #{dictType} AND deleted = 0")
    int logicDeleteByDictType(@Param("tenantId") Long tenantId, @Param("dictType") String dictType);
}
