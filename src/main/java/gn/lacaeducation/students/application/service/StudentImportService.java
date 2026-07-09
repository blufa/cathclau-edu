package gn.lacaeducation.students.application.service;

import gn.lacaeducation.common.BusinessRuleException;
import gn.lacaeducation.common.TenantContext;
import gn.lacaeducation.students.application.port.in.StudentUseCase;
import gn.lacaeducation.students.domain.Gender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk student import from a CSV file (features §14.1). Expected header (order-independent):
 * firstName,lastName,gender,birthDate,birthPlace,phone,email,nationality,studentNumber,originSchool.
 * Each row is created independently; the per-row report lists successes and errors (incl. quota).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentImportService {

    private static final DateTimeFormatter FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final StudentUseCase studentUseCase;

    public record RowError(int line, String message) {}
    public record ImportReport(int total, int created, List<RowError> errors) {}

    public ImportReport importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessRuleException("Fichier vide");
        Long tenantId = TenantContext.get();
        if (tenantId == null) throw new BusinessRuleException("Aucun établissement dans le contexte");

        String content = new String(readBytes(file), StandardCharsets.UTF_8).replace("﻿", "");
        String[] lines = content.split("\\r?\\n");
        if (lines.length < 2) throw new BusinessRuleException("Le fichier doit contenir un en-tête et au moins une ligne");

        String[] header = splitCsv(lines[0]);
        int iFirst = idx(header, "firstName", "prenom", "prénom");
        int iLast = idx(header, "lastName", "nom");
        int iGender = idx(header, "gender", "sexe");
        int iBirth = idx(header, "birthDate", "dateNaissance", "naissance");
        int iBirthPlace = idx(header, "birthPlace", "lieuNaissance");
        int iPhone = idx(header, "phone", "telephone", "téléphone");
        int iEmail = idx(header, "email");
        int iNat = idx(header, "nationality", "nationalite", "nationalité");
        int iNum = idx(header, "studentNumber", "matricule");
        int iOrigin = idx(header, "originSchool", "ecoleOrigine", "écoleOrigine");
        if (iFirst < 0 || iLast < 0) {
            throw new BusinessRuleException("En-tête invalide : colonnes 'firstName' et 'lastName' requises");
        }

        List<RowError> errors = new ArrayList<>();
        int created = 0;
        int total = 0;
        for (int n = 1; n < lines.length; n++) {
            if (lines[n].isBlank()) continue;
            total++;
            String[] c = splitCsv(lines[n]);
            try {
                String firstName = get(c, iFirst);
                String lastName = get(c, iLast);
                if (firstName.isBlank() || lastName.isBlank()) throw new BusinessRuleException("Prénom et nom requis");
                studentUseCase.create(new StudentUseCase.CreateStudentCommand(
                        tenantId, null, blankToNull(get(c, iNum)), firstName, lastName,
                        parseGender(get(c, iGender)), parseDate(get(c, iBirth)), blankToNull(get(c, iBirthPlace)),
                        null, blankToNull(get(c, iPhone)), blankToNull(get(c, iEmail)), blankToNull(get(c, iNat)),
                        blankToNull(get(c, iOrigin)), null,
                        null, null, null, null, null, null, null, null,
                        null, null));
                created++;
            } catch (RuntimeException e) {
                errors.add(new RowError(n + 1, e.getMessage()));
            }
        }
        log.info("Student CSV import for tenant {}: {} created, {} errors of {}", tenantId, created, errors.size(), total);
        return new ImportReport(total, created, errors);
    }

    private static byte[] readBytes(MultipartFile f) {
        try { return f.getBytes(); } catch (Exception e) { throw new BusinessRuleException("Lecture du fichier impossible"); }
    }

    private static String[] splitCsv(String line) {
        // Simple CSV: comma or semicolon separated, trims quotes. (No embedded separators.)
        String sep = line.contains(";") && !line.contains(",") ? ";" : ",";
        String[] parts = line.split(sep, -1);
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim().replaceAll("^\"|\"$", "").trim();
        return parts;
    }

    private static int idx(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            for (String n : names) if (header[i].equalsIgnoreCase(n)) return i;
        }
        return -1;
    }

    private static String get(String[] row, int i) {
        return i >= 0 && i < row.length ? row[i] : "";
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

    private static Gender parseGender(String raw) {
        if (raw == null) return Gender.MALE;
        String v = raw.trim().toUpperCase();
        if (v.startsWith("F")) return Gender.FEMALE;
        return Gender.MALE;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim();
        try { return LocalDate.parse(v); } catch (Exception ignored) { /* try FR */ }
        try { return LocalDate.parse(v, FR); } catch (Exception ignored) { return null; }
    }
}
