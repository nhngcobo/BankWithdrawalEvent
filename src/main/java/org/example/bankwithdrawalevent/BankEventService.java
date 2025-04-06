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

@Service
public class BankEventService {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger logger = LoggerFactory.getLogger(BankEventService.class);


    @Autowired
    public BankEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal fetchAccountBalance(Long accountId) {
        try {
            String sql = "SELECT balance FROM accounts WHERE accountId = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{accountId}, BigDecimal.class);

        } catch (Exception e) {
            logger.error("Failed to fetch account balance for account {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    public String withdraw(Long accountId, BigDecimal amount) {
        // Input validation
        WithdrawalEvent event = new WithdrawalEvent(amount, accountId, "Pending");

        if (accountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("INVALID_INPUT {}: {}", amount, accountId);
            event.setStatus("Failed");
            return event.toJson();
        }

        // Check current balance
        BigDecimal currentBalance = fetchAccountBalance(accountId);

        if (currentBalance != null && currentBalance.compareTo(amount) >= 0) {
            String sql = "UPDATE accounts SET balance = balance - ? WHERE accountId = ?";
            try {
                int rowsAffected = jdbcTemplate.update(sql, amount, accountId);
                if (rowsAffected > 0) {
                    event.setStatus("Success");
                    String smsText = "You have successfully withdrawn R" + event.getAmount() + " from your account";
                    pushNotification(smsText);
                    return "Success";
                } else {
                    event.setStatus("Failed");
                    logger.warn("Update query executed but no rows affected for account {}", event.toJson());
                    return "Failed";
                }
            } catch (Exception e) {
                event.setStatus("Failed");
                logger.error("Database error during withdrawal for account {}: {}", event.toJson(), e.getMessage());
                return "An error occurred while processing the withdrawal.";
            }
        }
        else {
            // Insufficient funds, optional notification
            event.setStatus("Failed");
            String smsText = "You have do not have enough money to withdraw R" + event.getAmount() + " from your account";
            pushNotification(smsText);

            logger.error("You have do not have enough funds to withdraw {}: {}", event.getAmount(), event.getStatus());

            return smsText;
        }
    }

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

        //Set SMS attributes (Transactional is for OTPs / alerts)
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
        snsClient.close();   //close connection
    }
}