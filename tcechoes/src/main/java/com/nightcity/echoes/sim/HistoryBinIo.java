package com.nightcity.echoes.sim;

import com.nightcity.echoes.TEchoes;
import com.nightcity.echoes.api.AncientMine;
import com.nightcity.echoes.api.ArchStyle;
import com.nightcity.echoes.api.Burial;
import com.nightcity.echoes.api.Culture;
import com.nightcity.echoes.api.Deity;
import com.nightcity.echoes.api.EdgeType;
import com.nightcity.echoes.api.EventEdge;
import com.nightcity.echoes.api.EventType;
import com.nightcity.echoes.api.Extinction;
import com.nightcity.echoes.api.HistoryEvent;
import com.nightcity.echoes.api.LanguageTemplate;
import com.nightcity.echoes.api.LegendKind;
import com.nightcity.echoes.api.LegendSeed;
import com.nightcity.echoes.api.LiveHistoryEntry;
import com.nightcity.echoes.api.Motif;
import com.nightcity.echoes.api.Person;
import com.nightcity.echoes.api.PersonRole;
import com.nightcity.echoes.api.Polity;
import com.nightcity.echoes.api.PolityFate;
import com.nightcity.echoes.api.Relation;
import com.nightcity.echoes.api.Route;
import com.nightcity.echoes.api.RouteKind;
import com.nightcity.echoes.api.ScriptType;
import com.nightcity.echoes.api.SimulationMode;
import com.nightcity.echoes.api.Site;
import com.nightcity.echoes.api.SiteOrigin;
import com.nightcity.echoes.api.SiteType;
import com.nightcity.tcivilization.api.TcMigrators;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

/**
 * Deterministic TCH1 binary reader/writer for <world>/tc/history.bin with 64-byte header,
 * per-section Deflate compression, header+payload+section CRC32 verification, atomic writes,
 * and TcMigrators forward migration support (18 §5.2.3, 33 §8.2, 40 §9.1).
 */
