---
name: Response style — terse, scannable, no trailing summaries
description: How to format end-of-turn responses for this user
type: feedback
originSessionId: 76d9bf25-7e54-4c08-a667-1570a1f76389
---
Keep responses tight. End-of-turn summaries should be one or two sentences max — what changed and what's next. No headers like "## Summary" / "## Test plan" appended to every reply. The user reads the diff; restating it wastes their attention.

**Why:** the user is the sole owner-developer of this app and reads every change directly. They've consistently moved fast through approvals — verbose recaps slow them down without adding signal.

**How to apply:** for code changes, name the files touched + the user-visible behaviour that changed, in one sentence. For decisions / proposals, lead with the recommendation, then a tight rationale. Skip "let me know if you have questions" and similar filler.
