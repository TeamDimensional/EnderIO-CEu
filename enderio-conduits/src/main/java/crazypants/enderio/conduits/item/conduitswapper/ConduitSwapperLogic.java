package crazypants.enderio.conduits.item.conduitswapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.BlockEnder;
import com.enderio.core.common.util.ItemUtil;

import crazypants.enderio.base.conduit.ConduitUtil;
import crazypants.enderio.base.conduit.IConduit;
import crazypants.enderio.base.conduit.IConduitItem;
import crazypants.enderio.base.conduit.IConduitNetwork;
import crazypants.enderio.base.conduit.IServerConduit;
import crazypants.enderio.base.conduit.RaytraceResult;
import crazypants.enderio.conduits.conduit.BlockConduitBundle;
import crazypants.enderio.conduits.conduit.TileConduitBundle;
import crazypants.enderio.conduits.conduit.liquid.ILiquidConduit;
import crazypants.enderio.conduits.lang.Lang;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public final class ConduitSwapperLogic {

  private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;

  private ConduitSwapperLogic() {
  }

  @SideOnly(Side.CLIENT)
  public static @Nonnull ItemStack resolveClientSourceStack(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
    if (!(world.getBlockState(pos).getBlock() instanceof BlockConduitBundle)) {
      return ItemStack.EMPTY;
    }

    BlockConduitBundle block = (BlockConduitBundle) world.getBlockState(pos).getBlock();
    RaytraceResult hit = block.doRayTrace(world, pos, player);
    if (hit == null || hit.component.conduitType == null) {
      return ItemStack.EMPTY;
    }

    TileConduitBundle bundle = BlockEnder.getAnyTileEntitySafe(world, pos, TileConduitBundle.class);
    if (bundle == null) {
      return ItemStack.EMPTY;
    }

    IConduit conduit = bundle.getConduit(hit.component.conduitType);
    if (conduit == null) {
      return ItemStack.EMPTY;
    }

    ItemStack result = conduit.createItem();
    result.setCount(1);
    return result;
  }

  public static @Nullable ConduitSwapperPayload buildPayload(@Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull ItemStack swapperStack,
      @Nonnull TileConduitBundle bundle, @Nonnull ItemStack sourceStack) {
    IServerConduit sourceConduit = findMatchingConduit(bundle, sourceStack);
    if (sourceConduit == null) {
      player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_SOURCE_MISSING.toChatServer(), true);
      return null;
    }

    List<IServerConduit> lineConduits;
    try {
      lineConduits = collectLineConduits(sourceConduit);
    } catch (PartialLineLoadException exception) {
      player.sendStatusMessage(Lang.GUI_NETWORK_PARTIALLY_UNLOADED.toChatServer(), true);
      return null;
    }

    boolean infiniteResources = player.capabilities.isCreativeMode;
    List<CandidateCounter> counters = infiniteResources ? collectCreativeCandidateCounts(sourceConduit.getBaseConduitType(), sourceConduit.createItem())
        : collectCandidateCounts(player, swapperStack, sourceConduit.getBaseConduitType(), sourceConduit.createItem());
    if (counters.isEmpty()) {
      player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_NO_COMPATIBLE.toChatServer(), true);
      return null;
    }

    sortCandidates(counters, lineConduits.size(), infiniteResources);

    List<ConduitSwapperPayload.Candidate> candidates = new ArrayList<>(counters.size());
    for (CandidateCounter counter : counters) {
      candidates.add(new ConduitSwapperPayload.Candidate(counter.stack, counter.inventoryCount, counter.networkCount));
    }

    return new ConduitSwapperPayload(bundle.getPos(), hand, sourceConduit.createItem(), lineConduits.size(),
        sourceConduit.getBaseConduitType() == ILiquidConduit.class, infiniteResources, candidates);
  }

  public static boolean applySwap(@Nonnull EntityPlayer player, @Nonnull EnumHand hand, @Nonnull ItemStack swapperStack, @Nonnull TileConduitBundle bundle,
      @Nonnull ItemStack sourceStack, @Nonnull ItemStack targetStack) {
    IServerConduit sourceConduit = findMatchingConduit(bundle, sourceStack);
    if (sourceConduit == null) {
      player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_SOURCE_MISSING.toChatServer(), true);
      return false;
    }

    if (!(targetStack.getItem() instanceof IConduitItem)) {
      player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_APPLY_FAILED.toChatServer(), true);
      return false;
    }

    IConduitItem conduitItem = (IConduitItem) targetStack.getItem();
    if (conduitItem.getBaseConduitType() != sourceConduit.getBaseConduitType()) {
      player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_APPLY_FAILED.toChatServer(), true);
      return false;
    }

    List<IServerConduit> lineConduits;
    try {
      lineConduits = collectLineConduits(sourceConduit);
    } catch (PartialLineLoadException exception) {
      player.sendStatusMessage(Lang.GUI_NETWORK_PARTIALLY_UNLOADED.toChatServer(), true);
      return false;
    }

    boolean consumeResources = !player.capabilities.isCreativeMode;
    if (consumeResources) {
      int requiredCount = lineConduits.size();
      int inventoryAvailable = countInventory(player, targetStack);
      int networkAvailable = countWireless(player, swapperStack, targetStack);
      if (inventoryAvailable + networkAvailable < requiredCount) {
        player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_NOT_ENOUGH.toChatServer(requiredCount - inventoryAvailable - networkAvailable), true);
        return false;
      }

      int extractedFromNetwork = 0;
      int desiredNetworkCount = Math.max(0, requiredCount - inventoryAvailable);
      if (desiredNetworkCount > 0) {
        extractedFromNetwork = extractFromWireless(player, swapperStack, targetStack, desiredNetworkCount);
      }

      int inventoryNeeded = requiredCount - extractedFromNetwork;
      if (inventoryNeeded > inventoryAvailable) {
        returnExtractedToStorage(player, swapperStack, targetStack, extractedFromNetwork, bundle.getPos());
        player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_NOT_ENOUGH.toChatServer(inventoryNeeded - inventoryAvailable), true);
        return false;
      }

      int consumedFromInventory = consumeFromInventory(player, targetStack, inventoryNeeded);
      if (consumedFromInventory < inventoryNeeded) {
        returnExtractedToStorage(player, swapperStack, targetStack, extractedFromNetwork, bundle.getPos());
        player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_NOT_ENOUGH.toChatServer(inventoryNeeded - consumedFromInventory), true);
        return false;
      }
    }

    List<ItemStack> returnedDrops = new ArrayList<>();
    for (IServerConduit conduit : lineConduits) {
      TileConduitBundle conduitBundle = (TileConduitBundle) conduit.getBundle().getEntity();
      Map<EnumFacing, NBTTagCompound> settings = captureConnectionSettings(conduit);

      if (consumeResources) {
        for (ItemStack drop : conduit.getDrops()) {
          if (!drop.isEmpty()) {
            returnedDrops.add(drop.copy());
          }
        }
      }

      IServerConduit replacementConduit = conduitItem.createConduit(targetStack, player);
      if (!conduitBundle.removeConduit(conduit)) {
        continue;
      }

      if (!conduitBundle.addConduit(replacementConduit)) {
        conduitBundle.addConduit(conduit);
        player.sendStatusMessage(Lang.GUI_CONDUIT_SWAPPER_APPLY_FAILED.toChatServer(), true);
        break;
      }

      restoreConnectionSettings(replacementConduit, settings);
    }

    if (consumeResources) {
      returnDrops(player, swapperStack, returnedDrops, bundle.getPos());
    }
    player.inventoryContainer.detectAndSendChanges();
    player.swingArm(hand);
    return true;
  }

  public static boolean isWithinInteractionRange(@Nonnull EntityPlayer player, @Nonnull BlockPos pos) {
    return player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_INTERACTION_DISTANCE_SQUARED;
  }

  private static void returnDrops(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull Collection<ItemStack> drops,
      @Nonnull BlockPos dropPos) {
    List<ReturnedStackCounter> storedInWireless = new ArrayList<>();

    for (ItemStack drop : drops) {
      ItemStack remainder = drop.copy();

      if (!remainder.isEmpty() && swapperStack.getItem() instanceof ItemConduitSwapper) {
        ItemConduitSwapper swapperItem = (ItemConduitSwapper) swapperStack.getItem();
        if (swapperItem.canAccessWireless(player, swapperStack)) {
          int previousCount = remainder.getCount();
          remainder = ConduitSwapperWirelessHelper.store(swapperItem, player, swapperStack, remainder);
          int storedCount = previousCount - (remainder.isEmpty() ? 0 : remainder.getCount());
          if (storedCount > 0) {
            getOrCreateReturnedStackCounter(storedInWireless, drop).count += storedCount;
          }
        }
      }

      if (remainder.isEmpty()) {
        continue;
      }

      if (!player.inventory.addItemStackToInventory(remainder)) {
        ItemUtil.spawnItemInWorldWithRandomMotion(player.world, remainder, dropPos);
      }
    }

    for (ReturnedStackCounter counter : storedInWireless) {
      TextComponentTranslation stackName = new TextComponentTranslation(counter.stack.getUnlocalizedName() + ".name");
      stackName.getStyle().color = TextFormatting.AQUA;

      player.sendMessage(Lang.GUI_CONDUIT_SWAPPER_AE2_STORED.toChatServer(counter.count, stackName));
    }
  }

  private static void returnExtractedToStorage(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull ItemStack targetStack, int count,
      @Nonnull BlockPos dropPos) {
    if (count <= 0) {
      return;
    }

    ItemStack extracted = targetStack.copy();
    extracted.setCount(count);
    returnDrops(player, swapperStack, Collections.singletonList(extracted), dropPos);
  }

  private static int countWireless(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull ItemStack targetStack) {
    if (!(swapperStack.getItem() instanceof ItemConduitSwapper)) {
      return 0;
    }

    ItemConduitSwapper swapperItem = (ItemConduitSwapper) swapperStack.getItem();
    if (!swapperItem.canAccessWireless(player, swapperStack)) {
      return 0;
    }

    return ConduitSwapperWirelessHelper.countStack(swapperItem, player, swapperStack, targetStack);
  }

  private static int extractFromWireless(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull ItemStack targetStack, int count) {
    if (!(swapperStack.getItem() instanceof ItemConduitSwapper)) {
      return 0;
    }

    ItemConduitSwapper swapperItem = (ItemConduitSwapper) swapperStack.getItem();
    if (!swapperItem.canAccessWireless(player, swapperStack)) {
      return 0;
    }

    return ConduitSwapperWirelessHelper.extract(swapperItem, player, swapperStack, targetStack, count);
  }

  private static int countInventory(@Nonnull EntityPlayer player, @Nonnull ItemStack targetStack) {
    int count = 0;

    for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
      ItemStack stack = player.inventory.getStackInSlot(slot);
      if (!ItemUtil.areStacksEqual(stack, targetStack)) {
        continue;
      }

      count += stack.getCount();
    }

    return count;
  }

  private static int consumeFromInventory(@Nonnull EntityPlayer player, @Nonnull ItemStack targetStack, int requiredCount) {
    int remaining = requiredCount;

    for (int slot = 0; slot < player.inventory.getSizeInventory() && remaining > 0; slot++) {
      ItemStack stack = player.inventory.getStackInSlot(slot);
      if (!ItemUtil.areStacksEqual(stack, targetStack)) {
        continue;
      }

      int removed = Math.min(remaining, stack.getCount());
      stack.shrink(removed);
      remaining -= removed;

      if (stack.isEmpty()) {
        player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
      }
    }

    return requiredCount - remaining;
  }

  private static @Nonnull List<CandidateCounter> collectCandidateCounts(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack,
      @Nonnull Class<? extends IConduit> baseType, @Nonnull ItemStack sourceStack) {
    List<CandidateCounter> counters = new ArrayList<>();

    for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
      ItemStack stack = player.inventory.getStackInSlot(slot);
      if (!isCompatibleCandidate(stack, baseType, sourceStack)) {
        continue;
      }

      CandidateCounter counter = getOrCreateCounter(counters, stack);
      counter.inventoryCount += stack.getCount();
    }

    if (swapperStack.getItem() instanceof ItemConduitSwapper) {
      ItemConduitSwapper swapperItem = (ItemConduitSwapper) swapperStack.getItem();
      if (swapperItem.canAccessWireless(player, swapperStack)) {
        ConduitSwapperWirelessHelper.forEachCandidate(swapperItem, player, swapperStack, baseType, sourceStack, (stack, count) -> {
          CandidateCounter counter = getOrCreateCounter(counters, stack);
          counter.networkCount += count;
        });
      }
    }

    return counters;
  }

  private static @Nonnull List<CandidateCounter> collectCreativeCandidateCounts(@Nonnull Class<? extends IConduit> baseType, @Nonnull ItemStack sourceStack) {
    List<CandidateCounter> counters = new ArrayList<>();

    for (Item item : Item.REGISTRY) {
      if (!(item instanceof IConduitItem)) {
        continue;
      }

      IConduitItem conduitItem = (IConduitItem) item;
      if (conduitItem.getBaseConduitType() != baseType) {
        continue;
      }

      NonNullList<ItemStack> subItems = NonNullList.create();
      item.getSubItems(CreativeTabs.SEARCH, subItems);
      for (ItemStack stack : subItems) {
        if (!isCompatibleCandidate(stack, baseType, sourceStack)) {
          continue;
        }

        getOrCreateCounter(counters, stack);
      }
    }

    return counters;
  }

  private static void sortCandidates(@Nonnull List<CandidateCounter> counters, int lineLength, boolean infiniteResources) {
    Collections.sort(counters, new Comparator<CandidateCounter>() {
      @Override
      public int compare(CandidateCounter left, CandidateCounter right) {
        if (infiniteResources) {
          return left.stack.getDisplayName().compareToIgnoreCase(right.stack.getDisplayName());
        }

        int canReplaceCompare = Boolean.compare(right.getTotalCount() >= lineLength, left.getTotalCount() >= lineLength);
        if (canReplaceCompare != 0) {
          return canReplaceCompare;
        }

        int countCompare = Integer.compare(right.getTotalCount(), left.getTotalCount());
        if (countCompare != 0) {
          return countCompare;
        }

        return left.stack.getDisplayName().compareToIgnoreCase(right.stack.getDisplayName());
      }
    });
  }

  private static boolean isCompatibleCandidate(@Nonnull ItemStack stack, @Nonnull Class<? extends IConduit> baseType, @Nonnull ItemStack sourceStack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof IConduitItem) || ItemUtil.areStacksEqual(stack, sourceStack)) {
      return false;
    }

    IConduitItem conduitItem = (IConduitItem) stack.getItem();
    return conduitItem.getBaseConduitType() == baseType;
  }

  private static @Nonnull CandidateCounter getOrCreateCounter(@Nonnull List<CandidateCounter> counters, @Nonnull ItemStack stack) {
    ItemStack normalizedStack = stack.copy();
    normalizedStack.setCount(1);

    for (CandidateCounter counter : counters) {
      if (ItemUtil.areStacksEqual(counter.stack, normalizedStack)) {
        return counter;
      }
    }

    CandidateCounter counter = new CandidateCounter(normalizedStack);
    counters.add(counter);
    return counter;
  }

  private static @Nonnull ReturnedStackCounter getOrCreateReturnedStackCounter(@Nonnull List<ReturnedStackCounter> counters, @Nonnull ItemStack stack) {
    ItemStack normalizedStack = stack.copy();
    normalizedStack.setCount(1);

    for (ReturnedStackCounter counter : counters) {
      if (ItemUtil.areStacksEqual(counter.stack, normalizedStack)) {
        return counter;
      }
    }

    ReturnedStackCounter counter = new ReturnedStackCounter(normalizedStack);
    counters.add(counter);
    return counter;
  }

  private static @Nonnull Map<EnumFacing, NBTTagCompound> captureConnectionSettings(@Nonnull IServerConduit conduit) {
    Map<EnumFacing, NBTTagCompound> settings = new EnumMap<>(EnumFacing.class);

    for (EnumFacing direction : EnumFacing.VALUES) {
      NBTTagCompound tag = new NBTTagCompound();
      if (conduit.writeConnectionSettingsToNBT(direction, tag)) {
        settings.put(direction, tag);
      }
    }

    return settings;
  }

  private static void restoreConnectionSettings(@Nonnull IServerConduit conduit, @Nonnull Map<EnumFacing, NBTTagCompound> settings) {
    for (Map.Entry<EnumFacing, NBTTagCompound> entry : settings.entrySet()) {
      conduit.readConduitSettingsFromNBT(entry.getKey(), entry.getValue());
    }
  }

  private static @Nullable IServerConduit findMatchingConduit(@Nonnull TileConduitBundle bundle, @Nonnull ItemStack sourceStack) {
    for (IServerConduit conduit : bundle.getServerConduits()) {
      if (ItemUtil.areStacksEqual(conduit.createItem(), sourceStack)) {
        return conduit;
      }
    }

    return null;
  }

  private static @Nonnull List<IServerConduit> collectLineConduits(@Nonnull IServerConduit sourceConduit) throws PartialLineLoadException {
    IConduitNetwork<?, ?> network = sourceConduit.getNetwork();
    if (network != null) {
      List<IServerConduit> result = new ArrayList<>(network.getConduits());
      sortByPosition(result);
      return result;
    }

    World world = sourceConduit.getBundle().getBundleworld();
    List<IServerConduit> result = new ArrayList<>();
    Deque<IServerConduit> pending = new ArrayDeque<>();
    Set<BlockPos> visited = new HashSet<>();

    pending.add(sourceConduit);
    while (!pending.isEmpty()) {
      IServerConduit conduit = pending.removeFirst();
      BlockPos position = conduit.getBundle().getLocation();
      if (!visited.add(position)) {
        continue;
      }

      result.add(conduit);

      for (EnumFacing direction : conduit.getConduitConnections()) {
        BlockPos nextPos = position.offset(direction);
        if (!world.isBlockLoaded(nextPos)) {
          throw new PartialLineLoadException();
        }

        @SuppressWarnings("unchecked")
        IConduit connected = ConduitUtil.getConduit(world, nextPos, (Class<IConduit>) conduit.getBaseConduitType());
        if (connected instanceof IServerConduit) {
          pending.add((IServerConduit) connected);
        }
      }
    }

    sortByPosition(result);
    return result;
  }

  private static void sortByPosition(@Nonnull List<IServerConduit> conduits) {
    Collections.sort(conduits, new Comparator<IServerConduit>() {
      @Override
      public int compare(IServerConduit left, IServerConduit right) {
        BlockPos leftPos = left.getBundle().getLocation();
        BlockPos rightPos = right.getBundle().getLocation();

        int compareX = Integer.compare(leftPos.getX(), rightPos.getX());
        if (compareX != 0) {
          return compareX;
        }

        int compareY = Integer.compare(leftPos.getY(), rightPos.getY());
        if (compareY != 0) {
          return compareY;
        }

        return Integer.compare(leftPos.getZ(), rightPos.getZ());
      }
    });
  }

  private static class CandidateCounter {

    private final @Nonnull ItemStack stack;
    private int inventoryCount;
    private int networkCount;

    private CandidateCounter(@Nonnull ItemStack stack) {
      this.stack = stack;
    }

    private int getTotalCount() {
      return inventoryCount + networkCount;
    }

  }

  private static class ReturnedStackCounter {

    private final @Nonnull ItemStack stack;
    private int count;

    private ReturnedStackCounter(@Nonnull ItemStack stack) {
      this.stack = stack;
    }

  }

  private static class PartialLineLoadException extends Exception {

    private static final long serialVersionUID = 461643271574163016L;

  }

}
