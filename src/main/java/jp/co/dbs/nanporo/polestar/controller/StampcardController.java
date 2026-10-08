package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    /**
     * 割引券を使用して、店員への提示画面に切り替えます。
     */
    @PostMapping("/stampcard/use")
    public String useCoupon(Principal principal, RedirectAttributes redirectAttributes) {
        if (!service.activateStampCoupon(principal.getName())) {
            redirectAttributes.addFlashAttribute("couponError", "使用できる割引カードがありません。");
        }
        return "redirect:/stampcard";
    }

    /**
     * 提示中の割引を取り消し、割引券を戻します。
     */
    @PostMapping("/stampcard/cancel")
    public String cancelCoupon(Principal principal) {
        service.cancelStampCoupon(principal.getName());
        return "redirect:/stampcard";
    }
}
