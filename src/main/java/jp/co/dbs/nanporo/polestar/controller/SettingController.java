package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;
import org.springframework.web.bind.annotation.RequestBody;


@Controller 
public class SettingController {
    
    @Autowired 
    private  UserService service;

    // プロフィール画面表示
    @GetMapping ("/settings")
    public  String getSetting(
        Principal principal, Model model) {

        String mail = principal.getName();
        UserEntity user = service.findByMail(mail);

        model.addAttribute("user", user);
        return "settings";
    }

    // システム自動停止
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

    // ユーザ情報編集画面表示
    @GetMapping ("/settings/edit")
    public  String getSettingEdit(
        Principal principal, Model model) {

        String mail = principal.getName();
        UserEntity user = service.findByMail(mail);

        model.addAttribute("user", user);
        return "settings/edit";
    }

    // ユーザ情報編集
    @PostMapping("/settings/edit/update")
    public String updateProfile(
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
            if(oldPassword != null) { // パスワード変更なし

                service.updateNoPassword(name, nowMail, mailAddress);

            } else { // パスワード変更あり

                // 現在のパスワードの確認
                boolean result = service.passwordCheck(mailAddress, oldPassword);
                if(!result) {
                    redirectAttributes.addAttribute("oldPasswordError", true);
                    return "redirect:/settings/edit";
                }

                // 新パスワードの確認
                if(!newPassword.equals(newPasswordConf)) {
                    redirectAttributes.addAttribute("newPasswordError", true);
                    return "redirect:/settings/edit";
                }

                service.updateYesPassword(name, nowMail, mailAddress, newPassword);
            }

            return "redirect:settings";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", true);
            return "redirect:/settings/edit";
        }
    }
}
