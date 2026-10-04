package jp.co.dbs.nanporo.polestar.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

class EntityClassesTest {

    @Test
    @DisplayName("ユーザーEntityの全項目を保持する")
    void testUserEntity() {
        UserEntity actual = new UserEntity();
        actual.setMail("user@example.com");
        actual.setName("山田太郎");
        actual.setPassword("encoded");
        actual.setRole("顧客");
        actual.setMemberRank("シルバー");
        actual.setGender("男");
        actual.setBirthday(Date.valueOf("1990-01-02"));
        actual.setCancelCount(2);
        actual.setAlive(true);
        actual.setPoint(100);
        actual.setPointCardComplete(3);

        UserEntity expected = new UserEntity();
        expected.setMail("user@example.com");
        expected.setName("山田太郎");
        expected.setPassword("encoded");
        expected.setRole("顧客");
        expected.setMemberRank("シルバー");
        expected.setGender("男");
        expected.setBirthday(Date.valueOf("1990-01-02"));
        expected.setCancelCount(2);
        expected.setAlive(true);
        expected.setPoint(100);
        expected.setPointCardComplete(3);

        assertEquivalent(actual, expected);
        assertThat(actual.getAlive()).isTrue();
    }

    @Test
    @DisplayName("注文Entityの全項目を保持する")
    void testOrderEntity() {
        OrderEntity actual = new OrderEntity();
        actual.setOrderId(1);
        actual.setOrderNumber("M0001");
        actual.setGetTime(Timestamp.valueOf("2026-10-01 13:00:00"));
        actual.setMail("user@example.com");
        actual.setRegisterTime(Timestamp.valueOf("2026-10-01 12:00:00"));
        actual.setSumMoney(1200);
        actual.setMemo("要望");
        actual.setStatus("受付");

        OrderEntity expected = new OrderEntity();
        expected.setOrderId(1);
        expected.setOrderNumber("M0001");
        expected.setGetTime(Timestamp.valueOf("2026-10-01 13:00:00"));
        expected.setMail("user@example.com");
        expected.setRegisterTime(Timestamp.valueOf("2026-10-01 12:00:00"));
        expected.setSumMoney(1200);
        expected.setMemo("要望");
        expected.setStatus("受付");

        assertEquivalent(actual, expected);
    }

    @Test
    @DisplayName("注文複合キーのコンストラクターと等価性を確認する")
    void testOrderDetailKey() {
        OrderDetailKey actual = new OrderDetailKey(1, 2);
        OrderDetailKey expected = new OrderDetailKey();
        expected.setOrderId(1);
        expected.setOrderCount(2);

        assertEquivalent(actual, expected);
        assertThat(actual).isInstanceOf(Serializable.class);
    }

    @Test
    @DisplayName("注文詳細Entityの全項目と複合IDを保持する")
    void testOrderDetailEntity() {
        OrderDetailEntity actual = new OrderDetailEntity();
        actual.setOrderId(1);
        actual.setOrderCount(2);
        actual.setGoodsId("G01");
        actual.setSetGoodsId(3);
        actual.setCount(4);
        actual.setPlusZangiCount(1);
        actual.setCustomId(50);

        OrderDetailEntity expected = new OrderDetailEntity();
        expected.setOrderId(1);
        expected.setOrderCount(2);
        expected.setGoodsId("G01");
        expected.setSetGoodsId(3);
        expected.setCount(4);
        expected.setPlusZangiCount(1);
        expected.setCustomId(50);

        assertEquivalent(actual, expected);
        assertThat(OrderDetailEntity.class.getAnnotation(IdClass.class).value())
                .isEqualTo(OrderDetailKey.class);
    }

    @Test
    @DisplayName("取引Entityの全項目を保持する")
    void testTransactionEntity() {
        TransactionEntity actual = new TransactionEntity();
        actual.setTransactionId(10);
        actual.setOrderId(1);
        actual.setMail("user@example.com");
        actual.setTransactionDate(LocalDateTime.of(2026, 10, 1, 12, 0));
        actual.setUseCoupon("使用");
        actual.setReceivedMoney(2000);
        actual.setChangeMoney(800);
        actual.setSumMoney(1200);

        TransactionEntity expected = new TransactionEntity();
        expected.setTransactionId(10);
        expected.setOrderId(1);
        expected.setMail("user@example.com");
        expected.setTransactionDate(LocalDateTime.of(2026, 10, 1, 12, 0));
        expected.setUseCoupon("使用");
        expected.setReceivedMoney(2000);
        expected.setChangeMoney(800);
        expected.setSumMoney(1200);

        assertEquivalent(actual, expected);
    }

    @Test
    @DisplayName("取引明細複合キーのコンストラクターと等価性を確認する")
    void testTransactionDetailKey() {
        TransactionDetailKey actual = new TransactionDetailKey(10, 2);
        TransactionDetailKey expected = new TransactionDetailKey();
        expected.setTransactionId(10);
        expected.setReservationCount(2);

        assertEquivalent(actual, expected);
        assertThat(actual).isInstanceOf(Serializable.class);
    }

