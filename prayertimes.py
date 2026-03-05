import requests
import json 
from datetime import datetime 

with open("config.json","r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config["lon"]

url = f"http://api.aladhan.com/v1/timings?latitude={lat}&longitude={lon}&method=2"

response = requests.get(url)
data = response.json()

