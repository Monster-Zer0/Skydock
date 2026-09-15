/**
 * Original detailed helm geometry, designed from the machinery reference sheet.
 * North (-Z) is the operator-facing side. Bounds are one block wide/deep and
 * below twenty model units high. Every element uses a legal single-axis Java
 * rotation; slanted console coordinates are baked into each element's center.
 * The wheel is genuinely open: eight continuous mitred bars surround spokes.
 */
const parts = [];
const round = n => Math.round(n * 100000) / 100000;
function box(name, material, from, to, rotation, origin) {
  const part = {name, material, from:from.map(round), to:to.map(round)};
  if (rotation) { part.rotation = rotation; part.origin = origin.map(round); }
  parts.push(part);
}
function centered(name, material, center, size, rotation) {
  box(name,material,center.map((n,i)=>n-size[i]/2),center.map((n,i)=>n+size[i]/2),rotation,center);
}
// Metal shoes and individual timber bearers remain visible below the cabinet.
box('foundation front timber bearer','wood',[1,0.5,1.8],[15,1.65,4.2]);
box('foundation rear timber bearer','wood',[1,0.5,11.6],[15,1.65,14]);
for (const x of [0.55,12.55]) for (const z of [1.3,11.2]) {
  const id=`foot ${x} ${z}`;
  box(`${id} cast shoe`,'dark',[x,0,z],[x+2.9,1.35,z+3.4]);
  box(`${id} upper bevel`,'iron',[x+.22,1.35,z+.22],[x+2.68,1.8,z+3.18]);
  box(`${id} brass toe strap`,'brass',[x+.3,.22,z-.035],[x+2.6,.6,z+.2]);
  for(const dx of [.63,2.23]) box(`${id} anchor ${dx}`,'brass',[x+dx-.16,1.8,z+1.15],[x+dx+.16,2.03,z+1.47]);
}
box('cabinet lower iron sill','dark',[2.9,1.65,6.2],[13.1,2.5,13.6]);
box('cabinet timber carcass','wood',[3.5,2.5,6.75],[12.5,13.3,13]);
// Raised enamel framing leaves recessed timber panels with genuine seams.
for(const x of [3.2,12.15]) for(const z of [6.4,12.65])
  box(`cabinet corner upright ${x} ${z}`,'teal',[x,2.55,z],[x+.65,13.7,z+.6]);
for(const z of [6.32,13.02]) {
  box(`cabinet ${z} lower frame`,'teal',[3.4,3.1,z],[12.6,3.7,z+.3]);
  box(`cabinet ${z} top frame`,'teal',[3.4,12.15,z],[12.6,13,z+.3]);
  for(let i=0;i<4;i++) box(`cabinet ${z} recessed plank ${i}`,'wood',[3.96+i*2.04,3.8,z+.045],[5.94+i*2.04,12.05,z+.245]);
  for(const x of [3.75,12.05]) for(const y of [3.32,12.48])
    box(`cabinet ${z} corner rivet ${x} ${y}`,'brass',[x-.15,y-.15,z-.08],[x+.15,y+.15,z+.36]);
}
for(const x of [3.13,12.72]) {
  box(`side ${x} lower panel rail`,'teal',[x,3.1,6.9],[x+.25,3.7,12.8]);
  box(`side ${x} upper panel rail`,'teal',[x,11.9,6.9],[x+.25,12.6,12.8]);
  for(let i=0;i<3;i++) box(`side ${x} timber inset ${i}`,'wood',[x+.025,3.83,7.04+i*1.86],[x+.225,11.78,8.83+i*1.86]);
  for(const z of [7.3,11.8]) box(`side ${x} panel retaining strap ${z}`,'brass',[x-.04,4.5,z],[x+.3,4.83,z+.5]);
}
box('rear brass identification plate','brass',[6.55,10.15,13.32],[9.45,11.35,13.47]);
box('rear plate dark engraved inset','dark',[6.95,10.58,13.475],[9.05,10.91,13.51]);
for(const y of [5.15,9.25]) {
  box(`left service door hinge ${y}`,'iron',[2.99,y,8.1],[3.35,y+.6,9.4]);
  box(`left service door hinge pin ${y}`,'brass',[2.91,y-.13,8.68],[3.25,y+.73,8.92]);
}
// Two gusseted legs support the spindle independently of the instrument panel.
for(const x of [4.25,10.75]) {
  box(`wheel pedestal ${x}`,'dark',[x,2.1,4.7],[x+1,9.4,6.3]);
  box(`wheel pedestal brass shoe ${x}`,'brass',[x-.18,2.3,4.52],[x+1.18,2.78,6.46]);
  centered(`wheel pedestal diagonal gusset ${x}`,'iron',[x+.5,4.0,5.35],[.65,3.9,.72],[0,0,x<8?-22.5:22.5]);
}
box('spindle bearing bridge','dark',[4.45,8.8,4.9],[11.55,10.3,6.3]);
box('spindle bronze bearing block','brass',[6.95,8.45,4.6],[9.05,10.55,6.4]);
box('spindle turned iron axle','iron',[7.43,8.93,2.68],[8.57,10.07,6.9]);

