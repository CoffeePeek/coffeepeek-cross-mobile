# Home check-ins

Owns the private home preparation domain contract, validation, HTTP implementation,
and photo upload flow. Only application composition may construct the repository;
UI consumes its domain interface. It may depend on shared network infrastructure
and Ktor, but has no dependency on shop check-ins or the legacy domain/data/network
modules. DTOs and the Ktor implementation remain in `data` and are not exposed.

A separate module is needed because the application composition module must not
own feature business rules or HTTP representations, and the legacy layer modules
are not the destination for new features. The existing image picker and navigation
remain in the application during this incremental migration.
