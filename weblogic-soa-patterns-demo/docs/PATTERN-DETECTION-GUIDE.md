# Pattern Detection Guide

## Complete Reference for Detecting Oracle WebLogic/SOA/OSB Communication in Java Code

This guide provides exhaustive patterns for detecting Java code that communicates with:
- Oracle WebLogic Server
- Oracle Service Bus (OSB)
- Oracle SOA Suite (BPEL)

---

## Quick Reference: Pattern Confidence Levels

| Confidence | Meaning | Action |
|------------|---------|--------|
| **100%** | WebLogic-specific, guaranteed match | Always flag as WebLogic |
| **95%** | Very high confidence | Flag as likely WebLogic/SOA |
| **90%** | High confidence | Flag, verify with context |
| **85%** | Medium-high confidence | Flag, check URL/properties |
| **80%** | Medium confidence | Investigate further |
| **< 80%** | Needs context | Combine with other patterns |

---

## PART 1: OUTGOING PATTERNS (Java → WebLogic/SOA/OSB)

### 1.1 100% WebLogic-Specific Patterns

These patterns **ONLY** exist in WebLogic environments:

```java
// PATTERN 1: WebLogic JNDI Factory (100%)
env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");

// PATTERN 2: T3 Protocol (100%)
env.put(Context.PROVIDER_URL, "t3://weblogic-server:7001");
env.put(Context.PROVIDER_URL, "t3s://weblogic-server:7002");  // SSL

// PATTERN 3: WebLogic Cluster URL (100%)
env.put(Context.PROVIDER_URL, "t3://server1:7001,server2:7001");

// PATTERN 4: WebLogic imports (100%)
import weblogic.jndi.WLInitialContextFactory;
import weblogic.jms.extensions.*;
import weblogic.wsee.*;

// PATTERN 5: WebLogic JMS Unit-of-Order (100%)
message.setStringProperty("JMS_BEA_UnitOfOrder", "...");
```

**Regex Detection:**
```regex
weblogic\.jndi\.WLInitialContextFactory
t3s?://[^"'\s]+
weblogic\.(jndi|jms|wsee)\.
JMS_BEA_UnitOfOrder
```

---

### 1.2 SOAP Client Patterns (95% Confidence)

#### JAX-WS Client (@WebServiceClient)

```java
// PATTERN: @WebServiceClient annotation (100% SOAP client)
@WebServiceClient(
    name = "OrderService",
    targetNamespace = "http://example.com/order/service",
    wsdlLocation = "http://server:7001/soa-infra/services/.../OrderService?wsdl"
)
public class OrderServiceClient { ... }

// PATTERN: Service.create() (95%)
Service service = Service.create(wsdlLocation, SERVICE_QNAME);

// PATTERN: service.getPort() (95%)
OrderServicePortType port = service.getPort(PORT_QNAME, OrderServicePortType.class);

// PATTERN: Port method call (90%)
OrderResponse response = orderServicePort.processOrder(request);

// PATTERN: @WebServiceRef injection (95%)
@WebServiceRef(wsdlLocation = "http://server:7001/...?wsdl")
private OrderService_Service service;
```

**Detection Rules:**
```
IF annotation = "@WebServiceClient" THEN SOAP_CLIENT (100%)
IF class name ends with "PortType" THEN SOAP_PORT (95%)
IF class name ends with "_Service" THEN SOAP_STUB (90%)
IF method call on *PortType THEN SOAP_CALL (90%)
IF wsdlLocation contains "soa-infra" THEN ORACLE_SOA (95%)
IF wsdlLocation contains "?wsdl" THEN SOAP_ENDPOINT (80%)
```

---

#### Spring WebServiceTemplate

```java
// PATTERN: marshalSendAndReceive (95%)
OrderResponse response = (OrderResponse) webServiceTemplate.marshalSendAndReceive(
    endpoint, request);

// PATTERN: With SoapActionCallback (95%)
webServiceTemplate.marshalSendAndReceive(endpoint, request,
    new SoapActionCallback("processOrder"));

// PATTERN: sendSourceAndReceiveToResult (95%)
webServiceTemplate.sendSourceAndReceiveToResult(endpoint, source, result);

// PATTERN: sendAndReceive (95%)
webServiceTemplate.sendAndReceive(endpoint, callback);

// PATTERN: WebServiceGatewaySupport (90%)
public class MyClient extends WebServiceGatewaySupport { ... }
getWebServiceTemplate().marshalSendAndReceive(request);
```

