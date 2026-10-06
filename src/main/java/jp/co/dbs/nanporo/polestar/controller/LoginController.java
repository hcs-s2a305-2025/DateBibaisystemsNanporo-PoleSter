package jp.co.dbs.nanporo.polestar.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
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
    public String getLogin(Model model) {
        // リダイレクト経由のメッセージが届いていない場合、GETアクセス時も休業日チェックを実施
        if (!model.containsAttribute("closedMessage")) {
            String closeType = service.getCloseDay();
            if (closeType != null) {
                model.addAttribute(
                    "closedMessage", 
                    "本日は「" + closeType + "」のため、店長のみログイン可能です。"
                );
            }
        }
        return "login";
    }
    
    /**
     * ログイン成功時の権限判定・画面振り分け処理
     */
    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication,
                                HttpServletRequest request, 
                                RedirectAttributes redirectAttributes) {

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
        // 店長以外の場合、休業日判定を実施
        if (!tentyou) {
            // 休業日確認
            String closeType = service.getCloseDay();

            if (closeType != null) {
                // ★ ログインセッションを完全に破棄（ログアウト状態にする）
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }

                redirectAttributes.addFlashAttribute(
                    "closedMessage",
                    "本日は「" + closeType + "」のため、店長以外のログインを制限しております。"
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