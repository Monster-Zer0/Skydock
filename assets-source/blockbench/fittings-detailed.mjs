/** Astra geometry study from the flight-fittings/deck-details reference sheets.
 * Inputs for native Blockbench cube creation; these are not exported game models.
 * North (-Z) is the presentation front. All dimensions use 16 units per block.
 */
const models = {};
let parts;
const box = (name, material, from, to, rotation, origin) => parts.push({name, material, from, to, ...(rotation ? {rotation, origin} : {})});
const model = (id, build) => { parts = []; build(); models[id] = parts; };
// Five contiguous strips make a stepped octagonal silhouette without overlapping faces.
const cuts = [[-1,-.75,.6],[-.75,-.45,.85],[-.45,.45,1],[.45,.75,.85],[.75,1,.6]];
const octXZ = (name, material, x, z, rx, rz, y0, y1) => cuts.forEach(([a,b,w],i) => box(`${name} ${i+1}`,material,[x-rx*w,y0,z+rz*a],[x+rx*w,y1,z+rz*b]));
const octXY = (name, material, x, y, r, z0, z1) => cuts.forEach(([a,b,w],i) => box(`${name} ${i+1}`,material,[x-r*w,y+r*a,z0],[x+r*w,y+r*b,z1]));
const boltN = (name,x,y,z,size=.6) => box(name,'edge',[x-size/2,y-size/2,z],[x+size/2,y+size/2,z+.25]);
const boltTop = (name,x,y,z,size=.6) => box(name,'edge',[x-size/2,y,z-size/2],[x+size/2,y+.25,z+size/2]);

model('ballast', () => {
  octXZ('Cast tank foundation','dark',8,8,7.25,7.25,.45,1.65);
  octXZ('Foundation brass bead','brass',8,8,7.15,7.15,1.65,2.05);
  octXZ('Pressure vessel chamfered jacket','teal',8,8,6.4,6.4,2.05,13.6);
  for (const x of [1,12.5]) for (const z of [1,12.5]) {
    box('Raised mounting foot','dark',[x,0,z],[x+2.5,.65,z+2.5]);
    box('Corner upright brass strap','brass',[x+.55,2,z+.55],[x+1.55,14.2,z+1.55]);
    for (const y of [2,12.15]) {
      box('Corner reinforced saddle','brass',[x-.05,y,z-.05],[x+2.35,y+1.7,z+2.35]);
      boltN('Saddle face rivet',x+1.15,y+.85,z-.3,.62);
      box('Saddle side rivet','edge',[x+2.35,y+.55,z+.75],[x+2.6,y+1.15,z+1.35]);
    }
  }
  for (const x of [4.25,6.2,9.2,11.15]) {
    box('Front pressed tank rib','dark',[x,3.75,1.42],[x+.45,12.1,1.7]);
    box('Rear pressed tank rib','dark',[x,3.75,14.3],[x+.45,12.1,14.58]);
  }
  for (const z of [4.25,6.45,9.15,11.35]) {
    box('Left pressed tank rib','dark',[1.42,3.75,z],[1.7,12.1,z+.45]);
    box('Right pressed tank rib','dark',[14.3,3.75,z],[14.58,12.1,z+.45]);
  }
  octXZ('Shoulder gasket','dark',8,8,6.6,6.6,13.6,14.15);
  octXZ('Bolted tank lid','brass',8,8,6.15,6.15,14.15,14.65);
  octXZ('Recessed teal lid face','teal',8,8,5.7,5.7,14.65,14.95);
  octXZ('Filler neck collar','dark',8,8,2.2,2.2,14.95,15.45);
  box('Filler plug','iron',[6.5,15.45,6.5],[9.5,15.85,9.5]);
  for (const x of [4.4,11.6]) boltTop('Lid clamp screw',x,14.95,8,.7);
  box('Gauge mounting boss','dark',[5.9,8.25,1.05],[10.1,12.45,2.1]);
  octXY('Pressure gauge stepped bezel','brass',8,10.35,2.05,.68,1.2);
  octXY('Pressure gauge ivory face','cream',8,10.35,1.63,.48,.69);
  box('Pressure needle','dark',[7.86,10.1,.34],[8.14,11.57,.47],[0,0,-45],[8,10.35,.4]);
  boltN('Gauge needle pivot',8,10.35,.25,.42);
  for (const x of [6.9,9.1]) box('Gauge scale end','dark',[x-.12,10.85,.32],[x+.12,11.15,.46]);
  box('Gauge scale crown','dark',[7.9,11.52,.32],[8.1,11.82,.46]);
  box('Drain neck','dark',[7,3.15,.9],[9,5.35,2]);
  box('Drain valve body','brass',[6.65,3.1,.45],[9.35,5.4,1.3]);
  box('Drain outlet','dark',[7.35,1.3,.7],[8.65,3.25,1.9]);
  box('Drain outlet lip','brass',[7.05,1.45,.4],[8.95,2.15,1.9]);
  box('Valve stem','iron',[7.68,4,.05],[8.32,4.65,.65]);
  box('Drain cock cross handle','brass',[5.65,4.12,.1],[10.35,4.57,.5]);
  for (const x of [5.6,9.75]) box('Drain handle squared end','edge',[x,3.87,.05],[x+.65,4.82,.55]);
});