    @Test
    @DisplayName("取引明細Entityの全項目と複合IDを保持する")
    void testTransactionDetailEntity() {
        TransactionDetailEntity actual = new TransactionDetailEntity();
        actual.setTransactionId(10);
        actual.setReservationCount(2);
        actual.setGoodsName("ザンギ弁当");
        actual.setSetGoodsName("弁当セット");
        actual.setCount(3);
        actual.setPlusZangiCount(1);
        actual.setCustomId(50);
        actual.setPrice(2500);

        TransactionDetailEntity expected = new TransactionDetailEntity();
        expected.setTransactionId(10);
        expected.setReservationCount(2);
        expected.setGoodsName("ザンギ弁当");
        expected.setSetGoodsName("弁当セット");
        expected.setCount(3);
        expected.setPlusZangiCount(1);
        expected.setCustomId(50);
        expected.setPrice(2500);

        assertEquivalent(actual, expected);
        assertThat(TransactionDetailEntity.class.getAnnotation(IdClass.class).value())
                .isEqualTo(TransactionDetailKey.class);
    }

    @Test
    @DisplayName("商品Entityの各項目を保持する")
    void testGoodsEntity() {
        GoodsEntity actual = new GoodsEntity();
        actual.setGoodsId("G01");
        actual.setGoodsName("ザンギ弁当");
        actual.setPhoto("goods.png");
        actual.setPrice(800);
        actual.setCalorie(650);
        actual.setAllergy("小麦");
        actual.setZangiCount(5);
        actual.setSoldOut(false);
        actual.setDetail("商品説明");
        actual.setWatchRank("一般");

        GoodsEntity expected = new GoodsEntity();
        expected.setGoodsId("G01");
        expected.setGoodsName("ザンギ弁当");
        expected.setPhoto("goods.png");
        expected.setPrice(800);
        expected.setCalorie(650);
        expected.setAllergy("小麦");
        expected.setZangiCount(5);
        expected.setSoldOut(false);
        expected.setDetail("商品説明");
        expected.setWatchRank("一般");

        assertEquivalent(actual, expected);
    }

    @Test
    @DisplayName("カスタム商品とセット商品Entityの各項目を保持する")
    void testCustomAndSetGoodsEntities() {
        CustomEntity custom = new CustomEntity();
        custom.setCustomId(50);
        custom.setGoodsName("ソース");
        custom.setPrice(80);
        custom.setCalorie(20);
        custom.setAllergy("大豆");
        custom.setSoldOut(false);

        CustomEntity expectedCustom = new CustomEntity();
        expectedCustom.setCustomId(50);
        expectedCustom.setGoodsName("ソース");
        expectedCustom.setPrice(80);
        expectedCustom.setCalorie(20);
        expectedCustom.setAllergy("大豆");
        expectedCustom.setSoldOut(false);
        assertEquivalent(custom, expectedCustom);

        SetGoodsEntity setGoods = new SetGoodsEntity();
        setGoods.setSetGoodsId(3);
        setGoods.setGoodsName("弁当セット");
        setGoods.setPrice(200);
        setGoods.setCalorie(100);
        setGoods.setAllergy("卵");
        setGoods.setSoldOut(true);

        SetGoodsEntity expectedSetGoods = new SetGoodsEntity();
        expectedSetGoods.setSetGoodsId(3);
        expectedSetGoods.setGoodsName("弁当セット");
        expectedSetGoods.setPrice(200);
        expectedSetGoods.setCalorie(100);
        expectedSetGoods.setAllergy("卵");
        expectedSetGoods.setSoldOut(true);
        assertEquivalent(setGoods, expectedSetGoods);
    }

    @Test
    @DisplayName("JPA Entityのテーブルマッピングを確認する")
    void testJpaMappings() {
        assertTable(OrderEntity.class, "order_t");
        assertTable(OrderDetailEntity.class, "order_detail_t");
        assertTable(TransactionEntity.class, "transaction_t");
        assertTable(TransactionDetailEntity.class, "transaction_detail_t");
        assertTable(CustomEntity.class, "custom_m");
        assertTable(SetGoodsEntity.class, "set_goods_m");
        assertThat(OrderDetailEntity.class.getAnnotation(IdClass.class).value())
                .isEqualTo(OrderDetailKey.class);
        assertThat(TransactionDetailEntity.class.getAnnotation(IdClass.class).value())
                .isEqualTo(TransactionDetailKey.class);
    }

    private void assertEquivalent(Object actual, Object expected) {
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
        assertThat(actual.toString()).isNotBlank();
    }

    private void assertTable(Class<?> entityClass, String tableName) {
        assertThat(entityClass.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(entityClass.getAnnotation(Table.class).name()).isEqualTo(tableName);
    }
}