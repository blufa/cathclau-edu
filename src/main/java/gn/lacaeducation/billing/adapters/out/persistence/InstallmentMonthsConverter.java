package gn.lacaeducation.billing.adapters.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persiste la liste des mois "YYYY-MM" couverts par une échéance dans une seule colonne texte
 * séparée par des virgules (même pattern que {@code EstablishmentTypeSetConverter}) — un mois au
 * format "YYYY-MM" ne contient jamais de virgule. Ordre d'insertion conservé.
 */
@Converter
public class InstallmentMonthsConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().filter(m -> m != null && !m.isBlank()).collect(Collectors.joining(","));
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
