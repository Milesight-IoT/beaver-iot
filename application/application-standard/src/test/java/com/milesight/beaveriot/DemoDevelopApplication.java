package com.milesight.beaveriot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author leon
 */
//@EnableAsync
//@EnableJpaAuditing
//@EnableJpaRepositories(repositoryBaseClass = BaseJpaRepositoryImpl.class )
//@SpringBootApplication
public class DemoDevelopApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "test");
        SpringApplication.run(StandardApplication.class, args);
    }
}
