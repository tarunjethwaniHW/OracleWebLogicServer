# WebLogic SOA Patterns Demo - Project Summary and Workflow

## Purpose

This project demonstrates **ALL possible ways** Java applications communicate with Oracle WebLogic Server, Oracle Service Bus (OSB), and Oracle SOA Suite (BPEL). Use this as a reference for detecting communication patterns in customer Java codebases.

---

## Project Structure

```
weblogic-soa-patterns-demo/
│
├── README.md                                    # Project overview
│
├── java-producers/                              # OUTGOING: Java → WebLogic/SOA
│   └── src/main/java/com/example/producer/
│       ├── soap/
│       │   ├── JaxWsClientProducer.java         # @WebServiceClient (100% SOAP)
│       │   ├── WebServiceTemplateProducer.java  # Spring WS (95%)
│       │   └── SaajClientProducer.java          # Low-level SOAP (90%)
│       └── jms/
│           ├── WebLogicJndiProducer.java        # 100% WebLogic-specific (t3://)
│           └── JmsTemplateProducer.java         # Spring JmsTemplate (85%)
│
├── java-consumers/                              # INCOMING: WebLogic/SOA → Java
│   └── src/main/java/com/example/consumer/
│       ├── soap/
│       │   └── JaxWsEndpointConsumer.java       # @WebService endpoint (95%)
│       └── jms/
│           └── JmsListenerConsumer.java         # @JmsListener (95%)
│
├── soa-composite/                               # Oracle SOA artifacts
│   ├── composite.xml                            # KEY LINKING FILE!
│   └── SOA/
│       ├── BPEL/
│       │   └── OrderProcess.bpel                # <receive> and <invoke>
│       └── WSDLs/
│           ├── OrderService.wsdl                # Entry WSDL
│           └── PaymentService.wsdl              # Exit WSDL
│
├── config/
│   └── application.properties                   # All WebLogic/SOA properties
│
└── docs/
    ├── PATTERN-DETECTION-GUIDE.md               # Complete detection reference
    └── PROJECT-SUMMARY-AND-WORKFLOW.md          # This file
```

---

## What Each File Demonstrates

### 100% WebLogic-Specific Patterns (Guaranteed Detection)

| Pattern | File | Line |
|---------|------|------|
| `weblogic.jndi.WLInitialContextFactory` | `WebLogicJndiProducer.java` | 42 |
| `t3://` protocol | `WebLogicJndiProducer.java` | 46-48 |
| `JMS_BEA_UnitOfOrder` | `WebLogicJndiProducer.java` | 178 |

These patterns are **ONLY** found in Oracle WebLogic environments. If you detect any of these, you have 100% certainty the code communicates with WebLogic.

---

### SOAP Patterns

| Producer Type | File | Key Pattern | Confidence |
|---------------|------|-------------|------------|
| JAX-WS Client | `JaxWsClientProducer.java` | `@WebServiceClient`, `port.processOrder()` | 100% |
| Spring WS | `WebServiceTemplateProducer.java` | `marshalSendAndReceive()` | 95% |
| SAAJ | `SaajClientProducer.java` | `SOAPConnection.call()` | 90% |

---

### JMS Patterns

| Type | File | Key Pattern | Confidence |
|------|------|-------------|------------|
| WebLogic JNDI | `WebLogicJndiProducer.java` | `WLInitialContextFactory`, `t3://` | 100% |
| Spring JMS | `JmsTemplateProducer.java` | `jmsTemplate.convertAndSend()` | 85% |

---

### Consumer Patterns

| Consumer Type | File | Key Pattern | Confidence |
|---------------|------|-------------|------------|
| SOAP Endpoint | `JaxWsEndpointConsumer.java` | `@WebService`, `@WebMethod` | 95% |
| JMS Listener | `JmsListenerConsumer.java` | `@JmsListener(destination=...)` | 95% |

---

## Complete Workflow Visualization

