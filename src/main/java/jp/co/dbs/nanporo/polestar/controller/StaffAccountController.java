package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class StaffAccountController {

    @Autowired 
    private  UserService service;

    @GetMapping ("/w/account/staff")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        Principal principal, Model model) {

        UserGetResponse response = service.getStaffList(pageable);

        model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        return "w/account/staff";
    }
}
