package com.prpa.Shortz.controller.advice;

import com.prpa.Shortz.controller.ShortUrlController;
import com.prpa.Shortz.model.exceptions.EmptyUriException;
import com.prpa.Shortz.model.exceptions.InvalidUriException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

@ControllerAdvice(assignableTypes = ShortUrlController.class)
public class ShortUrlControllerAdvice {

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public String handleInvalidMethodArgument() {
        final UriComponents uriManagementUri = MvcUriComponentsBuilder
                .fromMethodName(ShortUrlController.class, "getUris", null, null, null)
                .buildAndExpand();
        return "redirect:" + uriManagementUri;
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({EmptyUriException.class})
    public String handleNoUriRequestBody(EmptyUriException exception, ServletWebRequest request) {
        return "";
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({InvalidUriException.class})
    public String handleInvalidUri(InvalidUriException exception, ServletWebRequest request) {
        return "";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleValidationExceptions(MethodArgumentNotValidException ex) {
        ModelAndView mav = new ModelAndView();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            mav.getModelMap().put(fieldName, errorMessage);
        });
        ex.getBindingResult().getModel().forEach(mav.getModel()::put);

        mav.setViewName("/user/uris/newUri");
        return mav;
    }
}
