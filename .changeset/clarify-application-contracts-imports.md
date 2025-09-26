---
"scopes": patch
---

Clarify application-to-contracts import policy and add enforcement

- Document that Application boundary components (handlers, mappers, error mappers) may import contract types
- Add Konsist test to automatically enforce import rules
- Update architecture diagrams to reflect allowed dependencies
- Maintain domain purity while avoiding duplicate DTOs at boundaries
