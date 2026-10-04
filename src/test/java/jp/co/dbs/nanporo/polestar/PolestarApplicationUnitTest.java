package jp.co.dbs.nanporo.polestar;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

class PolestarApplicationUnitTest {

	@Test
	void mainDelegatesToSpringApplication() {
		String[] args = { "--spring.main.web-application-type=none" };
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			PolestarApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(PolestarApplication.class, args));
		}
	}

	@Test
	void applicationCanBeInstantiated() {
		new PolestarApplication();
	}
}
