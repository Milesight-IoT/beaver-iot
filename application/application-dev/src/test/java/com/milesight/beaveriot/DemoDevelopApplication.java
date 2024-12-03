package com.milesight.beaveriot;

import com.milesight.beaveriot.data.jpa.BaseJpaRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * @author leon
 */
@EnableAsync
//@EnableJpaAuditing
//@EnableJpaRepositories(repositoryBaseClass = BaseJpaRepositoryImpl.class )
@SpringBootApplication
public class DemoDevelopApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.milesight.beaveriot.DevelopApplication.class, args);
    }
}
