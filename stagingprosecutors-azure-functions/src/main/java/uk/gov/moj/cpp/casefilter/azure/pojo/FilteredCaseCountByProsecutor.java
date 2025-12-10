package uk.gov.moj.cpp.casefilter.azure.pojo;

import java.util.Map;

public class FilteredCaseCountByProsecutor {
    private Map<String, Long> totalFilteredCaseCountByProsecutor;
    private Map<String, Long> dailyFilteredCaseCountByProsecutor;

    public FilteredCaseCountByProsecutor(Map<String, Long>  totalFilteredCaseCountByProsecutor, Map<String, Long>  dailyFilteredCaseCountByProsecutor){
        this.totalFilteredCaseCountByProsecutor = totalFilteredCaseCountByProsecutor;
        this.dailyFilteredCaseCountByProsecutor = dailyFilteredCaseCountByProsecutor;
    }

    public Map<String, Long> getTotalFilteredCaseCountByProsecutor() {
        return totalFilteredCaseCountByProsecutor;
    }

    public void setTotalFilteredCaseCountByProsecutor(final Map<String, Long> totalFilteredCaseCountByProsecutor) {
        this.totalFilteredCaseCountByProsecutor = totalFilteredCaseCountByProsecutor;
    }

    public Map<String, Long> getDailyFilteredCaseCountByProsecutor() {
        return dailyFilteredCaseCountByProsecutor;
    }

    public void setDailyFilteredCaseCountByProsecutor(final Map<String, Long> dailyFilteredCaseCountByProsecutor) {
        this.dailyFilteredCaseCountByProsecutor = dailyFilteredCaseCountByProsecutor;
    }
}
