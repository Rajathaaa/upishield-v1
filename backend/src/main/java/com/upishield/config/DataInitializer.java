package com.upishield.config;

import com.upishield.entity.User;
import com.upishield.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(UserRepository users) {
        return args -> {
            if (users.count() == 0) {
                users.save(new User("Demo User", "demo@upishield.local"));
                users.save(new User("Test User", "test@upishield.local"));
            }
        };
    }
}
