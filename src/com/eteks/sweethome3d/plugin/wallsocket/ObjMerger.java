package com.eteks.sweethome3d.plugin.wallsocket;

import java.util.List;
import java.util.Locale;

/**
 * Merges several {@link ModelPart} fragments into a single OBJ body, applying
 * a translation to each fragment's vertices and renumbering face indices so the
 * fragments concatenate correctly. Faces are normalised to triangles with
 * {@code v//vn} corners (texture coordinates are dropped, n-gons are fanned).
 */
public final class ObjMerger {
  private ObjMerger() {
  }

  public static final class Placement {
    public final ModelPart part;
    public final float dx;
    public final float dy;
    public final float dz;

    public Placement(ModelPart part, float dx, float dy, float dz) {
      this.part = part;
      this.dx = dx;
      this.dy = dy;
      this.dz = dz;
    }
  }

  public static String merge(List<Placement> placements) {
    StringBuilder out = new StringBuilder();
    int vOffset = 0;
    int nOffset = 0;
    for (Placement pl : placements) {
      int partV = 0;
      int partN = 0;
      for (String raw : pl.part.objBody().split("\n", -1)) {
        String line = raw.trim();
        if (line.isEmpty()) {
          continue;
        }
        char c = line.charAt(0);
        if (c == 'v') {
          if (line.startsWith("vn ")) {
            out.append(line).append('\n');
            partN++;
          } else if (line.startsWith("v ")) {
            String[] t = line.split("\\s+");
            float x = Float.parseFloat(t[1]) + pl.dx;
            float y = Float.parseFloat(t[2]) + pl.dy;
            float z = Float.parseFloat(t[3]) + pl.dz;
            out.append(String.format(Locale.US, "v %.6f %.6f %.6f%n", x, y, z));
            partV++;
          }
        } else if (c == 'f') {
          appendFace(out, line, vOffset, nOffset);
        } else if (c == 'u' || c == '#') {
          out.append(line).append('\n');
        }
      }
      vOffset += partV;
      nOffset += partN;
    }
    return out.toString();
  }

  private static void appendFace(StringBuilder out, String line, int vOff, int nOff) {
    String[] toks = line.substring(1).trim().split("\\s+");
    int k = toks.length;
    int[] vs = new int[k];
    int[] ns = new int[k];
    for (int i = 0; i < k; i++) {
      String[] sub = toks[i].split("/", -1);
      vs[i] = Integer.parseInt(sub[0]) + vOff;
      ns[i] = (sub.length == 3 && !sub[2].isEmpty())
          ? Integer.parseInt(sub[2]) + nOff
          : -1;
    }
    for (int i = 1; i < k - 1; i++) {
      out.append("f ")
          .append(corner(vs[0], ns[0]))
          .append(' ').append(corner(vs[i], ns[i]))
          .append(' ').append(corner(vs[i + 1], ns[i + 1]))
          .append('\n');
    }
  }

  private static String corner(int v, int n) {
    return n >= 0 ? v + "//" + n : String.valueOf(v);
  }
}