model('mooring_clamp', () => {
  for (let i=0;i<4;i++) box('Oak deck mounting plank','wood',[1,.3,2+i*3],[15,1.65,4.9+i*3]);
  box('Lower iron foundation','dark',[1.4,0,2.4],[14.6,.45,13.6]);
  for (const x of [1,12.5]) for (const z of [2,11.5]) {
    box('Foundation corner shoe','iron',[x-.06,0,z-.06],[x+2.56,1.9,z+2.56]);
    box('Corner brass retaining band','brass',[x-.12,1.7,z-.12],[x+2.62,2.1,z+2.62]);
    boltTop('Deck anchor bolt',x+1.25,2.1,z+1.25,.75);
  }
  box('Winch mounting slab','dark',[3,1.65,4],[13,2.7,12]);
  box('Slab front brass edge','brass',[3.08,2.2,3.8],[12.92,2.6,4.1]);
  octXZ('Bollard lower flare','dark',8,8,3.6,3.6,2.7,4.1);
  octXZ('Bollard body','teal',8,8,2.8,2.8,4.1,8.9);
  octXZ('Bollard shoulder','dark',8,8,2.5,2.5,8.9,10);
  octXZ('Bollard crown','dark',8,8,1.95,1.95,10,10.75);
  box('Bollard top wear plate','iron',[6.6,10.75,6.6],[9.4,11.15,9.4]);
  // A closed coil, with individually modeled strand ridges around each straight run.
  for (const z of [4.45,10.75]) {
    box('Mooring rope horizontal core','canvas',[5.6,7.65,z],[10.4,8.45,z+.8]);
    for (let x=5.65;x<10.4;x+=.73) box('Rope strand winding','cream',[x,7.55,z-.08],[x+.38,8.58,z+.88]);
  }
  for (const x of [4.45,10.75]) {
    box('Mooring rope side core','canvas',[x,7.65,5.6],[x+.8,8.45,10.4]);
    for (let z=5.65;z<10.4;z+=.73) box('Rope side strand winding','cream',[x-.08,7.55,z],[x+.88,8.58,z+.38]);
  }
  for (const x of [5.05,10.95]) for (const z of [5.05,10.95]) {
    box('Rope angled corner','canvas',[x-.9,7.65,z-.43],[x+.9,8.45,z+.43],[0,x===z?-45:45,0],[x,8.05,z]);
    box('Rope corner raised strand','cream',[x-.21,7.54,z-.64],[x+.21,8.56,z+.64],[0,x===z?-45:45,0],[x,8.05,z]);
  }
  for (const side of [-1,1]) {
    const left=side<0, outer=left?.4:13.7, inner=left?2.45:11.5;
    box('Clamp foot hinge','dark',[left?2.1:10.9,2.7,6],[left?5.1:13.9,4.1,10]);
    box('Opposing lower jaw','brass',[left?.7:9.75,4.1,6],[left?6.25:15.3,5.65,10]);
    box('Hooked outer jaw upright','brass',[outer,5.25,6],[outer+1.9,9.6,10]);
    box('Upper inward jaw hook','brass',[left?.8:11.35,9.25,6],[left?4.65:15.2,10.6,10]);
    box('Hook bright top lip','edge',[left?1:11.55,10.6,6.2],[left?4.45:15,10.87,9.8]);
    box('Hook inner wear pad','iron',[inner,8.15,6.3],[inner+2.05,9.32,9.7]);
    box('Jaw outside reinforcing band','edge',[outer+.3,5.7,5.75],[outer+1.55,9.2,6.05]);
    boltN('Jaw hinge axle',left?3.7:12.3,4.35,5.68,.95);
  }
  box('Rear worm drive casing','teal',[5.8,3,10.85],[10.2,6.2,13.35]);
  box('Rear drive bearing plate','dark',[6.6,3.4,13.35],[9.4,6.5,13.9]);
  octXY('Rear drive wheel','brass',8,5,1.35,13.9,14.6);
  box('Rear drive axle','iron',[7.6,4.6,14.6],[8.4,5.4,15.05]);
  for (const x of [4.2,11.8]) boltTop('Bollard foot bolt',x,2.7,4.9,.65);
  box('Front rope keeper','iron',[6.1,2.7,3.65],[9.9,3.4,4.8]);
});

