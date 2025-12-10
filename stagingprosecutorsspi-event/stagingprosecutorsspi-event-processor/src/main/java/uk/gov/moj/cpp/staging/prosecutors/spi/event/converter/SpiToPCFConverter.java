package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.time.ZonedDateTime;

public class SpiToPCFConverter implements ParameterisedConverter<SpiProsecutionCaseReceived, InitiateProsecution, ZonedDateTime> {

    public InitiateProsecution convert(final SpiProsecutionCaseReceived source, final ZonedDateTime dateReceived) {

        final PoliceCase policeCase = source.getPoliceCase();

        return InitiateProsecution.initiateProsecution()
                .withCaseDetails(new CaseDetailsConverter().convert(source, dateReceived))
                .withDefendants(new CcDefendantConverter().convert(policeCase.getDefendants(), source.getPoliceCase().getCaseDetails().getInitialHearing()))
                .withChannel(SPI)
                .withExternalId(source.getOiId())
                .build();
    }

}
