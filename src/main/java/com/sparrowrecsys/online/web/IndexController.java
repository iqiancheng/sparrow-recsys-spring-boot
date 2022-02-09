package com.sparrowrecsys.online.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class IndexController {

    @RequestMapping("/")
    public String hello(){
        return "index.html";
    }
}
