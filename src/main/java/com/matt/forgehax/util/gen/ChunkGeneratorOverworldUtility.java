package com.matt.forgehax.util.gen;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.structure.*;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ChunkGeneratorOverworldUtility implements IChunkGenerator
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation[] SPAWNERTYPES = new ResourceLocation[]{EntityList.getKey(EntitySkeleton.class), EntityList.getKey(EntityZombie.class), EntityList.getKey(EntityZombie.class), EntityList.getKey(EntitySpider.class)};
    protected static final IBlockState STONE = Blocks.STONE.getDefaultState();
    World world;
    WorldSettings worldSettings;
    WorldType worldType;
    GameType gameType;
    BiomeProvider biomeProvider;
    private final Random rand;
    private NoiseGeneratorOctaves minLimitPerlinNoise;
    private NoiseGeneratorOctaves maxLimitPerlinNoise;
    private NoiseGeneratorOctaves mainPerlinNoise;
    private NoiseGeneratorPerlin surfaceNoise;
    public NoiseGeneratorOctaves scaleNoise;
    public NoiseGeneratorOctaves depthNoise;
    public NoiseGeneratorOctaves forestNoise;
    private final boolean mapFeaturesEnabled;
    private final WorldType terrainType;
    private final double[] heightMap;
    private final float[] biomeWeights;
    private ChunkGeneratorSettings settings;
    private IBlockState oceanBlock = Blocks.WATER.getDefaultState();
    private double[] depthBuffer = new double[256];
    private MapGenBase caveGenerator = new MapGenCaves();
    private MapGenStronghold strongholdGenerator = new MapGenStronghold();
    private MapGenVillage villageGenerator = new MapGenVillage();
    private MapGenMineshaft mineshaftGenerator = new MapGenMineshaft();
    private MapGenScatteredFeature scatteredFeatureGenerator = new MapGenScatteredFeature();
    private MapGenBase ravineGenerator = new MapGenRavine();
    private StructureOceanMonument oceanMonumentGenerator = new StructureOceanMonument();
    private WoodlandMansionUtility woodlandMansionGenerator = new WoodlandMansionUtility(this);
    private Biome[] biomesForGeneration;
    double[] mainNoiseRegion;
    double[] minLimitRegion;
    double[] maxLimitRegion;
    double[] depthRegion;

    public ChunkGeneratorOverworldUtility(World worldInput, long seed, boolean mapFeaturesEnabledIn, String generatorOptions)
    {
        {
            caveGenerator = TerrainGen.getModdedMapGen(caveGenerator, InitMapGenEvent.EventType.CAVE);
            strongholdGenerator = (MapGenStronghold) TerrainGen.getModdedMapGen(strongholdGenerator, InitMapGenEvent.EventType.STRONGHOLD);
            villageGenerator = (MapGenVillage) TerrainGen.getModdedMapGen(villageGenerator, InitMapGenEvent.EventType.VILLAGE);
            mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(mineshaftGenerator, InitMapGenEvent.EventType.MINESHAFT);
            scatteredFeatureGenerator = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(scatteredFeatureGenerator, InitMapGenEvent.EventType.SCATTERED_FEATURE);
            ravineGenerator = TerrainGen.getModdedMapGen(ravineGenerator, InitMapGenEvent.EventType.RAVINE);
            oceanMonumentGenerator = (StructureOceanMonument) TerrainGen.getModdedMapGen(oceanMonumentGenerator, InitMapGenEvent.EventType.OCEAN_MONUMENT);
            woodlandMansionGenerator = (WoodlandMansionUtility) TerrainGen.getModdedMapGen(woodlandMansionGenerator, InitMapGenEvent.EventType.WOODLAND_MANSION);
        }
        this.mapFeaturesEnabled = mapFeaturesEnabledIn;
        this.terrainType = WorldType.DEFAULT;
        this.rand = new Random(seed);
        this.minLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.maxLimitPerlinNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.mainPerlinNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.surfaceNoise = new NoiseGeneratorPerlin(this.rand, 4);
        this.scaleNoise = new NoiseGeneratorOctaves(this.rand, 10);
        this.depthNoise = new NoiseGeneratorOctaves(this.rand, 16);
        this.forestNoise = new NoiseGeneratorOctaves(this.rand, 8);
        this.heightMap = new double[825];
        this.biomeWeights = new float[25];
        this.world = worldInput;
        this.gameType = GameType.SURVIVAL;
        this.worldType = WorldType.DEFAULT;
        this.worldSettings = new WorldSettings(seed, gameType, true, false, worldType);
        this.world.getWorldInfo().populateFromWorldSettings(worldSettings);
        this.biomeProvider = new BiomeProvider(world.getWorldInfo());

        for (int i = -2; i <= 2; ++i)
        {
            for (int j = -2; j <= 2; ++j)
            {
                float f = 10.0F / MathHelper.sqrt((float)(i * i + j * j) + 0.2F);
                this.biomeWeights[i + 2 + (j + 2) * 5] = f;
            }
        }

        if (generatorOptions != null)
        {
            this.settings = ChunkGeneratorSettings.Factory.jsonToFactory(generatorOptions).build();
            this.oceanBlock = this.settings.useLavaOceans ? Blocks.LAVA.getDefaultState() : Blocks.WATER.getDefaultState();
            // worldIn.setSeaLevel(this.settings.seaLevel);
        }

        InitNoiseGensEvent.ContextOverworld ctx =
                new InitNoiseGensEvent.ContextOverworld(minLimitPerlinNoise, maxLimitPerlinNoise, mainPerlinNoise, surfaceNoise, scaleNoise, depthNoise, forestNoise);
        ctx = TerrainGen.getModdedNoiseGenerators(this.world, this.rand, ctx);
        this.minLimitPerlinNoise = ctx.getLPerlin1();
        this.maxLimitPerlinNoise = ctx.getLPerlin2();
        this.mainPerlinNoise = ctx.getPerlin();
        this.surfaceNoise = ctx.getHeight();
        this.scaleNoise = ctx.getScale();
        this.depthNoise = ctx.getDepth();
        this.forestNoise = ctx.getForest();
    }

    public void setBlocksInChunk(int x, int z, ChunkPrimer primer)
    {
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomesForGeneration(this.biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10);
        this.generateHeightmap(x * 4, 0, z * 4);

        for (int i = 0; i < 4; ++i)
        {
            int j = i * 5;
            int k = (i + 1) * 5;

            for (int l = 0; l < 4; ++l)
            {
                int i1 = (j + l) * 33;
                int j1 = (j + l + 1) * 33;
                int k1 = (k + l) * 33;
                int l1 = (k + l + 1) * 33;

                for (int i2 = 0; i2 < 32; ++i2)
                {
                    double d0 = 0.125D;
                    double d1 = this.heightMap[i1 + i2];
                    double d2 = this.heightMap[j1 + i2];
                    double d3 = this.heightMap[k1 + i2];
                    double d4 = this.heightMap[l1 + i2];
                    double d5 = (this.heightMap[i1 + i2 + 1] - d1) * 0.125D;
                    double d6 = (this.heightMap[j1 + i2 + 1] - d2) * 0.125D;
                    double d7 = (this.heightMap[k1 + i2 + 1] - d3) * 0.125D;
                    double d8 = (this.heightMap[l1 + i2 + 1] - d4) * 0.125D;

                    for (int j2 = 0; j2 < 8; ++j2)
                    {
                        double d9 = 0.25D;
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * 0.25D;
                        double d13 = (d4 - d2) * 0.25D;

                        for (int k2 = 0; k2 < 4; ++k2)
                        {
                            double d14 = 0.25D;
                            double d16 = (d11 - d10) * 0.25D;
                            double lvt_45_1_ = d10 - d16;

                            for (int l2 = 0; l2 < 4; ++l2)
                            {
                                if ((lvt_45_1_ += d16) > 0.0D)
                                {
                                    primer.setBlockState(i * 4 + k2, i2 * 8 + j2, l * 4 + l2, STONE);
                                }
                                else if (i2 * 8 + j2 < this.settings.seaLevel)
                                {
                                    primer.setBlockState(i * 4 + k2, i2 * 8 + j2, l * 4 + l2, this.oceanBlock);
                                }
                            }

                            d10 += d12;
                            d11 += d13;
                        }

                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    public void replaceBiomeBlocks(int x, int z, ChunkPrimer primer, Biome[] biomesIn)
    {
        if (!net.minecraftforge.event.ForgeEventFactory.onReplaceBiomeBlocks(this, x, z, primer, this.world)) return;
        double d0 = 0.03125D;
        this.depthBuffer = this.surfaceNoise.getRegion(this.depthBuffer, (double)(x * 16), (double)(z * 16), 16, 16, 0.0625D, 0.0625D, 1.0D);

        for (int i = 0; i < 16; ++i)
        {
            for (int j = 0; j < 16; ++j)
            {
                Biome biome = biomesIn[j + i * 16];
                biome.genTerrainBlocks(this.world, this.rand, primer, x * 16 + i, z * 16 + j, this.depthBuffer[j + i * 16]);
            }
        }
    }

    public Chunk generateChunk(int x, int z)
    {
        this.rand.setSeed((long)x * 341873128712L + (long)z * 132897987541L);
        ChunkPrimer chunkprimer = new ChunkPrimer();
        this.setBlocksInChunk(x, z, chunkprimer);
        this.biomesForGeneration = this.world.getBiomeProvider().getBiomes(this.biomesForGeneration, x * 16, z * 16, 16, 16);
        this.replaceBiomeBlocks(x, z, chunkprimer, this.biomesForGeneration);

        if (this.settings.useCaves)
        {
            this.caveGenerator.generate(this.world, x, z, chunkprimer);
        }

        if (this.settings.useRavines)
        {
            this.ravineGenerator.generate(this.world, x, z, chunkprimer);
        }

        if (this.mapFeaturesEnabled)
        {
            if (this.settings.useMineShafts)
            {
                this.mineshaftGenerator.generate(this.world, x, z, chunkprimer);
            }

            if (this.settings.useVillages)
            {
                this.villageGenerator.generate(this.world, x, z, chunkprimer);
            }

            if (this.settings.useStrongholds)
            {
                this.strongholdGenerator.generate(this.world, x, z, chunkprimer);
            }

            if (this.settings.useTemples)
            {
                this.scatteredFeatureGenerator.generate(this.world, x, z, chunkprimer);
            }

            if (this.settings.useMonuments)
            {
                this.oceanMonumentGenerator.generate(this.world, x, z, chunkprimer);
            }

            if (this.settings.useMansions)
            {
                this.woodlandMansionGenerator.generate(this.world, x, z, chunkprimer);
            }
        }

        Chunk chunk = new Chunk(this.world, chunkprimer, x, z);
        byte[] abyte = chunk.getBiomeArray();

        for (int i = 0; i < abyte.length; ++i)
        {
            abyte[i] = (byte)Biome.getIdForBiome(this.biomesForGeneration[i]);
        }

        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int i, int i1) {
        // nothing
    }

    private void generateHeightmap(int x, int y, int z)
    {
        this.depthRegion = this.depthNoise.generateNoiseOctaves(this.depthRegion, x, z, 5, 5, (double)this.settings.depthNoiseScaleX, (double)this.settings.depthNoiseScaleZ, (double)this.settings.depthNoiseScaleExponent);
        float f = this.settings.coordinateScale;
        float f1 = this.settings.heightScale;
        this.mainNoiseRegion = this.mainPerlinNoise.generateNoiseOctaves(this.mainNoiseRegion, x, y, z, 5, 33, 5, (double)(f / this.settings.mainNoiseScaleX), (double)(f1 / this.settings.mainNoiseScaleY), (double)(f / this.settings.mainNoiseScaleZ));
        this.minLimitRegion = this.minLimitPerlinNoise.generateNoiseOctaves(this.minLimitRegion, x, y, z, 5, 33, 5, (double)f, (double)f1, (double)f);
        this.maxLimitRegion = this.maxLimitPerlinNoise.generateNoiseOctaves(this.maxLimitRegion, x, y, z, 5, 33, 5, (double)f, (double)f1, (double)f);
        int i = 0;
        int j = 0;

        for (int k = 0; k < 5; ++k)
        {
            for (int l = 0; l < 5; ++l)
            {
                float f2 = 0.0F;
                float f3 = 0.0F;
                float f4 = 0.0F;
                int i1 = 2;
                Biome biome = this.biomesForGeneration[k + 2 + (l + 2) * 10];

                for (int j1 = -2; j1 <= 2; ++j1)
                {
                    for (int k1 = -2; k1 <= 2; ++k1)
                    {
                        Biome biome1 = this.biomesForGeneration[k + j1 + 2 + (l + k1 + 2) * 10];
                        float f5 = this.settings.biomeDepthOffSet + biome1.getBaseHeight() * this.settings.biomeDepthWeight;
                        float f6 = this.settings.biomeScaleOffset + biome1.getHeightVariation() * this.settings.biomeScaleWeight;

                        if (this.terrainType == WorldType.AMPLIFIED && f5 > 0.0F)
                        {
                            f5 = 1.0F + f5 * 2.0F;
                            f6 = 1.0F + f6 * 4.0F;
                        }

                        float f7 = this.biomeWeights[j1 + 2 + (k1 + 2) * 5] / (f5 + 2.0F);

                        if (biome1.getBaseHeight() > biome.getBaseHeight())
                        {
                            f7 /= 2.0F;
                        }

                        f2 += f6 * f7;
                        f3 += f5 * f7;
                        f4 += f7;
                    }
                }

                f2 = f2 / f4;
                f3 = f3 / f4;
                f2 = f2 * 0.9F + 0.1F;
                f3 = (f3 * 4.0F - 1.0F) / 8.0F;
                double d7 = this.depthRegion[j] / 8000.0D;

                if (d7 < 0.0D)
                {
                    d7 = -d7 * 0.3D;
                }

                d7 = d7 * 3.0D - 2.0D;

                if (d7 < 0.0D)
                {
                    d7 = d7 / 2.0D;

                    if (d7 < -1.0D)
                    {
                        d7 = -1.0D;
                    }

                    d7 = d7 / 1.4D;
                    d7 = d7 / 2.0D;
                }
                else
                {
                    if (d7 > 1.0D)
                    {
                        d7 = 1.0D;
                    }

                    d7 = d7 / 8.0D;
                }

                ++j;
                double d8 = (double)f3;
                double d9 = (double)f2;
                d8 = d8 + d7 * 0.2D;
                d8 = d8 * (double)this.settings.baseSize / 8.0D;
                double d0 = (double)this.settings.baseSize + d8 * 4.0D;

                for (int l1 = 0; l1 < 33; ++l1)
                {
                    double d1 = ((double)l1 - d0) * (double)this.settings.stretchY * 128.0D / 256.0D / d9;

                    if (d1 < 0.0D)
                    {
                        d1 *= 4.0D;
                    }

                    double d2 = this.minLimitRegion[i] / (double)this.settings.lowerLimitScale;
                    double d3 = this.maxLimitRegion[i] / (double)this.settings.upperLimitScale;
                    double d4 = (this.mainNoiseRegion[i] / 10.0D + 1.0D) / 2.0D;
                    double d5 = MathHelper.clampedLerp(d2, d3, d4) - d1;

                    if (l1 > 29)
                    {
                        double d6 = (double)((float)(l1 - 29) / 3.0F);
                        d5 = d5 * (1.0D - d6) + -10.0D * d6;
                    }

                    this.heightMap[i] = d5;
                    ++i;
                }
            }
        }
    }

    public void populate(int x, int z, Chunk chunk)
    {
        BlockFalling.fallInstantly = true;
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        Biome biome = this.world.getBiome(blockpos.add(16, 0, 16));
        this.rand.setSeed(this.world.getSeed());
        long k = this.rand.nextLong() / 2L * 2L + 1L;
        long l = this.rand.nextLong() / 2L * 2L + 1L;
        this.rand.setSeed((long)x * k + (long)z * l ^ this.world.getSeed());
        boolean flag = false;
        ChunkPos chunkpos = new ChunkPos(x, z);

        net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(true, this, this.world, this.rand, x, z, flag);

        if (this.mapFeaturesEnabled)
        {
            if (this.settings.useMineShafts)
            {
                this.mineshaftGenerator.generateStructure(this.world, this.rand, chunkpos);
            }

            if (this.settings.useVillages)
            {
                flag = this.villageGenerator.generateStructure(this.world, this.rand, chunkpos);
            }

            if (this.settings.useStrongholds)
            {
                this.strongholdGenerator.generateStructure(this.world, this.rand, chunkpos);
            }

            if (this.settings.useTemples)
            {
                this.scatteredFeatureGenerator.generateStructure(this.world, this.rand, chunkpos);
            }

            if (this.settings.useMonuments)
            {
                this.oceanMonumentGenerator.generateStructure(this.world, this.rand, chunkpos);
            }

            if (this.settings.useMansions)
            {
                this.woodlandMansionGenerator.generateStructure(this.world, this.rand, chunkpos);
            }
        }

        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && this.settings.useWaterLakes && !flag && this.rand.nextInt(this.settings.waterLakeChance) == 0)
            if (net.minecraftforge.event.terraingen.TerrainGen.populate(this, this.world, this.rand, x, z, flag, net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAKE))
            {
                int i1 = this.rand.nextInt(16) + 8;
                int j1 = this.rand.nextInt(256);
                int k1 = this.rand.nextInt(16) + 8;
                generateLakes(chunk, this.rand, blockpos.add(i1, j1, k1), Blocks.WATER);
            }

        if (!flag && this.rand.nextInt(this.settings.lavaLakeChance / 10) == 0 && this.settings.useLavaLakes)
            if (net.minecraftforge.event.terraingen.TerrainGen.populate(this, this.world, this.rand, x, z, flag, net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.LAVA))
            {
                int i2 = this.rand.nextInt(16) + 8;
                int l2 = this.rand.nextInt(this.rand.nextInt(248) + 8);
                int k3 = this.rand.nextInt(16) + 8;

                if (l2 < this.world.getSeaLevel() || this.rand.nextInt(this.settings.lavaLakeChance / 8) == 0)
                {
                    generateLakes(chunk, this.rand, blockpos.add(i2, l2, k3), Blocks.LAVA);
                }
            }

        if (this.settings.useDungeons)
            if (net.minecraftforge.event.terraingen.TerrainGen.populate(this, this.world, this.rand, x, z, flag, net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.DUNGEON))
            {
                for (int j2 = 0; j2 < this.settings.dungeonChance; ++j2)
                {
                    int i3 = this.rand.nextInt(16) + 8;
                    int l3 = this.rand.nextInt(256);
                    int l1 = this.rand.nextInt(16) + 8;
                    generateDungeon(chunk, this.rand, convertBlockPos(blockpos.add(i3, l3, l1)));
                }
            }

        DecoratorUtility decoratorUtility = new DecoratorUtility(chunk, this.rand, biome, new BlockPos(i, 0, j),);
        decorate(chunk, this.rand, biome, new BlockPos(i, 0, j));
        if (net.minecraftforge.event.terraingen.TerrainGen.populate(this, this.world, this.rand, x, z, flag, net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ANIMALS))
            WorldEntitySpawner.performWorldGenSpawning(this.world, biome, i + 8, j + 8, 16, 16, this.rand);
        blockpos = blockpos.add(8, 0, 8);

        if (net.minecraftforge.event.terraingen.TerrainGen.populate(this, this.world, this.rand, x, z, flag, net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.ICE))
        {
            for (int k2 = 0; k2 < 16; ++k2)
            {
                for (int j3 = 0; j3 < 16; ++j3)
                {
                    BlockPos blockpos1 = this.world.getPrecipitationHeight(blockpos.add(k2, 0, j3));
                    BlockPos blockpos2 = blockpos1.down();

                    if (this.world.canBlockFreezeWater(blockpos2))
                    {
                        this.world.setBlockState(blockpos2, Blocks.ICE.getDefaultState(), 2);
                    }

                    if (this.world.canSnowAt(blockpos1, true))
                    {
                        this.world.setBlockState(blockpos1, Blocks.SNOW_LAYER.getDefaultState(), 2);
                    }
                }
            }
        }//Forge: End ICE

        net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(false, this, this.world, this.rand, x, z, flag);

        BlockFalling.fallInstantly = false;
    }

    public boolean generateDungeon(Chunk chunk, Random rand, BlockPos position)
  {
    int i = 3;
    int j = rand.nextInt(2) + 2;
    int k = -j - 1;
    int l = j + 1;
    int i1 = -1;
    int j1 = 4;
    int k1 = rand.nextInt(2) + 2;
    int l1 = -k1 - 1;
    int i2 = k1 + 1;
    int j2 = 0;

    for (int k2 = k; k2 <= l; ++k2)
    {
      for (int l2 = -1; l2 <= 4; ++l2)
      {
        for (int i3 = l1; i3 <= i2; ++i3)
        {
          BlockPos blockpos = position.add(k2, l2, i3);
          Material material = chunk.getBlockState(blockpos).getMaterial();
          boolean flag = material.isSolid();

          if (l2 == -1 && !flag)
          {
            return false;
          }

          if (l2 == 4 && !flag)
          {
            return false;
          }

          if ((k2 == k || k2 == l || i3 == l1 || i3 == i2) && l2 == 0 && worldIn.isAirBlock(blockpos) && worldIn.isAirBlock(blockpos.up()))
          {
            ++j2;
          }
        }
      }
    }

    if (j2 >= 1 && j2 <= 5)
    {
      for (int k3 = k; k3 <= l; ++k3)
      {
        for (int i4 = 3; i4 >= -1; --i4)
        {
          for (int k4 = l1; k4 <= i2; ++k4)
          {
            BlockPos blockpos1 = position.add(k3, i4, k4);

            if (k3 != k && i4 != -1 && k4 != l1 && k3 != l && i4 != 4 && k4 != i2)
            {
              if (worldIn.getBlockState(blockpos1).getBlock() != Blocks.CHEST)
              {
                worldIn.setBlockToAir(blockpos1);
              }
            }
            else if (blockpos1.getY() >= 0 && !worldIn.getBlockState(blockpos1.down()).getMaterial().isSolid())
            {
              worldIn.setBlockToAir(blockpos1);
            }
            else if (worldIn.getBlockState(blockpos1).getMaterial().isSolid() && worldIn.getBlockState(blockpos1).getBlock() != Blocks.CHEST)
            {
              if (i4 == -1 && rand.nextInt(4) != 0)
              {
                worldIn.setBlockState(blockpos1, Blocks.MOSSY_COBBLESTONE.getDefaultState(), 2);
              }
              else
              {
                worldIn.setBlockState(blockpos1, Blocks.COBBLESTONE.getDefaultState(), 2);
              }
            }
          }
        }
      }

      for (int l3 = 0; l3 < 2; ++l3)
      {
        for (int j4 = 0; j4 < 3; ++j4)
        {
          int l4 = position.getX() + rand.nextInt(j * 2 + 1) - j;
          int i5 = position.getY();
          int j5 = position.getZ() + rand.nextInt(k1 * 2 + 1) - k1;
          BlockPos blockpos2 = new BlockPos(l4, i5, j5);

          if (worldIn.isAirBlock(blockpos2))
          {
            int j3 = 0;

            for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
            {
              if (worldIn.getBlockState(blockpos2.offset(enumfacing)).getMaterial().isSolid())
              {
                ++j3;
              }
            }

            if (j3 == 1)
            {
              worldIn.setBlockState(blockpos2, Blocks.CHEST.correctFacing(worldIn, blockpos2, Blocks.CHEST.getDefaultState()), 2);
              TileEntity tileentity1 = worldIn.getTileEntity(blockpos2);

              if (tileentity1 instanceof TileEntityChest)
              {
                ((TileEntityChest)tileentity1).setLootTable(LootTableList.CHESTS_SIMPLE_DUNGEON, rand.nextLong());
              }

              break;
            }
          }
        }
      }

      worldIn.setBlockState(position, Blocks.MOB_SPAWNER.getDefaultState(), 2);
      TileEntity tileentity = worldIn.getTileEntity(position);

      if (tileentity instanceof TileEntityMobSpawner)
      {
        ((TileEntityMobSpawner)tileentity).getSpawnerBaseLogic().setEntityId(this.pickMobSpawner(rand));
      }
      else
      {
        LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", Integer.valueOf(position.getX()), Integer.valueOf(position.getY()), Integer.valueOf(position.getZ()));
      }

      return true;
    }
    else
    {
      return false;
    }
  }

  /**
   * Randomly decides which spawner to use in a dungeon
   */
  private ResourceLocation pickMobSpawner(Random rand)
  {
    return net.minecraftforge.common.DungeonHooks.getRandomDungeonMob(rand);
  }

    public boolean generateStructures(Chunk chunkIn, int x, int z)
    {
        boolean flag = false;

        if (this.settings.useMonuments && this.mapFeaturesEnabled && chunkIn.getInhabitedTime() < 3600L)
        {
            flag |= this.oceanMonumentGenerator.generateStructure(this.world, this.rand, new ChunkPos(x, z));
        }

        return flag;
    }

    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos)
    {
        Biome biome = this.world.getBiome(pos);

        if (this.mapFeaturesEnabled)
        {
            if (creatureType == EnumCreatureType.MONSTER && this.scatteredFeatureGenerator.isSwampHut(pos))
            {
                return this.scatteredFeatureGenerator.getMonsters();
            }

            if (creatureType == EnumCreatureType.MONSTER && this.settings.useMonuments && this.oceanMonumentGenerator.isPositionInStructure(this.world, pos))
            {
                return this.oceanMonumentGenerator.getMonsters();
            }
        }

        return biome.getSpawnableList(creatureType);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World world, String s, BlockPos blockPos, boolean b) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunk, int i, int i1) {

    }

    // converts world blockpos to chunk block pos
    public BlockPos convertBlockPos(BlockPos blockPos) {
        return  new BlockPos(blockPos.getX() % 16, blockPos.getY(), blockPos.getZ() % 16);
    }

    public boolean isAirBlock(Chunk chunk, BlockPos position) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
        if (chunk.getBlockState(position) == Blocks.AIR) {
            return  true;
        } else {
            return false;
        }

    }

    boolean isWater(BlockPos pos, Chunk chunk)
    {
        return chunk.getBlockState(pos).getMaterial() == Material.WATER;
    }

    public boolean canBlockFreezeBody(BlockPos pos, boolean noWaterAdj, Chunk chunk)
    {
        Biome biome = biomeProvider.getBiome(pos);
        float f = biome.getTemperature(pos);

        if (f >= 0.15F)
        {
            return false;
        }
        else
        {
            if (pos.getY() >= 0 && pos.getY() < 256 && chunk.getLightFor(EnumSkyBlock.BLOCK, pos) < 10)
            {
                IBlockState iblockstate1 = chunk.getBlockState(pos);
                Block block = iblockstate1.getBlock();

                if ((block == Blocks.WATER || block == Blocks.FLOWING_WATER) && ((Integer)iblockstate1.getValue(BlockLiquid.LEVEL)).intValue() == 0)
                {
                    if (!noWaterAdj)
                    {
                        return true;
                    }

                    boolean flag = isWater(pos.west(), chunk) && this.isWater(pos.east(), chunk) && this.isWater(pos.north(), chunk) && this.isWater(pos.south(), chunk);

                    if (!flag)
                    {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public boolean generateLakes(Chunk chunk, Random rand, BlockPos position, Block block) {

        for (position = position.add(-8, 0, -8); position.getY() > 5 && isAirBlock(chunk, convertBlockPos(position)); position = position.down())
        {
            ;
        }

        if (position.getY() <= 4)
        {
            return false;
        }
        else
        {
            position = position.down(4);
            boolean[] aboolean = new boolean[2048];
            int i = rand.nextInt(4) + 4;

            for (int j = 0; j < i; ++j)
            {
                double d0 = rand.nextDouble() * 6.0D + 3.0D;
                double d1 = rand.nextDouble() * 4.0D + 2.0D;
                double d2 = rand.nextDouble() * 6.0D + 3.0D;
                double d3 = rand.nextDouble() * (16.0D - d0 - 2.0D) + 1.0D + d0 / 2.0D;
                double d4 = rand.nextDouble() * (8.0D - d1 - 4.0D) + 2.0D + d1 / 2.0D;
                double d5 = rand.nextDouble() * (16.0D - d2 - 2.0D) + 1.0D + d2 / 2.0D;

                for (int l = 1; l < 15; ++l)
                {
                    for (int i1 = 1; i1 < 15; ++i1)
                    {
                        for (int j1 = 1; j1 < 7; ++j1)
                        {
                            double d6 = ((double)l - d3) / (d0 / 2.0D);
                            double d7 = ((double)j1 - d4) / (d1 / 2.0D);
                            double d8 = ((double)i1 - d5) / (d2 / 2.0D);
                            double d9 = d6 * d6 + d7 * d7 + d8 * d8;

                            if (d9 < 1.0D)
                            {
                                aboolean[(l * 16 + i1) * 8 + j1] = true;
                            }
                        }
                    }
                }
            }

            for (int k1 = 0; k1 < 16; ++k1)
            {
                for (int l2 = 0; l2 < 16; ++l2)
                {
                    for (int k = 0; k < 8; ++k)
                    {
                        boolean flag = !aboolean[(k1 * 16 + l2) * 8 + k] && (k1 < 15 && aboolean[((k1 + 1) * 16 + l2) * 8 + k] || k1 > 0 && aboolean[((k1 - 1) * 16 + l2) * 8 + k] || l2 < 15 && aboolean[(k1 * 16 + l2 + 1) * 8 + k] || l2 > 0 && aboolean[(k1 * 16 + (l2 - 1)) * 8 + k] || k < 7 && aboolean[(k1 * 16 + l2) * 8 + k + 1] || k > 0 && aboolean[(k1 * 16 + l2) * 8 + (k - 1)]);

                        if (flag)
                        {
                            Material material = chunk.getBlockState(convertBlockPos(position.add(k1, k, l2))).getMaterial();

                            if (k >= 4 && material.isLiquid())
                            {
                                return false;
                            }

                            if (k < 4 && !material.isSolid() && chunk.getBlockState(convertBlockPos(position.add(k1, k, l2))).getBlock() != block)
                            {
                                return false;
                            }
                        }
                    }
                }
            }

            for (int l1 = 0; l1 < 16; ++l1)
            {
                for (int i3 = 0; i3 < 16; ++i3)
                {
                    for (int i4 = 0; i4 < 8; ++i4)
                    {
                        if (aboolean[(l1 * 16 + i3) * 8 + i4])
                        {
                            chunk.setBlockState(convertBlockPos(position.add(l1, i4, i3)), i4 >= 4 ? Blocks.AIR.getDefaultState() : block.getDefaultState()); // removed flags = 2
                        }
                    }
                }
            }

            for (int i2 = 0; i2 < 16; ++i2)
            {
                for (int j3 = 0; j3 < 16; ++j3)
                {
                    for (int j4 = 4; j4 < 8; ++j4)
                    {
                        if (aboolean[(i2 * 16 + j3) * 8 + j4])
                        {
                            BlockPos blockpos = position.add(i2, j4 - 1, j3);

                            if (chunk.getBlockState(convertBlockPos(blockpos)).getBlock() == Blocks.DIRT && chunk.getLightFor(EnumSkyBlock.SKY, convertBlockPos(position.add(i2, j4, j3))) > 0)
                            {
                                Biome biome = chunk.getBiome(convertBlockPos(blockpos), biomeProvider);

                                if (biome.topBlock.getBlock() == Blocks.MYCELIUM)
                                {
                                    chunk.setBlockState(convertBlockPos(blockpos), Blocks.MYCELIUM.getDefaultState()); // removed flags = 2
                                }
                                else
                                {
                                    chunk.setBlockState(convertBlockPos(blockpos), Blocks.GRASS.getDefaultState()); // removed flags = 2
                                }
                            }
                        }
                    }
                }
            }

            if (block.getDefaultState().getMaterial() == Material.LAVA)
            {
                for (int j2 = 0; j2 < 16; ++j2)
                {
                    for (int k3 = 0; k3 < 16; ++k3)
                    {
                        for (int k4 = 0; k4 < 8; ++k4)
                        {
                            boolean flag1 = !aboolean[(j2 * 16 + k3) * 8 + k4] && (j2 < 15 && aboolean[((j2 + 1) * 16 + k3) * 8 + k4] || j2 > 0 && aboolean[((j2 - 1) * 16 + k3) * 8 + k4] || k3 < 15 && aboolean[(j2 * 16 + k3 + 1) * 8 + k4] || k3 > 0 && aboolean[(j2 * 16 + (k3 - 1)) * 8 + k4] || k4 < 7 && aboolean[(j2 * 16 + k3) * 8 + k4 + 1] || k4 > 0 && aboolean[(j2 * 16 + k3) * 8 + (k4 - 1)]);

                            if (flag1 && (k4 < 4 || rand.nextInt(2) != 0) && chunk.getBlockState(convertBlockPos(position.add(j2, k4, k3))).getMaterial().isSolid())
                            {
                                chunk.setBlockState(convertBlockPos(position.add(j2, k4, k3)), Blocks.STONE.getDefaultState()); // removed flags = 2
                            }
                        }
                    }
                }
            }

            if (block.getDefaultState().getMaterial() == Material.WATER)
            {
                for (int k2 = 0; k2 < 16; ++k2)
                {
                    for (int l3 = 0; l3 < 16; ++l3)
                    {
                        int l4 = 4;

                        if (canBlockFreezeBody(convertBlockPos(position.add(k2, 4, l3)), false, chunk))
                        {
                            int flag = net.minecraftforge.common.ForgeModContainer.fixVanillaCascading ? 2| 16 : 2; //Forge: With bit 5 unset, it will notify neighbors and load adjacent chunks.
                            chunk.setBlockState(convertBlockPos(position.add(k2, 4, l3)), Blocks.ICE.getDefaultState()); //Forge
                        }
                    }
                }
            }

            return true;
        }
    }

    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos)
    {
        if (!this.mapFeaturesEnabled)
        {
            return false;
        }
        else if ("Stronghold".equals(structureName) && this.strongholdGenerator != null)
        {
            return this.strongholdGenerator.isInsideStructure(pos);
        }
        else if ("Mansion".equals(structureName) && this.woodlandMansionGenerator != null)
        {
            return this.woodlandMansionGenerator.isInsideStructure(pos);
        }
        else if ("Monument".equals(structureName) && this.oceanMonumentGenerator != null)
        {
            return this.oceanMonumentGenerator.isInsideStructure(pos);
        }
        else if ("Village".equals(structureName) && this.villageGenerator != null)
        {
            return this.villageGenerator.isInsideStructure(pos);
        }
        else if ("Mineshaft".equals(structureName) && this.mineshaftGenerator != null)
        {
            return this.mineshaftGenerator.isInsideStructure(pos);
        }
        else
        {
            return "Temple".equals(structureName) && this.scatteredFeatureGenerator != null ? this.scatteredFeatureGenerator.isInsideStructure(pos) : false;
        }
    }
}
