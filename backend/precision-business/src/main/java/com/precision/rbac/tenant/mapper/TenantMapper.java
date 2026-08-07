package com.precision.rbac.tenant.mapper;

import com.mybatisflex.core.BaseMapper;
import com.precision.rbac.tenant.dto.TenantQueryDTO;
import com.precision.rbac.tenant.entity.Tenant;
import com.precision.rbac.tenant.vo.TenantListVO;
import com.precision.rbac.tenant.vo.TenantOptionVO;
import com.precision.rbac.tenant.vo.TenantStatistics;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {

    @Select("SELECT * FROM sys_tenant WHERE code = #{code} AND deleted = 0")
    Tenant selectByCode(String code);

    @Select("SELECT code, name FROM sys_tenant WHERE status = 1 AND deleted = 0 AND (expire_time IS NULL OR expire_time > NOW()) ORDER BY id")
    List<TenantOptionVO> selectOptions();

    @Update("UPDATE sys_tenant SET deleted = 1, update_time = NOW() WHERE id = #{id}")
    int logicDeleteById(Long id);

    @Update("UPDATE sys_tenant SET status = #{status}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE sys_tenant SET config = #{config}::jsonb, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateConfig(@Param("id") Long id, @Param("config") String config);

    /** 分页列表查询（含 userCount 统计） */
    List<TenantListVO> selectList(@Param("query") TenantQueryDTO query);

    /** 分页列表计数 */
    long selectCount(@Param("query") TenantQueryDTO query);

    /** 统计租户下的用户数、部门数、角色数 */
    TenantStatistics selectStatistics(@Param("tenantId") Long tenantId);

    /** 查询租户下的用户数 */
    @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id = #{tenantId} AND deleted = 0")
    int countUsersByTenantId(Long tenantId);

    /** 查询租户下所有用户ID */
    @Select("SELECT id FROM sys_user WHERE tenant_id = #{tenantId} AND deleted = 0")
    List<Long> selectUserIdsByTenantId(Long tenantId);
}
