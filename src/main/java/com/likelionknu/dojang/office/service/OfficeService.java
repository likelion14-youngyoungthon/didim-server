package com.likelionknu.dojang.office.service;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.office.data.OfficeData;
import com.likelionknu.dojang.office.data.OfficeData.DongCenter;
import com.likelionknu.dojang.office.data.OfficeData.Office;
import com.likelionknu.dojang.office.data.dto.response.OfficeResponseDto;
import com.likelionknu.dojang.office.exception.OfficeException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OfficeService {
    private static final String RECEPTION = "접수처";
    private static final String PROVIDER = "이용기관";
    private static final String DONG_CENTER = "dong_center";

    private final LocalDate checkedAt;
    private final Map<String, DongCenter> dongCenters = new HashMap<>();
    private final Map<String, Office> offices = new LinkedHashMap<>();
    private final Map<String, String> serviceOffices = new HashMap<>();

    public OfficeService(JsonMapper jsonMapper) throws IOException {
        OfficeData data = jsonMapper.readValue(new ClassPathResource("data/offices.json").getInputStream(), OfficeData.class);
        this.checkedAt = data.checkedAt();
        data.dongCenters().forEach(center -> dongCenters.put(center.dong(), center));
        data.offices().forEach(office -> offices.put(office.id(), office));

        for (String path : List.of("data/rules_child_v2.json", "data/rules_adult_v2.json")) {
            JsonNode services = jsonMapper.readTree(new ClassPathResource(path).getInputStream()).path("services");
            services.forEach(service -> serviceOffices.putIfAbsent(service.path("id").asString(), service.path("office_id").asString()));
        }
    }

    public List<OfficeResponseDto> getOffices(String dong, String service, String kind) {
        DongCenter center = dongCenters.get(dong);
        if (center == null) {
            throw new OfficeException(ErrorCode.BAD_REQUEST, "dong: 용인시 읍면동이 아닙니다.");
        }
        if (kind != null && !kind.equals(RECEPTION) && !kind.equals(PROVIDER)) {
            throw new OfficeException(ErrorCode.BAD_REQUEST, "kind: 접수처 또는 이용기관이어야 합니다.");
        }
        // 이용기관 데이터는 아직 모으지 않았다
        if (PROVIDER.equals(kind)) {
            return List.of();
        }

        if (service == null) {
            List<OfficeResponseDto> result = new ArrayList<>();
            result.add(toResponse(center));
            offices.values().forEach(office -> result.add(toResponse(office)));
            return result;
        }

        String officeId = serviceOffices.get(service);
        if (officeId == null) {
            throw new OfficeException(ErrorCode.NOT_FOUND, "service: 없는 서비스입니다.");
        }
        if (officeId.equals(DONG_CENTER)) {
            return List.of(toResponse(center));
        }
        // 자동차 판매처, 사업 수행기관처럼 정해진 장소가 없는 곳은 빈 목록
        Office office = offices.get(officeId);
        return office == null ? List.of() : List.of(toResponse(office));
    }

    private OfficeResponseDto toResponse(DongCenter center) {
        return new OfficeResponseDto(DONG_CENTER, center.name(), RECEPTION, center.address(), center.phone(), center.lat(), center.lng(), checkedAt);
    }

    private OfficeResponseDto toResponse(Office office) {
        return new OfficeResponseDto(office.id(), office.name(), RECEPTION, office.address(), office.phone(), null, null, checkedAt);
    }
}
