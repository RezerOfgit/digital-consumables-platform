package com.dcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 数字化耗材管控平台启动类
 * @author Re-zero
 * @version 1.0
 */
@EnableAsync // 启用 Spring 异步任务支持，AI 风控依赖此配置
@SpringBootApplication
public class DigitalConsumablesPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalConsumablesPlatformApplication.class, args);
    }
}
