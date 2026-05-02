package com.dcp.controller;

import com.dcp.dto.R;
import com.dcp.exception.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试接口，用来验证 Spring MVC 的路由功能和全局异常拦截是否生效
 * @author Re-zero
 * @version 1.0
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 正常请求测试
     * @return
     */
    @GetMapping("/hello")
    public R<String> sayHello() {
        return R.ok("恭喜你，你的第一个 Web 接口调通了！");
    }

    /**
     * 模拟业务异常，验证 GlobalExceptionHandler 拦截
     * @return
     */
    @GetMapping("/error")
    public R<String> testError() {
        throw new BusinessException(4001, "模拟库存不足报错！");
    }
}
