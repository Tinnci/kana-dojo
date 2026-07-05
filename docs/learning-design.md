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

Implementation note: mastered kana are kept at level `5` after the first miss and demoted only after a repeated miss streak. Other non-mastered kana demote immediately on a miss. Any miss enters the mistake queue; correct answers clear the miss streak and leave the mistake queue once the kana reaches recall level `2`. Review due dates are stored as epoch days using a simple delay ladder: immediate for new/familiar or missed kana, then 1, 3, 7, and 14 days as recall reaches levels `2..5`.

Speed implementation note: speed practice targets fluent kana and treats correct answers over the pace target as repeats. The goal is automatic recall, so a slow correct answer is not framed as a wrong symbol, but it still returns to the queue.

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

### Stage 7: Combination Kana

Teach small `ゃ/ゅ/ょ` combinations after voiced kana. These should be learned as blended sounds rather than as two separate kana.

- k/s blends: kya kyu kyo, sha shu sho
- t/n blends: cha chu cho, nya nyu nyo
- h/m blends: hya hyu hyo, mya myu myo
- r/g blends: rya ryu ryo, gya gyu gyo
- j/b/p blends: ja ju jo, bya byu byo, pya pyu pyo
- Exercises: listen-first choice, romaji recall, trace with attention to the smaller second kana
- Goal: read common syllable blends without mentally spelling them out

### Stage 8: Special Small Kana

Teach special small kana and the katakana long vowel mark as visual/usage symbols. Do not rely on standalone audio prompts for these symbols because several of them only make sense inside words.

- Hiragana: small `っ`, `ゃ`, `ゅ`, `ょ`
- Katakana: small `ッ`, `ァ`, `ィ`, `ゥ`, `ェ`, `ォ`, `ャ`, `ュ`, `ョ`, and long vowel mark `ー`
- Exercises: kana recognition, romaji/label recall, pair matching, trace every symbol
- Goal: recognize size-sensitive kana and length marks inside real words

### Stage 9: Mixed Fluency

Mix all learned kana inside one script first, then mix hiragana and katakana later.

- Script-local review: hiragana only or katakana only
- Cross-script review: あ vs ア, か vs カ, and so on
- Exercises: rapid recognition, pair match, trace only for weak symbols
- Goal: automatic recall without relying on lesson order

## Lesson Grouping

Use twenty-one lessons per script: ten base-kana lessons, five voiced-kana lessons, five combination-kana lessons, then one special-symbol lesson.

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
| 16 | k/s blends | 6 | first small-y combinations |
| 17 | t/n blends | 6 | includes `cha/chu/cho` |
| 18 | h/m blends | 6 | shape-heavy combinations |
| 19 | r/g blends | 6 | mixes base and voiced blends |
| 20 | j/b/p blends | 9 | densest combination lesson |
| 21 | special symbols | varies | small kana and katakana length mark |

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

Writing implementation note: trace practice is guided shape scoring, not full handwriting recognition. It should check meaningful coverage, direction changes, and kana-specific checkpoints where available. If the app later adds ML Kit Digital Ink or another handwriting recognizer, the UI should present that as recognition with model download, language support, confidence, and fallback states instead of silently replacing the guided-trace score.

## Session Structure

A good short session is 3 to 5 minutes:

- 1 warm-up review from previous mistakes.
- 3 to 5 new kana or one row.
- 8 to 14 recognition prompts.
- 1 pair matching prompt.
- 2 to 3 trace prompts.
- 1 final mistake replay.

Do not add kanji until both hiragana and katakana base tables are stable. Kana fluency should be a separate milestone.
