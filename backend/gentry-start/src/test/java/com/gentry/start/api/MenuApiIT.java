package com.gentry.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 菜单管理 API 集成测试（5 端点）。 */
@DisplayName("菜单管理 API - /api/v1/menus")
class MenuApiIT extends BaseApiIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private Map<String, Object> createBody() {
        Map<String, Object> b = new HashMap<>();
        b.put("parentId", 0);
        b.put("name", "IT测试菜单");
        b.put("type", 1); // 目录
        b.put("sort", 99 + SEQ.incrementAndGet());
        b.put("path", "/itest" + SEQ.get());
        b.put("permission", "itest:view" + SEQ.get());
        b.put("visible", 1);
        b.put("status", 1);
        return b;
    }

    /**
     * 树查询仍用 ADMIN：{@code system:menu:list} 是**租户级**权限点，V15 刻意没收走。
     *
     * <p>租户管理员必须读得到整棵菜单树，否则「角色管理 → 权限」页画不出勾选框，
     * 它就再也分配不了任何权限 —— 那是把越权缺陷换成功能缺陷。</p>
     */
    @Test
    @DisplayName("树查询：admin → 200 + 数组")
    void tree() throws Exception {
        mockMvc.perform(authedGet("/api/v1/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * CRUD 用 SUPER_ADMIN。
     *
     * <p>原来这里用的是默认的 ADMIN。V15 把 {@code system:menu:{add,edit,remove}}
     * 收成平台级（{@code sys_menu} 是全局表，改菜单会影响所有租户），ADMIN 已经没有
     * 这三个权限，继续用它跑会全部 403。</p>
     */
    @Test
    @DisplayName("CRUD 流：新增→详情→改→删")
    void crudFlow() throws Exception {
        MvcResult r = mockMvc.perform(superPost("/api/v1/menus").content(json(createBody())))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        long id = parse(r).at("/data/id").asLong();

        // 详情用 ADMIN：读取走 system:menu:list，仍是租户级
        mockMvc.perform(authedGet("/api/v1/menus/{id}", id))
                .andExpect(jsonPath("$.data.name").value("IT测试菜单"));

        mockMvc.perform(superPut("/api/v1/menus/{id}", id)
                        .content(json(Map.of("name", "IT测试菜单改", "sort", 88, "permission", "itest:edit"))))
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(superDelete("/api/v1/menus/{id}", id))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("权限不足：无角色用户 → 403")
    void tree_forbidden() throws Exception {
        mockMvc.perform(bareGet("/api/v1/menus").header("Authorization", forbiddenAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录：不带 token → 401")
    void tree_unauthorized() throws Exception {
        mockMvc.perform(bareGet("/api/v1/menus"))
                .andExpect(status().isUnauthorized());
    }

    /** 用 SUPER_ADMIN：要测的是参数校验，用 ADMIN 会先被 403 挡掉（V15 起 add 是平台级）。 */
    @Test
    @DisplayName("参数校验：新增缺菜单名 → 200 + PARAM_ERROR(10002)")
    void create_missingName() throws Exception {
        Map<String, Object> bad = new HashMap<>();
        bad.put("parentId", 0);
        bad.put("type", 1);
        bad.put("sort", 1);
        mockMvc.perform(superPost("/api/v1/menus").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }
}
