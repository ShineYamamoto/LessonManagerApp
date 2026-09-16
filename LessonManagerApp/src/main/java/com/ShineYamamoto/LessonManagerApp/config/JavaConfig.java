package com.ShineYamamoto.LessonManagerApp.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JavaConfig {
	
	@Bean
	ModelMapper modelMaper() {
		return new ModelMapper();
	}
}
