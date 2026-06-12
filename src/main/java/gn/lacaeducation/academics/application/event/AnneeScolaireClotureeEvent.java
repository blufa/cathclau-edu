package gn.lacaeducation.academics.application.event;

/** Published when an academic year is closed. Consumed by grading to freeze grades. */
public record AnneeScolaireClotureeEvent(Long tenantId, Long academicYearId) {}
