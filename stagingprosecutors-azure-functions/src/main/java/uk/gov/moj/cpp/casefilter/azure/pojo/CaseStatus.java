package uk.gov.moj.cpp.casefilter.azure.pojo;

@SuppressWarnings({"squid:S00116"})
public class CaseStatus {
    private String RowKey;
    private boolean IsFiltered;
    private boolean IsEjected;

    public String getKey() { return this.RowKey; }

    public boolean isFiltered() { return this.IsFiltered;}

    public boolean isEjected() { return this.IsEjected; }
}