**Detection Rules:**
```
IF method = "marshalSendAndReceive" THEN SPRING_WS_CALL (95%)
IF method = "sendSourceAndReceiveToResult" THEN SPRING_WS_CALL (95%)
IF class extends "WebServiceGatewaySupport" THEN SPRING_WS_CLIENT (90%)
IF uses "SoapActionCallback" THEN SOAP_CALL (85%)
```

---

#### SAAJ Low-Level SOAP

```java
// PATTERN: SOAPConnection.call() (90%)
SOAPMessage response = soapConnection.call(soapRequest, endpoint);

// PATTERN: SOAPConnectionFactory (90%)
SOAPConnectionFactory factory = SOAPConnectionFactory.newInstance();
SOAPConnection conn = factory.createConnection();

// PATTERN: MessageFactory (85%)
MessageFactory messageFactory = MessageFactory.newInstance();
SOAPMessage message = messageFactory.createMessage();

// PATTERN: SOAPEnvelope manipulation (80%)
SOAPEnvelope envelope = soapPart.getEnvelope();
SOAPBody body = envelope.getBody();
SOAPElement elem = body.addChildElement("ProcessOrder", "ns", namespace);
```

**Detection Rules:**
```
IF import "javax.xml.soap.*" THEN SAAJ_API (85%)
IF method = "SOAPConnection.call" THEN SOAP_CALL (90%)
IF variable type = "SOAPMessage" THEN SOAP_MESSAGE (80%)
```

---

### 1.3 JMS Patterns (85% Confidence)

#### Spring JmsTemplate

```java
// PATTERN: jmsTemplate.convertAndSend (85%)
jmsTemplate.convertAndSend("OrderQueue", order);
jmsTemplate.convertAndSend(queueName, order, messagePostProcessor);

// PATTERN: jmsTemplate.send (85%)
jmsTemplate.send("OrderQueue", session -> {
    return session.createTextMessage(xml);
});

// PATTERN: jmsTemplate.sendAndReceive (85%)
Message response = jmsTemplate.sendAndReceive("OrderQueue", messageCreator);

// PATTERN: @Value with queue/topic (75%)
@Value("${weblogic.jms.order-queue}")
private String orderQueue;
```

**Detection Rules:**
```
IF method = "jmsTemplate.convertAndSend" THEN JMS_SEND (85%)
IF method = "jmsTemplate.send" THEN JMS_SEND (85%)
IF method = "jmsTemplate.sendAndReceive" THEN JMS_REQUEST_REPLY (85%)
IF @Value contains "jms" or "queue" or "topic" THEN JMS_DESTINATION (75%)
```

---

#### Raw JMS API

```java
// PATTERN: JNDI lookup for JMS (80%)
ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/ConnectionFactory");
Queue queue = (Queue) ctx.lookup("jms/OrderQueue");

// PATTERN: JMS Connection creation (80%)
Connection connection = connectionFactory.createConnection();
Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

// PATTERN: MessageProducer/QueueSender (85%)
MessageProducer producer = session.createProducer(destination);
producer.send(message);

// OR
QueueSender sender = queueSession.createSender(queue);
sender.send(message);
```

---

### 1.4 REST/HTTP Patterns (70-80% Confidence)

```java
// PATTERN: RestTemplate with SOA URL (varies)
restTemplate.postForObject("http://server:7001/soa-infra/services/...", request, Response.class);
restTemplate.postForObject("http://server:7001/osb/services/...", request, Response.class);

// PATTERN: WebClient with SOA URL (varies)
webClient.post()
    .uri("http://server:7001/soa-infra/services/default/OrderComposite/OrderService")
    .bodyValue(request)
    .retrieve();
```

**URL Pattern Confidence:**
| URL Contains | Confidence |
|--------------|------------|
| `/soa-infra/services/` | 95% Oracle SOA |
| `/osb/` | 95% Oracle OSB |
| `:7001/` | 80% WebLogic |
| `:8001/` | 80% WebLogic |
| `?wsdl` | 80% SOAP service |

---

