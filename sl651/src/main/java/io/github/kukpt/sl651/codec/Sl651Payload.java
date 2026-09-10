package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;

public class Sl651Payload {

  private ByteBuf sp;

  private MultiPack mp;

  public MultiPack mp() {
    return mp;
  }

  public ByteBuf sp() {
    return sp;
  }

  public Sl651Payload (MultiPack mp) {
    this.mp = mp;
  }

  public Sl651Payload (ByteBuf payload) {
    this.sp = payload;
  }





}
