package uk.gov.moj.cpp.staging.command.api.converter;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TimeOfHearingConverterTest {

    private static final String WINTER_INPUT_SPI_MESSAGE_FILE = "mockResources/SingleDefendantSingeOffenceWinterTiming.xml";
    private static final String DAYLIGHT_SAVING_INPUT_SPI_MESSAGE_FILE = "mockResources/SingleDefendantSingeOffenceDaylightSaving.xml";
    private static final String DAYLIGHT_SAVING_INPUT_SPI_MESSAGE_FILE_LONDON = "mockResources/SingleDefendantSingeOffenceDaylightSavingLondon.xml";

    @Test
    public void shouldNotUpdateBSTTimeOfHearing() throws Exception {

        String message = readFileToString(new File(this.getClass().getClassLoader().getResource(WINTER_INPUT_SPI_MESSAGE_FILE).getFile()));
        Document convertedMessage = getDocument(TimeOfHearingConverter.updateBSTTimeOfHearing(message));

        NodeList dataStreamContent = convertedMessage.getElementsByTagName("cjseEntity:DataStreamContent");
        Node dataStreamNode = dataStreamContent.item(0);
        String caseDataInXml = dataStreamNode.getTextContent();

        Document caseDocument = getDocument(caseDataInXml);
        Node timeOfHearingNode = caseDocument.getElementsByTagName(getNamespace(caseDocument) + "TimeOfHearing").item(0);
        String timeOfHearing = timeOfHearingNode.getTextContent();

        assertThat(timeOfHearing, equalTo("10:29:17"));
    }

    @Test
    public void shouldUpdateBSTTimeOfHearing() throws Exception {

        String message = readFileToString(new File(this.getClass().getClassLoader().getResource(DAYLIGHT_SAVING_INPUT_SPI_MESSAGE_FILE).getFile()));
        Document convertedMessage = getDocument(TimeOfHearingConverter.updateBSTTimeOfHearing(message));

        NodeList dataStreamContent = convertedMessage.getElementsByTagName("cjseEntity:DataStreamContent");
        Node dataStreamNode = dataStreamContent.item(0);
        String caseDataInXml = dataStreamNode.getTextContent();

        Document caseDocument = getDocument(caseDataInXml);
        Node timeOfHearingNode = caseDocument.getElementsByTagName(getNamespace(caseDocument) + "TimeOfHearing").item(0);
        String timeOfHearing = timeOfHearingNode.getTextContent();

        assertThat(timeOfHearing, equalTo("09:00:00+01:00"));
    }

    @Test
    public void shouldUpdateBSTTimeOfHearingForLondonPoliceForce() throws Exception {

        String message = readFileToString(new File(this.getClass().getClassLoader().getResource(DAYLIGHT_SAVING_INPUT_SPI_MESSAGE_FILE_LONDON).getFile()));
        Document convertedMessage = getDocument(TimeOfHearingConverter.updateBSTTimeOfHearing(message));

        NodeList dataStreamContent = convertedMessage.getElementsByTagName("cjseEntity:DataStreamContent");
        Node dataStreamNode = dataStreamContent.item(0);
        String caseDataInXml = dataStreamNode.getTextContent();

        Document caseDocument = getDocument(caseDataInXml);
        Node timeOfHearingNode = caseDocument.getElementsByTagName(getNamespace(caseDocument) + "TimeOfHearing").item(0);
        String timeOfHearing = timeOfHearingNode.getTextContent();

        assertThat(timeOfHearing, equalTo("10:00:00+01:00"));
    }

    private static String getNamespace(Document document) {
        return document.getDocumentElement().getPrefix() != null ? document.getDocumentElement().getPrefix() + ":" : "";
    }

    private static Document getDocument(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return db.parse(is);
    }

}