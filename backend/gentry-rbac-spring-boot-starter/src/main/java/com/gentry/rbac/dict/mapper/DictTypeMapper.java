package com.gentry.rbac.dict.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.dict.dto.DictTypeQueryDTO;
import com.gentry.rbac.dict.entity.DictType;
import com.gentry.rbac.dict.vo.DictTypeListVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

    @Select("SELECT * FROM sys_dict_type WHERE dict_type = #{dictType} AND deleted = 0")
    DictType selectByDictType(@Param("dictType") String dictType);

    @Select("SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = #{dictType} AND deleted = 0")
    int countByDictType(@Param("dictType") String dictType);

    @Update("UPDATE sys_dict_type SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int logicDeleteById(Long id);

    List<DictTypeListVO> selectList(@Param("query") DictTypeQueryDTO query);

    long selectCount(@Param("query") DictTypeQueryDTO query);
}
