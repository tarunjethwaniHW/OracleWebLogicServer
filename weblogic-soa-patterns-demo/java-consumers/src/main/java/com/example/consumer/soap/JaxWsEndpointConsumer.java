package com.example.consumer.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.BindingType;

/**
 * JAX-WS SOAP Endpoint Consumer
 *
 * PATTERN DETECTION: This Java class RECEIVES calls from Oracle SOA/BPEL/OSB.
 * When BPEL has <invoke partnerLink="PaymentService" operation="processPayment">,
 * it calls THIS endpoint.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS - INCOMING SOAP (SOA → Java)                                    │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. @WebService annotation (without @WebServiceClient)        → 95% SOAP Endpoint   │
 * │ 2. @WebService(serviceName = "...")                          → 95% Service Name    │
 * │ 3. @WebService(portName = "...")                             → 90% Port Name       │
 * │ 4. @WebMethod annotation                                     → 90% SOAP Operation  │
 * │ 5. @SOAPBinding annotation                                   → 85% Binding Style   │
 * │ 6. @WebParam, @WebResult annotations                         → 80% SOAP Params     │
 * │ 7. implements *PortType interface                            → 90% WSDL Interface  │
 * │ 8. Import: javax.jws.*                                       → 85% JAX-WS          │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract serviceName, targetNamespace from @WebService
 * - Match to BPEL <invoke partnerLink="..."> or composite.xml <reference>
 * - Match to WSDL <service name="..."> that BPEL/OSB uses
 */
@WebService(
    serviceName = "PaymentService",                           // Matches WSDL <service name>
    portName = "PaymentServicePort",                          // Matches WSDL <port name>
    targetNamespace = "http://example.com/payment/service",   // Matches WSDL targetNamespace
    endpointInterface = "com.example.consumer.soap.PaymentServicePortType"  // Interface contract
)
@SOAPBinding(
    style = SOAPBinding.Style.DOCUMENT,
    use = SOAPBinding.Use.LITERAL,
    parameterStyle = SOAPBinding.ParameterStyle.WRAPPED
)
public class JaxWsEndpointConsumer implements PaymentServicePortType {

    /**
     * Process Payment operation - called by BPEL/OSB
     *
     * DETECTION:
     * - @WebMethod marks this as a SOAP operation
     * - operationName matches WSDL <operation name="processPayment">
     * - This is what BPEL <invoke operation="processPayment"> calls
     *
     * LINKING:
     * - BPEL <invoke partnerLink="PaymentService" operation="processPayment">
     *   → composite.xml <reference name="PaymentServiceRef">
     *   → This method
     */
    @Override
    @WebMethod(operationName = "processPayment")
    @WebResult(name = "PaymentResponse")
    public PaymentResponse processPayment(
            @WebParam(name = "paymentRequest") PaymentRequest request) {

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("INCOMING SOAP CALL from Oracle SOA/BPEL");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Operation: processPayment");
        System.out.println("Order ID: " + request.getOrderId());
        System.out.println("Amount: " + request.getAmount());

        // Business logic - process the payment
        PaymentResponse response = new PaymentResponse();
        response.setOrderId(request.getOrderId());
        response.setApproved(true);
        response.setAuthorizationCode("AUTH-" + System.currentTimeMillis());
        response.setMessage("Payment processed successfully");

        System.out.println("Response: " + response.getAuthorizationCode());
        return response;
    }

    /**
     * Refund operation
     *
     * DETECTION: Another @WebMethod = another SOAP operation
     */
    @Override
    @WebMethod(operationName = "refundPayment")
    @WebResult(name = "RefundResponse")
    public RefundResponse refundPayment(
            @WebParam(name = "refundRequest") RefundRequest request) {

        System.out.println("INCOMING SOAP CALL: refundPayment");
        System.out.println("Original Order: " + request.getOriginalOrderId());
        System.out.println("Refund Amount: " + request.getRefundAmount());

        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setRefundId("REF-" + System.currentTimeMillis());

        return response;
    }

    /**
     * Check Payment Status
     */
    @Override
    @WebMethod(operationName = "getPaymentStatus")
    @WebResult(name = "PaymentStatus")
    public PaymentStatus getPaymentStatus(
            @WebParam(name = "orderId") String orderId) {

        System.out.println("INCOMING SOAP CALL: getPaymentStatus for " + orderId);

        PaymentStatus status = new PaymentStatus();
        status.setOrderId(orderId);
        status.setStatus("COMPLETED");
        status.setProcessedAt(java.time.LocalDateTime.now().toString());

        return status;
    }
}

// ============================================================================
// PORT TYPE INTERFACE
// DETECTION: Interface implemented by @WebService class
// Matches WSDL <portType name="PaymentServicePortType">
// ============================================================================
interface PaymentServicePortType {
    PaymentResponse processPayment(PaymentRequest request);
    RefundResponse refundPayment(RefundRequest request);
    PaymentStatus getPaymentStatus(String orderId);
}

// ============================================================================
// DTOs - Match WSDL message types
// ============================================================================
class PaymentRequest {
    private String orderId;
    private double amount;
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }
    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
}

class PaymentResponse {
    private String orderId;
    private boolean approved;
    private String authorizationCode;
    private String message;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String code) { this.authorizationCode = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

class RefundRequest {
    private String originalOrderId;
    private String authorizationCode;
    private double refundAmount;
    private String reason;

    public String getOriginalOrderId() { return originalOrderId; }
    public void setOriginalOrderId(String id) { this.originalOrderId = id; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String code) { this.authorizationCode = code; }
    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double amount) { this.refundAmount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

class RefundResponse {
    private boolean success;
    private String refundId;
    private String message;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getRefundId() { return refundId; }
    public void setRefundId(String refundId) { this.refundId = refundId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

class PaymentStatus {
    private String orderId;
    private String status;
    private String processedAt;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessedAt() { return processedAt; }
    public void setProcessedAt(String processedAt) { this.processedAt = processedAt; }
}
