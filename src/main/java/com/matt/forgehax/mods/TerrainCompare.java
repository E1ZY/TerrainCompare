package com.matt.forgehax.mods;

import com.matt.forgehax.util.gen.ChunkProviderServerUtility;
import com.matt.forgehax.util.gen.TerrainCompareUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.io.FileWriter;
import java.io.IOException;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  TerrainCompareUtils utils;
  Chunk chunk;
  FileWriter writer;
  int x;
  int z;

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
  }

  @Override
  protected void onEnabled() {
    x = 0;
    z = 0;
    utils = new TerrainCompareUtils();
    try {
      writer = new FileWriter("D:\\output.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
    ChunkProviderServerUtility chunkProviderServerUtility = utils.worldServer.getChunkProvider();

    for (int l1 = -48; l1 <= 48; l1 += 16)
    {
      for (int i2 = -48; i2 <= 48; i2 += 16)
      {
        chunkProviderServerUtility.provideChunk( MC.player.getPosition().getX() + l1 >> 4, MC.player.getPosition().getZ() + i2 >> 4);
      }
    }
    /*
    for (int l1 = -192; l1 <= 192; l1 += 16)
    {
      for (int i2 = -192; i2 <= 192; i2 += 16)
      {
        chunkProviderServerUtility.loadChunk(0 + l1 >> 4, 0 + i2 >> 4);
      }
    }*/


    //chunkProviderServerUtility.chunkGenerator.populate(x, z);
  }

  @Override
  protected void onDisabled() {
    for (int j = 0; j < 90; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          if(utils.worldServer.getBlockState(new BlockPos(MC.player.getPosition().getX() + x * 16 + i, j,  MC.player.getPosition().getZ() + z * 16 + k)).getBlock().getLocalizedName() != MC.world.getBlockState(new BlockPos(MC.player.getPosition().getX() + x * 16 + i, j, MC.player.getPosition().getZ() + z * 16 + k)).getBlock().getLocalizedName()) {
            try {
              writer.write( i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos(MC.player.getPosition().getX() + x * 16 + i, j,  MC.player.getPosition().getZ()+ z * 16 + k)).getBlock().getLocalizedName()
                + " " + MC.world.getBlockState(new BlockPos(MC.player.getPosition().getX() + x * 16 + i, j, MC.player.getPosition().getZ() + z * 16 + k)).getBlock().getLocalizedName()

              + '\n');
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          }
        }
      }
    try {
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    for (int j = 66; j < 66; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
            System.out.println(i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName());
        }
      }
    }
  }
}

