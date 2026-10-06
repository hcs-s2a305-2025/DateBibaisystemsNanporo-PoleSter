package jp.co.dbs.nanporo.polestar.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.service.UserService;


@Controller 
public class LoginController {

    private final AuthenticationManager authenticationManager;
    @Autowired 
    UserService service;

    LoginController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }
    
    /**
     * ログイン成功時の権限判定・画面振り分け処理
     */
    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication, RedirectAttributes redirectAttributes) {

        boolean tentyou = false;
        boolean tenin = false;

        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if (role.contains("店長")) {
                    tentyou = true;
                } else if (role.contains("店員")) {
                    tenin = true;
                }
            }
        }

        if (!tentyou) {
            // 休業日確認
            String closeType = service.getCloseDay();

            if (closeType != null) {
                redirectAttributes.addFlashAttribute(
                    "closedMessage",
                    "本日は「" + closeType + "」のため、システムを休止しております。"
                );
                return "redirect:/login";
            }
        }

        // 権限に「店長」または「店員」が含まれている場合
        if (tentyou || tenin) {
            return "redirect:/w/home"; // w/home.html 用のURLへ
        }
        
        // 顧客または権限なしの場合は一般用ホーム画面へ
        return "redirect:/";
    }
}