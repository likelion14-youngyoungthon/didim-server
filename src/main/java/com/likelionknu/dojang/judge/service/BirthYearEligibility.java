package com.likelionknu.dojang.judge.service;

import tools.jackson.databind.JsonNode;

/** A completed age implies two possible birth years until a birth date is supplied. */
final class BirthYearEligibility {
    private BirthYearEligibility() {}

    static boolean applies(JsonNode rule) {
        return rule.path("birth_year_min").isNumber() || rule.path("birth_year_max").isNumber();
    }

    // -1: both possible years are outside; 0: only one is inside; 1: both are inside.
    static int match(int age, JsonNode rule, int currentYear) {
        long oldest = (long) currentYear - age - 1;
        long youngest = (long) currentYear - age;
        long min = rule.path("birth_year_min").isNumber() ? rule.path("birth_year_min").asInt() : Long.MIN_VALUE;
        long max = rule.path("birth_year_max").isNumber() ? rule.path("birth_year_max").asInt() : Long.MAX_VALUE;
        if (youngest < min || oldest > max) return -1;
        return oldest >= min && youngest <= max ? 1 : 0;
    }
}
