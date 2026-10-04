package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;

@SpringBootTest(properties = "spring.sql.init.mode=never")
@Transactional
@Tag("integration")
@EnabledIf("isPostgresSchemaAvailable")
class JpaRepositoriesIntegrationTest {

	@Autowired
	private NamedParameterJdbcTemplate jdbc;

	@Autowired
	private OrderDetailRepository orderDetailRepository;

	@Autowired
	private OrderTRepository orderTRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private TransactionDetailRepository transactionDetailRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private StoreRepository storeRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private InnerdisplayRepository innerdisplayRepository;

	@Autowired
	private OuterdisplayRepository outerdisplayRepository;

	static boolean isPostgresSchemaAvailable() {
		Properties properties = new Properties();
		try (InputStream input = JpaRepositoriesIntegrationTest.class.getResourceAsStream("/application.properties")) {
			if (input == null) {
				return false;
			}
			properties.load(input);
			try (var connection = DriverManager.getConnection(
					properties.getProperty("spring.datasource.url"),
					properties.getProperty("spring.datasource.username"),
					properties.getProperty("spring.datasource.password"));
				Statement statement = connection.createStatement()) {
				statement.executeQuery("SELECT 1 FROM user_m, goods_m, order_t, order_detail_t, transaction_t, transaction_detail_t WHERE 1 = 0");
				return true;
			}
		} catch (Exception exception) {
			return false;
		}
	}

	@Test
	@DisplayName("JDBCとJPAの各Repositoryが同じ注文・商品・取引を読み書きする")
	void repositoriesSharePersistedRows() {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		String mail = "it-" + suffix + "@example.com";
		String goodsId = "T" + suffix;
		String orderNumber = "M" + suffix;
		LocalDateTime now = LocalDateTime.now();
		insertUser(mail);
		insertGoods(goodsId);

		OrderEntity order = new OrderEntity();
		order.setOrderNumber(orderNumber);
		order.setGetTime(Timestamp.valueOf(now));
		order.setMail(mail);
		order.setRegisterTime(Timestamp.valueOf(now.minusMinutes(1)));
		order.setSumMoney(850);
		order.setMemo("integration");
		order.setStatus("受付");
		order = orderTRepository.saveAndFlush(order);
		Integer savedOrderId = order.getOrderId();

		OrderDetailEntity orderDetail = new OrderDetailEntity();
		orderDetail.setOrderId(order.getOrderId());
		orderDetail.setOrderCount(1);
		orderDetail.setGoodsId(goodsId);
		orderDetail.setCount(1);
		orderDetail.setPlusZangiCount(0);
		orderDetailRepository.saveAndFlush(orderDetail);

		assertThat(orderDetailRepository.findByOrderId(order.getOrderId())).hasSize(1);
		assertThat(orderTRepository.findTodayOrderByNumber(orderNumber, LocalDate.now().atStartOfDay(),
				LocalDate.now().atTime(23, 59, 59, 999999999))).contains(order);
		assertThat(userRepository.findByMail(mail)).containsEntry("mail", mail);
		assertThat(storeRepository.getGoodsById(goodsId)).containsEntry("goods_name", "integration-item");
		assertThat(orderRepository.getActiveOrdersByMail(mail)).hasSize(1);
		assertThat(orderRepository.getOrderHistoryByMail(mail)).hasSize(1);
		assertThat(orderRepository.getOrderDetailsByOrderId(order.getOrderId())).hasSize(1);
		assertThat(innerdisplayRepository.getKitchenOrders()).anySatisfy(row ->
				assertThat(row).containsEntry("order_id", savedOrderId));
		assertThat(outerdisplayRepository.getAllActiveOrders()).anySatisfy(row ->
				assertThat(row).containsEntry("order_id", savedOrderId));

		TransactionEntity transaction = new TransactionEntity();
		transaction.setOrderId(order.getOrderId());
		transaction.setMail(mail);
		transaction.setTransactionDate(now);
		transaction.setReceivedMoney(1000);
		transaction.setChangeMoney(150);
		transaction.setSumMoney(850);
		transaction = transactionRepository.saveAndFlush(transaction);

		TransactionDetailEntity transactionDetail = new TransactionDetailEntity();
		transactionDetail.setTransactionId(transaction.getTransactionId());
		transactionDetail.setReservationCount(1);
		transactionDetail.setGoodsName("integration-item");
		transactionDetail.setCount(1);
		transactionDetail.setPlusZangiCount(0);
		transactionDetail.setPrice(850);
		transactionDetailRepository.saveAndFlush(transactionDetail);

		assertThat(transactionRepository.findById(transaction.getTransactionId())).isPresent();
		assertThat(transactionDetailRepository.findById(
				new jp.co.dbs.nanporo.polestar.entity.TransactionDetailKey(transaction.getTransactionId(), 1)))
				.isPresent();
	}

	private void insertUser(String mail) {
		MapSqlParameterSource params = new MapSqlParameterSource()
				.addValue("mail", mail)
				.addValue("name", "Integration")
				.addValue("password", "not-used")
				.addValue("role", "顧客")
				.addValue("rank", "一般")
				.addValue("gender", "未")
				.addValue("birthday", Date.valueOf("2000-01-01"));
		jdbc.update("INSERT INTO user_m (mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete) "
				+ "VALUES (:mail, :name, :password, :role, :rank, :gender, :birthday, 0, false, 0, 0)", params);
	}

	private void insertGoods(String goodsId) {
		jdbc.update("INSERT INTO goods_m (goods_id, goods_name, price, calorie, allergy, zangi_count, sold_out, detail, watch_rank) "
				+ "VALUES (:goodsId, 'integration-item', 850, 500, 'なし', 5, false, 'test', '一般')",
				new MapSqlParameterSource("goodsId", goodsId));
	}
}