public final class HistoryBinIo {
    public static final byte[] MAGIC = "TCH1".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] LEGACY_V0_MAGIC = "TCH0".getBytes(StandardCharsets.US_ASCII);
    public static final int HEADER_BYTES = 64;
    public static final int FORMAT_VERSION = WorldHistoryData.FORMAT_VERSION;
    public static final byte FLAG_DEFLATE = 0x01;
    public static final Identifier FILE_ID = Identifier.fromNamespaceAndPath(TEchoes.MOD_ID, "history_bin");

    public static final List<String> SECTION_ORDER = List.of(
            "cultures",
            "polities",
            "persons",
            "events",
            "edges",
            "sites",
            "routes",
            "mines",
            "legend_seeds",
            "region_index",
            "live_log"
    );

    private static boolean defaultMigratorsRegistered = false;

    static {
        registerDefaultMigrators();
    }

    private HistoryBinIo() {
    }

    public record Header(
            int formatVersion,
            int generatorVersion,
            long historySeed,
            SimulationMode mode,
            byte flags,
            int windowCenterRx,
            int windowCenterRz,
            int windowRadiusRegions,
            int startYear,
            int endYear,
            int sectionCount,
            int payloadCrc32,
            long uncompressedTotalBytes,
            int headerCrc32
    ) {
    }

    public static synchronized void registerDefaultMigrators() {
        if (defaultMigratorsRegistered) {
            return;
        }
        defaultMigratorsRegistered = true;
        TcMigrators.register(FILE_ID, 0, (fromVer, tag) -> {
            CompoundTag migrated = tag.copy();
            migrated.putInt(TcMigrators.FORMAT_VERSION_KEY, 1);
            if (!migrated.contains("generatorVersion")) {
                migrated.putInt("generatorVersion", WorldHistoryData.GENERATOR_VERSION);
            }
            if (!migrated.contains("mode")) {
                String legacyMode = migrated.getStringOr("legacy_mode", "standalone");
                migrated.putString("mode", legacyMode);
            }
            if (!migrated.contains("startYear")) {
                migrated.putInt("startYear", WorldHistorySimulator.START_YEAR);
            }
            if (!migrated.contains("endYear")) {
                migrated.putInt("endYear", WorldHistorySimulator.END_YEAR);
            }
            return migrated;
        });
    }

    public static Path resolvePath(Path worldRoot) {
        Objects.requireNonNull(worldRoot, "worldRoot");
        return worldRoot.resolve("tc").resolve("history.bin");
    }

    public static byte[] encodeToBytes(WorldHistoryData data) throws IOException {
        Objects.requireNonNull(data, "data");
        Map<String, byte[]> rawSections = encodeSectionsRaw(data);

        long uncompressedTotal = 0L;
        ByteArrayOutputStream payloadBaos = new ByteArrayOutputStream(64 * 1024);
        try (DataOutputStream dos = new DataOutputStream(payloadBaos)) {
            for (String secName : SECTION_ORDER) {
                byte[] raw = rawSections.getOrDefault(secName, new byte[0]);
                uncompressedTotal += raw.length;
                byte[] compressed = deflateDeterministic(raw);
                int secCrc = crc32(compressed, 0, compressed.length);
                dos.writeUTF(secName);
                dos.writeInt(raw.length);
                dos.writeInt(compressed.length);
                dos.writeInt(secCrc);
                dos.write(compressed);
            }
        }
        byte[] payload = payloadBaos.toByteArray();
        int payloadCrc = crc32(payload, 0, payload.length);

        ByteBuffer out = ByteBuffer.allocate(HEADER_BYTES + payload.length);
        out.put(MAGIC);
        out.putInt(data.formatVersion());
        out.putInt(data.generatorVersion());
        out.putLong(data.historySeed());
        out.put(data.mode().id());
        out.put(FLAG_DEFLATE);
        out.putShort((short) 0);
        out.putInt(data.windowCenterRx());
        out.putInt(data.windowCenterRz());
        out.putInt(data.windowRadiusRegions());
        out.putInt(data.startYear());
        out.putInt(data.endYear());
        out.putInt(SECTION_ORDER.size());
        out.putInt(payloadCrc);
        out.putLong(uncompressedTotal);
        byte[] array = out.array();
        int headerCrc = crc32(array, 0, HEADER_BYTES - 4);
        out.putInt(headerCrc);
        out.put(payload);
        return array;
    }

    public static void writeAtomic(Path file, WorldHistoryData data) throws IOException {
        Objects.requireNonNull(file, "file");
        byte[] bytes = encodeToBytes(data);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Header readAndValidateHeader(byte[] allBytes) throws IOException {
        if (allBytes == null || allBytes.length < HEADER_BYTES) {
            throw new IOException("history.bin truncated: length=" + (allBytes == null ? 0 : allBytes.length));
        }
        for (int i = 0; i < 4; i++) {
            if (allBytes[i] != MAGIC[i]) {
                throw new IOException("Invalid history.bin magic header");
            }
        }
        ByteBuffer buf = ByteBuffer.wrap(allBytes, 0, HEADER_BYTES);
        buf.position(4);
        int formatVer = buf.getInt();
        int genVer = buf.getInt();
        long hSeed = buf.getLong();
        SimulationMode mode = SimulationMode.fromId(buf.get());
        byte flags = buf.get();
        buf.getShort();
        int winX = buf.getInt();
        int winZ = buf.getInt();
        int winR = buf.getInt();
        int startY = buf.getInt();
        int endY = buf.getInt();
        int secCount = buf.getInt();
        int payloadCrc = buf.getInt();
        long uncomp = buf.getLong();
        int expectedHeaderCrc = buf.getInt();

        int actualHeaderCrc = crc32(allBytes, 0, HEADER_BYTES - 4);
        if (actualHeaderCrc != expectedHeaderCrc) {
            throw new IOException("history.bin header CRC32 mismatch: expected="
                    + Integer.toHexString(expectedHeaderCrc) + ", actual=" + Integer.toHexString(actualHeaderCrc));
        }
        int payloadLen = allBytes.length - HEADER_BYTES;
        int actualPayloadCrc = crc32(allBytes, HEADER_BYTES, payloadLen);
        if (actualPayloadCrc != payloadCrc) {
            throw new IOException("history.bin payload CRC32 mismatch: expected="
                    + Integer.toHexString(payloadCrc) + ", actual=" + Integer.toHexString(actualPayloadCrc));
        }
        return new Header(
                formatVer, genVer, hSeed, mode, flags, winX, winZ, winR,
                startY, endY, secCount, payloadCrc, uncomp, expectedHeaderCrc
        );
    }

    public static WorldHistoryData read(Path file) throws IOException {
        return read(file, FORMAT_VERSION);
    }

    public static WorldHistoryData read(Path file, int targetFormatVersion) throws IOException {
        byte[] allBytes = Files.readAllBytes(file);
        return decodeFromBytes(allBytes, targetFormatVersion);
    }

    public static WorldHistoryData decodeFromBytes(byte[] allBytes) throws IOException {
        return decodeFromBytes(allBytes, FORMAT_VERSION);
    }

    public static WorldHistoryData decodeFromBytes(byte[] allBytes, int targetFormatVersion) throws IOException {
        // Support legacy v0 prototype container ("TCH0" + compressed CompoundTag) via TcMigrators
        if (allBytes != null && allBytes.length >= 8
                && allBytes[0] == LEGACY_V0_MAGIC[0]
                && allBytes[1] == LEGACY_V0_MAGIC[1]
                && allBytes[2] == LEGACY_V0_MAGIC[2]
                && allBytes[3] == LEGACY_V0_MAGIC[3]) {
            CompoundTag legacyTag = decodeLegacyV0Container(allBytes);
            CompoundTag migrated = TcMigrators.migrate(FILE_ID, targetFormatVersion, legacyTag);
            return fromCompoundTag(migrated);
        }

        Header hdr = readAndValidateHeader(allBytes);
        if (hdr.formatVersion() > targetFormatVersion) {
            CompoundTag probe = new CompoundTag();
            probe.putInt(TcMigrators.FORMAT_VERSION_KEY, hdr.formatVersion());
            TcMigrators.migrate(FILE_ID, targetFormatVersion, probe);
        }

        WorldHistoryData decoded = decodeBinarySections(hdr, allBytes);
        if (hdr.formatVersion() < targetFormatVersion) {
            CompoundTag asTag = toCompoundTag(decoded);
            asTag.putInt(TcMigrators.FORMAT_VERSION_KEY, hdr.formatVersion());
            CompoundTag migrated = TcMigrators.migrate(FILE_ID, targetFormatVersion, asTag);
            return fromCompoundTag(migrated);
        }
        return decoded;
    }

    public static CompoundTag toCompoundTag(WorldHistoryData data) {
        Tag encoded = WorldHistoryData.CODEC.encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow(err -> new IllegalStateException("Failed encoding WorldHistoryData to NBT: " + err));
        return (CompoundTag) encoded;
    }

    public static WorldHistoryData fromCompoundTag(CompoundTag tag) {
        return WorldHistoryData.CODEC.parse(NbtOps.INSTANCE, tag)
                .getOrThrow(err -> new IllegalStateException("Failed decoding WorldHistoryData from NBT: " + err));
    }

    public static byte[] encodeLegacyV0Container(CompoundTag v0Payload) throws IOException {
        CompoundTag copy = v0Payload.copy();
        copy.remove(TcMigrators.FORMAT_VERSION_KEY);
        ByteArrayOutputStream nbtBytes = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(nbtBytes)) {
            NbtIo.write(copy, dos);
        }
        byte[] compressed = deflateDeterministic(nbtBytes.toByteArray());
        int crc = crc32(compressed, 0, compressed.length);
        ByteBuffer buf = ByteBuffer.allocate(12 + compressed.length);
        buf.put(LEGACY_V0_MAGIC);
        buf.putInt(compressed.length);
        buf.putInt(crc);
        buf.put(compressed);
        return buf.array();
    }

    private static CompoundTag decodeLegacyV0Container(byte[] allBytes) throws IOException {
        if (allBytes.length < 12) {
            throw new IOException("Truncated TCH0 legacy container");
        }
        ByteBuffer buf = ByteBuffer.wrap(allBytes);
        buf.position(4);
        int compLen = buf.getInt();
        int expectedCrc = buf.getInt();
        if (compLen <= 0 || 12 + compLen > allBytes.length) {
            throw new IOException("Invalid TCH0 compressed bounds");
        }
        int actualCrc = crc32(allBytes, 12, compLen);
        if (actualCrc != expectedCrc) {
            throw new IOException("TCH0 CRC32 mismatch");
        }
        byte[] inflated = inflateBytes(allBytes, 12, compLen, 16 * 1024 * 1024);
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(inflated))) {
            return NbtIo.read(dis, NbtAccounter.unlimitedHeap());
        }
    }

    private static WorldHistoryData decodeBinarySections(Header hdr, byte[] allBytes) throws IOException {
        Map<String, byte[]> sections = new LinkedHashMap<>();
        boolean compressed = (hdr.flags() & FLAG_DEFLATE) != 0;
        try (DataInputStream dis = new DataInputStream(
                new ByteArrayInputStream(allBytes, HEADER_BYTES, allBytes.length - HEADER_BYTES))) {
            for (int i = 0; i < hdr.sectionCount(); i++) {
                String name = dis.readUTF();
                int rawLen = dis.readInt();
                int compLen = dis.readInt();
                int expectedSecCrc = dis.readInt();
                if (rawLen < 0 || compLen < 0 || rawLen > 64 * 1024 * 1024 || compLen > allBytes.length) {
                    throw new IOException("Invalid section bounds for " + name);
                }
                byte[] compBytes = dis.readNBytes(compLen);
                if (compBytes.length != compLen) {
                    throw new IOException("Unexpected EOF in section " + name);
                }
                int actualSecCrc = crc32(compBytes, 0, compLen);
                if (actualSecCrc != expectedSecCrc) {
                    throw new IOException("Section CRC32 mismatch in " + name);
                }
                byte[] raw = compressed ? inflateBytes(compBytes, 0, compLen, rawLen) : compBytes;
                sections.put(name, raw);
            }
        }

        List<Culture> cultures = readCultures(sections.get("cultures"));
        PolitiesBundle pBundle = readPolitiesBundle(sections.get("polities"));
        List<Person> persons = readPersons(sections.get("persons"));
        List<HistoryEvent> events = readEvents(sections.get("events"));
        List<EventEdge> edges = readEdges(sections.get("edges"));
        SitesBundle sBundle = readSitesBundle(sections.get("sites"));
        List<Route> routes = readRoutes(sections.get("routes"));
        List<AncientMine> mines = readMines(sections.get("mines"));
        List<LegendSeed> legends = readLegendSeeds(sections.get("legend_seeds"));
        List<WorldHistoryData.RegionSummaryEntry> regionIndex = readRegionIndex(sections.get("region_index"));
        List<LiveHistoryEntry> liveLog = readLiveLog(sections.get("live_log"));

        return new WorldHistoryData(
                hdr.formatVersion(),
                hdr.generatorVersion(),
                hdr.historySeed(),
                hdr.mode(),
                hdr.windowCenterRx(),
                hdr.windowCenterRz(),
                hdr.windowRadiusRegions(),
                hdr.startYear(),
                hdr.endYear(),
                0L,
                cultures,
                pBundle.polities,
                persons,
                events,
                edges,
                sBundle.sites,
                routes,
                mines,
                legends,
                sBundle.extinctions,
                pBundle.relations,
                pBundle.metas,
                regionIndex,
                sBundle.traces,
                liveLog
        );
    }

    private static Map<String, byte[]> encodeSectionsRaw(WorldHistoryData d) throws IOException {
        Map<String, byte[]> map = new LinkedHashMap<>();
        map.put("cultures", writeCultures(d.cultures()));
        map.put("polities", writePolitiesBundle(d.polities(), d.relations(), d.polityMetas()));
        map.put("persons", writePersons(d.persons()));
        map.put("events", writeEvents(d.events()));
        map.put("edges", writeEdges(d.edges()));
        map.put("sites", writeSitesBundle(d.sites(), d.extinctions(), d.historicalTraces()));
        map.put("routes", writeRoutes(d.routes()));
        map.put("mines", writeMines(d.mines()));
        map.put("legend_seeds", writeLegendSeeds(d.legendSeeds()));
        map.put("region_index", writeRegionIndex(d.regionIndex()));
        map.put("live_log", writeLiveLog(d.liveLog()));
        return map;
    }

    private static byte[] writeCultures(List<Culture> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (Culture c : list) {
                out.writeInt(c.id());
                out.writeUTF(c.nameKey());
                out.writeUTF(c.displayName());
                out.writeByte(c.language().ordinal());
                out.writeByte(c.script().ordinal());
                out.writeByte(c.primaryStyle().ordinal());
                out.writeByte(c.secondaryStyle().ordinal());
                out.writeByte(c.burial().ordinal());
                out.writeByte(c.motif().ordinal());
                out.writeByte(c.deity().ordinal());
                out.writeLong(c.hearthRegion());
            }
        }
        return baos.toByteArray();
    }

    private static List<Culture> readCultures(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<Culture> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new Culture(
                        in.readInt(),
                        in.readUTF(),
                        in.readUTF(),
                        LanguageTemplate.fromOrdinal(in.readByte()),
                        ScriptType.fromOrdinal(in.readByte()),
                        ArchStyle.fromOrdinal(in.readByte()),
                        ArchStyle.fromOrdinal(in.readByte()),
                        Burial.fromOrdinal(in.readByte()),
                        Motif.fromOrdinal(in.readByte()),
                        Deity.fromOrdinal(in.readByte()),
                        in.readLong()
                ));
            }
            return list;
        }
    }

    private record PolitiesBundle(
            List<Polity> polities,
            List<WorldHistoryData.BilateralRelation> relations,
            List<WorldHistoryData.PolityMeta> metas
    ) {
    }

    private static byte[] writePolitiesBundle(
            List<Polity> polities,
            List<WorldHistoryData.BilateralRelation> relations,
            List<WorldHistoryData.PolityMeta> metas
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(polities.size());
            for (Polity p : polities) {
                out.writeInt(p.id());
                out.writeInt(p.cultureId());
                out.writeUTF(p.name());
                out.writeInt(p.foundedYear());
                out.writeInt(p.endYear());
                out.writeByte(p.peakEra());
                out.writeByte(p.finalEra());
                out.writeInt(p.finalPopulation());
                out.writeLong(p.capitalRegion());
                out.writeByte(p.fate().ordinal());
                writeIntList(out, p.predecessorIds());
                out.writeLong(p.civId().getMostSignificantBits());
                out.writeLong(p.civId().getLeastSignificantBits());
            }
            out.writeInt(relations.size());
            for (WorldHistoryData.BilateralRelation br : relations) {
                out.writeInt(br.polityA());
                out.writeInt(br.polityB());
                out.writeByte(br.relation().ordinal());
            }
            out.writeInt(metas.size());
            for (WorldHistoryData.PolityMeta pm : metas) {
                out.writeInt(pm.polityId());
                out.writeDouble(pm.aggression());
                writeLongList(out, pm.territoryRegions());
                writeLongList(out, pm.holySiteRegions());
            }
        }
        return baos.toByteArray();
    }

    private static PolitiesBundle readPolitiesBundle(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) {
            return new PolitiesBundle(List.of(), List.of(), List.of());
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<Polity> polities = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                int cid = in.readInt();
                String name = in.readUTF();
                int fy = in.readInt();
                int ey = in.readInt();
                int pe = in.readByte();
                int fe = in.readByte();
                int fpop = in.readInt();
                long cap = in.readLong();
                PolityFate fate = PolityFate.fromOrdinal(in.readByte());
                List<Integer> preds = readIntList(in);
                UUID civId = new UUID(in.readLong(), in.readLong());
                polities.add(new Polity(id, cid, name, fy, ey, pe, fe, fpop, cap, fate, preds, civId));
            }
            int rCount = in.readInt();
            List<WorldHistoryData.BilateralRelation> relations = new ArrayList<>(rCount);
            for (int i = 0; i < rCount; i++) {
                relations.add(new WorldHistoryData.BilateralRelation(
                        in.readInt(), in.readInt(), Relation.fromOrdinal(in.readByte())
                ));
            }
            int mCount = in.readInt();
            List<WorldHistoryData.PolityMeta> metas = new ArrayList<>(mCount);
            for (int i = 0; i < mCount; i++) {
                metas.add(new WorldHistoryData.PolityMeta(
                        in.readInt(), in.readDouble(), readLongList(in), readLongList(in)
                ));
            }
            return new PolitiesBundle(polities, relations, metas);
        }
    }

    private static byte[] writePersons(List<Person> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (Person p : list) {
                out.writeInt(p.id());
                out.writeInt(p.polityId());
                out.writeUTF(p.name());
                out.writeUTF(p.epithet());
                out.writeInt(p.born());
                out.writeInt(p.died());
                out.writeByte(p.role().ordinal());
                writeIntList(out, p.deeds());
            }
        }
        return baos.toByteArray();
    }

    private static List<Person> readPersons(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<Person> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new Person(
                        in.readInt(), in.readInt(), in.readUTF(), in.readUTF(),
                        in.readInt(), in.readInt(), PersonRole.fromOrdinal(in.readByte()), readIntList(in)
                ));
            }
            return list;
        }
    }

    private static byte[] writeEvents(List<HistoryEvent> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (HistoryEvent ev : list) {
                out.writeInt(ev.id());
                out.writeByte(ev.type().ordinal());
                out.writeInt(ev.year());
                out.writeLong(ev.region());
                writeIntList(out, ev.polities());
                writeIntList(out, ev.persons());
                out.writeShort(ev.importance());
                out.writeShort(ev.severity());
            }
        }
        return baos.toByteArray();
    }

    private static List<HistoryEvent> readEvents(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<HistoryEvent> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new HistoryEvent(
                        in.readInt(), EventType.fromOrdinal(in.readByte()), in.readInt(), in.readLong(),
                        readIntList(in), readIntList(in), in.readShort(), in.readShort()
                ));
            }
            return list;
        }
    }

    private static byte[] writeEdges(List<EventEdge> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (EventEdge edge : list) {
                out.writeInt(edge.from());
                out.writeInt(edge.to());
                out.writeByte(edge.type().ordinal());
            }
        }
        return baos.toByteArray();
    }

    private static List<EventEdge> readEdges(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<EventEdge> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new EventEdge(in.readInt(), in.readInt(), EdgeType.fromOrdinal(in.readByte())));
            }
            return list;
        }
    }

    private record SitesBundle(
            List<Site> sites,
            List<Extinction> extinctions,
            List<WorldHistoryData.HistoricalTraceRecord> traces
    ) {
    }

    private static byte[] writeSitesBundle(
            List<Site> sites,
            List<Extinction> extinctions,
            List<WorldHistoryData.HistoricalTraceRecord> traces
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(sites.size());
            for (Site s : sites) {
                out.writeInt(s.id());
                out.writeByte(s.type().ordinal());
                out.writeLong(s.anchor().asLong());
                out.writeInt(s.cultureId());
                out.writeInt(s.polityId());
                out.writeInt(s.builtYear());
                out.writeInt(s.abandonedYear());
                out.writeByte(s.style().ordinal());
                out.writeByte(s.origin().ordinal());
                out.writeFloat(s.weathering());
                out.writeByte(s.burialDepth());
                writeIntList(out, s.legendSeeds());
                out.writeInt(s.flags());
            }
            out.writeInt(extinctions.size());
            for (Extinction ex : extinctions) {
                out.writeInt(ex.continentId());
                out.writeUTF(ex.species().toString());
                out.writeInt(ex.year());
                out.writeInt(ex.causeEventId());
            }
            out.writeInt(traces.size());
            for (WorldHistoryData.HistoricalTraceRecord tr : traces) {
                out.writeByte(tr.kind().ordinal());
                out.writeLong(tr.pos().asLong());
                out.writeInt(tr.radiusBlocks());
                out.writeInt(tr.year());
                out.writeUTF(tr.depositId());
                out.writeFloat(tr.minedFraction());
            }
        }
        return baos.toByteArray();
    }

    private static SitesBundle readSitesBundle(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) {
            return new SitesBundle(List.of(), List.of(), List.of());
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<Site> sites = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                sites.add(new Site(
                        in.readInt(),
                        SiteType.fromOrdinal(in.readByte()),
                        BlockPos.of(in.readLong()),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        in.readInt(),
                        ArchStyle.fromOrdinal(in.readByte()),
                        SiteOrigin.fromOrdinal(in.readByte()),
                        in.readFloat(),
                        in.readByte(),
                        readIntList(in),
                        in.readInt()
                ));
            }
            int exCount = in.readInt();
            List<Extinction> exts = new ArrayList<>(exCount);
            for (int i = 0; i < exCount; i++) {
                exts.add(new Extinction(in.readInt(), Identifier.parse(in.readUTF()), in.readInt(), in.readInt()));
            }
            int trCount = in.readInt();
            List<WorldHistoryData.HistoricalTraceRecord> traces = new ArrayList<>(trCount);
            WorldHistoryData.TraceKind[] tKinds = WorldHistoryData.TraceKind.values();
            for (int i = 0; i < trCount; i++) {
                int kOrd = Math.clamp(in.readByte(), 0, tKinds.length - 1);
                traces.add(new WorldHistoryData.HistoricalTraceRecord(
                        tKinds[kOrd], BlockPos.of(in.readLong()), in.readInt(), in.readInt(), in.readUTF(), in.readFloat()
                ));
            }
            return new SitesBundle(sites, exts, traces);
        }
    }

    private static byte[] writeRoutes(List<Route> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (Route r : list) {
                out.writeInt(r.id());
                out.writeByte(r.kind().ordinal());
                out.writeInt(r.polityA());
                out.writeInt(r.polityB());
                out.writeInt(r.fromYear());
                out.writeInt(r.toYear());
                writeLongList(out, r.points());
                out.writeFloat(r.weathering());
            }
        }
        return baos.toByteArray();
    }

    private static List<Route> readRoutes(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<Route> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new Route(
                        in.readInt(), RouteKind.fromOrdinal(in.readByte()),
                        in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                        readLongList(in), in.readFloat()
                ));
            }
            return list;
        }
    }

    private static byte[] writeMines(List<AncientMine> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (AncientMine m : list) {
                out.writeInt(m.id());
                out.writeUTF(m.depositId());
                out.writeLong(m.entrance().asLong());
                out.writeFloat(m.minedFraction());
                out.writeInt(m.fromYear());
                out.writeInt(m.toYear());
                out.writeFloat(m.weathering());
            }
        }
        return baos.toByteArray();
    }

    private static List<AncientMine> readMines(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<AncientMine> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new AncientMine(
                        in.readInt(), in.readUTF(), BlockPos.of(in.readLong()),
                        in.readFloat(), in.readInt(), in.readInt(), in.readFloat()
                ));
            }
            return list;
        }
    }

    private static byte[] writeLegendSeeds(List<LegendSeed> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (LegendSeed ls : list) {
                out.writeInt(ls.id());
                out.writeByte(ls.kind().ordinal());
                out.writeInt(ls.eventId());
                out.writeInt(ls.siteId());
                out.writeInt(ls.chainId());
                out.writeShort(ls.chainIndex());
                out.writeByte(ls.truth());
            }
        }
        return baos.toByteArray();
    }

    private static List<LegendSeed> readLegendSeeds(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<LegendSeed> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new LegendSeed(
                        in.readInt(), LegendKind.fromOrdinal(in.readByte()),
                        in.readInt(), in.readInt(), in.readInt(), in.readShort(), in.readByte()
                ));
            }
            return list;
        }
    }

    private static byte[] writeRegionIndex(List<WorldHistoryData.RegionSummaryEntry> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (WorldHistoryData.RegionSummaryEntry e : list) {
                out.writeLong(e.regionKey());
                out.writeByte(e.hazardFlags());
                out.writeShort(e.dominantCulture());
                out.writeShort(e.lastPolity());
                out.writeFloat(e.climateWeatheringFactor());
                out.writeFloat(e.sedimentRate());
                writeIntList(out, e.siteIds());
                writeIntList(out, e.eventIds());
                writeIntList(out, e.legendSeedIds());
            }
        }
        return baos.toByteArray();
    }

    private static List<WorldHistoryData.RegionSummaryEntry> readRegionIndex(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<WorldHistoryData.RegionSummaryEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new WorldHistoryData.RegionSummaryEntry(
                        in.readLong(), in.readByte(), in.readShort(), in.readShort(),
                        in.readFloat(), in.readFloat(),
                        readIntList(in), readIntList(in), readIntList(in)
                ));
            }
            return list;
        }
    }

    private static byte[] writeLiveLog(List<LiveHistoryEntry> list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeInt(list.size());
            for (LiveHistoryEntry entry : list) {
                out.writeInt(entry.year());
                out.writeByte(entry.type().ordinal());
                out.writeLong(entry.pos().asLong());
                out.writeInt(entry.actors().size());
                for (UUID u : entry.actors()) {
                    out.writeLong(u.getMostSignificantBits());
                    out.writeLong(u.getLeastSignificantBits());
                }
                out.writeUTF(entry.sourceMod().toString());
                out.writeInt(entry.importance());
            }
        }
        return baos.toByteArray();
    }

    private static List<LiveHistoryEntry> readLiveLog(byte[] raw) throws IOException {
        if (raw == null || raw.length == 0) return List.of();
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw))) {
            int n = in.readInt();
            List<LiveHistoryEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                int year = in.readInt();
                EventType type = EventType.fromOrdinal(in.readByte());
                BlockPos pos = BlockPos.of(in.readLong());
                int actorCount = in.readInt();
                List<UUID> actors = new ArrayList<>(actorCount);
                for (int a = 0; a < actorCount; a++) {
                    actors.add(new UUID(in.readLong(), in.readLong()));
                }
                Identifier mod = Identifier.parse(in.readUTF());
                int imp = in.readInt();
                list.add(new LiveHistoryEntry(year, type, pos, actors, mod, imp));
            }
            return list;
        }
    }

    private static void writeIntList(DataOutputStream out, List<Integer> list) throws IOException {
        out.writeInt(list.size());
        for (int v : list) {
            out.writeInt(v);
        }
    }

    private static List<Integer> readIntList(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n <= 0) return List.of();
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(in.readInt());
        }
        return list;
    }

    private static void writeLongList(DataOutputStream out, List<Long> list) throws IOException {
        out.writeInt(list.size());
        for (long v : list) {
            out.writeLong(v);
        }
    }

    private static List<Long> readLongList(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n <= 0) return List.of();
        List<Long> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(in.readLong());
        }
        return list;
    }

    private static byte[] deflateDeterministic(byte[] raw) throws IOException {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, false);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, raw.length / 2));
            try (DeflaterOutputStream dos = new DeflaterOutputStream(baos, deflater)) {
                dos.write(raw);
            }
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] inflateBytes(byte[] src, int off, int len, int expectedMax) throws IOException {
        Inflater inflater = new Inflater(false);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.min(expectedMax, Math.max(256, len * 2)));
            try (InflaterInputStream iis = new InflaterInputStream(new ByteArrayInputStream(src, off, len), inflater)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = iis.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
            }
            return baos.toByteArray();
        } finally {
            inflater.end();
        }
    }

    public static int crc32(byte[] bytes, int offset, int length) {
        CRC32 crc = new CRC32();
        crc.update(bytes, offset, length);
        return (int) crc.getValue();
    }
}
