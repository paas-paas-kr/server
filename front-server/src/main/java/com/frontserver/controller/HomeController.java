package com.frontserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        // 처음 접속 시 로그인 페이지로 리다이렉트
        // (프론트엔드에서 로그인 체크 및 리다이렉트 처리)
        return "redirect:/auth/login";
    }

    @GetMapping("/home")
    public String home() {
        return "home/index";
    }
}
