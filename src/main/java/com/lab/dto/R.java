package com.lab.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * @author Re-zero
 * @version 1.0
 * 统一响应结果封装类
 */
@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int code;
    //对于 R.java，你可以把 Integer code 改成 int code，但一般不加 final，
    // 因为在某些极端场景下框架在反序列化 JSON 时可能会需要调用无参构造再 set 值。
    private String message;
    private T data;

    public static <T> R<T> ok() { return ok(null); }
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>(); r.setCode(200); r.setMessage("操作成功"); r.setData(data); return r;
    }
    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>(); r.setCode(200); r.setMessage(message); r.setData(data); return r;
    }
    public static <T> R<T> fail(String message) {
        R<T> r = new R<>(); r.setCode(500); r.setMessage(message); return r;
    }
    public static <T> R<T> fail(int code, String message) {
        R<T> r = new R<>(); r.setCode(code); r.setMessage(message); return r;
    }
}
