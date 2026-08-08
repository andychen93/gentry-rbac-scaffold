package com.precision.start.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API 集成测试基类。
 *
 * <p>启动完整 Spring 上下文（profile = mysql + test），用 MockMvc 打真实 {@code /api/v1/...}，
 * 完整经过 Sa-Token 鉴权 / 权限 / 租户 / 数据权限链路。
 *
 * <p>鉴权：通过<b>真实 {@code /auth/login}</b> 取 token。登录流程会往 SaSession 写入
 * {@code tenantId/userId/deptId/permissionList}（见 AuthServiceImpl.buildLoginResult），
 * SaTokenConfig 拦截器据此重建 UserContext。注意：<b>不能用裸 {@code StpUtil.login(userId)}</b>
 * ——那样会丢 tenantId，租户隔离失效。
 *
 * <p>隔离：{@code @Transactional} 回滚每个用例的 DB 写，种子数据不被污染，读靠 Flyway 种子；
 * 测试 Redis 用 db 15（见 application-test.yml），不污染运行中后端 db 0 会话。
 *
 * <p>权限矩阵复用种子用户：admin(全权限) 跑 happy-path；chenli(dev 角色，无 system:user/dept:*
 * 权限) 跑 403；不带 Authorization 头跑 401。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"mysql", "test"})
@Transactional
public abstract class BaseApiIT {

    protected static final String ADMIN = "admin";
    protected static final String ADMIN_PWD = "Abc@123456";
    protected static final String CHENLI = "chenli";        // dev 角色 → 用于 403
    protected static final String CHENLI_PWD = "Chenli@2026";
    protected static final String DEFAULT_TENANT = "default";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** 默认 admin 的 Authorization 头值（"Bearer xxx"），每个用例开始前重新登录。 */
    protected String auth;

    @BeforeEach
    void baseLogin() throws Exception {
        this.auth = login(ADMIN, ADMIN_PWD);
    }

    // ==================== 鉴权辅助 ====================

    /** 真实登录指定用户，返回 {@code "Bearer <token>"}。 */
    protected String login(String username, String password) throws Exception {
        // 每次登录带唯一 X-Forwarded-For：login 有 @RateLimit(10/min per IP)，
        // 同 IP 跑全量套件会被限流(40001)。IpUtil 优先读该头，故每次给不同 IP 绕过。
        int n = IP_SEQ.incrementAndGet();
        String xff = "10." + ((n >> 8) & 0xff) + "." + (n & 0xff) + ".1";
        String body = objectMapper.writeValueAsString(Map.of(
                "tenantCode", DEFAULT_TENANT,
                "username", username,
                "password", password));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", xff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + root.at("/data/token").asText();
    }

    /** 无角色用户序号（保证用户名唯一且 ≤20 字符，符合 UserCreateDTO 正则）。 */
    private static final AtomicInteger NO_PERM_SEQ = new AtomicInteger();

    /** 每次 login 的 X-Forwarded-For 序号，绕过 login 的 per-IP 限流。 */
    private static final AtomicInteger IP_SEQ = new AtomicInteger();

    /**
     * 经 admin 调用 {@code POST /api/v1/users} 建一个<b>无角色</b>用户并登录，返回其 Authorization 头。
     * 其 permissionList 为空 → 任意 {@code @SaCheckPermission} 端点都返回 403。
     * 用于各 IT 的 403 覆盖（chenli 的 dev 角色除 system:user:* 外几乎全权，无法稳定触发 403）。
     * 走 API（而非裸 JDBC）建用户，保证与登录查询在同一 MyBatis 事务内可见；用户行随测试事务回滚。
     */
    protected String forbiddenAuth() throws Exception {
        String username = "tnp" + NO_PERM_SEQ.incrementAndGet(); // 字母开头、4-20 字符
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("nickname", "无权限用户");
        body.put("password", "NoPerm@123");
        body.put("deptId", 100); // 必填上下文：buildLoginResult 里 session.set("deptId", null) 会 NPE
        body.put("status", 1);
        // 不传 roleIds → 无角色 → 无权限
        mockMvc.perform(authedPost("/api/v1/users").content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        return login(username, "NoPerm@123");
    }

    /** 解析响应 JSON 树。 */
    protected JsonNode parse(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** 序列化请求体。 */
    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ==================== admin 鉴权请求 builder（默认带 this.auth） ====================

    protected MockHttpServletRequestBuilder authedGet(String url, Object... uriVars) {
        return get(url, uriVars).header("Authorization", auth);
    }

    protected MockHttpServletRequestBuilder authedPost(String url) {
        return post(url).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder authedPut(String url, Object... uriVars) {
        return put(url, uriVars).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder authedDelete(String url, Object... uriVars) {
        return delete(url, uriVars).header("Authorization", auth);
    }

    // ===== 无鉴权请求 builder（用于 401 未登录 / 403 自定义低权限头） =====

    protected MockHttpServletRequestBuilder bareGet(String url, Object... uriVars) {
        return get(url, uriVars);
    }

    protected MockHttpServletRequestBuilder barePost(String url) {
        return post(url).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder barePut(String url, Object... uriVars) {
        return put(url, uriVars).contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder bareDelete(String url, Object... uriVars) {
        return delete(url, uriVars);
    }
}
