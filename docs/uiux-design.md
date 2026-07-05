# Kana Dojo UI/UX Direction

Kana Dojo should borrow Duolingo's pacing, not its exact look. The experience should feel calm, tactile, and rewarding: short sessions, visible progress, immediate feedback, and enough personality that daily practice does not feel like a form.

## Product Shape

The app has three primary loops:

1. Lesson path: learn new kana in a guided row-by-row sequence.
2. Practice: repair weak kana and confusable pairs.
3. Reference: browse the kana chart and hear symbols on demand.

The first screen should always answer one question: what should I do next?

## Visual Principles

- Use one clear primary action per screen.
- Make progress visible before asking for effort.
- Keep lesson cards compact enough to scan, but tactile enough to feel tappable.
- Use color as meaning: green for mastered, warm yellow for learning, coral for mistakes, violet for contrast work.
- Use large kana as the main visual asset. The written symbol is the product.
- Avoid decorative clutter. Expressiveness should come from shape, motion, spacing, and feedback.

## Material 3 Expressive Interpretation

Source check: Material Design's M3 Expressive article describes the update as a way to make products more engaging and easier to use. For Kana Dojo, that means expressive UI must improve lesson clarity and feedback speed, not just add decoration.

Material 3 Expressive should show up through:

- Confident shape: rounded lesson nodes, pill chips, large touch targets.
- Lively but purposeful motion: progress changes, screen transitions, selected states, and correct/incorrect feedback.
- Clear hierarchy: large kana prompts, medium lesson titles, small status labels.
- Color roles with contrast: primary for action, tertiary for contrast/confusable work, error/coral for misses.
- Accessible flow: readable text, stable layout, no hidden critical state behind color alone.

Implementation rules:

- The active next lesson should be visually stronger than the rest of the path.
- Answer feedback may scale, shift color, or reveal status, but it must not resize the answer grid.
- Motion should explain cause and effect: tap, answer, progress, complete.
- Exercise screens should prefer one strong prompt over multiple competing panels.
- Expressive color should keep semantic meaning stable across the app.

## Duolingo-Like Learning Feel

Use these mechanics:

- Path progression: locked future lessons and obvious next lesson.
- Micro sessions: each lesson is 2 to 5 minutes.
- Immediate correction: every answer changes state right away.
- Mistake replay: misses return in the same session and later review.
- Mastery labels: new, familiar, recall, contrast, fluent, mastered.
- Small wins: completion screen shows the kana just learned.

Avoid these:

- Streak pressure before the core learning loop is good.
- Too many currencies, badges, or screens.
- Long explanation screens.
- Kanji content before kana fluency.

## Screen Design

### Lessons

The lesson path should show:

- Script switcher at the top.
- Mastered count and next unlock rule.
- Lesson cards with stage chip, difficulty dots, kana preview, and mastery progress.
- Locked cards visible but subdued.

Bold question: should we make the path strictly linear, or allow advanced learners to test out of rows?

Recommendation: start linear, then add a placement test once the core exercises feel good.

### Exercise

Exercise screens should be distraction-free:

- Top progress bar and close button.
- Exercise title with icon.
- Large prompt.
- Four large choices or a full writing pad.
- Feedback banner that briefly confirms correct/incorrect.

Bold question: should wrong answers block progress until corrected?

Recommendation: no. Move forward, replay the mistake soon, and avoid turning a miss into friction.

### Mistakes

Mistakes should feel like a short repair session:

- Show why the queue exists: weak kana, not punishment.
- Prefer the weakest six kana.
- Mix recognition and trace prompts.
- Add confusable drills when available.

### Chart

The chart is a reference surface:

- Grid by script.
- Color by mastery.
- Tap to hear.
- Later: row filters and confusable highlighting.

## Motion Rules

- Use motion for cause and effect: tapping a lesson opens the runner, progress fills, feedback appears.
- Keep transitions short. Most motion should land under 300 ms.
- Use small scale changes for selected states.
- Avoid looping decorative animation during exercises.
- Respect future reduced-motion settings.

## Current Polish State

Implemented:

