package jp.co.dbs.nanporo.polestar.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.service.StoreService;

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
}
