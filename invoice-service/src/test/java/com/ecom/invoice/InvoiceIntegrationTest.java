package com.ecom.invoice;
import com.ecom.common.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("test")
class InvoiceIntegrationTest {
    @Autowired TestRestTemplate http;
    InvoiceContract.Request request() { return new InvoiceContract.Request(UUID.randomUUID(),"Buyer","buyer@example.test","Test address","INR",List.of(new InvoiceContract.Item(UUID.randomUUID(),UUID.randomUUID(),"Keyboard & Mouse",2,new BigDecimal("500.00")))); }
    HttpHeaders headers() { HttpHeaders h=new HttpHeaders(); h.setContentType(MediaType.TEXT_XML); h.set("X-Service-Key","test-only-internal-key-at-least-thirty-two-characters"); return h; }
    @Test void actualSoapEndpointGeneratesStableInvoice() {
        String xml=InvoiceXml.request(request());
        var response=http.postForEntity("/ws",new HttpEntity<>(xml,headers()),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var first=InvoiceXml.readResponse(response.getBody()); assertThat(first.total()).isEqualByComparingTo("1000.00");
        var second=InvoiceXml.readResponse(http.postForEntity("/ws",new HttpEntity<>(xml,headers()),String.class).getBody());
        assertThat(second.invoiceNumber()).isEqualTo(first.invoiceNumber());
        assertThat(first.items().getFirst().name()).isEqualTo("Keyboard & Mouse");
    }
    @Test void wsdlIsPublishedAndSoapRequiresInternalAuthentication() {
        assertThat(http.exchange("/ws/invoices.wsdl",HttpMethod.GET,new HttpEntity<>(headers()),String.class).getBody()).contains("generateInvoice");
        assertThat(http.postForEntity("/ws",InvoiceXml.request(request()),String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
    @Test void invalidSoapReturnsFault() {
        var response=http.postForEntity("/ws",new HttpEntity<>("<s:Envelope xmlns:s='http://schemas.xmlsoap.org/soap/envelope/'><s:Body><generateInvoiceRequest xmlns='urn:ecom:invoice:v1'/></s:Body></s:Envelope>",headers()),String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR); assertThat(response.getBody()).contains("Fault");
    }
}
