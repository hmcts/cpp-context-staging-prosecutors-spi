package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

public interface ParameterisedConverter<S, T, P> {

    T convert(final S source, final P param);
}
