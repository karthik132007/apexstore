package com.ecom.user;
import com.ecom.common.CommonConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication @Import(CommonConfiguration.class) 
public class UserApplication { public static void main(String[] args) { SpringApplication.run(UserApplication.class,args); } }
