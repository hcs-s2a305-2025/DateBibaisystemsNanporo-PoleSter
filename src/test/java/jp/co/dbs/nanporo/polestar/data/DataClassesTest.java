package jp.co.dbs.nanporo.polestar.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.sql.Timestamp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataClassesTest {

    @Test
    @DisplayName("ユーザーデータの各項目を保持する")
    void testUserData() {
        UserData data = new UserData();
        data.setMail("user@example.com");
        data.setName("山田太郎");
        data.setPassword("encoded");
        data.setRole("顧客");
        data.setMemberRank("ゴールド");
        data.setGender("男");
        data.setBirthday(Date.valueOf("1990-01-02"));
        data.setCancelCount(2);
        data.setAlive(true);
        data.setPoint(120);
        data.setPointCardComplete(3);

        UserData expected = new UserData();
        expected.setMail("user@example.com");
        expected.setName("山田太郎");
        expected.setPassword("encoded");
        expected.setRole("顧客");
        expected.setMemberRank("ゴールド");
        expected.setGender("男");
        expected.setBirthday(Date.valueOf("1990-01-02"));
        expected.setCancelCount(2);
        expected.setAlive(true);
        expected.setPoint(120);
        expected.setPointCardComplete(3);

        assertEquivalent(data, expected);
        assertThat(data.isAlive()).isTrue();
        assertThat(data.getPointCardComplete()).isEqualTo(3);
    }

    @Test
    @DisplayName("注文データの各項目を保持する")
    void testOrderData() {
        OrderData data = new OrderData();
        data.setOrderId(12);
        data.setOrderNumber("M0012");
        data.setGetTime(Timestamp.valueOf("2026-10-01 12:30:00"));
        data.setMail("user@example.com");
        data.setRegisterTime(Timestamp.valueOf("2026-10-01 12:00:00"));
        data.setSumMoney(1500);
        data.setMemo("備考");
        data.setStatus("受付");
        data.setOrderType("RESERVATION");

        OrderData expected = new OrderData();
        expected.setOrderId(12);
        expected.setOrderNumber("M0012");
        expected.setGetTime(Timestamp.valueOf("2026-10-01 12:30:00"));
        expected.setMail("user@example.com");
        expected.setRegisterTime(Timestamp.valueOf("2026-10-01 12:00:00"));
        expected.setSumMoney(1500);
        expected.setMemo("備考");
        expected.setStatus("受付");
        expected.setOrderType("RESERVATION");

        assertEquivalent(data, expected);
    }

    @Test
    @DisplayName("注文明細データの各項目を保持する")
    void testOrderDetailData() {
        OrderDetailData data = new OrderDetailData();
        data.setOrderId(12);
        data.setOrderCount(2);
        data.setGoodsId("G01");
        data.setSetGoodsId(3);
        data.setCount(4);
        data.setPlusZangiCount(1);
        data.setCustomId(50);

        OrderDetailData expected = new OrderDetailData();
        expected.setOrderId(12);
        expected.setOrderCount(2);
        expected.setGoodsId("G01");
        expected.setSetGoodsId(3);
        expected.setCount(4);
        expected.setPlusZangiCount(1);
        expected.setCustomId(50);

        assertEquivalent(data, expected);
    }

    @Test
    @DisplayName("商品データの各項目と独自アクセサを保持する")
    void testGoodsData() {
        GoodsData data = new GoodsData();
        data.setGoodsId("G01");
        data.setGoodsName("ザンギ弁当");
        data.setPhoto("goods.png");
        data.setPrice(800);
        data.setCalorie(650);
        data.setAllergy("小麦");
        data.setZangiCount(5);
        data.setSoldOut(true);
        data.setDetail("商品説明");
        data.setWatchRank("一般");
        data.setCategoryId("B");

        GoodsData expected = new GoodsData();
        expected.setGoodsId("G01");
        expected.setGoodsName("ザンギ弁当");
        expected.setPhoto("goods.png");
        expected.setPrice(800);
        expected.setCalorie(650);
        expected.setAllergy("小麦");
        expected.setZangiCount(5);
        expected.setSoldOut(true);
        expected.setDetail("商品説明");
        expected.setWatchRank("一般");
        expected.setCategoryId("B");

        assertEquivalent(data, expected);
        assertThat(data.getCategoryId()).isEqualTo("B");
        assertThat(data.getSoldOut()).isTrue();
        data.setSoldOut(null);
        assertThat(data.getSoldOut()).isNull();
    }

    @Test
    @DisplayName("カスタムデータの各項目と独自Booleanアクセサを保持する")
    void testCustomData() {
        CustomData data = new CustomData();
        data.setCustomId(50);
        data.setGoodsName("おろしポン酢");
        data.setPrice(80);
        data.setCalorie(20);
        data.setAllergy("大豆");
        data.setSoldOut(false);

        CustomData expected = new CustomData();
        expected.setCustomId(50);
        expected.setGoodsName("おろしポン酢");
        expected.setPrice(80);
        expected.setCalorie(20);
        expected.setAllergy("大豆");
        expected.setSoldOut(false);

        assertEquivalent(data, expected);
        assertThat(data.getSoldOut()).isFalse();
        data.setSoldOut(null);
        assertThat(data.getSoldOut()).isNull();
    }

    @Test
    @DisplayName("カテゴリデータの引数なし・全項目コンストラクターを使う")
    void testCategoryDataConstructors() {
        CategoryData empty = new CategoryData();
        assertThat(empty.getId()).isNull();
        assertThat(empty.getName()).isNull();

        CategoryData data = new CategoryData("B", "弁当");
        CategoryData expected = new CategoryData();
        expected.setId("B");
        expected.setName("弁当");

        assertEquivalent(data, expected);
    }

    @Test
    @DisplayName("アレルゲンデータの各項目を保持する")
    void testAllergenData() {
        AllergenData data = new AllergenData();
        data.setId("A01");
        data.setName("小麦");
        data.setChecked(true);

        AllergenData expected = new AllergenData();
        expected.setId("A01");
        expected.setName("小麦");
        expected.setChecked(true);

        assertEquivalent(data, expected);
    }

    @Test
    @DisplayName("カートデータの各項目を保持する")
    void testCartData() {
        CartData data = new CartData();
        data.setCartItemId("cart-1");
        data.setGoodsId("G01");
        data.setGoodsName("ザンギ弁当");
        data.setPrice(800);
        data.setPhoto("goods.png");
        data.setOrderDate("2026年10月1日");
        data.setZangiCount(6);
        data.setZangiPrice(100);
        data.setRiceCode("30");
        data.setRiceAmount("大盛り");
        data.setRicePrice(50);
        data.setSourceCode("50");
        data.setSourceType("おろしポン酢");
        data.setSourcePrice(80);
        data.setTotalPrice(1030);

        CartData expected = new CartData();
        expected.setCartItemId("cart-1");
        expected.setGoodsId("G01");
        expected.setGoodsName("ザンギ弁当");
        expected.setPrice(800);
        expected.setPhoto("goods.png");
        expected.setOrderDate("2026年10月1日");
        expected.setZangiCount(6);
        expected.setZangiPrice(100);
        expected.setRiceCode("30");
        expected.setRiceAmount("大盛り");
        expected.setRicePrice(50);
        expected.setSourceCode("50");
        expected.setSourceType("おろしポン酢");
        expected.setSourcePrice(80);
        expected.setTotalPrice(1030);

        assertEquivalent(data, expected);
    }

    @Test
    @DisplayName("メールデータの各項目を保持する")
    void testMailData() {
        MailData data = new MailData();
        data.setTo("to@example.com");
        data.setFrom("from@example.com");
        data.setSubject("件名");
        data.setBody("本文");

        MailData expected = new MailData();
        expected.setTo("to@example.com");
        expected.setFrom("from@example.com");
        expected.setSubject("件名");
        expected.setBody("本文");

        assertEquivalent(data, expected);
    }

    private void assertEquivalent(Object actual, Object expected) {
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
        assertThat(actual.toString()).isNotBlank();
    }
}