package mod.fossilsarch2.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import mod.fossilsarch2.FossilsArch2Mod;
import mod.fossilsarch2.block.FernBlock;
import mod.fossilsarch2.block.entity.FeederBlockEntity;
import mod.fossilsarch2.dinosaur.Dinosaur;
import mod.fossilsarch2.dinosaur.DinosaurUtils;
import mod.fossilsarch2.entity.DinoEggEntity;
import mod.fossilsarch2.entity.DinosaurEntity;
import mod.fossilsarch2.registry.ModBlocks;
import mod.fossilsarch2.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Server-side GameTests for the core dinosaur loop: growth, feeding, hunting and hatching,
 * plus the species data contract. Each test runs in {@code fossilsarch2:pen}, a 13x5x13 grass
 * floor walled with barriers; positions below are relative to that structure.
 *
 * <p>Run with {@code ./gradlew runGameTestServer}, or in a dev world with {@code /test runall}.
 */
@EventBusSubscriber(modid = FossilsArch2Mod.MOD_ID)
public final class FossilsGameTests {

    private static final Identifier PEN = id("pen");
    private static final BlockPos CENTRE = new BlockPos(6, 1, 6);
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    private static final int HUNGRY = 10;

    /** Test name -> (max ticks, body). Registration order is execution order. */
    private static final Map<String, TestCase> TESTS = new LinkedHashMap<>();

    private record TestCase(int maxTicks, Consumer<GameTestHelper> body) {}

    static {
        TESTS.put("species_codec_rejects_invalid_data",
                new TestCase(20, FossilsGameTests::speciesCodecRejectsInvalidData));
        TESTS.put("species_ids_default_namespace", new TestCase(20, FossilsGameTests::speciesIdsDefaultNamespace));
        TESTS.put("hatchling_stats_scale_with_growth",
                new TestCase(40, FossilsGameTests::hatchlingStatsScaleWithGrowth));
        TESTS.put("growth_progress_survives_reload", new TestCase(60, FossilsGameTests::growthProgressSurvivesReload));
        TESTS.put("adult_herbivore_eats_from_feeder",
                new TestCase(400, FossilsGameTests::adultHerbivoreEatsFromFeeder));
        TESTS.put("herbivore_grazes_mature_fern", new TestCase(400, FossilsGameTests::herbivoreGrazesMatureFern));
        TESTS.put("carnivore_ignores_ferns", new TestCase(240, FossilsGameTests::carnivoreIgnoresFerns));
        TESTS.put("sated_predator_leaves_livestock", new TestCase(240, FossilsGameTests::satedPredatorLeavesLivestock));
        TESTS.put("hungry_predator_hunts_and_eats", new TestCase(600, FossilsGameTests::hungryPredatorHuntsAndEats));
        TESTS.put("egg_hatches_in_light", new TestCase(100, FossilsGameTests::eggHatchesInLight));
    }

    private FossilsGameTests() {}

