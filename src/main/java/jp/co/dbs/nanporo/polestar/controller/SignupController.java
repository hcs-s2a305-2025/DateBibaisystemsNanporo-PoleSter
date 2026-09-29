package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * 顧客の新規会員登録（サインアップ）および登録後の初期プロフィール設定処理を担当するコントローラークラス。
 */
@Controller 
public class SignupController {

    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    UserService service;

    /**
     * 新規会員登録画面を表示します。
     *
     * @return 会員登録画面のテンプレートパス ("signup")
     */
    @GetMapping ("/signup")
    public String getSignup() {
        return "signup";
    }

    /**
     * 新規顧客アカウントの登録を行います。
     * 入力値の未入力チェック・パスワード確認の一致チェックを行い、問題がなければアカウントを作成します。
     * メールアドレスが重複している場合は一意制約違反（{@link DataIntegrityViolationException}）をキャッチしてエラーパラメータを付与します。
     *
     * @param mail 登録するメールアドレス
     * @param password パスワード
     * @param passwordConfirm パスワード（確認用）
     * @param redirectAttributes リダイレクト先へエラーメッセージ等のパラメータを引き継ぐためのオブジェクト
     * @return サインアップ画面へのリダイレクトパス ("redirect:/signup")
     */
    @PostMapping ("/signup")
    public String postSignup(
        @RequestParam ("mail") String mail,
        @RequestParam ("password") String password,
        @RequestParam ("passwordConfirm") String passwordConfirm,
        RedirectAttributes redirectAttributes){

            if(mail.isBlank()) {// メルアド未入力
                redirectAttributes.addAttribute("mailNullError", true);
                return "redirect:/signup";
            } else if (password.isBlank()) { // パスワード未入力
                redirectAttributes.addAttribute("passwordNullError", true);
                return "redirect:/signup";
            }
            
            if(!password.equals(passwordConfirm)) {
                redirectAttributes.addAttribute("passwordError", true);
                return "redirect:/signup";
            }

        try {
            service.registerCustomer(mail, password);

            redirectAttributes.addAttribute("success", true);
            redirectAttributes.addAttribute("mail", mail);
            return "redirect:/signup";
        
        } catch (DataIntegrityViolationException e) {
            // 登録済み
            redirectAttributes.addAttribute("sameMailError", true);
            return "redirect:/signup";
        
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", true);
            return "redirect:/signup";
        }
    }

    /**
     * 新規登録した顧客の追加プロフィール情報（性別・生年月日）を更新し、ログイン画面へ遷移します。
     *
     * @param mail 対象ユーザのメールアドレス
     * @param gender 性別
     * @param birthday 生年月日（"YYYY-MM-DD" フォーマットの文字列）
     * @param redirectAttributes リダイレクト属性オブジェクト
     * @return ログイン画面へのリダイレクトパス ("redirect:/login")
     */
    @PostMapping ("/signup/profile")
    public String updateProfile(
        @RequestParam ("mail") String mail,
        @RequestParam ("gender") String gender,
        @RequestParam ("birthday") String birthday,
        RedirectAttributes redirectAttributes) {
        
        service.updateProfile(mail, gender, birthday);

        return "redirect:/login";
    }
}