model('seat', () => {
  for (const x of [2,11.6]) for (const z of [2.8,11.8]) {
    box('Chair cast iron leg','dark',[x,0,z],[x+2.2,6.45,z+2.2]);
    box('Chair flared foot','iron',[x-.25,0,z-.2],[x+2.45,.85,z+2.4]);
    box('Foot brass shoe','brass',[x-.08,.65,z-.08],[x+2.28,1.2,z+2.28]);
    boltN('Leg foot rivet',x+1.1,1,z-.25,.55);
  }
  for (const z of [3.3,12.3]) box('Under seat cross stretcher','dark',[3.9,2.15,z],[12.1,3,z+.9]);
  for (const x of [2.8,12.3]) box('Under seat side stretcher','dark',[x,2.15,4.5],[x+.9,3,12.2]);
  box('Oak cushion support','wood',[1.8,5.25,2.1],[14.2,6.15,14.4]);
  box('Iron front apron','dark',[1.65,4.85,1.95],[14.35,5.55,2.65]);
  box('Apron fine brass edge','brass',[2.6,5.55,1.9],[13.4,5.85,2.3]);
  octXZ('Cushion lower welt','brass',8,7.75,5.65,5.25,6.15,6.43);
  octXZ('Leather cushion side boxing','leather',8,7.75,5.8,5.4,6.43,7.58);
  octXZ('Leather cushion rounded top','leather',8,7.75,5.55,5.15,7.58,7.96);
  for (const x of [4.9,6.9,8.9,10.9]) {
    box('Cushion sewn panel vertical stitch','canvas',[x,7.14,2.27],[x+.16,7.42,2.4]);
    box('Cushion sewn panel top stitch','canvas',[x,7.94,2.85],[x+.16,8,3.13]);
    boltN('Cushion front upholstery tack',x+.08,6.8,2.17,.27);
  }
  for (const x of [2.12,13.76]) for (const z of [5.7,8,9.9]) {
    box('Cushion side stitch','canvas',[x,7.14,z],[x+.16,7.42,z+.2]);
  }
  for (const x of [2.1,12.1]) {
    box('Backrest iron upright','dark',[x,5.7,13],[x+1.8,15.85,14.8]);
    box('Backrest brass front inlay','brass',[x+.55,8.25,12.75],[x+1.2,15.2,13.1]);
    for (const y of [8.4,14.45]) {
      box('Backrest corner bracket','iron',[x-.3,y,12.4],[x+2.1,y+1.5,14.9]);
      boltN('Backrest bracket rivet',x+.9,y+.75,12.15,.6);
    }
  }
  box('Backrest timber panel','wood',[3.75,7.9,13.25],[12.25,15.5,14.45]);
  for (let x=4.05;x<12;x+=1.6) box('Teal backrest vertical panel','teal',[x,8.35,12.7],[x+1.42,15.2,13.3]);
  box('Backrest crown brass trim','brass',[3.65,15.35,12.7],[12.35,16,14.6]);
  box('Rear lower reinforcement rail','brass',[3.7,8.45,14.45],[12.3,9.05,14.9]);
  box('Rear seat support rail','wood',[3.7,5.9,14.45],[12.3,6.7,14.9]);
  for (const x of [3.6,12.4]) {
    boltN('Front apron corner bolt',x,5.35,1.7,.75);
    box('Rear rail fastening','edge',[x-.3,8.45,14.9],[x+.3,9.05,15.15]);
  }
  for (const x of [1.45,12.95]) {
    box('Armrest front iron post','dark',[x+.35,5.8,4.8],[x+1.05,10.2,5.6]);
    box('Armrest brass support sleeve','brass',[x+.15,8.8,4.55],[x+1.25,10.05,5.85]);
    box('Oak armrest rail','wood',[x,10,3.65],[x+1.6,10.85,13.6]);
    box('Armrest leather top','leather',[x+.15,10.85,4.6],[x+1.45,11.15,12.7]);
    for (const z of [3.65,12.55]) box('Armrest brass end cap','brass',[x-.12,9.9,z-.08],[x+1.72,10.98,z+1.13]);
    boltN('Armrest front end screw',x+.8,10.4,3.37,.55);
  }
});

