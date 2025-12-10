package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SpiOutMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiResultListenerTest {

    @InjectMocks
    private SpiResultListener spiResultListener;

    @Mock
    private SpiOutMessageRepository spiOutMessageRepository;

    @Mock
    private Envelope<SpiResultPreparedForSending> envelope;

    @Captor
    private ArgumentCaptor<SpiOutMessage> argumentCaptor;

    @Mock
    private Metadata metadata;

    private static final String PTI_URN = "21EU9147637";
    private static final String PROSECUTOR_REFERENCE = "GB7890998798";

    private static final String XML_PAYLOAD = "<ResultedCaseMessage Flow=\"ResultedCasesForThePolice\" Interface=\"LibraStandardProsecutorPolice\" SchemaVersion=\"0.6g\" xmlns=\"http://www.dca.gov.uk/xmlschemas/libra\" xmlns:ns2=\"http://www.govtalk.gov.uk/people/bs7666\"><Session><CourtHearing><Hearing><CourtHearingLocation>B01LY00</CourtHearingLocation><DateOfHearing>2020-05-26</DateOfHearing><TimeOfHearing>09:00:00</TimeOfHearing></Hearing><PSAcode>2577</PSAcode></CourtHearing><Case><PTIURN>" + PTI_URN + "</PTIURN><Defendant><ProsecutorReference>" + PROSECUTOR_REFERENCE + "</ProsecutorReference><CourtIndividualDefendant><PersonDefendant><BasePersonDetails><PersonName><PersonTitle>Ormsby</PersonTitle><PersonGivenName1>Mike</PersonGivenName1><PersonFamilyName>Ormsby</PersonFamilyName></PersonName><Birthdate>1998-08-08</Birthdate><Gender>1</Gender></BasePersonDetails><BailConditions></BailConditions></PersonDefendant><BailStatus>C</BailStatus><Address><SimpleAddress><AddressLine1>56Police House</AddressLine1><AddressLine2>StreetDescription</AddressLine2><AddressLine3>Locality2O</AddressLine3></SimpleAddress></Address><PresentAtHearing>Y</PresentAtHearing><ReasonForBailConditionsOrCustody></ReasonForBailConditionsOrCustody></CourtIndividualDefendant><Offence><BaseOffenceDetails><OffenceSequenceNumber>000</OffenceSequenceNumber><OffenceCode>TW01046</OffenceCode><OffenceWording>Has a violent past and fear that he will commit further offences and\n" +
            "    interfere with witnesse</OffenceWording><OffenceTiming><OffenceDateCode>1</OffenceDateCode><OffenceStart><OffenceDateStartDate>2004-12-09</OffenceDateStartDate></OffenceStart></OffenceTiming><ChargeDate>2004-12-09</ChargeDate><ArrestDate>2005-11-19</ArrestDate></BaseOffenceDetails><InitiatedDate>2004-12-09</InitiatedDate><Plea>3</Plea><FinalDisposalIndicator>Y</FinalDisposalIndicator><ConvictingCourt>2577</ConvictingCourt><Result><ResultCode>4592</ResultCode><ResultText>Defendant's details changed</ResultText><NextHearing><NextHearingDetails><CourtHearingLocation>C05LV00</CourtHearingLocation><DateOfHearing>2020-09-20</DateOfHearing><TimeOfHearing>09:30:00</TimeOfHearing></NextHearingDetails><NextHearingReason>To attend or a warrant to issue</NextHearingReason></NextHearing><Outcome><Duration><DurationStartDate>2021-10-26</DurationStartDate><DurationEndDate>2021-10-28</DurationEndDate></Duration></Outcome></Result><Result><ResultCode>1018</ResultCode><ResultText>Conditional discharge\n" +
            "Period of conditional discharge 6 Days</ResultText><Outcome><Duration><DurationValue>6</DurationValue><DurationUnit>D</DurationUnit></Duration></Outcome></Result></Offence><Offence><BaseOffenceDetails><OffenceSequenceNumber>000</OffenceSequenceNumber><OffenceCode>PS01002</OffenceCode><OffenceWording>Has a violent past and fear that he will commit further offences and\n" +
            "                interfere with witnesse</OffenceWording><OffenceTiming><OffenceDateCode>1</OffenceDateCode><OffenceStart><OffenceDateStartDate>2004-12-09</OffenceDateStartDate></OffenceStart></OffenceTiming><ChargeDate>2004-12-09</ChargeDate><ArrestDate>2005-11-19</ArrestDate></BaseOffenceDetails><InitiatedDate>2004-12-09</InitiatedDate><Plea>3</Plea><FinalDisposalIndicator>Y</FinalDisposalIndicator><ConvictingCourt>2577</ConvictingCourt><Result><ResultCode>1018</ResultCode><ResultText>Conditional discharge\n" +
            "Period of conditional discharge 2 Months</ResultText><NextHearing><NextHearingDetails><CourtHearingLocation>C05LV00</CourtHearingLocation><DateOfHearing>2020-09-20</DateOfHearing><TimeOfHearing>09:30:00</TimeOfHearing></NextHearingDetails><BailStatusOffence>A</BailStatusOffence></NextHearing><Outcome><Duration><DurationValue>2</DurationValue><DurationUnit>M</DurationUnit></Duration></Outcome></Result></Offence></Defendant></Case></Session></ResultedCaseMessage>";

    private static final String XML_PAYLOAD_WITHOUT_PROSECUTOR_REFERENCE = "<ResultedCaseMessage Flow=\"ResultedCasesForThePolice\" Interface=\"LibraStandardProsecutorPolice\" SchemaVersion=\"0.6g\" xmlns=\"http://www.dca.gov.uk/xmlschemas/libra\" xmlns:ns2=\"http://www.govtalk.gov.uk/people/bs7666\"><Session><CourtHearing><Hearing><CourtHearingLocation>B01LY00</CourtHearingLocation><DateOfHearing>2020-05-26</DateOfHearing><TimeOfHearing>09:00:00</TimeOfHearing></Hearing><PSAcode>2577</PSAcode></CourtHearing><Case><PTIURN>" + PTI_URN + "</PTIURN><Defendant><CourtIndividualDefendant><PersonDefendant><BasePersonDetails><PersonName><PersonTitle>Ormsby</PersonTitle><PersonGivenName1>Mike</PersonGivenName1><PersonFamilyName>Ormsby</PersonFamilyName></PersonName><Birthdate>1998-08-08</Birthdate><Gender>1</Gender></BasePersonDetails><BailConditions></BailConditions></PersonDefendant><BailStatus>C</BailStatus><Address><SimpleAddress><AddressLine1>56Police House</AddressLine1><AddressLine2>StreetDescription</AddressLine2><AddressLine3>Locality2O</AddressLine3></SimpleAddress></Address><PresentAtHearing>Y</PresentAtHearing><ReasonForBailConditionsOrCustody></ReasonForBailConditionsOrCustody></CourtIndividualDefendant><Offence><BaseOffenceDetails><OffenceSequenceNumber>000</OffenceSequenceNumber><OffenceCode>TW01046</OffenceCode><OffenceWording>Has a violent past and fear that he will commit further offences and\n" +
            "    interfere with witnesse</OffenceWording><OffenceTiming><OffenceDateCode>1</OffenceDateCode><OffenceStart><OffenceDateStartDate>2004-12-09</OffenceDateStartDate></OffenceStart></OffenceTiming><ChargeDate>2004-12-09</ChargeDate><ArrestDate>2005-11-19</ArrestDate></BaseOffenceDetails><InitiatedDate>2004-12-09</InitiatedDate><Plea>3</Plea><FinalDisposalIndicator>Y</FinalDisposalIndicator><ConvictingCourt>2577</ConvictingCourt><Result><ResultCode>4592</ResultCode><ResultText>Defendant's details changed</ResultText><NextHearing><NextHearingDetails><CourtHearingLocation>C05LV00</CourtHearingLocation><DateOfHearing>2020-09-20</DateOfHearing><TimeOfHearing>09:30:00</TimeOfHearing></NextHearingDetails><NextHearingReason>To attend or a warrant to issue</NextHearingReason></NextHearing><Outcome><Duration><DurationStartDate>2021-10-26</DurationStartDate><DurationEndDate>2021-10-28</DurationEndDate></Duration></Outcome></Result><Result><ResultCode>1018</ResultCode><ResultText>Conditional discharge\n" +
            "Period of conditional discharge 6 Days</ResultText><Outcome><Duration><DurationValue>6</DurationValue><DurationUnit>D</DurationUnit></Duration></Outcome></Result></Offence><Offence><BaseOffenceDetails><OffenceSequenceNumber>000</OffenceSequenceNumber><OffenceCode>PS01002</OffenceCode><OffenceWording>Has a violent past and fear that he will commit further offences and\n" +
            "                interfere with witnesse</OffenceWording><OffenceTiming><OffenceDateCode>1</OffenceDateCode><OffenceStart><OffenceDateStartDate>2004-12-09</OffenceDateStartDate></OffenceStart></OffenceTiming><ChargeDate>2004-12-09</ChargeDate><ArrestDate>2005-11-19</ArrestDate></BaseOffenceDetails><InitiatedDate>2004-12-09</InitiatedDate><Plea>3</Plea><FinalDisposalIndicator>Y</FinalDisposalIndicator><ConvictingCourt>2577</ConvictingCourt><Result><ResultCode>1018</ResultCode><ResultText>Conditional discharge\n" +
            "Period of conditional discharge 2 Months</ResultText><NextHearing><NextHearingDetails><CourtHearingLocation>C05LV00</CourtHearingLocation><DateOfHearing>2020-09-20</DateOfHearing><TimeOfHearing>09:30:00</TimeOfHearing></NextHearingDetails><BailStatusOffence>A</BailStatusOffence></NextHearing><Outcome><Duration><DurationValue>2</DurationValue><DurationUnit>M</DurationUnit></Duration></Outcome></Result></Offence></Defendant></Case></Session></ResultedCaseMessage>";


    @Test
    public void onSpiResultPreparedForSending() {
        final ZonedDateTime now = ZonedDateTime.now();
        when(envelope.payload()).thenReturn(new SpiResultPreparedForSending(null, XML_PAYLOAD, PTI_URN, randomUUID(), randomUUID()));
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.createdAt()).thenReturn(Optional.of(now));

        spiResultListener.onSpiResultPreparedForSending(envelope);

        verify(spiOutMessageRepository).save(argumentCaptor.capture());
        final SpiOutMessage savedValue = argumentCaptor.getValue();
        assertThat(savedValue.getCaseUrn(), is(PTI_URN));
        assertThat(savedValue.getDefendantReference(), is(PROSECUTOR_REFERENCE));
        assertThat(savedValue.getPayload(), is(XML_PAYLOAD));
        assertThat(savedValue.getTimestamp(), is(now));
        assertThat(savedValue.getId(), notNullValue());
    }

    @Test
    public void onSpiResultPreparedForSending_defensiveTestWithoutReference() {
        final ZonedDateTime now = ZonedDateTime.now();
        when(envelope.payload()).thenReturn(new SpiResultPreparedForSending(null, XML_PAYLOAD_WITHOUT_PROSECUTOR_REFERENCE, PTI_URN, randomUUID(), randomUUID()));
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.createdAt()).thenReturn(Optional.of(now));

        spiResultListener.onSpiResultPreparedForSending(envelope);

        verify(spiOutMessageRepository).save(argumentCaptor.capture());
        final SpiOutMessage savedValue = argumentCaptor.getValue();
        assertThat(savedValue.getCaseUrn(), is(PTI_URN));
        assertThat(savedValue.getDefendantReference(), is(nullValue()));
        assertThat(savedValue.getPayload(), is(XML_PAYLOAD_WITHOUT_PROSECUTOR_REFERENCE));
        assertThat(savedValue.getTimestamp(), is(now));
        assertThat(savedValue.getId(), notNullValue());
    }
}