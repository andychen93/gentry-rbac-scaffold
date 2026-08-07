package com.precision.monitor.redis.service.impl;

import com.precision.monitor.redis.constant.RedisKeyDefines;
import com.precision.monitor.redis.service.RedisMonitorService;
import com.precision.monitor.redis.vo.RedisCommandStatVO;
import com.precision.monitor.redis.vo.RedisInfoVO;
import com.precision.monitor.redis.vo.RedisKeyDefineVO;
import com.precision.monitor.redis.vo.RedisMonitorVO;
import com.precision.monitor.redis.vo.RedisSlowLogVO;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

/**
 * Redis 监控服务实现。
 *
 * <p>直接通过 {@link StringRedisTemplate} 的 {@code RedisCallback} 调用服务器命令
 * ({@code INFO}、{@code DBSIZE})，解析结果为 VO。</p>
 */
@Service
public class RedisMonitorServiceImpl implements RedisMonitorService {

    private final StringRedisTemplate redisTemplate;

    public RedisMonitorServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public RedisMonitorVO getMonitorInfo() {
        Properties info = redisTemplate.execute((RedisCallback<Properties>) con -> con.serverCommands().info());
        Properties commandStats = redisTemplate.execute((RedisCallback<Properties>) con -> con.serverCommands().info("commandstats"));
        Long dbSize = redisTemplate.execute((RedisCallback<Long>) con -> con.serverCommands().dbSize());

        RedisMonitorVO vo = new RedisMonitorVO();
        vo.setInfo(parseInfo(info));
        vo.setDbSize(dbSize);
        vo.setCommandStats(parseCommandStats(commandStats));
        return vo;
    }

    @Override
    public List<RedisKeyDefineVO> getKeyDefines() {
        return RedisKeyDefines.getAll();
    }

