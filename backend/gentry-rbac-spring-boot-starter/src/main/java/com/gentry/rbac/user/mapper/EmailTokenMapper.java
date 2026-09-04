package com.gentry.rbac.user.mapper;

import com.gentry.rbac.user.entity.EmailToken;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 邮箱认证令牌 Mapper。
 *
 * <p>token_hash 全局唯一（uk_email_token_hash）——令牌本身是 256 位随机数，凭令牌即凭据。</p>
 */
public interface EmailTokenMapper extends BaseMapper<EmailToken> {

    @Select("SELECT * FROM sys_email_token WHERE token_hash = #{tokenHash} AND deleted = 0 LIMIT 1")
    EmailToken selectByTokenHash(@Param("tokenHash") String tokenHash);

    @Select("SELECT * FROM sys_email_token WHERE email = #{email} "
            + "AND purpose = #{purpose} AND deleted = 0 "
            + "ORDER BY id DESC LIMIT 1")
    EmailToken selectLatestByEmailAndPurpose(@Param("email") String email,
                                             @Param("purpose") String purpose);
}
