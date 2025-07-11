package uk.gov.hmcts.cp.pact.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;

public class JsonFileToObject {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static <T> T readJsonFromResources(String fileName, Class<T> clazz) throws Exception {
        File file = new File(JsonFileToObject.class.getClassLoader().getResource(fileName).toURI());
        return mapper.readValue(file, clazz);
    }
}