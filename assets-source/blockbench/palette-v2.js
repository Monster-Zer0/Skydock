(() => {
  const colors={brass:'#b58b4a',dark:'#30383c',wood:'#775436',teal:'#245552',cream:'#d8cba9',blueprint:'#183f48',glow:'#6de5df',iron:'#657173',leather:'#593930',canvas:'#d8cba9',amber:'#e8a63a',edge:'#d3ad66'};
  Project.texture_width=16;Project.texture_height=16;
  for(const[name,hex]of Object.entries(colors)){
    const canvas=document.createElement('canvas');canvas.width=canvas.height=64;
    const ctx=canvas.getContext('2d');const rgb=[1,3,5].map(i=>parseInt(hex.slice(i,i+2),16));
    for(let y=0;y<64;y++)for(let x=0;x<64;x++){
      let shade=((x*17+y*31+x*y*3)%9)-4;
      if(name==='wood')shade+=Math.round(Math.sin(x*.64+Math.sin(y*.09)*.7)*9)+(x%17===0?-12:0)+((x*7+y*3)%59===0?8:0);
      if(name==='canvas')shade+=(x%3===0||y%3===0?-4:2);
      if(name==='leather')shade+=(x===3||x===60||y===3||y===60?8:0)+((x*13+y*7)%43===0?-7:0);
      if(name==='brass'||name==='edge')shade+=(y<2?12:y>61?-12:0)+((x+3*y)%61===0?7:0);
      if(name==='teal')shade+=(x<2||y<2?7:x>61||y>61?-7:0)+((x*7+y*17)%137===0?10:0);
      if(name==='glow'||name==='amber')shade+=Math.round(28-Math.hypot(x-31.5,y-31.5)*1.4);
      ctx.fillStyle='rgb('+rgb.map(v=>Math.max(0,Math.min(255,v+shade))).join(',')+')';ctx.fillRect(x,y,1,1);
    }
    if(name==='canvas'){ctx.fillStyle='#b5a88c';for(let y=2;y<64;y+=5){ctx.fillRect(30,y,1,3);ctx.fillRect(33,y+2,1,3);}}
    if(name==='blueprint'){
      ctx.scale(4,4);ctx.fillStyle='#204952';for(let x=1;x<16;x+=4)ctx.fillRect(x,0,1,16);for(let y=1;y<16;y+=4)ctx.fillRect(0,y,16,1);
      ctx.fillStyle='#9dd7cf';for(let x=3;x<=12;x++){ctx.fillRect(x,4,1,1);ctx.fillRect(x,8,1,1);}ctx.fillRect(2,5,1,3);ctx.fillRect(13,5,1,3);ctx.fillRect(4,9,1,2);ctx.fillRect(11,9,1,2);ctx.fillRect(5,11,6,1);ctx.fillRect(6,12,4,1);ctx.fillRect(7,5,1,3);ctx.fillRect(10,5,1,3);ctx.fillRect(3,6,10,1);
      ctx.fillStyle='#477977';ctx.fillRect(1,14,5,1);ctx.fillRect(10,14,4,1);
    }
    const texture=new Texture({name:name+'.png',namespace:'skydock',folder:'block/palette_v2',uv_width:16,uv_height:16}).fromDataURL(canvas.toDataURL()).add();texture.uv_width=texture.uv_height=16;
  }
  Project.texture_width=Project.texture_height=16;
  return {textures:Texture.all.length,names:Texture.all.map(t=>t.name)};
})()
