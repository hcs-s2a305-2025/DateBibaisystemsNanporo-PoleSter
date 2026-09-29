package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.StoreService;
import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * AIメニュー提案機能に関するリクエストを処理するコントローラークラス。
 */
@Controller 
public class AiMenuController {

    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    UserService service;

    /** 店舗・商品関連のビジネスロジックを提供するサービス */
    @Autowired 
    StoreService storeService;

    /**
     * ユーザからの入力（希望や質問）を受け取り、AIによるメニュー提案結果を生成してメニュー画面へリダイレクトします。
     *
     * @param userPrompt ユーザが入力した質問・希望内容テキスト
     * @param principal ログイン中のユーザ情報
     * @param redirectAttributes リダイレクト先へデータを引き継ぐための属性オブジェクト
     * @return メニュー画面（/menu）へのリダイレクトパス
     */
    @PostMapping ("/menu/ai")
    public String postAiMenu(@RequestParam("userPrompt") String userPrompt,
                            Principal principal, RedirectAttributes redirectAttributes) {
        
        String mail = principal.getName();
        UserEntity response = service.findByMail(mail);
        String result = service.getAi(response, userPrompt);

        redirectAttributes.addFlashAttribute("aiAnswer", result);
        redirectAttributes.addFlashAttribute("userPrompt", userPrompt);
        return "redirect:/menu";
    }
}