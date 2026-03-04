import requests
import json

with open ("config.json", "r") as f:
    config = json.load(f)
    user_key = config["user_key"]
    api_key = config["api_key"]
    
def send_notification(message):
    url = "https://api.pushover.net/1/messages.json"
    data = {
        "token": api_key,
        "user": user_key,
        "message": message
    }


    requests.post(url, data=data)