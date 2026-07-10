package com.example.producer.soap;

import javax.xml.soap.*;
import javax.xml.namespace.QName;
import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * SAAJ (SOAP with Attachments API for Java) Client Producer
 *
 * PATTERN DETECTION: Low-level SOAP API - Direct XML manipulation.
 * Used when developers need full control over SOAP envelope.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS (Ranked by Confidence)                                          │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. SOAPConnection.call(message, endpoint)                    → 90% SOAP Call       │
 * │ 2. SOAPConnectionFactory.newInstance()                       → 90% SOAP Setup      │
 * │ 3. MessageFactory.newInstance()                              → 85% SOAP Message    │
 * │ 4. SOAPMessage creation                                      → 85% SOAP Message    │
 * │ 5. SOAPEnvelope, SOAPBody, SOAPHeader manipulation           → 80% SOAP Building   │
 * │ 6. Import: javax.xml.soap.*                                  → 90% SAAJ API        │
 * │ 7. addChildElement(), setTextContent() on SOAP elements      → 75% SOAP Building   │
 * │ 8. getMimeHeaders().addHeader("SOAPAction", ...)             → 85% SOAP Action     │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract endpoint URL from SOAPConnection.call() second parameter
 * - Extract SOAPAction from MimeHeaders
 * - Extract operation name from SOAPBody child element
 */
public class SaajClientProducer {

    private static final String NAMESPACE_URI = "http://example.com/order/service";
    private static final String NAMESPACE_PREFIX = "ord";

    // SOA endpoint - DETECTION: URL with soa-infra or osb pattern
    private final String orderServiceEndpoint =
        "http://soa-server:7001/soa-infra/services/default/OrderComposite/OrderService";

    // SAAJ factories - DETECTION: SAAJ factory instances
    private SOAPConnectionFactory soapConnectionFactory;
    private MessageFactory messageFactory;

    public SaajClientProducer() throws SOAPException {
        // DETECTION: SOAPConnectionFactory.newInstance()
        this.soapConnectionFactory = SOAPConnectionFactory.newInstance();

        // DETECTION: MessageFactory.newInstance()
        // Can specify SOAP version: MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
        this.messageFactory = MessageFactory.newInstance();
    }

    /**
     * Call Order Service via raw SOAP using SAAJ
     *
     * DETECTION: This method shows the complete SAAJ pattern
     * - Build SOAP message manually
     * - Send via SOAPConnection.call()
     * - Parse SOAP response
     */
    public String processOrder(String orderId, String customerId, double amount) throws Exception {
        System.out.println("Calling SOA via SAAJ: processOrder");

        // DETECTION: Create SOAPConnection
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        try {
            // Build the SOAP request
            SOAPMessage soapRequest = createOrderRequest(orderId, customerId, amount);

            // Log the request (for debugging)
            System.out.println("SOAP Request:");
            soapRequest.writeTo(System.out);
            System.out.println();

            // THIS IS THE KEY CALLEE TO DETECT
            // DETECTION: SOAPConnection.call(message, endpoint)
            // Parameter 1: SOAPMessage - the request
            // Parameter 2: Object - the endpoint URL
            SOAPMessage soapResponse = soapConnection.call(soapRequest, orderServiceEndpoint);

            // Parse and return response
            return parseOrderResponse(soapResponse);

        } finally {
            soapConnection.close();
        }
    }

    /**
     * Build SOAP request message
     *
     * DETECTION: Manual SOAP envelope construction
     * - MessageFactory.createMessage()
     * - SOAPEnvelope manipulation
     * - SOAPBody.addChildElement()
     */
    private SOAPMessage createOrderRequest(String orderId, String customerId, double amount)
            throws SOAPException {

        // DETECTION: messageFactory.createMessage()
        SOAPMessage soapMessage = messageFactory.createMessage();

        // DETECTION: Get SOAP parts
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        SOAPBody soapBody = envelope.getBody();
        SOAPHeader soapHeader = envelope.getHeader();

        // DETECTION: Add namespace declaration
        envelope.addNamespaceDeclaration(NAMESPACE_PREFIX, NAMESPACE_URI);

        // Add WS-Security header (common in enterprise SOAP)
        // DETECTION: WS-Security header construction
        addSecurityHeader(soapHeader);

        // DETECTION: Build SOAP body with operation element
        // The child element name often matches WSDL operation
        SOAPElement orderRequestElement = soapBody.addChildElement(
            "ProcessOrderRequest", NAMESPACE_PREFIX, NAMESPACE_URI);

        // Add request parameters
        // DETECTION: addChildElement() calls build the request structure
        SOAPElement orderIdElement = orderRequestElement.addChildElement("OrderId", NAMESPACE_PREFIX);
        orderIdElement.setTextContent(orderId);

        SOAPElement customerIdElement = orderRequestElement.addChildElement("CustomerId", NAMESPACE_PREFIX);
        customerIdElement.setTextContent(customerId);

        SOAPElement amountElement = orderRequestElement.addChildElement("Amount", NAMESPACE_PREFIX);
        amountElement.setTextContent(String.valueOf(amount));

        // DETECTION: Set SOAPAction header
        // This often matches WSDL <soap:operation soapAction="...">
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", "http://example.com/order/processOrder");

        soapMessage.saveChanges();
        return soapMessage;
    }

