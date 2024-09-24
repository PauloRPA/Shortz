package com.prpa.Shortz.integration;

import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Objects;

public class TestUtils {

    @SuppressWarnings("unchecked")
    public static <T> List<T> extractAttrbute(MvcResult mvcResult, String attribute, Class<T> expectedType) {
        Object urisPageObj = Objects.requireNonNull(mvcResult.getModelAndView()).getModel().get(attribute);
        Page<T> urisPage = (Page<T>) urisPageObj;
        return urisPage.stream().toList();
    }

}
