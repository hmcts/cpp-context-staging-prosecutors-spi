package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static java.time.ZonedDateTime.now;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiToPCFConverterTest {

    private final StoppedClock clock = new StoppedClock(now());

    private final SpiToPCFConverter target = new SpiToPCFConverter();

    @Test
    public void shouldConvertCaseToPCFCommand() throws IOException {

        final URL eventUrl = this.getClass().getClassLoader().getResource("stagingprosecutorsspi.event.prosecution-case-received-for-cc.json");
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = objectMapper.readValue(eventUrl, SpiProsecutionCaseReceived.class);

        final InitiateProsecution initiateProsecution = new SpiToPCFConverter().convert(spiProsecutionCaseReceived, clock.now());
        final UUID offenceId =  initiateProsecution.getDefendants().get(0).getOffences().get(0).getOffenceId();
        final UUID externalId =  initiateProsecution.getExternalId();

        final String initiateProsecutionJsonString = objectMapper.writeValueAsString(initiateProsecution);
        String expectedJsonString = IOUtils.toString(this.getClass().getClassLoader().getResource("cc.pcf.expected.payload.json"));
        expectedJsonString = expectedJsonString
                .replace("OFFENCE_ID", offenceId.toString())
                .replace("DATE_RECEIVED", clock.now().toLocalDate().toString())
                .replace("EXTERNAL_ID", externalId.toString());

        assertEquals(expectedJsonString, initiateProsecutionJsonString, STRICT);
    }

}
