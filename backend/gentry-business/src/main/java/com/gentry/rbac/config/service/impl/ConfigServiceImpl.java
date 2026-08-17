package com.gentry.rbac.config.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryCondition;
import com.mybatisflex.core.query.QueryWrapper;
import com.gentry.core.common.ErrorCode;
import com.gentry.core.common.PageResult;
import com.gentry.core.exception.BizException;
import com.gentry.rbac.config.dto.ConfigCreateDTO;
import com.gentry.rbac.config.dto.ConfigQueryDTO;
import com.gentry.rbac.config.dto.ConfigUpdateDTO;
import com.gentry.rbac.config.entity.Config;
import com.gentry.rbac.config.mapper.ConfigMapper;
import com.gentry.rbac.config.service.ConfigService;
import com.gentry.rbac.config.vo.ConfigVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements ConfigService {

    private final ConfigMapper configMapper;

    /** 参数缓存：configKey → configValue；写操作后失效对应 key，10 分钟兜底过期 */
    private final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public ConfigServiceImpl(ConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public PageResult<ConfigVO> list(ConfigQueryDTO query) {
        QueryColumn keyCol = new QueryColumn("config_key");
        QueryColumn nameCol = new QueryColumn("config_name");
        QueryColumn idCol = new QueryColumn("id");
        String key = query.getConfigKey();
        String name = query.getConfigName();
        // 手动组合条件（null 跳过），避免 where/and 链式在无 where 时生成非法 SQL
        QueryCondition cond = null;
        if (key != null && !key.isEmpty()) cond = keyCol.like(key);
        if (name != null && !name.isEmpty()) {
            QueryCondition nc = nameCol.like(name);
            cond = (cond == null) ? nc : cond.and(nc);
        }
        QueryWrapper qw = QueryWrapper.create().orderBy(idCol.desc());
        if (cond != null) qw.where(cond);
        Page<Config> page = configMapper.paginate(Page.of(query.getPageNum(), query.getPageSize()), qw);
        List<ConfigVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotalRow(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public ConfigVO create(ConfigCreateDTO dto) {
        if (configMapper.countByKey(dto.getConfigKey()) > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "error.config.key.exists");
        }
        Config c = new Config();
        c.setConfigName(dto.getConfigName());
        c.setConfigKey(dto.getConfigKey());
        c.setConfigValue(dto.getConfigValue());
        c.setConfigType(dto.getConfigType() != null ? dto.getConfigType() : "N");
        c.setRemark(dto.getRemark());
        configMapper.insert(c);
        return toVO(c);
    }

    @Override
    public void update(Long id, ConfigUpdateDTO dto) {
        Config existing = configMapper.selectOneById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        Config c = new Config();
        c.setId(id);
        c.setConfigName(dto.getConfigName());
        c.setConfigValue(dto.getConfigValue());
        c.setConfigType(dto.getConfigType());
        c.setRemark(dto.getRemark());
        configMapper.update(c); // configKey 不可改
        cache.invalidate(existing.getConfigKey());
    }

    @Override
    public void remove(Long id) {
        Config existing = configMapper.selectOneById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        configMapper.deleteById(id); // 逻辑删除（deleted=1）
        cache.invalidate(existing.getConfigKey());
    }

    @Override
    public void refreshCache() {
        cache.invalidateAll();
    }

    @Override
    public String getConfigValue(String key) {
        if (key == null || key.isEmpty()) return null;
        return cache.get(key, k -> {
            Config c = configMapper.selectByKey(k);
            return c != null ? c.getConfigValue() : null;
        });
    }

    private ConfigVO toVO(Config c) {
        ConfigVO vo = new ConfigVO();
        vo.setId(c.getId());
        vo.setConfigName(c.getConfigName());
        vo.setConfigKey(c.getConfigKey());
        vo.setConfigValue(c.getConfigValue());
        vo.setConfigType(c.getConfigType());
        vo.setRemark(c.getRemark());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
