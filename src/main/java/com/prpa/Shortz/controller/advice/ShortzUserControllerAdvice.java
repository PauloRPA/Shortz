package com.prpa.Shortz.controller.advice;

import com.prpa.Shortz.controller.ShortzUserController;
import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.util.ControllerUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(assignableTypes = ShortzUserController.class)
public class ShortzUserControllerAdvice {

    @ModelAttribute("userForm")
    public ShortzUserForm addDefaultShortzUserForm(ModelAndView mav) {
        return new ShortzUserForm();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationException(MethodArgumentNotValidException ex, Model model) {
        model.addAttribute("userForm", ex.getBindingResult().getTarget());

        // Transforma um erro de validação global tipo @FieldMatch em um erro atrelado a um campo
        String FIELD_MATCH = "match";
        ex.getGlobalErrors().stream()
                .filter(e -> e.getDefaultMessage().contains(FIELD_MATCH))
                .forEach(globalFieldMatchError -> {
                    String field = ControllerUtils.globalErrorToFieldByMessage(globalFieldMatchError, FIELD_MATCH);
                    if (!field.isBlank())
                        ex.getBindingResult().rejectValue(field, globalFieldMatchError.getDefaultMessage(), "The fields must match.");
                });

        ex.getBindingResult().getModel().forEach(model::addAttribute);
        return "register";
    }

}
