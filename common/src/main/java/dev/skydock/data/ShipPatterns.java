package dev.skydock.data;

import dev.skydock.Skydock;
import dev.skydock.block.FenceConnections;
import dev.skydock.block.LiftCellBlock;
import dev.skydock.block.SkydockBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The built-in shipyard catalog. Coordinates are dock-local: the deck begins at y=1,
 * the bow faces the controller at low z, and every hull starts three blocks behind it.
 *
 * <p>The catalog deliberately authors decorations as a second layer. Omitting that
 * layer changes the preview and bill of materials without changing structural mass,
 * lift demand, or the connected launchable core.</p>
 */
public final class ShipPatterns {
    private static final int FRONT_MARGIN = 3;
    private ShipPatterns() {}

    public static List<ShipPattern> all() { return Catalog.ALL; }

    public static List<ShipPattern> forTier(DockTier tier) {
        return Catalog.BY_TIER.getOrDefault(tier, List.of());
    }

    public static Optional<ShipPattern> get(ResourceLocation id) {
        return Optional.ofNullable(Catalog.BY_ID.get(id));
    }

    public static ShipPattern defaultFor(DockTier tier) {
        List<ShipPattern> patterns = forTier(tier);
        if (patterns.isEmpty()) throw new IllegalArgumentException("No built-in pattern for " + tier);
        return patterns.getFirst();
    }

    private static final class Catalog {
        private static final List<ShipPattern> ALL = createCatalog(Palette.production());
        private static final Map<ResourceLocation, ShipPattern> BY_ID = indexById(ALL);
        private static final Map<DockTier, List<ShipPattern>> BY_TIER = indexByTier(ALL);
    }

    /** A loader-free catalog used by unit tests to exercise the exact production geometry. */
    static List<ShipPattern> createTestCatalog() { return createCatalog(Palette.vanillaTestPalette()); }

    private static List<ShipPattern> createCatalog(Palette palette) {
        List<ShipPattern> patterns = new ArrayList<>(12);
        patterns.add(cutter(palette, "scout_cutter", "Windward Cutter", "A nimble timber cutter beneath a compact canvas envelope.", DockTier.SCOUT, 11, 19, 9, 2, 3));
        patterns.add(twinHull(palette, "scout_twinhull", "Twinfin Skiff", "Two fine hulls and an open center make a steady starter craft.", DockTier.SCOUT, 11, 19, 9, 1, 3));
        patterns.add(hauler(palette, "scout_hauler", "Tinker Tender", "A practical round-bowed tender with a sheltered workshop bay.", DockTier.SCOUT, 11, 19, 9, 1, 1));

        patterns.add(cutter(palette, "brig_cutter", "Peregrine", "A long raised-stern cutter built for fast regional passages.", DockTier.BRIG, 17, 27, 12, 3, 5));
        patterns.add(twinHull(palette, "brig_twinhull", "Brasswing Catamaran", "Parallel envelopes and twin brass drives span two lean hulls.", DockTier.BRIG, 17, 27, 12, 1, 5));
        patterns.add(hauler(palette, "brig_hauler", "Wayfarer Hauler", "A covered cargo barge with an upper helm and generous deck room.", DockTier.BRIG, 17, 27, 12, 1, 2));

        patterns.add(cutter(palette, "cruiser_cutter", "Meridian Clipper", "A broad clipper with a high quarterdeck and a commanding canvas crown.", DockTier.CRUISER, 23, 35, 15, 4, 7));
        patterns.add(twinHull(palette, "cruiser_twinhull", "Cloudsplitter", "Wide-set hulls, three bridge spans, and paired turbine banks cut cleanly through cloud.", DockTier.CRUISER, 23, 35, 15, 2, 7));
        patterns.add(hauler(palette, "cruiser_hauler", "Grand Caravan", "A glazed traveling warehouse with four lifted corner galleries.", DockTier.CRUISER, 23, 35, 15, 2, 3));

        patterns.add(cutter(palette, "dreadnought_cutter", "Sovereign", "A monumental royal cutter with a tiered stern and vast central envelope.", DockTier.DREADNOUGHT, 29, 43, 18, 5, 9));
        patterns.add(twinHull(palette, "dreadnought_twinhull", "Tempest Ark", "Twin aether hulls carry a fortress bridge between two immense balloon banks.", DockTier.DREADNOUGHT, 29, 43, 18, 2, 9));
        patterns.add(hauler(palette, "dreadnought_hauler", "Atlas Freighter", "A huge rounded freighter whose four canvas pylons frame an enclosed hold.", DockTier.DREADNOUGHT, 29, 43, 18, 2, 4));
        return List.copyOf(patterns);
    }

