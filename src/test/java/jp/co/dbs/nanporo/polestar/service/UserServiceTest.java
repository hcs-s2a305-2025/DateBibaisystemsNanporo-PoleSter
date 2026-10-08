package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.UserRepository.SalesFlashDto;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;

@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@Transactional
class UserServiceTest {
	
	@Autowired
	private UserService service;
	
	@Autowired 
    private NamedParameterJdbcTemplate jdbc;

	@Test
	@DisplayName("メールアドレスを入力しユーザ情報を返す(ログイン)")
	void testGetUserTrue() {
		// 引数の定義
		UserData data = new UserData();
		
		// 渡したい引数を設定
		data.setMail("murokishoon@example.com");
		
		// テスト対象メソッドを実行する
		UserEntity response = service.getUser(data);
		
		// 実行結果の確認
		assertThat(response.getMail()).isEqualTo("murokishoon@example.com");
	}
	
	@Test
	@DisplayName("存在しないメールアドレスを入力しエラーが発生する")
	void testGetUserFalse() {
		// 引数の定義
		UserData data = new UserData();
		
		// 渡したい引数を設定
		data.setMail("a@example.com");
		
		// テスト対象メソッドを実行し、実行結果の確認
		assertThatThrownBy(() -> service.getUser(data))
		.isInstanceOf(UsernameNotFoundException.class)
		.hasMessage("ユーザが見つかりません： a@example.com");
	}

	@Test
	@DisplayName("従業員一覧をページネーション情報と共に返す")
	void testGetStaffList() {
		// 渡したい引数を設定
		Pageable pageable = PageRequest.of(0, 10);
		String sort = "asc";
		
		// テスト対象メソッドを実行する
		UserGetResponse response = service.getStaffList(pageable, sort);
		
		// 実行結果の確認
		assertThat(response.getUsers().get(0).getMail()).isEqualTo("hiroshimaatsushi@example.com");
		assertThat(response.getUsers().get(1).getMail()).isEqualTo("koserayuuki@example.com");
	}

	@Test
	@DisplayName("顧客一覧をページネーション情報と共に返す")
	void testGetCustomerList() {
		// 渡したい引数を設定
		Pageable pageable = PageRequest.of(0, 10);
		String sort = "asc";
		
		// テスト対象メソッドを実行する
		UserGetResponse response = service.getCustomerList(pageable, sort);
		
		// 実行結果の確認
		assertThat(response.getUsers().get(0).getMail()).isEqualTo("isidaharu@example.com");
		assertThat(response.getUsers().get(1).getMail()).isEqualTo("murokishoon@example.com");
	}

	@Test
	@DisplayName("メールアドレスを入力しユーザ情報を返す（ログイン以外）")
	void testFindByMail() {
		// 渡したい引数を設定
		String mail = "koserayuuki@example.com";
		
		// テスト対象メソッドを実行する
		UserEntity response = service.findByMail(mail);
		
		// 実行結果の確認
		assertThat(response.getMail()).isEqualTo("koserayuuki@example.com");
		assertThat(response.getName()).isEqualTo("小瀬良優希");
		assertThat(response.getRole()).isEqualTo("店員");
		assertThat(response.getMemberRank()).isEqualTo("一般");
		assertThat(response.getAlive()).isEqualTo(false);
		assertThat(response.getPoint()).isEqualTo(0);
		assertThat(response.getPointCardComplete()).isEqualTo(0);
		assertThat(response.getGender()).isEqualTo("女");
		assertThat(response.getBirthday()).isEqualTo("1990-01-04");
	}

	@Test
	@DisplayName("メールアドレスを入力し通知一覧を返す")
	void testGetNotificationsByMail() {		
		// 渡したい引数を設定
		String mail = "isidaharu@example.com";
		
		// テスト対象メソッドを実行する
		List<Map<String, Object>> response = service.getNotificationsByMail(mail);
		
		// 実行結果の確認
		assertThat(response).hasSize(1);
		assertThat(response.get(0).get("content")).isEqualTo("ゴールド会員限定裏メニューをご利用いただけます。");
	}

	@Test
	@DisplayName("メールアドレスを入力しユーザ情報を更新する")
	void testUpdateStaff() {
		// 渡したい引数を設定
		String mail = "hiroshimaatsushi@example.com";
		String name = "あ";
		String role = "店員";
		boolean alive = true;
		
		// テスト対象メソッドを実行する
		service.updateStaff(mail, name, role, alive);
		
		// 更新後データを取得
		UserData data = new UserData();
		data.setMail(mail);
		UserEntity update = service.getUser(data);
		
		// 実行結果の確認
		assertThat(update.getName()).isEqualTo(name);
		assertThat(update.getRole()).isEqualTo(role);
		assertThat(update.getAlive()).isEqualTo(alive);
	}

