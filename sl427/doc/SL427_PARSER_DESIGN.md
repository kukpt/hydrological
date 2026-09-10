# SL/T 427-2021 解析器详细设计

## 1. 目标与边界

本设计面向 `sl427` Maven 模块，实现 SL/T 427-2021 中监测站与中心站之间的报文解析。核心目标是：

- 在 TCP 字节流中正确处理半包、粘包、噪声和多帧。
- 把帧边界、公共字段、AFN 语义和监测数据解码分层。
- 同时保留原始字节、解析值和数据质量，便于现场排障。
- 通过 `MessageSchema` 描述“规约规定的结构”，通过 `ProtocolContext` 提供“设备的点位配置”。
- 协议核心不访问数据库，不依赖具体网关或存储实现。

不在当前范围内：设备档案数据库、Redis 缓存、IoT 平台统一消息模型、厂商私有扩展的具体实现，以及 AFN `61H/83H` 图片报文。图片报只保留原始数据并返回 `UNSUPPORTED`，不处理图片长度特例、分片拼接和 JPEG/BMP 识别。

## 2. 设计依据及需要注意的版本问题

依据为本目录中的 [SLT 427-2021.pdf](./SLT%20427-2021.pdf)，重点对应：

- 6.2：传输服务与重发。
- 6.3：帧格式、控制域、地址域、CRC、AFN、PW 和 Tp。
- 7.2～7.5：参数设置、查询、控制和自报数据。
- 附录 A：AFN 定义。
- 附录 B：各 AFN 的帧字段组成。

注意：用户提供的 PDF 封面与页眉仍显示 `SL/T 427-20XX` 和“审定会后稿”。实现时以该文件的字段定义为当前基线，但每个已实现 AFN 都必须用真实报文或正式出版本复核。

## 3. 总体解析链路

```text
TCP bytes
   ↓
Sl427FrameDecoder          找 68、读 L、解决半包/粘包
   ↓
RawSl427Frame              一帧完整原始字节
   ↓
Sl427FrameParser           验证 68/L/16/CRC，解 C/A/AFN，保留 body
   ↓
Sl427FrameAssembler        DIV=1 时倒计重组
   ↓
MessageSchemaRegistry      根据方向 + AFN + 类型码 + 角色切分 D/PW/Tp
   ↓
AfnParserRegistry          分派到具体 AFN 解析器
   ↓
DataItemDecoder            BCD/BIN/位域/时间/状态
   ↓
ParseResult<ProtocolMessage>
```

约束：

1. `FrameDecoder` 只确定边界，不解释监测值。
2. CRC 不通过的帧不进入多帧重组。
3. 在 schema 选中前，`FrameParser` 不猜测 `D/PW/Tp` 边界。
4. 解析器不直接查库；设备点位从 `ProtocolContext` 传入。

## 4. 帧层设计

### 4.1 标准帧格式

```text
+--------+--------+--------+-----------------------+--------+--------+
| 68     | L      | 68     | USER DATA (L bytes)   | CS     | 16     |
+--------+--------+--------+-----------------------+--------+--------+
   1 B      1 B      1 B              L B              1 B      1 B
```

- `L` 是 1 字节 BIN，范围 `1..255`。
- 整帧字节数是 `L + 5`。
- `USER DATA = C + A + AFN + D + AUX`。
- `CS` 不计入 `L`，帧头和帧尾也不计入。
- 一般 AFN 为 1 字节；`FF 01..FE` 是 2 字节自定义 AFN。

最小用户数据长度：

```text
DIV=0, normal AFN: C(1) + A(5) + AFN(1) = 7
DIV=1, normal AFN: C(1) + DIVS(1) + A(5) + AFN(1) = 8
```

`Sl427FrameDecoder` 必须设置可配置的缓冲上限和噪声上限，不允许无界等待错误长度。

### 4.2 字节流状态机

