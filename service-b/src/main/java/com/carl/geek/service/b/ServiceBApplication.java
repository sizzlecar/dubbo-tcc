package com.carl.geek.service.b;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * @author carl.che
 */
@SpringBootApplication(scanBasePackages = {"com.carl.geek.service.b"},
        exclude = MongoAutoConfiguration.class)
public class ServiceBApplication {


    public static void main(String[] args) {
        SpringApplication.run(ServiceBApplication.class, args);
    }
}
