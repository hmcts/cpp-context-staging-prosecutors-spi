package uk.gov.moj.cpp.casefilter.azure.function;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.casefilter.azure.service.NotifyCourtStoreService;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

@SuppressWarnings({"squid:S2629", "squid:S1312", "squid:S1450", "squid:S3008"})
public class NotifyCourtStoreOnCaseEjected {
    private static final String CASE_URN = "CaseReference";
    private NotifyCourtStoreService notifyCourtStoreService;

    public NotifyCourtStoreOnCaseEjected() {
        if (this.notifyCourtStoreService == null) {
            this.notifyCourtStoreService = new NotifyCourtStoreService();
        }
    }

    public void setNotifyCourtStoreService(final NotifyCourtStoreService notifyCourtStoreService) {
        this.notifyCourtStoreService = notifyCourtStoreService;
    }

    @FunctionName("notifyCourtStoreOnCaseEject")
    public void notifyCourtStoreOnCaseEject(@EventGridTrigger(name = "event") String eventSchema,
                                            final ExecutionContext context) {

        final Logger logger = context.getLogger();

        logger.info(format("Java EventGrid trigger function notifyCourtStoreOnCaseEject begin, Event content: %s", eventSchema));

        final JsonObject jsonObj = FileUtil.getJsonObject(eventSchema);
        final String data = jsonObj.getString("data", null);
        logger.log(Level.INFO, format("data - %s", data ));

        if (data == null){
            logger.log(Level.INFO, "Data not present" );
            return ;
        }
        final JsonObject dataObj = FileUtil.getJsonObject(data);
        final String caseURN = dataObj.getString(CASE_URN, null);
        logger.log(Level.INFO, format("CaseReference - %s", caseURN ));

        if (isBlank(caseURN)) {
            logger.log(Level.INFO, "Please pass CaseURN in the query string" );
            return ;
        }

        notifyCourtStoreService.notifyCourtStore(caseURN, logger);
        logger.log(Level.INFO, "notifyCourtStoreOnCaseEject : Court store notified,  function exit");
    }
}
