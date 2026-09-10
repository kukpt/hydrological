package io.github.kukpt.sl427.model;

public enum ParameterType {
  RAINFALL,
  WATER_LEVEL,
  FLOW_TOTAL,
  FLOW,
  TOTAL_WATER,
  VELOCITY,
  GATE_POSITION,
  POWER,
  METEOROLOGY,
  ELECTRICITY,
  WATER_TEMPERATURE,
  WATER_QUALITY,
  SOIL_MOISTURE,
  EVAPORATION,
  STATUS,
  COMPOSITE,
  STATISTICAL_RAINFALL,
  VOLTAGE,
  PRESSURE;

  public static ParameterType forRealtime(int afn, int typeCode) {
    switch (typeCode) {
      case 1: return RAINFALL;
      case 2: return WATER_LEVEL;
      case 3: return FLOW_TOTAL;
      case 4: return VELOCITY;
      case 5: return GATE_POSITION;
      case 6: return POWER;
      case 7: return METEOROLOGY;
      case 8: return ELECTRICITY;
      case 9: return WATER_TEMPERATURE;
      case 10: return WATER_QUALITY;
      case 11: return SOIL_MOISTURE;
      case 12: return EVAPORATION;
      case 13: return afn == 0xB0 ? VOLTAGE : STATUS;
      case 14: return afn == 0xC0 ? STATISTICAL_RAINFALL : COMPOSITE;
      case 15: return PRESSURE;
      default: return null;
    }
  }
}
