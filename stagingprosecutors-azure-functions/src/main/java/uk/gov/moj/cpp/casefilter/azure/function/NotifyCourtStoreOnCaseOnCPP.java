package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;

import static java.lang.String.format;

import uk.gov.moj.cpp.casefilter.azure.service.NotifyCourtStoreService;

@SuppressWarnings("squid:S2629")
public class NotifyCourtStoreOnCaseOnCPP {
    private NotifyCourtStoreService notifyCourtStoreService;

    public NotifyCourtStoreOnCaseOnCPP() {
        if (this.notifyCourtStoreService == null) {
            this.notifyCourtStoreService = new NotifyCourtStoreService();
        }
    }

    @FunctionName("notifyCourtStoreOnCaseOnCpp")
    public void notifyCourtStoreOnCaseOnCpp(
            @EventGridTrigger(name = "event") String data,
            final ExecutionContext context) {
        context.getLogger().info(format("notifyCourtStoreOnCaseOnCpp: function entry, Event content: %s", data));
        notifyCourtStoreService.relayCase(data, context.getLogger());
        context.getLogger().info("notifyCourtStoreOnCaseOnCpp : Court store notified,  function exit");

    }

    public void setNotifyCourtStoreService(final NotifyCourtStoreService notifyCourtStoreService) {
        this.notifyCourtStoreService = notifyCourtStoreService;
    }
}
