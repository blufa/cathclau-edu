package gn.lacaeducation.biometric.adapters.in.web;

import gn.lacaeducation.biometric.application.service.BiometricService;
import gn.lacaeducation.biometric.application.service.BiometricService.DeviceCommand;
import gn.lacaeducation.biometric.domain.BiometricDevice;
import gn.lacaeducation.biometric.domain.BiometricDeviceType;
import gn.lacaeducation.biometric.domain.BiometricProtocol;
import gn.lacaeducation.common.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Biometric Devices", description = "Appareils biométriques : configuration, test, synchronisation")
@RestController
@RequestMapping("/api/v1/biometric/devices")
@PreAuthorize("hasAuthority('biometrique:manage')")
@RequiredArgsConstructor
public class BiometricDeviceController {

    private final BiometricService service;

    public record DeviceRequest(String name, BiometricDeviceType deviceType, String ipAddress, Integer port,
                                BiometricProtocol protocol, String location, Boolean active, Boolean realtimeSync,
                                Integer syncIntervalMinutes, String accuracyLevel, Boolean maskAllowed,
                                Boolean temperatureCheck, BigDecimal temperatureThreshold) {}

    @Operation(summary = "Lister les appareils")
    @GetMapping
    public List<BiometricDevice> list() { return service.devices(TenantContext.get()); }

    @Operation(summary = "Détail d'un appareil")
    @GetMapping("/{id}")
    public BiometricDevice get(@PathVariable Long id) { return service.device(id); }

    @Operation(summary = "Ajouter un appareil")
    @PostMapping
    public BiometricDevice create(@RequestBody DeviceRequest r) { return service.createDevice(toCommand(r)); }

    @Operation(summary = "Modifier un appareil")
    @PutMapping("/{id}")
    public BiometricDevice update(@PathVariable Long id, @RequestBody DeviceRequest r) { return service.updateDevice(id, toCommand(r)); }

    @Operation(summary = "Supprimer un appareil")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteDevice(id); return ResponseEntity.noContent().build(); }

    @Operation(summary = "Tester la connexion (TCP) à l'appareil")
    @PostMapping("/{id}/test-connection")
    public BiometricDevice testConnection(@PathVariable Long id) { return service.testConnection(id); }

    @Operation(summary = "Synchronisation manuelle (marque l'horodatage ; l'agent pousse les pointages)")
    @PostMapping("/{id}/sync")
    public BiometricDevice sync(@PathVariable Long id) { return service.syncDevice(id); }

    private DeviceCommand toCommand(DeviceRequest r) {
        return new DeviceCommand(TenantContext.get(), r.name(), r.deviceType(), r.ipAddress(), r.port(), r.protocol(),
                r.location(), r.active(), r.realtimeSync(), r.syncIntervalMinutes(), r.accuracyLevel(),
                r.maskAllowed(), r.temperatureCheck(), r.temperatureThreshold());
    }
}
