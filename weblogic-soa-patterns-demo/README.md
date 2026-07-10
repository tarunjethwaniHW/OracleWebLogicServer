# WebLogic SOA Communication Patterns Demo

## Purpose

This project demonstrates **ALL possible ways** Java applications communicate with:
- **Oracle WebLogic Server**
- **Oracle Service Bus (OSB)**
- **Oracle SOA Suite (BPEL)**

Use this as a reference for detecting communication patterns in customer Java codebases.

---

## Project Structure

```
weblogic-soa-patterns-demo/
│
├── java-producers/                      # OUTGOING: Java → WebLogic/SOA/OSB
│   └── src/main/java/com/example/producer/
│       ├── soap/                        # SOAP-based producers
│       │   ├── JaxWsClientProducer.java           # @WebServiceClient (JAX-WS)
│       │   ├── WebServiceTemplateProducer.java    # Spring WebServiceTemplate
│       │   ├── AxisClientProducer.java            # Apache Axis client
│       │   ├── CxfClientProducer.java             # Apache CXF client
│       │   └── SaajClientProducer.java            # SAAJ low-level SOAP
│       ├── jms/                         # JMS-based producers
│       │   ├── JmsTemplateProducer.java           # Spring JmsTemplate
│       │   ├── WebLogicJndiProducer.java          # Direct WebLogic JNDI (t3://)
│       │   └── RawJmsProducer.java                # Raw JMS API
│       ├── kafka/                       # Kafka-based producers (if OSB uses Kafka)
│       │   └── KafkaTemplateProducer.java         # Spring KafkaTemplate
│       ├── rest/                        # REST-based producers
│       │   ├── RestTemplateProducer.java          # Spring RestTemplate
│       │   └── WebClientProducer.java             # Spring WebClient (reactive)
│       └── ejb/                         # EJB-based producers
│           └── EjbClientProducer.java             # EJB Remote call via JNDI
│
├── java-consumers/                      # INCOMING: WebLogic/SOA/OSB → Java
│   └── src/main/java/com/example/consumer/
│       ├── soap/                        # SOAP endpoints (called by BPEL/OSB)
│       │   ├── JaxWsEndpointConsumer.java         # @WebService (server-side)
│       │   └── SpringWsEndpointConsumer.java      # Spring WS endpoint
│       ├── jms/                         # JMS listeners
│       │   ├── JmsListenerConsumer.java           # @JmsListener
│       │   └── MessageListenerConsumer.java       # MessageListener interface
│       ├── kafka/                       # Kafka listeners
│       │   └── KafkaListenerConsumer.java         # @KafkaListener
│       ├── rest/                        # REST endpoints (callback from OSB)
│       │   └── RestCallbackConsumer.java          # @RestController
│       └── ejb/                         # EJB beans (called by SOA adapter)
│           └── EjbBeanConsumer.java               # @Stateless EJB
│
├── soa-composite/                       # Oracle SOA Suite artifacts
│   ├── composite.xml                    # SOA wiring configuration
│   ├── SOA/
│   │   ├── BPEL/
│   │   │   └── OrderProcess.bpel        # BPEL orchestration
│   │   ├── WSDLs/
│   │   │   ├── OrderService.wsdl        # Entry service WSDL
│   │   │   ├── PaymentService.wsdl      # External service WSDL
│   │   │   └── InventoryService.wsdl    # External service WSDL
│   │   └── Schemas/
│   │       └── Order.xsd                # XSD schemas
│   └── config/
│       └── weblogic-ra.xml              # WebLogic adapter config
│
├── osb-project/                         # Oracle Service Bus artifacts
│   ├── proxy/
│   │   ├── OrderHttpProxy.proxy         # HTTP/REST proxy
│   │   ├── OrderSoapProxy.proxy         # SOAP proxy
│   │   └── OrderJmsProxy.proxy          # JMS proxy
│   ├── pipelines/
│   │   └── OrderPipeline.pipeline       # Message processing pipeline
│   └── business/
│       └── OrderBizService.biz          # Business service
│
├── config/                              # Configuration files
│   ├── application.properties           # Spring Boot config
│   ├── application.yml                  # Alternative YAML config
│   └── weblogic-jndi.properties         # WebLogic JNDI settings
│
└── docs/
    └── PATTERN-DETECTION-GUIDE.md       # How to detect each pattern
```

