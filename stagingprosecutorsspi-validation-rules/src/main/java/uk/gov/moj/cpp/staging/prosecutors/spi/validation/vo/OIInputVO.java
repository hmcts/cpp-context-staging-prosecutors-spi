package uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo;

public class OIInputVO {

    private String systemId;
    private String correlationId;

    public OIInputVO(final String systemId, final String correlationId) {
        this.systemId = systemId;
        this.correlationId = correlationId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(final String systemId) {
        this.systemId = systemId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    public static class OIInputVOBuilder {
        private String systemId;
        private String correlationId;

        public OIInputVOBuilder withSystemId(final String systemId) {
            this.systemId = systemId;
            return this;
        }

        public OIInputVOBuilder withCorrelationId(final String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public OIInputVO build() {
            return new OIInputVO(systemId, correlationId);
        }
    }
}
