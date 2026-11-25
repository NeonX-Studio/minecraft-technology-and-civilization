package com.nightcity.nations.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface NationsApi {
    enum OpinionTier {
        HOSTILE,
        COLD,
        NEUTRAL,
        FRIENDLY,
        CLOSE;

        public static OpinionTier fromOpinion(double opinion) {
            if (opinion <= -60.0D) {
                return HOSTILE;
            }
            if (opinion <= -20.0D) {
                return COLD;
            }
            if (opinion < 20.0D) {
                return NEUTRAL;
            }
            if (opinion < 60.0D) {
                return FRIENDLY;
            }
            return CLOSE;
        }
    }

    record RelationView(double opinion, double trust, double fear, OpinionTier tier) {
        public static RelationView neutral() {
            return new RelationView(0.0D, 50.0D, 0.0D, OpinionTier.NEUTRAL);
        }
    }

    static NationsApi get() {
        return Holder.current;
    }

    static void install(NationsApi impl) {
        Holder.current = impl == null ? Holder.EMPTY : impl;
    }

    Collection<NationState> activeNations();

    Optional<NationState> nation(UUID civId);

    Optional<UUID> regionOwner(ResourceKey<Level> dim, long regionKey);

    RelationView relation(UUID a, UUID b);

    boolean hasContact(UUID a, UUID b);

    Set<UUID> contactedCivs(UUID civ);

    void addOpinionModifier(UUID from, UUID to, Identifier id, double value, int halfLifeDays);

    final class Holder {
        static final NationsApi EMPTY = new NationsApi() {
            @Override
            public Collection<NationState> activeNations() {
                return List.of();
            }

            @Override
            public Optional<NationState> nation(UUID civId) {
                return Optional.empty();
            }

            @Override
            public Optional<UUID> regionOwner(ResourceKey<Level> dim, long regionKey) {
                return Optional.empty();
            }

            @Override
            public RelationView relation(UUID a, UUID b) {
                return RelationView.neutral();
            }

            @Override
            public boolean hasContact(UUID a, UUID b) {
                return false;
            }

            @Override
            public Set<UUID> contactedCivs(UUID civ) {
                return Set.of();
            }

            @Override
            public void addOpinionModifier(UUID from, UUID to, Identifier id, double value, int halfLifeDays) {
            }
        };

        static volatile NationsApi current = EMPTY;

        private Holder() {
        }
    }
}
