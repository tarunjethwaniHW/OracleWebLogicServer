package com.example.producer.jms;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * WebLogic JNDI JMS Producer
 *
 * PATTERN DETECTION: This contains 100% WEBLOGIC-SPECIFIC patterns!
 * These patterns ONLY exist in WebLogic environments.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ 100% WEBLOGIC-SPECIFIC PATTERNS (Guaranteed Detection)                             │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. "weblogic.jndi.WLInitialContextFactory"                   → 100% WebLogic       │
 * │ 2. "t3://" protocol URL                                      → 100% WebLogic       │
 * │ 3. "t3s://" protocol URL (secure)                            → 100% WebLogic       │
 * │ 4. Import: weblogic.jndi.*                                   → 100% WebLogic       │
 * │ 5. Import: weblogic.jms.*                                    → 100% WebLogic JMS   │
 * │ 6. JNDI path: "weblogic.jms.*"                               → 100% WebLogic JMS   │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ HIGH CONFIDENCE JMS PATTERNS                                                       │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. InitialContext with JNDI lookup                           → 85% JMS/EJB         │
 * │ 2. QueueConnectionFactory / TopicConnectionFactory           → 85% JMS             │
 * │ 3. ctx.lookup("jms/...")                                     → 80% JMS Resource    │
 * │ 4. ConnectionFactory.createConnection()                      → 80% JMS             │
 * │ 5. session.createProducer() / createConsumer()               → 80% JMS             │
 * │ 6. producer.send(message)                                    → 85% JMS Send        │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract queue/topic name from JNDI lookup or createQueue()
 * - Match to OSB JMS Proxy destination
 * - Trace to consumer @JmsListener(destination = "...")
 */
public class WebLogicJndiProducer {

    // ═══════════════════════════════════════════════════════════════════════════════════
    // 100% WEBLOGIC-SPECIFIC CONSTANTS
    // These strings are UNIQUE to Oracle WebLogic Server
    // ═══════════════════════════════════════════════════════════════════════════════════

    // DETECTION: WebLogic JNDI Factory - ONLY WebLogic uses this
    private static final String WEBLOGIC_JNDI_FACTORY = "weblogic.jndi.WLInitialContextFactory";

    // DETECTION: T3 Protocol - WebLogic proprietary protocol
    // t3:// = unencrypted, t3s:// = SSL encrypted
    private static final String WEBLOGIC_PROVIDER_URL = "t3://weblogic-server:7001";
    private static final String WEBLOGIC_PROVIDER_URL_SSL = "t3s://weblogic-server:7002";

    // DETECTION: WebLogic cluster URL pattern
    private static final String WEBLOGIC_CLUSTER_URL = "t3://server1:7001,server2:7001,server3:7001";

    // DETECTION: JNDI names for WebLogic JMS resources
    private static final String JMS_CONNECTION_FACTORY = "jms/OrderConnectionFactory";
    private static final String JMS_ORDER_QUEUE = "jms/OrderRequestQueue";
    private static final String JMS_RESPONSE_QUEUE = "jms/OrderResponseQueue";

    // WebLogic credentials
    private final String username = "weblogic";
    private final String password = "welcome1";

    private Context jndiContext;
    private QueueConnectionFactory connectionFactory;
    private Queue orderQueue;

    /**
     * Initialize WebLogic JNDI Context
     *
     * THIS IS THE PRIMARY WEBLOGIC DETECTION POINT
     *
     * DETECTION:
     * - Hashtable with INITIAL_CONTEXT_FACTORY = "weblogic.jndi.WLInitialContextFactory"
     * - Hashtable with PROVIDER_URL = "t3://..."
     * - new InitialContext(hashtable) with these values
     */
    public void initialize() throws NamingException, JMSException {
        // DETECTION: Hashtable environment for JNDI
        Hashtable<String, String> env = new Hashtable<>();

        // ═══════════════════════════════════════════════════════════════════════════
        // THIS IS 100% WEBLOGIC-SPECIFIC
        // No other application server uses "weblogic.jndi.WLInitialContextFactory"
        // ═══════════════════════════════════════════════════════════════════════════
        env.put(Context.INITIAL_CONTEXT_FACTORY, WEBLOGIC_JNDI_FACTORY);

        // ═══════════════════════════════════════════════════════════════════════════
        // THIS IS 100% WEBLOGIC-SPECIFIC
        // "t3://" protocol is WebLogic's proprietary protocol
        // No other application server uses t3://
        // ═══════════════════════════════════════════════════════════════════════════
        env.put(Context.PROVIDER_URL, WEBLOGIC_PROVIDER_URL);

        // WebLogic authentication
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        // DETECTION: new InitialContext with WebLogic-specific environment
        this.jndiContext = new InitialContext(env);

        // DETECTION: JNDI lookup for JMS resources
        // Pattern: ctx.lookup("jms/...")
        this.connectionFactory = (QueueConnectionFactory) jndiContext.lookup(JMS_CONNECTION_FACTORY);
        this.orderQueue = (Queue) jndiContext.lookup(JMS_ORDER_QUEUE);

        System.out.println("WebLogic JNDI context initialized successfully");
    }

