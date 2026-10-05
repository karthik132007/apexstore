package com.ecom.invoice;
import com.ecom.common.CommonConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication @Import(CommonConfiguration.class) 
public class InvoiceApplication { public static void main(String[] args) { SpringApplication.run(InvoiceApplication.class,args); } }
