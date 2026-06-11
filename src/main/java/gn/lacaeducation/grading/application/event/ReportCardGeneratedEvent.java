package gn.lacaeducation.grading.application.event;

public record ReportCardGeneratedEvent(Long reportCardId, Long studentId, Long tenantId) {}