### High-Level Flow

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              END-TO-END MESSAGE FLOW                                    │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   STEP 1                    STEP 2                    STEP 3                            │
│   ══════                    ══════                    ══════                            │
│                                                                                         │
│   ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐               │
│   │ JAVA PRODUCER   │       │ ORACLE SOA/OSB  │       │ JAVA CONSUMER   │               │
│   │                 │       │                 │       │                 │               │
│   │ JaxWsClient     │ ───► │ WSDL + BPEL     │ ───► │ JaxWsEndpoint   │               │
│   │ Producer.java   │ SOAP │ + composite.xml │ SOAP │ Consumer.java   │               │
│   │                 │      │                 │      │                 │               │
│   │ JmsTemplate     │ ───► │ JMS Proxy/      │ ───► │ JmsListener     │               │
│   │ Producer.java   │ JMS  │ Adapter         │ JMS  │ Consumer.java   │               │
│   └─────────────────┘       └─────────────────┘       └─────────────────┘               │
│                                                                                         │
│   YOUR CUSTOMER'S           ORACLE MIDDLEWARE         YOUR CUSTOMER'S                  │
│   JAVA APPLICATION          (WebLogic Server)         JAVA APPLICATION                 │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### Detailed Complete Linking Chain

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           COMPLETE LINKING CHAIN                                        │
│                     (File-by-File Tracing from Producer to Consumer)                    │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 1: JAVA PRODUCER (Sends request to Oracle SOA)                             │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: JaxWsClientProducer.java                                                │   │
│   │                                                                                 │   │
│   │   @WebServiceClient(name = "OrderService")         ◄── DETECTION POINT 1        │   │
│   │   port.processOrder(request)                       ◄── DETECTION POINT 2        │   │
│   │                                                                                 │   │
│   │   Extract: name="OrderService", operation="processOrder"                        │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ Match by: service name + operation           │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 2: WSDL (Service Contract - Entry Point)                                   │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: OrderService.wsdl                                                       │   │
│   │                                                                                 │   │
│   │   <service name="OrderService">                    ◄── Matches Java client      │   │
│   │   <operation name="processOrder">                  ◄── Matches method call      │   │
│   │   <soap:address location="http://server:7001/soa-infra/services/..."/>          │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ WSDL referenced by composite.xml             │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 3: COMPOSITE.XML (Wiring Configuration - THE KEY LINKING FILE!)            │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: composite.xml                                                           │   │
│   │                                                                                 │   │
│   │   <!-- Entry Point -->                                                          │   │
│   │   <service name="OrderService">                                                 │   │
│   │       <binding.ws location="http://server:7001/soa-infra/..."/>                 │   │
│   │   </service>                                                                    │   │
│   │                                                                                 │   │
│   │   <!-- Wire to BPEL -->                                                         │   │
│   │   <wire>                                                                        │   │
│   │       <source.uri>OrderService</source.uri>                                     │   │
│   │       <target.uri>OrderProcessBPEL/client</target.uri>                          │   │
│   │   </wire>                                                                       │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ Wire connects to BPEL component              │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 4: BPEL (Orchestration Logic)                                              │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: OrderProcess.bpel                                                       │   │
│   │                                                                                 │   │
│   │   <!-- ENTRY POINT: Receives from Java Producer -->                             │   │
│   │   <receive partnerLink="Client"                                                 │   │
│   │            operation="processOrder"                ◄── Matches Java call        │   │
│   │            createInstance="yes"/>                                               │   │
│   │                                                                                 │   │
│   │   ... business logic ...                                                        │   │
│   │                                                                                 │   │
│   │   <!-- EXIT POINT: Calls Java Consumer -->                                      │   │
│   │   <invoke partnerLink="PaymentService"             ◄── DETECTION POINT 3        │   │
│   │           operation="processPayment"/>             ◄── DETECTION POINT 4        │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ partnerLink traced via composite.xml         │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 5: COMPOSITE.XML (Wire to External Reference)                              │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: composite.xml                                                           │   │
│   │                                                                                 │   │
│   │   <!-- Wire from BPEL to Reference -->                                          │   │
│   │   <wire>                                                                        │   │
│   │       <source.uri>OrderProcessBPEL/PaymentService</source.uri>                  │   │
│   │       <target.uri>PaymentServiceRef</target.uri>                                │   │
│   │   </wire>                                                                       │   │
│   │                                                                                 │   │
│   │   <!-- External Reference (Exit Point) -->                                      │   │
│   │   <reference name="PaymentServiceRef">                                          │   │
│   │       <binding.ws location="http://payment-server:8080/services/PaymentService" │   │
│   │   </reference>                      ▲                                           │   │
│   │                                     │                                           │   │
│   │                                     └── This URL points to Java Consumer!       │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ URL points to Java consumer endpoint         │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 6: WSDL (Target Service Contract)                                          │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: PaymentService.wsdl                                                     │   │
│   │                                                                                 │   │
│   │   <service name="PaymentService">                  ◄── Matches Java @WebService │   │
│   │   <operation name="processPayment">                ◄── Matches Java @WebMethod  │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ Match by: serviceName + operationName        │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ STEP 7: JAVA CONSUMER (Receives request from Oracle SOA)                        │   │
│   ├─────────────────────────────────────────────────────────────────────────────────┤   │
│   │                                                                                 │   │
│   │   File: JaxWsEndpointConsumer.java                                              │   │
│   │                                                                                 │   │
│   │   @WebService(serviceName = "PaymentService")      ◄── DETECTION POINT 5        │   │
│   │   @WebMethod(operationName = "processPayment")     ◄── DETECTION POINT 6        │   │
│   │   public PaymentResponse processPayment(PaymentRequest request) {               │   │
│   │       // This method is called by Oracle SOA BPEL!                              │   │
│   │   }                                                                             │   │
│   │                                                                                 │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                         │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   ╔═════════════════════════════════════════════════════════════════════════════════╗   │
│   ║                                                                                 ║   │
│   ║   FINAL RESULT:                                                                 ║   │
│   ║                                                                                 ║   │
│   ║   JaxWsClientProducer.java ═══[CALLS via Oracle SOA]═══> JaxWsEndpointConsumer  ║   │
│   ║                                                                 .java           ║   │
│   ║                                                                                 ║   │
│   ╚═════════════════════════════════════════════════════════════════════════════════╝   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### JMS Workflow (Alternative Path)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           JMS LINKING CHAIN                                             │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│   JmsTemplateProducer.java                                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ jmsTemplate.convertAndSend("jms/OrderRequestQueue", order)                      │   │
│   │                              ▲                                                  │   │
│   │                              │                                                  │   │
│   │                              └── DESTINATION NAME TO EXTRACT                    │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ Queue name: "jms/OrderRequestQueue"          │
│                                          ▼                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ WebLogic JMS / OSB JMS Proxy                                                    │   │
│   │                                                                                 │   │
│   │ Message stored in queue, processed by OSB/SOA, routed to response queue         │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                          │                                              │
│                                          │ Response sent to: "jms/OrderResponseQueue"   │
│                                          ▼                                              │
│   JmsListenerConsumer.java                                                              │
│   ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│   │ @JmsListener(destination = "jms/OrderResponseQueue")                            │   │
│   │                              ▲                                                  │   │
│   │                              │                                                  │   │
│   │                              └── DESTINATION NAME TO MATCH                      │   │
│   │ public void onOrderResponse(Message message) { ... }                            │   │
│   └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                         │
│   ╔═════════════════════════════════════════════════════════════════════════════════╗   │
│   ║                                                                                 ║   │
│   ║   FINAL RESULT:                                                                 ║   │
│   ║                                                                                 ║   │
│   ║   JmsTemplateProducer.java ═══[via WebLogic JMS]═══> JmsListenerConsumer.java   ║   │
│   ║                                                                                 ║   │
│   ╚═════════════════════════════════════════════════════════════════════════════════╝   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Detection Summary

