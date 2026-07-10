package com.example.producer.soap;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceRef;
import java.net.URL;
import java.util.Map;

/**
 * JAX-WS SOAP Client Producer
 *
 * PATTERN DETECTION: This is the MOST COMMON way to call Oracle SOA/WebLogic via SOAP.
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────┐
 * │ DETECTION PATTERNS (Ranked by Confidence)                                          │
 * ├─────────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. @WebServiceClient annotation                              → 100% SOAP Client    │
 * │ 2. @WebServiceRef annotation (injection)                     → 100% SOAP Client    │
 * │ 3. Interface ending with "PortType"                          → 95% SOAP Port       │
 * │ 4. Class ending with "_Service"                              → 90% Generated Stub  │
 * │ 5. Service.create(URL, QName) call                           → 95% SOAP Setup      │
 * │ 6. service.getPort(QName, Class) call                        → 95% Get Port        │
 * │ 7. port.operationName() call on PortType                     → 90% SOAP Call       │
 * │ 8. Import: javax.xml.ws.*                                    → 85% JAX-WS          │
 * │ 9. URL containing "/soa-infra/services/"                     → 95% Oracle SOA      │
 * │ 10. URL containing "?wsdl"                                   → 80% WSDL endpoint   │
 * └─────────────────────────────────────────────────────────────────────────────────────┘
 *
 * LINKING:
 * - Extract: name, targetNamespace, wsdlLocation from @WebServiceClient
 * - Match to: WSDL <service name="..."> and <portType>
 * - Trace to: BPEL <receive operation="...">
 */
@WebServiceClient(
    name = "OrderService",
    targetNamespace = "http://example.com/order/service",
    wsdlLocation = "http://soa-server:7001/soa-infra/services/default/OrderProcessingComposite/OrderService?wsdl"
)
public class JaxWsClientProducer {

    // QNames match WSDL definitions - use these for linking
    private static final QName SERVICE_QNAME =
        new QName("http://example.com/order/service", "OrderService");
    private static final QName PORT_QNAME =
        new QName("http://example.com/order/service", "OrderServicePort");

    // Port interface - naming convention: *PortType
    // DETECTION: Field type ending with "PortType" = SOAP port
    private OrderServicePortType orderServicePort;

    // Alternative: Use @WebServiceRef for container-managed injection
    // DETECTION: @WebServiceRef = Container injects SOAP service
    @WebServiceRef(wsdlLocation = "http://soa-server:7001/soa-infra/services/default/OrderService?wsdl")
    private OrderService_Service injectedService;

    /**
     * Constructor using programmatic service creation
     *
     * DETECTION POINTS:
     * - Service.create(URL, QName) → Creating SOAP service from WSDL
     * - service.getPort(QName, Class) → Getting the callable port
     */
    public JaxWsClientProducer(String wsdlUrl) throws Exception {
        URL wsdlLocation = new URL(wsdlUrl);

        // DETECTION: Service.create() = SOAP service instantiation
        Service service = Service.create(wsdlLocation, SERVICE_QNAME);

        // DETECTION: getPort() = Getting callable SOAP port
        this.orderServicePort = service.getPort(PORT_QNAME, OrderServicePortType.class);

        // Configure WebLogic-specific settings
        configureWebLogicBinding((BindingProvider) orderServicePort);
    }

    /**
     * Call Order Processing operation via SOAP
     *
     * DETECTION: Method call on *PortType interface
     * - function_class = "OrderServicePortType"
     * - function_name = "processOrder"
     *
     * LINKING: Maps to WSDL <operation name="processOrder">
     *          Then to BPEL <receive operation="processOrder">
     */
    public OrderResponse processOrder(OrderRequest request) {
        System.out.println("Calling SOA via JAX-WS SOAP: processOrder");

        // THIS IS THE KEY CALLEE TO DETECT
        // Links to: WSDL <operation name="processOrder">
        // Links to: BPEL <receive partnerLink="Client" operation="processOrder">
        OrderResponse response = orderServicePort.processOrder(request);

        return response;
    }

    /**
     * Cancel Order operation
     *
     * DETECTION: Another operation on the same PortType
     */
    public CancelResponse cancelOrder(String orderId) {
        CancelRequest request = new CancelRequest();
        request.setOrderId(orderId);

        // CALLEE: orderServicePort.cancelOrder()
        // Maps to: WSDL <operation name="cancelOrder">
        return orderServicePort.cancelOrder(request);
    }

    /**
     * Get Order Status operation
     */
    public OrderStatusResponse getOrderStatus(String orderId) {
        OrderStatusRequest request = new OrderStatusRequest();
        request.setOrderId(orderId);

        // CALLEE: orderServicePort.getOrderStatus()
        return orderServicePort.getOrderStatus(request);
    }

    /**
     * Configure WebLogic-specific binding properties
     *
     * DETECTION: BindingProvider configuration
     * - WebLogic credentials
     * - Timeout settings
     */
    private void configureWebLogicBinding(BindingProvider bp) {
        Map<String, Object> ctx = bp.getRequestContext();

        // WebLogic authentication
        ctx.put(BindingProvider.USERNAME_PROPERTY, "weblogic");
        ctx.put(BindingProvider.PASSWORD_PROPERTY, "welcome1");

        // WebLogic timeout properties (vendor-specific)
        ctx.put("com.sun.xml.ws.connect.timeout", 30000);
        ctx.put("com.sun.xml.ws.request.timeout", 120000);

        // WebLogic-specific: JAX-WS RI properties
        ctx.put("com.sun.xml.internal.ws.connect.timeout", 30000);
        ctx.put("com.sun.xml.internal.ws.request.timeout", 120000);
    }
}

// ============================================================================
// PORT TYPE INTERFACE
// In real projects, this is GENERATED from WSDL using wsimport tool
// DETECTION: Interface name ending with "PortType"
// ============================================================================
interface OrderServicePortType {
    OrderResponse processOrder(OrderRequest request);
    CancelResponse cancelOrder(CancelRequest request);
    OrderStatusResponse getOrderStatus(OrderStatusRequest request);
}

// ============================================================================
// SERVICE CLASS (Generated stub pattern)
// DETECTION: Class name ending with "_Service"
// ============================================================================
class OrderService_Service extends Service {
    public OrderService_Service(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OrderServicePortType getOrderServicePort() {
        return super.getPort(OrderServicePortType.class);
    }
}

// ============================================================================
// DTOs - Would be generated from WSDL XSD schemas
// ============================================================================
class OrderRequest {
    private String orderId;
    private String customerId;
    private double amount;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

class OrderResponse {
    private String status;
    private String confirmationNumber;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConfirmationNumber() { return confirmationNumber; }
    public void setConfirmationNumber(String num) { this.confirmationNumber = num; }
}

class CancelRequest {
    private String orderId;
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

class CancelResponse {
    private boolean success;
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}

class OrderStatusRequest {
    private String orderId;
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}

class OrderStatusResponse {
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
