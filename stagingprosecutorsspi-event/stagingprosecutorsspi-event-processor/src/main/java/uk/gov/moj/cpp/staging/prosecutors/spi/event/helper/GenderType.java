package uk.gov.moj.cpp.staging.prosecutors.spi.event.helper;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

import java.util.Optional;

public enum GenderType {
    NOT_KNOWN(0),

    NOT_SPECIFIED(9),

    MALE(1),

    FEMALE(2);

    private final Integer value;

    GenderType(Integer value) {
        this.value = value;
    }

    public static Optional<Gender> valueFor(final Integer value) {
        for (final GenderType type : GenderType.values()) {
            if (type.value.equals(value)) {
                return Gender.valueFor(type.name());
            }
        }
        return Optional.empty();
    }

    public static int valueFor(final String value) {
        for (final GenderType type : GenderType.values()) {
            if (type.name().equals(value)) {
                return type.value;
            }
        }
        return NOT_SPECIFIED.value;
    }

}