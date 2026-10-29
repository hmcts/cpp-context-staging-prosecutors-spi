package uk.gov.moj.cpp.casefilter.azure.utils;

import uk.gov.justice.services.messaging.JsonObjects;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * J17/J25 parity characterisation (J17 golden master), SPI-M05.
 *
 * This module is deployed on its own as an Azure Function App. Its runtime javaVersion and JAVA_VERSION app setting
 * come from java.se.version, which is 17 on main and 25 after the upgrade, and the classes are compiled for that
 * version. It also loads utilities-core 25.104.x, whose JsonObjects is compiled for Java 25 (major version 69) and is
 * used by FileUtil, CpsPayloadTransformService, QueryMaterialAndSendNotificationService and RelayCaseToLibraFunction.
 * A Function App can only load these classes if it runs that Java runtime. The J25 upgrade moves the Function App
 * (deployed on Windows, see the azure-functions-maven-plugin runtime) to Java 25, so pin the J25 class-file level
 * (major version 69). The Function App's runtime must move to Java 25 in the same release as this jar.
 */
class AzureFunctionsBytecodeLevelParityTest {

    private static final int JAVA_25_CLASS_FILE_MAJOR_VERSION = 69;

    @Test
    void spiM05_frameworkJsonObjectsShouldTargetTheJava25FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(JsonObjects.class), is(JAVA_25_CLASS_FILE_MAJOR_VERSION));
    }

    @Test
    void spiM05_moduleClassesShouldTargetTheJava25FunctionAppRuntime() throws IOException {
        assertThat(classFileMajorVersion(FileUtil.class), is(JAVA_25_CLASS_FILE_MAJOR_VERSION));
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
