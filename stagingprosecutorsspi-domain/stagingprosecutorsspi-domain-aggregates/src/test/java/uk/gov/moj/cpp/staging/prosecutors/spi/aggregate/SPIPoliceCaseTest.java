package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiPoliceCaseEjected;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SPIPoliceCaseTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID OI_ID = randomUUID();
    private static final UUID OI_ID2 = randomUUID();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceCase policeCase1;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceCase policeCase2;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceCase policeCase3;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceCase policeCaseWithDuplicateDefendants;

    @Mock
    private PoliceDefendant policeDefendant1;

    @Mock
    private PoliceDefendant policeDefendant2;

    @Mock
    private PoliceDefendant policeDefendant3;

    @Mock
    private SpiInitialHearing initialHearing;

    private SPIPoliceCase spiPoliceCaseAggregate;


    @BeforeEach
    public void setup() {
        spiPoliceCaseAggregate = new SPIPoliceCase();
    }

    @Test
    public void shouldRaiseDefendantsAddedEventWhenSubsequentSPIMessageReceivedWithAdditionalDefendant() {
        spiPoliceCaseAggregate.receivePoliceCase(CASE_ID, OI_ID, this.policeCase1);
        final Stream<Object> eventStream = spiPoliceCaseAggregate.receivePoliceCase(CASE_ID, OI_ID2, this.policeCase2);

        final List<Object> eventList = eventStream.collect(toList());
        assertEquals(1, eventList.size());

        final SpiProsecutionCaseReceived spiProsecutionCaseUpdateReceived = (SpiProsecutionCaseReceived) eventList.get(0);
        assertThat(spiProsecutionCaseUpdateReceived.getPoliceCase(), is(policeCase2));
        assertThat(spiProsecutionCaseUpdateReceived.getCaseId(), is(CASE_ID));
        assertThat(spiProsecutionCaseUpdateReceived.getOiId(), is(OI_ID2));
    }

    @Test
    public void shouldCreateEventWithCorrectCaseIDWhenReceivingPoliceCase() {
        final UUID oiId = randomUUID();
        final Stream<Object> eventStream = spiPoliceCaseAggregate.receivePoliceCase(CASE_ID, oiId, this.policeCase1);

        final List<Object> eventList = eventStream.collect(toList());
        assertEquals(1, eventList.size());

        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = (SpiProsecutionCaseReceived) eventList.get(0);
        assertThat(spiProsecutionCaseReceived.getOiId(), is(oiId));
        assertThat(spiProsecutionCaseReceived.getCaseId(), is(CASE_ID));
        assertThat(spiProsecutionCaseReceived.getPoliceCase(), is(policeCase1));
    }

    @Test
    public void shouldCreateEventWithCorrectCaseIDWhenEjectingPoliceCase() {
        spiPoliceCaseAggregate.receivePoliceCase(CASE_ID, OI_ID, this.policeCase1);

        final Stream<Object> eventStream = spiPoliceCaseAggregate.handleEjectCase(CASE_ID);

        final List<Object> eventList = eventStream.collect(toList());
        assertEquals(1, eventList.size());

        final SpiPoliceCaseEjected spiPoliceCaseEjected = (SpiPoliceCaseEjected) eventList.get(0);
        assertThat(spiPoliceCaseEjected.getCaseId(), is(CASE_ID));

    }

    @Test
    public void shouldNotCreateEventWhenEjectingPoliceCaseForNonSPiCase() {
        final Stream<Object> eventStream = spiPoliceCaseAggregate.handleEjectCase(CASE_ID);

        final List<Object> eventList = eventStream.collect(toList());
        assertEquals(0, eventList.size());

    }
}
