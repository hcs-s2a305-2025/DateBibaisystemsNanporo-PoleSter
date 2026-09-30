package jp.co.dbs.nanporo.polestar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync 
@SpringBootApplication
public class PolestarApplication {

	public static void main(String[] args) {
		SpringApplication.run(PolestarApplication.class, args);
	}

}
