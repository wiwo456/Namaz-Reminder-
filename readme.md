Namaz Reminder

This repo now has three layers:

- `Python prototype` in the root files
- `Web preview` in `web/` so the full app flow can be tested in the browser now
- `Android app scaffold` in `android-app/` for the real phone app and widget later

Quick start for the browser preview:

1. Run `python3 -m http.server 8000`
2. Open `http://localhost:8000/web/`

The browser version shows:

- saved home coordinates
- device location option
- today’s prayer times
- next prayer countdown
- browser reminder preview
- widget-style preview card

