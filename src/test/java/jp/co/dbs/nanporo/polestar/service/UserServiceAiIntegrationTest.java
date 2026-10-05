package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sun.net.httpserver.HttpServer;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;

@Tag("integration")
class UserServiceAiIntegrationTest {

	@Test
	@DisplayName("AI生成APIへJSONを送信しレスポンスを受信する")
	void sendsRequestToAiApi() throws IOException {
		AtomicReference<String> requestBody = new AtomicReference<>();
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/generate", exchange -> {
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = "{\"response\":\"提案結果\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			try (var responseBody = exchange.getResponseBody()) {
				responseBody.write(response);
			}
		});
		server.start();

		try {
			UserService service = new UserService();
			ReflectionTestUtils.setField(service, "aiApiUrl",
					"http://127.0.0.1:" + server.getAddress().getPort() + "/api/generate");
			StoreRepository storeRepository = mock(StoreRepository.class);
			when(storeRepository.getAll()).thenReturn(java.util.List.of());
			ReflectionTestUtils.setField(service, "storeRepository", storeRepository);
			UserEntity user = new UserEntity();
			user.setBirthday(Date.valueOf("1990-01-04"));
			user.setGender("未設定");

			assertThat(service.getAi(user, "質問")).isEqualTo("提案結果");
			assertThat(requestBody.get()).contains("\"model\":\"gemma2\"", "\"stream\":false", "質問");
		} finally {
			server.stop(0);
		}
	}
}