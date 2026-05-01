package com.dcp.utils;

/**
 * @author Re-zero
 * @version 1.0
 * 基于 ThreadLocal 的用户上下文工具类
 * 作用：在同一个 HTTP 请求的线程内，无缝传递当前登录的用户名
 */
public class UserContext {
    // 声明一个静态的 ThreadLocal 变量
    private static final ThreadLocal<String> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void setUser(String username) {
        USER_THREAD_LOCAL.set(username);
    }

    public static String getUser() {
        return USER_THREAD_LOCAL.get();
    }

    public static void clear() {
        USER_THREAD_LOCAL.remove(); // 【面试必考】：防止内存泄漏必须调用！
    }
}