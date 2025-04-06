package org.example.bankwithdrawalevent;


import java.math.BigDecimal;

public class WithdrawalEvent {
    private BigDecimal amount;
    private final Long accountId;
    private String status;

    public WithdrawalEvent(BigDecimal amount, Long accountId, String status) {
        this.amount = amount;
        this.accountId = accountId;
        this.status = status;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Long getAccountId() { return accountId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String toJson() {
        return String.format("{\"amount\":\"%s\",\"accountId\":%d,\"status\":\"%s\"}", amount, accountId, status);
    }
}
