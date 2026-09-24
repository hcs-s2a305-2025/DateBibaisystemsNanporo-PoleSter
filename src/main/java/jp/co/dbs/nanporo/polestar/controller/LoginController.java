package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller 
public class LoginController {

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }
    
    /**
     * ログイン成功時の権限判定・画面振り分け処理
     */
    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                
                // 権限に「店長」または「店員」が含まれている場合
                if (role.contains("店長") || role.contains("店員")) {
                    return "redirect:/w/home"; // w/home.html 用のURLへ
                }
            }
        }
        
        // 顧客または権限なしの場合は一般用ホーム画面へ
        return "redirect:/";
    }
}