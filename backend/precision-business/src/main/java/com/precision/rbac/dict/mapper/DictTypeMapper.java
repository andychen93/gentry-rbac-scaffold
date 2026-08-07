package com.precision.rbac.dict.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.dict.dto.DictTypeQueryDTO;
import com.precision.rbac.dict.entity.DictType;
import com.precision.rbac.dict.vo.DictTypeListVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

    @Select("SELECT * FROM sys_dict_type WHERE tenant_id = #{tenantId} AND dict_type = #{dictType} AND deleted = 0")
    DictType selectByDictType(@Param("tenantId") Long tenantId, @Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE tenant_id = #{tenantId} AND dict_type = #{dictType} AND deleted = 0")
    int countByDictType(@Param("tenantId") Long tenantId, @Param("dictType") String dictType);

    @Update("UPDATE sys_dict_type SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int logicDeleteById(Long id);

    List<DictTypeListVO> selectList(@Param("query") DictTypeQueryDTO query, @Param("tenantId") Long tenantId);

    long selectCount(@Param("query") DictTypeQueryDTO query, @Param("tenantId") Long tenantId);
}
