package com.gentry;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.gentry.**.mapper")
@EnableScheduling
public class GentryApplication {

    public static void main(String[] args) {
        SpringApplication.run(GentryApplication.class, args);
    }
}
