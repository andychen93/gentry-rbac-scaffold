package com.precision.core.data;

import java.util.List;

/**
 * 数据权限计算结果的上下文（ThreadLocal）。
 *
 * <p>AOP 切面 {@link DataScopeAspect} 在进入标注了 {@link DataScope} 的方法前
 * 解析当前用户的权限范围并放入本上下文；Mapper XML / Service 可通过 {@link #get()}
 * 读取并自行拼接到 SQL 中。</p>
 *
 * <p>上下文使用三态：</p>
 * <ul>
 *     <li>{@link #get()} 返回 {@code null}：未进入 DataScope 方法，不做过滤</li>
 *     <li>返回的 {@link Condition#allData()}=true：全部数据，不做过滤</li>
 *     <li>其他：有具体的过滤条件</li>
 * </ul>
 */
public final class DataScopeContext {

    private static final ThreadLocal<Condition> CURRENT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(Condition condition) {
        CURRENT.set(condition);
    }

    public static Condition get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * 数据权限条件。
     *
     * @param allData            true=全部数据，无需过滤
     * @param deptIds            部门 ID 列表（按本部门 / 本部门及子部门时使用）
     * @param creatorUserId      创建人过滤（仅本人时使用）
     * @param deptIdField        SQL 中部门字段名（已含表别名）
     * @param createByField      SQL 中创建人字段名（已含表别名）
     */
    public record Condition(
            boolean allData,
            List<Long> deptIds,
            Long creatorUserId,
            String deptIdField,
            String createByField
    ) {
        public static Condition all() {
            return new Condition(true, List.of(), null, null, null);
        }

        public static Condition byDepts(List<Long> deptIds, String deptIdField) {
            return new Condition(false, deptIds, null, deptIdField, null);
        }

        public static Condition byCreator(Long userId, String createByField) {
            return new Condition(false, List.of(), userId, null, createByField);
        }

        public boolean hasDeptFilter() {
            return !allData && deptIds != null && !deptIds.isEmpty();
        }

        public boolean hasCreatorFilter() {
            return !allData && creatorUserId != null;
        }
    }
}
