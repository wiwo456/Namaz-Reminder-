import requests
import os

user_key = os.getenv("USER_KEY")
api_key = os.getenv("API_KEY")

def send_notification(message):
    url = "https://api.pushover.net/1/messages.json"

    data = {
        "token": api_key,
        "user": user_key,
        "message": message
    }

    requests.post(url, data=data)