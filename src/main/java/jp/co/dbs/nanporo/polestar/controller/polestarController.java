package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class polestarController {

    @GetMapping("/")
    public String home() {
        return "Polestar System (DBS Nanporo) is Running!";
    }
}