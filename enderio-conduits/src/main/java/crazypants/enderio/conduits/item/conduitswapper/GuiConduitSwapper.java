package crazypants.enderio.conduits.item.conduitswapper;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.enderio.core.common.BlockEnder;

import crazypants.enderio.conduits.conduit.TileConduitBundle;
import crazypants.enderio.conduits.lang.Lang;
import crazypants.enderio.conduits.network.PacketApplyConduitSwapper;
import crazypants.enderio.conduits.network.PacketHandler;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiConduitSwapper extends GuiScreen {

  private static final int SLOT_SIZE = 16;
  private static final int SLOT_SPACING = 20;
  private static final int PANEL_PADDING = 14;
  private static final int PANEL_MIN_SIZE = 48;
  private static final int PANEL_COLOR = 0xA0000000;
  private static final int SOURCE_COLOR = 0x40505050;
  private static final int SOURCE_HOVER_COLOR = 0x70505050;
  private static final int SUCCESS_COLOR = 0x6030A050;
  private static final int SUCCESS_HOVER_COLOR = 0x9030A050;
  private static final int FAILURE_COLOR = 0x60A03040;
  private static final int FAILURE_HOVER_COLOR = 0x90A03040;
  private static final int ARROW_COLOR = 0xFFFFFFFF;

  private final @Nonnull ConduitSwapperPayload payload;
  private final @Nonnull List<CandidateSlot> candidateSlots = new ArrayList<>();

  private Rectangle leftPanel = new Rectangle();
  private Rectangle rightPanel = new Rectangle();
  private Rectangle sourceSlot = new Rectangle();

  private @Nullable ConduitSwapperPayload.Candidate lastHoveredCandidate;

  public GuiConduitSwapper(@Nonnull ConduitSwapperPayload payload) {
    this.payload = payload;
  }

  @Override
  public void initGui() {
    super.initGui();

    candidateSlots.clear();

    int candidateCount = payload.getCandidates().size();
    int columnCount = Math.max(1, (int) Math.ceil(Math.sqrt(candidateCount)));
    int rowCount = Math.max(1, (int) Math.ceil(candidateCount / (double) columnCount));

    int gridWidth = columnCount * SLOT_SIZE + Math.max(0, columnCount - 1) * (SLOT_SPACING - SLOT_SIZE);
    int gridHeight = rowCount * SLOT_SIZE + Math.max(0, rowCount - 1) * (SLOT_SPACING - SLOT_SIZE);

    int panelWidth = Math.max(PANEL_MIN_SIZE, gridWidth + PANEL_PADDING * 2);
    int panelHeight = Math.max(PANEL_MIN_SIZE, gridHeight + PANEL_PADDING * 2);
    int arrowGap = 42;
    int totalWidth = panelWidth * 2 + arrowGap;

    int left = width / 2 - totalWidth / 2;
    int top = height / 2 - panelHeight / 2;

    leftPanel = new Rectangle(left, top, panelWidth, panelHeight);
    rightPanel = new Rectangle(left + panelWidth + arrowGap, top, panelWidth, panelHeight);
    sourceSlot = new Rectangle(leftPanel.x + (leftPanel.width - SLOT_SIZE) / 2, leftPanel.y + (leftPanel.height - SLOT_SIZE) / 2, SLOT_SIZE, SLOT_SIZE);

    int gridLeft = rightPanel.x + (rightPanel.width - gridWidth) / 2;
    int gridTop = rightPanel.y + (rightPanel.height - gridHeight) / 2;

    for (int index = 0; index < candidateCount; index++) {
      int row = index / columnCount;
      int column = index % columnCount;
      int slotX = gridLeft + column * SLOT_SPACING;
      int slotY = gridTop + row * SLOT_SPACING;

      candidateSlots.add(new CandidateSlot(payload.getCandidates().get(index), new Rectangle(slotX, slotY, SLOT_SIZE, SLOT_SIZE)));
    }
  }

  @Override
  public boolean doesGuiPauseGame() {
    return false;
  }

  @Override
  protected void keyTyped(char typedChar, int keyCode) throws IOException {
    if (keyCode == 1 || keyCode == mc.gameSettings.keyBindInventory.getKeyCode()) {
      mc.player.closeScreen();
      return;
    }

    super.keyTyped(typedChar, keyCode);
  }

  @Override
  protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
    CandidateSlot hoveredCandidate = getHoveredCandidate(mouseX, mouseY);
    if (mouseButton == 0 && hoveredCandidate != null) {
      if (canReplace(hoveredCandidate.candidate)) {
        TileConduitBundle bundle = BlockEnder.getAnyTileEntitySafe(mc.world, payload.getOrigin(), TileConduitBundle.class);
        if (bundle != null) {
          PacketHandler.sendToServer(new PacketApplyConduitSwapper(bundle, payload.getHand(), payload.getSourceStack(), hoveredCandidate.candidate.getStack()));
          mc.player.closeScreen();
          return;
        }
      }
    }

    if (!leftPanel.contains(mouseX, mouseY) && !rightPanel.contains(mouseX, mouseY)) {
      mc.player.closeScreen();
      return;
    }

    super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    drawPanel(leftPanel, PANEL_COLOR);
    drawPanel(rightPanel, PANEL_COLOR);
    drawArrow();

    drawRect(sourceSlot.x - 3, sourceSlot.y - 3, sourceSlot.x + sourceSlot.width + 3, sourceSlot.y + sourceSlot.height + 3,
        sourceSlot.contains(mouseX, mouseY) ? SOURCE_HOVER_COLOR : SOURCE_COLOR);

    CandidateSlot hoveredCandidate = getHoveredCandidate(mouseX, mouseY);
    if (hoveredCandidate != null) {
      lastHoveredCandidate = hoveredCandidate.candidate;
    }

    for (CandidateSlot candidateSlot : candidateSlots) {
      boolean hovered = candidateSlot.bounds.contains(mouseX, mouseY);
      boolean canReplace = canReplace(candidateSlot.candidate);
      int color = canReplace ? (hovered ? SUCCESS_HOVER_COLOR : SUCCESS_COLOR) : (hovered ? FAILURE_HOVER_COLOR : FAILURE_COLOR);

      drawRect(candidateSlot.bounds.x - 1, candidateSlot.bounds.y - 1, candidateSlot.bounds.x + candidateSlot.bounds.width + 1,
          candidateSlot.bounds.y + candidateSlot.bounds.height + 1, color);
    }

    String title = Lang.GUI_CONDUIT_SWAPPER_TITLE.get(payload.getLineLength());
    drawCenteredString(fontRenderer, title, width / 2, leftPanel.y - fontRenderer.FONT_HEIGHT - 8, 0xFFFFFF);

    // Render the items on top of the panels
    RenderHelper.enableGUIStandardItemLighting();
    itemRender.zLevel = 100.0F;
    itemRender.renderItemAndEffectIntoGUI(payload.getSourceStack(), sourceSlot.x, sourceSlot.y);
    for (CandidateSlot candidateSlot : candidateSlots) {
      itemRender.renderItemAndEffectIntoGUI(candidateSlot.candidate.getStack(), candidateSlot.bounds.x, candidateSlot.bounds.y);
    }
    itemRender.zLevel = 0.0F;
    RenderHelper.disableStandardItemLighting();

    GlStateManager.disableLighting();

    // Render the counts on top of the items
    GlStateManager.pushMatrix();
    GlStateManager.translate(0, 0, 200);
    renderCount(sourceSlot.x, sourceSlot.y, payload.getLineLength(), 0xFFFFFF);
    if (!payload.hasInfiniteResources()) {
      for (CandidateSlot candidateSlot : candidateSlots) {
        renderCount(candidateSlot.bounds.x, candidateSlot.bounds.y, candidateSlot.candidate.getTotalCount(), 0xFFFFFF);
      }
    }
    GlStateManager.popMatrix();

    // Render the tooltips on top of everything else
    CandidateSlot tooltipCandidate = getHoveredCandidate(mouseX, mouseY);
    if (tooltipCandidate != null) {
      drawHoveringText(getCandidateTooltip(tooltipCandidate.candidate), mouseX, mouseY);
    } else if (sourceSlot.contains(mouseX, mouseY)) {
      drawHoveringText(getSourceTooltip(), mouseX, mouseY);
    }
  }

  private void drawPanel(@Nonnull Rectangle panel, int color) {
    drawRect(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height, color);
  }

  private void drawArrow() {
    int startX = leftPanel.x + leftPanel.width + 12;
    int endX = rightPanel.x - 12;
    int centerY = leftPanel.y + leftPanel.height / 2;

    drawRect(startX, centerY - 2, endX - 8, centerY + 2, ARROW_COLOR);
    for (int index = 0; index < 8; index++) {
      drawRect(endX - 8 + index, centerY - 6 + index, endX - 7 + index, centerY + 7 - index, ARROW_COLOR);
    }
  }

  private void renderCount(int x, int y, int count, int color) {
    String text = shortenCount(count);
    int textWidth = fontRenderer.getStringWidth(text);

    GlStateManager.pushMatrix();
    GlStateManager.translate(0, 0, 200);
    GlStateManager.scale(0.5F, 0.5F, 1.0F);
    fontRenderer.drawStringWithShadow(text, (x + SLOT_SIZE) * 2 - textWidth - 2, (y + SLOT_SIZE) * 2 - 9, color);
    GlStateManager.popMatrix();
  }

  private @Nonnull List<String> getSourceTooltip() {
    List<String> tooltip = payload.getSourceStack().getTooltip(mc.player, getTooltipFlag());

    if (lastHoveredCandidate != null) {
      tooltip.add(TextFormatting.AQUA + Lang.GUI_CONDUIT_SWAPPER_ORIGINAL_REPLACE.get(payload.getLineLength(),
          lastHoveredCandidate.getStack().getDisplayName()));
    } else {
      tooltip.add(TextFormatting.GRAY + Lang.GUI_CONDUIT_SWAPPER_ORIGINAL_PICK.get(payload.getLineLength()));
    }

    if (payload.warnsFluidLoss()) {
      tooltip.add(TextFormatting.YELLOW + Lang.GUI_CONDUIT_SWAPPER_FLUID_WARNING.get());
    }

    return tooltip;
  }

  private @Nonnull List<String> getCandidateTooltip(@Nonnull ConduitSwapperPayload.Candidate candidate) {
    List<String> tooltip = candidate.getStack().getTooltip(mc.player, getTooltipFlag());

    if (payload.hasInfiniteResources()) {
      tooltip.add(TextFormatting.GREEN + Lang.GUI_CONDUIT_SWAPPER_CREATIVE_REPLACE.get(payload.getLineLength()));
    } else if (candidate.canReplace(payload.getLineLength())) {
      tooltip.add(TextFormatting.GREEN + Lang.GUI_CONDUIT_SWAPPER_CAN_REPLACE.get(payload.getLineLength(), candidate.getTotalCount()));
    } else {
      tooltip.add(TextFormatting.RED + Lang.GUI_CONDUIT_SWAPPER_NOT_ENOUGH.get(payload.getLineLength() - candidate.getTotalCount()));
    }

    if (!payload.hasInfiniteResources() && candidate.getNetworkCount() > 0) {
      tooltip.add(TextFormatting.GRAY + Lang.GUI_CONDUIT_SWAPPER_AE2_BONUS.get(candidate.getNetworkCount()));
    }

    return tooltip;
  }

  private boolean canReplace(@Nonnull ConduitSwapperPayload.Candidate candidate) {
    return payload.hasInfiniteResources() || candidate.canReplace(payload.getLineLength());
  }

  private @Nonnull ITooltipFlag getTooltipFlag() {
    return mc.gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL;
  }

  private @Nullable CandidateSlot getHoveredCandidate(int mouseX, int mouseY) {
    for (CandidateSlot candidateSlot : candidateSlots) {
      if (candidateSlot.bounds.contains(mouseX, mouseY)) {
        return candidateSlot;
      }
    }

    return null;
  }

  private @Nonnull String shortenCount(int count) {
    if (count < 1000) {
      return Integer.toString(count);
    }

    String[] suffixes = { "k", "M", "G", "T" };
    double value = count;
    int suffixIndex = -1;

    while (value >= 1000 && suffixIndex + 1 < suffixes.length) {
      value /= 1000;
      suffixIndex++;
    }

    if (suffixIndex < 0) {
      return Integer.toString(count);
    }

    if (value >= 100 || Math.abs(value - Math.rint(value)) < 0.05) {
      return Integer.toString((int) Math.round(value)) + suffixes[suffixIndex];
    }

    return String.format(Locale.ROOT, "%.1f%s", value, suffixes[suffixIndex]);
  }

  private static class CandidateSlot {

    private final @Nonnull ConduitSwapperPayload.Candidate candidate;
    private final @Nonnull Rectangle bounds;

    private CandidateSlot(@Nonnull ConduitSwapperPayload.Candidate candidate, @Nonnull Rectangle bounds) {
      this.candidate = candidate;
      this.bounds = bounds;
    }

  }

}
