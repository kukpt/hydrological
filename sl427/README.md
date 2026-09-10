# SL427

`sl427` 是 SL/T 427-2021 水资源监测数据传输规约的 Java 8 解析模块，包名为 `io.github.kukpt.sl427`。

## 已实现

- TCP 半包、粘包和前置噪声处理。
- `68 L 68 ... CRC 16` 帧校验、CRC-8 `0xE5`、控制域、两种站址和自定义 AFN。
- `DIV/DIVS` 普通分帧倒计、去重、顺序和大小校验。
- AFN `02H` 链路检测、`B0H` 实时查询/响应、`C0H` 实时自报/确认、`84H` 输入电压自报/确认。
- 雨量、水位、流量/累计水量、流速、闸位、功率、气象、电量、水温、水质、土壤含水率、蒸发量、状态、统计雨量、电压和水压的结构化解码。
- 设备点位数量由 `ProtocolContext` 校验；无配置时从唯一可判定的报文长度推导。
- 未注册 AFN 返回 `UNSUPPORTED` 并保留原文。`61H/83H` 图片报按当前要求不解析。

## 使用

### 独立启动

`Sl427MainVerticle` 默认在 `0.0.0.0:11824` 监听 TCP 连接，每个连接使用独立的有状态解析器：

```java
io.github.kukpt.sl427.Sl427MainVerticle.main(args);
```

日志等级可通过 JVM 参数或环境变量设置，JVM 参数优先：

```bash
java -Dsl427.log.level=DEBUG ...
# 或
export SL427_LOG_LEVEL=DEBUG
```

支持 `TRACE`、`DEBUG`、`INFO`、`WARN`、`ERROR` 和 `OFF`。要查看完整的分阶段解码日志，请使用 `DEBUG`。

Server 会在完整、成功解码 `81H`、`82H`、`84H` 或 `C0H` 上行自报后，自动发送 S2 确认帧。确认帧保留上行帧的 FCB、地址和 AFN，并默认使用 `COMPATIBLE(00H)` 工作模式与中心站当前时间 Tp。`83H` 图片帧仍不在当前普通帧流解码范围内。

嵌入时可以指定确认后的终端工作模式和原始报文记录器：

```java
new Sl427MainVerticle(11824, TerminalWorkMode.SELF_REPORT, trafficRecorder);
```

### Jar 集成

SL427 与 SL651 使用相同的 `Server -> Endpoint -> Handler` 调用风格：

```java
SimpleProtocolContext context = SimpleProtocolContext.builder()
  .protocolVersion("2021")
  .addPoint(new SimpleProtocolPoint("flow-1", ParameterType.FLOW_TOTAL, 1, "m3/s"))
  .build();

Sl427Server server = Sl427Server.create(vertx, new Sl427ServerOptions()
  .setPort(11824)
  .setTimeoutOnConnect(180)
  .setTrafficLogBaseDir("./hy-data")
  .setProtocolContext(context));

server.endpointHandler(endpoint -> endpoint
    .messageHandler(message -> {
      // 处理成功解码的消息
    })
    .decodeFailureHandler(result -> {
      // 处理 CONFIG_MISMATCH、UNSUPPORTED 等结果
    })
    .exceptionHandler(Throwable::printStackTrace)
    .closeHandler(closed -> {
    }))
  .onlineHandler(endpoint -> {
    // 首条有效报文识别出站址后触发
  })
  .offlineHandler(endpoint -> {
    // 已识别站点的连接断开后触发
  })
  .exceptionHandler(Throwable::printStackTrace)
  .listen();
```

`endpointHandler` 表示 TCP 连接建立；`onlineHandler` 表示已从首条有效报文识别出站址。
在线设备使用 `SL427:<canonicalAddress>` 作为稳定 ID，可以通过
`server.getEndpoint(endpointId)` 或 `server.onlineEndpoints()` 查询。同一站址重新连接时，新连接会替换旧连接。

原始收发报文默认写入 `./hy-data`，保留 30 天。可以修改目录或关闭内置存储：

```java
new Sl427ServerOptions()
  .setTrafficLogBaseDir("/data/sl427-traffic")
  .setEnableTrafficStore(false);
```

SL427 与 SL651 共用 `monitoring-web`。任意一个 Server 启动 Web 后，两种协议的
Provider 都会注册到同一页面；SL427 当前提供在线状态和报文历史，不提供人工下行。

```java
new Sl427ServerOptions()
  .enableMetricsWeb(true)
  .setMetricsWebUserName(System.getenv("HY_METRICS_USERNAME"))
  .setMetricsWebPassword(System.getenv("HY_METRICS_PASSWORD"))
  // 可选：允许浏览器直接访问 SL427 TCP 端口
  .enableHttpProxy(true);
```

SL651 和 SL427 都可开启 HTTP 代理并指向同一个 Web 后端，但通常只需要对外开放其中一个协议端口。

多站点使用不同点位配置时，可按已解码的站址动态获取 Context：

```java
new Sl427ServerOptions().setProtocolContextResolver(address ->
  contextCache.get("SL427:" + address.canonicalKey()));
```

### 嵌入式解析

`Sl427Parser` 是有连接状态的解析器，每个 TCP 连接应创建一个实例：

```java
Sl427Parser parser = new Sl427Parser();

socket.handler(buffer -> {
  for (ParseResult<ProtocolMessage> result : parser.accept(buffer, protocolContext)) {
    if (result.isSuccess()) {
      ProtocolMessage message = result.value();
      // 处理 LinkDetectionMessage、RealtimeDataMessage 或 CommandMessage
    } else {
      // 记录 result.status()、result.issues() 和 result.raw()
    }
  }
});
```

单独使用 Parser 时，可传入公共记录器。每个切出的完整帧只记录一次，记录中会包含 `protocol=SL427`：

```java
ProtocolTrafficRecorder recorder = new EventBusProtocolTrafficRecorder(
  vertx, ProtocolTrafficTopics.APPEND);
Sl427Parser parser = new Sl427Parser(endpointId, remoteAddress, recorder);
```

解码过程统一使用 `[SL427][decode:<stage>]` 前缀。帧流、校验、报文头、分帧组包、Schema、正文拆分、实时数据类型和每个测量值均输出 `DEBUG` 日志；校验失败、配置不匹配和不支持的报文输出 `WARN` 日志。原始报文持久化不受日志级别影响。

对已经切分好的单帧也可直接调用：

```java
ParseResult<ProtocolMessage> result = parser.parse(frameBytes, protocolContext);
```

## Maven 坐标

```xml
<dependency>
  <groupId>io.github.kukpt</groupId>
  <artifactId>sl427</artifactId>
  <version>1.0.5.CR9</version>
</dependency>
```
