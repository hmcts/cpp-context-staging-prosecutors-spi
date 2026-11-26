package uk.gov.moj.cpp.casefilter.azure.service;

import static java.lang.String.format;
import static java.lang.System.getenv;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;

import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

@SuppressWarnings({"squid:S2629", "squid:S2221", "squid:S00112", "squid:S1162"})
public class QueryMaterialAndSendNotificationService {
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String BASE_URI =  getenv("INTEGRATION_HOST_URI");
    private static final String MATERIAL_QUERY_ENDPOINT = BASE_URI + "/material-query-api/query/api/rest/material/material/%s?stream=true";
    private static final String MEDIA_TYPE = "application/vnd.material.query.material+json";

    private static final String USER_ID = "CJSCPPUID";
    private static final String MATERIAL_SYSTEM_USER_ID_VALUE = "material-user-id";

    private CpsApiService cpsApiService;
    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    public QueryMaterialAndSendNotificationService() {
        this.cpsApiService = new CpsApiService();
    }

    public ResponseDto getMaterialById(final String materialId, final String fileName, final JsonObject transformedPayload, final Logger logger) {

        final String endPointUrl = format(MATERIAL_QUERY_ENDPOINT, materialId);

        logger.info(format("Querying for material: %s", endPointUrl));

        final HttpGet httpGet = new HttpGet(endPointUrl);
        httpGet.addHeader("Accept", MEDIA_TYPE);
        httpGet.addHeader(USER_ID, getenv(MATERIAL_SYSTEM_USER_ID_VALUE));

        try (final CloseableHttpClient httpClient = httpClientWrapper.createSecureHttpClient();
             final CloseableHttpResponse response = httpClient.execute(httpGet)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            if (SC_OK == statusCode) {
                logger.info("statusCode: " + statusCode);
                return getDocumentFromAzureBlog(IOUtils.toString(response.getEntity().getContent()), fileName, transformedPayload, logger);
            }
            final String errorMessage = format("Unable to query for material with ID '%s'.  Obtained status code: %s", materialId, statusCode);
            logger.severe(errorMessage);
            throw new RuntimeException(errorMessage);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private ResponseDto getDocumentFromAzureBlog(final String blogUrl, final String fileName, final JsonObject transformedPayload, final Logger logger) {
        final HttpGet httpGetDocument = new HttpGet(blogUrl);
        httpGetDocument.addHeader(USER_ID, getenv(MATERIAL_SYSTEM_USER_ID_VALUE));
        try (final CloseableHttpClient azureBlobClient = httpClientWrapper.createSecureHttpClient();
             final CloseableHttpResponse azureBlobResponse = azureBlobClient.execute(httpGetDocument)) {
            final int statusCodeAzureBlobResponse = azureBlobResponse.getStatusLine().getStatusCode();
            if (SC_OK == statusCodeAzureBlobResponse) {
                final HttpEntity azureBlobResponseEntity = azureBlobResponse.getEntity();
                final InputStream materialInputStream = azureBlobResponseEntity.getContent();
                // need to make the call here as we need the inputstream handle which will not be available outside of this try block
                return cpsApiService.sendMaterialNotification(transformedPayload, materialInputStream, fileName, logger,
                        Optional.ofNullable(Optional.ofNullable(transformedPayload.getJsonObject("materialNotification"))
                                        .orElse(createObjectBuilder().build()).getString("materialContentType", null))
                                .orElse(APPLICATION_PDF));
            }
            final String errorMessage = format("Unable to get document from azure blog, URL is '%s'.", blogUrl);
            logger.severe(errorMessage);
            throw new RuntimeException(errorMessage);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
