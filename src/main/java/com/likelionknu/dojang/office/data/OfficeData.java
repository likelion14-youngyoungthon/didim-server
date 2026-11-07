package com.likelionknu.dojang.office.data;

import java.time.LocalDate;
import java.util.List;

/** 출처: 처인·기흥·수지구청 행정복지센터 안내 페이지, 기관 공식 누리집. 좌표는 처인구청 지도에 있는 곳만 넣었다. */
public record OfficeData(LocalDate checkedAt, List<DongCenter> dongCenters, List<Office> offices) {

    public record DongCenter(String gu, String dong, String name, String address, String phone, Double lat, Double lng, String source) {
    }

    public record Office(String id, String name, String address, String phone, String source) {
    }
}
