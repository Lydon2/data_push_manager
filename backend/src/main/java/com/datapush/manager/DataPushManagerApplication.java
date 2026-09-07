package com.datapush.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据对接平台启动类
 * 
 * @author Data Push Team
 * @since 2024-11-20
 */
@SpringBootApplication
@MapperScan("com.datapush.manager.mapper")
@EnableScheduling
public class DataPushManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataPushManagerApplication.class, args);
        System.out.println("========================================");
        System.out.println("数据对接平台启动成功！");
        System.out.println("访问地址: http://localhost:8080/api");
        System.out.println("========================================");
    }
}
