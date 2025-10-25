package com.frontserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/document")
@RequiredArgsConstructor
public class DocumentController {

    /**
     * 문서 페이지
     */
    @GetMapping
    public String documentList(Model model) {
        return "document/list";
    }
}
