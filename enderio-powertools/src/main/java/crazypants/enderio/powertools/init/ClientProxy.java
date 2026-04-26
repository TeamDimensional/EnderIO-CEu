package crazypants.enderio.powertools.init;

import javax.annotation.Nonnull;

import crazypants.enderio.powertools.EnderIOPowerTools;
import crazypants.enderio.powertools.machine.capbank.network.ClientNetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(modid = EnderIOPowerTools.MODID, value = Side.CLIENT)
public class ClientProxy extends CommonProxy {

  @Override
  public void init(@Nonnull FMLPreInitializationEvent event) {
    super.init(event);
  }

  @Override
  public void init(@Nonnull FMLInitializationEvent event) {
    super.init(event);
    MinecraftForge.EVENT_BUS.register(ClientNetworkManager.getInstance());
  }

  @Override
  public void init(@Nonnull FMLPostInitializationEvent event) {
    super.init(event);
  }
  
  @Override
  public float getPartialTicks() {
    return Minecraft.getMinecraft().getRenderPartialTicks();
  }

}
