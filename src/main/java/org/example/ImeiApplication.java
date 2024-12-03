package org.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "org.example")
public class ImeiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImeiApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(ApplicationContext context) {
        return args -> {
            ImeiProcessor imeiProcessor = context.getBean(ImeiProcessor.class);
            imeiProcessor.processImeis();
        };
    }
}
