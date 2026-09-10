# monitoring-web

`monitoring-web` provides one authenticated Web UI shared by SL651 and SL427.
Protocol modules register a `ProtocolMonitoringProvider`; the first Server configured with
`enableMetricsWeb(true)` starts the HTTP backend. Both protocol TCP listeners can optionally
enable HTTP detection with `enableHttpProxy(true)` and proxy to that same backend.

The proxy is disabled by default. In a process that exposes both protocol ports, normally only
one port should enable it. SL651 contributes its downstream and debug actions; SL427 currently
contributes a read-only online endpoint view.
