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
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  private final TerrainCompareUtils utils;
  private final ArrayList<BlockPos> posArray;
  private int x;
  private int z;

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
    utils = new TerrainCompareUtils();

    posArray = new ArrayList<>();

  }

  @SubscribeEvent
  public void onTick(final LocalPlayerUpdateEvent event) {
    x = MC.player.getPosition().getX() >> 4;
    z = MC.player.getPosition().getZ() >> 4;
    posArray.clear();
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

