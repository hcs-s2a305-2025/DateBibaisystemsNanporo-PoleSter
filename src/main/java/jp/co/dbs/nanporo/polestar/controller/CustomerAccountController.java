package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Pageable;

import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class CustomerAccountController {
    
    @Autowired 
    private  UserService service;

    // 顧客管理画面表示
    @GetMapping ("/w/account/customer")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "asc") String sort,
        Principal principal, Model model) {

        // UserGetResponse response = service.getStaffList(pageable, sort);

        // model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", sort);
        return "w/account/customer";
    }
}
