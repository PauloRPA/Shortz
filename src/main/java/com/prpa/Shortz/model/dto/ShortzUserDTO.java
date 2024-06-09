package com.prpa.Shortz.model.dto;

import com.prpa.Shortz.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor @NoArgsConstructor
public class ShortzUserDTO {

    private String username;

    private String email;

    private Integer urlCount;

    private Role role;

    private Boolean enabled;

}
