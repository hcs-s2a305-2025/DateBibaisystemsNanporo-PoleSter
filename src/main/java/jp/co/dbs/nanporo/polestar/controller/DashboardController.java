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

/**
 * 店舗管理画面のダッシュボード機能および即時アクション（臨時休業、全顧客向け一括通知）のリクエストを処理するコントローラークラス。
 */
@Controller 
public class DashboardController {

    /** ユーザおよび店舗集計機能を提供するサービス */
    @Autowired 
    UserService service;

    /**
     * ダッシュボード画面を表示します。
     * 本日の注文件数および現在時間帯の売上フラッシュ情報を取得して画面に設定します。
     *
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return ダッシュボード画面のテンプレートパス ("w/dashboard")
     */
    @GetMapping("/w/dashboard")
    public String getDashboard(Principal principal, Model model) {

        int orderCnt = service.countOrder();
        SalesFlashDto salesFlash = service.getHourlySalesFlash();

        model.addAttribute("orderCnt", orderCnt);
        model.addAttribute("salesFlash", salesFlash);

        return "w/dashboard";
    }

    /**
     * 当日（本日）付での「臨時休業」を一括登録します。
     * すでに本日の臨時休業が登録済みの場合は重複エラー（409 Conflict）を返却します。
     *
     * @return 処理結果レスポンス
     *         
     * {@code 200 OK} : 登録成功 ("OK")
     * {@code 409 CONFLICT} : すでに登録済み ("Already Closed")
     * {@code 500 INTERNAL_SERVER_ERROR} : システムエラー ("Error")
     *      
     */
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

    /**
     * 全顧客ユーザに対して一括でお知らせ（通知）を送信します。
     *
     * @param content 送信する通知メッセージの内容
     * @return 処理結果レスポンス
     *         
     *  {@code 200 OK} : 送信成功 ("OK")
     *  {@code 500 INTERNAL_SERVER_ERROR} : 送信失敗・システムエラー ("Error")
     *  
     */
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
