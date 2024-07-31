package com.prpa.Shortz.model.form;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URL;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class ShortUrlForm {

    @NotBlank
    @org.hibernate.validator.constraints.URL
    private URL url;

    private String slug;
}
