package com.ecom.order;
import com.ecom.common.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
@Component
public class InvoiceClient {
    private final RestClient client;
    public InvoiceClient(RestClient.Builder builder,@Value("${app.invoice-url}") String url) { client=builder.clone().baseUrl(url).build(); }
    public InvoiceContract.Invoice generate(InvoiceContract.Request request) {
        String response=client.post().uri("/ws").contentType(MediaType.TEXT_XML).header("SOAPAction","\"\"").body(InvoiceXml.request(request)).retrieve().body(String.class);
        return InvoiceXml.readResponse(response);
    }
}