const wheel={x:8,y:9.5,z:2.95};
// Radial bars use axis-aligned cardinal cuboids and only +/-45-degree diagonals.
function radial(name, material, degree, start, end, width, z, depth) {
  const angle=degree*Math.PI/180, mid=(start+end)/2;
  const center=[wheel.x+Math.cos(angle)*mid,wheel.y+Math.sin(angle)*mid,z];
  const vertical=degree===90||degree===270;
  const diagonal=degree%90!==0;
  centered(name,material,center,vertical?[width,end-start,depth]:[end-start,width,depth],diagonal?[0,0,degree===45||degree===225?45:-45]:undefined);
}
// The side centerline apothem is 4.75. Tangential end overlaps make a continuous
// flat-sided octagon, with eight clearly visible empty sectors around the hub.
for(let i=0;i<8;i++) {
  const degree=i*45, angle=degree*Math.PI/180;
  const c=[8+Math.cos(angle)*4.75,9.5+Math.sin(angle)*4.75,2.95];
  const vertical=i%4===0, diagonal=i%2===1;
  const tangent=degree===45||degree===225?-45:45;
  centered(`wheel rim oak mitre ${i}`,'wood',c,vertical?[.85,4.36,.88]:[4.36,.85,.88],diagonal?[0,0,tangent]:undefined);
  centered(`wheel rim front bronze inlay ${i}`,'brass',[c[0],c[1],2.475],vertical?[.18,4.04,.075]:[4.04,.18,.075],diagonal?[0,0,tangent]:undefined);
  radial(`wheel spoke ${i}`,'wood',degree,.9,4.54,.4,2.96,.5);
  radial(`wheel spoke root ferrule ${i}`,'brass',degree,1.18,1.73,.58,2.92,.6);
  radial(`wheel spoke outer shoe ${i}`,'brass',degree,3.98,4.59,.58,2.92,.64);
  radial(`wheel grip tenon ${i}`,'brass',degree,5.04,5.66,.55,2.94,.64);
  radial(`wheel grip carved oak ${i}`,'wood',degree,5.6,6.56,.64,2.94,.72);
  radial(`wheel grip end flare ${i}`,'wood',degree,6.49,6.92,.81,2.94,.81);
  radial(`wheel grip end cap ${i}`,'brass',degree,6.88,7.02,.7,2.94,.73);
  radial(`wheel rim joint pin ${i}`,'brass',degree,4.61,4.88,.26,2.435,.105);
}
// Overlapping stepped blocks create an octagonal bronze boss, not a solid wheel.
function boss(name,material,radius,z1,z2) {
  const inset=radius*.41421356;
  box(`${name} horizontal` ,material,[8-radius,9.5-inset,z1],[8+radius,9.5+inset,z2]);
  box(`${name} vertical`,material,[8-inset,9.5-radius,z1],[8+inset,9.5+radius,z2]);
  for(const sx of [-1,1]) for(const sy of [-1,1])
    centered(`${name} bevel ${sx} ${sy}`,material,[8+sx*radius*.5,9.5+sy*radius*.5,(z1+z2)/2],[radius*.7072,radius*.7072,z2-z1],[0,0,45]);
}
boss('wheel central bronze boss','brass',1.24,2.05,3.38);
boss('wheel central dark recess','dark',.82,1.98,2.045);
boss('wheel central spindle cap','brass',.55,1.82,2.06);

// Console is pitched back 45 degrees. Every local cuboid shares this plane.
const q=Math.SQRT1_2;
function consoleBox(name,material,u,v,t,w,h,d) {
  centered(name,material,[8+u,14+q*v-q*t,7+q*v+q*t],[w,h,d],[45,0,0]);
}
consoleBox('console cast back shell','dark',0,3.2,.15,10.7,7.1,1.15);
consoleBox('console raised enamel face','teal',0,3.2,-.49,10.08,6.47,.22);
for(const u of [-5.2,5.2]) consoleBox(`console brass side frame ${u}`,'brass',u,3.2,-.65,.22,7.08,.23);
for(const v of [-.26,6.66]) consoleBox(`console brass end frame ${v}`,'brass',0,v,-.65,10.65,.24,.23);
for(const u of [-4.79,4.79]) for(const v of [.17,3.22,6.15])
  consoleBox(`console border screw ${u} ${v}`,'brass',u,v,-.745,.2,.2,.085);
