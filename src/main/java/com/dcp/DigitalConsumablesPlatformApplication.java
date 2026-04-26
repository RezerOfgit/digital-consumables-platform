package com.dcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class DigitalConsumablesPlatformApplication {

    public static void main(String[] args) {

        // 【临时加的一行代码】：打印出 123456 的正确密文
//        System.out.println("123456 的密文是: " + new BCryptPasswordEncoder().encode("123456"));

        SpringApplication.run(DigitalConsumablesPlatformApplication.class, args);
    }

}
