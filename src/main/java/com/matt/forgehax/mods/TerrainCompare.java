package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.gen.ChunkProviderServerUtility;
import com.matt.forgehax.util.gen.TerrainCompareUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecartChest;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;

import static com.matt.forgehax.Helper.getMinecraft;
import static com.matt.forgehax.Helper.getWorld;
import static net.minecraftforge.event.world.ChunkEvent.*;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  TerrainCompareUtils utils;
  FileWriter writer;
  ChunkProviderServerUtility chunkProviderServerUtility;
  ArrayList<BlockPos> posArray;
  int x;
  int z;
  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
  }

  @Override
  protected void onEnabled() {
    posArray = new ArrayList<BlockPos>();
    x = MC.player.getPosition().getX();
    z = MC.player.getPosition().getZ();
    utils = new TerrainCompareUtils();
    try {
      writer = new FileWriter("D:\\output.txt");
    } catch (IOException e) {
      e.printStackTrace();
    }
    chunkProviderServerUtility = utils.worldServer.getChunkProvider();
    prepareTerrain(x, z);
  }

  public void prepareTerrain(int x, int z){
    for (int l1 = -48; l1 <= 48; l1 += 16)
    {
      for (int i2 = -48; i2 <= 48; i2 += 16)
      {
        chunkProviderServerUtility.provideChunk( x + l1 >> 4, z + i2 >> 4);
      }
    }
  }

  /*@SubscribeEvent
  public void OnChunkLoad(ChunkEvent.Load e){
    for (int l1 = -16; l1 <= 16; l1 += 16)
    {
      for (int i2 = -16; i2 <= 16; i2 += 16)
      {
        chunkProviderServerUtility.provideChunk( e.getChunk().x + l1 >> 4, e.getChunk().z + i2 >> 4);
      }
    }
  }*/

  /*
  @SubscribeEvent
  public void onChunkLoad(ChunkEvent.Load e){
      prepareTerrain(e.getChunk().x * 16, e.getChunk().z * 16);
  }*/

  @SubscribeEvent
  public void OnTick(LocalPlayerUpdateEvent event) {
    x = MC.player.getPosition().getX() >> 4;
    z = MC.player.getPosition().getZ() >> 4;
    for (int j = 0; j < 256; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          BlockPos pos = new BlockPos(x * 16 + i, j,  z * 16 + k);
          int color = -1;
          // System.out.println(DimensionManager.getWorld(0).getBlockState(pos).getBlock().getLocalizedName() + " " + utils.worldServer.getBlockState(pos).getBlock().getLocalizedName());

          if (Minecraft.getMinecraft().world.getBlockState(pos).getBlock().getLocalizedName() != utils.worldServer.getBlockState(pos).getBlock().getLocalizedName()){
            color = Colors.ORANGE.toBuffer();
          }

          if (color != -1) {
            posArray.add(pos);

          }

        }
      }
    }
  }

  @SubscribeEvent
  public void onRender(RenderEvent event) {
    event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

    for (BlockPos pos : posArray) {
      GeometryTessellator.drawCuboid(event.getBuffer(), pos, GeometryMasks.Line.ALL, Colors.ORANGE.toBuffer());
    }
    event.getTessellator().draw();
  }



  @Override
  protected void onDisabled() {
    x = MC.player.getPosition().getX() >> 4;
    z = MC.player.getPosition().getZ() >> 4;
    for (int j = 0; j < 90; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          if(utils.worldServer.getBlockState(new BlockPos(x * 16 + i, j,  z * 16 + k)).getBlock().getLocalizedName() != Minecraft.getMinecraft().world.getBlockState(new BlockPos(x * 16 + i, j, z * 16 + k)).getBlock().getLocalizedName()) {
            try {
              writer.write( i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos( x * 16 + i, j,  z * 16 + k)).getBlock().getLocalizedName()
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

