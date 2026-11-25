package com.nightcity.technology.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TechDefinition(
        Identifier id,
        String owner,
        TechEra era,
        int rp,
        boolean keystone,
        int threshold,
        List<KnowledgeSource> sources,
        List<Identifier> prerequisites,
        TechUnlocks unlocks,
        TreeHint tree,
        Identifier icon,
        boolean diplomatic,
        Optional<Identifier> fallbackEffect,
        boolean fallback,
        boolean disabled,
        Optional<String> originalOwner
) {
    public record KnowledgeSource(
            KnowledgeSourceType type,
            Identifier trigger,
            Map<String, String> params,
            int amount,
            Optional<Integer> dailyCap,
            Optional<String> hint
    ) {
        public static final Codec<KnowledgeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
                KnowledgeSourceType.CODEC.fieldOf("type").forGetter(KnowledgeSource::type),
                Identifier.CODEC.fieldOf("trigger").forGetter(KnowledgeSource::trigger),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("params", Map.of()).forGetter(KnowledgeSource::params),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("amount", 1).forGetter(KnowledgeSource::amount),
                ExtraCodecs.POSITIVE_INT.optionalFieldOf("daily_cap").forGetter(KnowledgeSource::dailyCap),
                Codec.STRING.optionalFieldOf("hint").forGetter(KnowledgeSource::hint)
        ).apply(i, KnowledgeSource::new));
    }

    public record KnowledgeBlock(int threshold, List<KnowledgeSource> sources) {
        public static final Codec<KnowledgeBlock> CODEC = RecordCodecBuilder.create(i -> i.group(
                ExtraCodecs.POSITIVE_INT.fieldOf("threshold").forGetter(KnowledgeBlock::threshold),
                KnowledgeSource.CODEC.listOf().optionalFieldOf("sources", List.of()).forGetter(KnowledgeBlock::sources)
        ).apply(i, KnowledgeBlock::new));
    }

    public record TechUnlocks(
            List<Identifier> recipes,
            List<Identifier> items,
            List<Identifier> blocks,
            List<Identifier> features,
            List<Identifier> jobs
    ) {
        public static final TechUnlocks EMPTY = new TechUnlocks(List.of(), List.of(), List.of(), List.of(), List.of());
        public static final Codec<TechUnlocks> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.listOf().optionalFieldOf("recipes", List.of()).forGetter(TechUnlocks::recipes),
                Identifier.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(TechUnlocks::items),
                Identifier.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(TechUnlocks::blocks),
                Identifier.CODEC.listOf().optionalFieldOf("features", List.of()).forGetter(TechUnlocks::features),
                Identifier.CODEC.listOf().optionalFieldOf("jobs", List.of()).forGetter(TechUnlocks::jobs)
        ).apply(i, TechUnlocks::new));
    }

    public record TreeHint(String lane, int rowHint) {
        public static final TreeHint DEFAULT = new TreeHint("tools", 0);
        public static final Codec<TreeHint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("lane", "tools").forGetter(TreeHint::lane),
                Codec.INT.optionalFieldOf("row_hint", 0).forGetter(TreeHint::rowHint)
        ).apply(i, TreeHint::new));
    }

    public record RawTechJson(
            String owner,
            String rawEra,
            int rp,
            boolean keystone,
            KnowledgeBlock knowledge,
            List<Identifier> prerequisites,
            TechUnlocks unlocks,
            TreeHint tree,
            Identifier icon,
            boolean diplomatic,
            Optional<Identifier> fallbackEffect,
            boolean fallback,
            boolean disabled,
            Optional<String> originalOwner
    ) {
        public static final Codec<RawTechJson> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("owner").forGetter(RawTechJson::owner),
                Codec.STRING.fieldOf("era").forGetter(RawTechJson::rawEra),
                ExtraCodecs.POSITIVE_INT.fieldOf("rp").forGetter(RawTechJson::rp),
                Codec.BOOL.optionalFieldOf("keystone", false).forGetter(RawTechJson::keystone),
                KnowledgeBlock.CODEC.fieldOf("knowledge").forGetter(RawTechJson::knowledge),
                Identifier.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(RawTechJson::prerequisites),
                TechUnlocks.CODEC.optionalFieldOf("unlocks", TechUnlocks.EMPTY).forGetter(RawTechJson::unlocks),
                TreeHint.CODEC.optionalFieldOf("tree", TreeHint.DEFAULT).forGetter(RawTechJson::tree),
                Identifier.CODEC.optionalFieldOf("icon", Identifier.withDefaultNamespace("book")).forGetter(RawTechJson::icon),
                Codec.BOOL.optionalFieldOf("diplomatic", false).forGetter(RawTechJson::diplomatic),
                Identifier.CODEC.optionalFieldOf("fallback_effect").forGetter(RawTechJson::fallbackEffect),
                Codec.BOOL.optionalFieldOf("fallback", false).forGetter(RawTechJson::fallback),
                Codec.BOOL.optionalFieldOf("disabled", false).forGetter(RawTechJson::disabled),
                Codec.STRING.optionalFieldOf("original_owner").forGetter(RawTechJson::originalOwner)
        ).apply(i, RawTechJson::new));
    }

    public TechDefinition {
        sources = List.copyOf(sources);
        prerequisites = List.copyOf(prerequisites);
    }

    public TechDefinition withPrerequisites(List<Identifier> updatedPrerequisites) {
        return new TechDefinition(
                id, owner, era, rp, keystone, threshold, sources,
                updatedPrerequisites, unlocks, tree, icon, diplomatic, fallbackEffect, fallback, disabled, originalOwner
        );
    }
}
