package com.frontserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @GetMapping
    public String chatList(Model model) {
        return "chat/list";
    }

    @GetMapping("/{threadId}")
    public String chatRoom(@PathVariable String threadId, Model model) {
        // ChatGPT 스타일: 모든 채팅은 list.html에서 처리
        // threadId가 있으면 list.html에서 해당 채팅방을 자동 선택하도록 리다이렉트
        return "redirect:/chat";
    }
}
