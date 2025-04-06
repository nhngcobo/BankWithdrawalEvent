package org.example.bankwithdrawalevent;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    @Autowired
    public BankEventService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal fetchAccountBalance(Long accountId) {
        try {
            String sql = "SELECT balance FROM accounts WHERE accountId = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{accountId}, BigDecimal.class);

        } catch (Exception e) {
            System.out.println("Account may not exist: " + e.getMessage());
            return null;
        }
    }

    public String withdraw(Long accountId, BigDecimal amount) {
        // Check current balance
        BigDecimal currentBalance = fetchAccountBalance(accountId);
        if (currentBalance != null && currentBalance.compareTo(amount) >= 0) {
            // Update balance
            String sql = "UPDATE accounts SET balance = balance - ? WHERE accountId = ?";
            int rowsAffected = jdbcTemplate.update(sql, amount, accountId);
            if (rowsAffected > 0) {
                String smsText = "You have successfully withdrawn R" + amount + " from your account ";
                pushNotification(smsText);
                return "Success";
            } else {
                // In case the update fails for reasons other than a balance check
                return "Failed";
            }
        } else {
            // Insufficient funds, optional notification
            String smsText = "You have do not have enough money to withdraw R" + amount + " from your account";
            pushNotification(smsText);
            return "Insufficient funds for withdrawal";
        }
    }

    public void pushNotification(String smsText) {
        //Only Open connection when to AWS when you want to send the notification.

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
            System.out.println("SMS sent! Message ID: " + response.messageId());
        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
        }
        snsClient.close();
    }
}