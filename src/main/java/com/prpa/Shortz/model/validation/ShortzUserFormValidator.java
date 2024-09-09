package com.prpa.Shortz.model.validation;

import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.service.ShortzUserService;
import com.prpa.Shortz.util.ControllerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.Validator;

import static java.util.Objects.requireNonNullElse;

@Component
public class ShortzUserFormValidator implements Validator {

    private final ShortzUserService shortzUserService;

    @Autowired
    public ShortzUserFormValidator(ShortzUserService shortzUserService) {
        this.shortzUserService = shortzUserService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ShortzUserForm.class);
    }

    @Override
    public void validate(Object targetObj, Errors errors) {
        final ShortzUserForm form = ((ShortzUserForm) targetObj);

        attachFieldMatchToPasswordField(form, errors);
        checkIfItAlreadyExists(form, errors);
    }

    private void attachFieldMatchToPasswordField(ShortzUserForm target, Errors errors) {
        for (ObjectError globalFieldMatchError : errors.getGlobalErrors()) {
            String field = ControllerUtils.globalErrorToFieldByMessage(globalFieldMatchError, "match");
            if (!field.isBlank()) {
                String message = requireNonNullElse(globalFieldMatchError.getDefaultMessage(), "");
                errors.rejectValue(field, message, "The fields must match.");
            }
        }
    }

    private void checkIfItAlreadyExists(ShortzUserForm form, Errors errors) {
        if (shortzUserService.existsByUsernameIgnoreCase(form.getUsername())) {
            errors.rejectValue("username", "error.exists");
        }

        if (shortzUserService.existsByEmailIgnoreCase(form.getEmail())) {
            errors.rejectValue("email","error.exists");
        }
    }
}
