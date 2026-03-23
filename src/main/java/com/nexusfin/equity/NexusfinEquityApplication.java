package com.nexusfin.equity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@MapperScan("com.nexusfin.equity.repository")
@ConfigurationPropertiesScan("com.nexusfin.equity.config")
public class NexusfinEquityApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusfinEquityApplication.class, args);
    }
}
