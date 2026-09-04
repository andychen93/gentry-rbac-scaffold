package com.gentry.rbac.dict.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * 字典缓存失效广播的订阅端。
 *
 * <p>Spring Boot 只自动配置 RedisTemplate，不会创建
 * {@link RedisMessageListenerContainer}，所以这里显式注册一个，
 * 订阅 {@link DictCacheManager#INVALIDATE_CHANNEL}，把消息交给
 * {@code onInvalidateMessage} 清理本地 L1。</p>
 */
@Configuration
public class DictCacheConfig {

    @Bean
    public MessageListenerAdapter dictInvalidateListener(DictCacheManager cacheManager) {
        // 反射调用 onInvalidateMessage(String)，容器会把消息体反序列化成 String 传入
        return new MessageListenerAdapter(cacheManager, "onInvalidateMessage");
    }

    @Bean
    public RedisMessageListenerContainer dictCacheListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter dictInvalidateListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(dictInvalidateListener,
                new PatternTopic(DictCacheManager.INVALIDATE_CHANNEL));
        return container;
    }
}
