package com.matt.forgehax.util.gen;

import com.matt.forgehax.util.gen.WorldServerUtility;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.storage.WorldInfo;
import scala.collection.parallel.ParIterableLike;

import java.io.File;

public class TerrainCompareUtils {

  WorldSettings worldSettings;
  WorldInfo worldInfo;
  public WorldServerUtility worldServer;

    public TerrainCompareUtils(){
      this.worldSettings = new WorldSettings(
          -4172144997902289642L,
          GameType.SURVIVAL,
          true,
          false,
          WorldType.DEFAULT);
      try {
        this.worldInfo = Minecraft.getMinecraft().world.getWorldInfo();
      } catch (NullPointerException e) {
        this.worldInfo = new WorldInfo(worldSettings, "TerrainCompare");
      }
      this.worldInfo.populateFromWorldSettings(worldSettings);

      this.worldServer = new WorldServerUtility(
          new AnvilSaveHandler(new File(Minecraft.getMinecraft().mcDataDir + "/forgehax/cache"), "TerrainCompareUtils", false, Minecraft.getMinecraft().getDataFixer()),
          worldInfo,
          0,
          new Profiler());
    }

    public IBlockState getBlockState(final BlockPos pos) {
      generateChunkSquare(pos.getX() >> 4, pos.getZ() >> 4);
      return worldServer.getBlockState(pos);
    }

  /**
   *
   * ensures the chunk in the center gets populated
   *
   * @param x in chunk pos of the chunk in the middle
   * @param z in chunk pos
   */
    public void generateChunkSquare(final int x, final int z) {
      for (int i = -1; i < 2; i++) {
        for (int j = -1; j < 2; j++) {
          worldServer.getChunkProvider().provideChunk(x + i, z + j);
          worldServer.tick(); // TODO is this a good idea? maybe have a setting how many ticks to do before population and then after
        }
      }
    }


}
