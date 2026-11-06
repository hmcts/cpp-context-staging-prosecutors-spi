package uk.gov.moj.cpp.casefilter.azure.utils;

import uk.gov.justice.services.messaging.JsonObjects;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * J17/J25 parity characterisation (J17 golden master), SPI-M05.
 *
 * This module is deployed on its own as an Azure Function App whose Java runtime is Java 17. Everything it loads
 * must therefore be Java 17 bytecode (class-file major version 61) or older. The J25 upgrade pins the Function App
 * to Java 17 but brings in utilities-core 25.104.x, whose JsonObjects is compiled for Java 25 (major version 69);
 * a Java 17 host cannot load it (UnsupportedClassVersionError) the first time FileUtil, CpsPayloadTransformService,
 * QueryMaterialAndSendNotificationService or RelayCaseToLibraFunction touches it.
 */
class AzureFunctionsBytecodeLevelParityTest {

    private static final int JAVA_17_CLASS_FILE_MAJOR_VERSION = 61;

    @Test
    void spiM05_frameworkJsonObjectsShouldBeLoadableByTheJava17FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(JsonObjects.class), lessThanOrEqualTo(JAVA_17_CLASS_FILE_MAJOR_VERSION));
    }

    @Test
    void spiM05_moduleClassesShouldBeLoadableByTheJava17FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(FileUtil.class), lessThanOrEqualTo(JAVA_17_CLASS_FILE_MAJOR_VERSION));
    }

    private static int classFileMajorVersion(final Class<?> type) throws IOException {
        try (InputStream classFile = type.getResourceAsStream(type.getSimpleName() + ".class");
             DataInputStream data = new DataInputStream(classFile)) {
            data.readInt();
            data.readUnsignedShort();
            return data.readUnsignedShort();
        }
    }
}
