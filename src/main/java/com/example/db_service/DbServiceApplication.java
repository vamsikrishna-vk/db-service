package com.example.db_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DbServiceApplication {
	@Autowired
	DbFileService dbFileService;

	public static void main(String[] args) {
		SpringApplication.run(DbServiceApplication.class, args);
	}

}
