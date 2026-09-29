package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.data.domain.Pageable;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * 店舗管理側の顧客アカウント管理機能に関するリクエストを処理するコントローラークラス。
 */
@Controller 
public class CustomerAccountController {
    
    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    private  UserService service;

    /**
     * 顧客アカウント管理画面を表示します。
     * ページネーションおよびソート条件に基づいて顧客一覧を取得し、画面へ渡します（アクセス制限: 店長のみ）。
     *
     * @param pageable ページネーション情報（デフォルト: 1ページあたり10件）
     * @param sort ソート順の指定（デフォルト: "asc"）
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return 顧客管理画面のテンプレートパス ("w/account/customer")
     */
    @PreAuthorize("hasAuthority('店長')")
    @GetMapping ("/w/account/customer")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "asc") String sort,
        Principal principal, Model model) {

        UserGetResponse response = service.getCustomerList(pageable, sort);

        model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", sort);
        return "w/account/customer";
    }

    /**
     * 編集画面・ダイアログ用に対象顧客の詳細情報（1件）を取得します。
     *
     * @param mail 取得対象顧客のメールアドレス
     * @return 該当する顧客の {@link UserEntity}（JSON形式）
     */
    @GetMapping("/w/account/customer/detail")
    @ResponseBody
    public UserEntity getCustomerDetail(@RequestParam("mail") String mail) {
        return service.findByMail(mail);
    }

    /**
     * 指定された顧客アカウントを物理削除します。
     *
     * @param mail 削除対象顧客のメールアドレス
     * @return 処理結果レスポンス（成功時: "OK"）
     */
    @PostMapping("/w/account/customer/delete")
    @ResponseBody
    public ResponseEntity<String> deleteCustomer(@RequestParam("mail") String mail) {
        service.deleteUser(mail);
        return ResponseEntity.ok("OK");
    }

    /**
     * 指定された顧客アカウントの利用を一時停止状態に更新します。
     *
     * @param mail 停止対象顧客のメールアドレス
     * @return 処理結果レスポンス（成功時: "OK"）
     */
    @PostMapping("/w/account/customer/stop")
    @ResponseBody
    public ResponseEntity<String> stopCustomer(@RequestParam("mail") String mail) {
        service.stopUser(mail);
        return ResponseEntity.ok("OK");
    }

    /**
     * 利用停止中となっている顧客アカウントの停止状態を解除します。
     *
     * @param mail 解除対象顧客のメールアドレス
     * @return 処理結果レスポンス（成功時: "OK"）
     */
    @PostMapping("/w/account/customer/resume")
    @ResponseBody
    public ResponseEntity<String> resumeCustomer(@RequestParam("mail") String mail) {
        service.resumeUser(mail);
        return ResponseEntity.ok("OK");
    }
}
