package com.precision.rbac.config.service;

import com.precision.core.common.PageResult;
import com.precision.rbac.config.dto.ConfigCreateDTO;
import com.precision.rbac.config.dto.ConfigQueryDTO;
import com.precision.rbac.config.dto.ConfigUpdateDTO;
import com.precision.rbac.config.vo.ConfigVO;

/**
 * 系统参数配置服务。
 *
 * <p>提供 key-value 参数的 CRUD + Caffeine 缓存。
 * {@link #getConfigValue(String)} 可供业务（如登录安全策略、密码策略）读取，
 * 优先级：sys_config &gt; yml 默认值。</p>
 */
public interface ConfigService {

    PageResult<ConfigVO> list(ConfigQueryDTO query);

    ConfigVO create(ConfigCreateDTO dto);

    void update(Long id, ConfigUpdateDTO dto);

    void remove(Long id);

    /** 刷新全部参数缓存 */
    void refreshCache();

    /** 读取参数值（命中缓存；不存在返回 null） */
    String getConfigValue(String key);
}
