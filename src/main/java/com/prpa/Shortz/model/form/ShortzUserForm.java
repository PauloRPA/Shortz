package com.prpa.Shortz.model.form;

import com.prpa.Shortz.model.annotations.FieldMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@FieldMatch(fieldName = "password", confirmFieldName = "confirmPassword", message = "error.match.confirmPassword")
@Data @AllArgsConstructor @NoArgsConstructor
public class ShortzUserForm {

    @Length(min = 3, message = "Length must be at least of 3")
    @NotBlank(message = "Must not be blank")
    private String username;

    @Email
    @NotBlank(message = "Must not be blank")
    private String email;

    @Length(min = 6, message = "Length must be at least of 6")
    @NotBlank(message = "Must not be blank")
    private String password;

    @Length(min = 6, message = "Length must be at least of 6")
    @NotBlank(message = "Must not be blank")
    private String confirmPassword;

}
