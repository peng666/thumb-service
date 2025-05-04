package com.peng.thumb;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.peng.thumb.mapper")
public class ThumbServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThumbServiceApplication.class, args);
	}

}
