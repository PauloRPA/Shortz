package com.prpa.Shortz.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ShortUrlForm {

    @NotBlank(message = "The uri must not be blank.")
    private String uri;

    private String slug;
}
