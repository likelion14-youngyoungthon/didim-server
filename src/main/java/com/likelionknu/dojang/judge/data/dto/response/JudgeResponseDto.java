package com.likelionknu.dojang.judge.data.dto.response;

import java.util.List;

public record JudgeResponseDto(String id, String name, String verdict, String status, boolean hidden,
        Office office, List<String> formNames, List<FormFile> formFiles, List<String> documents,
        Integer processingDays, String deadline, List<String> notes, Integer eligibleFromAge, Basis basis,
        String category, String shortDesc, String description, String checkReason,
        Benefit benefit, List<Condition> conditions) {
    public record Office(String id, String name, String phone) {}
    public record FormFile(String name, String url) {}
    public record Basis(String confidence, List<String> sources, List<String> unverified, String incomeNote) {}
    public record Benefit(String amount, String note) {}
    public record Condition(String key, String label, String rule, String value, Boolean met) {}
}
