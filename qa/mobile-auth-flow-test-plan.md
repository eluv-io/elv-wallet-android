# Mobile Sign-In — Manual QA Test Plan

A step-by-step guide for testing sign-in, sign-out, and everything around them on the
**mobile phone app**. No coding or developer tools are needed — just the app on a phone (or
emulator) and the ability to log in with real test credentials.

Work through each test case in order. For every case, do the steps and check that what you see
matches the **Expected result**. Record PASS / FAIL and any notes in the results table at the
bottom. If something looks even slightly off (wrong screen, flicker, frozen spinner, unexpected
text), write it down.

---

## What you need before you start

- The mobile app installed and open on a phone or emulator.
- A working set of **test login credentials** (username/password or whatever the login page
  asks for).
- A way to set the phone's **default browser** (Android **Settings → Apps → Default apps →
  Browser app**). Some tests ask you to switch it. Install **three** browsers so you can cover all
  the paths: **Chrome** and **Firefox** (which support an in-app sign-in window), and **Opera
  Mini** (which does **not** — it always opens the full browser app). Opera Mini is free on the
  Play Store.
- A reliable internet connection (and the ability to turn it off for one test).

---

## Key things to know about how sign-in looks

You don't sign in from a dedicated "login screen." Instead:

1. You start on the app's main browse screen (Discover).
2. When you tap a property that requires you to be signed in, the app **dims the screen**
   (a dark see-through overlay) and shows a **spinning loading circle** in the middle.
3. A **sign-in web page** then opens so you can enter your credentials. Depending on the phone's
   default browser, this appears in one of **two visually different ways** — a small in-app sign-in
   window, or a switch to the full browser app (see *"Two ways the sign-in page can look"* below).
   **Both are correct** — the important thing is that a sign-in page appears and that finishing it
   brings you back to the app.
4. After you finish signing in, you're returned to the app. The dark overlay + spinner appears
   again briefly, and the word **"Almost done"** shows under the spinner for a moment.
5. You then land on the **property's detail page** — the thing you originally tapped.

If you back out of the sign-in page without finishing, you should be returned to the browse
screen you started from, with no changes.

### Two ways the sign-in page can look

**(a) In-app sign-in window** — used by browsers like **Chrome** and **Firefox**.
- A panel **slides up on top of** the wallet app; you **don't fully switch** to another app.
- The top bar is **minimal**: the page title, a small **lock** icon, an **✕ (close)** button, and a
  **⋮** menu — but **no editable address bar and no browser tabs**.
- It uses a **dark color theme** (set by the app).
- Closing it with the **✕** drops you straight back into the wallet app.

**(b) Full browser app** — used by browsers like **Opera Mini**.
- The phone **switches to the separate browser app**; the wallet app goes to the background and
  appears as its **own separate entry** in the recent-apps switcher.
- You see the **full browser interface**: an **address bar** with the page URL, **tabs**, and
  browser menus. It is **not** dark-themed by the app.
- You return to the wallet app with the system **Back** button or the app switcher.

Either look is correct. What must be **identical** either way is the outcome: finishing sign-in
returns you to the app and the property detail page, and backing out returns you to the browse
screen.

> **Important — test with more than one browser.** The sign-in page looks different depending on
> the default browser (in-app window vs. full browser app — see above), but the **end result must
> be identical**. In **every** case the app works out whether you *finished* or *backed out* by
> watching for your return to the app — so the cancel/return checks (C1, C3, F2, and Section G)
> matter for **all** browsers, not just Opera Mini.
> Please run the core tests (Section B) once with **Chrome** as default and once with **Firefox**
> as default, then run **Section G** with **Opera Mini** as default. Opera Mini is the clearest
> case to watch (you fully leave the app), but the same return-detection is happening behind the
> in-app window too.

---

## Section A — Getting to sign-in

### A1 — Tapping a property that requires sign-in
**Pre:** You are signed out (if unsure, do test E1 first to sign out).
**Steps:**
1. From the main browse screen, tap a property tile that requires login.
**Expected result:**
- The screen dims with a dark see-through overlay and a spinning circle appears in the center.
- A sign-in web page opens (either an in-app sign-in window on top of the app, or the full
  browser app).

### A2 — Tapping a property you're already signed in for
**Pre:** You have already signed in for this property (e.g. just completed B1).
**Steps:**
1. Go back to the browse screen and tap the **same** property again.
**Expected result:**
- No sign-in page appears. You go **straight** to the property's detail page.

### A3 — Tapping a property that doesn't need sign-in
**Pre:** Find a property that is open / free to view (ask the team which test property qualifies).
**Steps:**
1. Tap that property.
**Expected result:**
- No sign-in page appears; you go straight to its detail page, even if signed out.

---

## Section B — Completing sign-in successfully (run with Chrome, then with Firefox)