---

## Communication Patterns Overview

### OUTGOING (Java → WebLogic/SOA/OSB)

| # | Method | File | Key Pattern | Confidence |
|---|--------|------|-------------|------------|
| 1 | JAX-WS SOAP | `JaxWsClientProducer.java` | `@WebServiceClient` | 100% |
| 2 | Spring WS SOAP | `WebServiceTemplateProducer.java` | `webServiceTemplate.marshalSendAndReceive()` | 95% |
| 3 | Axis SOAP | `AxisClientProducer.java` | `org.apache.axis.client.Call` | 90% |
| 4 | CXF SOAP | `CxfClientProducer.java` | `JaxWsProxyFactoryBean` | 90% |
| 5 | SAAJ SOAP | `SaajClientProducer.java` | `SOAPConnection.call()` | 90% |
| 6 | JMS Template | `JmsTemplateProducer.java` | `jmsTemplate.convertAndSend()` | 85% |
| 7 | WebLogic JNDI | `WebLogicJndiProducer.java` | `WLInitialContextFactory`, `t3://` | **100%** |
| 8 | Raw JMS | `RawJmsProducer.java` | `ConnectionFactory.createConnection()` | 80% |
| 9 | Kafka | `KafkaTemplateProducer.java` | `kafkaTemplate.send()` | 85% |
| 10 | REST Template | `RestTemplateProducer.java` | `restTemplate.postForObject()` | 70% |
| 11 | WebClient | `WebClientProducer.java` | `webClient.post()` | 70% |
| 12 | EJB Remote | `EjbClientProducer.java` | `@EJB`, `ctx.lookup()` | 90% |

### INCOMING (WebLogic/SOA/OSB → Java)

| # | Method | File | Key Pattern | Confidence |
|---|--------|------|-------------|------------|
| 1 | JAX-WS Endpoint | `JaxWsEndpointConsumer.java` | `@WebService` (server) | 95% |
| 2 | Spring WS Endpoint | `SpringWsEndpointConsumer.java` | `@Endpoint`, `@PayloadRoot` | 90% |
| 3 | JMS Listener | `JmsListenerConsumer.java` | `@JmsListener` | 95% |
| 4 | MessageListener | `MessageListenerConsumer.java` | `implements MessageListener` | 85% |
| 5 | Kafka Listener | `KafkaListenerConsumer.java` | `@KafkaListener` | 95% |
| 6 | REST Callback | `RestCallbackConsumer.java` | `@RestController` + callback URL | 70% |
| 7 | EJB Bean | `EjbBeanConsumer.java` | `@Stateless`, `@Remote` | 85% |

---

## Quick Reference: Detection Patterns

### 100% WebLogic-Specific (Guaranteed Detection)

```java
// Pattern 1: WebLogic JNDI Factory
env.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");

// Pattern 2: T3 Protocol URL
env.put(Context.PROVIDER_URL, "t3://weblogic-server:7001");

// Pattern 3: SOA-Infra URL path
String url = "http://server:7001/soa-infra/services/default/OrderComposite/OrderService";

// Pattern 4: OSB URL path
String url = "http://server:7001/osb/services/OrderProxy";
```

### High Confidence SOAP Patterns

```java
// JAX-WS Client
@WebServiceClient(name = "OrderService", wsdlLocation = "...")
OrderServicePortType port = service.getPort(OrderServicePortType.class);
port.processOrder(request);

// Spring WebServiceTemplate
webServiceTemplate.marshalSendAndReceive(endpoint, request);
```

### JMS/Kafka Patterns

```java
// JMS Producer
jmsTemplate.convertAndSend("OrderQueue", message);

// JMS Consumer
@JmsListener(destination = "OrderResponseQueue")
public void onMessage(Message msg) { ... }

// Kafka Producer
kafkaTemplate.send("orders.inbound", key, message);

// Kafka Consumer
@KafkaListener(topics = "orders.outbound")
public void consume(OrderMessage order) { ... }
```
