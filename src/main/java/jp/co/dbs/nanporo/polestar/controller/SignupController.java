package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class SignupController {

    @Autowired 
    UserService service;

    @GetMapping ("/signup")
    public String getSignup() {
        return "signup";
    }

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