    @Override
    public List<RedisSlowLogVO> getSlowLog(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 200) limit = 200;
        final int fetch = limit;
        List<Object> raw = redisTemplate.execute((RedisCallback<List<Object>>) con -> {
            Object native_ = con.getNativeConnection();
            try {
                Object result = invokeSlowlog(native_, "slowlogGet", new Class<?>[]{int.class}, new Object[]{fetch});
                if (result instanceof List<?> list) {
                    @SuppressWarnings("unchecked")
                    List<Object> r = (List<Object>) list;
                    return r;
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("SLOWLOG GET 调用失败: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new IllegalStateException("SLOWLOG GET 调用失败: " + e.getMessage(), e);
            }
            return Collections.emptyList();
        });
        if (raw == null) return Collections.emptyList();
        return parseSlowLog(raw);
    }

    @Override
    public void resetSlowLog() {
        redisTemplate.execute((RedisCallback<Object>) con -> {
            Object native_ = con.getNativeConnection();
            try {
                invokeSlowlog(native_, "slowlogReset", new Class<?>[]{}, new Object[]{});
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("SLOWLOG RESET 调用失败: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new IllegalStateException("SLOWLOG RESET 调用失败: " + e.getMessage(), e);
            }
            return null;
        });
    }

    /**
     * 通用的 SLOWLOG 调用入口，兼容 Lettuce 的三种原生连接类型：
     * <ul>
     *     <li>RedisAsyncCommandsImpl → 需要 .get() 拿结果</li>
     *     <li>RedisCommands（同步）  → 直接返回</li>
     *     <li>StatefulRedisConnection → 需先 .sync() 再调用</li>
     * </ul>
     */
    private static Object invokeSlowlog(Object native_, String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        Object target = native_;

        // 若是 StatefulRedisConnection，取 sync()
        java.lang.reflect.Method syncMethod;
        try {
            syncMethod = native_.getClass().getMethod("sync");
        } catch (NoSuchMethodException ignored) {
            syncMethod = null;
        }
        if (syncMethod != null) {
            target = syncMethod.invoke(native_);
        }

        java.lang.reflect.Method m = target.getClass().getMethod(methodName, argTypes);
        Object result = m.invoke(target, args);

        // Async 变体：返回 RedisFuture / CompletionStage
        if (result instanceof java.util.concurrent.Future<?> future) {
            return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        if (result instanceof java.util.concurrent.CompletionStage<?> stage) {
            return stage.toCompletableFuture().get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        return result;
    }

    private static java.lang.reflect.Method findNoArgMethod(Class<?> cls, String name) {
        try {
            return cls.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    // ============ Protected for unit tests ============

    /** 将 Redis INFO Properties 解析为 RedisInfoVO。 */
    RedisInfoVO parseInfo(Properties info) {
        RedisInfoVO vo = new RedisInfoVO();
        if (info == null) {
            vo.setUsedMemoryPercent(0.0);
            return vo;
        }
        vo.setRedisVersion(info.getProperty("redis_version"));
        vo.setRedisMode(info.getProperty("redis_mode"));
        vo.setUptimeInSeconds(parseLong(info.getProperty("uptime_in_seconds")));
        vo.setConnectedClients(parseInt(info.getProperty("connected_clients")));
        vo.setUsedMemory(parseLong(info.getProperty("used_memory")));
        vo.setMaxMemory(parseLong(info.getProperty("maxmemory")));

        long maxMemory = vo.getMaxMemory() != null ? vo.getMaxMemory() : 0L;
        long usedMemory = vo.getUsedMemory() != null ? vo.getUsedMemory() : 0L;
        if (maxMemory > 0) {
            vo.setUsedMemoryPercent(Math.round(usedMemory * 10000.0 / maxMemory) / 100.0);
        } else {
            vo.setUsedMemoryPercent(0.0);
        }

        // 默认看 db0（0 号库）
        String keyspace = info.getProperty("db0");
        if (keyspace != null) {
            parseKeyspace(keyspace, vo);
        }
        vo.setAofEnabled(info.getProperty("aof_enabled"));
        vo.setRdbLastSaveTime(info.getProperty("rdb_last_save_time"));

        // ==== 扩展指标（stats 节 + clients 节）====
        vo.setTotalCommandsProcessed(parseLong(info.getProperty("total_commands_processed")));
        vo.setTotalConnectionsReceived(parseLong(info.getProperty("total_connections_received")));
        vo.setInstantaneousOpsPerSec(parseLong(info.getProperty("instantaneous_ops_per_sec")));
        vo.setKeyspaceHits(parseLong(info.getProperty("keyspace_hits")));
        vo.setKeyspaceMisses(parseLong(info.getProperty("keyspace_misses")));
        long hits = vo.getKeyspaceHits() != null ? vo.getKeyspaceHits() : 0L;
        long misses = vo.getKeyspaceMisses() != null ? vo.getKeyspaceMisses() : 0L;
        long total = hits + misses;
        if (total > 0) {
            vo.setHitRate(Math.round(hits * 10000.0 / total) / 100.0);
        } else {
            vo.setHitRate(0.0);
        }
        vo.setPubsubChannels(parseInt(info.getProperty("pubsub_channels")));
        vo.setPubsubPatterns(parseInt(info.getProperty("pubsub_patterns")));
        return vo;
    }

    /** 解析 Keyspace 行："keys=1523,expires=800,avg_ttl=1200000"。 */
    void parseKeyspace(String keyspace, RedisInfoVO vo) {
        for (String part : keyspace.split(",")) {
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            switch (kv[0].trim()) {
                case "keys" -> vo.setTotalKeys(parseLong(kv[1]));
                case "expires" -> vo.setExpiresKeys(parseLong(kv[1]));
                case "avg_ttl" -> vo.setAvgTtl(parseLong(kv[1]));
                default -> {}
            }
        }
    }

    /**
     * 解析 INFO commandstats 返回值。
     * <p>每行形如 {@code cmdstat_get=calls=50000,usec=120000,usec_per_call=2.40}，按 calls 降序排列。</p>
     */
    List<RedisCommandStatVO> parseCommandStats(Properties stats) {
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }
        List<RedisCommandStatVO> result = new ArrayList<>();
        for (var entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            if (!key.startsWith("cmdstat_")) {
                continue;
            }
            String cmd = key.substring("cmdstat_".length());
            RedisCommandStatVO stat = new RedisCommandStatVO();
            stat.setName(cmd);
            for (String part : entry.getValue().toString().split(",")) {
                String[] kv = part.split("=");
                if (kv.length != 2) continue;
                switch (kv[0].trim()) {
                    case "calls" -> stat.setCalls(parseLong(kv[1]));
                    case "usec" -> stat.setUsec(parseLong(kv[1]));
                    case "usec_per_call" -> stat.setUsecPerCall(parseLongRoundDown(kv[1]));
                    default -> {}
                }
            }
            result.add(stat);
        }
        result.sort(Comparator.comparingLong((RedisCommandStatVO s) -> s.getCalls() != null ? s.getCalls() : 0L).reversed());
        return result;
    }

    private static Long parseLong(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 兼容 "2.40" 这种小数串，向下取整为 Long。 */
    private static Long parseLongRoundDown(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            int dot = s.indexOf('.');
            String intPart = dot >= 0 ? s.substring(0, dot) : s;
            return Long.parseLong(intPart.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        Long v = parseLong(s);
        return v == null ? null : v.intValue();
    }

    /**
     * 解析 SLOWLOG GET 返回值。
     * <p>Redis 协议返回格式（数组）：</p>
     * <pre>
     *   1) (integer) id
     *   2) (integer) timestamp
     *   3) (integer) duration (microseconds)
     *   4) (array)   command args
     *   5) (bulk)    client addr (4.0+)
     *   6) (bulk)    client name (4.0+)
     * </pre>
     *
     * <p>按耗时降序排列。</p>
     */
    @SuppressWarnings("unchecked")
    List<RedisSlowLogVO> parseSlowLog(List<Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<RedisSlowLogVO> result = new ArrayList<>();
        for (Object entry : raw) {
            if (!(entry instanceof List)) continue;
            List<Object> row = (List<Object>) entry;
            if (row.size() < 4) continue;
            RedisSlowLogVO vo = new RedisSlowLogVO();
            vo.setId(toLong(row.get(0)));
            vo.setTimestamp(toLong(row.get(1)));
            vo.setDurationMicros(toLong(row.get(2)));

            Object argsObj = row.get(3);
            List<String> args = new ArrayList<>();
            if (argsObj instanceof List<?> list) {
                for (Object a : list) {
                    args.add(toStr(a));
                }
            }
            vo.setArgs(args);
            if (row.size() >= 5) vo.setClientAddress(toStr(row.get(4)));
            if (row.size() >= 6) vo.setClientName(toStr(row.get(5)));
            result.add(vo);
        }
        result.sort(Comparator
                .comparingLong((RedisSlowLogVO v) -> v.getDurationMicros() != null ? v.getDurationMicros() : 0L)
                .reversed());
        return result;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        if (o instanceof byte[] b) return parseLong(new String(b, StandardCharsets.UTF_8));
        return parseLong(o.toString());
    }

    private static String toStr(Object o) {
        if (o == null) return null;
        if (o instanceof byte[] b) return new String(b, StandardCharsets.UTF_8);
        return o.toString();
    }
}
