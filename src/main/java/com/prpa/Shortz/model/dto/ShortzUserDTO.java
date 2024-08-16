package com.prpa.Shortz.model.dto;

import com.prpa.Shortz.model.enums.Role;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Data @Builder
@AllArgsConstructor @NoArgsConstructor
@EqualsAndHashCode
public class ShortzUserDTO {

    @EqualsAndHashCode.Exclude
    private UUID id;

    @Length(min = 3)
    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @Min(value = -1)
    @Max(value = 1_000_000)
    @NotNull(message = "{0} must not be null")
    private int urlCreationLimit;

    @NotNull(message = "{0} must not be null")
    private Role role;

    private boolean enabled;

}
