package uk.gov.moj.cpp.staging.command.api.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.sql.Date.valueOf;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_TIME;
import static java.util.TimeZone.getTimeZone;

public class TimeOfHearingConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeOfHearingConverter.class);

    private static final String BST_SUFFIX = "+01:00";

    public enum PoliceForceCodeWithBSTTimeOfHearing {
        HERTFORDSHIRE("41"),
        KENT("46"),
        ESSEX("42"),
        SUFFOLK("37"),
        NORFOLK("36"),
        BEDFORDSHIRE("40"),
        WARWICKSHIRE("23"),
        CAMBRIDGESHIRE("35"),
        NORTHUMBRIA("10"),
        WEST_MERCIA("22"),
        WEST_MIDLANDS("20"),
        SOUTH_YORKSHIRE("14"),
        LANCASHIRE("04"),
        CUMBRIA("03"),
        DURHAM("11"),
        GREATER_MANCHESTER("06"),
        DYFED_POWYS("63"),
        LONDON("01");

        private static Map<String, PoliceForceCodeWithBSTTimeOfHearing> allCodesMap = Arrays.stream(PoliceForceCodeWithBSTTimeOfHearing.values())
                .collect(Collectors.toMap(PoliceForceCodeWithBSTTimeOfHearing::getCode, Function.identity()));

        private String code;

        PoliceForceCodeWithBSTTimeOfHearing(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static boolean contains(String code) {
            return allCodesMap.get(code) != null;
        }
    }


    public static String updateBSTTimeOfHearing(String messageContent) {
        try {
            final Document parentDocument = getDocument(messageContent);
            final NodeList dataStreamContent = parentDocument.getElementsByTagName("cjseEntity:DataStreamContent");
            final Node dataStreamNode = dataStreamContent.item(0);

            if (dataStreamNode == null) {
                return messageContent;
            }
            final String caseDataInXml = dataStreamNode.getTextContent();

            final Document caseDocument = getDocument(caseDataInXml);
            final String namespaceURI = getNamespace(caseDocument);

            final String ptiURN = caseDocument.getElementsByTagName(namespaceURI + "PTIURN").item(0).getTextContent();

            final String dateOfHearing = caseDocument.getElementsByTagName(namespaceURI + "DateOfHearing").item(0).getTextContent();

            final Node timeOfHearingNode = caseDocument.getElementsByTagName(namespaceURI + "TimeOfHearing").item(0);

            String timeOfHearing = timeOfHearingNode.getTextContent();


            final String UNKNOWN_TIME_OF_HEARING = "HH-MM-SS";
            final String EUROPE_LONDON_ZONE_ID = "Europe/London";

            final LocalDate dateOfHearingObj = LocalDate.parse(dateOfHearing, ISO_LOCAL_DATE);

            LOGGER.debug("Before:PTIURN:{}, TimeOfHearing:{} , dateOfHearing:{}", ptiURN, timeOfHearing, dateOfHearingObj);

            if (timeOfHearing == null) {
                timeOfHearing = UNKNOWN_TIME_OF_HEARING;
            } else {

                if (isInRequiredPoliceForce(ptiURN) &&
                        getTimeZone(EUROPE_LONDON_ZONE_ID).inDaylightTime(valueOf(dateOfHearingObj))) {
                    timeOfHearing = LocalTime.parse(timeOfHearing, ISO_TIME).format(ISO_TIME) + BST_SUFFIX;
                }
            }
            LOGGER.debug("After:TimeOfHearing:{}", timeOfHearing);

            timeOfHearingNode.setTextContent(timeOfHearing);
            dataStreamNode.setTextContent(getStringFromDocument(caseDocument));

            return getStringFromDocument(parentDocument);
        } catch (TransformerException | ParserConfigurationException | IOException | SAXException e) {
            LOGGER.error("Exception while updateBSTTimeOfHearing, unable to perform updateBSTTimeOfHearing", e);
            return messageContent;
        }
    }

    private static String getNamespace(Document document) {
        return document.getDocumentElement().getPrefix() != null ? document.getDocumentElement().getPrefix() + ":" : "";
    }

    @SuppressWarnings("squid:S2755")
    private static Document getDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        return documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    @SuppressWarnings("squid:S4435") // false positive
    private static String getStringFromDocument(Document doc) throws TransformerException {
        final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), result);
        return writer.toString();
    }

    private static boolean isInRequiredPoliceForce(String ptiURN) {
        return PoliceForceCodeWithBSTTimeOfHearing.contains(ptiURN.substring(0, 2));
    }
}