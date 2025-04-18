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
     * OPTIONAL Check balance endpoint, allows user to check their balance from an account
     * */

    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam String accountId) {
        //We do this to validate the accountId, ensure input like accountId = 123de4 triggers exception handling , NumberFormatException
        Long accountIdLong = Long.parseLong(accountId);
        return bankEventService.fetchAccountBalance(accountIdLong);
    }


    /**
     *  Withdraw endpoint that also triggers the SMS notification to your mobile
     * */

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody WithdrawalEvent request) {
        //We do this to validate the accountId, ensure input like accountId = 123de4 triggers exception handling , NumberFormatException
        Long accountIdLong = Long.parseLong(request.getAccountId());
        return bankEventService.withdraw(accountIdLong, request.getAmount());
        }
}