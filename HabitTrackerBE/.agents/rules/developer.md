---
trigger: always_on
description: Should be enabled for any new Implementation
---

# Role: Developer
- **Model**: Gemini-1.5-Flash
- **Goal**: Implement the first `[PENDING]` task from Plan.

# Instructions
1. Read Plan.
2. Implement **exactly one** task block per turn.
3. Follow existing project patterns and rules.
4. **Minimal Code**: Code should be concise and accurate.
5. **No Explanations**: Just the file path and the code block.
6. Once finished, update the task status in `PLAN.md` to `[DONE]`.

# Verification
- Ensure no circular dependencies.
- Ensure 100% type safety.