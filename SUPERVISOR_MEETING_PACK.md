# Supervisor Meeting Pack

Meeting target:
- **Wednesday, April 22, 2026**

This file is the short pack for the supervisor meeting. It keeps the product story, demo order, and next-step queue aligned with the current EzBoost build.

## 1. Demo Flow
Use one clean account and one stable dataset.

Demo order:
1. Open `DataImport`
2. Show monthly preview flow
3. Show room preview flow
4. Show thresholds and readiness
5. Open `Multiplier Settings`
6. Open `Segment Settings`
7. Open `Event Settings`
8. Run `BoostMe`
9. Show monthly forecast
10. Show export

What to emphasize during the demo:
- imports are previewed before commit
- readiness is visible before optimization
- multiplier and segment screens are now easier to interpret
- events are clearly separated by source and affect the forecast visibly
- BoostMe now explains the optimization outcome instead of only dumping prices

## 2. Talk Track
Use this exact explanation if needed:

Monthly history is used to learn the hotel's demand timing. It supports dynamic season classification and helps fit a demand curve when there is enough valid historical data.

Room inventory defines what the optimizer can actually price. It gives the system room counts, base ADR, minimum ADR, and maximum ADR, which become the pricing search constraints.

The genetic algorithm optimizes **seasonal** prices per room type, not month-by-month prices. After that, the forecast maps each future month to a historically learned base season, then applies event overrides that can bump the month upward in season intensity.

So the current model is **seasonal GA with monthly mapping**, which matches the intended thesis scope for now.

## 3. One-Sentence Technical Notes
### Fallback demand curve
If historical price-demand fitting is weak or insufficient, EzBoost uses a default negative linear curve so the GA still behaves like a demand-sensitive optimizer instead of assuming price has no demand effect.

### Forecast scope
The forecast is not a fresh monthly re-optimization; it is the seasonal GA result mapped into months, then adjusted upward when configured events apply.

### Data trust improvement
User data is more isolated than before, and imports are previewed before commit so the hotel manager can inspect what will be saved.

## 4. Honest Limitation Statement
Use this if the supervisor asks what still needs work:

The biggest remaining engineering gap is that `BoostMe.jsp` is still scriptlet-heavy and the project still needs stronger integration-style coverage around the full import -> optimize -> export flow.

## 5. Immediate Post-Meeting Queue
Do not widen scope unless the supervisor explicitly asks for it.

Immediate queue:
1. Move display-prep logic out of `BoostMe.jsp`
2. Add integration-style tests for:
   - import preview -> commit
   - optimize -> result render
   - export after optimization
   - event override impact on monthly forecast
3. Do one final frontend polish pass only for issues found during manual rehearsal

Deferred unless requested:
- month-by-month re-optimization
- new forecasting model work
- deeper multiplier capability expansion

## 6. Manual Rehearsal Checklist
Verify these states before the meeting:

- `DataImport` shows a sensible empty or import-required state
- monthly preview renders clearly
- room preview renders clearly
- thresholds look readable and explained
- multiplier live preview updates correctly
- segment table is readable and not cramped
- event fetch/manual add flow behaves clearly
- BoostMe result layout is readable
- monthly forecast clearly shows event-adjusted months
- export works after an optimization run
- navigation still routes unready users to import-first flows where intended

## 7. Assets To Bring
Screenshots already available:
- `C:\Users\qwibb\Downloads\EzBoost-main\enhance\import previews.png`
- `C:\Users\qwibb\Downloads\EzBoost-main\enhance\events.png`

Recommended supporting files:
- `IMPROVEMENT_PLAN.md`
- this meeting pack
