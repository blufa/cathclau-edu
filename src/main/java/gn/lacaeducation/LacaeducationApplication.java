package gn.lacaeducation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LacaeducationApplication {

	public static void main(String[] args) {
		SpringApplication.run(LacaeducationApplication.class, args);
	}

}
