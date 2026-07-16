Namaz Reminder

This repo now has three layers:

- `Python prototype` in the root files
- `Web preview` in `web/` so the full app flow can be tested in the browser now
- `Android app scaffold` in `android-app/` for the real phone app and widget later

Quick start for the browser preview:

1. Run `python3 -m http.server 8000`
2. Open `http://localhost:8000/web/`

Masjid finder backend:

1. Run `python3 api_server.py`
2. Test `http://127.0.0.1:8010/health`
3. Query `http://127.0.0.1:8010/api/nearest-masjid?lat=40.9364&lon=-74.1767`

Current backend files:

- [api_server.py](/Users/hussain/Desktop/Namaz reminder/api_server.py)
- [masjid_finder.py](/Users/hussain/Desktop/Namaz reminder/masjid_finder.py)

The browser version shows:

- saved home coordinates
- device location option
- today’s prayer times
- next prayer countdown
- browser reminder preview
- widget-style preview card