### B1 — Normal successful sign-in
**Pre:** Signed out. Tap a property that requires login (as in A1).
**Steps:**
1. On the sign-in page, enter valid test credentials and complete the login.
**Expected result:**
- You are returned to the app automatically.
- The dark overlay + spinner reappears briefly, and **"Almost done"** shows under the spinner
  for a moment.
- You then land on the **detail page** of the property you originally tapped.
- No error messages, no stuck spinner, no blank screen.

### B2 — Sign-in window appearance check
**Steps:**
1. While the sign-in page is open during B1, note **how** it appeared.
**Expected result:** (compare against *"Two ways the sign-in page can look"* near the top)
- With Chrome or Firefox as default: an **in-app sign-in window** opens on top of the app (you
  don't fully leave the app) — minimal top bar, no address bar or tabs, **dark color theme**.
- With Opera Mini as default: the phone switches to the **full Opera Mini browser app** — full
  address bar and tabs, no in-app window. This is expected, and is covered in detail in Section G.
- In all cases, completing or closing it brings you back to the app correctly.

### B3 — "Almost done" appears only after credentials are accepted
**Steps:**
1. Watch the spinner carefully right after you finish entering credentials.
**Expected result:**
- "Almost done" appears only **after** you've completed the login and are sent back to the app —
  not while you're still typing on the sign-in page.

---

## Section C — Cancelling / backing out of sign-in

### C1 — Backing out of the sign-in page
**Pre:** Start a sign-in (A1) so the sign-in page is open.
**Steps:**
1. Without entering credentials, dismiss the sign-in page — press the device **Back** button,
   close the sign-in window (the X or swipe it away), or return to the app.
**Expected result:**
- You are returned to the **browse screen** you started from.
- The dark overlay and spinner are gone. You are **not** signed in. No error message.

### C2 — Cancel, then try again
**Pre:** You just did C1 (cancelled).
**Steps:**
1. Tap the same property again to start sign-in again.
2. This time, complete the login with valid credentials.
**Expected result:**
- A fresh sign-in page opens normally.
- Sign-in completes and you land on the property's detail page — exactly as in B1.
  (Cancelling once must not break the next attempt.)

### C3 — Successful sign-in is never mistaken for a cancel
**Steps:**
1. Do a normal successful sign-in (B1) and watch closely as you return to the app.
**Expected result:**
- You proceed cleanly to the property detail page.
- You are **not** bounced back to the browse screen, and you do **not** see two competing
  outcomes (e.g. flashing back to browse and then forward again). Exactly one result: success.

---

## Section D — Switching between properties / providers

> Some properties may use different sign-in systems ("providers"). The app treats being signed in
> for one provider as separate from another.

### D1 — Property using a different sign-in system
**Pre:** Signed in for Property A (a property using one login system).
**Steps:**
1. Tap Property B, which uses a **different** login system than A (ask the team which test
   properties qualify).
**Expected result:**
- Even though you're already signed in for A, the sign-in page **does** appear for B (because
  it's a different login system).
- Completing it lands you on Property B's detail page.

### D2 — Returning to the first property still works
**Pre:** You've signed in for both A and B (D1 done).
**Steps:**
1. Go back and tap Property A again.
**Expected result:**
- No sign-in needed; you go straight to Property A's detail page.

---

## Section E — Signing out

### E1 — Sign out with confirmation
**Pre:** Signed in.
**Steps:**
1. Go to your profile and tap **Sign out**.
**Expected result:**
- A confirmation dialog appears titled **"Sign out?"** with the message
  **"You'll need to sign in again to access your items."** and a **Sign out** button.

### E2 — Confirm sign out
**Steps:**
1. In the dialog from E1, tap **Sign out**.
**Expected result:**
- You are signed out. Tapping a property that requires login now shows the sign-in page again
  (confirm with A1).

### E3 — Cancel sign out
**Pre:** Signed in.
**Steps:**
1. Tap **Sign out**, then dismiss the confirmation dialog (tap outside it or press Back / Cancel).
**Expected result:**
- The dialog closes and you remain **signed in**. Nothing changes.

### E4 — Sign out, then sign back in
**Steps:**
1. Sign out (E2).
2. Tap a property that requires login and complete sign-in again.
**Expected result:**
- Sign-in works normally and you land on the property detail page.

---

## Section F — Interruptions and edge cases

### F1 — Rotating the phone during sign-in
**Pre:** Sign-in page is open (A1).
**Steps:**
1. Rotate the phone (portrait ↔ landscape) while the sign-in page is open, then finish signing in.
**Expected result:**
- The app does not open a second sign-in page or restart the flow. Sign-in completes once and
  lands you on the property detail page.
- (If the device or app is locked to one orientation, note that and skip.)

### F2 — Leaving and returning to the app mid sign-in
**Pre:** Sign-in page is open (A1).
**Steps:**
1. Switch to another app (e.g. open the recent-apps switcher and pick something else), then come
   back to the wallet app **without** finishing sign-in.
**Expected result:**
- Reasonable behavior: either the sign-in page is still available to finish, or you're treated as
  having cancelled and are returned to the browse screen. **No crash, no permanent stuck spinner.**
  Note exactly what happens.

### F3 — No internet connection
**Pre:** Turn the phone's internet off (airplane mode).
**Steps:**
1. Tap a property that requires login.
**Expected result:**
- The app does not crash or hang forever. Note what happens (error message, the sign-in page
  failing to load, returning to browse, etc.). Turn internet back on afterward.

### F4 — No usable browser on the device
**Pre:** Only attempt if you can disable all browsers (advanced; coordinate with the team — skip
if unsure).
**Steps:**
1. With no browser available, tap a property that requires login.
**Expected result:**
- The app does **not** crash. It returns you to the browse screen (no sign-in possible). Re-enable
  the browser afterward.

### F5 — Closing the app entirely during sign-in
**Pre:** Sign-in page is open (A1).
**Steps:**
1. Fully close the app (swipe it away from recent apps), then reopen it.
**Expected result:**
- The app reopens without crashing. Note whether you're back on the browse screen and whether you
  can start sign-in cleanly again.

---

## Section G — Sign-in with a no-window browser (Opera Mini)

Some browsers — like **Opera Mini** — don't support the small in-app sign-in window. With one of
these set as the default browser, the app opens the **full browser app** for sign-in: the phone
**switches away** to the browser (full address bar and tabs, **no** dark in-app panel), and you come
back to the wallet app afterward.

The app works out whether you finished or backed out by **noticing your return** to the app. It does
this for the in-app window too, but it's **most visible here** — because you fully leave the app —
which is why these cases are worth checking carefully.

**Pre for this whole section:** Set the phone's default browser to **Opera Mini** (Android
**Settings → Apps → Default apps → Browser app**). Keep Chrome and Firefox installed but not set
as default.

