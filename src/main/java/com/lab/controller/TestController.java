package com.lab.controller;

import com.lab.dto.R;
import com.lab.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Re-zero
 * @version 1.0
 * 测试接口，用来验证 Spring MVC 的路由功能和全局异常拦截是否生效
 */
@RestController // 告诉 Spring：这是一个接收 Web 请求的前台接待员，并且返回 JSON 数据
@RequestMapping("/test") // 给这个接待员分配一个工位路径
public class TestController {

    // 测试 1：正常的成功请求
    @GetMapping("/hello")
    public R<String> sayHello() {
        // 我们不直接 return "Hello"，而是把它装进我们的 R 快递盒里！
        return R.ok("恭喜你，你的第一个 Web 接口调通了！");
    }

    // 测试 2：模拟业务报错，测试全局异常拦截器
    @GetMapping("/error")
    public R<String> testError() {
        // 我们直接抛出一个业务异常，看看 GlobalExceptionHandler 能不能拦住它并包装成 JSON
        throw new BusinessException(4001, "模拟库存不足报错！");
    }
}