model('brass_lantern', () => {
  octXZ('Lantern iron foot','dark',8,8,3.25,3.25,0,.9);
  octXZ('Lantern foot bevel','iron',8,8,3.55,3.55,.9,1.4);
  octXZ('Lantern lower brass sill','brass',8,8,3.8,3.8,1.4,2.05);
  octXZ('Lantern glass seat','dark',8,8,3.15,3.15,2.05,2.4);
  octXZ('Amber luminous glass','amber',8,8,2.5,2.5,2.4,10.5);
  box('Flame golden heart','edge',[6.65,3.4,5.38],[9.35,9.55,5.55]);
  box('Flame luminous heart','cream',[7.3,4.05,5.28],[8.7,8.85,5.41]);
  box('Left glass golden heart','edge',[5.38,3.4,6.65],[5.55,9.55,9.35]);
  box('Left glass luminous heart','cream',[5.28,4.05,7.3],[5.41,8.85,8.7]);
  for (const x of [4.85,10.5]) for (const z of [4.85,10.5]) {
    box('Lantern corner brass bar','brass',[x,2.05,z],[x+.65,10.95,z+.65]);
    for (const y of [2.05,9.85]) box('Corner bar cast collar','edge',[x-.2,y,z-.2],[x+.85,y+.75,z+.85]);
  }
  box('Lantern front center mullion','dark',[7.8,2.35,5.05],[8.2,10.5,5.25]);
  box('Lantern rear door stile','dark',[7.75,2.35,10.55],[8.25,10.5,10.8]);
  box('Rear door latch plate','brass',[7.2,5.55,10.7],[8.8,6.85,11]);
  box('Rear latch pivot','iron',[7.65,5.95,11],[8.35,6.55,11.25]);
  for (const y of [3.2,8.65]) box('Rear door hinge','dark',[10.35,y,10.42],[11.25,y+.85,11.23]);
  octXZ('Lantern upper brass sill','brass',8,8,3.8,3.8,10.5,11.15);
  octXZ('Eave charcoal shadow','dark',8,8,4,4,11.15,11.65);
  octXZ('Roof stepped lower bevel','iron',8,8,3.4,3.4,11.65,12.2);
  octXZ('Roof upper step','dark',8,8,2.7,2.7,12.2,12.85);
  octXZ('Roof brass crown','brass',8,8,1.85,1.85,12.85,13.35);
  box('Vent chimney','dark',[7,13.35,7],[9,13.95,9]);
  box('Ring mounting tang','brass',[7.65,13.95,7.7],[8.35,14.35,8.3]);
  // Small open octagonal suspension ring, seen clearly from the front.
  box('Hanging ring top','brass',[7.45,15.55,7.65],[8.55,16,8.35]);
  box('Hanging ring bottom','brass',[7.45,14.05,7.65],[8.55,14.5,8.35]);
  for (const x of [6.9,8.65]) box('Hanging ring side','brass',[x,14.65,7.65],[x+.45,15.4,8.35]);
  for (const x of [7.35,8.65]) for (const y of [14.5,15.55]) box('Hanging ring chamfer','brass',[x-.4,y-.2,7.65],[x+.4,y+.2,8.35],[0,0,(x<8)===(y<15)?-45:45],[x,y,8]);
});

