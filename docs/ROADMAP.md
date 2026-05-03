# Everest Focus — Product Roadmap & Monetization Plan

## Positioning

**Tagline**: "The focus timer that IS your watch face."

The only WearOS app where checking the time and checking focus progress are the same action. Zero-friction focus: tap the dial to start. No app launch, no phone required.

## Monetization Model: Freemium + One-Time Pro Unlock ($4.99)

### Tier Structure

| Feature | Free | Pro ($4.99) |
|---------|------|-------------|
| Pomodoro timer (tap to start) | Yes | Yes |
| 1 theme (Minimal) | Yes | — |
| All themes + future themes | — | Yes |
| Basic stats (today's total, sessions) | Yes | Yes |
| Weekly/monthly insights + trends | — | Yes |
| Calendar awareness | Yes | Yes |
| Adaptive breaks | — | Yes |
| Focus profiles (Deep Work, Study, Sprint) | — | Yes |
| Auto-DND on focus start | — | Yes |
| Unlimited history (vs 7-day free) | — | Yes |
| All 6 complications | — | Yes (2 free) |
| Streak tracking | Yes | Yes |
| Focus score | Yes | Yes |

### Pricing Rationale

$4.99 is above the $1.99 "throwaway" tier but below the $9.99 deliberation threshold. WearOS apps with always-on utility justify this price.

## Phase 0: Launch-Ready (2-3 weeks)

- [ ] Add `IS_PRO` billing flag to DataStore
- [ ] Integrate Google Play Billing Library (one-time non-consumable)
- [ ] Gate themes (Ember, Ocean, Monochrome) behind Pro
- [ ] Gate adaptive breaks behind Pro
- [ ] Limit free history to 7 days (Pro = 30 days)
- [ ] Limit free complications to 2 (Status + Focus Duration)
- [ ] First-run onboarding (3-screen swipe tutorial)
- [ ] Fix `runBlocking` in SettingsActivity (ANR risk)
- [ ] Privacy policy (GitHub Pages)
- [ ] Play Store listing: screenshots, feature graphic, descriptions
- [ ] Content rating questionnaire
- [ ] Data safety section declaration
- [ ] Accessibility: contentDescription on complications
- [ ] Submit to closed testing track (14-day clock)
- [ ] Crash reporting (Firebase Crashlytics)

## Phase 1: Pro Value (3-4 weeks post-launch)

- [ ] Auto-DND toggle on focus start, restore on break/idle
- [ ] Focus profiles: 3 presets (Deep Work 50min, Study 25min, Sprint 15min)
- [ ] Weekly insights screen (best day, trend, avg score chart)
- [ ] Guided breathing canvas animation during break
- [ ] 2 new themes (Forest, Neon)
- [ ] Haptic pattern options (gentle, assertive, silent)

## Phase 2: Ecosystem (2-3 months post-launch)

- [ ] Companion phone app (Jetpack Compose)
- [ ] Watch-to-phone sync (DataLayer API)
- [ ] Phone-side notification whitelist during focus
- [ ] Data export (CSV/JSON from phone app)
- [ ] Health Connect integration (log focus as mindfulness)
- [ ] Phone home screen widget

## Phase 3: Growth (4-6 months)

- [ ] Subscription tier on companion app ($2.99/mo)
- [ ] Wear OS Tiles support
- [ ] Social accountability / streak sharing
- [ ] Todoist/Notion integration
- [ ] AI coach (suggest optimal session length from patterns)
- [ ] Localization (top 5 languages)

## Revenue Projections (Conservative)

| Period | Installs/mo | Conversion | Revenue/mo |
|--------|-------------|------------|------------|
| Month 1-3 | 200-500 | 5-8% | $35-140 |
| Month 4-6 | 500-1,500 | 8-12% | $140-630 |
| Month 7-12 | 1,500-3,000 | 10-15% | $525-1,575 |
| Year 2 | 3,000-5,000 | 12-15% | $1,260-2,625 |

**Year 1 cumulative: $4,000-$12,000**
**Year 2 cumulative: $15,000-$31,500**

## Competitive Landscape

| App | Watch Face? | Intelligence? | Friction |
|-----|-------------|---------------|----------|
| Forest | No (app only) | No | High (open app) |
| Focus Timer | No | No | High |
| Tide | No WearOS | Sounds | N/A |
| **Everest Focus** | **Yes** | **Adaptive + Calendar** | **One tap** |
