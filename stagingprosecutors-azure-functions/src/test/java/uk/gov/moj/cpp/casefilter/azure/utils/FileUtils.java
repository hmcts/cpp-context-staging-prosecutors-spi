package uk.gov.moj.cpp.casefilter.azure.utils;

import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static String getPayload(final String path) {
        String request = null;
        try {
            request = Resources.toString(Resources.getResource(path), defaultCharset());
        } catch (final Exception e) {
            LOGGER.error("Error consuming file from location - ", path, e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }
}
