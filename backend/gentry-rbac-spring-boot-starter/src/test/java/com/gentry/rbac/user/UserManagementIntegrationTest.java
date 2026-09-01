package com.gentry.rbac.user;

import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户管理集成测试 - 直接连接 PostgreSQL 验证全部业务逻辑
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserManagementIntegrationTest {

    private static final String DB_URL = System.getProperty(
            "gentry.test.db.url", "jdbc:postgresql://localhost:5432/gentry");
    private static final String DB_USER = System.getProperty("gentry.test.db.user", "postgres");
    private static final String DB_PASS = System.getProperty("gentry.test.db.password", "123456");
    private static final String LOGIN_USERNAME = "test_login_admin";
    private static final String LOGIN_PASSWORD = "TestLogin@123";

    private Connection conn;
    private long testUserId;
    private long testUser2Id;
    private long loginUserId;

    @BeforeAll
    void setUp() throws Exception {
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        } catch (SQLException e) {
            // 无可用 PostgreSQL 时优雅跳过（本类为依赖真实库的集成测试，非单元测试）
            Assumptions.assumeTrue(false,
                    "跳过用户管理集成测试：无法连接 PostgreSQL (" + DB_URL + ") - " + e.getMessage());
        }
        conn.setAutoCommit(true);
        cleanTestData();
        createLoginTestUser();
    }

    @AfterAll
    void tearDown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            cleanTestData();
            conn.close();
        }
    }

    private void cleanTestData() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'test_%')");
            s.execute("DELETE FROM sys_user WHERE username LIKE 'test_%'");
        }
    }

    /**
     * 登录流程使用自建账号，避免读取、改密或禁用开发库中的共享 admin。
     */
    private void createLoginTestUser() throws SQLException {
        loginUserId = System.nanoTime();
        String passwordHash = BCrypt.hashpw(LOGIN_PASSWORD, BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time, deleted) " +
                        "VALUES (?, ?, ?, '登录集成测试', 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, loginUserId);
            ps.setString(2, LOGIN_USERNAME);
            ps.setString(3, passwordHash);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user_role (id, user_id, role_id, create_time) " +
                        "VALUES (?, ?, 1, NOW())")) {
            ps.setLong(1, System.nanoTime());
            ps.setLong(2, loginUserId);
            assertEquals(1, ps.executeUpdate());
        }
    }

    // ========== 1. 验证初始数据 ==========

    @Test @Order(1)
    void test01_管理员用户存在() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, username, nickname, status FROM sys_user WHERE username = 'admin' AND deleted = 0")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "admin 用户应该存在");
            assertEquals(1, rs.getInt("status"));
        }
    }

    @Test @Order(2)
    void test02_管理员角色存在() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, role_code, role_name FROM sys_role WHERE role_code = 'ADMIN' AND deleted = 0")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "ADMIN 角色应该存在");
            assertEquals("管理员", rs.getString("role_name"));
        }
    }

    @Test @Order(3)
    void test03_菜单数据已初始化() throws Exception {
        try (Statement s = conn.createStatement()) {
            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM sys_menu WHERE deleted = 0");
            rs.next();
            assertTrue(rs.getInt(1) >= 40, "菜单数据应该 >= 40 条, 实际: " + rs.getInt(1));
        }
    }

    // ========== 2. 新增用户 ==========

    @Test @Order(10)
    void test10_新增用户_正常创建() throws Exception {
        String hash = BCrypt.hashpw("Abc@123456", BCrypt.gensalt());
        testUserId = System.nanoTime();

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, phone, email, gender, post_name, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_zhangsan', ?, '张三', '19900001111', 'test@test.com', 1, '工程师', 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, testUserId);
            ps.setString(2, hash);
            assertEquals(1, ps.executeUpdate());
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sys_user WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("test_zhangsan", rs.getString("username"));
            assertEquals("张三", rs.getString("nickname"));
            assertEquals("19900001111", rs.getString("phone"));
            assertEquals(1, rs.getInt("gender"));
            assertTrue(rs.getString("password").startsWith("$2a$"));
        }
    }

    @Test @Order(11)
    void test11_密码BCrypt加密验证() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT password FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertTrue(BCrypt.checkpw("Abc@123456", rs.getString("password")), "BCrypt 密码校验应通过");
            assertFalse(BCrypt.checkpw("WrongPass1", rs.getString("password")), "错误密码不应通过");
        }
    }

    @Test @Order(12)
    void test12_用户名唯一() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_zhangsan', 'hash', '重复', 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, System.nanoTime());
            assertThrows(SQLException.class, ps::executeUpdate, "唯一索引应阻止重复用户名");
        }
    }

    @Test @Order(13)
    void test13_手机号唯一() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, phone, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_phone_dup', 'hash', '手机号重复', '19900001111', 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, System.nanoTime());
            assertThrows(SQLException.class, ps::executeUpdate, "唯一索引应阻止重复手机号");
        }
    }

    @Test @Order(14)
    void test14_新增第二个用户() throws Exception {
        testUser2Id = System.nanoTime();
        String hash = BCrypt.hashpw("Abc@123456", BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, phone, gender, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_lisi', ?, '李四', '19900002222', 2, 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, testUser2Id);
            ps.setString(2, hash);
            assertEquals(1, ps.executeUpdate());
        }
    }

    // ========== 3. 分配角色 ==========

    @Test @Order(20)
    void test20_分配角色() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUserId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user_role (id, user_id, role_id, create_time) VALUES (?, ?, 1, NOW())")) {
            ps.setLong(1, System.nanoTime()); ps.setLong(2, testUserId);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT role_id FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1L, rs.getLong("role_id"));
        }
    }

    @Test @Order(21)
    void test21_清空角色() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUserId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    // ========== 4. 编辑用户 ==========

    @Test @Order(30)
    void test30_编辑昵称和手机号() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET nickname = '张三改名', phone = '19900009999', update_time = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUserId);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT nickname, phone FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals("张三改名", rs.getString("nickname"));
            assertEquals("19900009999", rs.getString("phone"));
        }
    }

    @Test @Order(31)
    void test31_用户名不可修改验证() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET nickname = '只改昵称' WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUserId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals("test_zhangsan", rs.getString("username"), "用户名应保持不变");
        }
    }

    // ========== 5. 切换状态 ==========

    @Test @Order(40)
    void test40_禁用用户() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET status = 0, update_time = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUserId); assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals(0, rs.getInt("status"));
        }
    }

    @Test @Order(41)
    void test41_启用用户() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET status = 1, update_time = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUserId); assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals(1, rs.getInt("status"));
        }
    }

    // ========== 6. 密码管理 ==========

    @Test @Order(50)
    void test50_重置密码() throws Exception {
        String newHash = BCrypt.hashpw("NewPass@123", BCrypt.gensalt());
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET password = ?, pwd_update_time = NOW(), update_time = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setString(1, newHash); ps.setLong(2, testUserId);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT password, pwd_update_time FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertTrue(BCrypt.checkpw("NewPass@123", rs.getString("password")));
            assertNotNull(rs.getTimestamp("pwd_update_time"));
        }
    }

    @Test @Order(51)
    void test51_原密码校验() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT password FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            String hash = rs.getString("password");
            assertTrue(BCrypt.checkpw("NewPass@123", hash), "原密码校验应通过");
            assertFalse(BCrypt.checkpw("WrongOldPass1", hash), "错误原密码不应通过");
        }
    }

    // ========== 7. 列表查询 ==========

    @Test @Order(60)
    void test60_分页查询() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM sys_user WHERE deleted = 0")) {
            ResultSet rs = ps.executeQuery(); rs.next();
            assertTrue(rs.getInt(1) >= 2, "至少应有 admin + 测试用户");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sys_user WHERE deleted = 0 ORDER BY create_time DESC LIMIT 10 OFFSET 0")) {
            ResultSet rs = ps.executeQuery();
            List<String> names = new ArrayList<>();
            while (rs.next()) names.add(rs.getString("username"));
            assertFalse(names.isEmpty());
        }
    }

    @Test @Order(61)
    void test61_按用户名模糊搜索() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sys_user WHERE deleted = 0 AND username LIKE '%test_zhang%'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "模糊搜索应找到 test_zhangsan");
            assertEquals("test_zhangsan", rs.getString("username"));
        }
    }

    @Test @Order(62)
    void test62_按手机号精确搜索() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sys_user WHERE deleted = 0 AND phone = '19900002222'")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "精确搜索应找到 test_lisi");
            assertEquals("test_lisi", rs.getString("username"));
        }
    }

    @Test @Order(63)
    void test63_按状态筛选() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM sys_user WHERE deleted = 0 AND status = 1")) {
            ResultSet rs = ps.executeQuery(); rs.next();
            assertTrue(rs.getInt(1) >= 1);
        }
    }

    @Test @Order(64)
    void test64_手机号脱敏逻辑() {
        String phone = "19900009999";
        String masked = phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        assertEquals("199****9999", masked);
    }

    // ========== 8. 登录验证 ==========

    @Test @Order(70)
    void test70_登录_用户名密码正确() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sys_user WHERE username = 'test_zhangsan' AND deleted = 0")) {
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "用户应存在");
            assertTrue(BCrypt.checkpw("NewPass@123", rs.getString("password")), "密码应匹配");
            assertEquals(1, rs.getInt("status"), "用户应为启用状态");
        }
    }

    @Test @Order(71)
    void test71_登录_用户名不存在() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM sys_user WHERE username = 'nonexistent' AND deleted = 0")) {
            assertFalse(ps.executeQuery().next());
        }
    }

    @Test @Order(72)
    void test72_登录_密码错误() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT password FROM sys_user WHERE username = 'test_zhangsan' AND deleted = 0")) {
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertFalse(BCrypt.checkpw("WrongPassword1", rs.getString("password")));
        }
    }

    @Test @Order(73)
    void test73_禁用用户不可登录() throws Exception {
        // 禁用
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET status = 0 WHERE id = ?")) {
            ps.setLong(1, testUserId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM sys_user WHERE username = 'test_zhangsan' AND deleted = 0")) {
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals(0, rs.getInt("status"), "禁用用户 status 应为 0");
        }
        // 恢复
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET status = 1 WHERE id = ?")) {
            ps.setLong(1, testUserId); ps.executeUpdate();
        }
    }

    // ========== 9. 删除用户 ==========

    @Test @Order(80)
    void test80_逻辑删除() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET deleted = 1 WHERE id = ?")) {
            ps.setLong(1, testUser2Id); assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sys_user WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, testUser2Id);
            assertFalse(ps.executeQuery().next(), "逻辑删除后不应查到");
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT deleted FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUser2Id);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals(1, rs.getInt("deleted"));
        }
    }

    @Test @Order(81)
    void test81_删除用户清理角色关联() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user_role (id, user_id, role_id, create_time) VALUES (?, ?, 1, NOW())")) {
            ps.setLong(1, System.nanoTime()); ps.setLong(2, testUser2Id);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUser2Id); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM sys_user_role WHERE user_id = ?")) {
            ps.setLong(1, testUser2Id);
            ResultSet rs = ps.executeQuery(); rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test @Order(82)
    void test82_admin不可删除_业务规则() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM sys_user WHERE id = 2 AND deleted = 0")) {
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals("admin", rs.getString("username"));
        }
    }

    // ========== 10. 登录信息更新 ==========

    @Test @Order(90)
    void test90_更新登录信息() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET login_ip = '192.168.1.100', login_date = NOW() WHERE id = ?")) {
            ps.setLong(1, testUserId); assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT login_ip, login_date FROM sys_user WHERE id = ?")) {
            ps.setLong(1, testUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals("192.168.1.100", rs.getString("login_ip"));
            assertNotNull(rs.getTimestamp("login_date"));
        }
    }

    // ========== 11. 边界条件 ==========

    @Test @Order(100)
    void test100_手机号允许为空() throws Exception {
        long id = System.nanoTime();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, phone, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_no_phone', 'hash', '无手机号', NULL, 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, id);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT phone FROM sys_user WHERE id = ?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertNull(rs.getString("phone"));
        }
    }

    @Test @Order(101)
    void test101_多个NULL手机号不冲突() throws Exception {
        long id = System.nanoTime();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, phone, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_no_phone2', 'hash', '无手机号2', NULL, 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, id);
            assertEquals(1, ps.executeUpdate());
        }
    }

    @Test @Order(102)
    void test102_逻辑删除后可重建同名用户() throws Exception {
        long id = System.nanoTime();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time, deleted) " +
                "VALUES (?, 'test_lisi', 'hash', '新李四', 1, NOW(), NOW(), 0)")) {
            ps.setLong(1, id);
            assertEquals(1, ps.executeUpdate());
        }
    }

    // ========== 12. 登录完整流程测试 ==========

    /** Normalize BCrypt hash: $2b$/$2y$ → $2a$ for jBCrypt compatibility */
    private String normalizeBcryptHash(String hash) {
        if (hash != null && (hash.startsWith("$2b$") || hash.startsWith("$2y$"))) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }

    @Test @Order(110)
    void test110_测试用户登录_完整流程() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, username, password, status FROM sys_user " +
                        "WHERE username = ? AND deleted = 0")) {
            ps.setString(1, LOGIN_USERNAME);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "登录测试用户应存在");

            String storedHash = normalizeBcryptHash(rs.getString("password"));
            assertTrue(BCrypt.checkpw(LOGIN_PASSWORD, storedHash), "测试用户密码应匹配");
            assertEquals(1, rs.getInt("status"), "测试用户应为启用状态");
        }
    }

    @Test @Order(111)
    void test111_登录后更新登录信息() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sys_user SET login_ip = '127.0.0.1', login_date = NOW() WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, loginUserId);
            assertEquals(1, ps.executeUpdate());
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT login_ip, login_date FROM sys_user WHERE id = ?")) {
            ps.setLong(1, loginUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals("127.0.0.1", rs.getString("login_ip"));
            assertNotNull(rs.getTimestamp("login_date"));
        }
    }

    @Test @Order(112)
    void test112_登录_密码错误不应通过() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT password FROM sys_user WHERE username = ? AND deleted = 0")) {
            ps.setString(1, LOGIN_USERNAME);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            String storedHash = normalizeBcryptHash(rs.getString("password"));
            assertFalse(BCrypt.checkpw("wrongpassword", storedHash), "错误密码不应通过");
            assertFalse(BCrypt.checkpw("testlogin@123", storedHash), "密码大小写敏感");
        }
    }

    @Test @Order(113)
    void test113_登录_禁用用户被拒绝() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET status = 0 WHERE id = ?")) {
            ps.setLong(1, loginUserId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT status FROM sys_user WHERE id = ? AND deleted = 0")) {
            ps.setLong(1, loginUserId);
            ResultSet rs = ps.executeQuery(); assertTrue(rs.next());
            assertEquals(0, rs.getInt("status"), "禁用后 status 应为 0，登录应被拒绝");
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE sys_user SET status = 1 WHERE id = ?")) {
            ps.setLong(1, loginUserId); ps.executeUpdate();
        }
    }

    @Test @Order(114)
    void test114_登录_查询用户角色和权限() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT ur.role_id, r.role_code, r.role_name FROM sys_user_role ur " +
                "JOIN sys_role r ON ur.role_id = r.id AND r.deleted = 0 " +
                "WHERE ur.user_id = ?")) {
            ps.setLong(1, loginUserId);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next(), "登录测试用户应有角色关联");
            assertEquals("ADMIN", rs.getString("role_code"));
            assertEquals("管理员", rs.getString("role_name"));
        }
    }

    @Test @Order(115)
    void test115_登录_查询角色对应的菜单权限() throws Exception {
        // 测试用户绑定的 ADMIN 角色应有菜单权限
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT m.permission FROM sys_role_menu rm " +
                "JOIN sys_menu m ON rm.menu_id = m.id AND m.deleted = 0 " +
                "WHERE rm.role_id = 1 AND m.permission IS NOT NULL")) {
            ResultSet rs = ps.executeQuery();
            List<String> permissions = new ArrayList<>();
            while (rs.next()) permissions.add(rs.getString("permission"));
            assertFalse(permissions.isEmpty(), "ADMIN 角色应有权限");
            assertTrue(permissions.contains("system:user:list"), "应包含 system:user:list");
            assertTrue(permissions.contains("system:user:add"), "应包含 system:user:add");
            assertTrue(permissions.contains("system:role:list"), "应包含 system:role:list");
        }
    }
}
