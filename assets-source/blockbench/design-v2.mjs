/** Astra art direction. These are inputs to native Blockbench MCP tools, never runtime model exports. */
import {writeFileSync,readFileSync} from 'node:fs';
import detailedEngines from './engines-detailed.mjs';
import detailedHelm from './helm-detailed.mjs';
import detailedDocks from './docks-detailed.mjs';
import detailedFittings from './fittings-detailed.mjs';
const liftLayouts=JSON.parse(readFileSync(new URL('./lift-envelope-layouts.json',import.meta.url),'utf8'));
export const palette={brass:'#b58b4a',dark:'#30383c',wood:'#775436',teal:'#245552',cream:'#d8cba9',blueprint:'#183f48',glow:'#6de5df',iron:'#657173',leather:'#593930',canvas:'#d8cba9',amber:'#e8a63a',edge:'#d3ad66'};
const models={};let parts=[];
const box=(name,material,from,to,rotation,origin)=>{parts.push({name,material,from,to,...(rotation?{rotation,origin}:{} )});};
const model=(id,fn)=>{parts=[];fn();models[id]=parts;};
const fencePost=()=>{
 box('Center timber post','wood',[6,0,6],[10,15.3,10]);
 box('Cast post shoe','dark',[5.65,0,5.65],[10.35,1.1,10.35]);box('Brass shoe band','brass',[5.8,1.1,5.8],[10.2,2.6,10.2]);
 box('Post crown dark lip','dark',[5.5,14.65,5.5],[10.5,15.1,10.5]);box('Crown bronze molding','brass',[5.65,15.1,5.65],[10.35,15.55,10.35]);box('Crown raised cap','edge',[6.05,15.55,6.05],[9.95,16,9.95]);
 for(const y of [3.3,10.8]){for(const z of [5.94,9.92]){box('Post rail socket strap','brass',[6.65,y,z],[9.35,y+1.15,z+.14]);for(const x of [7.05,8.7])box('Socket rivet','dark',[x,y+.38,z-.05],[x+.25,y+.68,z+.19]);}for(const x of [5.94,9.92]){box('Post side socket strap','brass',[x,y,6.65],[x+.14,y+1.15,9.35]);for(const z of [7.05,8.7])box('Side socket rivet','dark',[x-.05,y+.38,z],[x+.19,y+.68,z+.25]);}}
};
const fenceSide=(dir='north')=>{for(const y of [5,12]){if(dir==='north'){box('Connected north rail','wood',[7,y,0],[9,y+2,8]);box('North rail wear','edge',[6.98,y+1.95,0],[9.02,y+2.05,8]);}else{box('Connected transverse rail','wood',[0,y,7],[16,y+2,9]);box('Transverse rail wear','edge',[0,y+1.95,6.98],[16,y+2.05,9.02]);}}};
model('timber_railing_post',fencePost);model('timber_railing_side',()=>fenceSide());model('timber_railing_inventory',()=>{fencePost();fenceSide('eastwest');});
for(let mask=0;mask<64;mask++)model('lift_cell_'+mask,()=>{
  const original=liftLayouts[mask];
  for(const e of original.elements){const face=Object.values(e.faces).find(f=>f.texture);let mat=(original.textures[face.texture.slice(1)]||'canvas').split('/').pop();if(mat==='wood')mat='leather';if(e.name.startsWith('Envelope')&&!(mask&8)&&e.to[1]<=4)mat='teal';box(e.name,mat,e.from,e.to,e.rotation?[e.rotation.axis==='x'?e.rotation.angle:0,e.rotation.axis==='y'?e.rotation.angle:0,e.rotation.axis==='z'?e.rotation.angle:0]:undefined,e.rotation?.origin);}
  for(const [bit,side]of [[32,'north'],[16,'south']])if(!(mask&bit)){
    for(let y=0;y<16;y+=2){const shell=original.elements.filter(e=>e.name.startsWith('Envelope')&&e.from[1]<=y&&e.to[1]>y&&e.from[0]<9&&e.to[0]>7);if(!shell.length)continue;const z=side==='north'?Math.min(...shell.map(e=>e.from[2])):Math.max(...shell.map(e=>e.to[2]));box('Exterior brass suspension band','brass',[7.3,y,side==='north'?z-.04:z-.16],[8.7,y+2,side==='north'?z+.16:z+.04]);}
    const z=side==='north'?-.12:15.88;box('Suspension buckle','brass',[6.5,6.5,z],[9.5,9.5,z+.24]);box('Buckle inset','teal',[7,7,side==='north'?z-.04:z+.24],[9,9,side==='north'?z:z+.28]);
  }
});
Object.assign(models,detailedEngines,detailedDocks,detailedFittings); models.helm=detailedHelm;
export {models};
if(process.argv[2])writeFileSync(process.argv[2],JSON.stringify({palette,models},null,2));


