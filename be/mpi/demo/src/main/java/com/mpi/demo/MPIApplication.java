package com.mpi.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling 
public class MPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(MPIApplication.class, args);
	}

}
