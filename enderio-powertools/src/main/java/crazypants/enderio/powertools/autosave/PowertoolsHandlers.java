package crazypants.enderio.powertools.autosave;

import crazypants.enderio.base.autosave.BaseHandlers;
import crazypants.enderio.base.events.EnderIOLifecycleEvent;
import crazypants.enderio.powertools.EnderIOPowerTools;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@EventBusSubscriber(modid = EnderIOPowerTools.MODID)
public class PowertoolsHandlers extends BaseHandlers {

  @SubscribeEvent
  public static void register(EnderIOLifecycleEvent.PreInit event) {
    REGISTRY.register(new HandleStatCollector());
  }
}