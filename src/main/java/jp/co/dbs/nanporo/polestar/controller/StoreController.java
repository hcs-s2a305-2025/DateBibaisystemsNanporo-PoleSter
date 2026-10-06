package jp.co.dbs.nanporo.polestar.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jp.co.dbs.nanporo.polestar.data.AllergenData;
import jp.co.dbs.nanporo.polestar.data.CategoryData;
import jp.co.dbs.nanporo.polestar.data.CustomData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;
import jp.co.dbs.nanporo.polestar.service.StoreService;
import jp.co.dbs.nanporo.polestar.service.UserService;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller 
public class StoreController {
    
    @Autowired 
    private StoreService storeService;

    @Autowired 
    private StoreRepository storeRepository;

    @Autowired 
    private UserService userService;

    @GetMapping("/menu")
    public String showMenu(@RequestParam(name = "prefix", required = false) String prefix, 
                            Authentication authentication,
                            Model model) {
        // 1. ログインユーザーの会員ランクを取得（未ログイン時や設定なし時は「一般」）
        String userMemberRank = "一般";
        if (authentication != null && authentication.isAuthenticated()) {
            // ログイン中のメールアドレス（識別子）を取得
            String mail = authentication.getName(); 
            
            // DBからユーザー情報を取得
            UserEntity user = userService.findByMail(mail);
            if (user != null && user.getMemberRank() != null) {
                userMemberRank = user.getMemberRank(); // 「ゴールド」「シルバー」等
            }
        }
        //1. カテゴリ絞り込みを考慮した商品リストの取得
        List<GoodsData> menuList = storeService.getMenuList(prefix, userMemberRank);
        // 2. 下部タブ表示用のカテゴリリストを取得
        List<CategoryData> categoryList = storeService.getCategoryList();
        model.addAttribute("menuList", menuList);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("selectedPrefix", prefix);
        return "menu";
    }

    @GetMapping("/w/polestarpos")
    public String showPos(Model model) {
        List<GoodsData> menuList = storeService.getMenuList(null); // 全商品を取得
        model.addAttribute("menuList", menuList);
        return "w/polestarpos";
    }

    // CashHistoryController での処理に置き換えたため、以下のメソッドはコメントアウト
    // @GetMapping("/w/casherhistory")
    // public String showCashHistory(Model model) {
    //     return "w/casherhistory";
    // }
    
    @GetMapping("/w/editmenu")
    public String showEditMenu(@RequestParam(name = "prefix", required = false) String prefix, Model model) {
        // 1. カテゴリ絞り込みを考慮した商品リストを取得
        List<GoodsData> menuList = storeService.getMenuList(prefix);
        // 2. タブ表示用のカテゴリリストを取得
        List<CategoryData> categoryList = storeService.getCategoryList();
        model.addAttribute("menuList", menuList);
        model.addAttribute("categoryList", categoryList);
        model.addAttribute("selectedPrefix", prefix);
        return "w/editmenu";
    }

    @GetMapping("/w/editmenu/edit")
    public String getEdit(@RequestParam(name = "goodsId", required = false) String goodsId,Model model) {
        
        GoodsData goods;

        if(goodsId != null && !goodsId.isEmpty()){
            // 既存商品の編集時
            goods = storeService.getGoodsDetail(goodsId);
        } else {
            // 新規商品追加時
            goods = new GoodsData();
            goods.setWatchRank("一般");
            goods.setSoldOut(false);
        }
        model.addAttribute("goods", goods);

        // カスタムオプション・カテゴリ・アレルゲンの取得
        List<CustomData> riceOptions = storeService.getRiceOptions();
        List<CustomData> sauceOptions = storeService.getSauceOptions();
        model.addAttribute("riceOptions", riceOptions);
        model.addAttribute("sauceOptions", sauceOptions);

        List<CategoryData> categories = storeService.getCategoryList();
        List<AllergenData> allergens = storeService.getAllergenList(goodsId);
        model.addAttribute("categories", categories);
        model.addAttribute("allergens", allergens);
        
        return "w/editmenu/edit";
    }

