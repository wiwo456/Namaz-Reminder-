import requests
import json

# Load config
with open("config.json", "r") as f:
    config = json.load(f)

TOKEN = config["discord_token"]
CHANNEL_ID = config["channel_id"]

# Discord API endpoint
url = f"https://discord.com/api/v10/channels/{CHANNEL_ID}/messages"

# Headers for authentication
headers = {
    "Authorization": f"Bot {TOKEN}",
    "Content-Type": "application/json"
}

def send_notification(message):
    data = {
        "content": message
    }

    response = requests.post(url, headers=headers, json=data, timeout=10)

    # Optional: print if something fails
    if response.status_code != 200:
        print("Error sending message:", response.text)