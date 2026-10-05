package com.ecom.invoice;
import com.ecom.common.InvoiceXml;
import org.springframework.ws.server.endpoint.annotation.*;
import org.w3c.dom.Element;
@Endpoint
public class InvoiceEndpoint {
    private final InvoiceService service;
    public InvoiceEndpoint(InvoiceService service) { this.service=service; }
    @PayloadRoot(namespace=InvoiceXml.NS,localPart="generateInvoiceRequest") @ResponsePayload
    public Element generate(@RequestPayload Element request) { return InvoiceXml.response(service.generate(InvoiceXml.readRequest(request))); }
}
