package org.example.bankwithdrawalevent;

import java.math.BigDecimal;

public class WithdrawalEvent {
    private final BigDecimal amount;
    private final String accountId;
    private String status;

    public WithdrawalEvent(BigDecimal amount, String accountId, String status) {
        this.amount = amount;
        this.accountId = accountId;
        this.status = status;
    }

    public BigDecimal getAmount() { return amount; }

    public String getAccountId() { return accountId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String toJson() {
        return String.format("{\"amount\":\"%s\",\"accountId\":%s,\"status\":\"%s\"}", amount, accountId, status);
    }
}
