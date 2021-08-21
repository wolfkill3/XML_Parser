import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ColorPropertiesEvaluator {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOURCE_FILE_PATH = "src/main/resources/source_file.xml";
    private static final String TARGET_FILE_PATH = "src/main/resources/target_file.xml";
    private static final String NODE_REGEX = ".//Neutral";
    private static final String COLOR_PROPERTY = "Actor.Color";
    private static final String NODE_BY_QUERY = "Origin";
    private static final boolean ADD_NEW_PROPERTY_FLAG = true; // if TRUE -> create Actor.Color property to Neutral node
    private static final Map<String, String> ATTRIBUTES = new HashMap<>();

    static {
        ATTRIBUTES.put("R", "128---------------------------------------------------------------------------");
        ATTRIBUTES.put("G", "128");
        ATTRIBUTES.put("B", "128");
    }

    public static void main(String[] args) {
        File file = new File(SOURCE_FILE_PATH);
        try {
            Document document = buildDocument(file);
            NodeList nodes = getDocumentNodes(document, NODE_REGEX);
            LOGGER.info("Start evaluate document nodes");
            evaluateDocumentNodes(nodes, document);
            String docStrings = getDocumentText(document);
            LOGGER.info("End of evaluation document nodes");
            writeTargetFile(docStrings);
            System.out.println(docStrings);
            LOGGER.info("\n===================================================================================================\n");
        } catch (SAXException | IOException | ParserConfigurationException | TransformerException | XPathExpressionException e) {
            LOGGER.error(e);
        }
    }

    private static void writeTargetFile(String docStrings) throws IOException {
        LOGGER.info("Start write to " + Path.of(TARGET_FILE_PATH).getFileName());
        OutputStream outputStream = new FileOutputStream(TARGET_FILE_PATH);
        outputStream.write(docStrings.getBytes(StandardCharsets.UTF_8));
        outputStream.close();
        LOGGER.info("Stop write");
    }

    private static Document buildDocument(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory.newDocumentBuilder().parse(file);
    }

    private static NodeList getDocumentNodes(
        final Document document,
        final String regex) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        XPathExpression xPathExpression = xPath.compile(regex);
        return (NodeList) xPathExpression.evaluate(document, XPathConstants.NODESET);
    }

    private static String getDocumentText(Document document) throws TransformerException {
        StringWriter writer = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return (writer.toString());
    }

    private static void evaluateDocumentNodes(NodeList nodeList, Document document) throws TransformerException {
        IntStream.range(0, nodeList.getLength()).mapToObj(nodeList::item).forEach(neutralNode -> {
            NodeList neutralProperties = neutralNode.getChildNodes();
            getColorPropertyNode(document, neutralNode, neutralProperties);
        });
    }

    private static void getColorPropertyNode(final Document document, final Node neutralNode, final NodeList nodes) {
        Node colorProperty = null;
        boolean hasOriginNode = false;

        for (int j = 0; j < nodes.getLength(); j++) {
            Node node = nodes.item(j);
            if (COLOR_PROPERTY.equals(node.getNodeName())) {
                colorProperty = getNodeByQuery(node, COLOR_PROPERTY);
            }

            if (!hasOriginNode && hasNodeByNameRecursive(node, NODE_BY_QUERY) != null) {
                hasOriginNode = true;
            }
        }

        if (hasOriginNode) {
            populateRGBProperties(document, neutralNode, (Element) colorProperty);
        }
    }

    private static void populateRGBProperties(final Document document, final Node neutralNode, final Element element) {
        if (element != null) {
            ATTRIBUTES.forEach(element::setAttribute);
            LOGGER.info("Evaluated color properties");
        } else if (ADD_NEW_PROPERTY_FLAG) {
            Element colorProperty = document.createElement(COLOR_PROPERTY);
            ATTRIBUTES.forEach(colorProperty::setAttribute);
            neutralNode.appendChild(colorProperty);
            LOGGER.info("Added color properties to node");
        }
    }

    private static Node getNodeByQuery(final Node childNode, String nodeName) {
        Node soughtNode = childNode;
        NodeList childNodeList = soughtNode.getChildNodes();

        while (childNodeList.getLength() > 0) {
            try {
                childNodeList = soughtNode.getChildNodes();

                List<Node> listOfChildNodes = IntStream.range(0, childNodeList.getLength())
                                                       .mapToObj(childNodeList::item)
                                                       .collect(Collectors.toList());

                if (isThisSoughtNode(soughtNode, nodeName, listOfChildNodes)) {
                    return soughtNode;
                }
                Node lastNode = soughtNode;
                soughtNode = listOfChildNodes.stream()
                                             .filter(n -> n.getNodeName().equals(nodeName))
                                             .findFirst()
                                             .orElse(soughtNode);
                if (lastNode == soughtNode) {
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error(e);
                return null;
            }
        }

        if (nodeName.equals(soughtNode.getNodeName())) {
            return soughtNode;
        } else {
            return null;
        }
    }

    private static Node hasNodeByNameRecursive(Node node, String nodeName) {
        NodeList childNodes = node.getChildNodes();

        if (childNodes.getLength() == 0) {
            return null;
        }

        List<Node> listOfChildNodes = IntStream.range(0, childNodes.getLength())
                                               .mapToObj(childNodes::item)
                                               .collect(Collectors.toList());

        for (Node childNode : listOfChildNodes) {
            if (childNode.getNodeName().equals(nodeName)) {
                return childNode;
            }
        }

        for (Node childNode : listOfChildNodes) {
            Node recursiveNode = hasNodeByNameRecursive(childNode, nodeName);
            if (recursiveNode != null && recursiveNode.getNodeName().equals(nodeName)) {
                return recursiveNode;
            }
        }

        return null;
    }

    private static boolean isThisSoughtNode(final Node soughtNode, final String tag, final List<Node> listOfChildNodes) {
        return listOfChildNodes.isEmpty() && tag.equals(soughtNode.getNodeName());
    }
}