```text
SEEK_START
  ├─ byte != 0x68: 丢弃 1 byte，继续查找
  └─ byte == 0x68: 进入 READ_LENGTH

READ_LENGTH
  └─ 不足 2 byte: 等待

VERIFY_SECOND_START
  ├─ index+2 != 0x68: 丢弃第一个 0x68，回到 SEEK_START
  └─ 正确: expected = L + 5

WAIT_FRAME
  ├─ readable < expected: 等待后续 TCP 数据
  └─ readable >= expected: 截取一帧，继续解剩余字节
```

错误恢复时只丢弃已证明不可能是帧头的字节，不要在一次错误后清空整个 TCP 缓冲区。

### 4.3 控制域

```text
D7      D6      D5..D4       D3..D0
DIR     DIV       FCB       command/type
```

- `DIR=0`：中心站发出的下行帧。
- `DIR=1`：终端机发出的上行帧。
- `DIV=0`：单帧。
- `DIV=1`：控制域后紧跟 1 字节 `DIVS`，从 `255` 向 `1` 倒计，`1` 是最后一帧。
- `FCB` 是 2 bit，新传输服务设置，重发时递减，从动站响应时回传相应值。
- 低 4 bit 的含义与方向有关：下行 `0000` 为发送/确认，`0001..1111` 为查询参数类型；上行 `0000` 为认可，`0001..1111` 为自报参数类型。

`ControlField` 保留 `raw` 值，并提供 `direction/split/frameCount/typeCode`，不在该类中根据 AFN 猜测最终报文角色。

### 4.4 地址域

`A` 始终是 5 字节，支持两种格式：

1. `A1(3-byte BCD) + A2(2-byte BIN)`：行政区划码 + 监测站/中继站地址。A2 范围为监测站 `1..60000`、中继站 `60001..65534`、广播 `65535`、无效 `0`。
2. `00 + 4-byte station code`：特征码 `00H` + 符合 SL 502-2010 的 8 位 HEX 测站编码。

`StationAddress` 应同时保留：

- `rawBytes`
- `AddressMode`
- 方式 1 的 `administrativeCode/stationNumber`
- 方式 2 的 `stationCode`
- 稳定的 `canonicalKey`

不能只用一个拼接的十六进制字符串代替结构化地址。厂商地址偏差通过 `AddressCodec` 兼容策略处理。

### 4.5 CRC-8

CS 对完整 `USER DATA`（从 C 开始，到 D/AUX 结束）计算，不包含两个 `68`、`L`、CS 和 `16`。

生成多项式为 `x^7 + x^6 + x^5 + x^2 + 1`，对应 `0xE5`。当前黄金样例验证的算法参数为：

```text
width   = 8
poly    = 0xE5
init    = 0x00
refIn   = false
refOut  = false
xorOut  = 0x00
```

CRC 必须以无状态类 `Sl427Crc8` 单独实现，用参数化测试锁定，不与帧解析代码混在一起。

### 4.6 原始帧对象

```java
final class Sl427Frame {
  int declaredLength;
  ControlField control;
  Integer splitCountdown;
  StationAddress address;
  Afn afn;
  byte[] bodyRaw;       // 尚未根据 schema 切分的 D + AUX
  int checksum;
  byte[] rawFrame;
}
```

`bodyRaw` 是必须字段。因为 PW 和 Tp 不是每帧都存在，不应由一个通用帧解析器从尾部盲切。

## 5. Schema 与 Context

### 5.1 Schema 是规约知识

```java
final class SchemaKey {
  Direction direction;
  int afn;
  int typeCode;
  MessageRole role; // COMMAND, QUERY, RESPONSE, REPORT, CONFIRM
}

interface MessageSchema {
  SchemaKey key();
  AuxLayout auxLayout();
  LengthEvaluation evaluateLength(ProtocolContext context, int bodyLength);
  ParseResult<? extends ProtocolMessage> parse(Sl427Frame frame,
                                                ProtocolContext context);
}
```

