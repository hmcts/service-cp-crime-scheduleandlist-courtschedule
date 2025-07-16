package uk.gov.hmcts.cp.services;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryCaseUrnMapper implements CaseUrnMapperService {

    private final Map<String, String> caseUrnToCaseIdMap = new ConcurrentHashMap<>();

    public void saveCaseUrnMapping(final String caseUrn, final String caseId) {
        caseUrnToCaseIdMap.put(caseUrn, caseId);
    }

    @Override
    public String getCaseId(final String caseUrn) {
        return caseUrnToCaseIdMap.getOrDefault(caseUrn, "default-case-id");
    }

    public void clearAllMappings() {
        caseUrnToCaseIdMap.clear();
    }
}