package com.phoenix.business;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
@SpringBootApplication(exclude=UserDetailsServiceAutoConfiguration.class) public class PhoenixBusinessApplication { public static void main(String[] args){SpringApplication.run(PhoenixBusinessApplication.class,args);} }
