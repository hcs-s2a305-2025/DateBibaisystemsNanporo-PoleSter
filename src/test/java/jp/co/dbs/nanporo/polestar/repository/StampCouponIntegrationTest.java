package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Tag("integration")
@ActiveProfiles("test")
class StampCouponIntegrationTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Test
    @DisplayName("割引券の付与・使用・消し込み・取消が実DBで期待どおり動作する")
    void couponLifecycle() {
        String mail = createUser();

        // 券がない状態では使用できない
        assertThat(repository.activateStampCoupon(mail)).isFalse();

        repository.addStampCoupon(mail, 2);
        assertThat(state(mail)).containsEntry("stamp_coupon", 2).containsEntry("stamp_coupon_active", false);

        // 使用すると1枚消費して提示中になり、二重使用はできない
        assertThat(repository.activateStampCoupon(mail)).isTrue();
        assertThat(repository.activateStampCoupon(mail)).isFalse();
        assertThat(state(mail)).containsEntry("stamp_coupon", 1).containsEntry("stamp_coupon_active", true);

        // 取り消すと券が戻り、未提示では取り消せない
        assertThat(repository.cancelStampCoupon(mail)).isTrue();
        assertThat(repository.cancelStampCoupon(mail)).isFalse();
        assertThat(state(mail)).containsEntry("stamp_coupon", 2).containsEntry("stamp_coupon_active", false);

        // 会計時の消し込みは提示中のときだけ成功し、券は戻らない
        assertThat(repository.useActiveStampCoupon(mail)).isFalse();
        repository.activateStampCoupon(mail);
        assertThat(repository.useActiveStampCoupon(mail)).isTrue();
        assertThat(repository.useActiveStampCoupon(mail)).isFalse();
        assertThat(state(mail)).containsEntry("stamp_coupon", 1).containsEntry("stamp_coupon_active", false);
    }

    private String createUser() {
        String mail = UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("""
                INSERT INTO user_m(mail, name, password, role, member_rank, gender, birthday, cancel_count)
                VALUES (:mail, 'テスト', 'pw', '1', '一般', '女', DATE '1990-01-01', 0)
                """, Map.of("mail", mail));
        return mail;
    }

    private Map<String, Object> state(String mail) {
        Map<String, Object> row = repository.findByMail(mail);
        return Map.of(
                "stamp_coupon", ((Number) row.get("stamp_coupon")).intValue(),
                "stamp_coupon_active", row.get("stamp_coupon_active"));
    }
}
