package gn.lacaeducation.platform.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
    @NotBlank String schoolName,
    @NotBlank @Email String email,
    @NotBlank String phone,
    String address,
    String city,
    String country,
    String director
) {}
