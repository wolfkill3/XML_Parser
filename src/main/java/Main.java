import java.io.*;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);
    private static final String FILE_PATH = "src/main/resources/source_file.xml";
    private static final String NODE_REGEX = ".//Neutral";
    private static final String PROPERTY_NAME = "Actor.Color";
    private static final boolean ADD_NEW_PROPERTY_FLAG = true; // if TRUE -> create Actor.Color property to Neutral node
    private static final Map<String, String> ATTRIBUTES = new HashMap<>();

    static {
        ATTRIBUTES.put("R", "128");
        ATTRIBUTES.put("G", "128");
        ATTRIBUTES.put("B", "128");
    }

    public static void main(String[] args) {
        File file = new File(FILE_PATH);
        try {
            Document document = buildDocument(file);
            NodeList nodes = getNodeList(document, NODE_REGEX);
            evaluateNodes(nodes, document);
            System.out.println(nodeToString(document));
        } catch (SAXException | IOException | ParserConfigurationException | TransformerException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    private static Document buildDocument(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static NodeList getNodeList(
        final Document document,
        final String regex) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression = xPath.compile(regex);
        return (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
    }

    private static String nodeToString(Document document) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return (writer.toString());
    }

    private static void evaluateNodes(NodeList nodeList, Document document) throws TransformerException {
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node neutralNode = nodeList.item(i);
            NodeList neutralProperties = neutralNode.getChildNodes();
            Element element = (Element) getColorPropertyNode(neutralProperties);
            if (element != null) {
                ATTRIBUTES.forEach(element::setAttribute);
            } else if (ADD_NEW_PROPERTY_FLAG) {
                Element colorProperty = document.createElement(PROPERTY_NAME);
                ATTRIBUTES.forEach(colorProperty::setAttribute);
                neutralNode.appendChild(colorProperty);
            }
        }
    }

    private static Node getColorPropertyNode(final NodeList nodes) {
        Node colorProperty = null;

        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (PROPERTY_NAME.equals(node.getNodeName())) {
                colorProperty = node;
            }
        }
        return colorProperty;
    }
}