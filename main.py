import requests
import json
import time
from datetime import datetime, timedelta
from prayertimes import prayer_times
from notifier import send_notification


# Load storage file
with open("storage.json", "r") as f:
    storage = json.load(f)

# Load config
with open("config.json", "r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config["lon"]
    user_key = config["user_key"]
    api_key = config["api_key"]


send_notification("Namaz reminder is now active...")
print("Namaz bot started...")


ramadan_end = datetime(2026, 3, 19).date()


while True:

    now = datetime.now()
    today = now.date()

    # Reset storage every midnight
    if now.hour == 0 and now.minute == 0:
        storage["prayers_sent"] = []
        with open("storage.json", "w") as f:
            json.dump(storage, f, indent=4)

    # Get prayer times
    times = prayer_times(lat, lon)

    current_time = now.strftime("%H:%M")

    for prayer, prayer_time in times.items():

        if prayer_time == current_time:

            today_str = str(today)

            if today_str not in storage:
                storage[today_str] = []

            if prayer not in storage[today_str]:

                send_notification(f"It's time for {prayer}")

                storage[today_str].append(prayer)

                with open("storage.json", "w") as f:
                    json.dump(storage, f, indent=4)

    time.sleep(30)