`SchemaKey` 不能只有 AFN。同一 AFN 的查询帧、响应帧、自报帧和确认帧的 `D/PW/Tp` 布局可以不同。

`AuxLayout` 明确为以下之一：

- `NONE`
- `PASSWORD_2`
- `TIME_TAG_7`
- `PASSWORD_2_AND_TIME_TAG_7`
- `CUSTOM`

例如：

- AFN `02H` 链路检测：无 AUX，D 为 1 字节。
- 大多数重要下行参数设置命令：`D + PW(2) + Tp(7)`。
- 参数设置响应通常不含 PW/Tp，但必须以附录 B 的具体表为准。
- AFN `81H/82H/C0H` 的自报帧与确认帧包含 `Tp(7)`。
- AFN `84H` 的自报帧不含 Tp，但确认帧含 Tp。AFN `83H` 图片报当前不注册 schema。

### 5.2 Context 是设备知识

```java
public interface ProtocolContext {
  String protocolVersion();
  AddressMode addressMode();
  List<? extends ProtocolPoint> points(ParameterType type);
  Optional<String> property(String key);
}

public interface ProtocolPoint {
  String id();
  ParameterType type();
  int channelNo();
  String unit();
}
```

`ProtocolContext` 由上层网关按站址加载并缓存。`sl427` 模块只定义接口，不包含 JDBC、SQLite、Redis 或 Spring 代码。

### 5.3 监测点数 N 的确定

优先级：

1. 报文自描述的数量或位图，但位图可能只表示“有此类参数”，不代表同类点数。
2. `ProtocolContext.points(type).size()`。
3. 只在 schema 明确、剩余长度可整除且结果唯一时，从长度推导 N，且生成警告。

不允许在不知道尾部固定字段和 AUX 长度时直接使用 `remaining / itemSize`。

配置 N 与报文长度冲突时返回 `CONFIG_MISMATCH`，保留原始数据，不把后续状态或 Tp 强行当作监测值。

## 6. AFN 分派

```java
interface AfnParser<T extends ProtocolMessage> {
  Set<SchemaKey> supports();
  ParseResult<T> parse(Sl427Frame frame, ProtocolContext context);
}
```

`AfnParserRegistry` 在启动时校验 key 重复，运行时 O(1) 查找，不使用长 `if/else` 链。

AFN 分组：

| 组 | AFN | 首期策略 |
| --- | --- | --- |
| 链路检测 | `02H` | 第一批实现 |
| 参数设置 | `10H..4FH`, `A0H..AFH` | 按附录 B 逐个增加 schema |
| 参数查询 | `50H..65H`, `B0H..B2H` | `B0H` 第一批实现 |
| 自报 | `81H`, `82H`, `84H`, `C0H` | `C0H` 第一批实现；`83H` 图片报返回 `UNSUPPORTED` |
| 控制 | `90H..96H` | 在基础帧稳定后增加 |
| 自定义 | `FF01H..FFFEH` | 保留 raw + SPI，默认 `UNSUPPORTED` |

## 7. B0/C0 实时数据解析

### 7.1 参数类型与数据长度

B0 响应和 C0 自报的参数类型由 C 的低 4 bit 确定。除特别定义外，实时数据 D 的最后 4 字节是 `alarmStatus(2) + terminalStatus(2)`。

| typeCode | 参数 | 单点长度 | D 长度公式 |
| --- | --- | ---: | --- |
| `0001` | 雨量 | 3 | `3 + 4` |
| `0010` | 水位 | 4 | `4*N + 4` |
| `0011` | 流量+累计水量 | 10 | `10*N + 4` |
| `0100` | 流速 | 3 | `3*N + 4` |
| `0101` | 闸位 | 3 | `3*N + 4` |
| `0110` | 功率 | 3 | `3*N + 4` |
| `0111` | 气象 | 8 | `8 + 4` |
| `1000` | 电量 | 4 | `4*N + 4` |
| `1001` | 水温 | 2 | `2*N + 4` |
| `1010` | 水质 | 4（加 5-byte 位图） | `5 + 4*N + 4` |
| `1011` | 土壤含水率+深度 | 4 | `4*N + 4` |
| `1100` | 蒸发量 | 3 | `3*N + 4` |
| `1101` | 报警/状态，也用于输入电压 | schema 决定 | 电压为 `2 + 4` |
| `1110` | 下行为综合参数；上行为统计雨量 | 变长 | schema 决定 |
| `1111` | 水压 | 4 | `4*N + 4` |

