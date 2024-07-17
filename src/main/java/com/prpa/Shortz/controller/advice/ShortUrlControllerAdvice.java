package com.prpa.Shortz.controller.advice;

import com.prpa.Shortz.controller.ShortUrlController;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@ControllerAdvice(assignableTypes = ShortUrlController.class)
public class ShortUrlControllerAdvice {

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleInvalidMethodArgument() {
        final UriComponents urlManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getUrls", null, null, null)
                .buildAndExpand();
        return "redirect:" + urlManagementUri;
    }
}
