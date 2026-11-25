package com.nightcity.tcivilization.world;

import com.nightcity.tcivilization.api.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldApisFallbackTest {

    @AfterEach
    void tearDown() {
        TcWorldApis.resetForTests();
    }

    @Test
    void fallbackDefaultsRemainSafeAndNeutralWhenWildlandsAbsent() {
        TcWorldApis.resetForTests();
        assertFalse(TcWorldApis.planet().isPlanetActive());
        assertEquals(PlanetApi.PLANET_WIDTH, TcWorldApis.planet().planetWidth());
        assertEquals(123_456, TcWorldApis.planet().wrapX(123_456));
        assertEquals(100, TcWorldApis.planet().wrappedDx(100, 200));
        assertEquals(64, TcWorldApis.planet().surfaceY(0, 0));

        ClimateNormals normals = TcWorldApis.climate().normals(BlockPos.ZERO);
        assertEquals("Cfb", normals.koppen());
        assertEquals(12, normals.tempMonthlyC().length);
        assertEquals(12, normals.precipMonthlyMm().length);

        WeatherState weather = TcWorldApis.weather().current(null, BlockPos.ZERO);
        assertEquals(PrecipType.NONE, weather.type());

        StrataColumn strata = TcWorldApis.geo().strata(0, 0);
        assertEquals(GeoProvince.CRATON_PLATFORM, strata.province());
        assertEquals(Identifier.withDefaultNamespace("stone"), TcWorldApis.geo().rockAt(0, 32, 0));

        SiteScore suit = TcWorldApis.suitability().citySiteScore(new BlockPos(0, 68, 0));
        assertTrue(suit.total() >= 60);

        HazardProfile haz = TcWorldApis.hazards().profile(BlockPos.ZERO);
        assertEquals(0.0f, TcWorldApis.hazards().hazardLevel(BlockPos.ZERO), 1e-5f);
        assertNotNull(haz);

        ProspectReading reading = TcWorldApis.prospecting().queryAbstract(
                java.util.UUID.randomUUID(), ProspectMethod.SENSES, BlockPos.ZERO, 64);
        assertTrue(reading.anomalies().isEmpty());

        LocalSeason sSpring = TcWorldApis.seasons().season(null, BlockPos.ZERO, 4);
        LocalSeason sSummer = TcWorldApis.seasons().season(null, BlockPos.ZERO, 16);
        assertEquals(Season.SPRING, sSpring.astronomical());
        assertEquals(Season.SUMMER, sSummer.astronomical());
    }
}
