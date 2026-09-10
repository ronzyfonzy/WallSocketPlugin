package com.eteks.sweethome3d.plugin.wallsocket;

/**
 * A fragment of OBJ body text ({@code v}/{@code vn}/{@code f}/{@code usemtl})
 * in local coordinates. Parametric parts build this with an {@link ObjWriter};
 * Blender-authored modules supply the same text read from a bundled resource,
 * so both compose identically through {@link ObjMerger}.
 */
public class ModelPart {
  private final String body;

  public ModelPart(String body) {
    this.body = body;
  }

  public String objBody() {
    return body;
  }
}
