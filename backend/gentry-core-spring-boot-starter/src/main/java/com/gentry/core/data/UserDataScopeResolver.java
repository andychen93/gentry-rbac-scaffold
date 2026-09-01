package com.gentry.core.data;

/**
 * 数据权限解析 SPI：根据 userId 查出其最大数据权限范围。
 *
 * <p>语义约定：</p>
 * <ul>
 *     <li>1 = 全部数据</li>
 *     <li>2 = 本部门及子部门</li>
 *     <li>3 = 仅本部门</li>
 *     <li>4 = 仅本人</li>
 *     <li>5 = 自定义（当前按 2 处理）</li>
 * </ul>
 *
 * <p>多角色时取最宽松的范围（即 data_scope 值最小）。</p>
 */
public interface UserDataScopeResolver {

    /**
     * 返回用户的数据权限范围。未绑定角色或未登录时返回 null（表示跳过过滤）。
     */
    Integer resolveDataScope(Long userId);
}
