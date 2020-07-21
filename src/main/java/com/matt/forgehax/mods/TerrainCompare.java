package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.gen.TerrainCompareUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static com.matt.forgehax.Helper.getMinecraft;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  private final TerrainCompareUtils utils;
  private final ArrayList<BlockPos> posArray;
  private double managedPosX;
  private double managedPosZ;
  private int x;
  private int z;

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
    utils = new TerrainCompareUtils();

    posArray = new ArrayList<>();

  }

  @Override
  protected void onEnabled() {
    x = MC.player.getPosition().getX();
    z = MC.player.getPosition().getZ();
    prepareTerrain(x, z);
  }

  public void prepareTerrain(int x, int z){
    for (int l1 = -48; l1 <= 48; l1 += 16)
    {
      for (int i2 = -48; i2 <= 48; i2 += 16)
      {
        try {
          utils.worldServer.getChunkProvider().provideChunk(x + l1 >> 4, z + i2 >> 4);
        } catch (Exception e) {

        }
      }
    }
  }

  @SubscribeEvent
  public void onTick(final LocalPlayerUpdateEvent event) {
    x = MC.player.getPosition().getX() >> 4;
    z = MC.player.getPosition().getZ() >> 4;
    posArray.clear();
    try {
      EntityPlayerSP player = getMinecraft().player;
      double d0 = managedPosX - player.posX;
      double d1 = managedPosZ - player.posZ;
      double d2 = d0 * d0 + d1 * d1;

      if (d2 >= 64.0D) {
        int k = (int) managedPosX >> 4;
        int l = (int) managedPosZ >> 4;
        int i1 = 3;
        int j1 = x - k;
        int k1 = z - l;

        if (j1 != 0 || k1 != 0) {
          for (int l1 = x - i1; l1 <= x + i1; ++l1) {
            for (int i2 = z - i1; i2 <= z + i1; ++i2) {
              utils.worldServer.getChunkProvider().provideChunk(l1 - j1, i2 - k1);
            }
          }
        }
          managedPosX = player.posX;
          managedPosZ = player.posZ;
      }
    } catch (Exception e) {

    }
    for (int j = 0; j < 256; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          final BlockPos pos = new BlockPos(x * 16 + i, j, z * 16 + k);
          if (!Block.isEqualTo(MC.world.getBlockState(pos).getBlock(), utils.worldServer.getBlockState(pos).getBlock())) {
            posArray.add(pos);
          }

        }
      }
    }
  }

  @SubscribeEvent
  public void onRender(final RenderEvent event) {
    event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

    for (final BlockPos pos : posArray) {
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, Colors.ORANGE.toBuffer());
    }
    event.getTessellator().draw();
  }

  // logs the differences in a file for debugging
  @Override
  protected void onDisabled() {
    final FileWriter writer;
    try {
      writer = new FileWriter("D:\\output.txt");
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }


    x = MC.player.getPosition().getX() >> 4;
    z = MC.player.getPosition().getZ() >> 4;
    for (int j = 0; j < 90; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          if (utils.worldServer.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName() != Minecraft.getMinecraft().world.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName()) {
            try {
              writer.write(i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName()
                  + " " + Minecraft.getMinecraft().world.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName()

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

