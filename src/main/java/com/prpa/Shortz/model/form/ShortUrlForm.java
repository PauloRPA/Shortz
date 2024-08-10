package com.prpa.Shortz.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ShortUrlForm {

    @NotBlank(message = "The url must not be blank.")
    private String url;

    private String slug;
}
