package com.pot.user.service;

import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.pot.user.service.mapper")
@ComponentScan(basePackages = {"com.pot.user", "com.pot.common"})
@EnableLeafServer
public class ServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

}
