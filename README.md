# sl651

`sl651` 是一个基于 Vert.x 和 Netty 的 SL651 水文协议服务端/编解码库，用于接收、解析和响应遥测终端上报的水文报文。

项目提供 TCP 服务端封装、SL651 报文解码、常见上行报文类型处理、M2 链路应答、CRC16 校验、多包报文合并，以及可选的 Web 调试监控页面。

## 功能特性

- 支持 SL651 上行报文解析和 CRC16 校验。
- 支持 M2 链路模式应答。
- 支持多包报文接收与合并。
- 支持常见功能码报文处理：
  - 链路维持报 `0x2F`
  - 测试报 `0x30`
  - 均匀时段水文信息报 `0x31`
  - 遥测站定时报 `0x32`
  - 遥测站加报报 `0x33`
  - 遥测站小时报 `0x34`
  - 图片报 `0x36`
  - 泵站控制 `0x4C`
- 支持原始 `ByteBuf` 回调，便于处理暂未封装的功能码。
- 可选启用 Metrics Web 调试页面，查看连接端点、报文日志并开启/关闭端点调试。

## 技术栈

- Java 8
- Maven
- Vert.x `4.5.23`
- Netty
- Jackson
- JUnit 4 / Vert.x Unit

## 快速开始

### 环境要求

请先确认本机已安装：

```bash
java -version
mvn -version
```

项目使用 Java 8 编译目标：

```xml
<maven.compiler.source>1.8</maven.compiler.source>
<maven.compiler.target>1.8</maven.compiler.target>
```

### 构建

```bash
mvn clean package
```

### 运行测试

```bash
mvn test
```

## Maven 依赖

如果作为库引入，可使用以下坐标：

```xml
<dependency>
  <groupId>io.github.kukpt</groupId>
  <artifactId>sl651</artifactId>
  <version>1.0.5.CR7</version>
</dependency>
```

## 启动服务端

下面示例启动一个 SL651 TCP 服务端，监听 `11883` 端口：

```java
import io.github.kukpt.sl651.HydrologicalEndpoint;
import io.github.kukpt.sl651.HydrologicalServer;
import io.github.kukpt.sl651.HydrologicalServerOptions;
import io.vertx.core.Vertx;

public class ServerExample {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    HydrologicalServerOptions options = new HydrologicalServerOptions()
        .setPort(11883);

    HydrologicalServer.create(vertx, options)
        .endpointHandler(ServerExample::handleEndpoint)
        .exceptionHandler(Throwable::printStackTrace)
        .listen()
        .onSuccess(server -> System.out.println("SL651 server started: " + server.actualPort()))
        .onFailure(Throwable::printStackTrace);
  }

  private static void handleEndpoint(HydrologicalEndpoint endpoint) {
    System.out.println("connected: " + endpoint.remoteAddress());

    endpoint.messageHandler(message -> {
      message.checkDecoderResult();

      System.out.println("station: " + message.telemetryStationAddress());
      System.out.println("function type: " + Integer.toHexString(message.functionType()));

      message.timingMessageHandler(timing -> {
        System.out.println("stream id: " + timing.streamId());
        timing.elementResults().forEach(System.out::println);
      });

      message.pictureMessageHandler(picture -> {
        System.out.println("picture message stream id: " + picture.streamId());
      });

      message.byteBufHandler(payload -> {
        System.out.println("raw payload length: " + payload.readableBytes());
      });

      message.handle();
    });

    endpoint.closeHandler(ep -> System.out.println("closed: " + ep.endpointId()));
    endpoint.exceptionHandler(Throwable::printStackTrace);
  }
}
```

## 配置项

`HydrologicalServerOptions` 继承自 Vert.x `NetServerOptions`，除 TCP 服务端配置外，还提供以下协议相关配置：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `port` | `11883` | TCP 服务监听端口 |
| `m2LinkMode` | `true` | 是否启用 M2 链路模式 |
| `centralStationAddress` | `0x01` | 中心站地址 |
| `protocolPassword` | `0x1234` | 协议密码 |
| `timeoutOnConnect` | `180` | 连接空闲超时时间，单位为秒 |
| `frameEndType` | `FrameEndType.ESC_MODE` | 下行报文结束控制方式 |
| `enableMetricsWeb` | `false` | 是否启用 Web 调试页面 |
| `metricsWebUserName` | 读取 `HY_METRICS_USERNAME` | Web 调试页面用户名 |
| `metricsWebPassword` | 读取 `HY_METRICS_PASSWORD` | Web 调试页面密码，启用 Web 调试时至少 12 位 |
| `metricsLogBaseDir` | `${user.dir}/file-uploads` | 报文日志保存目录 |

示例：

```java
HydrologicalServerOptions options = new HydrologicalServerOptions()
    .setHost("0.0.0.0")
    .setPort(11883)
    .setProtocolPassword(0x1234)
    .setCentralStationAddress((short) 0x01)
    .setTimeoutOnConnect(180)
    .enableMetricsWeb(true)
    .setMetricsWebUserName(System.getenv("HY_METRICS_USERNAME"))
    .setMetricsWebPassword(System.getenv("HY_METRICS_PASSWORD"))
    .setMetricsLogBaseDir("/tmp/hy_logs");
```

## Web 调试页面

启用 `enableMetricsWeb(true)` 后，服务会额外启动一个 HTTP Web 服务。该 Web 服务监听随机可用端口，启动日志中会打印端口：

```text
Metrics Web Server started. port: <port>
```

Web 调试页面不再提供固定默认账号密码。启用前必须设置账号和密码，否则服务会拒绝启动：

```bash
export HY_METRICS_USERNAME=admin
export HY_METRICS_PASSWORD='change-this-to-a-long-random-password'
```

也可以通过代码显式设置：

```java
new HydrologicalServerOptions()
    .enableMetricsWeb(true)
    .setMetricsWebUserName("admin")
    .setMetricsWebPassword("change-this-to-a-long-random-password");
```

访问地址：

```text
http://127.0.0.1:<port>/wisetion
```

Web 调试页面支持查看端点列表、读取端点报文记录，以及开启/关闭指定端点的调试日志。

## 项目结构

```text
src/main/java/io/github/kukpt/sl651
├── HydrologicalServer.java          # 服务端接口
├── HydrologicalEndpoint.java        # 连接端点接口
├── HydrologicalServerOptions.java   # 服务端与协议配置
├── MainVerticle.java                # 本地启动示例
├── MetricsWebVerticle.java          # Web 调试服务
├── codec/                           # 协议编解码、消息头、上下行报文
├── impl/                            # 服务端和连接实现
├── message/                         # 上行报文正文模型
├── metrics/                         # 调试、流量监控和报文存储
└── utils/                           # CRC、协议常量和工具类
```

测试用例位于：

```text
src/test/java/io/github/kukpt/sl651
```

其中 `ServerDecodeTest` 展示了通过 TCP 客户端发送十六进制水文报文并在服务端完成解析的流程。

## 常见开发命令

```bash
# 编译
mvn clean compile

# 执行测试
mvn test

# 打包
mvn clean package
```

## License

本项目使用 Apache License 2.0，详见 [LICENSE](LICENSE)。