model('canvas_awning', () => {
  for (const x of [1,13]) {
    box('Rear oak support post','wood',[x,0,13],[x+2,15.65,15]);
    box('Post iron foot','dark',[x-.3,0,12.7],[x+2.3,.95,15.3]);
    box('Post brass shoe','brass',[x-.2,.95,12.8],[x+2.2,1.7,15.2]);
    boltN('Post shoe anchor',x+1,1.3,12.55,.55);
    box('Rear post top cap','brass',[x-.15,15.35,12.85],[x+2.15,15.95,15.15]);
    box('Rear post visible timber end','wood',[x+.2,15.95,13.2],[x+1.8,16,14.8]);
    box('Diagonal canopy knee brace','wood',[x+.38,6.9,9.75],[x+1.62,14.1,11.05],[-45,0,0],[x+1,10.5,10.4]);
    box('Brace lower iron saddle','iron',[x+.13,7.15,12.65],[x+1.87,8.6,13.4]);
    boltN('Brace lower saddle pin',x+1,7.9,12.38,.6);
    box('Brace upper iron saddle','iron',[x+.13,12.45,6.65],[x+1.87,13.45,8.3]);
    for (const z of [.15,13.15]) {
      const y=z<1?11.95:13.9;
      box('Roof corner brass buckle','brass',[x-.5,y,z],[x+2.5,y+1.5,z+1.3]);
      box('Roof buckle raised pin','edge',[x+.675,y+.425,z-.10],[x+1.325,y+1.075,z+.03]);
      box('Buckle dark recessed face','dark',[x+.2,y+.35,z-.05],[x+1.8,y+1.15,z+.1]);
      box('Buckle central rivet','edge',[x+.76,y+.51,z-.15],[x+1.24,y+.99,z-.03]);
    }
  }
  box('Rear transverse beam','wood',[.4,13.65,13.3],[15.6,14.6,14.7]);
  box('Front transverse beam','wood',[.4,11.95,.25],[15.6,12.8,1.5]);
  // Four gently rising roof sections reproduce the reference's forward fall.
  for (let row=0;row<4;row++) {
    const z=row*3.75, front=row===0?.55:z, y=12.85+row*.55;
    for (const x of [1.15,13.15]) box('Stepped side timber rafter','wood',[x,y-.9,front],[x+1.7,y,z+3.75]);
    box('Left teal canopy band','teal',[0,y,front],[5.3,y+.65,z+3.75]);
    box('Ivory center canopy panel','canvas',[5.3,y,front],[10.7,y+.65,z+3.75]);
    box('Right teal canopy band','teal',[10.7,y,front],[16,y+.65,z+3.75]);
    for (const x of [5.28,10.55]) box('Canvas stitched panel seam','cream',[x,y+.65,front+.12],[x+.17,y+.7,z+3.63]);
  }
  for (const [x0,x1,mat] of [[0,5.3,'teal'],[5.3,10.7,'canvas'],[10.7,16,'teal']]) {
    box('Front hanging canopy valance',mat,[x0,11.7,.35],[x1,13.42,.75]);
    box('Valance rolled lower hem',mat,[x0+.12,11.45,0],[x1-.12,11.9,.55]);
  }
  for (const x of [5.28,10.55]) box('Valance panel stitched seam','cream',[x,11.65,.29],[x+.17,13.39,.4]);
  for (const x of [2.8,12.65]) for (const z of [2.5,6.2,10,13.5]) {
    const row=Math.floor(z/3.75), y=13.52+row*.55;
    box('Canopy brass lashing eyelet','brass',[x,y,z],[x+.55,y+.12,z+.55]);
  }
});

