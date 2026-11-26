package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.System.getenv;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.apache.http.entity.mime.MultipartEntityBuilder.create;

import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

@SuppressWarnings({"squid:S2629", "squid:S2221"})
public class CpsApiService {
    private static final String APIM_CPS_NOTIFICATION_ENDPOINT = getenv("APIM_CPS_NOTIFICATION_ENDPOINT");
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String NOTIFICATION_TXT = "Notification.txt";
    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();
    private static final String COURT_STORE_APIM_KEY = getenv("COURT_STORE_APIM_KEY");
    private static final String OCP_APIM_SUBSCRIPTION_KEY = "Ocp-Apim-Subscription-Key";

    @SuppressWarnings({"squid:S2139", "squid:S00112", "squid:S2142"})
    public ResponseDto sendMaterialNotification(final JsonObject payload, final InputStream inputStream, final String filename, final Logger logger, final String contentType) {
        logger.info(String.format("Sending material notification to %s", APIM_CPS_NOTIFICATION_ENDPOINT));
        final HttpEntity data = create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("Material", inputStream, ContentType.create(
                        Optional.ofNullable(contentType).orElse(APPLICATION_PDF),
                        Charset.defaultCharset()), filename)
                .addBinaryBody("Notification", new ByteArrayInputStream(payload.toString().getBytes()), TEXT_PLAIN, NOTIFICATION_TXT)
                .build();
        final HttpUriRequest request = RequestBuilder
                .post(APIM_CPS_NOTIFICATION_ENDPOINT)
                .addHeader(OCP_APIM_SUBSCRIPTION_KEY, COURT_STORE_APIM_KEY)
                .setEntity(data)
                .build();
        logger.info(String.format("CPS JSON Payload : %s, mimetype %s", filename, contentType));
        logger.info(String.format("Executing request : %s", request.getRequestLine()));

        try (final CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
             final CloseableHttpResponse response = httpClient.execute(request)) {

            final int statusCode = response.getStatusLine().getStatusCode();
            final String messageBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != SC_OK) {
                logger.severe("Call to CPS notification endpoint failed with http status code : " + statusCode +
                        ", : response body :  " + messageBody);
            } else {
                logger.info(String.format("Call to CPS notification endpoint successful with http status code : %s and payload : %s", statusCode, messageBody));
            }
            return new ResponseDto(statusCode, messageBody);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}