package crazypants.enderio.conduits.item.conduitswapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import com.enderio.core.common.util.NullHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class ConduitSwapperPayload {

  public static class Candidate {

    private final @Nonnull ItemStack stack;
    private final int inventoryCount;
    private final int networkCount;

    public Candidate(@Nonnull ItemStack stack, int inventoryCount, int networkCount) {
      ItemStack normalizedStack = stack.copy();
      if (!normalizedStack.isEmpty()) {
        normalizedStack.setCount(1);
      }

      this.stack = normalizedStack;
      this.inventoryCount = Math.max(0, inventoryCount);
      this.networkCount = Math.max(0, networkCount);
    }

    public @Nonnull ItemStack getStack() {
      return stack.copy();
    }

    public int getInventoryCount() {
      return inventoryCount;
    }

    public int getNetworkCount() {
      return networkCount;
    }

    public int getTotalCount() {
      return inventoryCount + networkCount;
    }

    public boolean canReplace(int lineLength) {
      return getTotalCount() >= lineLength;
    }

    private void write(@Nonnull ByteBuf buf) {
      ByteBufUtils.writeItemStack(buf, stack);
      buf.writeInt(inventoryCount);
      buf.writeInt(networkCount);
    }

    private static @Nonnull Candidate read(@Nonnull ByteBuf buf) {
      ItemStack stack = NullHelper.notnullF(ByteBufUtils.readItemStack(buf), "readItemStack returned null");
      int inventoryCount = buf.readInt();
      int networkCount = buf.readInt();

      return new Candidate(stack, inventoryCount, networkCount);
    }

  }

  private final @Nonnull BlockPos origin;
  private final @Nonnull EnumHand hand;
  private final @Nonnull ItemStack sourceStack;
  private final int lineLength;
  private final boolean warnsFluidLoss;
  private final boolean infiniteResources;
  private final @Nonnull List<Candidate> candidates;

  public ConduitSwapperPayload(@Nonnull BlockPos origin, @Nonnull EnumHand hand, @Nonnull ItemStack sourceStack, int lineLength, boolean warnsFluidLoss,
      boolean infiniteResources, @Nonnull List<Candidate> candidates) {
    ItemStack normalizedSource = sourceStack.copy();
    if (!normalizedSource.isEmpty()) {
      normalizedSource.setCount(1);
    }

    this.origin = origin;
    this.hand = hand;
    this.sourceStack = normalizedSource;
    this.lineLength = Math.max(1, lineLength);
    this.warnsFluidLoss = warnsFluidLoss;
    this.infiniteResources = infiniteResources;
    this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
  }

  public @Nonnull BlockPos getOrigin() {
    return origin;
  }

  public @Nonnull EnumHand getHand() {
    return hand;
  }

  public @Nonnull ItemStack getSourceStack() {
    return sourceStack.copy();
  }

  public int getLineLength() {
    return lineLength;
  }

  public boolean warnsFluidLoss() {
    return warnsFluidLoss;
  }

  public boolean hasInfiniteResources() {
    return infiniteResources;
  }

  public @Nonnull List<Candidate> getCandidates() {
    return candidates;
  }

  public void write(@Nonnull ByteBuf buf) {
    buf.writeLong(origin.toLong());
    buf.writeInt(hand.ordinal());
    ByteBufUtils.writeItemStack(buf, sourceStack);
    buf.writeInt(lineLength);
    buf.writeBoolean(warnsFluidLoss);
    buf.writeBoolean(infiniteResources);
    buf.writeInt(candidates.size());

    for (Candidate candidate : candidates) {
      candidate.write(buf);
    }
  }

  public static @Nonnull ConduitSwapperPayload read(@Nonnull ByteBuf buf) {
    BlockPos origin = BlockPos.fromLong(buf.readLong());

    int handOrdinal = buf.readInt();
    EnumHand hand = handOrdinal >= 0 && handOrdinal < EnumHand.values().length ? EnumHand.values()[handOrdinal] : EnumHand.MAIN_HAND;

    ItemStack sourceStack = NullHelper.notnullF(ByteBufUtils.readItemStack(buf), "readItemStack returned null");
    int lineLength = buf.readInt();
    boolean warnsFluidLoss = buf.readBoolean();
    boolean infiniteResources = buf.readBoolean();
    int candidateCount = Math.max(0, buf.readInt());

    List<Candidate> candidates = new ArrayList<>(candidateCount);
    for (int index = 0; index < candidateCount; index++) {
      candidates.add(Candidate.read(buf));
    }

    return new ConduitSwapperPayload(origin, hand, sourceStack, lineLength, warnsFluidLoss, infiniteResources, candidates);
  }

}
