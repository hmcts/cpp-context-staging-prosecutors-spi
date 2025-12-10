package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.IntStream.rangeClosed;

import uk.gov.justice.services.test.utils.core.random.Generator;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UrnGenerator extends Generator<String> {

    private String force;
    private String unit;
    private String number;
    private String year;

    private UrnGenerator() {
        force = RandomGenerator.integer(10, 99).next().toString();
        unit = RandomGenerator.string(2).next().toUpperCase();
        number = RandomGenerator.integer(10000, 99999).next().toString();
        year = RandomGenerator.integer(10, 99).next().toString();
    }

    public static UrnGenerator withAthenaPoliceForceCode() {
        final UrnGenerator urnGenerator = new UrnGenerator();
        urnGenerator.force = urnGenerator.getRandomAthenaPoliceForceCode();
        return urnGenerator;
    }

    public static UrnGenerator withNonAthenaPoliceForceCode() {
        final UrnGenerator urnGenerator = new UrnGenerator();
        urnGenerator.force = "55";
        return urnGenerator;
    }

    public static String aRandomUrn() {
        return new UrnGenerator().next();
    }

    @Override
    public String next() {
        return force + unit + number + year;
    }

    private static final List<String> ATHENA_POLICE_FORCE_CODES = newArrayList("46", "42", "37", "36", "40",
            "23", "35", "10", "22", "20", "14", "04", "03", "11", "06", "63");


    private String getRandomAthenaPoliceForceCode() {
        return RandomGenerator.values(ATHENA_POLICE_FORCE_CODES).next();
    }

    private String getRandomNonAthenaPoliceForceCode() {
        return RandomGenerator.values(rangeClosed(10, 99).filter(j -> !ATHENA_POLICE_FORCE_CODES.contains(j)).boxed()
                .collect(Collectors.toCollection(ArrayList::new))).next().toString();
    }
}
