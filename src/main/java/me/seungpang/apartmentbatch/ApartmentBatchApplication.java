package me.seungpang.apartmentbatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableBatchProcessing
@SpringBootApplication
public class ApartmentBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApartmentBatchApplication.class, args);
	}

}
