package com.ecom.common;
import org.w3c.dom.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.InputSource;
import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Small explicit SOAP contract shared by the client and server; no database models are shared. */
public final class InvoiceXml {
    public static final String NS="urn:ecom:invoice:v1";
    public static final String SOAP="http://schemas.xmlsoap.org/soap/envelope/";
    private InvoiceXml() {}
    public static Document parse(String xml) {
        try {
            var f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            f.setFeature("http://xml.org/sax/features/external-general-entities",false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch(Exception e) { throw ApiException.bad("Invalid invoice XML"); }
    }
    private static Document document() { try { var f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true); return f.newDocumentBuilder().newDocument(); } catch(Exception e) { throw new IllegalStateException(e); } }
    private static Element add(Element parent,String name,Object value) { Element e=parent.getOwnerDocument().createElementNS(NS,"inv:"+name); if(value!=null) e.setTextContent(value.toString()); parent.appendChild(e); return e; }
    public static String text(Element e,String name) {
        for(Node n=e.getFirstChild();n!=null;n=n.getNextSibling()) if(n instanceof Element child && NS.equals(child.getNamespaceURI()) && name.equals(child.getLocalName())) return child.getTextContent();
        throw ApiException.bad("Missing invoice field: "+name);
    }
    private static List<InvoiceContract.Item> items(Element root) {
        var list=new ArrayList<InvoiceContract.Item>();
        NodeList nodes=root.getElementsByTagNameNS(NS,"item");
        for(int i=0;i<nodes.getLength();i++) { Element e=(Element)nodes.item(i); list.add(new InvoiceContract.Item(UUID.fromString(text(e,"productId")),UUID.fromString(text(e,"sellerId")),text(e,"name"),Integer.parseInt(text(e,"quantity")),new BigDecimal(text(e,"unitPrice")))); }
        return list;
    }
    private static void items(Element root,List<InvoiceContract.Item> items) {
        Element list=add(root,"items",null);
        for(var item:items) { Element e=add(list,"item",null); add(e,"productId",item.productId()); add(e,"sellerId",item.sellerId()); add(e,"name",item.name()); add(e,"quantity",item.quantity()); add(e,"unitPrice",item.unitPrice()); }
    }
    public static InvoiceContract.Request readRequest(Element root) { return new InvoiceContract.Request(UUID.fromString(text(root,"orderId")),text(root,"customerName"),text(root,"customerEmail"),text(root,"address"),text(root,"currency"),items(root)); }
    public static String request(InvoiceContract.Request r) {
        Document d=document(); Element envelope=d.createElementNS(SOAP,"soap:Envelope"); d.appendChild(envelope); Element body=d.createElementNS(SOAP,"soap:Body"); envelope.appendChild(body); Element root=add(body,"generateInvoiceRequest",null);
        add(root,"orderId",r.orderId()); add(root,"customerName",r.customerName()); add(root,"customerEmail",r.customerEmail()); add(root,"address",r.address()); add(root,"currency",r.currency()); items(root,r.items()); return serialize(envelope);
    }
    public static Element response(InvoiceContract.Invoice i) {
        Document d=document(); Element root=d.createElementNS(NS,"inv:generateInvoiceResponse"); d.appendChild(root);
        add(root,"id",i.id()); add(root,"invoiceNumber",i.invoiceNumber()); add(root,"orderId",i.orderId()); add(root,"customerName",i.customerName()); add(root,"customerEmail",i.customerEmail()); add(root,"address",i.address()); add(root,"currency",i.currency()); add(root,"total",i.total()); add(root,"issuedAt",i.issuedAt()); items(root,i.items()); return root;
    }
    public static InvoiceContract.Invoice readResponse(String xml) {
        Document d=parse(xml); NodeList nodes=d.getElementsByTagNameNS(NS,"generateInvoiceResponse"); if(nodes.getLength()!=1) throw ApiException.bad("Invalid invoice response"); Element root=(Element)nodes.item(0);
        return new InvoiceContract.Invoice(UUID.fromString(text(root,"id")),text(root,"invoiceNumber"),UUID.fromString(text(root,"orderId")),text(root,"customerName"),text(root,"customerEmail"),text(root,"address"),text(root,"currency"),new BigDecimal(text(root,"total")),Instant.parse(text(root,"issuedAt")),items(root));
    }
    public static String serialize(Node node) { try { var f=TransformerFactory.newInstance(); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET,""); var t=f.newTransformer(); var out=new StringWriter(); t.transform(new DOMSource(node),new StreamResult(out)); return out.toString(); } catch(Exception e) { throw new IllegalStateException(e); } }
}
