# CrptApi Implementation for Chestny ZNAK API

## Overview

This task involves creating a thread-safe Java class that interacts with the **Chestny ZNAK** (Honest Sign) API. The class will enforce a rate limit to ensure that the number of API requests within a given time interval does not exceed a specified limit. The main method will allow the creation of documents for introducing goods produced in Russia into circulation.

The implementation ensures:
- **Thread-safety:** Safe execution in a multi-threaded environment.
- **Rate-limiting:** Enforcing a maximum number of API requests in a specified time period without throwing exceptions for exceeding the limit.

---

## Features

- **Thread-safe execution** using concurrency utilities.
- **Configurable rate limits** that block further requests when exceeded, instead of throwing errors.
- **Document creation method** that sends a document and its signature to the API as a JSON payload.
- **Extensible design** allowing for future enhancements and additional API methods.

---

## Constructor

```java
public CrptApi(TimeUnit timeUnit, int requestLimit)
```

### Parameters:
- `timeUnit`: Specifies the time interval (e.g., seconds, minutes) over which the rate limit is monitored.
- `requestLimit`: The maximum number of API requests allowed within the defined time interval.

---

## Method

```java
public void createDocument(Document document, String signature)
```

### Parameters:
- `document`: A Java object representing the document to be sent to the API.
- `signature`: A string containing the digital signature for the document.

This method converts the document object to a JSON format and makes an HTTP POST request to the Chestny ZNAK API. If the rate limit is exceeded, it blocks until the request can be safely sent.

---

## Usage Example

```java
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // Create an API instance with a limit of 5 requests per second
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5); 
        
        // Create a document object (replace with actual document data)
        Document doc = new Document(/* parameters */);
        String signature = "your-signature";

        try {
            // Send the document to the API
            api.createDocument(doc, signature);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Cleanly shutdown the API client
            api.shutdown();
        }
    }
}
```

---

## Shutdown Method

```java
public void shutdown()
```

This method properly terminates the internal scheduler responsible for resetting the rate limit, preventing any potential resource leaks.

---

## Implementation Details

- **HttpClient**: Used to send HTTP requests to the Chestny ZNAK API.
- **Semaphore**: Implements the rate-limiting functionality by controlling the number of concurrent requests within the specified time frame.
- **ScheduledExecutorService**: Resets the request counter at the start of each new time interval, ensuring compliance with the rate limit.

---

## Example JSON Payload

The request body format for the `POST` request to the API is:

```json
{
  "description": { 
    "participantInn": "string" 
  },
  "doc_id": "string",
  "doc_status": "string",
  "doc_type": "LP_INTRODUCE_GOODS",
  "importRequest": true,
  "owner_inn": "string",
  "participant_inn": "string",
  "producer_inn": "string",
  "production_date": "2020-01-23",
  "production_type": "string",
  "products": [ 
    { 
      "certificate_document": "string",
      "certificate_document_date": "2020-01-23",
      "certificate_document_number": "string",
      "owner_inn": "string",
      "producer_inn": "string",
      "production_date": "2020-01-23",
      "tnved_code": "string",
      "uit_code": "string",
      "uitu_code": "string" 
    }
  ],
  "reg_date": "2020-01-23",
  "reg_number": "string"
}
```

---

## Conclusion

This Java class provides a scalable, thread-safe solution for interacting with the **Chestny ZNAK** API, ensuring compliance with rate limits and providing a flexible foundation for future API interactions. The design is optimized for multi-threaded environments and future extensibility.
