#!/usr/bin/env python3
"""
Minimal OTLP/HTTP (JSON) receiver for development: shows what the apps send, with no
Docker and no account anywhere. Standard library only.

    python3 tools/otlp-receiver.py            # listens on :4318, the OTLP/HTTP default

Point the apps at it:
    Android emulator: OTLP_ENDPOINT=http://10.0.2.2:4318   (local.properties)
    iOS simulator:    OTLP_ENDPOINT = http:/$()/localhost:4318   (Secrets.xcconfig)

Each request is checked against the OTLP JSON shape the backends expect (hex ids,
64-bit numbers as strings, one value per attribute) and printed as one line per span or
log. Anything malformed is answered with 400, like a real collector, and reported.
"""
import json
import re
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

HEX32 = re.compile(r"^[0-9a-f]{32}$")
HEX16 = re.compile(r"^[0-9a-f]{16}$")
VALUE_KINDS = {"stringValue", "boolValue", "intValue", "doubleValue"}


def attrs(items):
    out = {}
    for item in items or []:
        value = item["value"]
        kinds = VALUE_KINDS & value.keys()
        assert len(kinds) == 1, f"attribute {item['key']} must have exactly one value: {value}"
        kind = kinds.pop()
        if kind == "intValue":
            assert isinstance(value[kind], str), f"intValue of {item['key']} must be a string"
        out[item["key"]] = value[kind]
    return out


def check_traces(body):
    lines = []
    for resource_spans in body["resourceSpans"]:
        resource = attrs(resource_spans["resource"]["attributes"])
        assert "service.name" in resource, "resource needs service.name"
        for scope_spans in resource_spans["scopeSpans"]:
            for span in scope_spans["spans"]:
                assert HEX32.match(span["traceId"]), f"traceId must be 32 hex chars: {span['traceId']}"
                assert HEX16.match(span["spanId"]), f"spanId must be 16 hex chars: {span['spanId']}"
                if "parentSpanId" in span:
                    assert HEX16.match(span["parentSpanId"]), "parentSpanId must be 16 hex chars"
                start, end = span["startTimeUnixNano"], span["endTimeUnixNano"]
                assert isinstance(start, str) and isinstance(end, str), "timestamps must be strings"
                millis = (int(end) - int(start)) / 1e6
                status = "ERROR" if span["status"]["code"] == 2 else "ok"
                parent = span.get("parentSpanId", "-")[:6]
                lines.append(
                    f"  span  {span['name']:<32} {millis:8.1f}ms  {status:<5} "
                    f"trace={span['traceId'][:6]} span={span['spanId'][:6]} parent={parent} "
                    f"{attrs(span['attributes'])}"
                )
        lines.insert(0, f"traces from {resource.get('service.name')} {resource.get('os.type')} "
                        f"{resource.get('service.version', '')} {resource.get('deployment.environment.name', '')} "
                        f"device={resource.get('device.manufacturer', '-')} {resource.get('device.model.identifier', '-')} "
                        f"id={resource.get('device.id', '-')}")
    return lines


def check_logs(body):
    lines = []
    for resource_logs in body["resourceLogs"]:
        resource = attrs(resource_logs["resource"]["attributes"])
        for scope_logs in resource_logs["scopeLogs"]:
            for record in scope_logs["logRecords"]:
                assert isinstance(record["timeUnixNano"], str), "timeUnixNano must be a string"
                lines.append(
                    f"  log   {record['severityText']:<5} {record['body']['stringValue']} {attrs(record['attributes'])}"
                )
        lines.insert(0, f"logs from {resource.get('service.name')} {resource.get('os.type')}")
    return lines


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        raw = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        try:
            body = json.loads(raw)
            if self.path == "/v1/traces":
                lines = check_traces(body)
            elif self.path == "/v1/logs":
                lines = check_logs(body)
            else:
                self.send_error(404)
                return
        except (AssertionError, KeyError, ValueError) as error:
            print(f"REJECTED {self.path}: {error}", flush=True)
            self.send_response(400)
            self.end_headers()
            return
        auth = "with Authorization" if self.headers.get("Authorization") else "no Authorization"
        print(f"POST {self.path} ({auth}) " + "\n".join(lines), flush=True)
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(b"{}")

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 4318
    print(f"OTLP/HTTP receiver on :{port}", flush=True)
    HTTPServer(("0.0.0.0", port), Handler).serve_forever()
