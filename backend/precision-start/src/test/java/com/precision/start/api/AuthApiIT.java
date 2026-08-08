package com.precision.start.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 认证 API 集成测试（4 端点）：登录 / 登出 / 用户信息 / 修改密码。 */
@DisplayName("认证 API - /api/v1/auth")
class AuthApiIT extends BaseApiIT {

    private static final AtomicInteger NODPT = new AtomicInteger();

    private Map<String, Object> loginBody(String username, String password) {
        return Map.of("tenantCode", DEFAULT_TENANT, "username", username, "password", password);
    }

    @Test
    @DisplayName("登录：admin 正确账号密码 → 200 + token")
    void login_success() throws Exception {
        mockMvc.perform(barePost("/api/v1/auth/login").content(json(loginBody(ADMIN, ADMIN_PWD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.username").value(ADMIN));
    }

    @Test
    @DisplayName("登录：错误密码 → 200 + LOGIN_FAILED(20018)")
    void login_wrongPassword() throws Exception {
        mockMvc.perform(barePost("/api/v1/auth/login").content(json(loginBody(ADMIN, "Wrong@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20018));
    }

    @Test
    @DisplayName("登录：不存在用户 → 200 + LOGIN_FAILED(20018)")
    void login_unknownUser() throws Exception {
        mockMvc.perform(barePost("/api/v1/auth/login")
                        .content(json(loginBody("nobody_" + System.nanoTime(), "Abc@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(20018));
    }

    @Test
    @DisplayName("登录：缺用户名 → 200 + PARAM_ERROR(10002)")
    void login_missingUsername() throws Exception {
        Map<String, Object> bad = new java.util.HashMap<>(loginBody("", "Abc@123456"));
        mockMvc.perform(barePost("/api/v1/auth/login").content(json(bad)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10002));
    }

    @Test
    @DisplayName("用户信息：admin 带 token → 200")
    void userInfo_admin() throws Exception {
        mockMvc.perform(authedGet("/api/v1/auth/user-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value(ADMIN));
    }

    @Test
    @DisplayName("用户信息：不带 token → 401")
    void userInfo_withoutToken() throws Exception {
        mockMvc.perform(bareGet("/api/v1/auth/user-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("修改自己的密码：旧密码正确 → 200（事务回滚，不影响其他用例）")
    void changeOwnPassword() throws Exception {
        mockMvc.perform(authedPut("/api/v1/auth/password")
                        .content(json(Map.of("oldPassword", ADMIN_PWD, "newPassword", "NewAbc@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("登出：带 token → 200")
    void logout() throws Exception {
        mockMvc.perform(authedPost("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("登录：无部门用户不再 500（deptId=null 回归，曾因 ConcurrentHashMap NPE）")
    void login_userWithoutDept() throws Exception {
        String username = "nodpt" + NODPT.incrementAndGet();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("nickname", "无部门用户");
        body.put("password", "NoPerm@123");
        body.put("status", 1); // 故意不传 deptId
        mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(jsonPath("$.code").value(0));
        mockMvc.perform(barePost("/api/v1/auth/login").content(json(loginBody(username, "NoPerm@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
