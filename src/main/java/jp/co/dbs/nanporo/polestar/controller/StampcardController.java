package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller 
public class StampcardController {

    @Autowired 
    UserService service;

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
