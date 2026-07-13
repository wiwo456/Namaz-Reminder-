PWA-first web app for the Namaz Reminder project.

How to run:

1. Open a terminal in the project root.
2. Run `python3 -m http.server 8000`.
3. Visit `http://localhost:8000/web/`.

How to deploy on Vercel:

1. Import this repo into Vercel.
2. Keep the project root as the repo root.
3. Vercel will use `vercel.json` to serve the `web/` folder as the site root.
4. Open the generated `https://...vercel.app` URL on Android Chrome.
5. Use Chrome menu `Install app` after the first load/refresh.

What this PWA does:
- Splits the experience into three blocks: design, implementation, and final product
- Shows the phone-style prayer app flow in the browser
- Uses saved home coordinates or browser location
- Stores settings in local browser storage
- Highlights the next prayer and shows a live countdown
- Can trigger browser notifications while the page remains open
- Registers a service worker and manifest for installability/offline caching

How to install on a phone:

1. Serve the `web/` folder over HTTP or HTTPS.
2. Open the site in Chrome on Android or Safari on iPhone.
3. Use the in-app install button when available, or the browser's `Add to Home Screen` option.

What this PWA does not do yet:
- Install a real Android home screen widget
- Run background updates like a native Android alarm app when the browser is fully closed

Backend note:
- The current PWA does not require the Python files in the repo to run on Vercel.
- If you later want the deployed Vercel frontend to call a backend running on your Mac, that backend must be exposed on a public `https://` URL and allow CORS from your Vercel domain.
- A local-only address such as `localhost` or `192.168.x.x` will not work for public users visiting the Vercel site.
