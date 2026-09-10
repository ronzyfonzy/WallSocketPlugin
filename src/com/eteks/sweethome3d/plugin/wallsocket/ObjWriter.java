package com.eteks.sweethome3d.plugin.wallsocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Emits OBJ geometry text. Coordinates are authored in the OBJ Y-up convention
 * (X = width/right, Y = up/height, Z = depth with +Z toward the room). The
 * generated text is a body of {@code v}/{@code vn}/{@code f}/{@code usemtl}
 * statements with no header, intended to be merged and wrapped by
 * {@link SocketModel}.
 */
public class ObjWriter {
  private final StringBuilder body = new StringBuilder();
  private int vCount;
  private int nCount;

  /** Selects the material used by subsequent faces. */
  public void useMaterial(String name) {
    body.append("usemtl ").append(name).append('\n');
  }

  public String objBody() {
    return body.toString();
  }

  /** Axis-aligned box centered at (cx, cy, cz). */
  public void box(float cx, float cy, float cz, float w, float h, float d) {
    float x0 = cx - w / 2f, x1 = cx + w / 2f;
    float y0 = cy - h / 2f, y1 = cy + h / 2f;
    float z0 = cz - d / 2f, z1 = cz + d / 2f;
    List<float[]> profile = new ArrayList<float[]>();
    profile.add(new float[]{x0, y0});
    profile.add(new float[]{x1, y0});
    profile.add(new float[]{x1, y1});
    profile.add(new float[]{x0, y1});
    prism(profile, z0, z1);
  }

  /** Cylinder/prism of radius r along Z, centered at (cx, cy, cz). */
  public void cylinderZ(float cx, float cy, float cz, float r, float depth, int segments) {
    prism(circle(cx, cy, r, segments), cz - depth / 2f, cz + depth / 2f);
  }

