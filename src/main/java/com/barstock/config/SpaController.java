package com.barstock.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {
    @RequestMapping(value = {"/", "/stock", "/products", "/suppliers", "/invoices", "/users", "/login", "/register"})
    public String forward() { return "forward:/index.html"; }
}