注意：对上行自报，`1110` 是统计雨量，不是综合参数。这是 `SchemaKey` 必须包含方向的典型原因。

### 7.2 数据项解码器

```text
RainfallDecoder             3-byte packed BCD, scale=1
WaterLevelDecoder           4-byte packed BCD, sign nibble, scale=3
FlowDecoder                 5-byte packed BCD, sign + unit bits, scale=3
TotalWaterDecoder           5-byte packed BCD, unsigned
VelocityDecoder             3-byte packed BCD, sign nibble, scale=3
GatePositionDecoder         3-byte packed BCD, scale=2
PowerDecoder                3-byte packed BCD
MeteorologyDecoder          pressure + wind + direction + temperature
WaterTemperatureDecoder     2-byte packed BCD, scale=1
WaterQualityDecoder         5-byte bitmap + ordered 4-byte values
SoilMoistureDecoder         moisture + depth
EvaporationDecoder          3-byte packed BCD, scale=1
PressureDecoder             4-byte packed BCD, scale=2
VoltageDecoder              2-byte packed BCD, scale=2
ElectricityDecoder          4-byte packed BCD, scale=2
StatusDecoder               2-byte alarm + 2-byte terminal bit fields
TimeTagDecoder              second, minute, hour, day, month, year, delay
```

BCD 解码规则：

- 数值字节按标准表定义的低位在前方式重排半字节。
- 除标准明确用于符号或单位的 bit 外，BCD nibble 必须为 `0..9`。
- 全 `A` 是标准中使用的未采集/无效占位，返回 `value=null, quality=INVALID`，不强制转数字。
- 部分 `A` 或其他非法 nibble 返回 `INVALID_DATA`，并保留该字段原始值。
- 数值统一用 `BigDecimal`，避免 `double` 的二进制浮点误差。

### 7.3 黄金样例

引用对话中的真实报文：

```text
68 1C 68
B3
32 00 10 01 10
C0
AA AA AA AA AA
05 01 00 00 00
00 00 20 00
00 00 10 30 03 22 00
12
16
```

经标准表复核后的切分：

| 字段 | 原始值 | 解释 |
| --- | --- | --- |
| start1 | `68` | 正确 |
| L | `1C` | 28，整帧长 `28+5=33` |
| start2 | `68` | 正确 |
| C | `B3` | `DIR=1, DIV=0, FCB=3, type=0011` |
| A | `32 00 10 01 10` | 保留 raw，由 `AddressCodec` 产生规范站址 |
| AFN | `C0` | 自报实时数据 |
| D | 14 bytes | `10*N+4`，N=1 |
| flow | `AA AA AA AA AA` | 无效/未采集 |
| totalWater | `05 01 00 00 00` | 105 m³ |
| alarmStatus | `00 00` | 原始位域 |
| terminalStatus | `20 00` | 原始位域，再按状态表展开 |
| Tp | `00 00 10 30 03 22 00` | `2022-03-30 10:00:00`, delay=0 min |
| CS | `12` | CRC-8 校验通过 |
| end | `16` | 正确 |

CRC 测试输入必须是从 `B3` 到 Tp 最后一个 `00` 的 28 字节，预期结果 `0x12`。

## 8. 多帧报文

`DIV=1` 时，每一帧先独立验证长度、帧尾和 CRC，再交给 `Sl427FrameAssembler`。

建议重组 key：

