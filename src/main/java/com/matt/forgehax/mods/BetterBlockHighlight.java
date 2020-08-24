package com.matt.forgehax.mods;

import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.tesselation.GeometryMasks;
import com.matt.forgehax.util.tesselation.GeometryTessellator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

@RegisterMod
public class BetterBlockHighlight extends ToggleMod {

  private final Setting<Integer> alpha =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("alpha")
          .description("alpha")
          .min(0)
          .max(255)
          .defaultTo(255)
          .build();

  private final Setting<Integer> red =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("red")
          .description("red")
          .min(0)
          .max(255)
          .defaultTo(255)
          .build();

  private final Setting<Integer> green =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("green")
          .description("green")
          .min(0)
          .max(255)
          .defaultTo(255)
          .build();

  private final Setting<Integer> blue =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("blue")
          .description("blue")
          .min(0)
          .max(255)
          .defaultTo(255)
          .build();

  private final Setting<Float> width =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("width")
          .description("line width")
          .min(0.f)
          .defaultTo(1.f)
          .build();

  private final Setting<Boolean> allowEdit =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("allow-edit")
          .description("check whether you are allowed to edit and not draw the box then. You can see you dont have perms when this setting is true")
          .defaultTo(true)
          .build();

  public BetterBlockHighlight() {
    super(
        Category.RENDER, "BetterBlockHighlight", true, "Make selected block bounding box more visible");
  }

  private float toFloat(int colorVal) {
    return colorVal / 255.f;
  }

  @SubscribeEvent
  public void onDrawBlockHighlight(final DrawBlockHighlightEvent event) {
    event.setCanceled(true); // we cancel it always
    //checkRenderBoundingBox(event.getPartialTicks());
  }

  @SubscribeEvent
  public void onRender(final RenderEvent event) {
    checkRenderBoundingBox((float) event.getPartialTicks());
  }

  private void checkRenderBoundingBox(final float partialTicks) {
    final EntityPlayer entity = MC.player;
    if (isDrawBlockOutline() && MC.objectMouseOver != null) {// && !entity.isInsideOfMaterial(Material.WATER)) {
      GlStateManager.disableAlpha();

      // here is event check
      drawSelectionBox(entity, MC.objectMouseOver, partialTicks);
      GlStateManager.enableAlpha();
    }
  }


  private boolean isDrawBlockOutline() {
    final Entity entity = MC.getRenderViewEntity();
    boolean flag = //entity instanceof EntityPlayer &&
        !MC.gameSettings.hideGUI;

    if (allowEdit.get() && flag && entity instanceof EntityPlayer && !((EntityPlayer) entity).capabilities.allowEdit) {
      ItemStack itemstack = ((EntityPlayer) entity).getHeldItemMainhand();

      if (MC.objectMouseOver != null && MC.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
        BlockPos blockpos = MC.objectMouseOver.getBlockPos();
        Block block = MC.world.getBlockState(blockpos).getBlock();

        if (MC.playerController.getCurrentGameType() == GameType.SPECTATOR) {
          flag = block.hasTileEntity(MC.world.getBlockState(blockpos)) && MC.world.getTileEntity(blockpos) instanceof IInventory;
        } else {
          flag = !itemstack.isEmpty() && (itemstack.canDestroy(block) || itemstack.canPlaceOn(block));
        }
      }
    }

    return flag;

  }

  /**
   * Draws the selection box for the player.
   */
  public void drawSelectionBox(EntityPlayer player, RayTraceResult movingObjectPositionIn, float partialTicks) {
    if (movingObjectPositionIn.typeOfHit == RayTraceResult.Type.BLOCK) {
      GlStateManager.enableBlend();
      GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
      //GlStateManager.glLineWidth(2.0F);
      GlStateManager.disableTexture2D();
      GlStateManager.depthMask(false);
      final BlockPos blockpos = movingObjectPositionIn.getBlockPos();
      final IBlockState iblockstate = MC.world.getBlockState(blockpos);

      // TODO also disable AIR check?
      if (iblockstate.getMaterial() != Material.AIR && MC.world.getWorldBorder().contains(blockpos)) {
        final double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        final double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        final double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        drawSelectionBoundingBox(iblockstate.getSelectedBoundingBox(MC.world, blockpos).grow(0.0020000000949949026D).offset(-x, -y, -z));
      }

      GlStateManager.depthMask(true);
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
    }
    // TODO do something when its not hitting anything
    // TODO draw frame around entity if hitting entity

    // TODO display dubhit somehow?
    // TODO show hitInfo if exists
    // TODO sidehit (facing) is obvious so we dont care? (sometimes it is also useful to see in vanilla tho)
    // TODO hitVec should also (only necessary if some mod sets it ig)
    // TODO show blockpos or entity hit somehow even if not hit?

    // TODO this should be seperate mod but it could have a key to switch between entity, or block hit, and even going
    //  as far as going to the block behind of blocks (requires changes after getMouseOver tho)

    // TODO a mod that displays how you would place a block somehow (I have no idea how or how to even get the state)
  }

  public void drawSelectionBoundingBox(final AxisAlignedBB box) {
    GlStateManager.disableDepth();
    GlStateManager.glLineWidth(width.get());
    drawBoundingBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    GlStateManager.enableDepth();
  }

  public void drawBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    Tessellator tessellator = Tessellator.getInstance();
    BufferBuilder bufferbuilder = tessellator.getBuffer();
    bufferbuilder.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR); // changed from 3; changed from DefaultVertexFormats.POSITION_COLOR
    GeometryTessellator.drawLines(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, GeometryMasks.Line.ALL, red.get(), green.get(), blue.get(), alpha.get());
    //drawBoundingBox(bufferbuilder, minX, minY, minZ, maxX, maxY, maxZ, toFloat(red.get()), toFloat(green.get()), toFloat(blue.get()), toFloat(alpha.get()));
    tessellator.draw();
  }

  public void drawBoundingBox(BufferBuilder buffer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float red, float green, float blue, float alpha) {
    buffer.pos(minX, minY, minZ).color(red, green, blue, 0.0F).endVertex();
    buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(minX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex(); // I think the bug/problem is that it has a übergang after the event. during the event the color is static for the complete line
    buffer.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex();
    buffer.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, maxY, minZ).color(red, green, blue, 0.0F).endVertex();
    buffer.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
    buffer.pos(maxX, minY, minZ).color(red, green, blue, 0.0F).endVertex();
  }

}
