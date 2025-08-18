package com.pot.member.service;

import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.pot.member.service.mapper")
@ComponentScan(basePackages = {"com.pot.member", "com.pot.common"})
@EnableLeafServer
public class MemberServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(MemberServiceApplication.class, args);
    }

}