## PART 2: INCOMING PATTERNS (WebLogic/SOA/OSB → Java)

### 2.1 SOAP Endpoint Patterns (95% Confidence)

```java
// PATTERN: @WebService (server-side) - 95%
@WebService(
    serviceName = "PaymentService",
    portName = "PaymentServicePort",
    targetNamespace = "http://example.com/payment"
)
public class PaymentServiceImpl implements PaymentServicePortType { ... }

// PATTERN: @WebMethod (SOAP operation) - 90%
@WebMethod(operationName = "processPayment")
public PaymentResponse processPayment(PaymentRequest request) { ... }

// PATTERN: @SOAPBinding - 85%
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)

// PATTERN: implements *PortType - 90%
public class MyService implements OrderServicePortType { ... }
```

**Detection Rules:**
```
IF annotation = "@WebService" AND NOT "@WebServiceClient" THEN SOAP_ENDPOINT (95%)
IF annotation = "@WebMethod" THEN SOAP_OPERATION (90%)
IF implements "*PortType" THEN SOAP_IMPLEMENTATION (90%)
```

---

### 2.2 JMS Listener Patterns (95% Confidence)

```java
// PATTERN: @JmsListener (95%)
@JmsListener(destination = "jms/OrderResponseQueue")
public void onOrderResponse(Message message) { ... }

// PATTERN: @JmsListener with concurrency (95%)
@JmsListener(destination = "jms/OrderQueue", concurrency = "1-5")
public void onOrder(@Payload String body, @Header(...) String header) { ... }

// PATTERN: @JmsListener with selector (90%)
@JmsListener(destination = "jms/EventQueue", selector = "EventType = 'ORDER_COMPLETED'")
public void onOrderCompleted(Message message) { ... }

// PATTERN: MessageListener interface (85%)
public class MyListener implements MessageListener {
    @Override
    public void onMessage(Message message) { ... }
}
```

**Detection Rules:**
```
IF annotation = "@JmsListener" THEN JMS_CONSUMER (95%)
IF implements "MessageListener" THEN JMS_CONSUMER (85%)
IF method = "onMessage(Message)" THEN JMS_HANDLER (85%)
```

---

### 2.3 Kafka Listener Patterns (90% Confidence)

```java
// PATTERN: @KafkaListener (90%)
@KafkaListener(topics = "orders.outbound", groupId = "order-consumers")
public void onOrder(OrderMessage order, Acknowledgment ack) { ... }

// PATTERN: @KafkaListener with headers (90%)
@KafkaListener(topics = "${kafka.topics.orders}")
public void onOrder(
    @Payload OrderMessage order,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset) { ... }
```

---

### 2.4 REST Callback Patterns (70% Confidence)

```java
// PATTERN: REST endpoint that OSB calls (needs context)
@RestController
@RequestMapping("/api/callback")
public class OsbCallbackController {

    @PostMapping("/order-status")
    public ResponseEntity<Void> onOrderStatus(@RequestBody OrderStatus status) { ... }
}
```

**Note:** REST endpoints need verification against OSB configuration to confirm they're called by OSB.

---

### 2.5 EJB Patterns (85% Confidence)

```java
// PATTERN: @Stateless EJB (85%)
@Stateless
@Remote(PaymentServiceRemote.class)
public class PaymentServiceBean implements PaymentServiceRemote { ... }

// PATTERN: @EJB client injection (90%)
@EJB(lookup = "ejb/PaymentService")
private PaymentServiceRemote paymentService;
```

---

## PART 3: FILE-BASED DETECTION

### 3.1 WSDL Files

Extract these elements for linking:

```xml
<!-- 1. Service Name (links to @WebServiceClient name) -->
<service name="OrderService">

<!-- 2. Target Namespace (links to @WebServiceClient targetNamespace) -->
<definitions targetNamespace="http://example.com/order/service">

<!-- 3. Endpoint URL (links to wsdlLocation) -->
<soap:address location="http://server:7001/soa-infra/services/..."/>

<!-- 4. Operations (links to Java method calls) -->
<operation name="processOrder">

<!-- 5. Port Type (links to Java interface) -->
<portType name="OrderServicePortType">
```

---

### 3.2 BPEL Files

Extract these elements:

