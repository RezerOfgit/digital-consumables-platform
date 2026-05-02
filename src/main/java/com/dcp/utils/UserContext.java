package com.dcp.utils;

/**
 * 基于 ThreadLocal 的用户上下文工具类：在同一线程内传递当前登录用户名
 * @author Re-zero
 * @version 1.0
 */
public class UserContext {

    private static final ThreadLocal<String> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(String username) {
        USER_THREAD_LOCAL.set(username);
    }

    public static String getUser() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal，防止 Tomcat 线程池复用导致内存泄漏
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}