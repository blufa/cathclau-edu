package gn.lacaeducation.platform.adapters.in.web.dto;

import gn.lacaeducation.platform.application.port.in.SignupUseCase.SignupResult;

public record SignupResponse(Long tenantId, String slug, String status, String message) {
    public static SignupResponse from(SignupResult r) {
        return new SignupResponse(r.tenantId(), r.slug(), r.status().name(), r.message());
    }
}
