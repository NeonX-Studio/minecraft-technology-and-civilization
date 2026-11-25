package com.nightcity.skills.client;

import com.nightcity.skills.TSkills;
import com.nightcity.skills.api.Skill;
import com.nightcity.skills.api.SkillsApi;
import com.nightcity.skills.data.TalentDef;
import com.nightcity.skills.data.TalentLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class SkillsPageScreen extends Screen {
    private static final int MARGIN_X = 12;
    private static final int MARGIN_Y = 10;
    private static final int HEADER_H = 26;
    private static final int FOOTER_H = 26;
    private static final int SIDEBAR_W = 148;
    private static final int SKILL_ROW_H = 18;

    private record SlotBox(TalentDef.Slot slot, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private Skill selectedSkill = Skill.FORAGING;
    private final List<Identifier> stagedTalents = new ArrayList<>();
    private int scrollOffset = 0;
    private int talentScrollOffset = 0;
    private Button commitButton;
    private Button clearButton;

    public SkillsPageScreen() {
        super(Component.translatable("gui.tcskills.title"));
    }

    @Override
    protected void init() {
        int right = this.width - MARGIN_X;
        int top = MARGIN_Y;
        int btnY = this.height - MARGIN_Y - 22;

        addRenderableWidget(Button.builder(Component.literal("×"), b -> onClose())
                .bounds(right - 22, top + 4, 18, 18)
                .build());

        commitButton = Button.builder(Component.translatable("gui.tcskills.commit"), b -> {
            var player = Minecraft.getInstance().player;
            if (player != null && !stagedTalents.isEmpty()) {
                for (Identifier tid : stagedTalents) {
                    player.connection.sendCommand("tcskills learn " + tid);
                }
                stagedTalents.clear();
                syncActionButtons();
            }
        }).bounds(right - 198, btnY, 96, 18).build();
        addRenderableWidget(commitButton);

        clearButton = Button.builder(Component.translatable("gui.tcskills.clear_staged"), b -> {
            stagedTalents.clear();
            syncActionButtons();
        }).bounds(right - 98, btnY, 90, 18).build();
        addRenderableWidget(clearButton);

        syncActionButtons();
    }

    private void syncActionButtons() {
        boolean hasStaged = !stagedTalents.isEmpty();
        if (commitButton != null) {
            commitButton.active = hasStaged;
        }
        if (clearButton != null) {
            clearButton.active = hasStaged;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null && this.minecraft.level != null) {
            this.extractBlurredBackground(graphics);
        }
        graphics.fillGradient(0, 0, this.width, this.height, 0xD8080C12, 0xEC04070B);

        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;
        int bottom = this.height - MARGIN_Y;

        graphics.fill(left, top, right, bottom, 0xFF151A22);
        graphics.fill(left + 1, top + 1, right - 1, top + HEADER_H, 0xFF1E2632);
        graphics.fill(left + 1, bottom - FOOTER_H, right - 1, bottom - 1, 0xFF1A212B);
        graphics.fill(left + 1, top + HEADER_H, left + SIDEBAR_W, bottom - FOOTER_H, 0xFF12171F);

        graphics.horizontalLine(left + 1, right - 2, top + HEADER_H, 0xFF334155);
        graphics.horizontalLine(left + 1, right - 2, bottom - FOOTER_H, 0xFF334155);
        graphics.verticalLine(left + SIDEBAR_W, top + HEADER_H, bottom - FOOTER_H, 0xFF334155);
        graphics.outline(left, top, right - left, bottom - top, 0xFF475569);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;
        int bottom = this.height - MARGIN_Y;

        graphics.text(this.font, this.title, left + 10, top + 9, 0xFFF8FAFC, false);

        Player player = Minecraft.getInstance().player;
        int growth = player != null ? SkillsApi.get().growthLevel(player) : 1;
        int free = player != null ? SkillsApi.get().freePoints(player) : 1;
        int stagedCost = computeStagedCost();
        String suffix = stagedCost > 0 ? (" (-" + stagedCost + ")") : "";
        String statsTxt = String.format(Locale.ROOT, "G %d  ·  Pts %d%s", growth, free, suffix);
        graphics.text(this.font, statsTxt, right - 30 - this.font.width(statsTxt), top + 9, 0xFFFBBF24, false);

        List<Skill> visibleSkills = visibleSkills();
        int bodyTop = top + HEADER_H + 4;
        int bodyBottom = bottom - FOOTER_H - 4;
        int availH = Math.max(36, bodyBottom - bodyTop);
        int maxRows = Math.max(1, availH / SKILL_ROW_H);
        this.scrollOffset = Math.clamp(this.scrollOffset, 0, Math.max(0, visibleSkills.size() - maxRows));

        int listX = left + 5;
        int listW = SIDEBAR_W - 10;
        graphics.enableScissor(left + 1, bodyTop, left + SIDEBAR_W - 1, bodyBottom);
        for (int i = 0; i < maxRows && (i + scrollOffset) < visibleSkills.size(); i++) {
            Skill s = visibleSkills.get(i + scrollOffset);
            int y = bodyTop + i * SKILL_ROW_H;
            int lvl = player != null ? SkillsApi.get().level(player, s) : 0;
            boolean specialty = player != null && SkillsApi.get().isSpecialty(player, s);
            boolean selected = (s == selectedSkill);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY < y + SKILL_ROW_H - 2;

            int bg = selected ? 0xFF28364A : (hovered ? 0xFF1E2734 : 0xFF171E28);
            graphics.fill(listX, y, listX + listW, y + SKILL_ROW_H - 2, bg);
            if (selected) {
                graphics.fill(listX, y, listX + 3, y + SKILL_ROW_H - 2, 0xFF3B82F6);
                graphics.outline(listX, y, listW, SKILL_ROW_H - 2, 0xFF60A5FA);
            } else {
                graphics.outline(listX, y, listW, SKILL_ROW_H - 2, 0xFF293545);
            }

            String lvlStr = "Lv." + lvl;
            int lvlW = this.font.width(lvlStr);
            String prefix = specialty ? "★ " : "";
            String skillName = this.font.plainSubstrByWidth(
                    prefix + Component.translatable(s.translationKey()).getString(),
                    Math.max(40, listW - lvlW - 12)
            );
            graphics.text(this.font, skillName, listX + 6, y + 3, specialty ? 0xFFFDE68A : 0xFFF1F5F9, false);
            graphics.text(this.font, lvlStr, listX + listW - lvlW - 4, y + 3, selected ? 0xFF93C5FD : 0xFF94A3B8, false);

            int barW = listW - 10;
            int fillW = Math.round(barW * Math.clamp(lvl / 50.0f, 0.0f, 1.0f));
            graphics.fill(listX + 5, y + SKILL_ROW_H - 4, listX + 5 + barW, y + SKILL_ROW_H - 3, 0xFF0F141B);
            if (fillW > 0) {
                graphics.fill(listX + 5, y + SKILL_ROW_H - 4, listX + 5 + fillW, y + SKILL_ROW_H - 3, 0xFF38BDF8);
            }
        }
        graphics.disableScissor();

        int paneX = left + SIDEBAR_W + 8;
        int paneW = Math.max(150, right - 8 - paneX);
        int skillLvl = player != null ? SkillsApi.get().level(player, selectedSkill) : 0;

        graphics.fill(paneX, bodyTop, paneX + paneW, bodyTop + 22, 0xFF1C2532);
        graphics.outline(paneX, bodyTop, paneW, 22, 0xFF334155);
        graphics.text(this.font, Component.translatable(selectedSkill.translationKey()), paneX + 8, bodyTop + 4, 0xFF60A5FA, false);
        String groupMeta = selectedSkill.group().name() + " · Lv." + skillLvl;
        graphics.text(this.font, groupMeta, paneX + paneW - this.font.width(groupMeta) - 8, bodyTop + 4, 0xFFFBBF24, false);
        String effectsStr = this.font.plainSubstrByWidth(
                selectedSkill.primaryEffect() + "  |  " + selectedSkill.secondaryEffect(),
                paneW - 16
        );
        graphics.text(this.font, effectsStr, paneX + 8, bodyTop + 13, 0xFF94A3B8, false);

        int treeTop = bodyTop + 26;
        int treeBottom = bodyBottom;
        int treeH = Math.max(40, treeBottom - treeTop);
        int totalTreeContentH = 6 * 23;
        this.talentScrollOffset = Math.clamp(this.talentScrollOffset, 0, Math.max(0, totalTreeContentH - treeH));
        List<SlotBox> boxes = computeSlotBoxes(paneX, treeTop - talentScrollOffset, paneW);

        TalentLoader.TalentSnapshot snap = TSkills.talents().snapshot();
        Map<TalentDef.Slot, TalentDef> tree = snap.bySkillAndSlot().getOrDefault(selectedSkill, Map.of());
        TalentDef hoveredTalent = null;

        graphics.enableScissor(paneX, treeTop, paneX + paneW, treeBottom);
        int midX = paneX + paneW / 2;
        int baseTop = treeTop - talentScrollOffset;
        graphics.verticalLine(midX, baseTop + 18, baseTop + 5 * 23 + 2, 0xFF334155);
        int leftBranchX = paneX + (paneW - 6) / 4;
        int rightBranchX = paneX + paneW - (paneW - 6) / 4;
        graphics.horizontalLine(leftBranchX, rightBranchX, baseTop + 2 * 23 - 3, 0xFF334155);
        graphics.horizontalLine(leftBranchX, rightBranchX, baseTop + 3 * 23 - 3, 0xFF334155);

        for (SlotBox box : boxes) {
            TalentDef def = tree.get(box.slot());
            boolean hovered = box.contains(mouseX, mouseY) && mouseY >= treeTop && mouseY <= treeBottom;
            boolean staged = def != null && stagedTalents.contains(def.id());
            boolean learned = def != null && player != null && SkillsApi.get().hasTalent(player, def.id());
            boolean meetsLvl = def != null && skillLvl >= def.level();

            int cardBg = learned ? 0xFF132A24 : (staged ? 0xFF2B2314 : (hovered ? 0xFF243042 : 0xFF1A222E));
            int border = learned ? 0xFF10B981 : (staged ? 0xFFF59E0B : (meetsLvl ? 0xFF60A5FA : 0xFF475569));
            graphics.fill(box.x(), box.y(), box.x() + box.w(), box.y() + box.h(), cardBg);
            graphics.outline(box.x(), box.y(), box.w(), box.h(), border);

            if (def != null) {
                String tag = box.slot().jsonKey().toUpperCase(Locale.ROOT);
                String meta = "Lv." + def.level() + " · " + def.cost() + "pt";
                int metaW = this.font.width(meta);
                String rawName = formatIdName(def.id().getPath());
                String line = this.font.plainSubstrByWidth(tag + " · " + rawName, Math.max(32, box.w() - metaW - 14));
                int textCol = learned ? 0xFF6EE7B7 : (staged ? 0xFFFDE68A : (meetsLvl ? 0xFFF8FAFC : 0xFFCBD5E1));
                graphics.text(this.font, line, box.x() + 6, box.y() + 6, textCol, false);
                graphics.text(this.font, meta, box.x() + box.w() - metaW - 6, box.y() + 6, 0xFF94A3B8, false);
                if (hovered) {
                    hoveredTalent = def;
                }
            } else {
                graphics.text(this.font, box.slot().jsonKey().toUpperCase(Locale.ROOT) + " —", box.x() + 6, box.y() + 6, 0xFF64748B, false);
            }
        }
        graphics.disableScissor();

        String footerLegend = stagedTalents.isEmpty()
                ? "● Learned (Green)  ·  ● Staged (Amber)  ·  ● Available (Blue)"
                : ("Staged: " + stagedTalents.size() + " (" + stagedCost + " pt)");
        String fittedLegend = this.font.plainSubstrByWidth(footerLegend, Math.max(60, (right - 208) - (left + 10)));
        graphics.text(this.font, fittedLegend, left + 10, bottom - FOOTER_H + 9, 0xFF94A3B8, false);

        graphics.nextStratum();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (hoveredTalent != null) {
            graphics.setComponentTooltipForNextFrame(this.font, buildTalentTooltip(hoveredTalent, player, skillLvl), mouseX, mouseY);
        }
    }

    private List<SlotBox> computeSlotBoxes(int paneX, int startY, int paneW) {
        int rowStep = 23;
        int cardH = 19;
        int singleW = Math.min(264, paneW - 8);
        int singleX = paneX + (paneW - singleW) / 2;
        int halfW = (singleW - 8) / 2;

        List<SlotBox> out = new ArrayList<>(7);
        out.add(new SlotBox(TalentDef.Slot.T1, singleX, startY, singleW, cardH));
        out.add(new SlotBox(TalentDef.Slot.T2, singleX, startY + rowStep, singleW, cardH));
        out.add(new SlotBox(TalentDef.Slot.T3A, singleX, startY + rowStep * 2, halfW, cardH));
        out.add(new SlotBox(TalentDef.Slot.T3B, singleX + halfW + 8, startY + rowStep * 2, singleW - halfW - 8, cardH));
        out.add(new SlotBox(TalentDef.Slot.T4, singleX, startY + rowStep * 3, singleW, cardH));
        out.add(new SlotBox(TalentDef.Slot.T5, singleX, startY + rowStep * 4, singleW, cardH));
        out.add(new SlotBox(TalentDef.Slot.C, singleX, startY + rowStep * 5, singleW, cardH));
        return out;
    }

    private List<Component> buildTalentTooltip(TalentDef def, Player player, int skillLvl) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(formatIdName(def.id().getPath())).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Slot " + def.slot().jsonKey().toUpperCase(Locale.ROOT)
                + " · Req Lv." + def.level() + " · Cost " + def.cost() + " pt")
                .withStyle(skillLvl >= def.level() ? ChatFormatting.AQUA : ChatFormatting.RED));
        def.eraGate().ifPresent(era ->
                lines.add(Component.literal("Era Gate: " + era.getPath()).withStyle(ChatFormatting.GRAY)));
        def.exclusiveWith().ifPresent(ex ->
                lines.add(Component.literal("Exclusive: " + ex.getPath()).withStyle(ChatFormatting.YELLOW)));
        for (TalentDef.Modifier mod : def.modifiers()) {
            lines.add(Component.literal(String.format(Locale.ROOT, "  %+d%% %s", Math.round(mod.amount() * 100.0f), mod.effect().getPath()))
                    .withStyle(ChatFormatting.GREEN));
        }
        for (TalentDef.OpGrant op : def.ops()) {
            lines.add(Component.literal("  Unlock: " + op.op().getPath()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        boolean learned = player != null && SkillsApi.get().hasTalent(player, def.id());
        if (learned) {
            lines.add(Component.literal("✔ Learned").withStyle(ChatFormatting.GREEN));
        } else if (stagedTalents.contains(def.id())) {
            lines.add(Component.literal("● Staged (click to unstage)").withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.literal("Click to stage talent").withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private int computeStagedCost() {
        int sum = 0;
        Map<Identifier, TalentDef> all = TSkills.talents().snapshot().byId();
        for (Identifier id : stagedTalents) {
            TalentDef d = all.get(id);
            if (d != null) {
                sum += d.cost();
            }
        }
        return sum;
    }

    private static String formatIdName(String path) {
        String[] parts = path.split("[_/]");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int left = MARGIN_X;
        int top = MARGIN_Y;
        int right = this.width - MARGIN_X;
        int bottom = this.height - MARGIN_Y;
        int bodyTop = top + HEADER_H + 4;
        int bodyBottom = bottom - FOOTER_H - 4;

        List<Skill> visible = visibleSkills();
        if (event.x() >= left + 5 && event.x() <= left + SIDEBAR_W - 5 && event.y() >= bodyTop && event.y() <= bodyBottom) {
            int idx = (int) ((event.y() - bodyTop) / SKILL_ROW_H) + scrollOffset;
            if (idx >= 0 && idx < visible.size()) {
                selectedSkill = visible.get(idx);
                talentScrollOffset = 0;
                return true;
            }
        }

        int paneX = left + SIDEBAR_W + 8;
        int paneW = Math.max(150, right - 8 - paneX);
        int treeTop = bodyTop + 26;
        if (event.x() >= paneX && event.x() <= paneX + paneW && event.y() >= treeTop && event.y() <= bodyBottom) {
            Map<TalentDef.Slot, TalentDef> tree = TSkills.talents().snapshot().bySkillAndSlot()
                    .getOrDefault(selectedSkill, Map.of());
            for (SlotBox box : computeSlotBoxes(paneX, treeTop - talentScrollOffset, paneW)) {
                if (box.contains(event.x(), event.y())) {
                    TalentDef def = tree.get(box.slot());
                    if (def != null) {
                        Player player = Minecraft.getInstance().player;
                        if (player != null && SkillsApi.get().hasTalent(player, def.id())) {
                            return true;
                        }
                        if (!stagedTalents.remove(def.id())) {
                            def.exclusiveWith().ifPresent(stagedTalents::remove);
                            stagedTalents.add(def.id());
                        }
                        syncActionButtons();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
        if (delta == 0) {
            return false;
        }
        if (mouseX <= MARGIN_X + SIDEBAR_W) {
            this.scrollOffset = Math.max(0, this.scrollOffset + delta);
        } else {
            this.talentScrollOffset = Math.max(0, this.talentScrollOffset + delta * 14);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<Skill> visibleSkills() {
        boolean beyond = ModList.get() != null && ModList.get().isLoaded("tcbeyond");
        List<Skill> out = new ArrayList<>();
        for (Skill s : Skill.values()) {
            if (s != Skill.ARCANA || beyond) {
                out.add(s);
            }
        }
        return out;
    }
}
