package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.co.dbs.nanporo.polestar.repository.UserRepository.SalesFlashDto;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class DashboardController {

    @Autowired 
    UserService service;

    @GetMapping("/w/dashboard")
    public String getDashboard(Principal principal, Model model) {

        int orderCnt = service.countOrder();
        SalesFlashDto salesFlash = service.getHourlySalesFlash();

        model.addAttribute("orderCnt", orderCnt);
        model.addAttribute("salesFlash", salesFlash);

        return "w/dashboard";
    }

    @PostMapping("/w/dashboard/close")
    @ResponseBody
    public ResponseEntity<String> closeSystem() {
        try {
            service.insertClose();
            return ResponseEntity.ok("OK");
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // UNIQUE違反 409
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already Closed");
        } catch (Exception e) {
            // その他 500
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    @PostMapping("/w/dashboard/notice/broadcast")
    @ResponseBody
    public ResponseEntity<String> sendBroadcastNotice(@RequestParam("content") String content) {
        try {
            service.sendBroadcastNotice(content);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
