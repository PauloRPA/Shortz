package com.prpa.Shortz.model.validation;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.form.ShortzUserEditForm;
import com.prpa.Shortz.service.ShortzUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class ShortzUserEditFormValidator implements Validator {

    private final ShortzUserService shortzUserService;

    @Autowired
    public ShortzUserEditFormValidator(ShortzUserService shortzUserService) {
        this.shortzUserService = shortzUserService;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.isAssignableFrom(ShortzUserEditForm.class);
    }

    @Override
    public void validate(Object formObj, Errors errors) {
        final ShortzUserEditForm form = ((ShortzUserEditForm) formObj);

        if (form.getId() == null) {
            errors.rejectValue("username", "error.editForm.notfound");
            return;
        }

        Optional<ShortzUser> userFound = shortzUserService.findById(Long.parseLong(form.getId()));
        userFound.ifPresent(user -> checkIfItAlreadyExists(form, user, errors));
    }

    private void checkIfItAlreadyExists(ShortzUserEditForm form, ShortzUser userFound, Errors errors) {
        if (form.getUsername() != null) {
            final boolean isNotRenamingSameUser = !form.getUsername().equalsIgnoreCase(userFound.getUsername());

            if (isNotRenamingSameUser && shortzUserService.existsByUsernameIgnoreCase(form.getUsername())) {
                errors.rejectValue("username", "error.exists");
            }
        }

        if (form.getEmail() != null) {
            final boolean isNotChangingCurrentUserEmail = !form.getEmail().equalsIgnoreCase(userFound.getEmail());

            if (isNotChangingCurrentUserEmail && shortzUserService.existsByEmailIgnoreCase(form.getEmail())) {
                errors.rejectValue("email","error.exists");
            }
        }

    }
}
