package com.matt.forgehax.util.gen;

import com.matt.forgehax.util.gen.WorldServerUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.storage.WorldInfo;

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
      this.worldInfo = Minecraft.getMinecraft().world.getWorldInfo();
      this.worldInfo.populateFromWorldSettings(worldSettings);

      this.worldServer = new WorldServerUtility(
          new AnvilSaveHandler(new File("/TerrainCompare" ), "TerrainCompareUtils", false, Minecraft.getMinecraft().getDataFixer()),
          worldInfo,
          0,
          new Profiler());
    }


}
