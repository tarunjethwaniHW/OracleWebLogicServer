package com.example.producer.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.UUID;

/**
 * Spring JmsTemplate Producer
 *
 * PATTERN DETECTION: Spring's preferred way to send JMS messages.
 * Most common in modern Spring Boot applications connecting to WebLogic/OSB.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS (Ranked by Confidence)                                          │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. jmsTemplate.convertAndSend(destination, message)          → 85% JMS Send        │
 * │ 2. jmsTemplate.send(destination, messageCreator)             → 85% JMS Send        │
 * │ 3. jmsTemplate.sendAndReceive()                              → 85% JMS Req-Reply   │
 * │ 4. @Autowired JmsTemplate                                    → 80% JMS Client      │
 * │ 5. @Value with queue/topic name                              → 75% JMS Destination │
 * │ 6. Import: org.springframework.jms.*                         → 80% Spring JMS      │
 * │ 7. MessageCreator implementation                             → 75% JMS Message     │
 * │ 8. MessagePostProcessor for headers                          → 75% JMS Properties  │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract destination name from convertAndSend() first parameter
 * - If variable, resolve from @Value annotation
 * - Match to OSB JMS Proxy or @JmsListener destination
 */
@Service
public class JmsTemplateProducer {

    // DETECTION: @Autowired JmsTemplate
    @Autowired
    private JmsTemplate jmsTemplate;

    // DETECTION: @Value with queue names
    // These are the destinations to extract for linking
    @Value("${weblogic.jms.order-queue:jms/OrderRequestQueue}")
    private String orderRequestQueue;

    @Value("${weblogic.jms.response-queue:jms/OrderResponseQueue}")
    private String orderResponseQueue;

    @Value("${weblogic.jms.event-topic:jms/OrderEventTopic}")
    private String orderEventTopic;

    @Value("${weblogic.jms.priority-queue:jms/OrderPriorityQueue}")
    private String priorityQueue;

    /**
     * Simple send using convertAndSend
     *
     * DETECTION: jmsTemplate.convertAndSend(destination, message)
     * - Parameter 1: Queue/Topic name (EXTRACT FOR LINKING)
     * - Parameter 2: Message object (auto-converted)
     */
    public void sendOrder(OrderMessage order) {
        System.out.println("Sending order via JmsTemplate: " + order.getOrderId());

        // THIS IS THE KEY CALLEE TO DETECT
        // Pattern: jmsTemplate.convertAndSend(queueName, message)
        jmsTemplate.convertAndSend(orderRequestQueue, order);

        System.out.println("Order sent to: " + orderRequestQueue);
    }

    /**
     * Send with message post-processor (to set JMS properties)
     *
     * DETECTION: convertAndSend with MessagePostProcessor lambda
     * The third parameter is a lambda/callback that modifies the message
     */
    public String sendOrderWithProperties(OrderMessage order) {
        String correlationId = UUID.randomUUID().toString();

        // DETECTION: convertAndSend with 3 parameters
        // Parameter 3: MessagePostProcessor to set JMS properties
        jmsTemplate.convertAndSend(orderRequestQueue, order, message -> {
            // Set JMS properties (OSB/consumers can filter on these)
            message.setJMSCorrelationID(correlationId);
            message.setStringProperty("OrderType", order.getOrderType());
            message.setStringProperty("Priority", order.getPriority());
            message.setStringProperty("CustomerId", order.getCustomerId());
            message.setDoubleProperty("Amount", order.getAmount());
            message.setStringProperty("Source", "JmsTemplateProducer");

            return message;
        });

        return correlationId;
    }

