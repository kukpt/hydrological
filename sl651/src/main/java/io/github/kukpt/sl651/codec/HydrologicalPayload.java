package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;

public class HydrologicalPayload {

  private ByteBuf sp;

  private MultiPack mp;

  public MultiPack mp() {
    return mp;
  }

  public ByteBuf sp() {
    return sp;
  }

  public HydrologicalPayload (MultiPack mp) {
    this.mp = mp;
  }

  public HydrologicalPayload (ByteBuf payload) {
    this.sp = payload;
  }





}