  /** Hollow cylinder (tube / ring) along Z, centered at (cx, cy, cz). */
  public void tube(float cx, float cy, float cz, float outerR, float innerR,
                   float depth, int segments) {
    float z0 = cz - depth / 2f, z1 = cz + depth / 2f;
    int n = segments;
    float[][] outer = new float[n][2];
    float[][] inner = new float[n][2];
    for (int i = 0; i < n; i++) {
      double a = 2 * Math.PI * i / n;
      float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
      outer[i][0] = cx + outerR * ca;
      outer[i][1] = cy + outerR * sa;
      inner[i][0] = cx + innerR * ca;
      inner[i][1] = cy + innerR * sa;
    }
    // top ring (z1), normal +Z
    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      int b = vCount + 1;
      emitVertex(outer[i][0], outer[i][1], z1);
      emitVertex(outer[j][0], outer[j][1], z1);
      emitVertex(inner[j][0], inner[j][1], z1);
      emitVertex(inner[i][0], inner[i][1], z1);
      emitNormal(0, 0, 1);
      int nIdx = nCount;
      face(b, b + 1, b + 2, nIdx);
      face(b, b + 2, b + 3, nIdx);
    }
    // bottom ring (z0), normal -Z
    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      int b = vCount + 1;
      emitVertex(outer[i][0], outer[i][1], z0);
      emitVertex(inner[i][0], inner[i][1], z0);
      emitVertex(inner[j][0], inner[j][1], z0);
      emitVertex(outer[j][0], outer[j][1], z0);
      emitNormal(0, 0, -1);
      int nIdx = nCount;
      face(b, b + 1, b + 2, nIdx);
      face(b, b + 2, b + 3, nIdx);
    }
    // outer wall (normal outward)
    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      double a = 2 * Math.PI * i / n;
      float nx = (float) Math.cos(a), ny = (float) Math.sin(a);
      int b = vCount + 1;
      emitVertex(outer[i][0], outer[i][1], z0);
      emitVertex(outer[j][0], outer[j][1], z0);
      emitVertex(outer[j][0], outer[j][1], z1);
      emitVertex(outer[i][0], outer[i][1], z1);
      emitNormal(nx, ny, 0);
      int nIdx = nCount;
      face(b, b + 1, b + 2, nIdx);
      face(b, b + 2, b + 3, nIdx);
    }
    // inner wall (normal inward)
    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      double a = 2 * Math.PI * i / n;
      float nx = -(float) Math.cos(a), ny = -(float) Math.sin(a);
      int b = vCount + 1;
      emitVertex(inner[j][0], inner[j][1], z0);
      emitVertex(inner[i][0], inner[i][1], z0);
      emitVertex(inner[i][0], inner[i][1], z1);
      emitVertex(inner[j][0], inner[j][1], z1);
      emitNormal(nx, ny, 0);
      int nIdx = nCount;
      face(b, b + 1, b + 2, nIdx);
      face(b, b + 2, b + 3, nIdx);
    }
  }

  /** Flat filled circle at z = cz facing +Z. */
  public void disc(float cx, float cy, float cz, float r, int segments) {
    List<float[]> profile = circle(cx, cy, r, segments);
    int base = vCount + 1;
    for (float[] p : profile) {
      emitVertex(p[0], p[1], cz);
    }
    emitNormal(0, 0, 1);
    int nIdx = nCount;
    int n = profile.size();
    for (int i = 1; i <= n - 2; i++) {
      face(base, base + i, base + i + 1, nIdx);
    }
  }

  /** Rounded-rectangle plate extruded along Z, centered at (cx, cy, cz). */
  public void roundedRectPrism(float cx, float cy, float cz, float w, float h, float d,
                               float cornerRadius, int cornerSegments) {
    if (cornerRadius <= 0) {
      box(cx, cy, cz, w, h, d);
      return;
    }
    float a = w / 2f, b = h / 2f;
    float rr = Math.min(cornerRadius, Math.min(a, b));
    float z0 = cz - d / 2f, z1 = cz + d / 2f;

    // arc centers
    float trx = a - rr, try_ = b - rr;
    float brx = a - rr, bry = -b + rr;
    float blx = -a + rr, bly = -b + rr;
    float tlx = -a + rr, tly = b - rr;

    List<float[]> pts = new ArrayList<float[]>();
    pts.add(new float[]{blx, -b});                                        // start (bottom-left of bottom edge)
    pts.add(new float[]{brx, -b});                                        // bottom edge
    addArc(pts, brx, bry, rr, -Math.PI / 2, 0, cornerSegments, true);     // bottom-right corner
    pts.add(new float[]{a, try_});                                        // right edge
    addArc(pts, trx, try_, rr, 0, Math.PI / 2, cornerSegments, true);     // top-right corner
    pts.add(new float[]{tlx, b});                                         // top edge
    addArc(pts, tlx, tly, rr, Math.PI / 2, Math.PI, cornerSegments, true);// top-left corner
    pts.add(new float[]{-a, bly});                                        // left edge
    addArc(pts, blx, bly, rr, Math.PI, 3 * Math.PI / 2, cornerSegments, false); // bottom-left corner (closes)
    prism(pts, z0, z1);
  }

  private void addArc(List<float[]> pts, float cx, float cy, float rr,
                      double a0, double a1, int segments, boolean includeEnd) {
    int n = includeEnd ? segments : segments - 1;
    for (int i = 1; i <= n; i++) {
      double t = (double) i / segments;
      double ang = a0 + (a1 - a0) * t;
      pts.add(new float[]{cx + rr * (float) Math.cos(ang), cy + rr * (float) Math.sin(ang)});
    }
  }

  private List<float[]> circle(float cx, float cy, float r, int segments) {
    List<float[]> pts = new ArrayList<float[]>(segments);
    for (int i = 0; i < segments; i++) {
      double a = 2 * Math.PI * i / segments;
      pts.add(new float[]{cx + r * (float) Math.cos(a), cy + r * (float) Math.sin(a)});
    }
    return pts;
  }

  /** Extrudes a CCW (front view) 2D profile between z0 and z1, with flat caps. */
  private void prism(List<float[]> profile, float z0, float z1) {
    int n = profile.size();
    // front cap (+Z)
    int base = vCount + 1;
    for (float[] p : profile) {
      emitVertex(p[0], p[1], z1);
    }
    emitNormal(0, 0, 1);
    int nIdx = nCount;
    for (int i = 1; i <= n - 2; i++) {
      face(base, base + i, base + i + 1, nIdx);
    }
    // back cap (-Z, reversed winding)
    base = vCount + 1;
    for (float[] p : profile) {
      emitVertex(p[0], p[1], z0);
    }
    emitNormal(0, 0, -1);
    nIdx = nCount;
    for (int i = 1; i <= n - 2; i++) {
      face(base, base + i + 1, base + i, nIdx);
    }
    // side walls
    for (int i = 0; i < n; i++) {
      float[] p = profile.get(i);
      float[] q = profile.get((i + 1) % n);
      float ex = q[0] - p[0], ey = q[1] - p[1];
      float len = (float) Math.sqrt(ex * ex + ey * ey);
      float nx = ey / len, ny = -ex / len;
      base = vCount + 1;
      emitVertex(p[0], p[1], z0);
      emitVertex(q[0], q[1], z0);
      emitVertex(q[0], q[1], z1);
      emitVertex(p[0], p[1], z1);
      emitNormal(nx, ny, 0);
      nIdx = nCount;
      face(base, base + 1, base + 2, nIdx);
      face(base, base + 2, base + 3, nIdx);
    }
  }

  private void emitVertex(float x, float y, float z) {
    body.append(String.format(Locale.US, "v %.6f %.6f %.6f%n", x, y, z));
    vCount++;
  }

  private void emitNormal(float x, float y, float z) {
    body.append(String.format(Locale.US, "vn %.6f %.6f %.6f%n", x, y, z));
    nCount++;
  }

  private void face(int a, int b, int c, int n) {
    body.append("f ").append(a).append("//").append(n)
        .append(' ').append(b).append("//").append(n)
        .append(' ').append(c).append("//").append(n).append('\n');
  }
}
