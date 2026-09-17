package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class polestarController {

    @GetMapping("/")
    public String home() {
        return "home";
    }
}