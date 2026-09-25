package jp.co.dbs.nanporo.polestar.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jp.co.dbs.nanporo.polestar.data.AllergenData;
import jp.co.dbs.nanporo.polestar.data.CategoryData;
import jp.co.dbs.nanporo.polestar.data.CustomData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;
import jp.co.dbs.nanporo.polestar.service.StoreService;
import org.springframework.web.bind.annotation.RequestParam;


@Controller 
public class StoreController {
    
    @Autowired 
    private StoreService storeService;

    @GetMapping("/menu")
    public String showMenu(Model model) {
        List<GoodsData> menuList = storeService.getMenuList();
        model.addAttribute("menuList", menuList);
        return "menu";
    }

    @GetMapping("/w/polesterpos")
    public String showPos(Model model) {
        return "w/polesterpos";
    }

    @GetMapping("/w/casherhistory")
    public String showCashHistory(Model model) {
        return "w/casherhistory";
    }
    
    @GetMapping("/w/editmenu")
    public String shoeEditMenu(Model model) {
        List<GoodsData> menuList = storeService.getMenuList();
        model.addAttribute("menuList", menuList);
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
    public String updateProduct(@ModelAttribute GoodsEditRequest request) {
        storeService.updateGoods(request);
        return "redirect:/w/editmenu";
    }
    
}
