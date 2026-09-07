package io.github.kukpt.sl651.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;

public class MultiPack {

  public MultiPack(int totalPack) {
    this.totalPack = totalPack;
    this.packs = new ArrayList<Pack>(this.totalPack);
  }

  public void addPack(int currentPack, ByteBuf data) {
    byte[] bytes = new byte[data.readableBytes()];
    data.readBytes(bytes);
    data.release();
    this.addPack(currentPack, bytes);
  }

  public ByteBuf buffers() {
    ByteBuf buf = Unpooled.buffer();
    for (Pack p : packs) {
      buf.writeBytes(p.data);
    }
    return buf;
  }

  public void addPack(int currentPack, byte[] pack) {
    this.packs.add(new Pack(currentPack, pack));
  }

  private final int totalPack;

  private final ArrayList<Pack> packs;


  private class Pack {

    Pack(int currentPack, byte[] data) {
      this.currentPack = currentPack;
      this.data = data;
    }

    int currentPack;

    byte[] data;

  }

}
