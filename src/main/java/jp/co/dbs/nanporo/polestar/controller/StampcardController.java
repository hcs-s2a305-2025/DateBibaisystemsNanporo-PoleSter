package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * 顧客向けのスタンプカード画面（ポイント・会員ランク情報の表示）に関するリクエストを処理するコントローラークラス。
 */
@Controller 
public class StampcardController {

    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    UserService service;

    /**
     * スタンプカード画面を表示します。
     * ログイン中のユーザ情報（ポイント数、達成枚数、会員ランク）を取得し、次のランク昇格までに必要なスタンプカード達成枚数を計算して画面へ渡します。
     *
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return スタンプカード画面のテンプレートパス ("stampcard")
     */
    @GetMapping ("/stampcard")
    public String getStampcard(Principal principal, Model model) {

        String mail = principal.getName();
        UserEntity response = service.findByMail(mail);

        String rank = response.getMemberRank();
        int maisu = response.getPointCardComplete();

        int nextRankProgress = 0;
        if ("一般".equals(rank)) {
            nextRankProgress = 1 - maisu;
        } else if ("ブロンズ".equals(rank)) {
            nextRankProgress = 3 - maisu;
        } else if ("シルバー".equals(rank)) {
            nextRankProgress = 5 - maisu;
        }

        model.addAttribute("nextRankProgress", nextRankProgress);
        model.addAttribute("user", response);
        return "stampcard";
    }
}
