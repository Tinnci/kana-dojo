# Kana Dojo Learning Design

Kana Dojo should teach kana through a short, repeatable loop:

1. Recognize the symbol.
2. Hear the sound.
3. Match sound and symbol in both directions.
4. Write the shape.
5. Review mistakes until recall is stable.

The app should avoid teaching kana as Chinese-looking characters. Chinese literacy helps with visual memory, but kana fluency needs direct symbol-to-sound recall.

## Difficulty Model

Use five mastery levels per kana. The existing app already stores mastery as `0..5`, so difficulty can build on that instead of adding a separate progress system.

| Level | Name | Goal | Exercise Mix | Promotion Gate |
| --- | --- | --- | --- | --- |
| 0 | New | First exposure | hear, kana -> romaji, romaji -> kana | 2 correct answers |
| 1 | Familiar | Basic recognition | 60% choice, 20% pair match, 20% trace | 3 correct, no recent miss |
| 2 | Recall | Faster recall | timed choice, pair match, trace | 4 correct across 2 sessions |
| 3 | Confusable | Separate lookalikes | contrast drills, minimal pairs, trace | beat confusable set twice |
| 4 | Fluent | Low-friction recall | mixed review, no hints | 5 correct over spaced reviews |
| 5 | Mastered | Maintenance | rare spaced review | demote on repeated miss |

Mistakes should be heavier than new content. A missed kana should reappear once immediately, once near the end of the lesson, and once in the next review session.

## Learning Curve

### Stage 1: Anchor Sounds

Teach vowels first because they are the pronunciation base for every row.

- Hiragana: あ い う え お
- Katakana: ア イ ウ エ オ
- Exercises: hear, choose kana, choose romaji, trace large shape
- Goal: associate each vowel sound directly with the kana

### Stage 2: Regular Rows

Teach rows with regular consonant-vowel structure. Keep each lesson to one row.

- k row: ka ki ku ke ko
- s row: sa shi su se so
- t row: ta chi tsu te to
- n row: na ni nu ne no
- Exercises: bidirectional choice, pair matching, row chart recall
- Goal: understand row rhythm while noticing irregular sounds like `shi`, `chi`, and `tsu`

### Stage 3: Shape-Heavy Rows

Introduce rows where shapes are more visually dense or similar.

- h row: ha hi fu he ho
- m row: ma mi mu me mo
- y row: ya yu yo
- Exercises: trace, visual discrimination, delayed recall
- Goal: reduce visual confusion before adding more symbols

### Stage 4: Tail Rows

Teach the remaining base kana with special usage notes.

- r row: ra ri ru re ro
- w/n group: wa wo n
- Exercises: mixed choice, pair match, listening, trace
- Goal: finish base kana and prepare for full-table review

### Stage 5: Confusable Sets

Run focused contrast drills. These should appear only after the learner has seen both symbols.

- Hiragana: さ / ち, ぬ / め
- Katakana: シ / ツ, ソ / ン, シ / ン
- Exercises: two-option speed rounds, trace comparison, mistake review
- Goal: force attention to stroke direction and proportion

### Stage 6: Voiced Kana

Add dakuten and handakuten after the base table is stable. These should feel like a transformation of known rows, not a completely separate alphabet.

- g row: ga gi gu ge go
- z row: za ji zu ze zo
- d row: da ji zu de do
- b row: ba bi bu be bo
- p row: pa pi pu pe po
- Exercises: listen-first choice, romaji recall, trace every marked kana
- Goal: recognize sound shifts from marks while separating duplicate romaji pairs like `じ/ぢ` and `ず/づ`

### Stage 7: Mixed Fluency

Mix all learned kana inside one script first, then mix hiragana and katakana later.

- Script-local review: hiragana only or katakana only
- Cross-script review: あ vs ア, か vs カ, and so on
- Exercises: rapid recognition, pair match, trace only for weak symbols
- Goal: automatic recall without relying on lesson order

## Lesson Grouping

Use fifteen lessons per script: ten base-kana lessons, then five voiced-kana lessons.

| Lesson | Group | Kana Count | Difficulty Note |
| --- | --- | ---: | --- |
| 1 | vowels | 5 | easiest anchor group |
| 2 | k row | 5 | regular row |
| 3 | s row | 5 | includes `shi`; add confusable review |
| 4 | t row | 5 | includes `chi` and `tsu` |
| 5 | n row | 5 | hiragana `nu` can confuse with `me` later |
| 6 | h row | 5 | includes `fu`; shape practice matters |
| 7 | m row | 5 | add `nu/me` review after this row |
| 8 | y row | 3 | shorter confidence lesson |
| 9 | r row | 5 | sound is hard for English speakers; keep audio frequent |
| 10 | w/n group | 3 | special symbols `wo` and standalone `n` |
| 11 | g row | 5 | first dakuten transformation |
| 12 | z row | 5 | includes `ji` and `zu` |
| 13 | d row | 5 | contrasts `ぢ/づ` or `ヂ/ヅ` with z-row kana |
| 14 | b row | 5 | voiced h-row transformation |
| 15 | p row | 5 | handakuten transformation |

The app should unlock the next lesson when the current lesson average mastery reaches `2`, but it should keep weak kana visible in mistakes until they reach `4`.

## Exercise Types

Use these exercise types in increasing difficulty:

- Listen and pick kana: easiest, good for first exposure.
- Kana to romaji: basic recognition.
- Romaji to kana: harder recall.
- Pair matching: medium difficulty, good for row-level memory.
- Trace kana: shape practice; required for every new kana at least once.
- Confusable contrast: only after both symbols have been introduced.
- Mixed review: only uses kana at mastery `2+`.
- Speed round: only uses kana at mastery `3+`.

## Session Structure

A good short session is 3 to 5 minutes:

- 1 warm-up review from previous mistakes.
- 3 to 5 new kana or one row.
- 8 to 14 recognition prompts.
- 1 pair matching prompt.
- 2 to 3 trace prompts.
- 1 final mistake replay.

Do not add kanji until both hiragana and katakana base tables are stable. Kana fluency should be a separate milestone.
