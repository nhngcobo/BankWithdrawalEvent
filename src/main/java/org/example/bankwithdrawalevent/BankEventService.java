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
            logger.error("Failed to fetch account balance for account {}: {}", accountId, e.getMessage());
            return null;
        }
    }


    /**
     * Withdraw function that takes in an account and the amount requested for the withdrawal, also makes use of the check balance function
     * */

    public String withdraw(Long accountId, BigDecimal amount) {
        WithdrawalEvent event = new WithdrawalEvent(amount,  String.valueOf(accountId), StatusConstants.PENDING);

        // Input validation, if request is invalid return an error object
        if (accountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("INVALID_INPUT {}: {}", amount, accountId);
            event.setStatus(StatusConstants.FAILED);
            return event.toJson();
        }

        // Check current balance
        BigDecimal currentBalance = fetchAccountBalance(accountId);

        if (currentBalance == null) {
            event.setStatus(StatusConstants.FAILED);
            String smsText = String.format("There was a problem withdrawing from your accountId: %s", event.getAccountId());
            //pushNotification(smsText);     /** Optional send SMS*/
            logger.warn("There was a problem withdrawing from account {}", event.getAccountId());
            return smsText;
        }

        if (currentBalance.compareTo(amount) >= 0) {
            String sql = "UPDATE accounts SET balance = balance - ? WHERE accountId = ?";
            try {
                int rowsAffected = jdbcTemplate.update(sql, amount, accountId);
                if (rowsAffected > 0) {
                    event.setStatus(StatusConstants.SUCCESS);
                    String smsText = String.format("You have successfully withdrawn R%s from your account", event.getAmount());
                    pushNotification(smsText);
                    return smsText;
                } else {
                    event.setStatus(StatusConstants.FAILED);
                    logger.warn("Update query executed but no rows affected for account {}", event.toJson());
                    return event.toJson();
                }
            } catch (Exception e) {
                event.setStatus(StatusConstants.FAILED);
                logger.error("Database error during withdrawal for account {}: {}", event.toJson(), e.getMessage());
                return event.toJson();
            }
        } else {
            event.setStatus(StatusConstants.FAILED);
            String smsText = String.format("You do not have enough money to withdraw R%s from your account", event.getAmount());
            //pushNotification(smsText);   /** Optional send SMS*/
            logger.warn("Insufficient funds to withdraw {}: {}", event.getAmount(), event.getStatus());
            return smsText;
        }
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