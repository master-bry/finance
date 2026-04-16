package com.master.finance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomNumberEditor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.master.finance.model.Investment;
import com.master.finance.service.InvestmentService;
import com.master.finance.service.UserService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/investments")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    @Autowired
    private UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Double.class, new CustomNumberEditor(Double.class, true));
    }

    @GetMapping
    public String listInvestments(Authentication authentication, Model model) {
        String userId = getUserId(authentication);
        model.addAttribute("investments", investmentService.getUserInvestments(userId));
        model.addAttribute("totalInvested", investmentService.getTotalInvested(userId));
        model.addAttribute("totalCurrentValue", investmentService.getTotalCurrentValue(userId));
        model.addAttribute("totalProfitLoss", investmentService.getTotalProfitLoss(userId));

        model.addAttribute("currentPage", "investments");
        model.addAttribute("pageSubtitle", "Manage your investment portfolio");
        model.addAttribute("title", "Investments");

        return "investments/index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("investment")) {
            model.addAttribute("investment", new Investment());
        }
        model.addAttribute("currentPage", "investments");
        model.addAttribute("pageSubtitle", "Add a new investment");
        model.addAttribute("title", "Add Investment");
        return "investments/add";
    }

    @PostMapping("/add")
    public String addInvestment(@Valid @ModelAttribute("investment") Investment investment,
                                BindingResult result,
                                Authentication authentication,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("investment", investment);
            model.addAttribute("currentPage", "investments");
            model.addAttribute("pageSubtitle", "Add a new investment");
            model.addAttribute("title", "Add Investment");
            return "investments/add";
        }
        try {
            String userId = getUserId(authentication);
            investment.setUserId(userId);
            investmentService.saveInvestment(investment);
            redirectAttributes.addFlashAttribute("success", "Investment added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add investment: " + e.getMessage());
        }
        return "redirect:/investments";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable String id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        return investmentService.getInvestment(id)
                .filter(investment -> investment.getUserId().equals(userId))
                .map(investment -> {
                    model.addAttribute("investment", investment);
                    model.addAttribute("currentPage", "investments");
                    model.addAttribute("pageSubtitle", "Edit investment details");
                    model.addAttribute("title", "Edit Investment");
                    return "investments/edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Investment not found or access denied.");
                    return "redirect:/investments";
                });
    }

    @PostMapping("/edit/{id}")
    public String updateInvestment(@PathVariable String id,
                                   @Valid @ModelAttribute("investment") Investment investment,
                                   BindingResult result,
                                   Authentication authentication,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("investment", investment);
            model.addAttribute("currentPage", "investments");
            model.addAttribute("pageSubtitle", "Edit investment details");
            model.addAttribute("title", "Edit Investment");
            return "investments/edit";
        }
        try {
            String userId = getUserId(authentication);
            investmentService.getInvestment(id).ifPresentOrElse(existing -> {
                if (existing.getUserId().equals(userId)) {
                    investment.setId(id);
                    investment.setUserId(userId);
                    investmentService.saveInvestment(investment);
                    redirectAttributes.addFlashAttribute("success", "Investment updated successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Access denied.");
                }
            }, () -> redirectAttributes.addFlashAttribute("error", "Investment not found."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
        }
        return "redirect:/investments";
    }

    @GetMapping("/delete/{id}")
    public String softDeleteInvestment(@PathVariable String id,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            investmentService.getInvestment(id).ifPresentOrElse(investment -> {
                if (investment.getUserId().equals(userId)) {
                    investmentService.softDeleteInvestment(id);
                    redirectAttributes.addFlashAttribute("success", "Investment deleted successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Access denied.");
                }
            }, () -> redirectAttributes.addFlashAttribute("error", "Investment not found."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Delete failed: " + e.getMessage());
        }
        return "redirect:/investments";
    }

    @GetMapping("/update-value/{id}")
    public String showUpdateValueForm(@PathVariable String id,
                                      Authentication authentication,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        return investmentService.getInvestment(id)
                .filter(investment -> investment.getUserId().equals(userId))
                .map(investment -> {
                    model.addAttribute("investment", investment);
                    model.addAttribute("currentPage", "investments");
                    model.addAttribute("pageSubtitle", "Update current value");
                    model.addAttribute("title", "Update Value");
                    return "investments/update-value";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Investment not found.");
                    return "redirect:/investments";
                });
    }

    @PostMapping("/update-value/{id}")
    public String updateValue(@PathVariable String id,
                              @RequestParam Double currentValue,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            investmentService.getInvestment(id).ifPresentOrElse(investment -> {
                if (investment.getUserId().equals(userId)) {
                    investmentService.updateCurrentValue(id, currentValue);
                    redirectAttributes.addFlashAttribute("success", "Investment value updated successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Access denied.");
                }
            }, () -> redirectAttributes.addFlashAttribute("error", "Investment not found."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Update failed: " + e.getMessage());
        }
        return "redirect:/investments";
    }

    @GetMapping("/add-transaction/{id}")
    public String showAddTransactionForm(@PathVariable String id,
                                         Authentication authentication,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        String userId = getUserId(authentication);
        return investmentService.getInvestment(id)
                .filter(investment -> investment.getUserId().equals(userId))
                .map(investment -> {
                    model.addAttribute("investment", investment);
                    model.addAttribute("currentPage", "investments");
                    model.addAttribute("pageSubtitle", "Add investment transaction");
                    model.addAttribute("title", "Add Transaction");
                    return "investments/add-transaction";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Investment not found.");
                    return "redirect:/investments";
                });
    }

    @PostMapping("/add-transaction/{id}")
    public String addTransaction(@PathVariable String id,
                                 @RequestParam String type,
                                 @RequestParam Double amount,
                                 @RequestParam String description,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            String userId = getUserId(authentication);
            investmentService.getInvestment(id).ifPresentOrElse(investment -> {
                if (investment.getUserId().equals(userId)) {
                    investmentService.addTransaction(id, type, amount, description);
                    redirectAttributes.addFlashAttribute("success", "Transaction added successfully!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Access denied.");
                }
            }, () -> redirectAttributes.addFlashAttribute("error", "Investment not found."));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to add transaction: " + e.getMessage());
        }
        return "redirect:/investments";
    }

    private String getUserId(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}