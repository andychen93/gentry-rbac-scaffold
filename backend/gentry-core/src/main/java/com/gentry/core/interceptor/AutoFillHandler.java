package com.gentry.core.interceptor;

import com.mybatisflex.annotation.InsertListener;
import com.mybatisflex.annotation.UpdateListener;
import com.gentry.core.constant.TenantConstants;
import com.gentry.core.entity.BaseEntity;
import com.gentry.core.entity.TenantEntity;
import com.gentry.core.security.UserContext;

import java.time.LocalDateTime;

/**
 * MyBatis-Flex 全局监听器，自动填充公共字段
 */
public class AutoFillHandler implements InsertListener, UpdateListener {

    @Override
    public void onInsert(Object entity) {
        if (entity instanceof TenantEntity tenantEntity) {
            if (tenantEntity.getTenantId() == null) {
                Long tenantId = UserContext.getTenantId();
                tenantEntity.setTenantId(tenantId != null ? tenantId : TenantConstants.DEFAULT_TENANT_ID);
            }
        }
        if (entity instanceof BaseEntity baseEntity) {
            if (baseEntity.getCreateBy() == null) {
                baseEntity.setCreateBy(UserContext.getUserId());
            }
            if (baseEntity.getCreateTime() == null) {
                baseEntity.setCreateTime(LocalDateTime.now());
            }
            baseEntity.setUpdateBy(UserContext.getUserId());
            baseEntity.setUpdateTime(LocalDateTime.now());
            if (baseEntity.getDeleted() == null) {
                baseEntity.setDeleted(0);
            }
        }
    }

    @Override
    public void onUpdate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            baseEntity.setUpdateBy(UserContext.getUserId());
            baseEntity.setUpdateTime(LocalDateTime.now());
        }
    }
}
