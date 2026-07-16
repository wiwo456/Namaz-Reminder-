import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from masjid_finder import DEFAULT_RADIUS_METERS, find_nearest_masjid


HOST = "127.0.0.1"
PORT = 8010


class ApiHandler(BaseHTTPRequestHandler):
    server_version = "NamazReminderAPI/1.0"

    def do_OPTIONS(self) -> None:
        self.send_response(HTTPStatus.NO_CONTENT)
        self._send_cors_headers()
        self.end_headers()

    def do_GET(self) -> None:
        parsed_url = urlparse(self.path)

        if parsed_url.path == "/health":
            self._send_json(
                HTTPStatus.OK,
                {
                    "status": "ok",
                },
            )
            return

        if parsed_url.path == "/api/nearest-masjid":
            self._handle_nearest_masjid(parsed_url.query)
            return

        self._send_json(
            HTTPStatus.NOT_FOUND,
            {
                "error": "Route not found",
            },
        )

    def _handle_nearest_masjid(self, query_string: str) -> None:
        query_params = parse_qs(query_string)

        try:
            latitude = float(query_params["lat"][0])
            longitude = float(query_params["lon"][0])
        except (KeyError, IndexError, ValueError):
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {
                    "error": "Query params 'lat' and 'lon' are required as valid numbers",
                },
            )
            return

        radius_param = query_params.get("radius", [str(DEFAULT_RADIUS_METERS)])[0]
        try:
            radius_meters = max(500, min(int(radius_param), 25_000))
        except ValueError:
            self._send_json(
                HTTPStatus.BAD_REQUEST,
                {
                    "error": "Query param 'radius' must be a valid integer",
                },
            )
            return

        try:
            result = find_nearest_masjid(latitude, longitude, radius_meters)
        except Exception as error:
            self._send_json(
                HTTPStatus.BAD_GATEWAY,
                {
                    "error": "Failed to query masjid data",
                    "details": str(error),
                },
            )
            return

        if result is None:
            self._send_json(
                HTTPStatus.NOT_FOUND,
                {
                    "error": "No masjid found nearby",
                    "radius_meters": radius_meters,
                },
            )
            return

        self._send_json(
            HTTPStatus.OK,
            {
                "data": result,
            },
        )

    def _send_json(self, status: HTTPStatus, payload: dict) -> None:
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self._send_cors_headers()
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_cors_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")

    def log_message(self, format: str, *args) -> None:
        return


def run() -> None:
    server = ThreadingHTTPServer((HOST, PORT), ApiHandler)
    print(f"Namaz Reminder API listening on http://{HOST}:{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    run()