    // --- Registration ---

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION,
                helper -> TESTS.forEach((name, test) -> helper.register(id(name), test.body())));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        TESTS.forEach((name, test) -> event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, PEN, test.maxTicks(), 0, true))));
    }

    // --- Species data ---

    private static void speciesCodecRejectsInvalidData(GameTestHelper helper) {
        String valid = """
                {"display_name": "Test", "diet": "carnivore",
                 "combat": {"health": 10, "speed": 0.2, "attack_damage": 2},
                 "growth": {"width": 1, "height": 1, "baby_scale": 0.5, "max_age": 4, "grow_time": 100, "hatch_time": 100},
                 "food": {"meat_nutrition": 1, "meat_saturation": 0.1, "cooked_meat_nutrition": 2,
                          "cooked_meat_saturation": 0.2, "meat_drop_count": 1}}""";
        helper.assertTrue(parse(valid).isSuccess(), "valid species JSON should parse");
        helper.assertTrue(parse(valid.replace("\"grow_time\": 100", "\"grow_time\": 0")).isError(),
                "grow_time 0 must be rejected (it is a tick divisor)");
        helper.assertTrue(parse(valid.replace("\"carnivore\"", "\"CARNIVORE_ISH\"")).isError(),
                "unknown diet must be a parse error, not an exception");
        helper.assertTrue(parse(valid.replace("\"baby_scale\": 0.5", "\"baby_scale\": 2.0")).isError(),
                "baby_scale above adult_scale must be rejected");
        helper.succeed();
    }

    private static com.mojang.serialization.DataResult<Dinosaur> parse(String json) {
        return Dinosaur.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static void speciesIdsDefaultNamespace(GameTestHelper helper) {
        helper.assertTrue(id("triceratops").equals(DinosaurUtils.parseSpeciesId("triceratops")),
                "bare (pre-namespace) species IDs default to fossilsarch2");
        helper.assertTrue(
                Identifier.fromNamespaceAndPath("addon", "raptor").equals(DinosaurUtils.parseSpeciesId("addon:raptor")),
                "namespaced species IDs are kept");
        helper.assertTrue(DinosaurUtils.get(helper.getLevel(), id("triceratops")) != null,
                "built-in species resolve from the datapack registry");
        helper.succeed();
    }

    // --- Growth ---

    private static void hatchlingStatsScaleWithGrowth(GameTestHelper helper) {
        DinosaurEntity dino = helper.spawn(species("triceratops"), CENTRE);
        double adultHealth = dino.getDinosaur().combat().health();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(dino.isJuvenile(), "a fresh dinosaur is a hatchling");
            assertNear(helper, dino.getAttributeValue(Attributes.MAX_HEALTH), adultHealth * 0.35,
                    "hatchling max health");
            dino.setDinoAge(dino.getDinosaur().growth().maxAge());
            assertNear(helper, dino.getAttributeValue(Attributes.MAX_HEALTH), adultHealth, "adult max health");
            helper.assertTrue(!dino.isJuvenile(), "max age is adult");
            helper.succeed();
        });
    }

    private static void growthProgressSurvivesReload(GameTestHelper helper) {
        DinosaurEntity original = helper.spawn(species("triceratops"), CENTRE);
        int growTime = original.getDinosaur().growth().growTime();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(original.getGrowthTicks() > 0, "growth ticks accumulate");

            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess());
            original.saveWithoutId(out);
            CompoundTag saved = out.buildResult();
            helper.assertTrue(saved.getIntOr("GrowthTicks", -1) == original.getGrowthTicks(), "growth ticks are saved");

            // Reload as if the chunk unloaded just before the next growth stage.
            saved.putInt("GrowthTicks", growTime - 2);
            saved.remove("UUID");
            original.discard();
            DinosaurEntity reloaded = species("triceratops").create(helper.getLevel(), EntitySpawnReason.LOAD);
            reloaded.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
            helper.getLevel().addFreshEntity(reloaded);

            helper.succeedWhen(
                    () -> helper.assertTrue(reloaded.getDinoAge() == 1, "reloaded dinosaur grows on schedule"));
        });
    }

    // --- Feeding ---

    private static void adultHerbivoreEatsFromFeeder(GameTestHelper helper) {
        helper.setBlock(CENTRE, ModBlocks.FEEDER.get());
        FeederBlockEntity feeder = helper.getBlockEntity(CENTRE, FeederBlockEntity.class);
        feeder.setItem(FeederBlockEntity.VEG_SLOT, new ItemStack(Items.WHEAT, 64));

        DinosaurEntity dino = hungryAdult(helper, "triceratops", CORNER);
        helper.succeedWhen(() -> helper.assertTrue(dino.getHunger() > HUNGRY,
                "a full-size triceratops (hitbox wider than the old fixed reach) eats from the feeder"));
    }

    private static void herbivoreGrazesMatureFern(GameTestHelper helper) {
        helper.setBlock(CENTRE, ModBlocks.FERN.get().defaultBlockState().setValue(FernBlock.AGE, FernBlock.MAX_AGE));
        DinosaurEntity dino = hungryAdult(helper, "triceratops", CORNER);
        helper.succeedWhen(() -> {
            helper.assertTrue(dino.getHunger() > HUNGRY, "grazing restores hunger");
            helper.assertTrue(helper.getBlockState(CENTRE).getValue(FernBlock.AGE) < FernBlock.MAX_AGE,
                    "the fern is eaten down");
        });
    }

    private static void carnivoreIgnoresFerns(GameTestHelper helper) {
        helper.setBlock(CENTRE, ModBlocks.FERN.get().defaultBlockState().setValue(FernBlock.AGE, FernBlock.MAX_AGE));
        DinosaurEntity dino = hungryAdult(helper, "tyrannosaurus", CORNER);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(helper.getBlockState(CENTRE).is(ModBlocks.FERN.get())
                    && helper.getBlockState(CENTRE).getValue(FernBlock.AGE) == FernBlock.MAX_AGE,
                    "a carnivore leaves ferns alone");
            helper.assertTrue(dino.getHunger() <= HUNGRY, "and gains nothing from them");
            helper.succeed();
        });
    }

    // --- Hunting ---

    private static void satedPredatorLeavesLivestock(GameTestHelper helper) {
        DinosaurEntity rex = adult(helper, "tyrannosaurus", CORNER);
        Pig pig = helper.spawn(EntityTypes.PIG, CENTRE);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(!rex.isHungry(), "precondition: the predator is fed");
            helper.assertTrue(pig.isAlive(), "a sated predator does not hunt");
            helper.succeed();
        });
    }

    private static void hungryPredatorHuntsAndEats(GameTestHelper helper) {
        DinosaurEntity rex = hungryAdult(helper, "tyrannosaurus", CORNER);
        Pig pig = helper.spawn(EntityTypes.PIG, CENTRE);
        helper.succeedWhen(() -> {
            helper.assertTrue(!pig.isAlive(), "a hungry predator hunts nearby prey");
            helper.assertTrue(rex.getHunger() > HUNGRY, "and the kill restores hunger");
        });
    }

    // --- Hatching ---

    private static void eggHatchesInLight(GameTestHelper helper) {
        Identifier speciesId = id("triceratops");
        helper.setBlock(CENTRE.east(), Blocks.GLOWSTONE);
        DinoEggEntity egg = helper.spawn(ModEntities.DINO_EGG.get(), CENTRE);
        egg.setSpeciesId(speciesId);
        egg.setHatchProgress(DinosaurUtils.get(helper.getLevel(), speciesId).growth().hatchTime() - 5);
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(species("triceratops"));
            helper.assertTrue(egg.isRemoved(), "the egg is consumed when it hatches");
        });
    }

    // --- Helpers ---

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(FossilsArch2Mod.MOD_ID, path);
    }

    private static EntityType<DinosaurEntity> species(String path) {
        return ModEntities.TYPES.get(path).get();
    }

    private static DinosaurEntity adult(GameTestHelper helper, String path, BlockPos pos) {
        DinosaurEntity dino = helper.spawn(species(path), pos);
        dino.setDinoAge(dino.getDinosaur().growth().maxAge());
        return dino;
    }

    private static DinosaurEntity hungryAdult(GameTestHelper helper, String path, BlockPos pos) {
        DinosaurEntity dino = adult(helper, path, pos);
        dino.setHunger(HUNGRY);
        return dino;
    }

    private static void assertNear(GameTestHelper helper, double actual, double expected, String what) {
        helper.assertTrue(Math.abs(actual - expected) < 0.01, what + ": expected " + expected + " but was " + actual);
    }
}
