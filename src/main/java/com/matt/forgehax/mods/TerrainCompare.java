package com.matt.forgehax.mods;

import com.matt.forgehax.util.gen.ChunkProviderServerUtility;
import com.matt.forgehax.util.gen.TerrainCompareUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.world.chunk.Chunk;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  TerrainCompareUtils utils;
  Chunk chunk;

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
  }

  @Override
  protected void onEnabled() {
    utils = new TerrainCompareUtils();
    ChunkProviderServerUtility chunkProviderServerUtility = utils.worldServer.getChunkProvider();
    chunk = chunkProviderServerUtility.provideChunk(0, 0);
  }

  @Override
  protected void onDisabled() {
    for (int j = 0; j < 66; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          System.out.println(i + " " + j + " " + k + " " + chunk.getBlockState(i, j, k).getBlock().getLocalizedName());
          }
        }
      }
    }
}
