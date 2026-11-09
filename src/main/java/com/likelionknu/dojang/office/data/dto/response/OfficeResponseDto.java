package com.likelionknu.dojang.office.data.dto.response;

import java.time.LocalDate;

public record OfficeResponseDto(String id, String name, String kind, String address, String phone, Double lat, Double lng, LocalDate checkedAt) {
}
