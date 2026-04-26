package crazypants.enderio.conduits.lang;

import javax.annotation.Nonnull;

import crazypants.enderio.base.lang.ILang;
import crazypants.enderio.conduits.EnderIOConduits;

public enum Lang implements ILang {

  GUI_CONDUIT_INSERT_MODE(".gui.conduit_insert_mode"),
  GUI_CONDUIT_EXTRACT_MODE(".gui.conduit_extract_mode"),
  GUI_CONDUIT_ENABLED_MODE(".gui.conduit_enabled_mode"),
  GUI_CONDUIT_DISABLED_MODE(".gui.conduit_enabled_mode"),

  GUI_CONDUIT_CHANNEL(".gui.conduit_channel"),
  GUI_ROUND_ROBIN_ENABLED(".gui.round_robin_enabled"),
  GUI_ROUND_ROBIN_DISABLED(".gui.round_robin_disabled"),
  GUI_SELF_FEED_ENABLED(".gui.self_feed_enabled"),
  GUI_SELF_FEED_DISABLED(".gui.self_feed_disabled"),
  GUI_ITEM_FILTER_UPGRADE(".gui.item_filter_upgrade"),
  GUI_ITEM_FUNCTION_UPGRADE(".gui.item_function_upgrade"),
  GUI_ITEM_FUNCTION_UPGRADE_2(".gui.item_function_upgrade2"),
  GUI_ITEM_FUNCTION_UPGRADE_3(".gui.item_function_upgrade3"),
  GUI_ITEM_FUNCTION_UPGRADE_DETAILS(".gui.item_function_upgrade.details"),
  GUI_ITEM_FUNCTION_UPGRADE_DETAILS2(".gui.item_function_upgrade.details2"),
  GUI_SIGNAL_COLOR(".gui.signal_color"),
  GUI_PRIORITY(".gui.priority"),

  GUI_LIQUID_AUTO_EXTRACT(".gui.liquid_auto_extract"),
  GUI_LIQUID_FILTER(".gui.liquid_filter"),
  GUI_LIQUID_WHITELIST(".gui.liquid_whitelist"),
  GUI_LIQUID_BLACKLIST(".gui.liquid_blacklist"),

  GUI_REDSTONE_SIGNAL_STRENGTH(".gui.redstone_signal_strength"),

  FLUID_MILLIBUCKETS_TICK(".fluid.millibuckets_tick"),
  GUI_LIQUID_TOOLTIP_MAX_EXTRACT(".item_liquid_conduit.tooltip.max_extract"),
  GUI_LIQUID_TOOLTIP_MAX_IO(".item_liquid_conduit.tooltip.max_io"),
  ITEM_LIQUID_CONDUIT_UNLOCKED_TYPE(".item_liquid_conduit.unlocked_type"),
  GUI_LIQUID_FUNCTION_UPGRADE_DETAILS(".gui.liquid_function_upgrade.details"),
  GUI_LIQUID_FUNCTION_UPGRADE_DETAILS2(".gui.liquid_function_upgrade.details2"),

  GUI_REDSTONE_CONDUIT_INPUT_MODE(".gui.redstone_conduit_input_mode"),
  GUI_REDSTONE_CONDUIT_OUTPUT_MODE(".gui.redstone_conduit_output_mode"),

  GUI_CONDUIT_BUNDLE_FULL(".gui.conduit_bundle_full"),
  GUI_NETWORK_PARTIALLY_UNLOADED(".gui.conduit.network.unloaded"),

  // Conduit Probe
  GUI_CONDUIT_PROBE_ITEM_HEADING(".gui.conduit_probe.item.heading"),
  GUI_CONDUIT_PROBE_ITEM_HEADING_NO_CONNECTIONS(".gui.conduit_probe.item.heading.no_connections"),

