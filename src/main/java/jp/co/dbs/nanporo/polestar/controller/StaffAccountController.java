package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class StaffAccountController {

    @Autowired 
    private  UserService service;

    // 従業員管理画面表示
    // @PreAuthorize("hasAuthority('店長')")
    @GetMapping ("/w/account/staff")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "asc") String sort,
        Principal principal, Model model) {

        UserGetResponse response = service.getStaffList(pageable, sort);

        model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", sort);
        return "w/account/staff";
    }

    // 編集データ取得（１件）
    @GetMapping("/w/account/staff/detail")
    @ResponseBody
    public UserEntity getStaffDetail(@RequestParam("mail") String mail) {
        return service.findByMail(mail);
    }

    // 編集処理
    @PostMapping("/w/account/staff/update")
    @ResponseBody
    public ResponseEntity<String> updateStaff(
            @RequestParam("mail") String mail,
            @RequestParam("name") String name,
            @RequestParam("role") String role,
            @RequestParam("alive") boolean alive) {

        service.updateStaff(mail, name, role, alive);
        return ResponseEntity.ok("OK");
    }

    // 削除処理
    @PostMapping("/w/account/staff/delete")
    @ResponseBody
    public ResponseEntity<String> deleteStaff(@RequestParam("mail") String mail) {
        service.deleteStaff(mail);
        return ResponseEntity.ok("OK");
    }

    // 新規登録処理
    @PostMapping("/w/account/staff/register")
    @ResponseBody
    public ResponseEntity<String> registerStaff(
            @RequestParam("mail") String mail,
            @RequestParam("name") String name,
            @RequestParam("role") String role) {
        
        try {
            service.registerStaff(mail, name, role);
            return ResponseEntity.ok("OK");
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // UNIQUE違反 409
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already Exists");
        } catch (Exception e) {
            // その他 500
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
