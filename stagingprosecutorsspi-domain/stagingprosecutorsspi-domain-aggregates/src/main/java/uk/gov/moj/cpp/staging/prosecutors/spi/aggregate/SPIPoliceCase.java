package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CaseFilterFailed;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ProsecutionCaseFiltered;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiPoliceCaseEjected;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings({"squid:S1068", " squid:S1450"})
public class SPIPoliceCase implements Aggregate {

    private static final long serialVersionUID = 101L;
    private UUID caseId;
    private String caseInitiation;
    private SPIPoliceCaseStatus spiPoliceCaseStatus = SPIPoliceCaseStatus.LIVE;

    public Stream<Object> receivePoliceCase(final UUID caseId, final UUID oiId, final PoliceCase policeCase) {

        return handleCaseReceivedRequest(caseId, policeCase, oiId);
    }

    public Stream<Object> handleEjectCase(final UUID caseId) {
        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();
        if (this.caseId != null) {
            eventStreamBuilder.add(new SpiPoliceCaseEjected(caseId));
        }
        return apply(eventStreamBuilder.build());
    }

    public Stream<Object> filterProsecutionCase(final UUID caseId) {
        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();
        if (this.caseId == null) {
            eventStreamBuilder.add(new ProsecutionCaseFiltered(caseId));

        } else {
            // We don't expect case filter command for case which is already created
            eventStreamBuilder.add(new CaseFilterFailed(caseId));
        }

        return apply(eventStreamBuilder.build());
    }

    private Stream<Object> handleCaseReceivedRequest(final UUID caseId, final PoliceCase policeCase, final UUID oiId) {
        final Stream.Builder<Object> eventStreamBuilder = Stream.builder();
        eventStreamBuilder.add(new SpiProsecutionCaseReceived(caseId, oiId, policeCase));
        return apply(eventStreamBuilder.build());
    }


    @SuppressWarnings("squid:S1602")
    @Override
    public Object apply(Object event) {
        return match(event).with(when(SpiProsecutionCaseReceived.class).apply(spiProsecutionCaseReceived -> {
                    this.caseId = spiProsecutionCaseReceived.getCaseId();
                    this.caseInitiation = spiProsecutionCaseReceived.getPoliceCase().getCaseDetails().getCaseInitiationCode();
                }),
                when(SpiPoliceCaseEjected.class).apply(spiPoliceCaseEjected -> {
                    this.spiPoliceCaseStatus = SPIPoliceCaseStatus.EJECTED;
                }),
                otherwiseDoNothing()
        );
    }

}
