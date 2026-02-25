import requests 
import json
import time  
from datetime import datetime
from prayertimes import prayer_times



with open ("config.json", "r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config ["lon"]

print("This is a namaz and ftar time reminding bot.") 


print(prayer_times)
while True:
    today = datetime.now().date()
    current_time = datetime.now().strftime("%I:%M %p")

    print(f"the date is {today} and hour is {current_time}")
    
    for prayer_name, prayer_time in prayer_times.items():
        if current_time == prayer_time:
            print(f"it is time for {prayer_name}")

    time.sleep(60)