### G1 — Successful sign-in via the full browser
**Pre:** Signed out, Opera Mini set as default. Tap a property that requires login (as in A1).
**Steps:**
1. The phone switches to the **full Opera Mini browser** showing the sign-in page.
2. Enter valid test credentials and complete the login.
**Expected result:**
- The phone returns to the wallet app **on its own** after login (you don't have to switch back
  manually).
- The dark overlay + spinner reappears briefly with **"Almost done"**, then you land on the
  **detail page** of the property you tapped.
- No stuck spinner, no blank screen, and you are not left sitting in the browser.

### G2 — Backing out of the full browser
**Pre:** Start a sign-in (G1) so Opera Mini is open on the sign-in page.
**Steps:**
1. Without signing in, press the device **Back** button to return to the wallet app.
**Expected result:**
- You are returned to the **browse screen** you started from, with no overlay or spinner.
- You are **not** signed in, and there is no error.

### G3 — Switching away and back without finishing
**Pre:** Start a sign-in (G1) so Opera Mini is open on the sign-in page.
**Steps:**
1. Open the recent-apps switcher and switch to a different app, then come back to the wallet app
   **without** completing sign-in.
**Expected result:**
- No crash and no permanently stuck spinner. Either the browse screen is shown (treated as
  cancelled) or you can still finish sign-in. Note exactly what happens.

### G4 — Successful sign-in is not mistaken for backing out
**Pre:** Opera Mini set as default.
**Steps:**
1. Do a full successful sign-in via Opera Mini (G1) and watch closely as the app comes back to the
   foreground.
**Expected result:**
- You land on the property detail page exactly once.
- You are **not** bounced back to the browse screen, and you don't see it flash to browse and then
  forward again. Exactly one outcome: success.

---

## Results log

| Test | Date | Device / default browser | PASS / FAIL | Notes |
|------|------|--------------------------|-------------|-------|
| A1 | | | | |
| A2 | | | | |
| A3 | | | | |
| B1 (Chrome) | | | | |
| B1 (Firefox) | | | | |
| B2 | | | | |
| B3 | | | | |
| C1 | | | | |
| C2 | | | | |
| C3 | | | | |
| D1 | | | | |
| D2 | | | | |
| E1 | | | | |
| E2 | | | | |
| E3 | | | | |
| E4 | | | | |
| F1 | | | | |
| F2 | | | | |
| F3 | | | | |
| F4 | | | | |
| F5 | | | | |
| G1 (Opera Mini) | | | | |
| G2 (Opera Mini) | | | | |
| G3 (Opera Mini) | | | | |
| G4 (Opera Mini) | | | | |

## When you're done
- Make sure internet is turned back on and any disabled browser is re-enabled.
- Sign back in (or out) to leave the app in whatever state the team asked for.
- Hand the filled-in results table back to the team, with screenshots/screen recordings for any
  FAIL or anything that looked off.
