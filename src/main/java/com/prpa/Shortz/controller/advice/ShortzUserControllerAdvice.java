package com.prpa.Shortz.controller.advice;

import com.prpa.Shortz.controller.ShortzUserController;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@ControllerAdvice(basePackageClasses = ShortzUserController.class)
public class ShortzUserControllerAdvice {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleInvalidMethodArgument() {
        final UriComponents adminPanelUri = MvcUriComponentsBuilder
            .fromMethodName(ShortzUserController.class, "getUserManagement", null, null, null)
            .buildAndExpand();
        return "redirect:" + adminPanelUri;
    }
}
