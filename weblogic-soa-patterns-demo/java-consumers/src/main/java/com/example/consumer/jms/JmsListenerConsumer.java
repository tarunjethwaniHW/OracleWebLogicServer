package com.example.consumer.jms;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * JMS Listener Consumer
 *
 * PATTERN DETECTION: This Java class RECEIVES messages from Oracle WebLogic JMS/OSB.
 * When OSB JMS Proxy or BPEL sends to a queue, this listener receives it.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS - INCOMING JMS (WebLogic/OSB → Java)                            │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. @JmsListener(destination = "...")                         → 95% JMS Consumer    │
 * │ 2. @JmsListener(containerFactory = "...")                    → 90% JMS Consumer    │
 * │ 3. @JmsListener(selector = "...")                            → 90% Filtered Listen │
 * │ 4. @Header(JmsHeaders.CORRELATION_ID)                        → 85% JMS Header      │
 * │ 5. @Payload annotation                                       → 80% Message Body    │
 * │ 6. implements MessageListener interface                      → 85% Raw JMS         │
 * │ 7. onMessage(Message message) method                         → 85% MessageListener │
 * │ 8. Import: org.springframework.jms.annotation.*              → 80% Spring JMS      │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract destination name from @JmsListener(destination = "...")
 * - Match to producer's jmsTemplate.convertAndSend(destination, ...)
 * - Match to OSB JMS Proxy output queue
 * - Match to WebLogicJndiProducer's queue name
 */
@Service
public class JmsListenerConsumer {

    /**
     * Listen for orders from WebLogic JMS Queue
     *
     * DETECTION:
     * - @JmsListener annotation = THIS IS A JMS CONSUMER
     * - destination = "jms/OrderResponseQueue" = QUEUE NAME TO LINK
     *
     * LINKING:
     * - Producer: jmsTemplate.convertAndSend("jms/OrderResponseQueue", ...)
     * - OSB: JMS Proxy publishes to this queue
     * - BPEL: JMS Adapter sends to this queue
     */
    @JmsListener(
        destination = "jms/OrderResponseQueue",
        concurrency = "1-5"
    )
    public void onOrderResponse(
            @Payload String messageBody,
            @Header(JmsHeaders.CORRELATION_ID) String correlationId,
            @Header(name = "OrderId", required = false) String orderId,
            @Header(name = "OrderType", required = false) String orderType,
            @Header(name = "ProcessedBy", required = false) String processedBy,
            Message rawMessage) {

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("INCOMING JMS MESSAGE from WebLogic/OSB");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Queue: jms/OrderResponseQueue");
        System.out.println("Correlation ID: " + correlationId);
        System.out.println("Order ID: " + orderId);
        System.out.println("Order Type: " + orderType);
        System.out.println("Processed By: " + processedBy);
        System.out.println("Message Body: " + messageBody);

        // Process the message (your business logic)
        processOrderResponse(messageBody, orderId);
    }

    /**
     * Listen for notifications from different queue
     *
     * DETECTION: Another @JmsListener = another consumer endpoint
     */
    @JmsListener(destination = "jms/NotificationQueue")
    public void onNotification(@Payload NotificationMessage notification) {

        System.out.println("INCOMING JMS NOTIFICATION");
        System.out.println("Type: " + notification.getType());
        System.out.println("Message: " + notification.getMessage());

        // Handle notification
        handleNotification(notification);
    }

    /**
     * Listen with JMS selector (filtered messages)
     *
     * DETECTION: @JmsListener with selector = filtered consumption
     * Only receives messages where JMS property matches selector
     */
    @JmsListener(
        destination = "jms/OrderEventQueue",
        selector = "EventType = 'ORDER_COMPLETED'"
    )
    public void onOrderCompletedEvent(TextMessage message) throws JMSException {

        System.out.println("INCOMING JMS EVENT (filtered): ORDER_COMPLETED");
        System.out.println("Order ID: " + message.getStringProperty("OrderId"));
        System.out.println("Message: " + message.getText());

        // Handle order completed event
    }

    /**
     * Listen to high-priority queue with more concurrency
     *
     * DETECTION: Different concurrency = scaling configuration
     */
    @JmsListener(
        destination = "jms/PriorityOrderQueue",
        concurrency = "3-10"  // More threads for priority messages
    )
    public void onPriorityOrder(
            @Payload String orderXml,
            @Header(name = "Priority", required = false) String priority) {

        System.out.println("INCOMING HIGH-PRIORITY ORDER");
        System.out.println("Priority: " + priority);

        // Process with higher priority
        processPriorityOrder(orderXml);
    }

    /**
     * Listen using custom container factory
     *
     * DETECTION: containerFactory = custom JMS configuration
     * May indicate WebLogic-specific JMS settings
     */
    @JmsListener(
        destination = "jms/TransactionalQueue",
        containerFactory = "weblogicJmsListenerContainerFactory"
    )
    public void onTransactionalMessage(Message message) throws JMSException {

        System.out.println("INCOMING TRANSACTIONAL MESSAGE");
        System.out.println("Message ID: " + message.getJMSMessageID());

        // Transactional processing - will be committed/rolled back
        processTransactionalMessage(message);
    }

    /**
     * Listen to Dead Letter Queue for failed messages
     *
     * DETECTION: DLQ listener = error handling
     */
    @JmsListener(destination = "jms/OrderDLQ")
    public void onDeadLetterMessage(
            Message message,
            @Header(name = "DLQ_REASON", required = false) String dlqReason,
            @Header(name = "ORIGINAL_DESTINATION", required = false) String originalDest)
            throws JMSException {

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("DEAD LETTER MESSAGE RECEIVED");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Original Destination: " + originalDest);
        System.out.println("DLQ Reason: " + dlqReason);
        System.out.println("Message ID: " + message.getJMSMessageID());

        // Handle dead letter (alert, log, manual review)
        handleDeadLetter(message, dlqReason);
    }

    // ========================================
    // Business Logic Methods
    // ========================================

    private void processOrderResponse(String messageBody, String orderId) {
        System.out.println("Processing order response for: " + orderId);
        // Save to database, trigger next workflow step, etc.
    }

    private void handleNotification(NotificationMessage notification) {
        System.out.println("Handling notification: " + notification.getType());
        // Send email, push notification, etc.
    }

    private void processPriorityOrder(String orderXml) {
        System.out.println("Priority processing: " + orderXml.substring(0, Math.min(100, orderXml.length())));
        // Expedited processing
    }

    private void processTransactionalMessage(Message message) throws JMSException {
        System.out.println("Transactional processing: " + message.getJMSMessageID());
        // This will be committed or rolled back based on success/failure
    }

    private void handleDeadLetter(Message message, String reason) {
        System.out.println("Dead letter handling - reason: " + reason);
        // Alert operations team, log for review, attempt remediation
    }
}

// ============================================================================
// DTOs
// ============================================================================
class NotificationMessage implements java.io.Serializable {
    private String type;
    private String message;
    private String orderId;
    private String timestamp;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
