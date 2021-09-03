package com.carl.geek.service.c;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * @author carl.che
 */
@SpringBootApplication(scanBasePackages = {"com.carl.geek.service.c"},
        exclude = MongoAutoConfiguration.class)
public class ServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}
