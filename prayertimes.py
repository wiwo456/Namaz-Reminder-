import requests
import json
from datetime import datetime


# load config
with open("config.json", "r") as f:
    config = json.load(f)

lat = config["lat"]
lon = config["lon"]


def get_prayer_times():
    try:
        url = f"https://api.aladhan.com/v1/timings?latitude={lat}&longitude={lon}&method=2"

        response = requests.get(url, timeout=10)
        data = response.json()

        hijri_month = data["data"]["date"]["hijri"]["month"]["number"]
        timings = data["data"]["timings"]

        def convert_time(t):
            time_obj = datetime.strptime(t, "%H:%M")
            return time_obj.strftime("%I:%M %p")

        prayer_times = {
            "fajr": convert_time(timings["Fajr"]),
            "dhuhr": convert_time(timings["Dhuhr"]),
            "asr": convert_time(timings["Asr"]),
            "maghrib": convert_time(timings["Maghrib"]),
            "isha": convert_time(timings["Isha"])
        }

        return prayer_times, hijri_month

    except Exception as e:
        print("The API error is:", e)
        return None