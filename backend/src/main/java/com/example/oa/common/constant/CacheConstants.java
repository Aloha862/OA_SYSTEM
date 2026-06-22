package com.example.oa.common.constant;

import java.time.Duration;

public final class CacheConstants {

    public static final String DICT_TYPE_PREFIX = "dict:type:";
    public static final String DEPARTMENT_TREE = "department:tree";
    public static final String NEWS_DETAIL_PREFIX = "news:detail:";
    public static final String NEWS_PUBLISHED_LIST = "news:list:published";
    public static final String USER_PERMISSION_PREFIX = "user:permission:";
    public static final String NOTIFICATION_UNREAD_PREFIX = "notification:unread:";
    public static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    public static final String WS_TICKET_PREFIX = "ws:ticket:";

    public static final Duration DICT_TTL = Duration.ofHours(6);
    public static final Duration DEPARTMENT_TTL = Duration.ofMinutes(30);
    public static final Duration NEWS_DETAIL_TTL = Duration.ofMinutes(10);
    public static final Duration NOTIFICATION_UNREAD_TTL = Duration.ofMinutes(2);

    private CacheConstants() {
    }
}