- Stage chips and difficulty dots on lesson cards.
- Locked lesson state based on previous lesson mastery.
- Animated lesson progress.
- Stronger active next-lesson state with animated card color, elevation, and node scale.
- Active next lesson now has a primary border and compact "Next" marker for faster path scanning.
- Animated answer feedback color and scale for choice exercises.
- Feedback banner and continue button enter/exit motion.
- Mode-aware practice queue panel for weak, contrast, writing, sound, speed, cross-script, and mixed review.
- Animated trace pad guide, border, background, stroke, and score-panel states.
- Trace comparison panel with model-vs-yours view and replayed learner stroke path.
- Stroke guidance cues for confusable kana in trace comparison.
- Outcome-aware lesson completion badge and restrained celebration sparks.
- Reduced-motion-aware lesson completion transition for summary and action reveal.
- Lesson-completion recommendations that route to repeat, mistake review, or the path.
- Clean lesson completions preview the next unlocked lesson before returning to the path.
- Data-driven path hero that previews the next lesson, overall progress, lesson progress, and review pressure.
- Reduced-motion top-bar toggle that disables trace replay animation while keeping comparison available.
- Persisted reduced-motion preference.
- Persisted sound toggle that quiets speech playback without changing lessons.
- Persisted haptics toggle that keeps answer feedback tactile without forcing vibration.
- Compact settings menu for motion, sound, and haptic controls, keeping script selection visible.
- Shared answer option component for kana, romaji, and sound-choice prompts.
- Extracted lesson-runner and completion shell so session flow can be polished independently from the main app scaffold.
- Extracted chart/reference UI so kana browsing and mastery visualization can evolve separately from the app shell.
- Extracted practice review UI so weak, contrast, writing, sound, speed, cross-script, and mixed queues can be refined independently.
- Extracted lesson path UI so journey, daily focus, progress, filters, and lesson nodes can be refined independently.
- Extracted shared exercise widgets so prompts, feedback, choices, audio, trace, and pair matching can be polished independently.
- Extracted common metric and stage chips so shared progress indicators stay consistent across path, lesson completion, and practice.
- Extracted settings and app shell bars so global navigation and preferences can be refined independently.
- Polished the app shell with a compact settings icon and segmented script switch for clearer global controls.
- Added focused tests for weak queue fallback, confusable options, and sound-practice fallback behavior.
- Added tested progress policy so mastered kana demote only after repeated misses while weaker kana still repair quickly.
- Persisted simple review due dates for future calendar-based spaced review.
- Surfaced due spaced-review counts in practice and included due recall-ready kana in weak review.
- Added due-review priority to the path hero and daily focus metrics so the first screen separates due recall from repair work.
- Added a compact due-kana preview to the daily focus panel before entering practice.
- Added a tested daily review intro state that explains due recall, mistake repair, or low-mastery repair before practice begins.
- Added a review-session completion state with accuracy, repaired kana, missed count, and a repeat-queue action.
- Added a tested stable-review completion action that returns to the path after clean queues while keeping repeat primary for missed queues.
- Added repaired and missed kana groups to review completion so learners can see exactly what changed in the queue.
- Added tested per-kana review session outcomes so completion distinguishes clean, repaired, and still-shaky kana.
- Added tested trace feedback cues for start, direction, and coverage, plus visible start/end markers in trace and comparison views.
- Added tested kana-specific trace overlays for high-confusion katakana such as シ/ツ and ソ/ン.
- Added adaptive trace remediation after weak checks, opening comparison and retry prompts before completing the exercise.
- Added tested lesson pacing so recognition, listening, matching, writing, and contrast prompts appear in deliberate phases.
- Added a lesson phase mix preview to the daily focus panel so learners see read, hear, match, write, and contrast counts before starting.
- Added compact lesson-node phase totals so the full path communicates each lesson's exercise load at a glance.
- Added a tested path practice recommendation that opens the recommended mode based on due review, weak kana, and lesson stage.
- Added tested practice queue explanations so weak, sound, contrast, writing, and empty fallback states are explicit.
- Added tested per-kana reason labels to practice queue intros so fallback modes show why each symbol was selected.
- Added tested practice-mode tab affordances that mark current, recommended, and fallback modes for faster scanning.
- Added tested practice queue source cues for selected-script, recommended, and cross-script practice contexts.
- Added tested practice-session goals so each queue states what a successful pass means before and during practice.
- Added reduced-motion-aware crossfade for practice-mode intent changes.
- Added a persisted, streak-free daily rhythm indicator to reward recent practice without reset pressure.
- Added a tested first-run path hint that explains why vowel anchors are the right starting point.
- Added a tested lesson-start preview that names the first exercise type and drill count before opening the runner.
- Added tested path-level resume cues after exiting a lesson mid-session.
- Added tested compact path feedback after completed lessons change the next recommended action.
- Added tested outcome-aware path action tones so next-lesson, due-review, and repair feedback read distinctly.
- Added tested compact locked-lesson copy that names the previous recall gate without adding path card height.
- Added tested path-stage progress copy so filtered lesson groups show stage fluency in the existing journey header.
- Added tested compact path-stage empty states when a filter has no actionable lesson.
- Added tested chart filter progress copy so reference browsing mirrors path-stage clarity.
- Added tested compact chart row guidance for filtered rows with no fluent kana yet.
- Added tested compact chart contrast summaries for lookalike-heavy chart views.
- Added tested chart row labels so filters and special-symbol rows avoid raw row IDs.
- Added tested compact chart-card tags that distinguish small kana and long marks.
- Added tested compact chart tap feedback when a reference card is selected for audio.
- Added a selected chart-card visual state so tapped kana stay anchored in the reference grid.
- Added tested compact chart-card mastery copy so pips have readable level labels.
- Added tested compact chart legend copy that explains fluent versus mastered.
- Added tested practice completion next-step copy for clean writing-focused queues.
- Added tested practice completion next-step copy for clean sound-focused queues.
- Added tested practice completion next-step copy for clean contrast-focused queues.
- Added tested practice completion next-step copy for clean speed-focused queues.
- Added tested practice completion next-step copy for clean cross-script queues.
- Added tested practice completion next-step copy for clean mixed-review queues.
- Added mode-aware visual tone to the practice completion next-step panel.
- Added a compact mode chip to the practice completion next-step panel.
- Added reduced-motion-aware entrance motion to the practice completion next-step panel.
- Added tested mode-aware repeat action labels to practice completion.
- Added tested compact outcome tone labels to practice completion metrics.
- Added tested compact accuracy tone copy to practice completion.
- Added tested compact repaired-versus-shaky guidance copy to practice completion.
- Added reduced-motion-aware entrance motion to practice outcome guidance.
- Added compact outcome guidance icon tones for repaired and shaky states.
- Added tested compact completion action rationale copy for return-to-path versus repeat.
- Added reduced-motion-aware entrance motion to practice action rationale.
- Added compact action icon tone for clean return-to-path completions.
- Added compact action icon tone for repeat-required completions.
- Added repeat-required primary action tone to practice completion.
- Added tested compact secondary repeat rationale for clean completions.
- Added tested compact action summary for no-attempt practice completions.
- Added tested action-rationale visibility so missed queues avoid duplicate repeat copy.
- Audited practice completion panel density and added vertical scrolling for compact screens.
- Added compact completion section dividers for long practice summaries.
- Added compact kana-group count labels to practice completion groups.
- Added overflow hint copy when practice completion groups preview only part of a long kana list.
- Added tested compact kana-group tone chips for clean, repaired, and shaky groups.
- Added compact section tones to practice completion dividers.
- Added compact action-button grouping for clean practice completions.
- Added compact action-button grouping for repeat-required practice completions.
- Added compact action group tone background for practice completion actions.
- Added reduced-motion-aware entrance motion to practice completion action groups.
- Added compact action-group labels for primary versus optional actions.
- Added compact action-group role tone for primary versus optional actions.
- Added tested compact action-group role copy for repeat-required actions.
- Added tested compact action-group role copy for optional clean repeats.
- Added compact action-role chip text overflow protection.
- Added tested compact completion action labels for narrow screens.
- Added compact completion action-button icon spacing for narrow screens.
- Added compact completion action-button touch target audit.
- Added tested compact completion action-button role ordering audit.
- Added compact completion action-button color contrast audit.
- Added tested compact completion action-button disabled-state audit.
- Added tested compact completion action-button disabled-state copy.
- Added tested compact completion action-button semantic labels.
- Added tested compact completion action-button accessibility state descriptions.
- Added compact completion action-button accessibility merge audit.
- Added tested compact completion action-button focus order audit.
- Added tested compact completion action-button accessibility disabled-announcement audit.
- Added tested compact completion action-button accessibility hint audit.
- Added compact completion action-button accessibility role audit.
- Added tested compact completion action-button accessibility action-copy audit.
- Added tested compact completion action-button accessibility disabled-hint audit.
- Added tested compact completion action-button accessibility unavailable-copy density audit.
- Added tested compact completion action-button accessibility unavailable-copy fallback audit.
- Added compact completion action-button accessibility helper text merge audit.
- Added tested compact completion action-button accessibility group summary audit.
- Added tested compact completion action-button accessibility group summary density audit.
- Added tested compact completion action-button accessibility group summary fallback audit.
- Added compact completion action-button accessibility group summary focus audit.
- Added compact completion action-button accessibility group summary role audit.
- Added compact completion action-button accessibility traversal order audit.
- Added tested compact completion action-button accessibility traversal density audit.
- Added tested compact completion action-button accessibility traversal fallback audit.
- Added compact completion action-button accessibility traversal helper naming audit.
- Added compact completion action-button accessibility traversal helper parameter audit.
- Added tested compact completion action-button accessibility traversal helper return audit.
- Added tested compact completion action-button accessibility traversal helper bounds audit.
- Added tested compact completion action-button accessibility traversal helper role-count audit.
- Added tested compact completion action-button accessibility traversal UI action-count audit.
- Added tested compact completion action-button accessibility traversal UI metadata pairing audit.
- Added compact completion action-button accessibility traversal UI metadata naming audit.
- Added tested compact completion action-button accessibility traversal UI metadata bounds audit.
- Added tested compact completion action-button accessibility traversal UI metadata ordering audit.
- Added tested compact completion action-button accessibility semantic-label metadata audit.
- Added tested compact completion action-button accessibility semantic-label metadata mode audit.
- Added tested compact completion action-button accessibility semantic-label hint metadata audit.
- Added tested compact completion action-button accessibility semantic-label hint metadata mode audit.
- Added tested compact completion action-button accessibility state-description metadata audit.
- Added tested compact completion action-button accessibility state-description metadata disabled-copy audit.
- Added tested compact completion action-button accessibility state-description metadata mode audit.
- Added tested compact completion action-button accessibility role-chip metadata audit.
- Added tested compact completion action-button accessibility role-chip metadata mode audit.
- Added tested compact completion action-button accessibility role-chip color metadata audit.
- Added tested compact completion action-button accessibility role-chip color metadata mode audit.

## Next Polish Pass

Implement next:

- Add compact completion action-button accessibility role-chip tone fallback audit.
