package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.application.service.BiometricService.PunchCommand;
import gn.lacaeducation.biometric.domain.BiometricPunch;
import gn.lacaeducation.biometric.domain.PunchDirection;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Biometric Punches", description = "Ingestion (Push API) et journal des pointages biométriques")
@RestController
@RequestMapping("/api/v1/biometric")
@PreAuthorize("hasAnyRole('SCHOOL_ADMIN','DIRECTOR','SECRETARY')")
@RequiredArgsConstructor
public class BiometricPunchController {

    private final BiometricService service;

    public record PunchRequest(String externalUserId, LocalDateTime punchTime, PunchDirection direction,
                               BigDecimal temperature, String rawPayload) {}

    @Operation(summary = "Journal des pointages (filtres appareil / période)")
    @GetMapping("/punches")
    public List<BiometricPunch> log(@RequestParam(required = false) Long deviceId,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.punches(TenantContext.get(), deviceId, from, to);
    }

    @Operation(summary = "Ingestion d'un pointage poussé par l'appareil/agent (Push API)")
    @PostMapping("/devices/{deviceId}/punches")
    public BiometricPunch ingest(@PathVariable Long deviceId, @RequestBody PunchRequest r) {
        return service.ingestPunch(TenantContext.get(), deviceId,
                new PunchCommand(r.externalUserId(), r.punchTime(), r.direction(), r.temperature(), r.rawPayload()));
    }

    @Operation(summary = "Ingestion d'un lot de pointages")
    @PostMapping("/devices/{deviceId}/punches/batch")
    public List<BiometricPunch> ingestBatch(@PathVariable Long deviceId, @RequestBody List<PunchRequest> r) {
        return service.ingestBatch(TenantContext.get(), deviceId, r.stream()
                .map(x -> new PunchCommand(x.externalUserId(), x.punchTime(), x.direction(), x.temperature(), x.rawPayload()))
                .toList());
    }
}
