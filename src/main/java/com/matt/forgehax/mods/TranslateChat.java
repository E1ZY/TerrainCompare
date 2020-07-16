package com.matt.forgehax.mods;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import static com.matt.forgehax.Helper.getMinecraft;

@RegisterMod
public class TranslateChat extends ToggleMod {

  boolean toggled;
  boolean isLegit;
  String s;

  public TranslateChat() {
    super(Category.MISC, "TranslateChat", false, "Translates messages you sent");
    toggled = false;
  }

  @Override
  protected void onEnabled() {
    toggled = true;
    isLegit = true;
  }

  @Override
  protected void onDisabled() {
    toggled = false;

  }

  protected void SendChatMessage(){
    this.disable();
    try {
      URL url = new URL("https://script.google.com/macros/s/AKfycbzvHqXSXY0LgwfFeltbNS_iYCcge8re0s-uY0-lvSJ0uuMDENoS/exec?message="
          + URLEncoder.encode(s, "Windows-1251") + "&langin=en&langout=de");
      URLConnection con = url.openConnection();
      InputStream in = con.getInputStream();
      String encoding = con.getContentEncoding();
      encoding = "UTF-8";
      s = IOUtils.toString(in, encoding);
      getMinecraft().player.sendChatMessage(s);
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.enable();
  }

  @SubscribeEvent
  public void onChatMessage(final PacketEvent event) {
    if (event.getPacket() instanceof CPacketChatMessage) {

      s = ((CPacketChatMessage) event.getPacket()).getMessage();
      SendChatMessage();
      event.setCanceled(true);
    }
  }


}
