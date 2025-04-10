package uk.gov.hmcts.cp.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.openapi.model.CourtHousesschema;
import uk.gov.hmcts.cp.openapi.model.CourtHousesschemaCourtHouse;

@Service
public class OpenApiService {

    public CourtHousesschema getCourtHouse(String courtId) {
        CourtHousesschema courtHousesschema = new CourtHousesschema();
        CourtHousesschemaCourtHouse courtHouse = new CourtHousesschemaCourtHouse();
        courtHouse.courtHouseName("House name 221B");
        courtHouse.courtHouseCode(courtId);
        courtHousesschema.courtHouse(courtHouse);
        return courtHousesschema;
    }
}
