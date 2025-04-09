package org.example.bankwithdrawalevent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 *  BankEventService where the application logic is.
 * */

@Service
public class BankEventService {

    private final JdbcTemplate jdbcTemplate;
    private final Logger logger = LoggerFactory.getLogger(BankEventService.class);


    @Autowired
    public BankEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Fetch account Balance function that checks the balance for a specific account/acountId
     * */

    public BigDecimal fetchAccountBalance(Long accountId) {
        try {
            String sql = "SELECT balance FROM accounts WHERE accountId = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{accountId}, BigDecimal.class);
        } catch (Exception e) {
            logger.error("Account not found: {}", accountId);
            throw new AccountNotFoundException("Account ID not found: " + accountId);
        }
    }


    /**
     * Withdraw function that takes in an account and the amount requested for the withdrawal, also makes use of the check balance function
     * */

    public String withdraw(Long accountId, BigDecimal amount) {
        WithdrawalEvent event = new WithdrawalEvent(amount,  String.valueOf(accountId), StatusConstants.PENDING);

        if (accountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            event.setStatus(StatusConstants.FAILED);
            throw new InvalidRequestException(String.format("Invalid account ID or amount.\n%s", event.toJson()));
        }

        BigDecimal currentBalance = fetchAccountBalance(accountId);

        if (currentBalance.compareTo(amount) < 0) {
            event.setStatus(StatusConstants.FAILED);
            throw new InsufficientFundsException(String.format("Insufficient funds for withdrawal.\n%s", event.toJson()));
        }

        String sql = "UPDATE accounts SET balance = balance - ? WHERE accountId = ?";
        int rowsAffected = jdbcTemplate.update(sql, amount, accountId);

        if (rowsAffected == 0) {
            logger.warn("No rows updated during withdrawal for account {}", accountId);
            throw new RuntimeException(String.format("Withdrawal failed, no rows updated during withdrawal for account.\n%s", event.toJson()));
        }

        event.setStatus(StatusConstants.SUCCESS);

        // Send SMS notification
        String smsText = String.format("You have successfully withdrawn R%s from your account", amount);
        pushNotification(smsText);

        return smsText +"\n"+event.toJson();
    }

    /**
     * Push notification sends the SMS update of a transaction to your mobile number.
     * */

    public void pushNotification(String smsText) {
        //Only Open connection  to AWS when you want to send a notification.
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                "",
                ""
        );

        SnsClient snsClient = SnsClient.builder()
                .region(Region.EU_NORTH_1)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        //Set SMS attributes
        Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
        smsAttributes.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                .stringValue("Transactional")
                .dataType("String")
                .build());

        // Construct SMS request
        PublishRequest request = PublishRequest.builder()
                .message(smsText)
                .phoneNumber("")
                .messageAttributes(smsAttributes)
                .build();

        try {
            PublishResponse response = snsClient.publish(request);
            logger.info("SMS sent! Message ID:  {}", response.messageId());
        } catch (Exception e) {
            logger.error("Error sending SMS: {}", e.getMessage());
        }
        snsClient.close();   // Close AWS connection
    }
}