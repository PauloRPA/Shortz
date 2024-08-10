package com.prpa.Shortz.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrlDTO {

    private String id;

    private String owner;

    private Instant creationTimestamp;

    @NotNull
    private Integer hit;

    @NotNull
    @NotBlank
    private String slug;

    @NotNull
    private URI url;
}
