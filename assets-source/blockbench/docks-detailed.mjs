/** Original Astra geometry, based on the approved shipyard controller reference.
 * Native front is north (-Z); coordinates use the Minecraft 16-unit block.
 * These descriptive parts are input to the native Blockbench modeling session.
 */
const models = {};
let parts = [];
const cube = (name, material, from, to, rotation, origin) => {
  parts.push({ name, material, from, to, ...(rotation ? { rotation, origin } : {}) });
};
const frontBolt = (name, x, y, z = 0.36) => {
  cube(name, 'iron', [x - .24, y - .24, z], [x + .24, y + .24, z + .23]);
};
function steppedDisc(name, material, x, y, r, z0, z1) {
  cube(name + ' middle', material, [x-r,y-r*.64,z0], [x+r,y+r*.64,z1]);
  cube(name + ' crown', material, [x-r*.64,y+r*.64,z0], [x+r*.64,y+r,z1]);
  cube(name + ' heel', material, [x-r*.64,y-r,z0], [x+r*.64,y-r*.64,z1]);
}
function gauge(name, x, y, r, angle) {
  steppedDisc(name+' cast bezel', 'brass', x,y,r,.96,1.63);
  steppedDisc(name+' porcelain dial', 'cream',x,y,r*.75,.79,.98);
  for(const [dx,dy] of [[0,.51],[-.51,0],[.51,0]])
    cube(name+' dial graduation','dark',[x+dx*r-.095,y+dy*r-.14,.70],[x+dx*r+.095,y+dy*r+.14,.80]);
  cube(name+' pointer','dark',[x-.075,y-.10,.60],[x+.075,y+r*.53,.72],[0,0,angle],[x,y,.70]);
  cube(name+' spindle','brass',[x-.16,y-.16,.56],[x+.16,y+.16,.71]);
}
function chassis(tier) {
  cube('Recessed cast foundation','dark',[1.3,.4,1.3],[14.7,2.8,14.7]);
  cube('Foundation front wear strip','iron',[3,1.0,1.0],[13,1.45,1.4]);
  cube('Cabinet structural core','dark',[1.8,2.2,1.8],[14.2,12.8,14.2]);
  cube('Teal upper armor','teal',[2.2,11.5,2.0],[13.8,14.0,13.8]);
  cube('Raised crown panel','teal',[3.1,14.0,4.1],[12.9,14.35,12.5]);
  cube('Crown front beveled shoulder','teal',[2.35,12.8,2.0],[13.65,14.0,3.0],[22.5,0,0],[8,13.4,2.5]);
  cube('Crown rear beveled shoulder','teal',[2.35,12.8,13.0],[13.65,14.0,14.0],[-22.5,0,0],[8,13.4,13.5]);
  // Four independent cast feet, with chunky cap armor and exposed fasteners.
  for(const x of [.5,12.7]) for(const z of [.5,12.7]) {
    const loc=(x<8?'Left':'Right')+' '+(z<8?'front':'rear');
    cube(loc+' iron shoe','dark',[x,0,z],[x+2.8,1.0,z+2.8]);
    cube(loc+' brass foot armor','brass',[x+.1,1,z+.1],[x+2.7,2.9,z+2.7]);
    cube(loc+' anchor head','iron',[x+1,2.9,z+1],[x+1.8,3.15,z+1.8]);
    if(z<8) frontBolt(loc+' foot rivet',x+1.4,1.9,.48);
  }
  // Teal corner posts and broad bolted shoulder brackets break up the cabinet.
  for(const x of [1.0,13.85]) for(const z of [1.0,13.85]) {
    const loc=(x<8?'Left':'Right')+' '+(z<8?'front':'rear');
    cube(loc+' teal corner pillar','teal',[x,2.7,z],[x+1.15,12.85,z+1.15]);
    cube(loc+' brass pillar spine','brass',[x+.38,3.3,z+.35],[x+.77,11.35,z+.79]);
    cube(loc+' shoulder bracket','brass',[x-.5,10.9,z-.5],[x+1.65,12.65,z+1.65]);
    if(z<8) frontBolt(loc+' shoulder pin',x+.57,11.78,.27);
  }
  // Individual horizontal timber boards keep the cabinet visibly built, not solid.
  for(let i=0;i<4;i++) {
    const y=3.1+i*1.85;
    cube('Left oak board '+i,'wood',[1.49,y,3.0],[1.84,y+1.7,12.7]);
    cube('Right oak board '+i,'wood',[14.16,y,3.0],[14.51,y+1.7,12.7]);
    cube('Rear oak board '+i,'wood',[3.0,y,14.16],[13,y+1.7,14.51]);
  }
  cube('Front console surround','teal',[2.2,3.0,1.08],[13.8,11.8,1.84]);
  // The cap rests on the enclosure and slightly overhangs both ends. Avoid
  // overlapping coplanar side faces, which shimmer in the native renderer.
  cube('Instrument brow shadow','dark',[3.0,10.65,1.28],[13.0,14.35,3.1]);
  cube('Instrument brow cap','brass',[2.94,14.35,1.22],[13.06,14.7,3.2]);
  cube('Instrument brow teal backing','teal',[3.25,11.0,1.07],[12.75,14.33,1.34]);
  if(tier<2) {
    cube('Rear horizontal retaining band','dark',[2.2,10.15,14.45],[13.8,10.85,14.8]);
    for(const x of [3.2,12.0]) cube('Rear strap bolt','brass',[x,10.3,14.79],[x+.55,10.8,15.03]);
  }
}
function screen(tier) {
  const x0=tier===0?3.25:2.65, x1=16-x0;
  const y0=tier===3?5.65:4.8, y1=10.9;
  cube('Blueprint display deep shadow','dark',[x0-.25,y0-.2,.72],[x1+.25,y1+.25,1.65]);
  cube('Blueprint glass full painted face','blueprint',[x0+.36,y0+.38,.55],[x1-.36,y1-.37,.76]);
  cube('Display lower brass sill','brass',[x0,y0,.33],[x1,y0+.42,.9]);
  cube('Display upper brass sill','brass',[x0,y1-.36,.33],[x1,y1,.9]);
  cube('Display left brass stile','brass',[x0,y0+.4,.33],[x0+.38,y1-.33,.9]);
  cube('Display right brass stile','brass',[x1-.38,y0+.4,.33],[x1,y1-.33,.9]);
  for(const x of [x0+.2,x1-.2]) for(const y of [y0+.2,y1-.18])
    cube('Display screw','iron',[x-.11,y-.11,.22],[x+.11,y+.11,.36]);
  for(const x of [x0+.45,x1-1.15]) {
    cube('Display lower hinge','dark',[x,y0-.7,.8],[x+.7,y0-.05,1.5]);
    cube('Display hinge spindle','brass',[x-.12,y0-.5,.67],[x+.82,y0-.23,.97]);
  }
}
function statusLamp() {
  cube('Status lamp cast mount','dark',[6.3,2.9,.76],[9.7,4.3,1.3]);
  cube('Status lamp brass lip','brass',[6.6,3.12,.48],[9.4,4.1,.9]);
  cube('Amber ready indicator','amber',[7.0,3.32,.35],[9.0,3.86,.6]);
}
function lever(name,x,angle) {
  cube(name+' pivot socket','dark',[x-.65,11.25,2.2],[x+.65,12.5,4.0]);
  cube(name+' axle cap','brass',[x-.79,11.5,2.35],[x+.79,12.12,3.3]);
  cube(name+' stem','brass',[x-.22,12.0,2.7],[x+.22,15.45,3.22],[0,0,angle],[x,12,3]);
  cube(name+' grip','leather',[x-.4,14.15,2.55],[x+.4,15.43,3.37],[0,0,angle],[x,12,3]);
  cube(name+' grip end cap','edge',[x-.42,15.15,2.52],[x+.42,15.6,3.4],[0,0,angle],[x,12,3]);
}
function grabRail() {
  for(const x of [2.9,12.3]) {
    cube('Overhead grab rail upright','brass',[x,13.45,5.8],[x+.8,15.95,6.6]);
    cube('Grab rail foot collar','dark',[x-.2,13.45,5.6],[x+1,14.05,6.8]);
    cube('Grab rail forward diagonal','brass',[x,12.35,2.1],[x+.8,13.05,6.9],[-22.5,0,0],[x+.4,12.7,4.5]);
  }
  cube('Overhead grab rail crossbar','brass',[2.9,15.15,5.8],[13.1,15.95,6.6]);
  cube('Grab rail dark handgrip','leather',[5.1,15.09,5.74],[10.9,16,6.66]);
}
function sidePipe(name,x,z) {
  cube(name+' vertical pipe','brass',[x,3.6,z],[x+.64,10.65,z+.68]);
  for(const y of [4.0,8.8,10.4]) cube(name+' pipe union','edge',[x-.14,y,z-.14],[x+.78,y+.55,z+.82]);
}
function rearVent() {
  cube('Rear service vent armor','teal',[4.5,4.0,14.4],[11.5,10.1,14.95]);
  cube('Rear vent shadow','dark',[5.15,4.55,14.95],[10.85,9.55,15.15]);
  for(let i=0;i<5;i++) cube('Rear vent louver '+i,'iron',[5.4,4.8+i*.9,15.1],[10.6,5.08+i*.9,15.48]);
}
function sideVent() {
  cube('Right side louver surround','teal',[14.46,4.1,5.0],[14.98,10.2,11.9]);
  cube('Right side louver cavity','dark',[14.98,4.6,5.5],[15.09,9.7,11.4]);
  for(let i=0;i<5;i++) cube('Right side cooling blade '+i,'iron',[15.07,4.83+i*.92,5.75],[15.46,5.12+i*.92,11.15]);
}
function powerVial(name,x) {
  cube(name+' black socket','dark',[x-.68,2.8,.62],[x+.68,5.45,1.4]);
  cube(name+' aether cell','glow',[x-.31,3.16,.40],[x+.31,5.13,.69]);
  cube(name+' glass reflection','cream',[x-.2,3.35,.33],[x-.07,4.99,.42]);
  for(const y of [2.91,5.07]) cube(name+' brass terminal','brass',[x-.55,y,.30],[x+.55,y+.34,.88]);
}
for(const [tier,id] of ['scout','brig','cruiser','dreadnought'].entries()) {
  parts=[];
  chassis(tier); screen(tier);
  if(tier===0) {
    gauge('Dock pressure',5.6,12.62,1.29,-22.5);
    lever('Single mooring control',12.3,-22.5);
    statusLamp();
    cube('Scout right inspection cover','dark',[14.49,5.1,6.2],[14.86,8.85,10.3]);
    cube('Scout inspection plate','brass',[14.86,5.5,6.6],[15.15,8.45,9.9]);
    cube('Scout plate keyhole','dark',[15.14,6.55,7.65],[15.32,7.35,8.25]);
    for(const z of [4,11.2]) cube('Scout crown strap','dark',[2.9,14.35,z],[13.1,14.58,z+.48]);
  } else if(tier===1) {
    gauge('Brig pressure',5.45,12.66,1.27,22.5);
    gauge('Brig mooring load',10.55,12.66,1.27,-45);
    lever('Port mooring control',2.9,0); lever('Starboard mooring control',13.1,0);
    statusLamp();
    sidePipe('Starboard steam',14.62,5.2);
    cube('Starboard lower steam elbow','brass',[14.62,3.6,5.2],[15.26,4.26,9.6]);
    cube('Starboard upper return elbow','brass',[14.62,10.0,5.2],[15.26,10.65,8.2]);
    for(const x of [3.4,11.9]) sidePipe('Rear manifold '+x,x,14.61);
  } else if(tier===2) {
    gauge('Cruiser pressure',5.45,12.62,1.30,-22.5);
    gauge('Cruiser mooring load',10.55,12.62,1.30,45);
    grabRail(); statusLamp(); sideVent(); rearVent();
    for(const x of [2.85,12.45]) sidePipe('Rear pressure riser '+x,x,14.56);
  } else {
    gauge('Fleet pressure',4.65,12.64,1.19,-22.5);
    gauge('Fleet alignment',8,12.64,1.19,22.5);
    gauge('Fleet mooring load',11.35,12.64,1.19,-45);
    grabRail();
    for(const x of [5.25,8,10.75]) powerVial('Fleet core '+x,x);
    sidePipe('Starboard pressure riser',14.65,4.0);
    cube('Starboard core armored mount','dark',[14.5,4.15,7.0],[15.13,10.25,10.5]);
    cube('Starboard core brass frame','brass',[15.12,4.5,7.35],[15.48,9.9,10.15]);
    cube('Starboard core recess','dark',[15.48,4.9,7.72],[15.65,9.5,9.78]);
    cube('Starboard aether core','glow',[15.65,5.3,8.36],[15.83,9.1,9.12]);
    cube('Starboard core glass glint','cream',[15.82,5.5,8.44],[15.92,8.9,8.61]);
    for(const x of [3.7,11.7]) {
      cube('Rear armor vertical retaining strap','dark',[x,2.9,14.5],[x+.6,13.3,14.87]);
      for(const y of [3.2,10.3,12.4]) cube('Rear armor strap rivet','brass',[x+.05,y,14.87],[x+.55,y+.5,15.12]);
    }
  }
  models[id+'_dock_controller']=parts;
}
export default models;
