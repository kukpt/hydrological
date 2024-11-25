package io.github.kukpt.sl651.codec;


public enum FunctionType {

  LINK_KEEP(0x2F), // 链路维持报        index: 1
  TEST(0x30),      // 测试报           index: 2
  PERIOD(0x31),    // 均匀时段水文信息报 index: 3
  TIMING(0x32),    // 遥测站定时报      index: 4
  ADDITIONAL(0x33),// 遥测站加报报      index: 5
  HOURLY(0x34),    // 遥测站小时报      index: 6
  PUMP_CONTROL(0x4C);  // 泵站控制


  private static int value2Index(int value) {
    return value - 0x2E;
  }

  private final int value;

  private static final FunctionType[] VALUES;

  static {
    final FunctionType[] values = values();
    VALUES = new FunctionType[0xFF + 1];
    for (FunctionType ft : values) {
      final int index = value2Index(ft.value);
      if (VALUES[index] != null) {
        throw new AssertionError("value already in use: " + ft.value);
      }
      // 对VALUES 重新排列，以方便访问
      VALUES[index] = ft;
    }
  }

  public int value() {
    return value;
  }

  public static FunctionType valueOf(int type) {

    int index = value2Index(type);
    if (index <= 0 || index >= VALUES.length) {
      throw new IllegalArgumentException("unknown message type: " + type);
    }
    return VALUES[index];
  }

  FunctionType(int value) {
    this.value = value;
  }
}