```xml
<!-- 1. Receive (entry point - Java producers call here) -->
<receive partnerLink="Client" operation="processOrder" createInstance="yes"/>

<!-- 2. Invoke (exit point - calls Java consumers) -->
<invoke partnerLink="PaymentService" operation="processPayment"/>

<!-- 3. Partner Links (connect to composite.xml) -->
<partnerLink name="PaymentService" .../>
```

---

### 3.3 composite.xml

The KEY linking file:

```xml
<!-- 1. Exposed Service (entry point from Java) -->
<service name="OrderService">
    <binding.ws location="http://server:7001/soa-infra/services/..."/>
</service>

<!-- 2. External Reference (exit point to Java) -->
<reference name="PaymentServiceRef">
    <binding.ws location="http://payment-server:8080/services/PaymentService"/>
</reference>

<!-- 3. Wires (connect everything) -->
<wire>
    <source.uri>OrderService</source.uri>
    <target.uri>OrderProcessBPEL/client</target.uri>
</wire>
```

---

## PART 4: LINKING ALGORITHM SUMMARY

### Step 1: Find Java Producers
```
Search for:
- @WebServiceClient → Extract name, namespace, wsdlLocation
- webServiceTemplate.marshalSendAndReceive() → Extract endpoint
- jmsTemplate.convertAndSend() → Extract destination
- WLInitialContextFactory / t3:// → Confirm WebLogic
```

### Step 2: Match to SOA Entry Points
```
Match producer to:
- WSDL <service name> = @WebServiceClient name
- WSDL <soap:address> = wsdlLocation (minus ?wsdl)
- BPEL <receive operation> = Java method name
```

### Step 3: Find SOA Exit Points
```
In BPEL, find:
- <invoke partnerLink="X" operation="Y">

In composite.xml, trace:
- <wire source.uri="BPELComponent/X" target.uri="ReferenceZ">
- <reference name="ReferenceZ"> → <binding.ws location="...">
```

### Step 4: Match to Java Consumers
```
Match reference to:
- @WebService(serviceName) = WSDL service name
- @WebMethod(operationName) = BPEL invoke operation
- @JmsListener(destination) = JMS adapter queue name
```

### Step 5: Create Link
```
Result:
JavaProducer.java → [CALLS via SOA] → JavaConsumer.java
```

---

## PART 5: REGEX PATTERNS FOR DETECTION

### Imports
```regex
import\s+javax\.xml\.ws\.(Service|WebServiceClient|WebServiceRef|BindingProvider)
import\s+javax\.xml\.soap\.
import\s+javax\.jws\.(WebService|WebMethod|WebParam)
import\s+org\.springframework\.ws\.
import\s+org\.springframework\.jms\.
import\s+org\.apache\.(axis|cxf)\.
import\s+weblogic\.(jndi|jms|wsee)\.
```

### Annotations
```regex
@WebServiceClient\s*\(
@WebService\s*\(
@WebServiceRef\s*\(
@WebMethod\s*\(
@JmsListener\s*\(
@KafkaListener\s*\(
@SOAPBinding\s*\(
```

### Method Calls
```regex
\.marshalSendAndReceive\s*\(
\.sendSourceAndReceiveToResult\s*\(
jmsTemplate\.\w*[Ss]end\s*\(
kafkaTemplate\.send\s*\(
\.getPort\s*\(
Service\.create\s*\(
soapConnection\.call\s*\(
```

### URLs
```regex
(https?://[^"'\s]*)/soa-infra/services/
(https?://[^"'\s]*)/osb/
t3s?://[^"'\s]+
:[78]001/
\?wsdl
```

---

## Summary: Detection Priority

| Priority | Pattern | Confidence |
|----------|---------|------------|
| 1 | `weblogic.jndi.WLInitialContextFactory` | 100% |
| 2 | `t3://` or `t3s://` URL | 100% |
| 3 | `/soa-infra/services/` in URL | 95% |
| 4 | `/osb/` in URL | 95% |
| 5 | `@WebServiceClient` annotation | 100% |
| 6 | `@WebService` annotation (server-side) | 95% |
| 7 | `@JmsListener` annotation | 95% |
| 8 | `*PortType` interface | 95% |
| 9 | `marshalSendAndReceive()` method | 95% |
| 10 | `jmsTemplate.send()` methods | 85% |
