import json
import time
from datetime import datetime
from prayertimes import get_prayer_times
from notifier import send_notification


# Load storage file
with open("storage.json", "r") as f:
    storage = json.load(f)


send_notification("Namaz reminder is now active...")
print("Namaz bot started...")


# Get prayer times once when the bot starts
prayer_times_today, hijri_month = get_prayer_times()
last_fetch_day = datetime.now().date()


while True:

    now = datetime.now()
    today = now.date()

    # If a new day starts → fetch prayer times again
    if today != last_fetch_day:
        prayer_times_today, hijri_month = get_prayer_times()
        last_fetch_day = today
        storage = {}

        with open("storage.json", "w") as f:
            json.dump(storage, f, indent=4)

    current_time = now.strftime("%I:%M %p")

    for prayer, prayer_time in prayer_times_today.items():

        today_str = str(today)

        if today_str not in storage:
            storage[today_str] = []

        if prayer_time == current_time and prayer not in storage[today_str]:

            send_notification(f"It's time for {prayer}")

            storage[today_str].append(prayer)

            with open("storage.json", "w") as f:
                json.dump(storage, f, indent=4)

    time.sleep(30)