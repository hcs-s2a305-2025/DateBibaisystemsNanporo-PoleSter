package jp.co.dbs.nanporo.polestar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import org.springframework.context.annotation.Configuration;

@Tag("unit")
class MessageConfigTest {

	@Test
	@DisplayName("メッセージ設定をSpring設定クラスとして構成し主要文言を保持する")
	void configurationAndMessages() {
		assertThat(MessageConfig.class).hasAnnotation(Configuration.class);
		assertThat(MessageConfig.USERID_REQUIRED_MESSAGE).isEqualTo("【M001】メールアドレスを入力してください。");
		assertThat(MessageConfig.PASSWORD_REQUIRED_MESSAGE).isEqualTo("【M002】パスワードを入力してください。");
		assertThat(MessageConfig.ORDER_REGISTERED_MESSAGE).isEqualTo("【M103】注文を確定しました。");
		assertThat(MessageConfig.NEGATIVE_AMOUNT_MESSAGE).contains("正しい金額");
		assertThat(new MessageConfig()).isNotNull();
	}
}