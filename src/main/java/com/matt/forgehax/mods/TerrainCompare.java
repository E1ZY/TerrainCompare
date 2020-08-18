package com.matt.forgehax.mods;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.gen.TerrainCompareUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import com.mojang.realmsclient.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jline.utils.DiffHelper;
import org.lwjgl.opengl.GL11;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  private final TerrainCompareUtils utils;
  private final ArrayList<ChunkPos> managedChunks; // chunks that are in render distance
  private final ArrayList<Pair<BlockPos, Difference>> difArray;
  private int renderDistance;
  private int playerX;
  private int playerZ;

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
    utils = new TerrainCompareUtils();
    managedChunks = new ArrayList<>();
    difArray = new ArrayList<>();
    renderDistance = 3; // default is 3
  }

  @SubscribeEvent
  public void onTick(final LocalPlayerUpdateEvent event) {
    difArray.clear();
    managedChunks.clear();

    if(MC.player.dimension != 0) {
      return;
    }

    playerX = MC.player.getPosition().getX() >> 4;
    playerZ = MC.player.getPosition().getZ() >> 4;

    for (int i = -renderDistance; i < renderDistance + 1; ++i) {
      for (int j = -renderDistance; j < renderDistance + 1; ++j) {
        managedChunks.add(new ChunkPos(playerX + i, playerZ + j));
      }
    }

    for (final ChunkPos chunkPos : managedChunks) {
      for (int y = 0; y < 256; ++y) {
        for (int x = 0; x < 16; ++x) {
          for (int z = 0; z < 16; ++z) {
            final BlockPos pos = new BlockPos(chunkPos.x * 16 + x, y, chunkPos.z * 16 + z);

            if (!Block.isEqualTo(MC.world.getBlockState(pos).getBlock(), utils.getBlockState(pos).getBlock())) {
              difArray.add(Pair.of(
                  pos, // position
                  Difference.create(MC.world.getBlockState(pos), utils.getBlockState(pos)))); // difference
            }
          }
        }
      }
    }

  }

  @SubscribeEvent
  public void onRender(final RenderEvent event) {
    event.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

    for (final Pair<BlockPos, Difference> pair : difArray) {
      GeometryTessellator.drawCuboid(event.getBuffer(), pair.first(), GeometryMasks.Line.ALL, getColor(pair.second()).toBuffer());
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

    playerX = MC.player.getPosition().getX() >> 4;
    playerZ = MC.player.getPosition().getZ() >> 4;
    for (int j = 0; j < 90; ++j) {
      for (int i = 0; i < 16; ++i) {
        for (int k = 0; k < 16; ++k) {
          if (utils.worldServer.getBlockState(new BlockPos(playerX * 16 + i, j, playerZ * 16 + k)).getBlock().getLocalizedName() != Minecraft.getMinecraft().world.getBlockState(new BlockPos(playerX * 16 + i, j, playerZ * 16 + k)).getBlock().getLocalizedName()) {
            try {
              writer.write(i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos(playerX * 16 + i, j, playerZ * 16 + k)).getBlock().getLocalizedName()
                  + " " + Minecraft.getMinecraft().world.getBlockState(new BlockPos(playerX * 16 + i, j, playerZ * 16 + k)).getBlock().getLocalizedName()

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
          System.out.println(i + " " + j + " " + k + " " + utils.worldServer.getBlockState(new BlockPos(playerX * 16 + i, j, playerZ * 16 + k)).getBlock().getLocalizedName());
        }
      }
    }
  }

  private Color getColor(Difference difference) {
    switch (difference) {
      case BUILT: return Colors.GREEN;
      case DESTROYED: return Colors.RED;
      case CHANGED: return Colors.BLUE;
      case TICKED: return Colors.YELLOW;
      case POPULATED: return Colors.PURPLE;
    }
    return Colors.BLACK;
  }

  private enum Difference {
    NONE, CHANGED, BUILT, DESTROYED, TICKED, POPULATED; // TODO add types for water and for population blocks...

    // TODO save these things in a collection type
    // it could either be a 3D array and the type NONE must also be used
    // or it might be better to use something like a HashMap? -> probably too much?
    // but array also sounds like too much...
    // it would stay in that collection now and changes will only happen when there is a blockupdate or something

    // I think the block has to be gotten somehow does it? so it cant be an ArrayList?

    public static Difference create(final IBlockState actual, final IBlockState supposed) {
      if (supposed == Blocks.GRASS.getDefaultState() && actual == Blocks.DIRT.getDefaultState()
        || supposed == Blocks.DIRT.getDefaultState() && actual == Blocks.GRASS.getDefaultState()
      ) {
        return TICKED;
      }

      if (supposed == Blocks.TALLGRASS.getDefaultState() && actual == Blocks.AIR.getDefaultState()
          || supposed == Blocks.AIR.getDefaultState() && actual == Blocks.TALLGRASS.getDefaultState()) {
        return POPULATED;
      }

      if ((supposed == Blocks.AIR.getDefaultState() || supposed == Blocks.WATER.getDefaultState())
          && (actual != Blocks.AIR.getDefaultState() || actual != Blocks.WATER.getDefaultState())) {
        return BUILT;
      }
      if ((supposed != Blocks.AIR.getDefaultState())
          && (actual == Blocks.AIR.getDefaultState() || actual == Blocks.WATER.getDefaultState())) {
        return DESTROYED;
      }
      if (supposed != Blocks.AIR.getDefaultState()) {
        return CHANGED;
      }

      return NONE;
    }

  }

}

