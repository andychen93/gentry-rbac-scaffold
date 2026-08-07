package com.precision;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.precision.**.mapper")
public class PrecisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrecisionApplication.class, args);
    }
}
