package uk.gov.moj.cpp.casefilter.azure.pojo;

public class FilteredCaseCount {
    private long totalFilteredCasesCount;
    private long dailyFilteredCasesCount;

    public FilteredCaseCount(long totalFilteredCasesCount, long dailyFilteredCasesCount) {
        this.totalFilteredCasesCount = totalFilteredCasesCount;
        this.dailyFilteredCasesCount = dailyFilteredCasesCount;
    }

    public long getTotalFilteredCasesCount() {
        return totalFilteredCasesCount;
    }

    public void setTotalFilteredCasesCount(final long totalFilteredCasesCount) {
        this.totalFilteredCasesCount = totalFilteredCasesCount;
    }

    public long getDailyFilteredCasesCount() {
        return dailyFilteredCasesCount;
    }

    public void setDailyFilteredCasesCount(final long dailyFilteredCasesCount) {
        this.dailyFilteredCasesCount = dailyFilteredCasesCount;
    }
}
