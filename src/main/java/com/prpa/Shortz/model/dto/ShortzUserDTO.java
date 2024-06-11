package com.prpa.Shortz.model.dto;

import com.prpa.Shortz.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Data
@AllArgsConstructor @NoArgsConstructor
public class ShortzUserDTO {

    private UUID id;

    @Length(min = 3, message = "Length must be at least of 3")
    @NotBlank(message = "{0} must not be blank")
    private String username;

    @Email
    @NotBlank(message = "{0} must not be blank")
    private String email;

    private Integer urlCount;

    private Role role;

    private Boolean enabled;

}
