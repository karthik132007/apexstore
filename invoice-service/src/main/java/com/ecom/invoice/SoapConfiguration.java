package com.ecom.invoice;
import com.ecom.common.*;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.*;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.*;
import java.util.*;
@EnableWs @Configuration
public class SoapConfiguration extends WsConfigurerAdapter {
    @Bean ServletRegistrationBean<MessageDispatcherServlet> soap(ApplicationContext context) { var servlet=new MessageDispatcherServlet(); servlet.setApplicationContext(context); servlet.setTransformWsdlLocations(true); return new ServletRegistrationBean<>(servlet,"/ws/*"); }
    @Bean XsdSchema invoiceSchema() { return new SimpleXsdSchema(new ClassPathResource("xsd/invoice.xsd")); }
    @Bean(name="invoices") DefaultWsdl11Definition wsdl(XsdSchema invoiceSchema) { var w=new DefaultWsdl11Definition(); w.setPortTypeName("InvoicePort"); w.setLocationUri("/ws"); w.setTargetNamespace(InvoiceXml.NS); w.setSchema(invoiceSchema); return w; }
    @Override public void addInterceptors(List<EndpointInterceptor> interceptors) { var v=new PayloadValidatingInterceptor(); v.setXsdSchema(invoiceSchema()); v.setValidateRequest(true); v.setValidateResponse(true); interceptors.add(v); }
    @Bean SoapFaultMappingExceptionResolver faults() {
        var resolver=new SoapFaultMappingExceptionResolver(); var fallback=new SoapFaultDefinition(); fallback.setFaultCode(SoapFaultDefinition.SERVER); fallback.setFaultStringOrReason("Invoice processing failed; retry with the same order ID"); resolver.setDefaultFault(fallback);
        var map=new Properties(); map.setProperty(ApiException.class.getName(),"CLIENT,Invalid invoice request"); map.setProperty(IllegalArgumentException.class.getName(),"CLIENT,Invalid invoice values"); resolver.setExceptionMappings(map); resolver.setOrder(0); return resolver;
    }
}
