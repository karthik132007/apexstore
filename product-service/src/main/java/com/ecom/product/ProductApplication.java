package com.ecom.product;
import com.ecom.common.CommonConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication @Import(CommonConfiguration.class) 
public class ProductApplication { public static void main(String[] args) { SpringApplication.run(ProductApplication.class,args); } }
