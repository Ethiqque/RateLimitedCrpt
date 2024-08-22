package crpt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import selsup.exception.DocumentCreationException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrptApi {

    @Value("${crpt.api.url}")
    private String apiUrl;

    @Value("${crpt.api.requestLimit}")
    private final int requestLimit;

    @Value("${crpt.api.timeIntervalMillis}")
    private final long timeIntervalMillis;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper;
    private final Semaphore semaphore = new Semaphore(requestLimit, true);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Document {
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class Product {
        private String certificateDocument;
        private String certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }

    public CrptApi(TimeUnit timeUnit, long timeInterval, int requestLimit) {
        this.timeIntervalMillis = timeUnit.toMillis(timeInterval);
        this.scheduler.scheduleAtFixedRate(() ->
                semaphore.release(
                        requestLimit - semaphore.availablePermits()),
                timeIntervalMillis, timeIntervalMillis, TimeUnit.MILLISECONDS);
    }

    public HttpResponse<String> createDocument(Document document, String signature) throws JsonProcessingException, DocumentCreationException {
        acquireSemaphore();
        try {
            String requestBody = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            log.info("Sending request to: {}", apiUrl);
            log.debug("Request body: {}", requestBody);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Received response with status code: {}", response.statusCode());

            return response;
        } catch (IOException e) {
            log.error("IOException occurred while creating the document", e);
            throw new DocumentCreationException("Failed to create document due to I/O error: " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Request interrupted", e);
            throw new DocumentCreationException("Request interrupted while creating document: " + e);
        } finally {
            releaseSemaphore();
        }
    }

    private void acquireSemaphore() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DocumentCreationException("Semaphore acquisition interrupted: " + e);
        }
    }

    private void releaseSemaphore() {
        semaphore.release();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Scheduler did not terminate");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws InterruptedException, JsonProcessingException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1, 10);

        Document document = api.new Document(
                "doc123",
                "NEW",
                "LP_INTRODUCE_GOODS",
                true,
                "1234567890",
                "0987654321",
                "1122334455",
                "2024-08-10",
                "PRODUCTION_TYPE",
                new Product[]{
                        api.new Product(
                                "cert123",
                                "2024-08-10",
                                "certnum123",
                                "1234567890",
                                "1122334455",
                                "2024-08-10",
                                "010101",
                                "uit123",
                                "uitu123"
                        )
                },
                "2024-08-10",
                "reg123"
        );

        String signature = "signature123";

        HttpResponse<String> response = api.createDocument(document, signature);
        System.out.println("Response: " + response.body());

        api.shutdown();
    }
}

// Tests

//package selsup;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mockito;
//import org.springframework.test.util.ReflectionTestUtils;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class CrptApiTest {
//
//    private CrptApi crptApi;
//    private HttpClient mockHttpClient;
//    private ObjectMapper mockObjectMapper;
//
//    @BeforeEach
//    void setUp() {
//        mockHttpClient = mock(HttpClient.class);
//        mockObjectMapper = mock(ObjectMapper.class);
//
//        crptApi = new CrptApi(TimeUnit.SECONDS, 1, 10);
//        ReflectionTestUtils.setField(crptApi, "httpClient", mockHttpClient);
//        ReflectionTestUtils.setField(crptApi, "objectMapper", mockObjectMapper);
//        ReflectionTestUtils.setField(crptApi, "apiUrl", "http://example.com/api");
//    }
//
//    @Test
//    void createDocumentTest() throws Exception {
//        CrptApi.Document document = new CrptApi.Document();
//        String signature = "signature123";
//        String jsonString = "{}";
//
//        when(mockObjectMapper.writeValueAsString(document)).thenReturn(jsonString);
//
//        HttpResponse<String> mockResponse = mock(HttpResponse.class);
//        when(mockResponse.statusCode()).thenReturn(200);
//        when(mockResponse.body()).thenReturn("Success");
//
//        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenReturn(mockResponse);
//
//        HttpResponse<String> response = crptApi.createDocument(document, signature);
//
//        assertNotNull(response);
//        assertEquals(200, response.statusCode());
//        assertEquals("Success", response.body());
//
//        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
//        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
//        HttpRequest capturedRequest = requestCaptor.getValue();
//
//        assertEquals("http://example.com/api", capturedRequest.uri().toString());
//        assertEquals("application/json", capturedRequest.headers().firstValue("Content-Type").orElse(""));
//        assertEquals(signature, capturedRequest.headers().firstValue("Signature").orElse(""));
//        assertEquals("POST", capturedRequest.method());
//    }
//
//    @Test
//    void createDocumentIOExceptionTest() throws Exception {
//        CrptApi.Document document = new CrptApi.Document();
//        String signature = "signature123";
//        String jsonString = "{}";
//
//        when(mockObjectMapper.writeValueAsString(document)).thenReturn(jsonString);
//        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenThrow(IOException.class);
//
//        assertThrows(DocumentCreationException.class, () -> {
//            crptApi.createDocument(document, signature);
//        });
//
//        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    void createDocumentShouldThrowInterruptedExceptionTest() throws Exception {
//        CrptApi.Document document = new CrptApi.Document();
//        String signature = "signature123";
//        String jsonString = "{}";
//
//        when(mockObjectMapper.writeValueAsString(document)).thenReturn(jsonString);
//        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
//                .thenThrow(InterruptedException.class);
//
//        assertThrows(DocumentCreationException.class, () -> {
//            crptApi.createDocument(document, signature);
//        });
//
//        verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
//    }
//
//    @Test
//    void acquireSemaphoreTest() {
//        for (int i = 0; i < 10; i++) {
//            crptApi.createDocument(new CrptApi.Document(), "signature123");
//        }
//        assertThrows(DocumentCreationException.class, () -> {
//            crptApi.createDocument(new CrptApi.Document(), "signature123");
//        });
//    }
//}
