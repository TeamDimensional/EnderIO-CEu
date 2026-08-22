package crazypants.enderio.conduits.item.conduitswapper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.api.client.gui.IAdvancedTooltipProvider;
import com.enderio.core.client.handlers.SpecialTooltipHandler;
import com.enderio.core.common.BlockEnder;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import appeng.core.localization.GuiText;
import appeng.util.ConfigManager;
import crazypants.enderio.api.IModObject;
import crazypants.enderio.api.tool.IHideFacades;
import crazypants.enderio.base.EnderIOTab;
import crazypants.enderio.base.render.IHaveRenderers;
import crazypants.enderio.conduits.conduit.TileConduitBundle;
import crazypants.enderio.conduits.network.PacketHandler;
import crazypants.enderio.conduits.network.PacketRequestConduitSwapper;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.Optional.Interface;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Optional.InterfaceList({ @Interface(iface = "appeng.api.features.IWirelessTermHandler", modid = "appliedenergistics2") })
public class ItemConduitSwapper extends Item implements IAdvancedTooltipProvider, IHaveRenderers, IHideFacades, IWirelessTermHandler {

  static final String MODID_AE2 = "appliedenergistics2";
  static Boolean isAE2Loaded = null;

  public static ItemConduitSwapper create(@Nonnull IModObject modObject, @Nullable Block block) {
    return new ItemConduitSwapper(modObject);
  }

  protected ItemConduitSwapper(@Nonnull IModObject modObject) {
    setCreativeTab(EnderIOTab.tabEnderIOConduits);
    modObject.apply(this);
    setMaxStackSize(1);
  }

  @Override
  public @Nonnull EnumActionResult onItemUseFirst(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull EnumFacing side, float hitX, float hitY, float hitZ, @Nonnull EnumHand hand) {
    if (world.isRemote) {
      ItemStack sourceStack = ConduitSwapperLogic.resolveClientSourceStack(world, pos, player);
      if (sourceStack.isEmpty()) {
        return EnumActionResult.PASS;
      }

      TileConduitBundle bundle = BlockEnder.getAnyTileEntitySafe(world, pos, TileConduitBundle.class);
      if (bundle != null) {
        PacketHandler.sendToServer(new PacketRequestConduitSwapper(bundle, hand, sourceStack));
        return EnumActionResult.SUCCESS;
      }

      return EnumActionResult.PASS;
    }

    return BlockEnder.getAnyTileEntitySafe(world, pos, TileConduitBundle.class) != null ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
  }

  @Override
  public boolean shouldCauseReequipAnimation(@Nonnull ItemStack oldStack, @Nonnull ItemStack newStack, boolean slotChanged) {
    return !ItemStack.areItemsEqual(oldStack, newStack);
  }

  @Override
  public boolean shouldHideFacades(@Nonnull ItemStack stack, @Nonnull EntityPlayer player) {
    return true;
  }

  public static boolean isAE2Loaded() {
    if (isAE2Loaded == null) {
      isAE2Loaded = Loader.isModLoaded(MODID_AE2);
    }

    return isAE2Loaded;
  }

  public boolean canAccessWireless(@Nonnull EntityPlayer player, @Nonnull ItemStack swapperStack) {
    return isAE2Loaded() && !getStoredEncryptionKey(swapperStack).isEmpty() && ConduitSwapperWirelessHelper.hasAccess(this, player, swapperStack);
  }

  public @Nonnull String getStoredEncryptionKey(@Nonnull ItemStack stack) {
    NBTTagCompound tag = stack.getTagCompound();
    return tag != null ? tag.getString("encryptionKey") : "";
  }

  private @Nonnull NBTTagCompound getOrCreateData(@Nonnull ItemStack stack) {
    if (!stack.hasTagCompound()) {
      stack.setTagCompound(new NBTTagCompound());
    }

    return stack.getTagCompound();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void registerRenderers(@Nonnull IModObject modObject) {
    ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(modObject.getRegistryName(), "inventory"));
  }

  @Override
  public void addBasicEntries(@Nonnull ItemStack itemstack, @Nullable EntityPlayer entityplayer, @Nonnull List<String> list,
      boolean flag) {
  }

  @Override
  public void addCommonEntries(@Nonnull ItemStack itemstack, @Nullable EntityPlayer entityplayer, @Nonnull List<String> list,
      boolean flag) {
    if (!isAE2Loaded()) {
      return;
    }

    if (getStoredEncryptionKey(itemstack).isEmpty()) {
      list.add(TextFormatting.RED + GuiText.Unlinked.getLocal());
    } else {
      list.add(TextFormatting.GREEN + GuiText.Linked.getLocal());
    }
  }

  @Override
  public void addDetailedEntries(@Nonnull ItemStack itemstack, @Nullable EntityPlayer entityplayer, @Nonnull List<String> list,
      boolean flag) {
    ArrayList<String> details = new ArrayList<>();
    SpecialTooltipHandler.addDetailedTooltipFromResources(details, getUnlocalizedName());
    list.addAll(details);
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public boolean canHandle(ItemStack is) {
    return !is.isEmpty() && is.getItem() == this;
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
    return true;
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public boolean hasPower(EntityPlayer player, double amount, ItemStack is) {
    return true;
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public IConfigManager getConfigManager(ItemStack target) {
    ConfigManager configManager = new ConfigManager((manager, settingName, newValue) -> manager.writeToNBT(getOrCreateData(target)));
    configManager.readFromNBT(getOrCreateData(target).copy());
    return configManager;
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public IGuiHandler getGuiHandler(ItemStack is) {
    return null;
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public String getEncryptionKey(ItemStack item) {
    return getStoredEncryptionKey(item);
  }

  @Optional.Method(modid = MODID_AE2)
  @Override
  public void setEncryptionKey(ItemStack item, String encKey, String name) {
    NBTTagCompound tag = getOrCreateData(item);
    tag.setString("encryptionKey", encKey);
    tag.setString("name", name);
  }

}