    @PostMapping("/product/update")
    public String updateProduct(@ModelAttribute GoodsEditRequest request,
                                @RequestParam(value="photoFile", required = false) MultipartFile photoFile
    ) {
        // アルファベット（例: "B"）が取得できます（未選択の場合は "" や null）
        String prefix = request.getCategoryId(); 
        
        if (prefix == null || prefix.isEmpty()) {
            prefix = "B"; // 未指定時のデフォルト値処理など
        }

        // 新しい画像ファイルがアップロードされたかチェック
        if (photoFile != null && !photoFile.isEmpty()) {
            try {
                // 元のファイル名を取得
                String fileName = photoFile.getOriginalFilename();

                // 1. 保存先ディレクトリの絶対パスを取得 (プロジェクト直下の src/main/resources/static/img)
                Path uploadDir = Paths.get("src/main/resources/static/img").toAbsolutePath();
                
                // 2. ディレクトリが存在しない場合は自動作成
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                }

                // 3. ファイルの保存先フルパスを生成
                Path filePath = uploadDir.resolve(fileName);
                
                // 4. 絶対パスを指定してファイルを転送・保存
                photoFile.transferTo(filePath.toFile());

                // 5. DB保存用（HTML参照用）のパスをリクエストにセット
                request.setPhoto(fileName);

            } catch (IOException e) {
                e.printStackTrace();
                // エラーハンドリング（ログ出力や画面エラー表示）
            }
        } else {
            // 画像が選択されなかった場合は、既存の画像パスを維持する処理を記述
        }

        storeService.saveGoods(request);
        return "redirect:/w/editmenu";
    }

    /**
     * 販売停止 / 販売再開 ステータス切り替え処理
     */
    @PostMapping("/w/editmenu/toggle-sold-out")
    public String toggleSoldOut(@RequestParam("goodsId") String goodsId,
                                @RequestParam("soldOut") boolean soldOut,
                                RedirectAttributes redirectAttributes) {

        storeService.updateSoldOut(goodsId, soldOut);

        if (soldOut) {
            redirectAttributes.addFlashAttribute("message", "商品を販売停止にしました。");
        } else {
            redirectAttributes.addFlashAttribute("message", "商品の販売を再開しました。");
        }

        // メニュー編集画面のリダイレクト先URLを設定（必要に応じて書き換えてください）
        return "redirect:/w/editmenu";
    }

    @PostMapping("/custom/save")
    public String saveCustom(@RequestParam(value = "customId", required = false) Integer customId,
                            @RequestParam("categoryType") String categoryType, // "rice" または "sauce"
                            @RequestParam("goodsName") String goodsName,
                            @RequestParam("price") Integer price,
                            @RequestParam("calorie") Integer calorie,
                            @RequestParam("allergy") String allergy,
                            @RequestParam(value = "soldOut", defaultValue = "false") boolean soldOut,
                            @RequestParam(value = "goodsId", required = false) String goodsId) { 
        if (customId == null) {
            // 【新規追加】カテゴリーに応じてIDを採番
            if ("rice".equals(categoryType)) {
                customId = storeRepository.generateRiceCustomId();
            } else {
                customId = storeRepository.generateSauceCustomId();
            }
            storeRepository.insertCustom(customId, goodsName, price, calorie, allergy, soldOut);
        } else {
            // 【更新】既存IDに対してUPDATE
            storeRepository.updateCustom(customId, goodsName, price, calorie, allergy, soldOut);
        }

        return "redirect:/w/editmenu/edit?goodsId=" + goodsId; // トッピング一覧のURLへ
    }

    @PostMapping("/custom/delete")
    public String deleteCustom(@RequestParam("customId") Integer customId,
                            @RequestParam (value = "goodsId", required = false) String goodsId) {
        storeRepository.deleteCustom(customId);
        return "redirect:/w/editmenu/edit?goodsId=" + goodsId;
    }
    
}
