package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dbs.nanporo.polestar.data.AllergenData;
import jp.co.dbs.nanporo.polestar.data.CustomData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StoreServiceUnitTest {

	@Mock
	private StoreRepository repository;

	@InjectMocks
	private StoreService service;

	@Test
	@DisplayName("商品一覧を数値・文字列・大文字キーの差を吸収して変換する")
	void getMenuList() {
		Map<String, Object> lowerCase = goodsRow("B001", 500L, "650", true);
		lowerCase.put("zangi_count", "6");
		Map<String, Object> upperCase = new HashMap<>();
		upperCase.put("GOODS_ID", "S001");
		upperCase.put("GOODS_NAME", "サイド");
		upperCase.put("PHOTO", null);
		upperCase.put("PRICE", "不正値");
		upperCase.put("CALORIE", null);
		upperCase.put("ALLERGY", null);
		upperCase.put("ZANGI_COUNT", null);
		upperCase.put("SOLD_OUT", "1");
		upperCase.put("DETAIL", null);
		upperCase.put("WATCH_RANK", null);
		upperCase.put("CATEGORY_ID", "S");
		when(repository.getAllGoods()).thenReturn(List.of(lowerCase, upperCase));

		List<GoodsData> goods = service.getMenuList(null);

		assertThat(goods).hasSize(2);
		assertThat(goods.get(0).getPrice()).isEqualTo(500);
		assertThat(goods.get(0).getCalorie()).isEqualTo(650);
		assertThat(goods.get(0).getZangiCount()).isEqualTo(6);
		assertThat(goods.get(0).getSoldOut()).isTrue();
		assertThat(goods.get(1).getGoodsId()).isEqualTo("S001");
		assertThat(goods.get(1).getPrice()).isZero();
		assertThat(goods.get(1).getCalorie()).isZero();
		assertThat(goods.get(1).getSoldOut()).isTrue();
	}

	@Test
	@DisplayName("カテゴリ指定時は前後の空白を除いて商品一覧を取得する")
	void getMenuListByPrefix() {
		when(repository.getGoodsByPrefix("S")).thenReturn(List.of(goodsRow("S001", 120, 200, false)));
		when(repository.getAllGoods()).thenReturn(List.of(goodsRow("B001", 500, 650, false)));

		List<GoodsData> goods = service.getMenuList("  S  ");
		List<GoodsData> allGoods = service.getMenuList("   ");

		assertThat(goods).hasSize(1);
		assertThat(goods.get(0).getGoodsId()).isEqualTo("S001");
		assertThat(allGoods).extracting(GoodsData::getGoodsId).containsExactly("B001");
		verify(repository).getGoodsByPrefix("S");
		verify(repository).getAllGoods();
	}

	@Test
	@DisplayName("商品詳細の未入力・未登録と売切値の各形式を処理する")
	void getGoodsDetail() {
		assertThat(service.getGoodsDetail(null)).isNull();
		assertThat(service.getGoodsDetail("  ")).isNull();
		verify(repository, never()).getGoodsById(org.mockito.ArgumentMatchers.anyString());
		when(repository.getGoodsById("B404")).thenReturn(null);
		when(repository.getGoodsById(" B001 ")).thenReturn(null);
		assertThat(service.getGoodsDetail("B404")).isNull();
		when(repository.getGoodsById("B001")).thenReturn(goodsRow("B001", "800", 700, "TRUE"));
		when(repository.getGoodsById("B002")).thenReturn(goodsRow("B002", 500, 300, "false"));
		when(repository.getGoodsById("B003")).thenReturn(goodsRow("B003", 500, 300, null));

		assertThat(service.getGoodsDetail(" B001 ")).isNull();
		GoodsData soldOut = service.getGoodsDetail("B001");
		GoodsData available = service.getGoodsDetail("B002");
		GoodsData defaultAvailable = service.getGoodsDetail("B003");

		assertThat(soldOut.getSoldOut()).isTrue();
		assertThat(available.getSoldOut()).isFalse();
		assertThat(defaultAvailable.getSoldOut()).isFalse();
	}

	@Test
	@DisplayName("ライス・ソースオプションと固定カテゴリを返す")
	void getOptionsAndCategories() {
		when(repository.getRiceCustoms()).thenReturn(List.of(
				customRow(10, "小盛り", 0, false), customRow(20, "普通", "50", "TRUE")));
		when(repository.getSauceCustoms()).thenReturn(List.of(
				customRow(50, "ソース", "不正", "1"), customRow(51, "ソースだく", 80, "false"),
				customRow(52, "ソースなし", 0, null)));

		List<CustomData> rice = service.getRiceOptions();
		List<CustomData> sauce = service.getSauceOptions();

		assertThat(rice).hasSize(2);
		assertThat(rice.get(0).getSoldOut()).isFalse();
		assertThat(rice.get(1).getPrice()).isEqualTo(50);
		assertThat(rice.get(1).getSoldOut()).isTrue();
		assertThat(sauce.get(0).getPrice()).isZero();
		assertThat(sauce.get(0).getSoldOut()).isTrue();
		assertThat(sauce.get(1).getSoldOut()).isFalse();
		assertThat(sauce.get(2).getSoldOut()).isFalse();
		assertThat(service.getCategoryList()).extracting("id").containsExactly("B", "S", "U");
	}

	@Test
	@DisplayName("空のDB行と大小文字キーを使った項目検索を処理する")
	void getMenuListWithMissingValues() throws Exception {
		when(repository.getAllGoods()).thenReturn(List.of(new HashMap<>()));

		assertThat(service.getMenuList(null)).hasSize(1)
				.first()
				.satisfies(goods -> {
					assertThat(goods.getGoodsId()).isNull();
					assertThat(goods.getPrice()).isZero();
				});

		var getValue = StoreService.class.getDeclaredMethod("getValue", Map.class, String.class);
		getValue.setAccessible(true);
		assertThat(getValue.invoke(service, Map.of("goods_id", "B001"), "GOODS_ID")).isEqualTo("B001");
		assertThat(getValue.invoke(service, Map.of(), "goods_id")).isNull();
	}

	@Test
	@DisplayName("商品アレルゲン一覧の選択状態を商品情報から生成する")
	void getAllergenList() {
		when(repository.getGoodsById("B002")).thenReturn(goodsRowWithAllergy("B002", "なし"));
		when(repository.getGoodsById("B003")).thenReturn(null);
		when(repository.getGoodsById("B004")).thenReturn(goodsRowWithAllergy("B004", null));
		Map<String, Object> allergenRow = goodsRow("B001", 100, 200, false);
		allergenRow.put("allergy", "小麦,大豆");
		when(repository.getGoodsById("B001")).thenReturn(allergenRow);

		List<AllergenData> active = service.getAllergenList("B001");
		List<AllergenData> none = service.getAllergenList("B002");
		List<AllergenData> missing = service.getAllergenList("B003");
		List<AllergenData> unspecified = service.getAllergenList("B004");

		assertThat(active).hasSize(12);
		assertThat(active).filteredOn(data -> Boolean.TRUE.equals(data.getChecked())).extracting(AllergenData::getName)
				.containsExactly("小麦", "大豆");
		assertThat(none).noneMatch(data -> Boolean.TRUE.equals(data.getChecked()));
		assertThat(missing).noneMatch(data -> Boolean.TRUE.equals(data.getChecked()));
		assertThat(unspecified).noneMatch(data -> Boolean.TRUE.equals(data.getChecked()));
	}

	@Test
	@DisplayName("新規商品登録は画像・アレルゲン・カテゴリ既定値を渡す")
	void saveNewGoods() {
		GoodsEditRequest request = new GoodsEditRequest();
		request.setGoodsName("新商品");
		request.setPhoto("new.png");
		request.setAllergy("なし");
		when(repository.generateGoodsId("B")).thenReturn("B007", "B008");

		service.saveGoods(request);

		verify(repository).insertGoods("B007", request, "/img/new.png", "");
		request.setPhoto("");
		request.setAllergy(null);
		request.setCategoryId("S");
		when(repository.generateGoodsId("S")).thenReturn("S004");
		service.saveGoods(request);
		verify(repository).insertGoods("S004", request, null, "");

		request.setGoodsId(" ");
		request.setPhoto(null);
		request.setCategoryId("");
		request.setAllergy("なし");
		service.saveGoods(request);
		verify(repository).insertGoods("B008", request, null, "");
	}

	@Test
	@DisplayName("既存商品の更新では画像未指定をnullで保持しアレルゲンを渡す")
	void saveExistingGoodsAndUpdateSoldOut() {
		GoodsEditRequest request = new GoodsEditRequest();
		request.setGoodsId("B001");
		request.setPhoto("");
		request.setAllergy("小麦");

		service.saveGoods(request);
		service.updateSoldOut("B001", true);

		verify(repository).updateGoods(request, null, "小麦");
		verify(repository).updateSoldOut("B001", true);
		verify(repository, never()).generateGoodsId(org.mockito.ArgumentMatchers.anyString());
	}

	private Map<String, Object> goodsRow(String id, Object price, Object calorie, Object soldOut) {
		Map<String, Object> row = new HashMap<>();
		row.put("goods_id", id);
		row.put("goods_name", "商品");
		row.put("photo", "goods.png");
		row.put("price", price);
		row.put("calorie", calorie);
		row.put("allergy", "小麦");
		row.put("zangi_count", 5);
		row.put("sold_out", soldOut);
		row.put("detail", "詳細");
		row.put("watch_rank", "一般");
		row.put("category_id", "B");
		return row;
	}

	private Map<String, Object> goodsRowWithAllergy(String id, String allergy) {
		Map<String, Object> row = goodsRow(id, 100, 200, false);
		row.put("allergy", allergy);
		return row;
	}

	private Map<String, Object> customRow(Object id, String name, Object price, Object soldOut) {
		Map<String, Object> row = new HashMap<>();
		row.put("custom_id", id);
		row.put("goods_name", name);
		row.put("price", price);
		row.put("calorie", "25");
		row.put("allergy", "大豆");
		row.put("sold_out", soldOut);
		return row;
	}
}