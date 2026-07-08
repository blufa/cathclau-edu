package gn.lacaeducation.platform.adapters.out.persistence;

import gn.lacaeducation.platform.domain.EstablishmentType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persiste l'ensemble des types d'établissement d'une école (une école peut être à la fois primaire,
 * collège, lycée…) dans une seule colonne texte séparée par des virgules. Ordre d'insertion conservé
 * ({@link LinkedHashSet}) pour un affichage stable.
 */
@Converter
public class EstablishmentTypeSetConverter implements AttributeConverter<Set<EstablishmentType>, String> {

    @Override
    public String convertToDatabaseColumn(Set<EstablishmentType> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public Set<EstablishmentType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(EstablishmentType::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
