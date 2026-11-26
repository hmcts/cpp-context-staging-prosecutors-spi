package uk.gov.moj.cpp.staging.soap.schema;

import static java.math.BigInteger.valueOf;
import static javax.xml.bind.JAXBContext.newInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.dca.xmlschemas.libra.BaseHearingStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDefendantStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.dca.xmlschemas.libra.CourtCaseStructure;
import uk.gov.dca.xmlschemas.libra.CourtDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtHearingStructure;
import uk.gov.dca.xmlschemas.libra.CourtIndividualDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtSessionStructure;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceResultedCaseStructure;

import java.io.StringWriter;
import java.time.LocalDateTime;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.jupiter.api.Test;

public class ObjectFactoryMarshallerTest {

    private static final String RESULTED_CASES_FOR_THE_POLICE_DEFAULT_VALUE = "ResultedCasesForThePolice";
    private static final String LIBRA_STANDARD_PROSECUTOR_POLICE_DEFAULT_VALUE = "LibraStandardProsecutorPolice";
    private static final String SCHEMA_VERSION_DEFAULT_VALUE = "0.6g";


    @Test
    public void getStdProsPoliceResultedCaseStructureTest() throws Exception {
        final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure = new StdProsPoliceResultedCaseStructure();
        stdProsPoliceResultedCaseStructure.setSession(buildCourtSession());
        stdProsPoliceResultedCaseStructure.setInterface(LIBRA_STANDARD_PROSECUTOR_POLICE_DEFAULT_VALUE);
        stdProsPoliceResultedCaseStructure.setFlow(RESULTED_CASES_FOR_THE_POLICE_DEFAULT_VALUE);
        stdProsPoliceResultedCaseStructure.setSchemaVersion(SCHEMA_VERSION_DEFAULT_VALUE);

        final String result = getStdProsPoliceResultedCaseStructure(stdProsPoliceResultedCaseStructure);
        assertNotNull(result);
     }

    private String getStdProsPoliceResultedCaseStructure(final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure) throws JAXBException {
        final JAXBElement<StdProsPoliceResultedCaseStructure> routeDataRequestTypeJAXBElement = new uk.gov.dca.xmlschemas.libra.ObjectFactory().createResultedCaseMessage(stdProsPoliceResultedCaseStructure);

        final StringWriter stringWriter = new StringWriter();
        newInstance(StdProsPoliceResultedCaseStructure.class).createMarshaller().marshal(routeDataRequestTypeJAXBElement, stringWriter);
        return stringWriter.toString();
    }

    private CourtSessionStructure buildCourtSession() {
        final CourtSessionStructure courtSessionStructure = new CourtSessionStructure();
        courtSessionStructure.setCourtHearing(buildCourtHearing());

        courtSessionStructure.getCase().add(buildCourtCaseStructure());
        return courtSessionStructure;
    }

    private CourtCaseStructure buildCourtCaseStructure() {
        CourtCaseStructure courtCaseStructure = new CourtCaseStructure();
        courtCaseStructure.setDefendant(buildCourtDefendantStructure());
        return courtCaseStructure;
    }

    private CourtDefendantStructure buildCourtDefendantStructure() {
        CourtDefendantStructure courtDefendantStructure = new CourtDefendantStructure();
        courtDefendantStructure.setProsecutorReference("sdasd");
        courtDefendantStructure.setCourtIndividualDefendant(buildCourtIndividualDefendant());
        return courtDefendantStructure;
    }

    private CourtIndividualDefendantStructure buildCourtIndividualDefendant() {
        CourtIndividualDefendantStructure courtIndividualDefendantStructure = new CourtIndividualDefendantStructure();
        courtIndividualDefendantStructure.setPresentAtHearing("T");
        courtIndividualDefendantStructure.setBailStatus("A");
        courtIndividualDefendantStructure.setPersonDefendant(buildPersonDefendant());

        return courtIndividualDefendantStructure;
    }

    private BasePersonDefendantStructure buildPersonDefendant() {
        BasePersonDefendantStructure basePersonDefendantStructure = new BasePersonDefendantStructure();
        basePersonDefendantStructure.setPNCidentifier("pnc12");
        basePersonDefendantStructure.setPersonStatedNationality("UK");
        basePersonDefendantStructure.setBasePersonDetails(buildBasePersonDetails());
        return basePersonDefendantStructure;
    }

    private BasePersonDetailStructure buildBasePersonDetails() {
        BasePersonDetailStructure basePersonDetailStructure = new BasePersonDetailStructure();
        basePersonDetailStructure.setPersonName(buildPersonName());
        basePersonDetailStructure.setGender((byte) 1);
        return basePersonDetailStructure;
    }

    private BasePersonNameStructure buildPersonName() {
        BasePersonNameStructure basePersonNameStructure = new BasePersonNameStructure();
        basePersonNameStructure.setPersonFamilyName("sadas");
        return basePersonNameStructure;
    }

    private CourtHearingStructure buildCourtHearing() {
        CourtHearingStructure courtHearingStructure = new CourtHearingStructure();
        courtHearingStructure.setPSAcode(valueOf(1223));
        courtHearingStructure.setHearing(buildHearing());
        return courtHearingStructure;
    }

    private BaseHearingStructure buildHearing() {
        BaseHearingStructure baseHearingStructure = new BaseHearingStructure();
        try {
            final LocalDateTime localDateTime = LocalDateTime.of(2019,04,10,10,15,59);
            baseHearingStructure.setDateOfHearing(DatatypeFactory.newInstance().newXMLGregorianCalendar(localDateTime.toString()));
            baseHearingStructure.setTimeOfHearing(DatatypeFactory.newInstance().newXMLGregorianCalendar(localDateTime.toString()));
            baseHearingStructure.setCourtHearingLocation("LUOJBV");
        } catch (DatatypeConfigurationException e) {
        }
        return baseHearingStructure;
    }
}