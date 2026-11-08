#!/usr/bin/env python3
"""Build the editable Blockbench project for a converted species via the Blockbench MCP plugin.

Usage: python3 tools/legacy_models/blockbench_export.py <species> [screenshot dir]

Needs Blockbench running with the MCP plugin (http://localhost:3000/bb-mcp). Imports the
generated geometry, texture and animations into a new GeckoLib project, runs the plugin's
GeckoLib validation, and saves art/models/<species>.bbmodel. With a screenshot directory it
also renders one pose per animation there.
"""
import json, sys, base64, urllib.request, os
URL='http://localhost:3000/bb-mcp'
REPO=os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
A=REPO+'/src/main/resources/assets/fossilsarch2'
SHOTS=sys.argv[2] if len(sys.argv) > 2 else None
def post(body, sid=None):
    h={'Content-Type':'application/json','Accept':'application/json, text/event-stream'}
    if sid: h['mcp-session-id']=sid
    r=urllib.request.urlopen(urllib.request.Request(URL,json.dumps(body).encode(),h),timeout=300)
    sid=r.headers.get('mcp-session-id') or sid; out=None
    for line in r.read().decode().splitlines():
        line=line[6:] if line.startswith('data: ') else line
        if line.startswith('{'): out=json.loads(line)
    return out,sid
_,SID=post({'jsonrpc':'2.0','id':1,'method':'initialize','params':{'protocolVersion':'2025-06-18','capabilities':{},'clientInfo':{'name':'fa2-push','version':'0'}}})
post({'jsonrpc':'2.0','method':'notifications/initialized'},SID)
N=[10]
def call(tool, args, img=None):
    N[0]+=1
    res,_=post({'jsonrpc':'2.0','id':N[0],'method':'tools/call','params':{'name':tool,'arguments':args}},SID)
    if 'error' in res: raise RuntimeError(f"{tool}: {res['error']}")
    r=res['result']
    texts=[]
    for c in r.get('content',[]):
        if c.get('type')=='image' and img:
            open(img,'wb').write(base64.b64decode(c['data']))
        elif c.get('type')=='text': texts.append(c['text'])
    if r.get('isError'): raise RuntimeError(f"{tool}: {' '.join(texts)[:500]}")
    return '\n'.join(texts)

def ed_rot(v): return [-v[0],-v[1],v[2]]
def ed_pos(v): return [-v[0],v[1],v[2]]

sp=sys.argv[1]
call('create_project',{'name':sp,'format':'geckolib_model'})
call('geckolib_set_project_settings',{'modid':'fossilsarch2','model_type':'Entity','model_identifier':sp})
call('from_geo_json',{'geojson':f'{A}/geckolib/models/{sp}.geo.json'})
call('create_texture',{'name':f'{sp}.png','data':f'{A}/textures/entity/{sp}/{sp}.png'})
anims=json.load(open(f'{A}/geckolib/animations/{sp}.animation.json'))['animations']
for name,a in anims.items():
    call('create_animation',{'name':name,'loop':a['loop'],'animation_length':a['animation_length'],'bones':{}})
    for bone,chs in a['bones'].items():
        for ch,frames in chs.items():
            conv={'rotation':ed_rot,'position':ed_pos,'scale':lambda v:v}[ch]
            kfs=[{'time':float(t),'values':conv(f['vector']),'interpolation':'catmullrom'} for t,f in frames.items()]
            call('manage_keyframes',{'animation_id':'animation.'+name,'action':'create','bone_name':bone,'channel':ch,'keyframes':kfs})
print(call('geckolib_validate_model',{'include_animations':True})[:1500])
# Compare Blockbench's GeckoLib geometry export against ours (cube count and bone names).
exp=json.loads(json.loads(call('geckolib_export_model',{'max_content_length':2000000}))['content'])
ours=json.load(open(f'{A}/geckolib/models/{sp}.geo.json'))
eb=exp['minecraft:geometry'][0]['bones']; ob=ours['minecraft:geometry'][0]['bones']
print('bones', [b['name'] for b in eb]==[b['name'] for b in ob], 'cubes', sum(len(b.get('cubes',[])) for b in eb), sum(len(b.get('cubes',[])) for b in ob))
# Save the editable project.
proj=json.loads(call('export_model',{'codec_id':'project','max_content_length':2000000}))
content=proj.get('content') or proj.get('files',[{}])[0].get('content')
if content:
    data=json.loads(content)
    for an in data.get('animations',[]):
        an['name']=an['name'].removeprefix('animation.')  # GeckoLib looks clips up by bare name
    os.makedirs(REPO+'/art/models',exist_ok=True)
    json.dump(data,open(f'{REPO}/art/models/{sp}.bbmodel','w'),indent='\t')
    print('bbmodel saved')
else:
    print('no project content:', str(proj)[:300])
# Pose screenshots.
if not SHOTS:
    sys.exit(0)
try: call('delete_offscreen_view',{'view':'p'+sp})
except Exception: pass
call('create_offscreen_view',{'id':'p'+sp,'width':700,'height':500,'copy_view':'none'})
call('set_mode',{'mode_id':'animate'})
for name in anims:
    a=anims[name]
    call('animation_timeline',{'animation_id':'animation.'+name,'action':'set_time','time':a['animation_length']*(0.25 if a['loop'] else 0.3)})
    call('set_camera_angle',{'view':'p'+sp,'projection':'perspective','position':[-38,26,-40],'target':[0,9,-2]},img=f'{SHOTS}/{sp}_{name}.png')
call('set_mode',{'mode_id':'edit'})
call('delete_offscreen_view',{'view':'p'+sp})
print('done')
