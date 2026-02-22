import requests 
import json 

with open ("config.json", "r") as f:
    config = json.load(f)
    lat = config["lat"]
    lon = config ["lon"]





print("This is a namaz and ftar time reminding bot.")   