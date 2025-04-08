package org.example.bankwithdrawalevent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public String getBalance(@RequestParam String accountId) {
        try {
            // Validate if accountId is numeric
            Long accountIdLong = Long.parseLong(accountId);
            Object balanceObj = bankEventService.fetchAccountBalance(accountIdLong);
            int currentBalance = Integer.parseInt(balanceObj.toString()); // Consider using proper return type from service

            // If the account is of Valid format ( can be converted to a number ) but is invalid
            if (currentBalance == StatusConstants.NO_ACCOUNT) {
                return String.format("There was a problem withdrawing from your accountId: %s", accountId);
            }
            return String.valueOf(currentBalance);

        } catch (NumberFormatException e) {
            return String.format("Invalid account Id format: R%s should be numeric.", accountId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch account balance", e);
        }
    }

    /**
     *  Withdraw endpoint that also triggers the SMS notification to your mobile
     * */

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody WithdrawalEvent request) {
        //We do this to validate the accountId, ensure input like accountId = 123de4 triggers error handling
        try {
            // Converts String to Long to check if accountId can be a Long
            Long accountIdLong = Long.valueOf(request.getAccountId());

            return bankEventService.withdraw(accountIdLong, request.getAmount());
        } catch (NumberFormatException e) {
            return String.format("Invalid account Id format: R%s should be numeric.", request.getAccountId());
        }
    }
}