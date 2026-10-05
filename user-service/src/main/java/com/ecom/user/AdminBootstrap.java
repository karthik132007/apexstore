package com.ecom.user;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class AdminBootstrap {
    @Bean ApplicationRunner admin(UserService service,@Value("${app.admin-email:}") String email,@Value("${app.admin-password:}") String password) { return args -> service.bootstrapAdmin(email,password); }
}
