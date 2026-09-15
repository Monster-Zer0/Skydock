#!/usr/bin/env node
/**
 * Imports Java Block/Item JSON and .bbmodel files exported from the Blockbench
 * MCP session. This tool never creates geometry: exports are required inputs.
 *
 * Usage:
 *   node tools/model-assets/import-mcp-exports.mjs <export-directory>
 *
 * Expected files are <id>.json and <id>.bbmodel. Texture PNGs may be placed in
 * <export-directory>/textures/<palette>/<name>.png. A manifest.json may supply
 * per-model palette values and alternate filenames; see the README beside this
 * script. The generated PNG path is assets/skydock/textures/block/<palette>/<name>.png.
 */
import { cp, mkdir, readFile, readdir, rm, stat, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..', '..');
const runtimeAssets = path.join(root, 'common', 'src', 'main', 'resources', 'assets', 'skydock');
const sourceArchive = path.join(root, 'assets-source', 'blockbench');
const ids = [
  'scout_dock_controller', 'brig_dock_controller', 'cruiser_dock_controller', 'dreadnought_dock_controller',
  'helm', 'seat', 'mooring_clamp', 'ballast',
  'engine', 'engine_reinforced', 'engine_turbine', 'engine_aether',
  'lift_cell', 'brass_lantern', 'canvas_awning', 'timber_railing', 'signal_flag'
];
const horizontalIds = new Set(['helm', 'seat', 'mooring_clamp', 'ballast', 'engine', 'engine_reinforced', 'engine_turbine', 'engine_aether', 'canvas_awning', 'timber_railing', 'signal_flag']);
const railingPartNames = new Set(['post', 'side', 'inventory']);
const liftBitOrder = ['east', 'west', 'up', 'down', 'south', 'north'];
const displayDefaults = {
  thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
  thirdperson_lefthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
  firstperson_righthand: { rotation: [0, 45, 0], scale: [0.4, 0.4, 0.4] },
  firstperson_lefthand: { rotation: [0, 225, 0], scale: [0.4, 0.4, 0.4] },
  gui: { rotation: [30, 225, 0], scale: [0.625, 0.625, 0.625] },
  ground: { translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
  fixed: { rotation: [0, 180, 0], scale: [0.5, 0.5, 0.5] }
};

function fail(message) { throw new Error(message); }
function basename(value) {
  return path.basename(String(value).replace(/\\\\/g, '/')).replace(/\.png$/i, '');
}
function paletteFor(texturePath, entry, manifest) {
  const configured = entry?.palette ?? manifest.palette;
  if (configured) return configured;
  const normalized = String(texturePath).replace(/\\\\/g, '/');
  const parts = normalized.split('/');
  if (parts.length > 1) return parts.at(-2);
  const named = basename(texturePath).match(/^([^_]+)__(.+)$/);
  return named ? named[1] : 'palette';
}
function textureLocation(texturePath, entry, manifest) {
  const palette = paletteFor(texturePath, entry, manifest);
  const name = basename(texturePath).replace(/^[^_]+__(.+)$/, '$1');
  return `skydock:block/${palette}/${name}`;
}
function validateElement(element, modelPath, index) {
  for (const key of ['from', 'to']) {
    if (!Array.isArray(element[key]) || element[key].length !== 3 || element[key].some(value => !Number.isFinite(value) || value < -16 || value > 32)) {
      fail(`${modelPath}: element ${index} has an invalid ${key}; Minecraft 1.21.1 permits coordinates only within -16..32.`);
    }
  }
  if (element.from.some((value, axis) => value >= element.to[axis])) fail(`${modelPath}: element ${index} has an empty or inverted bounds.`);
  if (element.rotation) {
    const rotation = element.rotation;
    if (!['x', 'y', 'z'].includes(rotation.axis) || ![-45, -22.5, 0, 22.5, 45].includes(rotation.angle) ||
        !Array.isArray(rotation.origin) || rotation.origin.length !== 3 || rotation.origin.some(value => !Number.isFinite(value))) {
      fail(`${modelPath}: element ${index} has a rotation unsupported by Minecraft 1.21.1.`);
    }
  }
}
function defaultParticle(id) {
  if (id === 'lift_cell') return 'canvas';
  if (id.startsWith('timber_railing_')) return 'wood';
  if (new Set(['helm', 'seat', 'mooring_clamp', 'canvas_awning', 'timber_railing', 'signal_flag']).has(id)) return 'wood';
  if (new Set(['scout_dock_controller', 'brig_dock_controller', 'cruiser_dock_controller', 'dreadnought_dock_controller']).has(id)) return 'teal';
  return 'brass';
}
function normalizeModel(raw, modelPath, entry, manifest, id) {
  if (!raw || typeof raw !== 'object' || !Array.isArray(raw.elements)) fail(`${modelPath}: expected a Java Block/Item model with an elements array.`);
  raw.elements.forEach((element, index) => validateElement(element, modelPath, index));
  const textures = raw.textures && typeof raw.textures === 'object' ? raw.textures : {};
  for (const [key, value] of Object.entries(textures)) {
    if (typeof value === 'string' && !value.startsWith('#')) textures[key] = textureLocation(value, entry, manifest);
  }
  if (!textures.particle) textures.particle = textureLocation(`${defaultParticle(id)}.png`, entry, manifest);
  raw.parent = 'block/block';
  // Blockbench's current Java exporter emits this metadata, but Minecraft 1.21.1
  // model JSON does not consume it.
  delete raw.format_version;
  raw.gui_light = 'front';
  raw.display = { ...displayDefaults, ...(raw.display ?? {}) };
  return raw;
}
async function json(file) {
  try { return JSON.parse((await readFile(file, 'utf8')).replace(/^\uFEFF/, '')); }
  catch (error) { fail(`${file}: invalid JSON (${error.message}).`); }
}
async function findPngs(directory) {
  const found = [];
  async function visit(current) {
    for (const item of await readdir(current, { withFileTypes: true })) {
      const child = path.join(current, item.name);
      if (item.isDirectory()) await visit(child);
      else if (item.isFile() && item.name.toLowerCase().endsWith('.png')) found.push(child);
    }
  }
  await visit(directory);
  return found;
}
function liftStates() {
  const variants = {};
  for (let mask = 0; mask < 64; mask += 1) {
    const state = liftBitOrder.map((name, bit) => `${name}=${Boolean(mask & (1 << bit))}`).join(',');
    variants[state] = { model: `skydock:block/lift_cell_${mask}` };
  }
  return { variants };
}
function blockState(id) {
  if (id === 'lift_cell') return liftStates();
  if (!horizontalIds.has(id)) return { variants: { '': { model: `skydock:block/${id}` } } };
  return { variants: {
    'facing=north': { model: `skydock:block/${id}` },
    'facing=east': { model: `skydock:block/${id}`, y: 90 },
    'facing=south': { model: `skydock:block/${id}`, y: 180 },
    'facing=west': { model: `skydock:block/${id}`, y: 270 }
  } };
}
async function ensure(file) { await mkdir(path.dirname(file), { recursive: true }); }
async function writeJson(file, value) { await ensure(file); await writeFile(file, `${JSON.stringify(value, null, 2)}\n`); }
function liftFileName(pattern, mask, extension) {
  if (Array.isArray(pattern)) return pattern[mask];
  return String(pattern ?? `lift_cell_{mask}.${extension}`).replace('{mask}', String(mask));
}
async function exists(file) { return stat(file).then(() => true).catch(() => false); }
async function importLiftCell(input, entry, manifest) {
  for (let mask = 0; mask < 64; mask += 1) {
    const rawName = liftFileName(entry.raw, mask, 'json');
    if (!rawName) fail('manifest.json: lift_cell raw model list must contain all 64 variants.');
    const rawPath = path.join(input, rawName);
    const model = normalizeModel(await json(rawPath), rawPath, entry, manifest, 'lift_cell');
    await writeJson(path.join(runtimeAssets, 'models', 'block', `lift_cell_${mask}.json`), model);

    const bbName = liftFileName(entry.bbmodel, mask, 'bbmodel');
    const variantSource = path.join(input, bbName);
    const sharedSource = path.join(input, 'lift_cell.bbmodel');
    const source = await exists(variantSource) ? variantSource : sharedSource;
    if (!(await exists(source))) fail(`Missing Blockbench source for lift_cell_${mask}: expected ${bbName} or lift_cell.bbmodel.`);
    await ensure(path.join(sourceArchive, `lift_cell_${mask}.bbmodel`));
    await cp(source, path.join(sourceArchive, `lift_cell_${mask}.bbmodel`));
  }
  // The original scaffold used a single cube_all lift_cell model. The runtime
  // now resolves only the authored mask models, beginning at lift_cell_0.
  await rm(path.join(runtimeAssets, 'models', 'block', 'lift_cell.json'), { force: true });
  await writeJson(path.join(runtimeAssets, 'models', 'item', 'lift_cell.json'), { parent: 'skydock:block/lift_cell_0' });
  await writeJson(path.join(runtimeAssets, 'blockstates', 'lift_cell.json'), blockState('lift_cell'));
}
async function importModel(input, runtimeId, entry, manifest) {
  const rawName = entry.raw ?? `${runtimeId}.json`;
  const bbName = entry.bbmodel ?? `${runtimeId}.bbmodel`;
  const rawPath = path.join(input, rawName);
  const bbPath = path.join(input, bbName);
  const model = normalizeModel(await json(rawPath), rawPath, entry, manifest, runtimeId);
  await writeJson(path.join(runtimeAssets, 'models', 'block', `${runtimeId}.json`), model);
  await ensure(path.join(sourceArchive, `${runtimeId}.bbmodel`));
  await cp(bbPath, path.join(sourceArchive, `${runtimeId}.bbmodel`));
}
async function importConnectedRailing(input, entry, manifest) {
  if (!entry.parts || typeof entry.parts !== 'object') fail('manifest.json: timber_railing.parts must define post, side, and inventory exports.');
  const names = Object.keys(entry.parts);
  if (names.length !== 3 || names.some(name => !railingPartNames.has(name))) {
    fail('manifest.json: timber_railing.parts accepts exactly post, side, and inventory.');
  }
  for (const name of ['post', 'side', 'inventory']) {
    const part = entry.parts[name];
    if (!part || typeof part !== 'object') fail(`manifest.json: timber_railing.parts.${name} must be an object.`);
    await importModel(input, `timber_railing_${name}`, part, manifest);
  }
  // The connected multipart blockstate is Java/blockstate-owned. Deliberately do
  // not write it here: importing geometry must not replace its connections.
  await writeJson(path.join(runtimeAssets, 'models', 'item', 'timber_railing.json'), { parent: 'skydock:block/timber_railing_inventory' });
}
async function main() {
  const exportDirectory = process.argv[2];
  if (!exportDirectory) fail('Usage: node tools/model-assets/import-mcp-exports.mjs <export-directory>');
  const input = path.resolve(exportDirectory);
  if (!(await stat(input)).isDirectory()) fail(`${input} is not a directory.`);
  const manifestPath = path.join(input, 'manifest.json');
  const manifest = await stat(manifestPath).then(() => json(manifestPath)).catch(() => ({}));
  const configuredIds = manifest.ids ?? ids;
  if (!Array.isArray(configuredIds)) fail(`${manifestPath}: ids must be an array when provided.`);
  const acceptedIds = new Set(ids);
  for (const id of configuredIds) if (!acceptedIds.has(id)) fail(`${manifestPath}: unsupported model id ${id}.`);
  const models = manifest.models ?? {};
  for (const id of configuredIds) {
    const entry = models[id] ?? {};
    if (id === 'lift_cell') {
      await importLiftCell(input, entry, manifest);
      continue;
    }
    if (id === 'timber_railing' && entry.parts) {
      await importConnectedRailing(input, entry, manifest);
      continue;
    }
    await importModel(input, id, entry, manifest);
    await writeJson(path.join(runtimeAssets, 'models', 'item', `${id}.json`), { parent: `skydock:block/${id}` });
    await writeJson(path.join(runtimeAssets, 'blockstates', `${id}.json`), blockState(id));
  }
  // Only declared texture inputs are importable. Renders and other review
  // artifacts may live beside exports without becoming Minecraft textures.
  const textureDirectory = path.join(input, 'textures');
  const importedTextures = await exists(textureDirectory) ? await findPngs(textureDirectory) : [];
  for (const texture of importedTextures) {
    const output = textureLocation(path.relative(input, texture), {}, manifest).replace('skydock:block/', '');
    const target = path.join(runtimeAssets, 'textures', 'block', `${output}.png`);
    await ensure(target);
    await cp(texture, target);
  }
  console.log(`Imported ${configuredIds.length} MCP-authored models from ${input}.`);
}

main().catch(error => { console.error(`Import failed: ${error.message}`); process.exitCode = 1; });
