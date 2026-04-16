package com.master.finance.controller;

import com.master.finance.model.Bill;
import com.master.finance.service.BillService;
import com.master.finance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bills")
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String listBills(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("bills", billService.getUserBills(userId));
        model.addAttribute("pendingBills", billService.getPendingBills(userId));
        model.addAttribute("overdueBills", billService.getOverdueBills(userId));
        model.addAttribute("currentPage", "bills");
        model.addAttribute("pageSubtitle", "Manage your recurring bills and payments");
        return "bills/index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("bill", new Bill());
        model.addAttribute("currentPage", "bills");
        return "bills/add";
    }

    @PostMapping("/add")
    public String addBill(@ModelAttribute Bill bill, Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            bill.setUserId(getUserId(authentication));
            billService.saveBill(bill);
            redirectAttributes.addFlashAttribute("success", "Bill added successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/bills";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id, Authentication authentication,
                               Model model, RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        return billService.getBill(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(b -> {
                    model.addAttribute("bill", b);
                    model.addAttribute("currentPage", "bills");
                    return "bills/edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Bill not found");
                    return "redirect:/bills";
                });
    }

    @PostMapping("/edit/{id}")
    public String updateBill(@PathVariable String id, @ModelAttribute Bill bill,
                             Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            bill.setId(id);
            bill.setUserId(getUserId(authentication));
            billService.saveBill(bill);
            redirectAttributes.addFlashAttribute("success", "Bill updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/bills";
    }

    @GetMapping("/pay/{id}")
    public String markAsPaid(@PathVariable String id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        billService.getBill(id).ifPresentOrElse(bill -> {
            if (bill.getUserId().equals(userId)) {
                billService.markAsPaid(id);
                redirectAttributes.addFlashAttribute("success", "Bill marked as paid. Transaction recorded.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Access denied");
            }
        }, () -> redirectAttributes.addFlashAttribute("error", "Bill not found"));
        return "redirect:/bills";
    }

    @GetMapping("/delete/{id}")
    public String deleteBill(@PathVariable String id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        billService.getBill(id).ifPresentOrElse(bill -> {
            if (bill.getUserId().equals(userId)) {
                billService.deleteBill(id);
                redirectAttributes.addFlashAttribute("success", "Bill deleted");
            } else {
                redirectAttributes.addFlashAttribute("error", "Access denied");
            }
        }, () -> redirectAttributes.addFlashAttribute("error", "Bill not found"));
        return "redirect:/bills";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}