    /**
     * Send message to WebLogic JMS Queue
     *
     * DETECTION: Standard JMS send pattern after WebLogic JNDI lookup
     */
    public void sendOrderMessage(String orderId, String customerId, double amount) throws JMSException {
        System.out.println("Sending to WebLogic JMS Queue: " + orderId);

        // DETECTION: JMS Connection creation from factory
        QueueConnection connection = connectionFactory.createQueueConnection();

        try {
            // DETECTION: JMS Session creation
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // DETECTION: Create producer for specific queue
            QueueSender sender = session.createSender(orderQueue);

            // Build message
            TextMessage message = session.createTextMessage();
            message.setText(buildOrderXml(orderId, customerId, amount));

            // Set JMS properties (OSB can route based on these)
            message.setStringProperty("OrderId", orderId);
            message.setStringProperty("CustomerId", customerId);
            message.setDoubleProperty("Amount", amount);
            message.setStringProperty("Source", "WebLogicJndiProducer");

            // DETECTION: send() call - the actual message send
            sender.send(message);

            System.out.println("Order message sent to WebLogic JMS: " + orderId);

        } finally {
            connection.close();
        }
    }

    /**
     * Send with request-reply pattern
     *
     * DETECTION: TemporaryQueue for synchronous response
     */
    public String sendOrderAndWaitForResponse(String orderId, long timeoutMs) throws JMSException {
        QueueConnection connection = connectionFactory.createQueueConnection();

        try {
            connection.start();
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            // DETECTION: TemporaryQueue for reply
            TemporaryQueue replyQueue = session.createTemporaryQueue();

            QueueSender sender = session.createSender(orderQueue);

            TextMessage requestMessage = session.createTextMessage();
            requestMessage.setText("<OrderRequest><OrderId>" + orderId + "</OrderId></OrderRequest>");
            requestMessage.setJMSReplyTo(replyQueue);  // DETECTION: setJMSReplyTo for request-reply
            requestMessage.setJMSCorrelationID(orderId);

            sender.send(requestMessage);

            // DETECTION: createReceiver on reply queue
            QueueReceiver receiver = session.createReceiver(replyQueue);

            // DETECTION: receive() with timeout - blocking wait for response
            TextMessage responseMessage = (TextMessage) receiver.receive(timeoutMs);

            if (responseMessage != null) {
                return responseMessage.getText();
            } else {
                throw new RuntimeException("Timeout waiting for response");
            }

        } finally {
            connection.close();
        }
    }

    /**
     * Send to Topic (publish-subscribe pattern)
     *
     * DETECTION: TopicConnectionFactory and Topic for pub-sub
     */
    public void publishOrderEvent(String eventType, String orderId) throws NamingException, JMSException {
        // DETECTION: Topic-based JMS resources
        TopicConnectionFactory topicFactory =
            (TopicConnectionFactory) jndiContext.lookup("jms/OrderEventTopicFactory");
        Topic orderEventTopic = (Topic) jndiContext.lookup("jms/OrderEventTopic");

        TopicConnection connection = topicFactory.createTopicConnection();

        try {
            TopicSession session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            // DETECTION: TopicPublisher for publish-subscribe
            TopicPublisher publisher = session.createPublisher(orderEventTopic);

            TextMessage message = session.createTextMessage();
            message.setText("<OrderEvent type='" + eventType + "'><OrderId>" + orderId + "</OrderId></OrderEvent>");
            message.setStringProperty("EventType", eventType);

            // DETECTION: publish() for topic
            publisher.publish(message);

            System.out.println("Order event published: " + eventType + " - " + orderId);

        } finally {
            connection.close();
        }
    }

    /**
     * Alternative: Using WebLogic-specific JMS extensions
     *
     * DETECTION: WebLogic JMS extension classes (if used)
     */
    public void sendWithWebLogicExtensions(String orderId) throws Exception {
        // NOTE: These imports would be:
        // import weblogic.jms.extensions.WLConnection;
        // import weblogic.jms.extensions.WLSession;

        // WebLogic-specific: Unit of Order (message grouping)
        // DETECTION: setStringProperty with JMS_BEA_UnitOfOrder
        // This is WebLogic-specific for ordered processing

        QueueConnection connection = connectionFactory.createQueueConnection();
        try {
            QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(orderQueue);

            TextMessage message = session.createTextMessage();
            message.setText("<Order><OrderId>" + orderId + "</OrderId></Order>");

            // DETECTION: WebLogic-specific Unit of Order
            // Ensures messages with same UnitOfOrder are processed in order
            message.setStringProperty("JMS_BEA_UnitOfOrder", "CustomerOrders-" + orderId.substring(0, 3));

            sender.send(message);
        } finally {
            connection.close();
        }
    }

    /**
     * Connect to WebLogic Cluster
     *
     * DETECTION: Multiple servers in t3:// URL
     */
    public void initializeCluster() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.INITIAL_CONTEXT_FACTORY, WEBLOGIC_JNDI_FACTORY);

        // DETECTION: Cluster URL pattern - comma-separated servers
        env.put(Context.PROVIDER_URL, WEBLOGIC_CLUSTER_URL);

        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        this.jndiContext = new InitialContext(env);

        System.out.println("Connected to WebLogic Cluster");
    }

    /**
     * Cleanup resources
     */
    public void cleanup() throws NamingException {
        if (jndiContext != null) {
            jndiContext.close();
        }
    }

    private String buildOrderXml(String orderId, String customerId, double amount) {
        return "<?xml version='1.0'?>" +
               "<OrderRequest xmlns='http://example.com/order'>" +
               "  <OrderId>" + orderId + "</OrderId>" +
               "  <CustomerId>" + customerId + "</CustomerId>" +
               "  <Amount>" + amount + "</Amount>" +
               "</OrderRequest>";
    }
}
