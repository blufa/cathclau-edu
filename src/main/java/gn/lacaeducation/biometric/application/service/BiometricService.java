package gn.lacaeducation.biometric.application.service;

import gn.lacaeducation.biometric.application.port.out.BiometricDeviceRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricEnrollmentRepository;
import gn.lacaeducation.biometric.application.port.out.BiometricPunchRepository;
import gn.lacaeducation.biometric.domain.*;
import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Biometric Attendance (côté logiciel, inspiré OpenEduCat) : registre des appareils, enrôlement des
 * personnes (mapping vers l'ID interne appareil), et ingestion des pointages via Push API avec
 * résolution de la personne. Les protocoles matériels (capture empreinte/visage, polling ZK) relèvent
 * d'un agent/SDK externe qui pousse les pointages sur l'API d'ingestion.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BiometricService {

    private final BiometricDeviceRepository deviceRepo;
    private final BiometricEnrollmentRepository enrollmentRepo;
    private final BiometricPunchRepository punchRepo;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ── Appareils ─────────────────────────────────────────────────────────────

    public record DeviceCommand(Long tenantId, String name, BiometricDeviceType deviceType, String ipAddress,
                                Integer port, BiometricProtocol protocol, String location, Boolean active,
                                Boolean realtimeSync, Integer syncIntervalMinutes, String accuracyLevel,
                                Boolean maskAllowed, Boolean temperatureCheck, BigDecimal temperatureThreshold) {}

    public BiometricDevice createDevice(DeviceCommand c) {
        if (c.name() == null || c.name().isBlank()) throw new BusinessRuleException("Le nom de l'appareil est requis");
        return deviceRepo.save(apply(BiometricDevice.builder()
                .tenantId(c.tenantId()).status(DeviceStatus.UNKNOWN)
                .apiKey(java.util.UUID.randomUUID().toString().replace("-", "")).build(), c));
    }

    public BiometricDevice updateDevice(Long id, DeviceCommand c) { return deviceRepo.save(apply(load(id), c)); }

    private BiometricDevice apply(BiometricDevice d, DeviceCommand c) {
        d.setName(c.name());
        d.setDeviceType(c.deviceType() != null ? c.deviceType() : BiometricDeviceType.FINGERPRINT);
        d.setIpAddress(c.ipAddress()); d.setPort(c.port());
        d.setProtocol(c.protocol() != null ? c.protocol() : BiometricProtocol.PUSH);
        d.setLocation(c.location());
        d.setActive(c.active() == null || c.active());
        d.setRealtimeSync(Boolean.TRUE.equals(c.realtimeSync()));
        d.setSyncIntervalMinutes(c.syncIntervalMinutes() != null ? c.syncIntervalMinutes() : 5);
        d.setAccuracyLevel(c.accuracyLevel());
        d.setMaskAllowed(c.maskAllowed() == null || c.maskAllowed());
        d.setTemperatureCheck(Boolean.TRUE.equals(c.temperatureCheck()));
        d.setTemperatureThreshold(c.temperatureThreshold());
        return d;
    }

    public void deleteDevice(Long id) { deviceRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<BiometricDevice> devices(Long tenantId) { return deviceRepo.findByTenantId(tenantId); }

    @Transactional(readOnly = true)
    public BiometricDevice device(Long id) { return load(id); }

    /** Test de connectivité réel : tentative de connexion TCP vers ip:port (timeout 2 s). */
    public BiometricDevice testConnection(Long id) {
        BiometricDevice d = load(id);
        DeviceStatus status = DeviceStatus.DISCONNECTED;
        if (d.getIpAddress() != null && !d.getIpAddress().isBlank() && d.getPort() != null) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(d.getIpAddress(), d.getPort()), 2000);
                status = DeviceStatus.CONNECTED;
            } catch (Exception e) {
                status = DeviceStatus.DISCONNECTED;
            }
        }
        d.setStatus(status);
        return deviceRepo.save(d);
    }

    /**
     * Synchronisation manuelle : marque l'horodatage. La récupération effective des enregistrements
     * depuis l'appareil est faite par l'agent/SDK externe qui pousse ensuite sur {@link #ingestPunch}.
     */
    public BiometricDevice syncDevice(Long id) {
        BiometricDevice d = load(id);
        d.setLastSyncAt(LocalDateTime.now());
        return deviceRepo.save(d);
    }

    // ── Enrôlement ─────────────────────────────────────────────────────────────

    public BiometricEnrollment enroll(Long tenantId, Long deviceId, BiometricSubjectType subjectType, Long subjectId,
                                      BiometricType biometricType, String externalUserId) {
        load(deviceId);
        if (subjectId == null) throw new BusinessRuleException("La personne à enrôler est requise");
        if (externalUserId == null || externalUserId.isBlank()) throw new BusinessRuleException("L'identifiant appareil (User ID) est requis");
        BiometricEnrollment e = enrollmentRepo.findByDeviceIdAndExternalUserId(deviceId, externalUserId)
                .orElseGet(() -> BiometricEnrollment.builder().tenantId(tenantId).deviceId(deviceId).build());
        e.setSubjectType(subjectType != null ? subjectType : BiometricSubjectType.STUDENT);
        e.setSubjectId(subjectId);
        e.setBiometricType(biometricType != null ? biometricType : BiometricType.FINGERPRINT);
        e.setExternalUserId(externalUserId);
        e.setStatus(EnrollmentStatus.ENROLLED);
        e.setEnrolledAt(LocalDateTime.now());
        return enrollmentRepo.save(e);
    }

    public void deleteEnrollment(Long id) { enrollmentRepo.deleteById(id); }

    @Transactional(readOnly = true)
    public List<BiometricEnrollment> enrollments(Long tenantId, Long deviceId) {
        return deviceId != null ? enrollmentRepo.findByDeviceId(deviceId) : enrollmentRepo.findByTenantId(tenantId);
    }

    // ── Badges (QR) — la génération enrôle automatiquement la personne ───────────

    public record BadgeResult(String qrPayload, String externalUserId, BiometricSubjectType subjectType,
                              Long subjectId, Long enrollmentId) {}

    /**
     * Génère un badge QR pour une personne (élève ou enseignant) et l'enrôle automatiquement (contre un
     * appareil virtuel « Badge QR »). Le QR « BADGE|tenant|type|id » est scannable pour le pointage.
     */
    public BadgeResult generateBadge(Long tenantId, BiometricSubjectType subjectType, Long subjectId) {
        if (subjectId == null) throw new BusinessRuleException("La personne est requise");
        BiometricSubjectType type = subjectType != null ? subjectType : BiometricSubjectType.STUDENT;
        BiometricDevice badgeDevice = devices(tenantId).stream()
                .filter(d -> "Badge QR".equalsIgnoreCase(d.getName())).findFirst()
                .orElseGet(() -> createDevice(new DeviceCommand(tenantId, "Badge QR", BiometricDeviceType.FACE, null,
                        null, null, "Virtuel", true, false, null, null, null, false, null)));
        String externalUserId = "BADGE-" + type.name() + "-" + subjectId;
        BiometricEnrollment e = enroll(tenantId, badgeDevice.getId(), type, subjectId, BiometricType.FACE, externalUserId);
        String qrPayload = "BADGE|" + tenantId + "|" + type.name() + "|" + subjectId;
        return new BadgeResult(qrPayload, externalUserId, type, subjectId, e.getId());
    }

    // ── Pointages (Push API) ────────────────────────────────────────────────────

    public record PunchCommand(String externalUserId, LocalDateTime punchTime, PunchDirection direction,
                               BigDecimal temperature, String rawPayload) {}

    public BiometricPunch ingestPunch(Long tenantId, Long deviceId, PunchCommand c) {
        BiometricDevice d = load(deviceId);
        if (c.externalUserId() == null || c.externalUserId().isBlank()) throw new BusinessRuleException("User ID appareil requis");
        BiometricEnrollment enr = enrollmentRepo.findByDeviceIdAndExternalUserId(deviceId, c.externalUserId()).orElse(null);
        d.setStatus(DeviceStatus.CONNECTED);
        d.setLastSyncAt(LocalDateTime.now());
        deviceRepo.save(d);
        LocalDateTime when = c.punchTime() != null ? c.punchTime() : LocalDateTime.now();
        PunchDirection dir = c.direction() != null ? c.direction() : PunchDirection.UNKNOWN;
        BiometricPunch punch = punchRepo.save(BiometricPunch.builder()
                .tenantId(tenantId).deviceId(deviceId).externalUserId(c.externalUserId())
                .subjectType(enr != null ? enr.getSubjectType() : null)
                .subjectId(enr != null ? enr.getSubjectId() : null)
                .punchTime(when).direction(dir)
                .temperature(c.temperature()).rawPayload(c.rawPayload())
                .createdAt(LocalDateTime.now()).build());
        // Notifie le module attendance (création de présence) pour les pointages rattachés à une personne.
        if (enr != null) {
            eventPublisher.publishEvent(new gn.lacaeducation.biometric.application.event.BiometricPunchRecorded(
                    tenantId, enr.getSubjectType().name(), enr.getSubjectId(), when, dir.name()));
        }
        return punch;
    }

    public List<BiometricPunch> ingestBatch(Long tenantId, Long deviceId, List<PunchCommand> punches) {
        return punches.stream().map(p -> ingestPunch(tenantId, deviceId, p)).toList();
    }

    @Transactional(readOnly = true)
    public List<BiometricPunch> punches(Long tenantId, Long deviceId, LocalDateTime from, LocalDateTime to) {
        if (deviceId != null) return punchRepo.findByTenantIdAndDeviceId(tenantId, deviceId);
        if (from != null && to != null) return punchRepo.findByTenantIdAndPunchTimeBetween(tenantId, from, to);
        return punchRepo.findByTenantId(tenantId);
    }

    private BiometricDevice load(Long id) {
        return deviceRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("BiometricDevice", id));
    }
}
