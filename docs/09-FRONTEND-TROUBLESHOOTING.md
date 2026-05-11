# 🖥️ VinUSBank Frontend Troubleshooting Log

This document tracks all the specific Angular, TypeScript, and UI-layer errors we encounter during the frontend development of VinUSBank, documenting exactly *why* they happened, *how* we diagnosed them, and the *exact steps* we took to fix them.

---

## Error 1: Angular Blank Screen - `NG04003: No base href set`

**Symptom:**
When navigating to `http://localhost:4200`, the screen was completely blank dark (only the CSS background applied). No login form was rendered, and no API calls were made.

**Why it happened:**
Angular relies on its powerful internal Router for single-page application navigation. In modern Angular versions, the Router strictly demands a `<base href="/">` HTML tag located in the `<head>` of the `index.html` file to compose URLs. If this tag is missing, the Angular application throws a fatal `NG04003` unhandled exception under the hood, ceasing to bootstrap `main.ts` or render the `<app-root>`.

**How we diagnosed it:**
1. The CSS was clearly loaded (as the dark background rendered), meaning `styles.css` was executing and the basic web server worked.
2. We verified the Angular compiler had 0 build errors.
3. We checked `index.html` and noticed the `<base href="/">` element was completely omitted.

**The Solution:**
We explicitly added the missing base reference inside `frontend/src/index.html`:
```html
<head>
  <meta charset="utf-8">
  <base href="/">
  <title>VinUSBank — Secure Digital Banking</title>
  ...
```

---

## Error 2: Angular Blank Screen - `RuntimeError: NG0908: Angular requires Zone.js`

**Symptom:**
After fixing the `base href` issue, the login page *still* failed to render. The browser console logged: `RuntimeError: NG0908: In this configuration Angular requires Zone.js in console`.

**Why it happened:**
By default, standard Angular uses `zone.js` to fuel its change detection mechanism (tracking events like clicks, HTTP responses, or timers to know when to redraw the screen). The application's `app.config.ts` was explicitly configured to use `provideZoneChangeDetection()`, but the `zone.js` library itself was missing from the project's dependencies (`package.json`) and was not imported on startup, leading to an immediate crash.

**How we diagnosed it:**
1. The error `NG0908` is uniquely specific and explicitly states `Zone.js` is missing.
2. We checked the frontend `package.json` and indeed, `zone.js` was entirely missing from both `dependencies` and `devDependencies`.

**The Solution:**
We installed the library natively and forcefully loaded it before Angular initialized:
1. Ran `npm install zone.js` to download the package.
2. Added `import 'zone.js';` to the very top of `frontend/src/main.ts` so it polyfills the environment before `bootstrapApplication` fires.
