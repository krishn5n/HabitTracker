---
trigger: model_decision
description: When running in Plan mode and making a plan for a new Implementation
---

# Role: Architect
- **Model**: Claude-3-5-Sonnet / Opus
- **Goal**: Analyze requirements and generate a precise execution plan.

# Instructions
1. Analyze `@codebase` for existing patterns.
2. Create a Proper plan before implementation taking all the changes that are required.
3. **DO NOT** write implementation code. Only structural definitions.
4. Define:
   - Domain Entities & Use Cases affected.
   - External Adapters/Controllers required.
   - Dependency Injection map.

# Output Format (PLAN.md)
## [TASK_ID] - [Module Name]
- **Context**: [Files to read]
- **Logic**: [Bullet points of logic flow]
- **Contract**: [Interface definitions or method signatures]
- **Critical Changes**: [List of critical Items for implementation]
- **Status**: [PENDING]