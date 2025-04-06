package org.example.bankwithdrawalevent;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



import java.math.BigDecimal;
@RestController
@RequestMapping("/bank")
public class BankAccountController {
    @Autowired
    private final BankEventService bankEventService;

    @Autowired
    public BankAccountController(BankEventService bankEventService) {
        this.bankEventService = bankEventService;
    }

    // Check Balance; usually the first thing a customer does
    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam Long accountId) {
        return bankEventService.fetchAccountBalance(accountId);
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam("accountId") Long accountId, @RequestParam("amount") BigDecimal amount) {
        return bankEventService.withdraw(accountId, amount);
    }
}