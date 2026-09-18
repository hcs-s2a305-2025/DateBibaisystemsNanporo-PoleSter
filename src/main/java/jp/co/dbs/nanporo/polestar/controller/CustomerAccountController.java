package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.data.domain.Pageable;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class CustomerAccountController {
    
    @Autowired 
    private  UserService service;

    // 顧客管理画面表示
    @PreAuthorize("hasAuthority('店長')")
    @GetMapping ("/w/account/customer")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "asc") String sort,
        Principal principal, Model model) {

        UserGetResponse response = service.getCustomerList(pageable, sort);

        model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", sort);
        return "w/account/customer";
    }

    // 編集データ取得（１件）
    @GetMapping("/w/account/customer/detail")
    @ResponseBody
    public UserEntity getCustomerDetail(@RequestParam("mail") String mail) {
        return service.findByMail(mail);
    }

    // 削除処理
    @PostMapping("/w/account/customer/delete")
    @ResponseBody
    public ResponseEntity<String> deleteCustomer(@RequestParam("mail") String mail) {
        service.deleteUser(mail);
        return ResponseEntity.ok("OK");
    }

    // 停止処理
    @PostMapping("/w/account/customer/stop")
    @ResponseBody
    public ResponseEntity<String> stopCustomer(@RequestParam("mail") String mail) {
        service.stopUser(mail);
        return ResponseEntity.ok("OK");
    }

    // 解除処理
    @PostMapping("/w/account/customer/resume")
    @ResponseBody
    public ResponseEntity<String> resumeCustomer(@RequestParam("mail") String mail) {
        service.resumeUser(mail);
        return ResponseEntity.ok("OK");
    }
}
