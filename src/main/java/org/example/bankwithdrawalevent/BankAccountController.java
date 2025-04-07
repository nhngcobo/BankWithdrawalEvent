package org.example.bankwithdrawalevent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 *  Separate the application LOGIC from the controller
 * */

@RestController
@RequestMapping("/bank")
public class BankAccountController {
    private final BankEventService bankEventService;

    /**
    *  Controller constructor that initialises the bank event service
    * */

    @Autowired
    public BankAccountController(BankEventService bankEventService) {
        this.bankEventService = bankEventService;
    }

    /**
     * Check balance endpoint, allows user to check their balance from an account
     * */

    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam Long accountId) {
        return bankEventService.fetchAccountBalance(accountId);
    }

    /**
     *  Withdraw endpoint that also triggers the SMS notification to your mobile
     * */

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody WithdrawalEvent request) {
        return bankEventService.withdraw(request.getAccountId(), request.getAmount());
    }
}