    /**
     * Add WS-Security header
     *
     * DETECTION: WS-Security namespace and elements
     * Common in enterprise WebLogic/SOA integrations
     */
    private void addSecurityHeader(SOAPHeader header) throws SOAPException {
        String wsseNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";

        // DETECTION: WS-Security elements
        SOAPElement securityElement = header.addChildElement("Security", "wsse", wsseNamespace);

        SOAPElement usernameToken = securityElement.addChildElement("UsernameToken", "wsse");

        SOAPElement username = usernameToken.addChildElement("Username", "wsse");
        username.setTextContent("weblogic");

        SOAPElement password = usernameToken.addChildElement("Password", "wsse");
        password.setAttribute("Type",
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
        password.setTextContent("welcome1");
    }

    /**
     * Parse SOAP response
     *
     * DETECTION: Response parsing from SOAPMessage
     */
    private String parseOrderResponse(SOAPMessage soapResponse) throws SOAPException {
        SOAPBody responseBody = soapResponse.getSOAPBody();

        // Check for SOAP Fault
        // DETECTION: SOAPFault handling
        if (responseBody.hasFault()) {
            SOAPFault fault = responseBody.getFault();
            throw new RuntimeException("SOAP Fault: " + fault.getFaultString());
        }

        // Extract response data
        // DETECTION: Navigating SOAP response elements
        SOAPElement responseElement = (SOAPElement) responseBody.getFirstChild();
        if (responseElement != null) {
            return responseElement.getTextContent();
        }

        return null;
    }

    /**
     * Call with SOAP 1.2 protocol
     *
     * DETECTION: SOAPConstants.SOAP_1_2_PROTOCOL
     * Different from default SOAP 1.1
     */
    public String processOrderSoap12(String orderId) throws Exception {
        // DETECTION: SOAP 1.2 MessageFactory
        MessageFactory soap12Factory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage soapMessage = soap12Factory.createMessage();

        SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
        SOAPBody body = envelope.getBody();

        // SOAP 1.2 uses different content type
        // DETECTION: MimeHeader content type for SOAP 1.2
        soapMessage.getMimeHeaders().setHeader("Content-Type", "application/soap+xml; charset=utf-8");

        SOAPElement orderElement = body.addChildElement("ProcessOrderRequest", "ord", NAMESPACE_URI);
        orderElement.addChildElement("OrderId", "ord").setTextContent(orderId);

        soapMessage.saveChanges();

        SOAPConnection connection = soapConnectionFactory.createConnection();
        try {
            // DETECTION: SOAPConnection.call() - same as SOAP 1.1
            SOAPMessage response = connection.call(soapMessage, orderServiceEndpoint);
            return parseOrderResponse(response);
        } finally {
            connection.close();
        }
    }

    /**
     * Send with attachments (MTOM/SwA pattern)
     *
     * DETECTION: AttachmentPart for binary data
     * Used for sending files, images, etc.
     */
    public void processOrderWithAttachment(String orderId, byte[] attachmentData, String contentType)
            throws Exception {

        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPBody body = soapMessage.getSOAPPart().getEnvelope().getBody();

        body.addChildElement("ProcessOrderWithAttachment", NAMESPACE_PREFIX, NAMESPACE_URI)
            .addChildElement("OrderId", NAMESPACE_PREFIX)
            .setTextContent(orderId);

        // DETECTION: AttachmentPart for SOAP attachments
        AttachmentPart attachment = soapMessage.createAttachmentPart();
        attachment.setContent(new java.io.ByteArrayInputStream(attachmentData), contentType);
        attachment.setContentId("orderDocument");
        soapMessage.addAttachmentPart(attachment);

        soapMessage.saveChanges();

        SOAPConnection connection = soapConnectionFactory.createConnection();
        try {
            connection.call(soapMessage, orderServiceEndpoint);
        } finally {
            connection.close();
        }
    }
}
