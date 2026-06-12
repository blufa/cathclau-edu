package gn.lacaeducation.academics.application.event;

/** Published when an academic year is archived. Marks the year's data read-only. */
public record AnneeScolaireArchiveeEvent(Long tenantId, Long academicYearId) {}
