package com.gentry.core.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * B 类 key 派生器测试（U-16 ~ U-21）。
 *
 * <p>派生规则的正确性直接决定「菜单/字典能不能被翻译」，而错误是<b>静默</b>的——
 * 派生出畸形 key 不会报错，只会永远查不到译文、回退成中文。所以边界要覆盖全。</p>
 */
class I18nKeyResolverTest {

    @Nested
    @DisplayName("MenuI18nKeyResolver")
    class Menu {

        @Test
        @DisplayName("resolve_有permission_按permission派生")
        void resolve_有permission_按permission派生() {
            assertThat(MenuI18nKeyResolver.resolve("system:user:add", "/system/user"))
                    .as("permission 优先于 path")
                    .isEqualTo("menu.system.user.add");
            assertThat(MenuI18nKeyResolver.resolve("system:user", null))
                    .isEqualTo("menu.system.user");
            assertThat(MenuI18nKeyResolver.resolve("monitor:redis:info", null))
                    .isEqualTo("menu.monitor.redis.info");
        }

        @Test
        @DisplayName("resolve_permission为空有path_按path派生")
        void resolve_permission为空有path_按path派生() {
            // 3 条目录菜单实测都是这种：permission 为 NULL，只有 path
            assertThat(MenuI18nKeyResolver.resolve(null, "/system")).isEqualTo("menu.system");
            assertThat(MenuI18nKeyResolver.resolve("", "/monitor")).isEqualTo("menu.monitor");
            assertThat(MenuI18nKeyResolver.resolve("   ", "/monitor-center"))
                    .as("连字符要保留，不能当分隔符")
                    .isEqualTo("menu.monitor-center");
            assertThat(MenuI18nKeyResolver.resolve(null, "/system/user"))
                    .isEqualTo("menu.system.user");
        }

        @Test
        @DisplayName("resolve_两者皆空_返回null")
        void resolve_两者皆空_返回null() {
            // null 是有语义的：前端据此回退显示库里的 name
            assertThat(MenuI18nKeyResolver.resolve(null, null)).isNull();
            assertThat(MenuI18nKeyResolver.resolve("", "")).isNull();
            assertThat(MenuI18nKeyResolver.resolve("  ", "  ")).isNull();
        }

        @Test
        @DisplayName("resolve_含首尾空格_trim后派生")
        void resolve_含首尾空格_trim后派生() {
            assertThat(MenuI18nKeyResolver.resolve("  system:user:add  ", null))
                    .isEqualTo("menu.system.user.add");
            assertThat(MenuI18nKeyResolver.resolve(null, "  /system  ")).isEqualTo("menu.system");
        }

        @Test
        @DisplayName("resolve_畸形path_折叠连续点并去尾点")
        void resolve_畸形path_折叠连续点并去尾点() {
            // 这类输入不会报错，只会产出永远查不到译文的畸形 key，必须归一化
            assertThat(MenuI18nKeyResolver.resolve(null, "//system")).isEqualTo("menu.system");
            assertThat(MenuI18nKeyResolver.resolve(null, "/system/")).isEqualTo("menu.system");
            assertThat(MenuI18nKeyResolver.resolve(null, "/system//user/"))
                    .isEqualTo("menu.system.user");
            assertThat(MenuI18nKeyResolver.resolve("system::user", null))
                    .isEqualTo("menu.system.user");
            assertThat(MenuI18nKeyResolver.resolve("system:user:", null))
                    .isEqualTo("menu.system.user");
        }

        @Test
        @DisplayName("resolve_path不以斜杠开头_仍能派生")
        void resolve_path不以斜杠开头_仍能派生() {
            // 库里的 path 都以 / 开头，但不能假定
            assertThat(MenuI18nKeyResolver.resolve(null, "system")).isEqualTo("menusystem");
        }

        @Test
        @DisplayName("resolve_种子数据全部可派生且无重复")
        void resolve_种子数据全部可派生且无重复() {
            // 与实测一致：3 目录靠 path、12 页面 + 56 按钮靠 permission，71/71 可派生
            String[][] samples = {
                    {null, "/system", "menu.system"},
                    {null, "/monitor", "menu.monitor"},
                    {null, "/monitor-center", "menu.monitor-center"},
                    {"system:user", "/system/user", "menu.system.user"},
                    {"system:menu:list", null, "menu.system.menu.list"},
                    {"monitor:redis:info", "/monitor/redis", "menu.monitor.redis.info"},
            };
            java.util.Set<String> keys = new java.util.LinkedHashSet<>();
            for (String[] s : samples) {
                String key = MenuI18nKeyResolver.resolve(s[0], s[1]);
                assertThat(key).isEqualTo(s[2]);
                assertThat(keys.add(key)).as("派生 key 不应重复: %s", key).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("DictI18nKeyResolver")
    class Dict {

        @Test
        @DisplayName("resolveData_正常_返回dict点type点value")
        void resolveData_正常_返回dict点type点value() {
            assertThat(DictI18nKeyResolver.resolveData("sys_user_gender", "1"))
                    .isEqualTo("dict.sys_user_gender.1");
            assertThat(DictI18nKeyResolver.resolveData("sys_user_post", "CEO"))
                    .as("dict_value 可以是字母，大小写原样保留")
                    .isEqualTo("dict.sys_user_post.CEO");
            assertThat(DictI18nKeyResolver.resolveData("sys_data_scope", "5"))
                    .isEqualTo("dict.sys_data_scope.5");
        }

        @Test
        @DisplayName("resolveData_入参任一为空_返回null")
        void resolveData_入参任一为空_返回null() {
            assertThat(DictI18nKeyResolver.resolveData(null, "1")).isNull();
            assertThat(DictI18nKeyResolver.resolveData("sys_user_gender", null)).isNull();
            assertThat(DictI18nKeyResolver.resolveData("", "1")).isNull();
            assertThat(DictI18nKeyResolver.resolveData("sys_user_gender", "  ")).isNull();
        }

        @Test
        @DisplayName("resolveData_含首尾空格_trim后派生")
        void resolveData_含首尾空格_trim后派生() {
            assertThat(DictI18nKeyResolver.resolveData(" sys_user_gender ", " 1 "))
                    .isEqualTo("dict.sys_user_gender.1");
        }

        @Test
        @DisplayName("resolveType_正常与空值")
        void resolveType_正常与空值() {
            assertThat(DictI18nKeyResolver.resolveType("sys_user_gender"))
                    .isEqualTo("dict.type.sys_user_gender");
            assertThat(DictI18nKeyResolver.resolveType(" sys_data_scope "))
                    .isEqualTo("dict.type.sys_data_scope");
            assertThat(DictI18nKeyResolver.resolveType(null)).isNull();
            assertThat(DictI18nKeyResolver.resolveType("  ")).isNull();
        }

        @Test
        @DisplayName("字典项key与字典类型key不会互相撞")
        void 字典项key与字典类型key不会互相撞() {
            // dict.type.* 前缀独立，避免 dict_type 名恰好等于某个 dict_value 时撞车
            assertThat(DictI18nKeyResolver.resolveType("sys_user_gender"))
                    .isNotEqualTo(DictI18nKeyResolver.resolveData("sys_user_gender", "type"));
        }
    }
}
