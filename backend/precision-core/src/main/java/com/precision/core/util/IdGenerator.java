package com.precision.core.util;

import com.mybatisflex.core.keygen.impl.FlexIDKeyGenerator;

/**
 * ID 生成工具类
 * <p>
 * 封装 MyBatis-Flex 的 FlexID 生成器，供批量插入等场景使用。
 * 单条插入时由 @Id 注解自动生成，无需手动调用。
 */
public final class IdGenerator {

    private static final FlexIDKeyGenerator FLEX_ID = new FlexIDKeyGenerator();

    private IdGenerator() {}

    /**
     * 生成一个全局唯一的 FlexID（雪花算法）
     */
    public static Long nextId() {
        Object id = FLEX_ID.generate(null, null);
        return (Long) id;
    }
}
