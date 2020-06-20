package com.matt.forgehax.mods;

import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

@RegisterMod
public class TerrainCompare extends ToggleMod {

  public TerrainCompare() {
    super(Category.RENDER, "TerrainCompare", false, "Shows how default terrain was changed");
  }

}
