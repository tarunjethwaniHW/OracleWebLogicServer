package com.example.producer.soap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

/**
 * Spring WebServiceTemplate SOAP Producer
 *
 * PATTERN DETECTION: Spring's preferred way to call SOAP services.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS (Ranked by Confidence)                                          │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. webServiceTemplate.marshalSendAndReceive()                → 95% SOAP Call       │
 * │ 2. webServiceTemplate.sendSourceAndReceiveToResult()         → 95% SOAP Call       │
 * │ 3. webServiceTemplate.sendAndReceive()                       → 95% SOAP Call       │
 * │ 4. extends WebServiceGatewaySupport                          → 90% Spring WS       │
 * │ 5. getWebServiceTemplate() call                              → 90% Spring WS       │
 * │ 6. SoapActionCallback usage                                  → 85% SOAP Action     │
 * │ 7. Import: org.springframework.ws.*                          → 85% Spring WS       │
 * │ 8. @Value with endpoint containing "soa-infra"               → 95% Oracle SOA      │
 * │ 9. Jaxb2Marshaller configuration                             → 80% SOAP Marshaling │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract endpoint URL from @Value or setDefaultUri()
 * - Extract SOAPAction from SoapActionCallback
 * - Match to WSDL service endpoint
 */
@Service
public class WebServiceTemplateProducer extends WebServiceGatewaySupport {

    // DETECTION: @Value with SOA endpoint URL
    // Look for: soa-infra, osb, :7001, :8001
    @Value("${soa.order.endpoint:http://soa-server:7001/soa-infra/services/default/OrderComposite/OrderService}")
    private String orderServiceEndpoint;

    @Value("${soa.payment.endpoint:http://soa-server:7001/soa-infra/services/default/PaymentComposite/PaymentService}")
    private String paymentServiceEndpoint;

    @Value("${soa.inventory.endpoint}")
    private String inventoryServiceEndpoint;

    // WebServiceTemplate - Spring's SOAP client
    // DETECTION: Field of type WebServiceTemplate
    private final WebServiceTemplate webServiceTemplate;

    /**
     * Constructor with WebServiceTemplate injection
     *
     * DETECTION: WebServiceTemplate bean configuration
     */
    public WebServiceTemplateProducer(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    /**
     * Call Order Service via SOAP using marshalSendAndReceive
     *
     * DETECTION: marshalSendAndReceive() is the PRIMARY indicator
     * - Parameter 1: Endpoint URL (resolve from @Value if variable)
     * - Parameter 2: Request object (marshaled to SOAP body)
     * - Return: Response object (unmarshaled from SOAP response)
     *
     * LINKING:
     * - Extract endpoint URL → match to WSDL <soap:address>
     * - SOAPAction → match to WSDL <soap:operation soapAction="">
     */
    public OrderResponse processOrder(OrderRequest request) {
        System.out.println("Calling SOA via Spring WebServiceTemplate: processOrder");

        // THIS IS THE KEY CALLEE TO DETECT
        // Pattern: webServiceTemplate.marshalSendAndReceive(endpoint, request)
        OrderResponse response = (OrderResponse) webServiceTemplate.marshalSendAndReceive(
            orderServiceEndpoint,
            request
        );

        return response;
    }

    /**
     * Call with SOAPAction callback
     *
     * DETECTION: SoapActionCallback indicates SOAP operation name
     */
    public OrderResponse processOrderWithAction(OrderRequest request) {
        // DETECTION: marshalSendAndReceive with SoapActionCallback
        // The SOAPAction often matches the operation name in WSDL
        OrderResponse response = (OrderResponse) webServiceTemplate.marshalSendAndReceive(
            orderServiceEndpoint,
            request,
            new SoapActionCallback("http://example.com/order/processOrder")
        );

        return response;
    }

    /**
     * Call Payment Service
     *
     * DETECTION: Same pattern, different endpoint
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        // CALLEE: webServiceTemplate.marshalSendAndReceive()
        // Endpoint: paymentServiceEndpoint (resolve from @Value)
        return (PaymentResponse) webServiceTemplate.marshalSendAndReceive(
            paymentServiceEndpoint,
            request,
            new SoapActionCallback("processPayment")
        );
    }

    /**
     * Call Inventory Service
     */
    public InventoryResponse checkInventory(InventoryRequest request) {
        return (InventoryResponse) webServiceTemplate.marshalSendAndReceive(
            inventoryServiceEndpoint,
            request
        );
    }

    /**
     * Alternative: Using inherited WebServiceGatewaySupport
     *
     * DETECTION: getWebServiceTemplate() from parent class
     */
    public OrderResponse processOrderViaGateway(OrderRequest request) {
        // DETECTION: getWebServiceTemplate().marshalSendAndReceive()
        // When class extends WebServiceGatewaySupport
        return (OrderResponse) getWebServiceTemplate().marshalSendAndReceive(request);
    }

    /**
     * Using sendSourceAndReceiveToResult for raw XML
     *
     * DETECTION: Alternative WebServiceTemplate method
     */
    public void processOrderRawXml(javax.xml.transform.Source requestSource,
                                    javax.xml.transform.Result responseResult) {
        // DETECTION: sendSourceAndReceiveToResult
        // Lower-level API, works with raw XML
        webServiceTemplate.sendSourceAndReceiveToResult(
            orderServiceEndpoint,
            requestSource,
            responseResult
        );
    }
}

// ============================================================================
// CONFIGURATION CLASS - How WebServiceTemplate is typically configured
// DETECTION: @Bean methods creating WebServiceTemplate
// ============================================================================
/*
@Configuration
class WebServiceConfig {

    // DETECTION: @Bean returning WebServiceTemplate
    @Bean
    public WebServiceTemplate webServiceTemplate(Jaxb2Marshaller marshaller) {
        WebServiceTemplate template = new WebServiceTemplate();

        // DETECTION: setDefaultUri with SOA endpoint
        template.setDefaultUri("http://soa-server:7001/soa-infra/services/default/OrderComposite/OrderService");

        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);

        // DETECTION: HttpComponentsMessageSender for timeout config
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        sender.setConnectionTimeout(30000);
        sender.setReadTimeout(120000);
        template.setMessageSender(sender);

        return template;
    }

    // DETECTION: Jaxb2Marshaller with context paths
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.example.generated.soap");
        return marshaller;
    }
}
*/

// ============================================================================
// DTOs
// ============================================================================
class PaymentRequest {
    private String orderId;
    private double amount;
    private String cardNumber;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
}

class PaymentResponse {
    private boolean approved;
    private String authorizationCode;

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String code) { this.authorizationCode = code; }
}

class InventoryRequest {
    private String productId;
    private int quantity;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

class InventoryResponse {
    private boolean available;
    private int availableQuantity;

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int qty) { this.availableQuantity = qty; }
}
