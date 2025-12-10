package uk.gov.moj.cpp.staging.prosecutors;

import uk.gov.justice.services.adapter.rest.interceptor.InputStreamFileInterceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntryProvider;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

public class StagingProsecutorsServiceCommandApiInterceptorChainProvider implements InterceptorChainEntryProvider {

    @Override
    public String component() {
        return COMMAND_API;
    }

    @Override
    public List<InterceptorChainEntry> interceptorChainTypes() {
        final List<InterceptorChainEntry> interceptorChainEntries = new ArrayList<>();
        interceptorChainEntries.add(new InterceptorChainEntry(6000, InputStreamFileInterceptor.class));
        return interceptorChainEntries;
    }
}