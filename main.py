import requests
import json
import time
from datetime import datetime
from datetime import timedelta
from prayertimes import prayer_times
from notifier import send_notification


# Load storage
with open("storage.json", "r") as f:
    storage = json.load(f)


# Load config
with open("config.json", "r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config["lon"]
    user_key = config["user_key"]
    api_key = config["api_key"]


# Start notification
send_notification("Namaz reminder is now active...")


print(prayer_times)


ramadan_end = datetime(2026, 3, 19).date()


while True:

    now = datetime.now()
    today = now.date()

    # Reset storage after midnight
    if now.hour == 0 and now.minute < 2:
        storage["prayers_sent"] = []

        with open("storage.json", "w") as f:
            json.dump(storage, f, indent=4)


    current_time = now.strftime("%I:%M %p")

    print(f"the date is {today} and hour is {current_time}")



    if today <= ramadan_end:

        maghrib_str = prayer_times["maghrib"]

        maghrib_conv = datetime.strptime(maghrib_str, "%I:%M %p")

        iftar_warning = maghrib_conv - timedelta(minutes=5)

        iftar_warning_str = iftar_warning.strftime("%I:%M %p")

        if current_time == iftar_warning_str:
            send_notification("🌙 Iftar is in 5 minutes")

    for prayer_name, prayer_time in prayer_times.items():

        if current_time == prayer_time and prayer_name not in storage["prayers_sent"]:

            send_notification(f"🕌 It is time for {prayer_name}")

            storage["prayers_sent"].append(prayer_name)

            with open("storage.json", "w") as f:
                json.dump(storage, f, indent=4)


    time.sleep(60)