```text
direction + canonicalAddress + AFN + typeCode
```

状态：

```java
final class AssemblyState {
  int previousDivs;
  List<byte[]> fragments;
  long totalBytes;
  long deadlineNanos;
}
```

规则：

- 首帧可以从任意 `DIVS > 1` 开始，下一帧必须是 `previousDivs - 1`。
- 收到重复 DIVS 时根据原始数据去重；同 DIVS 不同数据视为冲突。
- `DIVS=1` 后完成拼接并移除状态。
- 超时、倒计跳变、站址/AFN/类型变化时返回结构化错误。
- 限制单条重组的帧数、总字节数与全局并发数，防止内存消耗。
- 由于规约没有通用的独立消息 ID，同一站点同 AFN/类型的分帧报文应串行处理。

### 图片报处理边界

AFN `61H/83H` 当前不进入业务解析和多帧拼接。解码器如果能按普通 `L + 5` 规则完整取得帧，则保留 `C/A/AFN/bodyRaw/rawFrame`，返回 `status=UNSUPPORTED` 和 `issue.code=UNSUPPORTED_IMAGE_FRAME`；如果不能确定帧边界，则由连接层记录原始字节并终止该帧的语义解析。

普通数据报文的 DIV/DIVS 重组仍在当前范围内，不因排除图片报而删除。

## 9. ParseResult 和错误分类

```java
final class ParseResult<T> {
  ParseStatus status;
  T message;
  List<ParseIssue> issues;
  byte[] raw;
}

enum ParseStatus {
  SUCCESS,
  PARTIAL,
  UNSUPPORTED,
  CONFIG_MISMATCH,
  INVALID_FRAME,
  INVALID_DATA
}
```

分层原则：

- 半包不是错误，`FrameDecoder` 等待后续字节。
- 帧头、长度、帧尾、CRC 错误属于 `INVALID_FRAME`。
- 未实现 AFN 或厂商自定义 AFN 属于 `UNSUPPORTED`，仍返回帧元数据和 raw body。
- 点位数与长度公式冲突属于 `CONFIG_MISMATCH`。
- BCD/时间/位域越界属于 `INVALID_DATA`。
- 部分字段可解且不会造成后续字段错位时可返回 `PARTIAL`。

`ParseIssue` 至少包含 `code, field, offset, expected, actual, message`，运维日志不应只剩一条异常字符串。

## 10. 建议包结构

```text
io.github.kukpt.sl427
├── buffer
│   ├── ProtocolBufferReader
│   ├── BcdCodec
│   └── BitCodec
├── checksum
│   └── Sl427Crc8
├── codec
│   ├── Sl427FrameDecoder
│   ├── Sl427FrameEncoder
│   └── Sl427FrameParser
├── frame
│   ├── Sl427Frame
│   ├── ControlField
│   ├── StationAddress
│   ├── TimeTag
│   └── Afn
├── assembly
│   └── Sl427FrameAssembler
├── context
│   ├── ProtocolContext
│   └── ProtocolPoint
├── schema
│   ├── MessageSchema
│   ├── SchemaKey
│   └── MessageSchemaRegistry
├── parser
│   ├── AfnParser
│   ├── AfnParserRegistry
│   ├── LinkDetectionParser
│   └── RealtimeDataParser
├── data
│   ├── DataItemDecoder
│   ├── FlowDecoder
│   ├── TotalWaterDecoder
│   ├── WaterLevelDecoder
│   └── StatusDecoder
├── model
│   ├── ProtocolMessage
│   ├── RealtimeDataMessage
│   ├── LinkDetectionMessage
│   └── RawAfnMessage
└── result
    ├── ParseResult
    ├── ParseStatus
    └── ParseIssue
```

Vert.x/Netty 只出现在 `codec` 和服务端适配层；BCD、CRC、schema、data parser 保持可在普通单元测试中独立运行。

## 11. 处理时序

