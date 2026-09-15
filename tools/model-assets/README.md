# MCP model export importer

`import-mcp-exports.mjs` imports Java Block/Item JSON exported by the Blockbench MCP session. It does not generate or modify any element geometry.

Place each exported `<id>.json` and `<id>.bbmodel` in one scratch export directory. Place texture files below `textures/<palette>/<name>.png`; they become `assets/skydock/textures/block/<palette>/<name>.png`. The importer converts every exported texture reference to `skydock:block/<palette>/<name>`. Only PNGs below `textures/` are imported, so rendered previews and other review artifacts may safely live elsewhere in the export directory.

Run it only after the complete approved export set exists:

```text
node tools/model-assets/import-mcp-exports.mjs C:\\Users\\messe\\AppData\\Local\\Temp\\skydock-20260909\\blockbench\\exports
```

An optional `manifest.json` can set a global `palette`, limit `ids`, or override a model's raw JSON, `.bbmodel`, and palette paths:

```json
{
  "palette": "brass",
  "models": {
    "engine": { "raw": "engine.json", "bbmodel": "engine.bbmodel", "palette": "brass" }
  }
}
```

Lift-cell exports must be named `lift_cell_0.json` through `lift_cell_63.json`. The importer also archives either the matching `lift_cell_<mask>.bbmodel` source or a shared `lift_cell.bbmodel` source. The runtime blockstate has 64 variants with bit order east, west, up, down, south, north; `lift_cell_0` is the inventory model. A manifest can use `lift_cell_{mask}.json` and `lift_cell_{mask}.bbmodel` patterns explicitly, but these are the defaults.

## Connected timber railing

The connected railing uses three explicitly authored exports, each with a matching Blockbench source: `timber_railing_post`, `timber_railing_side`, and `timber_railing_inventory`. Configure them under the registered `timber_railing` id:

```json
{
  "ids": ["timber_railing"],
  "models": {
    "timber_railing": {
      "parts": {
        "post": { "raw": "timber_railing_post.json", "bbmodel": "timber_railing_post.bbmodel" },
        "side": { "raw": "timber_railing_side.json", "bbmodel": "timber_railing_side.bbmodel" },
        "inventory": { "raw": "timber_railing_inventory.json", "bbmodel": "timber_railing_inventory.bbmodel" }
      }
    }
  }
}
```

`side` is authored facing north. The railing blockstate owns its multipart connections: an unconditional post plus `north`, `east`, `south`, and `west` branches using the side model at y rotations `0`, `90`, `180`, and `270`. The importer intentionally leaves `blockstates/timber_railing.json` unchanged for this form, and writes the item model with parent `skydock:block/timber_railing_inventory`.

The only accepted part names are `post`, `side`, and `inventory`; all three are required. This does not add a block registry id, and ordinary manifests may continue to use a single legacy `timber_railing.json`/`.bbmodel` pair. Put any new shared material textures below `textures/palette/`, for example `textures/palette/amber.png` or `textures/palette/edge.png`; they import as `skydock:block/palette/amber` and `skydock:block/palette/edge`.

Do not run an import while another change owns the railing blockstate. The import overwrites models, item models, source archives, and imported palette textures; connected-railing imports protect only the multipart blockstate itself.
