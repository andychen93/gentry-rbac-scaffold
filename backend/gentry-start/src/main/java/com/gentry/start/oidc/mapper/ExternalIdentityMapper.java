package com.gentry.start.oidc.mapper;

import com.gentry.start.oidc.entity.ExternalIdentityEntity;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 外部身份映射 Mapper，对应 sys_external_identity 表。
 */
@Mapper
public interface ExternalIdentityMapper extends BaseMapper<ExternalIdentityEntity> {

    /**
     * 根据 (identityProvider, externalSubject) 查询绑定的本地用户 ID。
     */
    @Select("SELECT local_user_id FROM sys_external_identity "
        + "WHERE identity_provider = #{identityProvider} AND external_subject = #{externalSubject} AND deleted = 0")
    Long selectLocalUserId(@Param("identityProvider") String identityProvider,
                            @Param("externalSubject") String externalSubject);
}
