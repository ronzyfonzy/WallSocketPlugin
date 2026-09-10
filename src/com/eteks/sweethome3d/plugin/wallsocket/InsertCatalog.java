package com.eteks.sweethome3d.plugin.wallsocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Resolves a (type, half, standard) insert to a Blender-authored OBJ bundled as a
 * JAR resource, or returns {@code null} so the caller falls back to parametric
 * geometry. The OBJ bodies here follow the same convention as {@link ObjWriter}:
 * local origin at the module centre, back at z=0, front toward +Z, centimetres.
 */
public final class InsertCatalog {
  private final ClassLoader loader;

  public InsertCatalog(ClassLoader loader) {
    this.loader = loader;
  }

  public ModelPart resolve(InsertType type, boolean half, Standard standard) {
    String path = resourcePath(type, half, standard);
    if (path == null) {
      return null;
    }
    InputStream in = loader.getResourceAsStream(path);
    if (in == null) {
      return null;
    }
    try {
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      byte[] bytes = new byte[8192];
      int read;
      while ((read = in.read(bytes)) != -1) {
        buf.write(bytes, 0, read);
      }
      return new ModelPart(buf.toString("UTF-8"));
    } catch (IOException ex) {
      return null;
    } finally {
      try {
        in.close();
      } catch (IOException ignored) {
        // ignore
      }
    }
  }

  private static String resourcePath(InsertType type, boolean half, Standard standard) {
    // Blender-authored modules are disabled for now (parametric geometry is used).
    // To re-enable, return the resource path for a specific (type, half, standard) key,
    // e.g. "com/eteks/sweethome3d/plugin/wallsocket/inserts/socket_schuko_full.obj".
    return null;
  }
}
