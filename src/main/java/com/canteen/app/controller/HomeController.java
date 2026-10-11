package com.canteen.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/student";
    }

    @GetMapping("/portal")
    public String portal() {
        return "portal-select";
    }
}
