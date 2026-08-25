# Changelog

## [5.5.1] - 2026-08-25

_Ender Core version: 0.5.83_

### Tweaks

- Added simplified chinese translations for Conduit Swapper (thank you ZHAY!)

### Bugfixes

- Making Hootch in Vat will no longer always output 70 mb of Hootch, disregarding input multipliers
- Fixed a crash when filling an already full tank with a bucket

## [5.5.0] - 2026-08-22

_Ender Core version: 0.5.82_

### Features

- Added a Conduit Swapper tool to replace conduit lines with other tiers of conduits! (Thank you Aedial!)

### Tweaks
- Items can now be shift-clicked into the conduit filters from the inventory. (Thank you Aedial!)
- Item counts in the Limited Item Filter can now be shift-scrolled (for +10/-10 per wheel event) or ctrl-scrolled (for x2/x0.5 per wheel event).
- Scrolling down items in the filters will no longer put them below 1 item.

### Bugfixes

- Tools such as Water Sigil from Blood Magic can no longer dupe arbitrary fluids in EnderIO's fluid tanks.
- Fixed a GL state leak caused by rendering of entities inside an active Telepad.

## [5.4.2] - 2026-05-31

_Ender Core version: 0.5.81_

### Bugfixes

- Fixed the buttons in Advanced Item Filter and Lava-filled Tank not responding to a click correctly
- Fixed the conduit tick rate config having a cryptic comment
- Fixed a regression related to Enhanced machines
- Newly crafted Conduit Probes will now work correctly

## [5.4.1] - 2026-05-24

### Features

- Added Simplified Chinese translations (from ZHAY10086)
  - Ender Core configuration keys currently do not have these translations

### Tweaks

- Added a Global Power Multiplier config option
- SAG Mill Grinding Balls will no longer try to increase the output's stack size beyond the maximum item's stack capacity

### Bugfixes

- SAG Mill will now consume grinding balls before starting to execute a recipe for the first time
- Conduit Probe can now be safely put into an Omni-Wand
- Fixed several config options missing localization keys
- Fixed Gas Conduits missing localization keys
- Fixed the config window not opening when Mekanism is not installed

## [5.4.0] - 2026-05-01

_Ender Core version: 0.5.80_

### Build

- Migrated to RetroFuturaGradle
- Separated Powertools into its own subproject

### Features

- Added a Slot Switch option to Item Conduits
- The Item Conduits' Eco mode can now be disabled on a connection basis
- Conduits can now be configured to extract slower than twice per second on a connection basis
- The color pickers across the mod now use a popup UI instead of a button to cycle through all the colors
- Impulse Hopper's filters can now be configured by pulling items from JEI

### Tweaks

- EnderIO will no longer mention Cleanroom as an invalid base mod
- Machines now expose an unsided capability for the fans of tools such as Storage Scanner and Super Factory Manager
- EnderIO will no longer generate pdf files for its configs
- Adjusted the precise logic of conduit extraction to increase performance
- Machines are now able to auto-push and auto-pull on the same tick
- Pressurized and Ender Fluid Conduits now only pull by default (similar to Item Conduits) instead of pushing and pulling
- Limited Item Filter's stack size can now be increased past 64 without abusing bugs in other mods
- Adjusted power curves for several machines (i.e. Slice & Splice)
- Added a config option to let capacitor levels go above 10
- Machines with capacitor levels above 6 now work proportionally faster
- Machines with extremely high RF/t rates will no longer overconsume power to perform their recipe

### Bugfixes

- Pushing into the Impulse Hopper's push-only slot will no longer void items
- Impulse Hopper now validates the NBT of items inserted into it
- Having more than 1 Speed Extract Downgrade now decreases the extracted chunk size instead of increasing it
- The Enhanced Vat's efficiency scaler now works correctly
- Sending specially crafted packets through the Teleporter subsystem will no longer let the player to teleport anywhere they want
