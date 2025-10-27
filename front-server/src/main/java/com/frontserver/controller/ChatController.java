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
        model.addAttribute("threadId", threadId);
        return "chat/room";
    }
}
