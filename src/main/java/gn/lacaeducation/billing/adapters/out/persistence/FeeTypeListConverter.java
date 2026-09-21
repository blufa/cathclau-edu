package gn.lacaeducation.billing.adapters.out.persistence;

import gn.lacaeducation.billing.domain.FeeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persiste la liste des types de frais concernés par une règle (réduction familiale, avantage
 * personnel, exonération) dans une seule colonne texte séparée par des virgules (même pattern que
 * {@code InstallmentMonthsConverter}) — vide/null = tous les types de frais.
 */
@Converter
public class FeeTypeListConverter implements AttributeConverter<List<FeeType>, String> {

    @Override
    public String convertToDatabaseColumn(List<FeeType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<FeeType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(FeeType::valueOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
