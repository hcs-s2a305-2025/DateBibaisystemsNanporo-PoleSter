package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

/**
 * 店舗管理側の従業員アカウント管理機能（一覧表示・登録・編集・削除）に関するリクエストを処理するコントローラークラス。
 */
@Controller 
public class StaffAccountController {

    /** ユーザ関連のビジネスロジックを提供するサービス */
    @Autowired 
    private  UserService service;

    /**
     * 従業員アカウント管理画面を表示します。
     * ページネーションおよびソート条件に基づいて従業員一覧を取得し、画面へ渡します（アクセス制限: 店長のみ）。
     *
     * @param pageable ページネーション情報（デフォルト: 1ページあたり10件）
     * @param sort ソート順の指定（デフォルト: "asc"）
     * @param principal ログイン中のユーザ情報
     * @param model 画面描画用のモデルオブジェクト
     * @return 従業員管理画面のテンプレートパス ("w/account/staff")
     */
    @PreAuthorize("hasAuthority('店長')")
    @GetMapping ("/w/account/staff")
    public  String getStaffAccount(
        @PageableDefault(size=10) Pageable pageable,
        @RequestParam(name = "sort", defaultValue = "asc") String sort,
        Principal principal, Model model) {

        UserGetResponse response = service.getStaffList(pageable, sort);

        model.addAttribute("response", response);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("sort", sort);
        return "w/account/staff";
    }

    /**
     * 編集画面・ダイアログ用に対象従業員の詳細情報（1件）を取得します。
     *
     * @param mail 取得対象従業員のメールアドレス
     * @return 該当する従業員の {@link UserEntity}（JSON形式）
     */
    @GetMapping("/w/account/staff/detail")
    @ResponseBody
    public UserEntity getStaffDetail(@RequestParam("mail") String mail) {
        return service.findByMail(mail);
    }

    /**
     * 指定された従業員の基本情報（氏名・役割・利用状態）を更新します。
     *
     * @param mail 対象従業員のメールアドレス
     * @param name 新しい氏名
     * @param role 新しい役割（権限）
     * @param alive 利用状態フラグ（true: 無効/停止, false: 有効）
     * @return 処理結果レスポンス（成功時: "OK"）
     */
    @PostMapping("/w/account/staff/update")
    @ResponseBody
    public ResponseEntity<String> updateStaff(
            @RequestParam("mail") String mail,
            @RequestParam("name") String name,
            @RequestParam("role") String role,
            @RequestParam("alive") boolean alive) {

        service.updateStaff(mail, name, role, alive);
        return ResponseEntity.ok("OK");
    }

    /**
     * 指定された従業員アカウントを物理削除します。
     *
     * @param mail 削除対象従業員のメールアドレス
     * @return 処理結果レスポンス（成功時: "OK"）
     */
    @PostMapping("/w/account/staff/delete")
    @ResponseBody
    public ResponseEntity<String> deleteStaff(@RequestParam("mail") String mail) {
        service.deleteUser(mail);
        return ResponseEntity.ok("OK");
    }

    /**
     * 新しい従業員アカウントを登録します。
     * すでに登録済みのメールアドレスを指定した場合は重複エラー（409 Conflict）を返却します。
     *
     * @param mail 登録するメールアドレス
     * @param name 氏名
     * @param role 役割（権限）
     * @return 処理結果レスポンス
     * 
     *  {@code 200 OK} : 登録成功 ("OK")
     *  {@code 409 CONFLICT} : すでに登録済みのメールアドレス ("Already Exists")
     *  {@code 500 INTERNAL_SERVER_ERROR} : システムエラー ("Error")
     *  
     */
    @PostMapping("/w/account/staff/register")
    @ResponseBody
    public ResponseEntity<String> registerStaff(
            @RequestParam("mail") String mail,
            @RequestParam("name") String name,
            @RequestParam("role") String role) {
        
        try {
            service.registerStaff(mail, name, role);
            return ResponseEntity.ok("OK");
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // UNIQUE違反 409
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Already Exists");
        } catch (Exception e) {
            // その他 500
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }
}
