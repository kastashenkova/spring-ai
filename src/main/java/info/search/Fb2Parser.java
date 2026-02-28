package info.search;

import info.search.model.Doc;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Fb2Parser {

    public static Doc parse(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        // security
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",
                true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",
                false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",
                false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document xml = builder.parse(inputStream);
        xml.getDocumentElement().normalize();

        Element titleInfo = findElement(xml);
        String author = extractAuthors(titleInfo);
        String title = textOf(titleInfo, "book-title");
        String bodyText = extractBody(xml);

        Doc doc = new Doc();
        doc.setTitle(title);
        doc.setAuthor(author);
        doc.setContent(bodyText);
        return doc;
    }

    private static Element findElement(Document xml) {
        NodeList list = xml.getElementsByTagNameNS("*",
                "title-info");
        if (list.getLength() == 0) {
            list = xml.getElementsByTagName("title-info");
        }
        return list.getLength() > 0 ? (Element) list.item(0) : null;
    }

    private static String textOf(Element element, String tag) {
        if (element == null) {
            return "";
        }
        NodeList list = element.getElementsByTagNameNS("*", tag);
        if (list.getLength() == 0) {
            list = element.getElementsByTagName(tag);
        }
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return "";
    }

    private static String extractAuthors(Element titleInfo) {
        if (titleInfo == null) {
            return "";
        }

        // * — ignore namespace
        NodeList authors = titleInfo
                .getElementsByTagNameNS("*", "author");

        if (authors.getLength() == 0) {
            authors = titleInfo.getElementsByTagName("author");
        }

        List<String> names = new ArrayList<>();
        for (int i = 0; i < authors.getLength(); i++) {
            Node author = authors.item(i);
            if (author.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) author;
            String first = textOf(element, "first-name");
            String middle = textOf(element, "middle-name");
            String last = textOf(element, "last-name");
            String full = (first + " " + middle + " " + last).trim()
                    .replaceAll("\\s+", " ");
            if (!full.isBlank()) {
                names.add(full);
            }
        }
        return String.join(", ", names);
    }

    private static String extractBody(org.w3c.dom.Document xml) {
        NodeList bodies = xml.getElementsByTagNameNS("*", "body");
        if (bodies.getLength() == 0) {
            bodies = xml.getElementsByTagName("body");
        }
        if (bodies.getLength() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bodies.getLength(); i++) {
            collect(bodies.item(i), sb);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    // recursive
    private static void collect(Node node, StringBuilder sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getTextContent().trim();
            if (!text.isEmpty()) sb.append(text).append(" ");
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collect(children.item(i), sb);
        }
    }
}