    /**
     * Send using MessageCreator for full control
     *
     * DETECTION: jmsTemplate.send(destination, messageCreator)
     * MessageCreator gives full control over message creation
     */
    public void sendOrderWithMessageCreator(final String orderId, final String customerId, final double amount) {

        // DETECTION: jmsTemplate.send() with MessageCreator
        jmsTemplate.send(orderRequestQueue, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                // DETECTION: session.createTextMessage()
                TextMessage message = session.createTextMessage();

                // Build XML message manually
                String xml = "<?xml version='1.0'?>" +
                             "<Order>" +
                             "  <OrderId>" + orderId + "</OrderId>" +
                             "  <CustomerId>" + customerId + "</CustomerId>" +
                             "  <Amount>" + amount + "</Amount>" +
                             "</Order>";
                message.setText(xml);

                // Set properties
                message.setStringProperty("OrderId", orderId);

                return message;
            }
        });
    }

    /**
     * Send to specific queue (dynamic destination)
     *
     * DETECTION: convertAndSend with variable destination
     * Queue name might be passed as parameter
     */
    public void sendToQueue(String queueName, OrderMessage order) {
        System.out.println("Sending to dynamic queue: " + queueName);

        // DETECTION: Dynamic queue name - harder to trace statically
        jmsTemplate.convertAndSend(queueName, order);
    }

    /**
     * Send to topic (publish-subscribe)
     *
     * DETECTION: Same pattern but to a topic
     * May need to check JmsTemplate configuration for pubSubDomain
     */
    public void publishOrderEvent(String eventType, OrderMessage order) {
        // DETECTION: convertAndSend to topic
        // Whether it's queue or topic depends on JmsTemplate configuration
        jmsTemplate.convertAndSend(orderEventTopic, order, message -> {
            message.setStringProperty("EventType", eventType);
            return message;
        });
    }

    /**
     * Send high-priority message
     *
     * DETECTION: JMS priority setting
     */
    public void sendPriorityOrder(OrderMessage order) {
        // DETECTION: send() with explicit priority control
        jmsTemplate.send(priorityQueue, session -> {
            TextMessage message = session.createTextMessage();
            message.setText(order.toString());

            // DETECTION: setJMSPriority for priority messages
            // JMS priority: 0-4 normal, 5-9 expedited
            // Setting it here requires JmsTemplate.setExplicitQosEnabled(true)
            return message;
        });

        // Alternative: Configure JmsTemplate for priority
        // jmsTemplate.setExplicitQosEnabled(true);
        // jmsTemplate.setPriority(9);
        // jmsTemplate.convertAndSend(priorityQueue, order);
    }

    /**
     * Send and receive (synchronous request-reply)
     *
     * DETECTION: jmsTemplate.sendAndReceive()
     * Blocks until response is received or timeout
     */
    public OrderMessage sendOrderAndWait(OrderMessage order) {
        System.out.println("Sending order and waiting for response: " + order.getOrderId());

        // DETECTION: sendAndReceive() for request-reply pattern
        // This is synchronous - blocks the thread!
        Message responseMessage = jmsTemplate.sendAndReceive(orderRequestQueue, session -> {
            TextMessage requestMessage = session.createTextMessage();
            requestMessage.setText(order.toXml());
            requestMessage.setJMSCorrelationID(order.getOrderId());
            return requestMessage;
        });

        if (responseMessage != null) {
            // Parse response
            // return parseResponse(responseMessage);
            return new OrderMessage(); // Placeholder
        }

        return null;
    }

    /**
     * Receive from queue (for response handling)
     *
     * DETECTION: jmsTemplate.receive() or receiveAndConvert()
     * Used less often than @JmsListener but still valid
     */
    public OrderMessage receiveResponse() {
        // DETECTION: jmsTemplate.receiveAndConvert()
        // Polls the queue once - not a listener
        return (OrderMessage) jmsTemplate.receiveAndConvert(orderResponseQueue);
    }

    /**
     * Receive with selector (filtered receive)
     *
     * DETECTION: receiveSelected() with JMS selector
     */
    public OrderMessage receiveByCorrelationId(String correlationId) {
        // DETECTION: receiveSelectedAndConvert with JMS selector
        String selector = "JMSCorrelationID = '" + correlationId + "'";
        return (OrderMessage) jmsTemplate.receiveSelectedAndConvert(orderResponseQueue, selector);
    }
}

// ============================================================================
// MESSAGE DTO
// ============================================================================
class OrderMessage implements java.io.Serializable {
    private String orderId;
    private String customerId;
    private String orderType;
    private String priority;
    private double amount;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String toXml() {
        return "<Order><OrderId>" + orderId + "</OrderId></Order>";
    }

    @Override
    public String toString() {
        return "OrderMessage{orderId='" + orderId + "', customerId='" + customerId + "', amount=" + amount + "}";
    }
}
