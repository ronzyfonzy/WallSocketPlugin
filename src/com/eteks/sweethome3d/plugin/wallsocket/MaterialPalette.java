package com.eteks.sweethome3d.plugin.wallsocket;

import java.util.Locale;

/**
 * Named materials mapped to fixed palette colors. Parts reference materials by
 * name via {@code usemtl}; this class emits the matching {@code newmtl} / Kd
 * definitions once.
 */
public class MaterialPalette {
  private final StringBuilder mtl = new StringBuilder();

  public void add(String name, float r, float g, float b) {
    mtl.append("newmtl ").append(name).append('\n');
    mtl.append(String.format(Locale.US, "Kd %.4f %.4f %.4f%n", r, g, b));
    mtl.append("Ka 1 1 1\n");
    mtl.append("Ks 0.25 0.25 0.25\n");
    mtl.append("Ns 30\n");
    mtl.append("illum 2\n\n");
  }

  public String toMtl() {
    return mtl.toString();
  }
}
