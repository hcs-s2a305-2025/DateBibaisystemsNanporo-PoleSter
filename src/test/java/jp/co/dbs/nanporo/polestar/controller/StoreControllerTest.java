package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;
import jp.co.dbs.nanporo.polestar.service.StoreService;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreController controller;

    @Test
    @DisplayName("メニュー画面へ商品一覧を設定する")
    void testShowMenu() {
        List<GoodsData> goods = List.of(new GoodsData());
        when(storeService.getMenuList()).thenReturn(goods);
        var model = new ExtendedModelMap();

        assertThat(controller.showMenu(model)).isEqualTo("menu");
        assertThat(model.asMap()).containsEntry("menuList", goods);
    }

    @Test
    @DisplayName("POS画面へ商品一覧を設定する")
    void testShowPos() {
        List<GoodsData> goods = List.of(new GoodsData());
        when(storeService.getMenuList()).thenReturn(goods);
        var model = new ExtendedModelMap();

        assertThat(controller.showPos(model)).isEqualTo("w/polestarpos");
        assertThat(model.asMap()).containsEntry("menuList", goods);
    }

    @Test
    @DisplayName("会計履歴画面を表示する")
    void testShowCashHistory() {
        assertThat(controller.showCashHistory(new ExtendedModelMap())).isEqualTo("w/casherhistory");
    }

    @Test
    @DisplayName("商品編集一覧画面へ商品一覧を設定する")
    void testShowEditMenu() {
        List<GoodsData> goods = List.of(new GoodsData());
        when(storeService.getMenuList()).thenReturn(goods);
        var model = new ExtendedModelMap();

        assertThat(controller.shoeEditMenu(model)).isEqualTo("w/editmenu");
        assertThat(model.asMap()).containsEntry("menuList", goods);
    }

    @Test
    @DisplayName("既存商品の編集に必要な情報を取得する")
    void testGetEditExistingGoods() {
        GoodsData goods = new GoodsData();
        when(storeService.getGoodsDetail("G01")).thenReturn(goods);
        when(storeService.getRiceOptions()).thenReturn(List.of());
        when(storeService.getSauceOptions()).thenReturn(List.of());
        when(storeService.getCategoryList()).thenReturn(List.of());
        when(storeService.getAllergenList("G01")).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertThat(controller.getEdit("G01", model)).isEqualTo("w/editmenu/edit");
        assertThat(model.asMap()).containsEntry("goods", goods).containsKey("riceOptions")
                .containsKey("sauceOptions").containsKey("categories").containsKey("allergens");
    }

    @Test
    @DisplayName("新規商品の編集画面は初期値を設定する")
    void testGetEditNewGoods() {
        when(storeService.getRiceOptions()).thenReturn(List.of());
        when(storeService.getSauceOptions()).thenReturn(List.of());
        when(storeService.getCategoryList()).thenReturn(List.of());
        when(storeService.getAllergenList(null)).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertThat(controller.getEdit(null, model)).isEqualTo("w/editmenu/edit");
        GoodsData goods = (GoodsData) model.get("goods");
        assertThat(goods.getWatchRank()).isEqualTo("一般");
        assertThat(goods.getSoldOut()).isFalse();
    }

    @Test
    @DisplayName("空の商品IDは新規商品の編集として扱う")
    void testGetEditWithBlankGoodsId() {
        when(storeService.getRiceOptions()).thenReturn(List.of());
        when(storeService.getSauceOptions()).thenReturn(List.of());
        when(storeService.getCategoryList()).thenReturn(List.of());
        when(storeService.getAllergenList("")).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertThat(controller.getEdit("", model)).isEqualTo("w/editmenu/edit");
        assertThat(model.get("goods")).isInstanceOf(GoodsData.class);
    }

    @Test
    @DisplayName("商品更新時にカテゴリ未指定を補い商品を保存する")
    void testUpdateProductWithoutPhoto() {
        GoodsEditRequest request = new GoodsEditRequest();

        assertThat(controller.updateProduct(request, null)).isEqualTo("redirect:/w/editmenu");
        verify(storeService).saveGoods(request);
    }

    @Test
    @DisplayName("空カテゴリと空アップロードを許容して商品を保存する")
    void testUpdateProductWithBlankCategoryAndEmptyPhoto() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setCategoryId("");
        MultipartFile photo = org.mockito.Mockito.mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(true);

        assertThat(controller.updateProduct(request, photo)).isEqualTo("redirect:/w/editmenu");
        verify(storeService).saveGoods(request);
    }

    @Test
    @DisplayName("画像保存でIOExceptionが起きても商品を保存する")
    void testUpdateProductWhenPhotoTransferFails() throws Exception {
        GoodsEditRequest request = new GoodsEditRequest();
        MultipartFile photo = org.mockito.Mockito.mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getOriginalFilename()).thenReturn("failed.png");
        org.mockito.Mockito.doThrow(new java.io.IOException("write failed"))
                .when(photo).transferTo(any(File.class));

        assertThat(controller.updateProduct(request, photo)).isEqualTo("redirect:/w/editmenu");
        assertThat(request.getPhoto()).isNull();
        verify(storeService).saveGoods(request);
    }

    @Test
    @DisplayName("アップロード画像を商品に設定して保存する")
    void testUpdateProductWithPhoto() throws Exception {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setCategoryId("B");
        MultipartFile photo = org.mockito.Mockito.mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getOriginalFilename()).thenReturn("photo.png");
        doNothing().when(photo).transferTo(any(File.class));

        assertThat(controller.updateProduct(request, photo)).isEqualTo("redirect:/w/editmenu");
        assertThat(request.getPhoto()).isEqualTo("photo.png");
        verify(photo).transferTo(any(File.class));
        verify(storeService).saveGoods(request);
    }

    @Test
    @DisplayName("保存先ディレクトリがない場合に作成して画像を保存する")
    void testUpdateProductCreatesUploadDirectory() throws Exception {
        GoodsEditRequest request = new GoodsEditRequest();
        MultipartFile photo = org.mockito.Mockito.mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(photo.getOriginalFilename()).thenReturn("photo.png");
        doNothing().when(photo).transferTo(any(File.class));
        Path uploadDir = Paths.get("src/main/resources/static/img").toAbsolutePath();

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.exists(uploadDir)).thenReturn(false);
            files.when(() -> Files.createDirectories(uploadDir)).thenReturn(uploadDir);

            assertThat(controller.updateProduct(request, photo)).isEqualTo("redirect:/w/editmenu");

            files.verify(() -> Files.createDirectories(uploadDir));
        }

        assertThat(request.getPhoto()).isEqualTo("photo.png");
        verify(storeService).saveGoods(request);
    }

    @Test
    @DisplayName("販売停止時のメッセージを設定する")
    void testToggleSoldOutOn() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.toggleSoldOut("G01", true, attributes)).isEqualTo("redirect:/w/editmenu");
        assertThat(attributes.getFlashAttributes().get("message")).isEqualTo("商品を販売停止にしました。");
        verify(storeService).updateSoldOut("G01", true);
    }

    @Test
    @DisplayName("販売再開時のメッセージを設定する")
    void testToggleSoldOutOff() {
        var attributes = new RedirectAttributesModelMap();

        controller.toggleSoldOut("G01", false, attributes);

        assertThat(attributes.getFlashAttributes().get("message")).isEqualTo("商品の販売を再開しました。");
        verify(storeService).updateSoldOut("G01", false);
    }

    @Test
    @DisplayName("米カスタム新規登録時に米IDを採番する")
    void testSaveRiceCustom() {
        when(storeRepository.generateRiceCustomId()).thenReturn(100);

        String view = controller.saveCustom(null, "rice", "大盛", 50, 300, "なし", false, "G01");

        assertThat(view).isEqualTo("redirect:/w/editmenu/edit?goodsId=G01");
        verify(storeRepository).insertCustom(100, "大盛", 50, 300, "なし", false);
    }

    @Test
    @DisplayName("ソースカスタム新規登録時にソースIDを採番する")
    void testSaveSauceCustom() {
        when(storeRepository.generateSauceCustomId()).thenReturn(200);

        controller.saveCustom(null, "sauce", "タルタル", 80, 100, "卵", true, "G01");

        verify(storeRepository).insertCustom(200, "タルタル", 80, 100, "卵", true);
    }

    @Test
    @DisplayName("既存カスタムを更新する")
    void testUpdateCustom() {
        String view = controller.saveCustom(9, "rice", "普通", 0, 250, "なし", false, "G02");

        assertThat(view).isEqualTo("redirect:/w/editmenu/edit?goodsId=G02");
        verify(storeRepository).updateCustom(9, "普通", 0, 250, "なし", false);
    }

    @Test
    @DisplayName("カスタムを削除して商品編集画面へ戻る")
    void testDeleteCustom() {
        assertThat(controller.deleteCustom(9, "G03")).isEqualTo("redirect:/w/editmenu/edit?goodsId=G03");
        verify(storeRepository).deleteCustom(9);
    }
}