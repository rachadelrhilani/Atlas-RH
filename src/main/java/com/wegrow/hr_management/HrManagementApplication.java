package com.wegrow.hr_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.wegrow")
public class HrManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(HrManagementApplication.class, args);
	}

}
