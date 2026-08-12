package gn.lacaeducation.payments.adapters.out.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persiste les élèves couverts par un paiement groupé (§payments — fratrie) en une colonne texte
 * entourée de virgules (ex. ",12,45,") : ce padding rend une recherche d'appartenance par
 * {@code LIKE '%,45,%'} fiable, sans confondre l'id 4 avec 45.
 */
@Converter
public class PaymentStudentIdsConverter implements AttributeConverter<List<Long>, String> {

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return "," + attribute.stream().filter(java.util.Objects::nonNull)
                .map(String::valueOf).collect(Collectors.joining(",")) + ",";
    }

    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
