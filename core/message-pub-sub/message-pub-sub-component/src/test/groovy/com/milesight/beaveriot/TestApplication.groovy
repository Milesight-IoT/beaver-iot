package com.milesight.beaveriot


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class TestApplication {

    static void main(String[] args) {
        new SpringApplicationBuilder(TestApplication.class)
                .run(args)
    }

}
