# Wall Socket Plugin for Sweet Home 3D

A [Sweet Home 3D](https://www.sweethome3d.com/) plug-in that generates configurable,
modular wall sockets. Pick a frame, a finish, and the inserts you want, and it drops a
ready-to-use wall socket into your home plan.

## Features

- **Modular frames** — 1 to 5 gangs (55 mm apertures) in a beveled faceplate.
- **Per-gang inserts** — a gang holds one full-width insert or two half-width inserts:
  - Socket (Schuko / UK / Generic)
  - Rocker switch
  - Dimmer (rotary knob)
  - USB charger
  - RJ45 data
  - Blank cover
- **Electrical standards** — Schuko (Type F), UK (Type G), and Generic, each with its
  own socket geometry.
- **Finish palette** — separate colours for the frame and the inserts (white, pure white,
  black, anthracite, aluminium, champagne, cream).
- **Live 3D preview** in the configuration dialog.
- **Undo/redo** support and **German localization**.
- **Config metadata** — each created socket stores its configuration as first-class
  piece properties (see below), ready for downstream export.

## Reading the configuration

Every socket records its setup as properties on the `HomePieceOfFurniture`
(accessible via `getProperty(name)` / `getPropertyNames()`), so it persists in the
`.sh3d` file and can be exported programmatically:

| Property                  | Example value            |
| ------------------------- | ------------------------ |
| `wallsocket.type`         | `wall-socket`            |
| `wallsocket.standard`     | `SCHUKO`                 |
| `wallsocket.frameFinish`  | `ANTHRACITE`             |
| `wallsocket.insertFinish` | `WHITE`                  |
| `wallsocket.gangCount`    | `2`                      |
| `wallsocket.gang.1`       | `SOCKET`                 |
| `wallsocket.gang.2`       | `RJ45_H+ROCKER_H`        |

Per-gang values are a `+`-joined list of insert tokens; `_H` marks a half-width insert.
Enum names are locale-independent.

## How it works

The plug-in generates a single OBJ/MTL model at runtime from parametric primitives
(rounded-rect frames, cylinders, discs). Parts are composed through a
`ModelPart` / `ObjMerger` seam, so Blender-authored OBJ modules can be dropped in later
as plain resources — see `InsertCatalog`.

## Build

Requires [Apache Ant](https://ant.apache.org/) and a JDK (8+).

The build compiles against the Sweet Home 3D jar, which on macOS is at:

```
/Applications/Sweet Home 3D.app/Contents/app/SweetHome3D.jar
```

If your path differs, edit the `sh3d.jar` property in `build.xml`.

```sh
ant package
```

This produces `build/WallSocketPlugin.sh3p`.

## Install

Copy the `.sh3p` into Sweet Home 3D's plug-ins folder and restart the app:

- **macOS:** `~/Library/Application Support/eTeks/Sweet Home 3D/plugins`
- **Windows:** `C:\Users\<user>\AppData\Roaming\eTeks\Sweet Home 3D\plugins`
- **Linux:** `~/.eteks/sweethome3d/plugins`

Then use **Tools → Insert wall socket...**.

## Project layout

```
build.xml                              Ant build
src/.../plugin/wallsocket/             Java sources
resources/.../plugin/wallsocket/       ApplicationPlugin.properties, translations, icon
```

## License

GNU GPL v2 (see `ApplicationPlugin.properties`).
