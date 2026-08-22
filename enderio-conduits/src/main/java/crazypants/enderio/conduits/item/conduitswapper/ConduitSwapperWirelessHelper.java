package crazypants.enderio.conduits.item.conduitswapper;

import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.util.ItemUtil;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.me.helpers.BaseActionSource;
import appeng.util.item.AEItemStack;
import crazypants.enderio.base.conduit.IConduit;
import crazypants.enderio.base.conduit.IConduitItem;
import crazypants.enderio.conduits.init.ConduitObject;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class ConduitSwapperWirelessHelper {

  private ConduitSwapperWirelessHelper() {
  }

  public static void registerWirelessHandler() {
    AEApi.instance().registries().wireless().registerWirelessHandler((IWirelessTermHandler) ConduitObject.item_conduit_swapper.getItemNN());
  }

  static boolean hasAccess(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack) {
    WirelessTerminalGuiObject wireless = createWirelessObject(handler, player, swapperStack);
    if (wireless == null || !wireless.rangeCheck()) {
      return false;
    }

    return getItemMonitor(wireless) != null;
  }

  static int countStack(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull ItemStack template) {
    WirelessTerminalGuiObject wireless = createWirelessObject(handler, player, swapperStack);
    if (wireless == null || !wireless.rangeCheck()) {
      return 0;
    }

    IMEMonitor<IAEItemStack> monitor = getItemMonitor(wireless);
    if (monitor == null) {
      return 0;
    }

    IAEItemStack request = AEItemStack.fromItemStack(template.copy());
    if (request == null) {
      return 0;
    }

    IAEItemStack found = monitor.getStorageList().findPrecise(request);
    if (found == null) {
      return 0;
    }

    return clampCount(found.getStackSize());
  }

  static void forEachCandidate(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack,
      @Nonnull Class<? extends IConduit> baseType, @Nonnull ItemStack sourceStack, @Nonnull BiConsumer<ItemStack, Integer> consumer) {
    WirelessTerminalGuiObject wireless = createWirelessObject(handler, player, swapperStack);
    if (wireless == null || !wireless.rangeCheck()) {
      return;
    }

    IMEMonitor<IAEItemStack> monitor = getItemMonitor(wireless);
    if (monitor == null) {
      return;
    }

    for (IAEItemStack aeStack : monitor.getStorageList()) {
      if (aeStack == null || aeStack.getStackSize() <= 0) {
        continue;
      }

      ItemStack candidate = aeStack.createItemStack();
      if (!isCompatibleCandidate(candidate, baseType, sourceStack)) {
        continue;
      }

      consumer.accept(candidate, clampCount(aeStack.getStackSize()));
    }
  }

  static int extract(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack, @Nonnull ItemStack template,
      int count) {
    if (count <= 0) {
      return 0;
    }

    WirelessTerminalGuiObject wireless = createWirelessObject(handler, player, swapperStack);
    if (wireless == null || !wireless.rangeCheck()) {
      return 0;
    }

    IMEMonitor<IAEItemStack> monitor = getItemMonitor(wireless);
    if (monitor == null) {
      return 0;
    }

    ItemStack requestStack = template.copy();
    requestStack.setCount(count);

    IAEItemStack request = AEItemStack.fromItemStack(requestStack);
    if (request == null) {
      return 0;
    }

    IAEItemStack extracted = monitor.extractItems(request, Actionable.MODULATE, new BaseActionSource());
    if (extracted == null) {
      return 0;
    }

    return clampCount(extracted.getStackSize());
  }

  static @Nonnull ItemStack store(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack,
      @Nonnull ItemStack stackToStore) {
    if (stackToStore.isEmpty()) {
      return ItemStack.EMPTY;
    }

    WirelessTerminalGuiObject wireless = createWirelessObject(handler, player, swapperStack);
    if (wireless == null || !wireless.rangeCheck()) {
      return stackToStore;
    }

    IMEMonitor<IAEItemStack> monitor = getItemMonitor(wireless);
    if (monitor == null) {
      return stackToStore;
    }

    IAEItemStack request = AEItemStack.fromItemStack(stackToStore.copy());
    if (request == null) {
      return stackToStore;
    }

    IAEItemStack remainder = monitor.injectItems(request, Actionable.MODULATE, new BaseActionSource());
    if (remainder == null) {
      return ItemStack.EMPTY;
    }

    return remainder.createItemStack();
  }

  private static boolean isCompatibleCandidate(@Nonnull ItemStack stack, @Nonnull Class<? extends IConduit> baseType, @Nonnull ItemStack sourceStack) {
    if (stack.isEmpty() || !(stack.getItem() instanceof IConduitItem) || ItemUtil.areStacksEqual(stack, sourceStack)) {
      return false;
    }

    IConduitItem conduitItem = (IConduitItem) stack.getItem();
    return conduitItem.getBaseConduitType() == baseType;
  }

  private static int clampCount(long count) {
    return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, count));
  }

  private static @Nullable WirelessTerminalGuiObject createWirelessObject(@Nonnull ItemConduitSwapper handler, @Nonnull EntityPlayer player,
      @Nonnull ItemStack swapperStack) {
    if (swapperStack.isEmpty() || swapperStack.getItem() != handler || handler.getStoredEncryptionKey(swapperStack).isEmpty()) {
      return null;
    }

    return new WirelessTerminalGuiObject((IWirelessTermHandler) handler, swapperStack, player, player.world, -1, 0, 0);
  }

  private static @Nullable IMEMonitor<IAEItemStack> getItemMonitor(@Nonnull WirelessTerminalGuiObject wireless) {
    return wireless.getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
  }

}
