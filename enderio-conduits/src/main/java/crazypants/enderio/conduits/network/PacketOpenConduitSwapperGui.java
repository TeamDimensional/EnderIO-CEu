package crazypants.enderio.conduits.network;

import java.util.Collections;
import javax.annotation.Nonnull;

import crazypants.enderio.conduits.EnderIOConduits;
import crazypants.enderio.conduits.item.conduitswapper.ConduitSwapperPayload;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketOpenConduitSwapperGui implements IMessage {

  private @Nonnull ConduitSwapperPayload payload = new ConduitSwapperPayload(BlockPos.ORIGIN, EnumHand.MAIN_HAND,
      ItemStack.EMPTY, 1, false, false, Collections.emptyList());

  public PacketOpenConduitSwapperGui() {
  }

  public PacketOpenConduitSwapperGui(@Nonnull ConduitSwapperPayload payload) {
    this.payload = payload;
  }

  @Override
  public void fromBytes(@Nonnull ByteBuf buf) {
    payload = ConduitSwapperPayload.read(buf);
  }

  @Override
  public void toBytes(@Nonnull ByteBuf buf) {
    payload.write(buf);
  }

  public static class Handler implements IMessageHandler<PacketOpenConduitSwapperGui, IMessage> {

    @Override
    public IMessage onMessage(PacketOpenConduitSwapperGui message, MessageContext ctx) {
      EnderIOConduits.proxy.openConduitSwapperGui(message.payload);
      return null;
    }

  }

}
