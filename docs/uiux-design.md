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

Material 3 Expressive should show up through:

- Confident shape: rounded lesson nodes, pill chips, large touch targets.
- Lively but purposeful motion: progress changes, screen transitions, selected states, and correct/incorrect feedback.
- Clear hierarchy: large kana prompts, medium lesson titles, small status labels.
- Color roles with contrast: primary for action, tertiary for contrast/confusable work, error/coral for misses.
- Accessible flow: readable text, stable layout, no hidden critical state behind color alone.

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

## First Polish Pass

Implement next:

- Stage chips and difficulty dots on lesson cards.
- Locked lesson state based on previous lesson mastery.
- More expressive hero status.
- Animated lesson card sizing and progress.
- Stronger mistake/contrast visual language.

