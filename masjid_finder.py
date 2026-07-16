import math
from typing import Any, Optional

import requests


OVERPASS_URL = "https://overpass-api.de/api/interpreter"
DEFAULT_RADIUS_METERS = 5000
DEFAULT_TIMEOUT_SECONDS = 20
USER_AGENT = "namaz-reminder-masjid-finder/1.0"


def _haversine_meters(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    radius = 6_371_000
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    delta_phi = math.radians(lat2 - lat1)
    delta_lambda = math.radians(lon2 - lon1)

    a = (
        math.sin(delta_phi / 2) ** 2
        + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return radius * c


def _build_overpass_query(latitude: float, longitude: float, radius_meters: int) -> str:
    return f"""
[out:json][timeout:20];
(
  node["amenity"="place_of_worship"]["religion"="muslim"](around:{radius_meters},{latitude},{longitude});
  node["amenity"="place_of_worship"]["denomination"="muslim"](around:{radius_meters},{latitude},{longitude});
  node["building"="mosque"](around:{radius_meters},{latitude},{longitude});
  way["amenity"="place_of_worship"]["religion"="muslim"](around:{radius_meters},{latitude},{longitude});
  way["amenity"="place_of_worship"]["denomination"="muslim"](around:{radius_meters},{latitude},{longitude});
  way["building"="mosque"](around:{radius_meters},{latitude},{longitude});
  relation["amenity"="place_of_worship"]["religion"="muslim"](around:{radius_meters},{latitude},{longitude});
  relation["amenity"="place_of_worship"]["denomination"="muslim"](around:{radius_meters},{latitude},{longitude});
  relation["building"="mosque"](around:{radius_meters},{latitude},{longitude});
);
out center tags;
""".strip()


def _extract_coordinates(element: dict[str, Any]) -> tuple[Optional[float], Optional[float]]:
    if "lat" in element and "lon" in element:
        return element["lat"], element["lon"]

    center = element.get("center") or {}
    return center.get("lat"), center.get("lon")


def _format_address(tags: dict[str, Any]) -> Optional[str]:
    parts = [
        tags.get("addr:housenumber"),
        tags.get("addr:street"),
        tags.get("addr:city") or tags.get("addr:town") or tags.get("addr:suburb"),
        tags.get("addr:state"),
    ]
    filtered = [part for part in parts if part]
    return ", ".join(filtered) if filtered else None


def _normalize_result(
    element: dict[str, Any],
    origin_latitude: float,
    origin_longitude: float,
) -> Optional[dict[str, Any]]:
    latitude, longitude = _extract_coordinates(element)
    if latitude is None or longitude is None:
        return None

    tags = element.get("tags", {})
    name = tags.get("name") or tags.get("name:en") or "Nearby Masjid"
    distance_meters = _haversine_meters(origin_latitude, origin_longitude, latitude, longitude)

    return {
        "name": name,
        "latitude": latitude,
        "longitude": longitude,
        "distance_meters": round(distance_meters, 1),
        "address": _format_address(tags),
        "google_maps_url": (
            f"https://www.google.com/maps/dir/?api=1&destination={latitude},{longitude}"
        ),
    }


def find_nearest_masjid(
    latitude: float,
    longitude: float,
    radius_meters: int = DEFAULT_RADIUS_METERS,
) -> Optional[dict[str, Any]]:
    query = _build_overpass_query(latitude, longitude, radius_meters)
    response = requests.post(
        OVERPASS_URL,
        data=query,
        timeout=DEFAULT_TIMEOUT_SECONDS,
        headers={
            "Content-Type": "text/plain",
            "User-Agent": USER_AGENT,
        },
    )
    response.raise_for_status()

    payload = response.json()
    candidates = []
    for element in payload.get("elements", []):
        normalized = _normalize_result(element, latitude, longitude)
        if normalized is not None:
            candidates.append(normalized)

    if not candidates:
        return None

    return min(candidates, key=lambda candidate: candidate["distance_meters"])
