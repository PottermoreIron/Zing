package com.pot.member.facade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MemberFacadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemberFacadeApplication.class, args);
    }

}
