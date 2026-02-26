import requests 
import json
import time  
from datetime import datetime
from datetime import timedelta
from prayertimes import prayer_times


with open ("config.json", "r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config ["lon"]

print("This is a namaz and ftar time reminding bot.") 
print(prayer_times)

ramadan_end = datetime(2026, 3, 19).date()

while True:
    today = datetime.now().date()
    current_time = datetime.now().strftime("%I:%M %p")

    print(f"the date is {today} and hour is {current_time}")

    if today <= ramadan_end:
        maghribh_str = prayer_times["maghribh"]
        maghribh_conv = datetime.strptime(maghribh_str, "%I:%M %p") 

        iftar_warning = maghribh_conv - timedelta(minutes=5)
        iftar_warning_str = iftar_warning.strftime("%I:%M %p")

        if current_time == iftar_warning_str:
            print("Iftar is 5 minutes...")

    for prayer_name, prayer_time in prayer_times.items():
        if current_time == prayer_time:
            print(f"it is time for {prayer_name}")

    time.sleep(60)