### Priority Order for Pattern Detection

| Priority | Pattern | Where to Find | Confidence |
|----------|---------|---------------|------------|
| 1 | `weblogic.jndi.WLInitialContextFactory` | Java imports/code | **100%** |
| 2 | `t3://` or `t3s://` URL | Properties/code | **100%** |
| 3 | `/soa-infra/services/` in URL | Properties/code | **95%** |
| 4 | `/osb/` in URL | Properties/code | **95%** |
| 5 | `@WebServiceClient` annotation | Java class | **100%** |
| 6 | `@WebService` annotation (server) | Java class | **95%** |
| 7 | `@JmsListener` annotation | Java method | **95%** |
| 8 | `*PortType` interface | Java interface | **95%** |
| 9 | `marshalSendAndReceive()` | Java method call | **95%** |
| 10 | `jmsTemplate.send()` methods | Java method call | **85%** |

---

## Files to Analyze for Linking

### In Customer's Java Code

| File Type | Look For | Extract |
|-----------|----------|---------|
| `*.java` | `@WebServiceClient` | `name`, `targetNamespace`, `wsdlLocation` |
| `*.java` | `@WebService` | `serviceName`, `portName` |
| `*.java` | `@JmsListener` | `destination` |
| `*.java` | `port.*()` calls | Operation name |
| `*.properties` | `soa.*`, `osb.*`, `weblogic.*` | Endpoint URLs, queue names |

### In SOA Artifacts (if available)

| File Type | Look For | Extract |
|-----------|----------|---------|
| `*.wsdl` | `<service name>` | Service name, endpoint URL |
| `*.wsdl` | `<operation name>` | Operation names |
| `*.bpel` | `<receive>` | Entry point operations |
| `*.bpel` | `<invoke>` | Exit point partner links |
| `composite.xml` | `<service>` | Exposed entry points |
| `composite.xml` | `<reference>` | External service URLs |
| `composite.xml` | `<wire>` | Connection mappings |

---

## Key Insight: composite.xml is the Rosetta Stone

**composite.xml** is the single most important file for linking because it:

1. **Defines entry points** (`<service>`) where Java producers connect
2. **Contains BPEL reference** (`<component>`) that processes requests
3. **Defines exit points** (`<reference>`) where BPEL calls Java consumers
4. **Wires everything together** (`<wire>`) showing the complete flow

If you have access to `composite.xml`, you can trace the entire flow from producer to consumer.

---

## Quick Detection Checklist

### Is this Java code talking to WebLogic/SOA/OSB?

- [ ] Contains `weblogic.jndi.WLInitialContextFactory` → **YES (100%)**
- [ ] Contains `t3://` or `t3s://` URL → **YES (100%)**
- [ ] Contains URL with `/soa-infra/services/` → **YES (95%)**
- [ ] Contains URL with `/osb/` → **YES (95%)**
- [ ] Has `@WebServiceClient` annotation → **SOAP Client**
- [ ] Has `@WebService` annotation (no `@WebServiceClient`) → **SOAP Endpoint**
- [ ] Has `@JmsListener` annotation → **JMS Consumer**
- [ ] Uses `jmsTemplate.send()` or `convertAndSend()` → **JMS Producer**
- [ ] Implements `*PortType` interface → **SOAP Implementation**
