# Conventions transverses (à appliquer dans TOUS les modules)

- Pagination standard sur toutes les collections : `?page=0&size=20&sort=champ,asc`. Réponse enveloppée `{ content, page, size, totalElements, totalPages }`.
- Filtres : query params explicites (`?statut=ACTIF&classeId=12`).
- Erreurs : format RFC 7807 (`application/problem+json`) avec `type`, `title`, `status`, `detail`, `instance`, `errors[]` pour la validation.
- Dates en ISO-8601, montants en `BigDecimal` sérialisés en string, devise implicite tenant.

### Codes HTTP

`200` lecture/maj, `201` création (+ `Location`), `204` suppression, `400` validation, `401` non authentifié, `403` permission/quota/tenant, `404` introuvable, `409` conflit (doublon, état incohérent), `422` règle métier violée, `429` rate limit.

### Sécurité par défaut

- Chaque endpoint déclare la **permission** requise (`@PreAuthorize("hasAuthority('...')")`).
- Toute écriture est journalisée dans l'`AuditLog` (voir §13).
- Validation des entrées via `spring-boot-starter-validation` (Bean Validation).

---