model('signal_flag', () => {
  octXZ('Mast octagonal iron foundation','dark',8,8,2.25,2.25,0,.9);
  octXZ('Mast brass socket','brass',8,8,1.8,1.8,.9,1.85);
  box('Mast socket recessed face','dark',[6.9,1.05,6.13],[9.1,1.65,6.35]);
  boltN('Mast socket bolt',8,1.36,5.92,.65);
  box('Oak signal mast','wood',[7.25,1.8,7.25],[8.75,14.6,8.75]);
  box('Mast bright timber edge','edge',[7.28,2.15,7.18],[7.5,13.9,7.28]);
  for (const y of [2,4.25,13]) {
    box('Mast brass collar','brass',[6.95,y,6.95],[9.05,y+.6,9.05]);
    boltN('Mast collar pin',8,y+.3,6.73,.35);
  }
  box('Finial lower neck','dark',[7.4,14,7.4],[8.6,14.75,8.6]);
  box('Finial brass block','brass',[6.95,14.65,6.95],[9.05,15.8,9.05]);
  box('Finial bright crown','edge',[7.15,15.8,7.15],[8.85,16,8.85]);
  boltN('Finial face stud',8,15.2,6.74,.65);
  // The taper is stepped in plan and each cloth bay folds subtly in depth.
  const bays = [
    {a:8.95,b:10.7,lo:5.2,hi:12.8,z:7.58},
    {a:10.7,b:12.15,lo:5.8,hi:12.2,z:7.72},
    {a:12.15,b:13.5,lo:6.55,hi:11.45,z:7.86},
    {a:13.5,b:14.8,lo:7.25,hi:10.75,z:7.73},
    {a:14.8,b:15.85,lo:8.1,hi:9.9,z:7.6}
  ];
  for (const [i,p] of bays.entries()) {
    box('Teal pennant lower cloth bay','teal',[p.a,p.lo,p.z],[p.b,8.45,p.z+.46]);
    box('Ivory pennant identity stripe','canvas',[p.a,8.45,p.z-.03],[p.b,9.55,p.z+.49]);
    box('Teal pennant upper cloth bay','teal',[p.a,9.55,p.z],[p.b,p.hi,p.z+.46]);
    box('Pennant top folded hem','teal',[p.a,p.hi-.2,p.z-.05],[p.b,p.hi,p.z+.51]);
    box('Pennant bottom folded hem','teal',[p.a,p.lo,p.z-.05],[p.b,p.lo+.2,p.z+.51]);
    if(i<4) {
      box('Pennant vertical panel seam','dark',[p.b-.12,p.lo+.25,p.z-.035],[p.b,p.hi-.25,p.z+.02]);
      for (const y of [8.7,9.15]) box('Stripe seam stitching','cream',[p.b-.15,y,p.z-.06],[p.b-.03,y+.17,p.z-.02]);
    }
  }
  box('Pennant hoist reinforced edge','dark',[8.9,5.2,7.48],[9.15,12.8,8.14]);
  for (const y of [5.55,12.05]) {
    box('Pennant hoist brass eyelet','brass',[8.88,y,7.24],[9.5,y+.65,8.25]);
    box('Flag tie around mast','canvas',[7.07,y+.12,7.06],[9.02,y+.4,8.94]);
    box('Hoist cord knot','cream',[8.85,y+.16,7.08],[9.35,y+.61,7.52]);
  }
  box('Halyard rope','canvas',[8.86,3.25,8.75],[9.06,12.6,8.95]);
  box('Halyard cleat','brass',[8.7,3,8.75],[9.3,3.9,9.35]);
});

export default models;
