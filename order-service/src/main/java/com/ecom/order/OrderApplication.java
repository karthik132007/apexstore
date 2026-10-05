package com.ecom.order;
import com.ecom.common.CommonConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @Import(CommonConfiguration.class) @EnableScheduling
public class OrderApplication { public static void main(String[] args) { SpringApplication.run(OrderApplication.class,args); } }
