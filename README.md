# README

## Overview

This project provides a Java class (compatible with Java 17) designed to interact with the "Chestny ZNAK" API. The class ensures thread-safe operations and enforces a request rate limit, preventing the API from being overwhelmed with too many requests in a given time interval.

## Features

- **Thread-safe:** The class is designed to be safe for use in a multithreaded environment.
- **Request Limiting:** The constructor allows you to set a limit on the number of requests within a specified time interval. If the limit is reached, further requests are blocked until they can be safely executed.
- **Document Creation:** The class implements a method to create a document for introducing goods produced in Russia into circulation, which is then sent to the server via an HTTP POST request.

## Usage

### Constructor

```java
public CrptApi(TimeUnit timeUnit, int requestLimit)
```

- `timeUnit` — The time unit that defines the interval (e.g., second, minute) during which the request count is monitored.
- `requestLimit` — A positive integer that specifies the maximum number of requests allowed within the given time interval.

### Primary Method

```java
public void createDocument(Document document, String signature)
```

- `document` — A Java object representing the document that needs to be sent to the server.
- `signature` — A string containing the document's signature.

This method converts the provided document object into a JSON format and sends it to the server using an HTTP POST request. If the request limit is exceeded, the method will block until it is safe to proceed with the request.

### Shutdown

To gracefully shut down the task scheduler, you should call the `shutdown()` method:

```java
public void shutdown()
```

This method ensures that the scheduler is properly terminated, allowing the program to close cleanly.

## Example Usage

```java
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5); // Limit: 5 requests per second
        Document doc = new Document(/* parameters */);
        String signature = "your-signature";

        try {
            api.createDocument(doc, signature);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            api.shutdown();
        }
    }
}
```

## Additional Information

The implementation utilizes the following components:
- **HttpClient:** For sending HTTP requests.
- **Semaphore:** To control the number of concurrent requests.
- **ScheduledExecutorService:** To reset the request counter at defined time intervals.

## Conclusion

This project provides a convenient and flexible solution for interacting with the "Chestny ZNAK" API while ensuring compliance with request rate limits. The `CrptApi` class facilitates the safe integration of the API into multithreaded Java applications, ensuring all necessary constraints are adhered to and the application shuts down properly.

---

I am excited about the opportunity to work with your company and look forward to contributing to the creation of high-quality products together!