	@Test
	@DisplayName("メールアドレスを入力しユーザを削除する")
	void testDeleteUser() {
		// 渡したい引数を設定
		String mail = "koserayuuki@example.com";
		
		// テスト対象メソッドを実行する
		service.deleteUser(mail);
		
		// テスト対象メソッドを実行し、実行結果の確認
		UserData data = new UserData();
		data.setMail(mail);
		
		assertThatThrownBy(() -> service.getUser(data))
		.isInstanceOf(UsernameNotFoundException.class)
		.hasMessage("ユーザが見つかりません： " + mail);
	}

	@Test
	@DisplayName("店長・店員の新規登録を行う")
	void testRegisterStaff() {
		// 渡したい引数を設定
		String mail = "aaa@example.com";
		String name = "あああ";
		String role = "店員";
		
		// テスト対象メソッドを実行する
		service.registerStaff(mail, name, role);
		
		// 実行結果の確認
		UserData data = new UserData();
		data.setMail(mail);
		UserEntity response = service.getUser(data);
		
		assertThat(response.getMail()).isEqualTo("aaa@example.com");
		assertThat(response.getName()).isEqualTo("あああ");
		assertThat(response.getRole()).isEqualTo("店員");
	}

	@Test
	@DisplayName("メールアドレスを入力し利用停止状態にする")
	void testStopUser() {
		// 渡したい引数を設定
		String mail = "hiroshimaatsushi@example.com";
		
		// テスト対象メソッドを実行する
		service.stopUser(mail);
		
		// 実行結果の確認
		UserData data = new UserData();
		data.setMail(mail);
		UserEntity response = service.getUser(data);
		
		assertThat(response.getAlive()).isEqualTo(true);
	}

	@Test
	@DisplayName("メールアドレスを入力し利用停止状態を解除する")
	void testResumeUser() {
		// 渡したい引数を設定
		String mail = "hiroshimaatsushi@example.com";
		
		// テスト対象メソッドを実行する
		service.resumeUser(mail);
		
		// 実行結果の確認
		UserData data = new UserData();
		data.setMail(mail);
		UserEntity response = service.getUser(data);
		
		assertThat(response.getAlive()).isEqualTo(false);
	}

	@Test
	@DisplayName("当日の予約・注文件数を返す")
	void testCountOrder() {
		LocalDate today = LocalDate.now();
		Integer expectedCount = jdbc.queryForObject(
				"SELECT COUNT(*) FROM order_t WHERE CAST(get_time AS DATE) = :today AND status != 'キャンセル'",
				Map.of("today", today), Integer.class);

		// テスト対象メソッドを実行する
		int su = service.countOrder();
		
		// 実行結果の確認
		assertThat(su).isEqualTo(expectedCount);
	}

	@Test
	@DisplayName("当日付けで「臨時休業」を登録する")
	void testInsertClose() {
		// 今日の日付を取得
		LocalDate today = LocalDate.now();
		
		// テスト対象メソッドを実行する
		service.insertClose();
		
		// 実行結果の確認
		Map<String, Object> params = Map.of("today", today);
		
		String sql = "SELECT close_type FROM close_t WHERE close_day = :today";
		String closeType = jdbc.queryForObject(sql, params, String.class);
		
		assertThat(closeType).isEqualTo("臨時休業");
	}

	@Test
	@DisplayName("売上フラッシュ情報を返す")
	void testGetHourlySalesFlash() {
		// 現在時刻を取得
		int currentHour = LocalDateTime.now().getHour();
		String expectedTimeRange = String.format("%02d:00 〜 %02d:00", currentHour, currentHour + 1);
		
		// テスト対象メソッドを実行する
		SalesFlashDto response = service.getHourlySalesFlash();
		
		// 実行結果の確認
		assertThat(response).isNotNull(); // オブジェクトが取得できていること
		assertThat(response.timeRange()).isEqualTo(expectedTimeRange); // 時間帯文字列が正しいこと
		assertThat(response.totalSales()).isGreaterThanOrEqualTo(0); // 売上合計が0以上であること
		assertThat(response.customerCount()).isGreaterThanOrEqualTo(0); // 客数が0以上であること
	}

	@Test
	@DisplayName("通知内容を入力し一括通知を登録する")
	void testSendBroadcastNotice() {
		// 渡したい引数を設定
		String content = "こんにちは";
		
		// テスト対象メソッドを実行する
		service.sendBroadcastNotice(content);
		
		// 実行結果の確認
		String mail = "murokishoon@example.com";
		List<Map<String, Object>> noti = service.getNotificationsByMail(mail);
		
		assertThat(noti).isNotEmpty();
		assertThat(noti.get(0).get("content")).isEqualTo(content);
	}

//	@Test
//	void testCloseDays() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testPasswordCheck() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testUpdateNoPassword() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testUpdateYesPassword() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testRegisterCustomer() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testUpdateProfile() {
//		fail("まだ実装されていません");
//	}
//
//	@Test
//	void testGetAi() {
//		fail("まだ実装されていません");
//	}

}
