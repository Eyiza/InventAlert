package com.inventalert.notificationService.repository;

import com.inventalert.notificationService.model.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class RedisNotificationRepository {

    private final StringRedisTemplate redisTemplate;

    @Value("${notification.ttl-days:90}")
    private int ttlDays;

    private static final String NOTIFICATION_KEY    = "notification:%s:%s";
    private static final String USER_SORTED_SET_KEY = "user-notifications:%s:%s";
    private static final String UNREAD_COUNT_KEY    = "unread-count:%s:%s";
    private static final String PROCESSED_EVENT_KEY = "processed-event:%s";

    public boolean setEventProcessedIfAbsent(String eventId) {
        String key = PROCESSED_EVENT_KEY.formatted(eventId);
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(ttlDays));
        return Boolean.TRUE.equals(result);
    }

    public void saveHash(Notification notification) {
        String key = NOTIFICATION_KEY.formatted(
                notification.getCompanyId(), notification.getNotificationId());

        Map<String, String> hash = new HashMap<>();
        hash.put("notificationId", notification.getNotificationId());
        hash.put("companyId",      notification.getCompanyId());
        hash.put("userId",         notification.getUserId());
        hash.put("type",           notification.getType().name());
        hash.put("message",        notification.getMessage());
        hash.put("referenceId",    notification.getReferenceId() != null ? notification.getReferenceId() : "");
        hash.put("isRead",         "0");
        hash.put("createdAt",      notification.getCreatedAt().toString());

        redisTemplate.<String, String>opsForHash().putAll(key, hash);
        redisTemplate.expire(key, Duration.ofDays(ttlDays));
    }

    public void addToUserSortedSet(String companyId, String userId,
                                   String notificationId, double score) {
        String key = USER_SORTED_SET_KEY.formatted(companyId, userId);
        redisTemplate.opsForZSet().add(key, notificationId, score);
    }

    public void incrementUnreadCount(String companyId, String userId) {
        redisTemplate.opsForValue()
                .increment(UNREAD_COUNT_KEY.formatted(companyId, userId));
    }

    public Set<String> getNotificationIds(String companyId, String userId, long start, long stop) {
        String key = USER_SORTED_SET_KEY.formatted(companyId, userId);
        return redisTemplate.opsForZSet().reverseRange(key, start, stop);
    }

    public Map<String, String> getHash(String companyId, String notificationId) {
        String key = NOTIFICATION_KEY.formatted(companyId, notificationId);
        return redisTemplate.<String, String>opsForHash().entries(key);
    }

    public void markHashAsRead(String companyId, String notificationId) {
        String key = NOTIFICATION_KEY.formatted(companyId, notificationId);
        redisTemplate.opsForHash().put(key, "isRead", "1");
    }

    public void decrementUnreadCount(String companyId, String userId) {
        redisTemplate.opsForValue()
                .increment(UNREAD_COUNT_KEY.formatted(companyId, userId), -1);
    }

    public long getUnreadCount(String companyId, String userId) {
        String val = redisTemplate.opsForValue()
                .get(UNREAD_COUNT_KEY.formatted(companyId, userId));
        return val != null ? Long.parseLong(val) : 0L;
    }
}
