# Kana Dojo Writing Technology

Kana Dojo currently uses guided trace scoring, not full handwriting recognition. That is an intentional product boundary: the writing exercise teaches shape coverage, direction changes, and kana-specific checkpoints without claiming to recognize free handwriting.

This document records the implementation choices for future writing work so the app can improve without confusing learners or adding hard-to-maintain dependencies.

## Current Implementation

- UI input: Compose `Canvas` with drag gesture collection in `TraceExerciseUi`.
- Stroke persistence: simple `TracePoint` snapshot tokens for save/restore during a session, including explicit stroke-start markers so multi-stroke kana are not replayed or scored as one continuous path.
- Scoring: in-repo guided scoring in `TraceScoring`, using path length, spread, occupied grid cells, turn changes, optional kana-specific checkpoints, and stroke-count expectations for selected confusable or stroke-sensitive kana.
- UX claim: "guided trace scoring", not "handwriting recognition".

This is the right baseline for early kana learning because learners need shape confidence before unconstrained handwriting checks.

## Recommended Package Path

### Phase 1: AndroidX Ink For Input And Rendering

Use AndroidX Ink when we want higher-quality stroke authoring, rendering, brush behavior, or stroke storage.

Relevant artifacts on Google Maven:

- `androidx.ink:ink-authoring-compose`
- `androidx.ink:ink-brush-compose`
- `androidx.ink:ink-strokes`
- `androidx.ink:ink-rendering`
- `androidx.ink:ink-storage`

Recommended starting version: `1.0.0`.

Latest checked version on Google Maven: `1.1.0-alpha04` as of 2026-07-05.

Use this for:

- smoother stylus/finger stroke capture
- richer brush rendering
- structured stroke data
- future replay/comparison polish
- cleaner stroke-start data feeding the existing guided scoring contract

Do not use this as a recognizer. AndroidX Ink handles ink, not kana correctness.

### Phase 2: In-Repo Template Matching

For an open, GPL-friendly next step, add a small template matcher around normalized stroke paths. Good candidates are `$1` or `$P` style recognizers adapted to Kotlin.

Use this for:

- "close to this kana shape" feedback
- comparing learner strokes to a small kana-specific template set
- keeping scoring deterministic and testable

Avoid presenting this as ML handwriting recognition. It is shape similarity.

### Optional: ML Kit Digital Ink Recognition

Google ML Kit Digital Ink can provide real handwriting recognition.

Artifact checked on Google Maven:

- `com.google.mlkit:digital-ink-recognition:19.0.0`

Use only if we are willing to support:

- model download and deletion states
- offline/model-unavailable fallback UI
- confidence/candidate display
- language support limits
- dependency and GPL distribution review

If added, keep it behind a separate implementation boundary or build flavor. Do not silently replace guided trace scoring with ML Kit results.

## Current Decision

Keep the current guided trace scoring as the default writing exercise.

Next implementation work should prefer AndroidX Ink `1.0.0` for input/rendering quality, while preserving the existing trace scoring contract. Full handwriting recognition should remain optional until the app has model-management UI and a clear licensing/distribution decision.

## UX Requirements

- The UI must say "trace", "guided trace", "shape", or "stroke shape" unless a true recognizer is active.
- A recognizer-backed exercise must expose model download, unavailable, and fallback states.
- Writing should not block all lesson progress unless the learner has explicitly entered a writing-focused practice mode.
- Mistakes in writing should trigger comparison and retry guidance before being sent to broader weak-kana review.
