package com.precision;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.precision.**.mapper")
@EnableScheduling
public class PrecisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrecisionApplication.class, args);
    }
}
