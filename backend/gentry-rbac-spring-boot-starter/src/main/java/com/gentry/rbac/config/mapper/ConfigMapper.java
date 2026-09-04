package com.gentry.rbac.config.mapper;

import com.mybatisflex.core.BaseMapper;
import com.gentry.rbac.config.entity.Config;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConfigMapper extends BaseMapper<Config> {

    @Select("SELECT * FROM sys_config WHERE config_key = #{configKey} AND deleted = 0")
    Config selectByKey(@Param("configKey") String configKey);

    @Select("SELECT COUNT(*) FROM sys_config WHERE config_key = #{configKey} AND deleted = 0")
    int countByKey(@Param("configKey") String configKey);
}
