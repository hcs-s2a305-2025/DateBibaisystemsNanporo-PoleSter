package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * ログインユーザ自身のプロフィール設定・変更および定休日スケジュール設定に関するリクエストを処理するコントローラークラス。
 */
@Controller 
public class SettingController {
    
    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    private  UserService service;

    /**
     * プロフィール設定画面を表示します。
     *
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return プロフィール画面のテンプレートパス ("settings")
     */
    @GetMapping ("/settings")
    public  String getSetting(
        Principal principal, Model model) {

        String mail = principal.getName();
        UserEntity user = service.findByMail(mail);

        model.addAttribute("user", user);
        return "settings";
    }

    /**
     * 定休日の自動スケジュール設定（52週分の一括登録）を更新します。
     *
     * @param enabled 定休日の有効/無効フラグ（デフォルト: false）
     * @param stopDay 対象の曜日文字列（例: "月", "火" など）
     * @param redirectAttributes リダイレクト先へデータを引き継ぐための属性オブジェクト
     * @return 設定画面へのリダイレクトパス ("redirect:/settings")
     */
    @PostMapping("/settings/schedule")
    public String updateSchedule(
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            @RequestParam(name = "stop_day", required = false) String stopDay,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (enabled) { // ON
                service.closeDays(stopDay, 52);
            }
            redirectAttributes.addAttribute("success", true);
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", true);
        }

        return "redirect:/settings";
    }

    /**
     * ユーザ情報編集画面を表示します。
     *
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return ユーザ情報編集画面のテンプレートパス ("settings/edit")
     */
    @GetMapping ("/settings/edit")
    public  String getSettingEdit(
        Principal principal, Model model) {

        String mail = principal.getName();
        UserEntity user = service.findByMail(mail);

        model.addAttribute("user", user);
        return "settings/edit";
    }

    /**
     * ログインユーザの基本情報（名前、メールアドレス、パスワード）を更新します。
     * パスワード入力の有無を判定し、適切な更新処理を呼び出します。
     * また、メールアドレスが変更された場合は Spring Security の認証情報（SecurityContext）を再設定します。
     *
     * @param icon アイコン
     * @param name 新しい名前
     * @param mailAddress 新しいメールアドレス
     * @param oldPassword 現在のパスワード（任意）
     * @param newPassword 新しいパスワード（任意）
     * @param newPasswordConf 新しいパスワード（確認用・任意）
     * @param redirectAttributes リダイレクト先へデータを引き継ぐための属性オブジェクト
     * @param principal ログイン中のユーザ情報
     * @return 処理成功時はプロフィール画面へのリダイレクトパス、失敗時は編集画面へのリダイレクトパス
     */
    @PostMapping("/settings/edit/update")
    public String updateProfile(
        @RequestParam (name = "icon") String icon,
        @RequestParam(name = "name") String name,
        @RequestParam(name = "mailAddress") String mailAddress,
        @RequestParam(name = "oldPassword", required = false) String oldPassword,
        @RequestParam(name = "newPassword", required = false) String newPassword,
        @RequestParam(name = "newPasswordConf", required = false) String newPasswordConf,
        RedirectAttributes redirectAttributes,
        Principal principal
    ) {
        try {
            String nowMail = principal.getName();
            if(oldPassword.isEmpty() && newPassword.isEmpty() && newPasswordConf.isEmpty()) { // パスワード変更なし

                service.updateNoPassword(mailAddress, nowMail, name, icon);

            } else { // パスワード変更あり

                if(oldPassword.isEmpty() || newPassword.isEmpty() || newPasswordConf.isEmpty()) {
                    redirectAttributes.addAttribute("passwordNullError", true);
                }

                // 現在のパスワードの確認
                boolean result = service.passwordCheck(nowMail, oldPassword);
                if(!result) {
                    redirectAttributes.addAttribute("oldPasswordError", true);
                    return "redirect:/settings/edit";
                }

                // 新パスワードの確認
                if(!newPassword.equals(newPasswordConf)) {
                    redirectAttributes.addAttribute("newPasswordError", true);
                    return "redirect:/settings/edit";
                }

                service.updateYesPassword(mailAddress, nowMail, name, newPassword,icon);
            }

            // メルアド変更の場合
            if(!nowMail.equals(mailAddress)) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    mailAddress, // 新しいメルアド
                    auth.getCredentials(),
                    auth.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            return "redirect:/settings";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", true);
            return "redirect:/settings/edit";
        }
    }
}
