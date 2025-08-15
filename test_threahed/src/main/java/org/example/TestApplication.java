package org.example;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用程序入口类
 */
@SpringBootApplication              // 标注为 Spring Boot 应用
@EnableAsync                        // 启用异步支持（@Async）
@EnableScheduling                   // 启用定时任务支持（@Scheduled）
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

}