  GUI_CONDUIT_PROBE_NO_ITEMS(".gui.conduit_probe.item.receive.no_items"),
  GUI_CONDUIT_PROBE_NO_ITEM(".gui.conduit_probe.item.receive.no_item"),
  GUI_CONDUIT_PROBE_RECEIVE_ITEMS(".gui.conduit_probe.item.receive.items"),
  GUI_CONDUIT_PROBE_RECEIVE_ITEM(".gui.conduit_probe.item.receive.item"),

  GUI_ENERGY_CONDUIT(".gui.conduit_energy.header"),
  GUI_ITEM_CONDUIT(".gui.conduit_item.header"),
  GUI_FLUID_CONDUIT(".gui.conduit_fluid.header"),
  GUI_REDSTONE_CONDUIT(".gui.conduit_redstone.header"),

  GUI_CONDUIT_PROBE_EXTRACT_NO_ITEM_NO_TARGET(".gui.conduit_probe.item.extract.no_item.no_targets"),
  GUI_CONDUIT_PROBE_EXTRACT_NO_ITEM_TARGETS(".gui.conduit_probe.item.extract.no_item.targets"),
  GUI_CONDUIT_PROBE_EXTRACT_ITEM_NO_TARGET(".gui.conduit_probe.item.extract.item.no_targets"),
  GUI_CONDUIT_PROBE_EXTRACT_ITEM_TARGETS(".gui.conduit_probe.item.extract.item.targets"),

  GUI_CONDUIT_PROBE_POWER_TRACKED_1(".gui.conduit_probe.power.tracked_conduit.line1"),
  GUI_CONDUIT_PROBE_POWER_TRACKED_2(".gui.conduit_probe.power.tracked_conduit.line2"),
  GUI_CONDUIT_PROBE_POWER_TRACKED_3(".gui.conduit_probe.power.tracked_conduit.line3"),
  GUI_CONDUIT_PROBE_POWER_TRACKED_4(".gui.conduit_probe.power.tracked_conduit.line4"),

  GUI_CONDUIT_PROBE_POWER_NETWORK_1(".gui.conduit_probe.power.tracked_network.line1"),
  GUI_CONDUIT_PROBE_POWER_NETWORK_2(".gui.conduit_probe.power.tracked_network.line2"),
  GUI_CONDUIT_PROBE_POWER_NETWORK_3(".gui.conduit_probe.power.tracked_network.line3"),
  GUI_CONDUIT_PROBE_POWER_NETWORK_4(".gui.conduit_probe.power.tracked_network.line4"),
  GUI_CONDUIT_PROBE_POWER_NETWORK_5(".gui.conduit_probe.power.tracked_network.line5"),
  GUI_CONDUIT_PROBE_POWER_NETWORK_6(".gui.conduit_probe.power.tracked_network.line6"),

  GUI_CONDUIT_PROBE_REDSTONE_HEADING(".gui.conduit_probe.redstone.heading"),
  GUI_CONDUIT_PROBE_REDSTONE_HEADING_NO_CONNECTIONS(".gui.conduit_probe.redstone.heading.no_connections"),

  GUI_CONDUIT_PROBE_REDSTONE_STRONG(".gui.conduit_probe.redstone.strong"),
  GUI_CONDUIT_PROBE_REDSTONE_WEAK(".gui.conduit_probe.redstone.weak"),
  GUI_CONDUIT_PROBE_REDSTONE_EXTERNAL(".gui.conduit_probe.redstone.external"),

  ;

  private final @Nonnull String key;

  private Lang(@Nonnull String key) {
    if (key.startsWith(".")) {
      this.key = getLang().addPrefix(key.substring(1));
    } else {
      this.key = key;
    }
  }

  @Override
  @Nonnull
  public String getKey() {
    return key;
  }

  @Override
  @Nonnull
  public com.enderio.core.common.Lang getLang() {
    return EnderIOConduits.lang;
  }

  static {
    for (Lang text : values()) {
      text.checkTranslation();
    }
  }

}
