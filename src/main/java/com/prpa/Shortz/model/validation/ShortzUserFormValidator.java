package com.prpa.Shortz.model.validation;

import com.prpa.Shortz.model.form.ShortzUserForm;
import com.prpa.Shortz.service.ShortzUserService;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@NoArgsConstructor @Component
public class ShortzUserFormValidator implements Validator {

    private ShortzUserService userService;

    @Autowired
    public ShortzUserFormValidator(ShortzUserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ShortzUserForm.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        validateUsernameExists((ShortzUserForm) target, errors);
        validateEmailExists((ShortzUserForm) target, errors);
    }

    private void validateEmailExists(ShortzUserForm target, Errors errors) {
        if (userService.existsByEmailIgnoreCase(target.getEmail())) {
            errors.rejectValue("email","error.exists");
        }
    }

    private void validateUsernameExists(ShortzUserForm target, Errors errors) {
        if (userService.existsByUsernameIgnoreCase(target.getUsername())) {
            errors.rejectValue("username", "error.exists");
        }
    }

}
