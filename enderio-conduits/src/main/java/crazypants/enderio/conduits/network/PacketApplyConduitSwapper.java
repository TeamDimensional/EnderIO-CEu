package crazypants.enderio.conduits.network;

import javax.annotation.Nonnull;

import com.enderio.core.common.network.MessageTileEntity;
import com.enderio.core.common.util.NullHelper;

import crazypants.enderio.conduits.conduit.TileConduitBundle;
import crazypants.enderio.conduits.item.conduitswapper.ConduitSwapperLogic;
import crazypants.enderio.conduits.item.conduitswapper.ItemConduitSwapper;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketApplyConduitSwapper extends MessageTileEntity<TileConduitBundle> {

  private EnumHand hand = EnumHand.MAIN_HAND;
  private @Nonnull ItemStack sourceStack = ItemStack.EMPTY;
  private @Nonnull ItemStack targetStack = ItemStack.EMPTY;

  public PacketApplyConduitSwapper() {
  }

  public PacketApplyConduitSwapper(@Nonnull TileConduitBundle tile, @Nonnull EnumHand hand, @Nonnull ItemStack sourceStack, @Nonnull ItemStack targetStack) {
    super(tile);
    this.hand = hand;
    this.sourceStack = sourceStack.copy();
    if (!this.sourceStack.isEmpty()) {
      this.sourceStack.setCount(1);
    }
    this.targetStack = targetStack.copy();
    if (!this.targetStack.isEmpty()) {
      this.targetStack.setCount(1);
    }
  }

  @Override
  public void write(@Nonnull ByteBuf buf) {
    buf.writeInt(hand.ordinal());
    ByteBufUtils.writeItemStack(buf, sourceStack);
    ByteBufUtils.writeItemStack(buf, targetStack);
  }

  @Override
  public void read(@Nonnull ByteBuf buf) {
    int handOrdinal = buf.readInt();
    hand = handOrdinal >= 0 && handOrdinal < EnumHand.values().length ? EnumHand.values()[handOrdinal] : EnumHand.MAIN_HAND;
    sourceStack = NullHelper.notnullF(ByteBufUtils.readItemStack(buf), "readItemStack returned null");
    targetStack = NullHelper.notnullF(ByteBufUtils.readItemStack(buf), "readItemStack returned null");
  }

  public static class Handler implements IMessageHandler<PacketApplyConduitSwapper, IMessage> {

    @Override
    public IMessage onMessage(PacketApplyConduitSwapper message, MessageContext ctx) {
      EntityPlayerMP player = ctx.getServerHandler().player;
      World world = player.world;
      if (!world.isBlockLoaded(message.getPos())) {
        return null;
      }

      if (!ConduitSwapperLogic.isWithinInteractionRange(player, message.getPos())) {
        return null;
      }

      TileEntity tileEntity = world.getTileEntity(message.getPos());
      if (!(tileEntity instanceof TileConduitBundle)) {
        return null;
      }

      ItemStack heldItem = player.getHeldItem(message.hand);
      if (!(heldItem.getItem() instanceof ItemConduitSwapper)) {
        return null;
      }

      ConduitSwapperLogic.applySwap(player, message.hand, heldItem, (TileConduitBundle) tileEntity, message.sourceStack, message.targetStack);
      return null;
    }

  }

}
