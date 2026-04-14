package com.master.finance.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == 404) {
                model.addAttribute("title", "Page Not Found");
                return "error/404";
            } else if (statusCode == 500) {
                model.addAttribute("title", "Server Error");
                return "error/500";
            }
        }
        model.addAttribute("title", "Error");
        return "error/500";
    }

    @GetMapping("/error/offline")
    public String offline(Model model) {
        model.addAttribute("title", "Offline");
        return "error/offline";
    }

    @GetMapping("/error/maintenance")
    public String maintenance(Model model) {
        model.addAttribute("title", "Maintenance");
        return "error/maintenance";
    }
}