    private static Map<ResourceLocation, ShipPattern> indexById(List<ShipPattern> patterns) {
        Map<ResourceLocation, ShipPattern> result = new LinkedHashMap<>();
        for (ShipPattern pattern : patterns) {
            if (result.put(pattern.id(), pattern) != null) throw new IllegalStateException("Duplicate ship pattern id " + pattern.id());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<DockTier, List<ShipPattern>> indexByTier(List<ShipPattern> patterns) {
        Map<DockTier, List<ShipPattern>> result = new EnumMap<>(DockTier.class);
        for (DockTier tier : DockTier.values())
            result.put(tier, patterns.stream().filter(pattern -> pattern.tier() == tier).toList());
        return Collections.unmodifiableMap(result);
    }

    private static ShipPattern cutter(Palette palette, String id, String name, String description, DockTier tier,
                                      int width, int length, int maxHeight, int balloonRows, int balloonDepth) {
        Builder b = new Builder(tier, palette);
        int x0 = centeredX(tier, width), z0 = FRONT_MARGIN, cx = x0 + width / 2;
        int fullHalf = width / 2, bowRun = Math.max(4, length / 5);
        for (int z = 0; z < length; z++) {
            int half;
            if (z == 0) half = 0;
            else if (z < bowRun) half = Math.max(1, (fullHalf * z + bowRun - 1) / bowRun);
            else if (z >= length - 3) half = Math.max(2, fullHalf - (z - (length - 4)));
            else half = fullHalf;
            int left = cx - half, right = cx + half;
            b.fill(left, 1, z0 + z, right, 1, z0 + z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.fill(Math.min(cx, left + 1), 0, z0 + z, Math.max(cx, right - 1), 0, z0 + z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            if (z > 1 && z < length - 2 && z % 2 == 0) {
                b.block(left, 0, z0 + z, Blocks.SPRUCE_LOG.defaultBlockState());
                b.block(right, 0, z0 + z, Blocks.SPRUCE_LOG.defaultBlockState());
            }
        }

        int quarterStart = z0 + length - Math.max(7, length / 4 + 3);
        int cabinStart = z0 + length - Math.max(5, length / 6 + 3);
        int cabinHalf = Math.max(2, width / 6);
        b.fill(cx - cabinHalf - 1, 2, quarterStart, cx + cabinHalf + 1, 2, z0 + length - 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        b.stair(cx, 2, quarterStart - 1, Direction.SOUTH);
        for (int y = 3; y <= 4; y++) {
            b.fill(cx - cabinHalf, y, cabinStart, cx - cabinHalf, y, z0 + length - 2, Blocks.SPRUCE_LOG.defaultBlockState());
            b.fill(cx + cabinHalf, y, cabinStart, cx + cabinHalf, y, z0 + length - 2, Blocks.SPRUCE_LOG.defaultBlockState());
            b.fill(cx - cabinHalf + 1, y, z0 + length - 2, cx + cabinHalf - 1, y, z0 + length - 2, Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        b.fill(cx - cabinHalf, 5, cabinStart, cx + cabinHalf, 5, z0 + length - 2, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        b.block(cx, 3, cabinStart - 1, palette.helm());
        b.block(cx + Math.min(2, cabinHalf), 3, cabinStart - 1, palette.engine(tier));
        b.block(cx - Math.min(2, cabinHalf), 3, cabinStart - 1, palette.seat());
        b.block(cx - cabinHalf + 1, 3, z0 + length - 3, Blocks.CHEST.defaultBlockState());
        b.block(cx + cabinHalf - 1, 3, z0 + length - 3, Blocks.FURNACE.defaultBlockState());
        b.block(cx, 2, z0 + length / 2, palette.ballast());

        int balloonY = maxHeight - balloonRows * 2;
        int balloonX = cx - balloonRows;
        int balloonZ = z0 + 4;
        b.balloonArray(balloonX, balloonY, balloonZ, balloonRows, balloonDepth);
        b.support(balloonX, 2, balloonY - 1, balloonZ);
        b.support(balloonX + balloonRows * 2 - 1, 2, balloonY - 1, balloonZ);
        b.support(balloonX, 2, balloonY - 1, balloonZ + balloonDepth * 2 - 1);
        b.support(balloonX + balloonRows * 2 - 1, 2, balloonY - 1, balloonZ + balloonDepth * 2 - 1);

        int canopyZ = z0 + Math.max(5, length / 3);
        b.support(cx - 2, 2, 3, canopyZ);
        b.support(cx + 2, 2, 3, canopyZ);
        b.decorFill(cx - 2, 4, canopyZ, cx + 2, 4, canopyZ + 1, palette.awning());
        b.edgeRailingsForCutter(cx, width, z0, length, bowRun);
        b.decorIfEmpty(cx - fullHalf + 1, 2, z0 + length / 2, palette.lantern());
        b.decorIfEmpty(cx + fullHalf - 1, 2, z0 + length / 2, palette.lantern());
        b.decorIfEmpty(cx, 2, z0 + 1, palette.flag());
        return b.pattern(id, name, description);
    }

    private static ShipPattern twinHull(Palette palette, String id, String name, String description, DockTier tier,
                                       int width, int length, int maxHeight, int bankRows, int balloonDepth) {
        Builder b = new Builder(tier, palette);
        int x0 = centeredX(tier, width), z0 = FRONT_MARGIN;
        int hullWidth = Math.max(3, width / 5);
        if ((hullWidth & 1) == 0) hullWidth++;
        int leftCenter = x0 + hullWidth / 2;
        int rightCenter = x0 + width - 1 - hullWidth / 2;
        for (int z = 0; z < length; z++) {
            int half = z == 0 ? 0 : Math.min(hullWidth / 2, z);
            for (int center : new int[]{leftCenter, rightCenter}) {
                b.fill(center - half, 1, z0 + z, center + half, 1, z0 + z, Blocks.SPRUCE_PLANKS.defaultBlockState());
                b.fill(Math.min(center, center - half + 1), 0, z0 + z, Math.max(center, center + half - 1), 0, z0 + z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
        }
        int[] bridges = {z0 + length / 4, z0 + length / 2, z0 + (length * 3) / 4};
        for (int bridgeZ : bridges) {
            b.fill(leftCenter + hullWidth / 2 + 1, 1, bridgeZ, rightCenter - hullWidth / 2 - 1, 1, bridgeZ, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            b.fill(leftCenter + 1, 2, bridgeZ, rightCenter - 1, 2, bridgeZ, Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        int cx = x0 + width / 2, bridgeZ = bridges[1];
        b.fill(leftCenter + 1, 2, bridgeZ - 1, rightCenter - 1, 2, bridgeZ + 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        b.block(cx, 3, bridgeZ + 1, palette.helm());
        b.block(cx - 2, 3, bridgeZ + 1, palette.seat());
        b.block(cx + 2, 3, bridgeZ + 1, palette.seat());
        b.block(leftCenter, 2, z0 + length - 3, palette.engine(tier));
        b.block(rightCenter, 2, z0 + length - 3, palette.engine(tier));
        b.block(leftCenter, 2, bridgeZ + 1, palette.ballast());
        b.block(rightCenter, 2, bridgeZ + 1, palette.ballast());
        b.block(leftCenter, 2, z0 + length - 5, Blocks.CHEST.defaultBlockState());
        b.block(rightCenter, 2, z0 + length - 5, Blocks.FURNACE.defaultBlockState());

        int balloonY = maxHeight - bankRows * 2, balloonZ = z0 + 3;
        int bankWidth = bankRows * 2;
        int leftBankX = leftCenter - bankWidth / 2;
        int rightBankX = rightCenter - bankWidth / 2 + 1;
        b.balloonArray(leftBankX, balloonY, balloonZ, bankRows, balloonDepth);
        b.balloonArray(rightBankX, balloonY, balloonZ, bankRows, balloonDepth);
        for (int bankX : new int[]{leftBankX, rightBankX}) {
            int outerX = bankX == leftBankX ? bankX : bankX + bankWidth - 1;
            b.support(outerX, 2, balloonY - 1, balloonZ);
            b.support(outerX, 2, balloonY - 1, balloonZ + balloonDepth * 2 - 1);
        }

        for (int z = 2; z < length - 2; z++) {
            b.decorIfEmpty(x0, 2, z0 + z, palette.railing());
            b.decorIfEmpty(x0 + width - 1, 2, z0 + z, palette.railing());
        }
        b.decorIfEmpty(leftCenter, 2, z0 + 2, palette.flag());
        b.decorIfEmpty(rightCenter, 2, z0 + 2, palette.flag());
        b.decorIfEmpty(cx - 2, 3, bridges[0], palette.lantern());
        b.decorIfEmpty(cx + 2, 3, bridges[0], palette.lantern());
        b.decorFill(cx - 2, 5, bridgeZ, cx + 2, 5, bridgeZ, palette.awning());
        b.support(cx - 2, 3, 4, bridgeZ);
        b.support(cx + 2, 3, 4, bridgeZ);
        return b.pattern(id, name, description);
    }

    private static ShipPattern hauler(Palette palette, String id, String name, String description, DockTier tier,
                                     int width, int length, int maxHeight, int balloonRows, int balloonDepth) {
        Builder b = new Builder(tier, palette);
        int x0 = centeredX(tier, width), z0 = FRONT_MARGIN, cx = x0 + width / 2;
        for (int z = 0; z < length; z++) {
            int cut = (z == 0 || z == length - 1) ? 2 : (z == 1 || z == length - 2 ? 1 : 0);
            int left = x0 + cut, right = x0 + width - 1 - cut;
            b.fill(left, 1, z0 + z, right, 1, z0 + z, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.fill(left + 1, 0, z0 + z, right - 1, 0, z0 + z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        int cargoStart = z0 + Math.max(5, length / 5);
        int cargoEnd = z0 + length / 2 + 1;
        int cargoHalf = Math.max(3, width / 3);
        for (int z = cargoStart; z <= cargoEnd; z++) {
            boolean frame = z == cargoStart || z == cargoEnd || (z - cargoStart) % 4 == 0;
            if (frame) {
                b.fill(cx - cargoHalf, 2, z, cx - cargoHalf, 3, z, Blocks.SPRUCE_LOG.defaultBlockState());
                b.fill(cx + cargoHalf, 2, z, cx + cargoHalf, 3, z, Blocks.SPRUCE_LOG.defaultBlockState());
            } else {
                b.block(cx - cargoHalf, 2, z, Blocks.GLASS_PANE.defaultBlockState());
                b.block(cx + cargoHalf, 2, z, Blocks.GLASS_PANE.defaultBlockState());
            }
            b.fill(cx - cargoHalf, 4, z, cx + cargoHalf, 4, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }

        int cabinStart = z0 + length - Math.max(7, length / 5 + 3);
        int cabinEnd = z0 + length - 2;
        int cabinHalf = Math.max(3, width / 4);
        for (int y = 2; y <= 3; y++) {
            b.fill(cx - cabinHalf + 1, y, cabinStart, cx - 1, y, cabinStart, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.fill(cx + 1, y, cabinStart, cx + cabinHalf - 1, y, cabinStart, Blocks.SPRUCE_PLANKS.defaultBlockState());
            b.fill(cx - cabinHalf, y, cabinStart, cx - cabinHalf, y, cabinEnd, Blocks.SPRUCE_LOG.defaultBlockState());
            b.fill(cx + cabinHalf, y, cabinStart, cx + cabinHalf, y, cabinEnd, Blocks.SPRUCE_LOG.defaultBlockState());
            for (int x = cx - cabinHalf + 1; x <= cx + cabinHalf - 1; x++)
                b.block(x, y, cabinEnd, y == 2 && Math.floorMod(x - cx, 3) == 0 ? Blocks.GLASS_PANE.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
        }
        b.fill(cx - cabinHalf, 4, cabinStart, cx + cabinHalf, 4, cabinEnd, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        int stairX = cx - Math.max(2, cabinHalf / 2);
        b.stair(stairX, 2, cabinStart - 3, Direction.SOUTH);
        b.block(stairX, 2, cabinStart - 2, Blocks.SPRUCE_LOG.defaultBlockState());
        b.stair(stairX, 3, cabinStart - 2, Direction.SOUTH);
        b.fill(stairX, 2, cabinStart - 1, stairX, 3, cabinStart - 1, Blocks.SPRUCE_LOG.defaultBlockState());
        b.stair(stairX, 4, cabinStart - 1, Direction.SOUTH);
        b.block(cx, 5, cabinStart + 1, palette.helm());
        b.block(cx + 2, 5, cabinStart + 1, palette.engine(tier));
        b.block(cx - 2, 5, cabinStart + 1, palette.seat());
        b.block(cx - cabinHalf + 1, 2, cabinEnd - 1, Blocks.CHEST.defaultBlockState());
        b.block(cx + cabinHalf - 1, 2, cabinEnd - 1, Blocks.FURNACE.defaultBlockState());
        b.block(cx + 1, 2, cargoEnd + 1, palette.ballast());

        int balloonY = maxHeight - balloonRows * 2;
        int bankWidth = balloonRows * 2;
        int leftX = x0 + 1, rightX = x0 + width - 1 - bankWidth;
        int frontZ = z0 + 3, rearZ = z0 + length - 3 - balloonDepth * 2;
        for (int bx : new int[]{leftX, rightX}) for (int bz : new int[]{frontZ, rearZ}) {
            b.balloonArray(bx, balloonY, bz, balloonRows, balloonDepth);
            int outerX = bx == leftX ? bx : bx + bankWidth - 1;
            b.support(outerX, 2, balloonY - 1, bz);
        }

        for (int z = 3; z < length - 3; z++) {
            b.decorIfEmpty(x0, 2, z0 + z, palette.railing());
            b.decorIfEmpty(x0 + width - 1, 2, z0 + z, palette.railing());
        }
        b.decorFill(cx - cargoHalf + 1, 5, cargoStart + 1, cx + cargoHalf - 1, 5, cargoStart + 2, palette.awning());
        b.decorIfEmpty(cx - cabinHalf, 5, cabinStart + 1, palette.lantern());
        b.decorIfEmpty(cx + cabinHalf, 5, cabinStart + 1, palette.lantern());
        b.decorIfEmpty(cx, 5, cabinEnd, palette.flag());
        return b.pattern(id, name, description);
    }

    private static int centeredX(DockTier tier, int width) { return (tier.width - width) / 2; }
    private record Palette(BlockState helm, BlockState seat, BlockState ballast, BlockState lift,
                           BlockState brassEngine, BlockState reinforcedEngine, BlockState turbineEngine, BlockState aetherEngine,
                           BlockState lantern, BlockState awning, BlockState railing, BlockState flag) {
        private BlockState engine(DockTier tier) {
            return switch (tier) {
                case SCOUT -> brassEngine;
                case BRIG -> reinforcedEngine;
                case CRUISER -> turbineEngine;
                case DREADNOUGHT -> aetherEngine;
            };
        }
        private static Palette production() {
            return new Palette(SkydockBlocks.HELM.get().defaultBlockState(), SkydockBlocks.SEAT.get().defaultBlockState(),
                    SkydockBlocks.BALLAST.get().defaultBlockState(), SkydockBlocks.LIFT_CELL.get().defaultBlockState(),
                    SkydockBlocks.ENGINE.get().defaultBlockState(), SkydockBlocks.ENGINE_REINFORCED.get().defaultBlockState(),
                    SkydockBlocks.ENGINE_TURBINE.get().defaultBlockState(), SkydockBlocks.ENGINE_AETHER.get().defaultBlockState(),
                    SkydockBlocks.BRASS_LANTERN.get().defaultBlockState(), SkydockBlocks.CANVAS_AWNING.get().defaultBlockState(),
                    SkydockBlocks.TIMBER_RAILING.get().defaultBlockState(), SkydockBlocks.SIGNAL_FLAG.get().defaultBlockState());
        }
        private static Palette vanillaTestPalette() {
            return new Palette(Blocks.LECTERN.defaultBlockState(), Blocks.OAK_STAIRS.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
                    Blocks.WHITE_WOOL.defaultBlockState(), Blocks.PISTON.defaultBlockState(), Blocks.STICKY_PISTON.defaultBlockState(),
                    Blocks.BLAST_FURNACE.defaultBlockState(), Blocks.SMOKER.defaultBlockState(), Blocks.LANTERN.defaultBlockState(),
                    Blocks.CYAN_WOOL.defaultBlockState(), Blocks.OAK_FENCE.defaultBlockState(), Blocks.CYAN_BANNER.defaultBlockState());
        }
    }

    private static final class Builder {
        private final DockTier tier;
        private final Palette palette;
        private final LinkedHashMap<BlockPos, ShipPattern.Block> cells = new LinkedHashMap<>();

        private Builder(DockTier tier, Palette palette) { this.tier = tier; this.palette = palette; }

        private void block(int x, int y, int z, BlockState state) { put(x, y, z, state, false, false); }
        private void decorIfEmpty(int x, int y, int z, BlockState state) { put(x, y, z, state, true, true); }
        private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) block(x, y, z, state);
        }
        private void decorFill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
            for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) decorIfEmpty(x, y, z, state);
        }
        private void put(int x, int y, int z, BlockState state, boolean decoration, boolean onlyIfEmpty) {
            BlockPos pos = new BlockPos(x, y, z);
            ShipPattern.Block previous = cells.get(pos);
            if (previous == null) cells.put(pos, new ShipPattern.Block(pos, state, decoration));
            else if (!onlyIfEmpty && (previous.state().getBlock() != state.getBlock() || previous.decoration() != decoration))
                throw new IllegalStateException("Conflicting authored blocks at " + pos + ": " + previous.state().getBlock() + " and " + state.getBlock());
        }
        private void stair(int x, int y, int z, Direction facing) {
            block(x, y, z, Blocks.SPRUCE_STAIRS.defaultBlockState()
                    .setValue(StairBlock.FACING, facing).setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));
        }
        private void support(int x, int minY, int maxY, int z) {
            fill(x, minY, z, x, maxY, z, Blocks.SPRUCE_LOG.defaultBlockState());
        }
        private void balloonArray(int x, int y, int z, int rowsX, int rowsZ) {
            fill(x, y, z, x + rowsX * 2 - 1, y + rowsX * 2 - 1, z + rowsZ * 2 - 1, palette.lift());
        }
        private void edgeRailingsForCutter(int cx, int width, int z0, int length, int bowRun) {
            int fullHalf = width / 2;
            int previousHalf = -1;
            for (int z = 2; z < length - 3; z++) {
                int half = z < bowRun ? Math.max(1, (fullHalf * z + bowRun - 1) / bowRun) : fullHalf;
                int innerHalf = previousHalf < 0 ? half : Math.min(previousHalf, half);
                for (int offset = innerHalf; offset <= half; offset++) {
                    decorIfEmpty(cx - offset, 2, z0 + z, palette.railing());
                    decorIfEmpty(cx + offset, 2, z0 + z, palette.railing());
                }
                previousHalf = half;
            }
        }
        private ShipPattern pattern(String id, String name, String description) {
            resolveNeighborStates();
            return new ShipPattern(Skydock.id(id), name, description, tier, List.copyOf(cells.values()));
        }
        private void resolveNeighborStates() {
            for (Map.Entry<BlockPos, ShipPattern.Block> entry : cells.entrySet()) {
                ShipPattern.Block cell = entry.getValue();
                BlockState state = cell.state();
                if (state.getBlock() == palette.lift().getBlock() && state.hasProperty(LiftCellBlock.NORTH)) {
                    state = state.setValue(LiftCellBlock.NORTH, isLift(entry.getKey().north()));
                    state = state.setValue(LiftCellBlock.EAST, isLift(entry.getKey().east()));
                    state = state.setValue(LiftCellBlock.SOUTH, isLift(entry.getKey().south()));
                    state = state.setValue(LiftCellBlock.WEST, isLift(entry.getKey().west()));
                    state = state.setValue(LiftCellBlock.UP, isLift(entry.getKey().above()));
                    state = state.setValue(LiftCellBlock.DOWN, isLift(entry.getKey().below()));
                } else if (state.getBlock() instanceof IronBarsBlock) {
                    IronBarsBlock pane = (IronBarsBlock) state.getBlock();
                    state = state.setValue(CrossCollisionBlock.NORTH, paneConnects(pane, entry.getKey().north(), Direction.SOUTH));
                    state = state.setValue(CrossCollisionBlock.EAST, paneConnects(pane, entry.getKey().east(), Direction.WEST));
                    state = state.setValue(CrossCollisionBlock.SOUTH, paneConnects(pane, entry.getKey().south(), Direction.NORTH));
                    state = state.setValue(CrossCollisionBlock.WEST, paneConnects(pane, entry.getKey().west(), Direction.EAST));
                } else if (state.getBlock() instanceof FenceBlock) {
                    state = FenceConnections.resolve(state, entry.getKey(), this::stateAt, EmptyBlockGetter.INSTANCE);
                }
                entry.setValue(new ShipPattern.Block(entry.getKey(), state, cell.decoration()));
            }
        }
        private boolean isLift(BlockPos pos) {
            ShipPattern.Block cell = cells.get(pos);
            return cell != null && cell.state().getBlock() == palette.lift().getBlock();
        }
        private BlockState stateAt(BlockPos pos) {
            ShipPattern.Block cell = cells.get(pos);
            return cell == null ? Blocks.AIR.defaultBlockState() : cell.state();
        }
        private boolean paneConnects(IronBarsBlock pane, BlockPos neighborPos, Direction faceTowardPane) {
            ShipPattern.Block neighbor = cells.get(neighborPos);
            return neighbor != null && pane.attachsTo(neighbor.state(),
                    neighbor.state().isFaceSturdy(EmptyBlockGetter.INSTANCE, neighborPos, faceTowardPane));
        }
    }
}