```text
Client        FrameDecoder       FrameParser       Assembler       Schema/Parser       Gateway
  | bytes          |                  |                 |                 |                |
  |--------------->|                  |                 |                 |                |
  |                 | complete frame   |                 |                 |                |
  |                 |----------------->|                 |                 |                |
  |                 |                  | validate CRC    |                 |                |
  |                 |                  |---------------->|                 |                |
  |                 |                  |                 | assembled/raw   |                |
  |                 |                  |                 |---------------->|                |
  |                 |                  |                 |                 | load context   |
  |                 |                  |                 |                 |--------------->|
  |                 |                  |                 |                 |<---------------|
  |                 |                  |                 |                 | parse result   |
  |                 |                  |                 |                 |--------------->|
```

设备上下文建议由 Gateway 按 `SL427:<canonicalAddress>` 缓存。配置变更时主动失效，不在每条报文中查数据库。

## 12. 测试设计

### 12.1 帧层

- 一次输入完整帧。
- 逐字节输入同一帧。
- 一次输入两帧。
- 噪声 + 完整帧。
- 假 `68` + 完整帧。
- L 过小、L 与实际长度不符、错误第二起始符、错误结束符。
- CRC 正确、CRC 错误。
- 自定义两字节 AFN。

### 12.2 数据项

每种编码至少测试：0、最大值、正数、负数、单位 bit、全 A、部分 A、非法 BCD、长度不足。

### 12.3 Schema/Context

- N=0/1/16。
- 配置 N 与数据长度一致。
- 配置 N 过大/过小时返回 `CONFIG_MISMATCH`。
- 无 context 但长度可唯一推导时返回带 warning 的结果。
- 无 context 且长度不可唯一推导时不强解。

### 12.4 多帧

- `3,2,1` 正常重组。
- 重复帧去重。
- 缺失中间帧。
- 倒计不连续。
- 同 key 交叉报文。
- 超时回收和总字节上限。

### 12.5 黄金报文

首个端到端测试使用 7.3 的 C0 报文，断言：

- `L=28`、总长 33。
- `DIR=UP, DIV=false, type=FLOW_TOTAL`。
- AFN `C0`。
- CRC 预期 `12`。
- N=1。
- 瞬时流量为 invalid。
- 累计水量为 105 m³。
- Tp 为 `2022-03-30 10:00:00`, delay=0。

## 13. 分阶段实施

### 阶段 1：帧和基础数据编码

- `Sl427Crc8`
- `BcdCodec`
- `ControlField`
- `StationAddress`
- `Sl427FrameDecoder`
- `Sl427FrameParser`
- 7.3 黄金报文帧层测试

### 阶段 2：首批业务报文

- AFN `02H` 链路检测
- AFN `B0H` 实时值查询/响应
- AFN `C0H` 实时数据自报/确认
- 水位、流量/水量、状态和 Tp

### 阶段 3：全实时参数与多帧

- B0/C0 其他参数类型
- `ProtocolContext`
- `MessageSchemaRegistry`
- `Sl427FrameAssembler`

### 阶段 4：设置、查询、控制与编码

- 附录 B 剩余 AFN schema
- `Sl427FrameEncoder`
- PW/Tp 下行构造
- FCB、超时、最多 3 次重发与响应匹配

### 阶段 5：厂商兼容

- 厂商 codec/schema 扩展 SPI

图片报不纳入上述实施阶段，后续如有明确需求再单独设计。

## 14. 待确认项

1. 提供的 PDF 为审定会后稿，需与正式出版本核对差异。
2. 地址方式 1 的规范展示字符串和现场设备的字节排列需用设备档案核对。
3. C0 上行 `type=1110` 统计雨量是否在 4 字节雨量数据后仍附加 4 字节状态，应用正式版标准和抓包确认。
4. FCB 的现场设备初始值和厂商重发行为需通过联调锁定。

上述待确认项都应通过局部策略或 schema 解决，不应在通用 parser 中散落厂商 `if` 分支。