// All three gauges have stepped circular silhouettes, inset cream faces,
// individually cut scale marks, and raised dark needles.
for(let index=0;index<3;index++) {
  const u=(index-1)*3.15,v=4.12;
  for(const [name,mat,r,t,d] of [['bezel','brass',1.24,-.83,.32],['recess','dark',1.065,-1.01,.06],['dial','cream',.945,-1.055,.045]]) {
    consoleBox(`gauge ${index} ${name} main`,mat,u,v,t,r*2,r*1.24,d);
    consoleBox(`gauge ${index} ${name} crown`,mat,u,v,t,r*1.24,r*2,d);
    consoleBox(`gauge ${index} ${name} middle`,mat,u,v,t,r*1.75,r*1.75,d);
  }
  for(const [tick,dx,dy,w,h] of [['north',0,.73,.1,.19],['east',.73,0,.19,.1],['west',-.73,0,.19,.1],['northwest',-.5,.5,.12,.17],['northeast',.5,.5,.12,.17]])
    consoleBox(`gauge ${index} scale ${tick}`,'dark',u+dx,v+dy,-1.085,w,h,.026);
  // Different readings retain clean, legal one-axis geometry on a pitched face.
  const offset=[-.27,0,.27][index];
  consoleBox(`gauge ${index} needle stem`,'dark',u+offset/2,v+.22,-1.105,.11,.57,.035);
  if(offset!==0) consoleBox(`gauge ${index} needle sweep`,'dark',u+offset/2,v+.48,-1.106,Math.abs(offset)+.1,.11,.037);
  consoleBox(`gauge ${index} needle pivot`,'brass',u,v,-1.13,.22,.22,.075);
}
consoleBox('console lower brass legend plate','brass',-1.6,1.25,-.74,3.6,.59,.12);
consoleBox('console legend dark inset','dark',-1.6,1.25,-.82,2.83,.16,.04);
consoleBox('console lower toggle socket','dark',2.12,1.2,-.78,.81,.81,.18);
consoleBox('console toggle stem','iron',2.12,1.29,-1.02,.2,.64,.38);
consoleBox('console toggle amber tip','amber',2.12,1.59,-1.1,.39,.25,.28);

// Starboard throttle has its own hardwood plinth, travel quadrant and handgrip.
box('throttle plinth lower bracket','iron',[13.0,4.15,7.1],[14.7,5.05,10.8]);
box('throttle timber side plinth','wood',[13.1,5.05,7.5],[14.45,12.4,10.4]);
for(const y of [5.35,10.95]) box(`throttle plinth strap ${y}`,'brass',[13.04,y,7.42],[14.56,y+.34,10.5]);
box('throttle cast quadrant housing','dark',[12.94,11.98,7.06],[14.75,13.05,10.7]);
box('throttle quadrant brass front cheek','brass',[14.68,12.4,7.48],[14.96,13.51,10.24]);
box('throttle quadrant upper step','brass',[14.68,13.5,8.04],[14.96,13.86,9.71]);
box('throttle quadrant travel slot','dark',[14.97,13.11,8.13],[15.035,13.4,9.66]);
box('throttle lever pivot pin','iron',[12.8,12.61,8.63],[15.19,13.19,9.21]);
centered('throttle slanted brass lever','brass',[14.2,14.67,8.2],[.38,3.85,.43],[22.5,0,0]);
centered('throttle leather handgrip','leather',[14.2,16.43,7.47],[.79,1.02,.83],[22.5,0,0]);
centered('throttle grip brass upper cap','brass',[14.2,16.91,7.27],[.81,.16,.85],[22.5,0,0]);
centered('throttle grip brass lower ferrule','brass',[14.2,15.93,7.68],[.74,.18,.78],[22.5,0,0]);
for(const z of [7.63,10.07]) box(`throttle brass cheek screw ${z}`,'iron',[14.965,12.61,z],[15.1,12.9,z+.28]);

export const designNotes = {
  forward:'north', materialUvUnits:16, textureResolution:64,
  wheel:'Open continuous eight-sided timber rim, brass inlay, eight spokes and ferruled grips.',
  gauges:'Geometric cream faces, stepped octagonal bezels, five scale ticks and individual needles. No custom texture required.',
  rotation:'Only single-axis rotations of +/-22.5 or +/-45 degrees; no rotated groups required.'
};
export default parts;
