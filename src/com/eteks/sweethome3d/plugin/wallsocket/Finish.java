package com.eteks.sweethome3d.plugin.wallsocket;

/** Fixed finish palette with diffuse RGB colors. */
public enum Finish {
  WHITE(0.94f, 0.94f, 0.92f),
  PURE_WHITE(0.98f, 0.98f, 0.97f),
  BLACK(0.10f, 0.10f, 0.10f),
  ANTHRACITE(0.22f, 0.23f, 0.24f),
  ALUMINIUM(0.82f, 0.83f, 0.85f),
  CHAMPAGNE(0.88f, 0.84f, 0.74f),
  CREAM(0.92f, 0.89f, 0.82f);

  public final float r;
  public final float g;
  public final float b;

  Finish(float r, float g, float b) {
    this.r = r;
    this.g = g;
    this.b = b;
  }
}
