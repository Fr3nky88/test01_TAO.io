package it.tao.io.test01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Test01Application {

	public static void main(String[] args) {
		SpringApplication.run(Test01Application.class, args);
	}

}
