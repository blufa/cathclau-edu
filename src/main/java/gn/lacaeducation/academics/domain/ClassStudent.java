package gn.lacaeducation.academics.domain;

/**
 * A lightweight projection of a student in the context of a class roster.
 */
public record ClassStudent(Long id, String firstName, String lastName, String studentNumber, String email) {}
