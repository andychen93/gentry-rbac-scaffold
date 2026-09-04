package com.gentry.rbac.dict.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.dict.entity.DictData;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DictDataMapper extends BaseMapper<DictData> {

    List<DictData> selectByDictType(@Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict_data WHERE dict_type = #{dictType} AND dict_value = #{dictValue} AND deleted = 0")
    int countByDictValue(@Param("dictType") String dictType, @Param("dictValue") String dictValue);

    @Update("UPDATE sys_dict_data SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int logicDeleteById(Long id);

    @Update("UPDATE sys_dict_data SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE dict_type = #{dictType} AND deleted = 0")
    int logicDeleteByDictType(@Param("dictType") String dictType);
}
