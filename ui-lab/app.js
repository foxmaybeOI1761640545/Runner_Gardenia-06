'use strict';
(() => {
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const themes = [
    {id:'precision',name:'Precision Pro',cn:'专业运动仪表',description:'让每一项数据，都在正确的位置。石墨黑与荧光绿构成清晰的运动仪表，把注意力留给当下的表现。',language:'精密网格 · 大号数字 · 高对比',native:'XML 网格 + 原生图表',colors:['#121614','#ccf96b','#f2f5ec']},
    {id:'quiet',name:'Quiet Minimal',cn:'极简系统风格',description:'去掉多余的声音，留下运动本身。用留白、轻盈的排版与低干扰的线条，让数据自然呼吸。',language:'开放布局 · 细字重 · 留白',native:'原生布局 + 轻量分隔线',colors:['#fafbf9','#3c6a54','#24352e']},
    {id:'explorer',name:'Outdoor Explorer',cn:'户外探险风格',description:'把界面变成随身的野外手记。森林绿、大地色与等高线，将路线放在视野中心。',language:'地图优先 · 地形纹理 · 手记',native:'XML + Canvas 轨迹概览',colors:['#f0eee2','#345c3d','#cbd5b7']},
    {id:'neon',name:'Neon Pulse',cn:'科技霓虹风格',description:'让每一次心跳都有迹可循。青色波形、紫色层次和克制的微光，构成实时运动的节奏。',language:'心率优先 · 波形 · 霓虹边缘',native:'ChartView + 轻量描边',colors:['#0c0e1b','#70f4ed','#b78ae5']},
    {id:'swiss',name:'Swiss Editorial',cn:'瑞士编辑设计',description:'像翻开一本运动杂志。大幅数字、非对称网格和醒目的红色标记，带来直接而有力量的信息表达。',language:'非对称 · 红色标记 · 粗线',native:'XML 排版 + Shape Drawable',colors:['#f6f2e9','#171816','#d13627']},
    {id:'industrial',name:'Industrial Gauge',cn:'工业仪表风格',description:'一块可靠的随身仪表。琥珀色读数、机械刻度和等宽排版，让运动状态清楚可查。',language:'环形刻度 · 等宽数字 · 硬面板',native:'XML + Canvas 刻度仪表',colors:['#262824','#efbd5d','#535548']},
    {id:'wellness',name:'Calm Wellness',cn:'柔和健康风格',description:'以温和的方式陪伴每一次出发。奶油白、薄荷绿与柔软的圆形层次，让记录运动成为舒适的日常。',language:'柔和圆环 · 有机曲线 · 软卡片',native:'Material 形状 + 自绘圆环',colors:['#f7f5ec','#80a289','#354c42']},
    {id:'aurora',name:'Aurora Glass',cn:'现代玻璃科技',description:'把极光的层次装进口袋。蓝紫渐变与透明描边呈现轻盈质感，实色内容层保证每个数字都能读清。',language:'渐变背景 · 透明层次 · 浮层',native:'渐变 Drawable · 无模糊依赖',colors:['#151b39','#bccaff','#2f7385']},
    {id:'racing',name:'Racing Performance',cn:'赛车竞速风格',description:'把专注切换到竞速模式。黑白红、倾斜的大型数字和紧凑的分区，形成果断的竞技表达。',language:'倾斜数字 · 速度条纹 · 记分板',native:'XML 面板 + Path 装饰',colors:['#f3f3ef','#161817','#cc302c']},
    {id:'brutal',name:'Data Brutalism',cn:'数据粗野主义',description:'数据不需要修饰，只需要被看见。荧光黄、几何分块与硬边阴影，把核心信息直接摆在眼前。',language:'几何分块 · 硬边阴影 · 原始数据',native:'XML 网格 + Shape 边框',colors:['#fafaf2','#11120c','#e9f34b']}
  ];
  const paths = {
    run:'<path d="m8 21 3-6 4 1 3 5M6 13l5-4 4 3 4-1M11 9l2 6M16 4a2 2 0 1 0 .01 0"/>',
    history:'<path d="M3 11a9 9 0 1 1 2 7M3 5v6h6M12 7v5l3 2"/>',
    heart:'<path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z"/>',
    settings:'<path d="m9 3-1 3-3 1-2 3 2 2-1 3 2 3 3-1 3 3 3-2 3 1 2-3-1-3 2-2-2-3-3-1-1-3Z"/><circle cx="12" cy="11" r="3"/>',
    gps:'<circle cx="12" cy="12" r="6"/><path d="M12 2v4m0 12v4M2 12h4m12 0h4"/><circle cx="12" cy="12" r="1"/>',
    bluetooth:'<path d="M7 7l10 10-5 4V3l5 4L7 17"/>',
    arrow:'<path d="m14 6-6 6 6 6"/>',
    play:'<path d="m8 5 11 7-11 7Z"/>', pause:'<path d="M8 5v14M16 5v14"/>', stop:'<rect x="6" y="6" width="12" height="12" rx="1"/>',
    clock:'<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    speed:'<path d="M4 19a10 10 0 1 1 16 0M12 14l5-6M5 14h1m12 0h1M8 6l1 2"/>',
    export:'<path d="M12 15V3m-4 4 4-4 4 4M5 13v7h14v-7"/>',
    check:'<circle cx="12" cy="12" r="9"/><path d="m7 12 3 3 7-7"/>',
    alert:'<path d="m12 3 10 18H2Z M12 9v5m0 3v1"/>',
    scan:'<path d="M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5M7 12h10"/>',
    chart:'<path d="M3 3v18h18M5 15l4-5 4 3 6-7"/>',shield:'<path d="m12 2 8 4v6c0 6-8 10-8 10S4 18 4 12V6Z"/><path d="m8 12 3 3 5-6"/>'
  };
  const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths[name] || paths.run}</svg>`;
  const safe = value => String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const defaultRows = [
    {id:1,name:'清晨 · 河畔跑',date:'2026.11.05',time:'07:12',distance:5.24,seconds:1714,average:148,max:171,min:113},
    {id:2,name:'傍晚 · 公园跑',date:'2026.11.03',time:'18:35',distance:3.80,seconds:1342,average:139,max:158,min:105},
    {id:3,name:'周末 · 轻松跑',date:'2026.11.01',time:'08:06',distance:8.12,seconds:2860,average:144,max:165,min:108}
  ];
  const initial = () => ({gps:'fixed',gpsGap:false,heart:'connected',session:'recording',seconds:1714,distance:5.24,page:'run',detail:1,chartReturn:'heart',selectedDevice:0,autoReconnect:true,unit:'km',rate:'pace',contact:true,emptyHistory:false,exportFailure:false,modal:null,exportResult:null});
  let state = initial(), rows = defaultRows.map(x=>({...x})), themeIndex = 0, connectionGeneration = 0, scanGeneration = 0;
  // Each theme owns an ordered set of positions, not just a preferred primary ID.
  // Swapping never rebuilds or sorts the secondary array. Demo reset leaves it intact.
  const metricLayoutsByTheme = Object.fromEntries(themes.map(theme => [theme.id,
    theme.id === 'neon'
      ? {primary:'heartRate', secondary:['distance','duration','paceSpeed','gpsAccuracy'], notice:''}
      : {primary:'distance', secondary:['duration','paceSpeed','heartRate','gpsAccuracy'], notice:''}
  ]));
  const metricLayout = () => metricLayoutsByTheme[themes[themeIndex].id];
  let favoriteIds = [];
  try {favoriteIds = JSON.parse(localStorage.getItem('gardenia-favorites') || '[]');if(!Array.isArray(favoriteIds))favoriteIds=[];} catch {favoriteIds=[];}
  const devices = [{name:'Gardenia HR-01',id:'D0:11:22:33:44:01',rssi:-54},{name:'Runner Chest 02',id:'D0:11:22:33:44:02',rssi:-72}];
  const gpsNames = {searching:'正在搜星',fixed:'定位成功',lost:'GPS 信号丢失'};
  const heartNames = {disconnected:'未连接',scanning:'扫描中',available:'发现设备',connecting:'连接中',connected:'已连接',reconnecting:'正在重连',stale:'数据过期'};
  const sessionNames = {idle:'准备出发',recording:'正在记录',paused:'用户主动暂停'};
  const content = $('#phone-content');
  const screen = $('#phone-screen');
  const pad = n => String(n).padStart(2,'0');
  const duration = seconds => {const s=Math.floor(seconds);return s>=3600?`${pad(Math.floor(s/3600))}:${pad(Math.floor(s%3600/60))}:${pad(s%60)}`:`${pad(Math.floor(s/60))}:${pad(s%60)}`;};
  const distance = value => (state.unit==='mi'?value/1.609344:value).toFixed(2);
  const pace = (km,seconds) => {const p = seconds/(state.unit==='mi'?km/1.609344:km);return `${Math.floor(p/60)}′${pad(Math.floor(p%60))}″`;};
  const speed = (km,seconds) => ((state.unit==='mi'?km/1.609344:km)*3600/Math.max(seconds,1)).toFixed(1);
  const rateUnit = () => state.rate==='pace'?`/${state.unit}`:state.unit==='mi'?'mph':'km/h';
  const rateValue = (km,seconds) => state.rate==='pace'?pace(km,seconds):speed(km,seconds);
  const fresh = () => state.heart==='connected' && state.contact;
  const currentBpm = () => fresh() ? (state.session==='recording'?148:82) : '--';
  const canStart = () => state.gps==='fixed';
  const selectedRow = () => rows.find(x=>x.id===state.detail) || rows[0] || defaultRows[0];
  function metric(value,unit,label,name='',options={}) {
    const interactive = options.id && options.id !== 'gpsAccuracy';
    const tag = interactive ? 'button' : 'div';
    const binding = options.id ? ` data-metric-role="secondary" data-metric-id="${options.id}" data-slot="${options.slot}"` : '';
    const action = interactive ? ` type="button" data-action="swap-metric" aria-label="${safe(swapLabel(options.id))}"` : '';
    return `<${tag} class="metric${interactive?' metric-swap':''}"${binding}${action}><span class="value"><span class="metric-value-text">${safe(value)}</span><small class="metric-unit">${safe(unit)}</small></span><span class="label">${name?icon(name):''}<span class="metric-label-text">${safe(label)}</span></span></${tag}>`;
  }
  function metricData(id,d=motion()) {
    const metrics = {
      distance:{value:d.distance,unit:state.unit,label:'运动距离',kicker:'DISTANCE',icon:'run'},
      duration:{value:d.duration,unit:'',label:'运动时长',kicker:'DURATION',icon:'clock'},
      paceSpeed:{value:d.rate,unit:rateUnit(),label:state.rate==='pace'?'当前配速':'当前速度',kicker:state.rate==='pace'?'PACE':'SPEED',icon:'speed'},
      heartRate:{value:String(d.bpm),unit:'BPM',label:'实时心率',kicker:'HEART RATE',icon:'heart'},
      gpsAccuracy:{value:state.gps==='fixed'?'±5':'--',unit:state.gps==='fixed'?'m':'',label:'定位精度',kicker:'GPS ACCURACY',icon:'gps'}
    };
    return {...metrics[id],id};
  }
  function swapLabel(id) {
    return `将${metricData(id).label}设为主显示，与${metricData(metricLayout().primary).label}交换`;
  }
  function primaryMetric(d) {
    const m=metricData(metricLayout().primary,d);
    return `<div class="primary-readout" data-metric-role="primary" data-metric-id="${m.id}"><div class="hero-label"><span class="metric-kicker">${m.kicker}</span> / <span class="metric-label-text">${safe(m.label)}</span></div><div class="hero-number" data-metric-id="${m.id}" data-value-size="${String(m.value).length>=7?'long':'normal'}"><span class="metric-value-text">${safe(m.value)}</span><small class="metric-unit">${safe(m.unit)}</small></div></div>`;
  }
  function swapMetric(slot) {
    const layout=metricLayout();
    if(!Number.isInteger(slot)||slot<0||slot>=3||layout.secondary[slot]==='gpsAccuracy')return;
    [layout.primary,layout.secondary[slot]]=[layout.secondary[slot],layout.primary];
    layout.notice=`主显示已切换为${metricData(layout.primary).label}`;
    render();
    $('#metric-announcement').textContent=layout.notice;
  }
  function updateMotionMetrics() {
    const d=motion();
    $$('[data-metric-role]',content).forEach(element=>{
      const m=metricData(element.dataset.metricId,d);
      $('.metric-value-text',element).textContent=m.value;
      $('.metric-unit',element).textContent=m.unit;
      $('.metric-label-text',element).textContent=m.label;
      const number=$('.hero-number',element);
      if(number)number.dataset.valueSize=String(m.value).length>=7?'long':'normal';
    });
  }
  function motion() {
    const idle=state.session==='idle', valid=state.gps==='fixed';
    return {distance:valid?distance(idle?0:state.distance):'--',duration:duration(idle?0:state.seconds),rate:valid&&state.session==='recording'?rateValue(5.24,1714):'--',bpm:currentBpm()};
  }
  function appHeader(title,sub='RUNNER GARDENIA') {return `<header class="app-header"><div><div class="app-kicker">${safe(sub)}</div><h2>${safe(title)}</h2></div><span class="demo-tag">模拟数据</span></header>`;}
  function recordLabel(){return `<div class="record-state"><i></i>${safe(sessionNames[state.session])} <span class="muted">/ 模拟</span></div>`;}
  function statusRow(){return `<div class="status-row"><button class="status-pill ${state.gps!=='fixed'?'warning':''}" data-action="gps-page">${icon('gps')}${gpsNames[state.gps]}</button><button class="status-pill ${!fresh()?'warning':''}" data-page="heart">${icon('bluetooth')}${heartNames[state.heart]}</button></div>`;}
  function chart(historical=false,compact=false){
    if(!historical&&!fresh())return '<div class="chart-empty">等待有效心率数据<br>断连与过期区间不连线</div>';
    const a='M26 94L33 88L39 95L46 78L53 84L60 70L67 73L74 63L81 76L88 64L95 68L102 49L109 55L116 43L123 54L130 40L137 47L144 29L151 40L158 34L165 44L172 36';
    const b='M192 48L199 42L206 54L213 45L220 60L227 49L234 64L241 57L248 72L255 62L262 69L269 63L276 77L283 67L290 81L301 74';
    return `<svg class="chart" viewBox="0 0 320 145" role="img" aria-label="${historical?'历史':'模拟实时'}心率曲线，横轴时间，纵轴 BPM，数据缺口断线"><path d="M26 25H306M26 65H306M26 105H306" stroke="currentColor" opacity=".12" stroke-dasharray="3 4" fill="none"/><text x="0" y="28">180</text><text x="0" y="68">140</text><text x="0" y="108">100</text><path d="${a} ${b}" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linejoin="round"/><path d="M181 22v90" stroke="currentColor" stroke-dasharray="2 4" opacity=".3"/><text x="26" y="135">${historical?'0′':'-60s'}</text><text x="140" y="135">${historical?'14′':'-30s'}</text><text x="275" y="135">${historical?'28′':'现在'}</text>${compact?'':'<text x="158" y="13">数据缺口</text>'}</svg>`;
  }
  function chartCard(historical=false){return `<section class="card chart-card"><div class="chart-heading"><strong>心率曲线</strong><span>BPM · ${historical?'运动全程':'模拟最近 60 秒'}</span></div>${chart(historical)}<button class="text-button" data-action="heart-chart">查看心率详情 ↗</button></section>`;}
  function route(historical=false){
    const visible=historical || (state.gps==='fixed'||state.gps==='lost')&&state.session!=='idle';
    const hasGap=historical?Boolean(selectedRow().gap):state.gpsGap;
    const routeLine=hasGap?'M76 136 90 111 104 90 139 81 169 57 M206 65 226 96 254 117 246 143 218 151 187 136':'M76 136 90 111 104 90 139 81 169 57 206 65 226 96 254 117 246 143 218 151 187 136';
    return `<div class="route-map">${!visible?`<div class="route-empty">${icon('gps')}${state.gps==='lost'?'信号丢失，轨迹停止更新':state.gps==='searching'?'等待有效定位，不绘制轨迹':'开始后显示模拟轨迹'}</div>`:`<svg viewBox="0 0 350 190" role="img" aria-label="本地 SVG 模拟轨迹概览，不是真实地图"><g stroke="currentColor" opacity=".12" fill="none" stroke-width="1"><path d="M-10 25Q50 100 120 30T270 65T400 20M-20 45Q45 130 120 55T270 90T400 45M-25 65Q40 160 120 80T270 115T400 70M-20 85Q45 190 120 105T270 140T400 95M-20 105Q45 220 120 130T270 165T400 120M-20 125Q45 240 120 155T270 190T400 145"/><path d="M245-10Q185 65 235 115T210 220" stroke-width="18"/><path d="m-10 120 100-20 80 10 90-70 110-20" stroke-width="5"/></g><path data-route-line="true" d="${routeLine}" stroke="currentColor" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/><circle cx="76" cy="136" r="6" fill="var(--surface)" stroke="currentColor" stroke-width="3"/><circle cx="187" cy="136" r="6" fill="currentColor"/><text x="304" y="22" fill="currentColor" font-size="10" font-family="Arial">N ↑</text></svg><span class="map-caption">${!historical&&state.gps==='lost'?'GPS 丢失 · 已记录轨迹冻结':hasGap?'模拟轨迹 · 缺失区间断开':'本地 SVG · 模拟轨迹概览'}</span>`}</div>`;
  }
  function dataGrid(d) {
    const layout=metricLayout();
    const cards=layout.secondary.map((id,slot)=>{
      const m=metricData(id,d);
      return metric(m.value,m.unit,m.label,m.icon,{id,slot});
    }).join('');
    return `<div class="metric-grid motion-grid" aria-label="副显示指标，点击可与主显示交换">${cards}</div><p class="metric-swap-hint">${safe(layout.notice?`主显示已切换为${metricData(layout.primary,d).label}`:'点击副指标，与主显示交换')}</p>`;
  }
  function hero(d){
    const readout=primaryMetric(d);
    switch(themes[themeIndex].id){
      case 'quiet':return `<div class="hero metrics-hero">${recordLabel()}${readout}</div>${dataGrid(d)}`;
      case 'explorer':return `<div class="hero map-hero metrics-hero">${route()}<div class="map-overlay">${readout}</div></div>${recordLabel()}${dataGrid(d)}`;
      case 'neon':return `<div class="hero pulse-hero metrics-hero">${readout}<div class="pulse-chart-label">心率曲线 · BPM</div>${chart(false,true)}</div>${recordLabel()}${dataGrid(d)}`;
      case 'swiss':return `<div class="hero editorial-hero metrics-hero"><div class="editorial-index">01 / ${metricData(metricLayout().primary,d).kicker}</div>${readout}</div>${recordLabel()}${dataGrid(d)}`;
      case 'industrial':return `<div class="hero gauge-hero metrics-hero">${readout}</div>${recordLabel()}${dataGrid(d)}`;
      case 'wellness':return `<div class="hero wellness-hero metrics-hero">${readout}</div>${recordLabel()}${dataGrid(d)}`;
      case 'aurora':return `<div class="hero glass-hero metrics-hero">${recordLabel()}${readout}</div>${dataGrid(d)}`;
      case 'racing':return `<div class="hero race-hero metrics-hero">${recordLabel()}${readout}</div>${dataGrid(d)}`;
      case 'brutal':return `${recordLabel()}<div class="hero brutal-hero metrics-hero">${readout}</div>${dataGrid(d)}`;
      default:return `${recordLabel()}<div class="hero metrics-hero">${readout}</div>${dataGrid(d)}`;
    }
  }
  function recordActions(){
    if(state.session==='idle')return `<div class="record-actions single"><button class="primary full" data-action="start" ${!canStart()?'disabled':''}>${icon('play')}${canStart()?'开始记录':'等待有效定位'}</button></div>`;
    return `<div class="record-actions"><button class="primary" data-action="${state.session==='paused'?'resume':'pause'}" ${state.session==='paused'&&!canStart()?'disabled':''}>${icon(state.session==='paused'?'play':'pause')}${state.session==='paused'?'恢复记录':'暂停记录'}</button><button class="secondary" data-action="finish">${icon('stop')}结束</button></div>`;
  }
  function gpsNote(){return state.gps==='searching'?'正在搜索卫星，等待有效定位。定位精度尚未确定，距离、速度和配速不提供估算。':state.gps==='lost'?'GPS 信号丢失：距离与轨迹停止更新，恢复后仅记录新位置，不补绘丢失区间。':state.session==='paused'?'用户已主动暂停。位置有效，但运动记录未更新；需点击恢复记录。':'模拟定位精度 ± 5 m · 有效定位；所有位置与记录仅作设计演示。';}
  function runPage(){const d=motion();const last=rows[0];return `${appHeader('运动','RUNNER GARDENIA / P0')}${statusRow()}${hero(d)}${recordActions()}<p class="state-note ${state.gps!=='fixed'?'warning':''}">${gpsNote()}</p>${themes[themeIndex].id==='explorer'?'':route()}<div class="app-section-label">最近一次运动 <span>本地 · 模拟</span></div>${state.emptyHistory||!last?'<p class="state-note">还没有运动记录。开始你的第一次出发。</p>':`<button class="card recent" data-detail="${last.id}"><span class="recent-icon">${icon('run')}</span><div><strong>${safe(last.name)}</strong><p>${distance(last.distance)} ${state.unit} · ${duration(last.seconds)} · ${last.average} BPM</p></div><span>↗</span></button>`}`;}
  function gpsPage(){return `<div class="detail-heading"><button class="icon-button" data-page="run" aria-label="返回运动">${icon('arrow')}</button><h2>GPS 定位状态</h2></div><div class="card device-hero"><div class="heart-symbol">${icon('gps')}</div><h2>${gpsNames[state.gps]}</h2><span class="demo-tag">模拟 GPS · 不访问真实位置</span></div><div class="card"><div class="setting-row"><span>运动记录状态</span><strong>${sessionNames[state.session]}</strong></div><div class="setting-row"><span>定位精度</span><strong>${state.gps==='fixed'?'± 5 m':'尚未确定'}</strong></div><div class="setting-row"><span>有效位置</span><strong>${state.gps==='fixed'?'模拟位置有效':'等待有效定位'}</strong></div></div><p class="state-note">${gpsNote()}</p><p class="state-note">未展示经纬度，未申请浏览器定位权限。通过手机外的状态模拟器切换 GPS 状态。</p>${recordActions()}`;}
  function historyPage(){const list=state.emptyHistory?[]:rows;return `${appHeader('运动历史','YOUR LOCAL JOURNAL')}${list.length?`<section class="card history-summary"><div class="app-kicker" style="color:inherit">模拟记录 / 共 ${list.length} 次</div><div class="value">${distance(list.reduce((n,r)=>n+r.distance,0))}<small>${state.unit}</small></div><p>每一次出发，都被好好记录。</p></section><div class="app-section-label">全部记录 <span>按日期排序 · 模拟数据</span></div>${list.map(row=>`<button class="history-row" data-detail="${row.id}"><header><strong style="font-size:12px;letter-spacing:0">${safe(row.name)}</strong><span>${safe(row.date)} ↗</span></header><div class="row"><strong>${distance(row.distance)}<small>${state.unit}</small></strong><span>${duration(row.seconds)}</span></div><p>平均${state.rate==='pace'?'配速':'速度'} ${rateValue(row.distance,row.seconds)} ${rateUnit()}<br>平均心率 ${row.average||'--'} BPM · 最高 ${row.max||'--'} BPM</p></button>`).join('')}`:`<div class="empty-state">${icon('history')}<h3>第一条记录，等你出发</h3><p>运动记录只保存在本地。<br>结束一次运动后，就能在这里回顾。</p><button class="primary full" data-page="run">前往运动</button></div>`}`;}
  function detailPage(){const r=selectedRow();return `<div class="detail-heading"><button class="icon-button" data-page="history" aria-label="返回历史记录">${icon('arrow')}</button><h2>运动详情</h2><span class="demo-tag">模拟记录</span></div><div class="app-kicker">${safe(r.date)} / ${safe(r.time)}</div><h2 style="font-size:22px">${safe(r.name)}</h2><div class="hero"><div class="hero-label">运动距离</div><div class="hero-number">${distance(r.distance)}<small>${state.unit}</small></div></div><section class="card detail-stats">${metric(duration(r.seconds),'','运动时长')}${metric(speed(r.distance,r.seconds),state.unit==='mi'?'mph':'km/h','平均速度')}${metric(rateValue(r.distance,r.seconds),rateUnit(),state.rate==='pace'?'平均配速':'平均速度')}${metric(r.average||'--','BPM','平均心率')}${metric(r.max||'--','BPM','最高心率')}${metric(r.min||'--','BPM','最低心率')}</section><div class="app-section-label">GPS 轨迹 <span>本地 SVG 示意</span></div>${route(true)}<p class="state-note">仅示意已有运动轨迹，无在线底图；不代表实时位置。</p>${chartCard(true)}<button class="primary full" data-action="export">${icon('export')}导出 GPX</button><p class="state-note">导出的是示例文件，不包含真实运动数据。</p>`;}
  function heartPage(){const device=devices[state.selectedDevice];return `${appHeader('心率与设备','FOLLOW YOUR PULSE')}<section class="card device-hero"><div class="heart-symbol">${icon('heart')}</div><div class="bpm-number">${currentBpm()}<small>BPM</small></div><div class="connection-label">${state.heart==='connected'&&!state.contact?'接触不良':heartNames[state.heart]}</div><div class="device-meta"><span>数据更新时间<strong>${fresh()?'刚刚 · 模拟':state.heart==='stale'?'12 秒前 · 已过期':'尚无有效数据'}</strong></span><span>接触状态<strong>${state.heart==='connected'?(state.contact?'接触良好':'接触不良'):'未确认'}</strong></span></div></section><div class="row"><div class="app-section-label" style="margin:10px 0">心率带管理</div><button class="text-button" data-action="scan">${state.heart==='scanning'?'扫描中…':'扫描附近设备'}</button></div>${state.heart==='scanning'?'<p class="state-note">BLE 模拟扫描中，正在发现附近心率带…</p>':''}<ul class="device-list" aria-label="模拟附近设备">${devices.map((d,i)=>`<li><button class="device-option ${i===state.selectedDevice?'selected':''}" data-device="${i}" ${state.heart==='scanning'?'disabled':''}>${icon('bluetooth')}<div><strong>${safe(d.name)}</strong><small>${safe(d.id)} · RSSI ${d.rssi} dBm</small></div><span>${i===state.selectedDevice?'已选择':'连接'}</span></button></li>`).join('')}</ul><div class="connect-actions"><button class="primary full" data-action="reconnect" ${state.heart==='connecting'?'disabled':''}>${icon('bluetooth')}${state.heart==='connecting'?'正在连接所选心率带…':'重新连接所选心率带'}</button><button class="secondary full" data-action="disconnect" ${state.heart==='disconnected'?'disabled':''}>主动断开</button></div><p class="state-note">所选：${safe(device.name)}。自动重连可能无法恢复数据；设备可被发现时，仍可尝试上方手动重连。只有收到新数据后才显示实时 BPM。</p><label class="setting-row"><span>自动重连</span><input data-setting="autoReconnect" type="checkbox" ${state.autoReconnect?'checked':''}></label><label class="setting-row"><span>模拟接触良好</span><input data-setting="contact" type="checkbox" ${state.contact?'checked':''}></label>${chartCard(false)}<p class="state-note">设备与读数均为模拟，不申请蓝牙权限。</p>`;}
  function heartChartPage(){const historical=state.chartReturn==='detail';const r=selectedRow();return `<div class="detail-heading"><button class="icon-button" data-action="chart-back" aria-label="返回${historical?'运动详情':'心率设备'}">${icon('arrow')}</button><h2>心率详情</h2></div><p class="state-note">${historical?`${safe(r.name)} · ${safe(r.date)}`:'当前心率 · 模拟最近 60 秒'}</p><div class="card device-hero"><div class="hero-label">${historical?'全程平均心率':'实时心率'}</div><div class="bpm-number">${historical?(r.average||'--'):currentBpm()}<small>BPM</small></div></div><section class="card chart-card"><div class="chart-heading"><strong>心率 / BPM</strong><span>${historical?'活动时间 / 分钟':'时间 / 秒'}</span></div>${chart(historical)}<p class="state-note">线条缺口表示数据缺失，禁止用平滑连线伪造采样。暂停时间不加入运动活动时间轴。</p></section><div class="metric-grid">${metric(historical?(r.min||'--'):fresh()?139:'--','BPM','最低心率')}${metric(historical?(r.max||'--'):fresh()?157:'--','BPM','最高心率')}</div><p class="state-note">仅可视化有效采样，不做健康诊断或训练建议。</p>`;}
  function settingsPage(){return `${appHeader('设置','MADE FOR YOUR RUN')}<div class="app-section-label">运动显示</div><section class="card"><label class="setting-row"><span>距离单位</span><select data-setting="unit" aria-label="距离单位"><option value="km" ${state.unit==='km'?'selected':''}>公里 km</option><option value="mi" ${state.unit==='mi'?'selected':''}>英里 mi</option></select></label><label class="setting-row"><span>配速 / 速度偏好</span><select data-setting="rate" aria-label="配速速度偏好"><option value="pace" ${state.rate==='pace'?'selected':''}>配速</option><option value="speed" ${state.rate==='speed'?'selected':''}>速度</option></select></label><label class="setting-row"><span>心率带自动重连</span><input type="checkbox" data-setting="autoReconnect" ${state.autoReconnect?'checked':''}></label></section><p class="state-note">自动重连可能失败。心率页始终保留手动重新连接入口。</p><div class="app-section-label">隐私与离线</div><section class="card"><div class="row"><strong style="font-size:13px">只属于你的运动记录</strong>${icon('shield')}</div><p class="settings-note">原生 P0 的运动数据保存在设备本地，通过 GPX 主动导出。此原型只使用内存中的模拟数据，不采集位置或蓝牙信息。</p><p class="settings-note">网站不请求外部字体、图片、地图或 API。下载后可直接打开 index.html，完全离线体验。</p></section><div class="app-section-label">应用信息</div><section class="card"><img class="about-flower" src="assets/gardenia.svg" alt="栀子花标志"><strong>Runner Gardenia</strong><p class="settings-note">P0 UI 设计提案 · 当前方案 ${pad(themeIndex+1)}<br>参考基线 P0.1 · 不是正式 Android 应用<br>所有运动数据、设备和导出内容均为示例。</p></section>`;}
  const pageRenderers={run:runPage,gps:gpsPage,history:historyPage,detail:detailPage,heart:heartPage,'heart-chart':heartChartPage,settings:settingsPage};
  function renderNav(){const active=state.page==='detail'?'history':state.page==='heart-chart'?(state.chartReturn==='detail'?'history':'heart'):state.page==='gps'?'run':state.page;$('#app-nav').innerHTML=[['run','运动'],['history','历史'],['heart','心率'],['settings','设置']].map(([id,title])=>`<button data-page="${id}" ${active===id?'aria-current="page"':''}>${icon(id)}<span>${title}</span></button>`).join('');}
  function render({top=false,animate=false}={}){
    const scroll=content.scrollTop;
    const focus=document.activeElement;
    const restoreFocus=content.contains(focus);
    const focusKey=focus?.dataset?.action==='swap-metric'?`[data-action="swap-metric"][data-slot="${focus.dataset.slot}"]`:focus?.dataset?.action?`[data-action="${focus.dataset.action}"]`:focus?.dataset?.setting?`[data-setting="${focus.dataset.setting}"]`:focus?.dataset?.device?`[data-device="${focus.dataset.device}"]`:null;
    content.innerHTML=pageRenderers[state.page]();
    content.scrollTop=top?0:scroll;
    if(animate){content.classList.remove('screen-enter');void content.offsetWidth;content.classList.add('screen-enter');}
    renderNav();syncControls();renderModal();
    if(focusKey&&restoreFocus)$(focusKey,content)?.focus({preventScroll:true});
  }
  function navigate(page){if(!pageRenderers[page])return;stopForInteraction();state.page=page;state.modal=null;render({top:true,animate:true});content.focus({preventScroll:true});}
  function syncControls(){for(const kind of ['gps','heart','session']){$$(`#${kind}-control button`).forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.value===state[kind])));}$('#gps-help').textContent=state.gps==='fixed'?'有效模拟定位 · 精度 ± 5 m':state.gps==='searching'?'尚无有效位置 · 精度未确定':'停止更新轨迹 · 恢复后不补点';$('#empty-history').checked=state.emptyHistory;$('#export-failure').checked=state.exportFailure;}
  function toast(message){const el=$('#app-toast');el.textContent=message;el.classList.add('visible');clearTimeout(toast.timer);toast.timer=setTimeout(()=>el.classList.remove('visible'),3200);}
  function sampleGpx(){return `<?xml version="1.0" encoding="UTF-8"?>\n<gpx version="1.1" creator="Runner Gardenia UI Lab SIMULATION" xmlns="http://www.topografix.com/GPX/1/1" xmlns:gpxtpx="http://www.garmin.com/xmlschemas/TrackPointExtension/v1"><metadata><name>模拟数据 - 非真实运动记录</name><desc>Offline synthetic UI lab sample. Not a real workout.</desc></metadata><trk><name>SIMULATION Gardenia River Run</name><trkseg><trkpt lat="31.2300" lon="121.4700"><time>2026-11-05T23:12:00Z</time><extensions><gpxtpx:TrackPointExtension><gpxtpx:hr>139</gpxtpx:hr></gpxtpx:TrackPointExtension></extensions></trkpt><trkpt lat="31.2304" lon="121.4705"><time>2026-11-05T23:12:10Z</time></trkpt></trkseg><trkseg><trkpt lat="31.2310" lon="121.4710"><time>2026-11-05T23:13:00Z</time></trkpt></trkseg></trk></gpx>`;}
  let modalReturn=null;
  function openExport(){modalReturn=document.activeElement;state.modal='export';state.exportResult=null;renderModal();updatePlayback();}
  function closeModal(){state.modal=null;$('#modal-root').innerHTML='';content.removeAttribute('inert');$('#app-nav').removeAttribute('inert');modalReturn?.focus({preventScroll:true});updatePlayback();}
  function renderModal(){
    if(!state.modal){$('#modal-root').innerHTML='';content.removeAttribute('inert');$('#app-nav').removeAttribute('inert');return;}
    const r=selectedRow(),fileName=`Gardenia-SIMULATION-${r.date.replaceAll('.','-')}-${r.id}.gpx`;
    const result=state.exportResult;
    $('#modal-root').innerHTML=`<div class="sheet-backdrop"><section class="export-sheet" role="dialog" aria-modal="true" aria-labelledby="export-title" tabindex="-1"><div class="sheet-handle"></div><div class="row"><h2 id="export-title">${result?(result==='failed'?'导出失败':'导出成功'):'导出 GPX'}</h2><button class="icon-button" style="width:32px;height:32px;border-radius:50%" data-action="close-export" aria-label="关闭导出弹窗">×</button></div>${result?`<div class="export-result">${icon(result==='failed'?'alert':'check')}<p>${result==='failed'?'模拟文件保存失败。记录仍然保留，请关闭失败模拟后重试。':result==='shared'?'已完成系统分享。示例文件不包含真实运动数据。':'已生成示例文件并交给浏览器下载；实际保存位置由浏览器决定。'}</p></div><div class="sheet-buttons"><button class="primary full" data-action="${result==='failed'?'retry-export':'close-export'}">${result==='failed'?'重新尝试':'完成'}</button></div>`:`<p>保存或分享一个 GPX 示例。文件包含合成轨迹与示例心率；仅用于交互验收。</p><div class="export-file">${safe(fileName)}<small>GPX 1.1 · UTF-8 · 明确标记 SIMULATION</small></div><div class="sheet-buttons"><button class="primary full" data-action="save-gpx">${icon('export')}保存示例文件</button><button class="secondary full" data-action="share-gpx">保存 / 分享</button><button class="text-button" data-action="close-export">取消</button></div>`}</section></div>`;
    content.setAttribute('inert','');$('#app-nav').setAttribute('inert','');$('.export-sheet').focus({preventScroll:true});
  }
  async function exportGpx(share){
    if(state.exportFailure){state.exportResult='failed';renderModal();return;}
    const r=selectedRow(),name=`Gardenia-SIMULATION-${r.date.replaceAll('.','-')}-${r.id}.gpx`;
    try {
      const file=new File([sampleGpx()],name,{type:'application/gpx+xml'});
      if(share && navigator.canShare?.({files:[file]}) && navigator.share){await navigator.share({files:[file],title:'模拟 GPX 示例'});state.exportResult='shared';}
      else {const url=URL.createObjectURL(file);const a=document.createElement('a');a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);state.exportResult='saved';}
      renderModal();
    }catch(e){if(e.name==='AbortError'){toast('已取消分享，仍可保存示例文件。');return;}state.exportResult='failed';renderModal();}
  }
  function connectDevice(){connectionGeneration++;const token=connectionGeneration;state.heart='connecting';render();setTimeout(()=>{if(token!==connectionGeneration)return;state.heart='connected';state.contact=true;render();toast('已收到新的模拟心率数据。');},1000);}
  function setSimulation(kind,value){
    stopForInteraction();
    if(kind==='heart'){connectionGeneration++;scanGeneration++;}
    if(kind==='session'){
      if(value==='recording' && state.gps!=='fixed' && state.session==='idle'){toast('先等待有效定位，再开始有效记录。');syncControls();return;}
      if(value==='idle'){state.seconds=0;state.distance=0;}
    }
    if(kind==='gps'&&value!=='fixed'&&state.session==='recording')state.gpsGap=true;
    state[kind]=value;render();
  }
  function handleAction(action,button){
    stopForInteraction();
    switch(action){
      case 'swap-metric':swapMetric(Number(button.dataset.slot));break;
      case 'gps-page':navigate('gps');break;
      case 'start':if(canStart()){state.session='recording';state.seconds=0;state.distance=0;state.gpsGap=false;render();}break;
      case 'pause':state.session='paused';render();break;
      case 'resume':if(canStart()){state.session='recording';render();}break;
      case 'finish':{
        const r={id:Math.max(...rows.map(x=>x.id),0)+1,name:'刚刚结束 · 模拟跑步',date:'2026.11.07',time:'09:41',gap:state.gpsGap,distance:state.distance,seconds:state.seconds,average:fresh()?148:0,max:fresh()?157:0,min:fresh()?139:0};rows.unshift(r);state.emptyHistory=false;state.detail=r.id;state.session='idle';state.seconds=0;state.distance=0;navigate('detail');toast('模拟运动已结束，仅保存在本页内存。');break;}
      case 'scan':{connectionGeneration++;const token=++scanGeneration;state.heart='scanning';render();setTimeout(()=>{if(token!==scanGeneration)return;state.heart='available';render();},1400);break;}
      case 'reconnect':scanGeneration++;connectDevice();break;
      case 'disconnect':connectionGeneration++;scanGeneration++;state.heart='disconnected';render();break;
      case 'heart-chart':state.chartReturn=state.page==='detail'?'detail':'heart';navigate('heart-chart');break;
      case 'chart-back':navigate(state.chartReturn);break;
      case 'export':openExport();break;
      case 'close-export':closeModal();break;
      case 'retry-export':state.exportResult=null;renderModal();break;
      case 'save-gpx':void exportGpx(false);break;
      case 'share-gpx':void exportGpx(true);break;
    }
  }
  screen.addEventListener('click',e=>{const b=e.target.closest('button');if(!b||b.disabled)return;if(b.dataset.page)navigate(b.dataset.page);else if(b.dataset.detail){state.detail=Number(b.dataset.detail);navigate('detail');}else if(b.dataset.device!==undefined){state.selectedDevice=Number(b.dataset.device);connectDevice();}else if(b.dataset.action)handleAction(b.dataset.action,b);});
  screen.addEventListener('change',e=>{if(!e.target.dataset.setting)return;stopForInteraction();const key=e.target.dataset.setting;state[key]=e.target.type==='checkbox'?e.target.checked:e.target.value;render();});
  const reducedMotion=matchMedia('(prefers-reduced-motion: reduce)');
  let autoplay=!reducedMotion.matches, userPaused=false, hoverPaused=false, focusPaused=false, dragging=false;
  let autoTimer=null;
  function updatePlayback(){
    const paused=!autoplay||userPaused||hoverPaused||focusPaused||dragging||document.hidden||Boolean(state.modal);
    $('#autoplay').textContent=autoplay&&!userPaused?'Ⅱ':'▷';$('#autoplay').setAttribute('aria-label',autoplay&&!userPaused?'暂停自动轮播':'开始自动轮播');$('#autoplay').setAttribute('aria-pressed',String(autoplay&&!userPaused));
    $('#playback-status').textContent=paused?(reducedMotion.matches&&!autoplay?'已减少动态效果':'自动轮播已暂停'):'自动轮播 · 8 秒';
    clearTimeout(autoTimer);if(!paused)autoTimer=setTimeout(()=>{selectTheme(themeIndex+1,false);updatePlayback();},8000);
  }
  function stopForInteraction(){userPaused=true;updatePlayback();}
  function selectTheme(index,manual=true){
    if(manual)stopForInteraction();themeIndex=(index+themes.length)%themes.length;
    const t=themes[themeIndex];screen.dataset.theme=t.id;
    $('#concept-number').textContent=pad(themeIndex+1);$('#collection-count').textContent=`${pad(themeIndex+1)} / 10`;
    $('#concept-title').innerHTML=t.name.split(' ').map(safe).join(' <br>');$('#concept-cn').textContent=t.cn;$('#concept-description').textContent=t.description;
    $('#concept-language').textContent=t.language;$('#native-note').textContent=t.native;$('#swatches').innerHTML=t.colors.map(c=>`<span style="background:${c}" title="${c}"></span>`).join('');
    $$('.theme-card').forEach((b,i)=>b.setAttribute('aria-pressed',String(i===themeIndex)));$$('#pagination button').forEach((b,i)=>b.setAttribute('aria-current',String(i===themeIndex)));
    {const card=$$('.theme-card')[themeIndex];const track=$('#theme-track');const target=card.offsetLeft-track.offsetLeft-track.clientWidth/2+card.clientWidth/2;track.scrollTo({left:target,behavior:reducedMotion.matches?'auto':'smooth'});}
    updateFavorite();render({animate:true});
  }
  function updateFavorite(){const saved=favoriteIds.includes(themes[themeIndex].id);$('#favorite').setAttribute('aria-pressed',String(saved));$('#favorite').innerHTML=`<span aria-hidden="true">${saved?'★':'☆'}</span> ${saved?'已收藏方案':'收藏当前方案'}`;}
  $('#favorite').addEventListener('click',()=>{stopForInteraction();const id=themes[themeIndex].id;favoriteIds=favoriteIds.includes(id)?favoriteIds.filter(x=>x!==id):[...favoriteIds,id];try{localStorage.setItem('gardenia-favorites',JSON.stringify(favoriteIds));}catch{/* Private mode still allows this session's selection. */}updateFavorite();});
  for(const [kind,options] of Object.entries({gps:[['searching','搜星中'],['fixed','已定位'],['lost','信号丢失']],heart:[['disconnected','未连接'],['scanning','扫描中'],['connecting','连接中'],['connected','已连接'],['reconnecting','重连中'],['stale','数据过期']],session:[['idle','空闲'],['recording','记录中'],['paused','暂停']]})){
    $(`#${kind}-control`).innerHTML=options.map(([value,label])=>`<button type="button" data-value="${value}" aria-pressed="false">${label}</button>`).join('');
    $(`#${kind}-control`).addEventListener('click',e=>{const b=e.target.closest('button');if(b&&!b.disabled)setSimulation(kind,b.dataset.value);});
  }
  $('#empty-history').addEventListener('change',e=>{state.emptyHistory=e.target.checked;stopForInteraction();render();});
  $('#export-failure').addEventListener('change',e=>{state.exportFailure=e.target.checked;stopForInteraction();});
  $('#reset-demo').addEventListener('click',()=>{stopForInteraction();connectionGeneration++;scanGeneration++;state=initial();rows=defaultRows.map(x=>({...x}));Object.values(metricLayoutsByTheme).forEach(layout=>{layout.notice='';});$('#metric-announcement').textContent='';render({top:true});toast('已恢复默认模拟记录。');});
  $('#theme-track').innerHTML=themes.map((t,i)=>`<button type="button" class="theme-card" data-theme-index="${i}" aria-pressed="${i===0}" aria-label="方案 ${pad(i+1)} ${safe(t.name)} ${safe(t.cn)}"><div class="thumb" data-theme="${t.id}"><div class="thumb-fallback">${pad(i+1)}</div></div><span class="selected-check" aria-hidden="true">✓</span><span class="card-label"><span class="card-number">${pad(i+1)}</span>${safe(t.name)}</span><span class="card-cn">${safe(t.cn)}</span></button>`).join('');
  // Thumbnails are local screenshots captured from this exact running prototype.
  // File URLs can load PNGs without fetch; a missing preview keeps the CSS fallback.
  if(document.documentElement.dataset.previews==='ready') $$('.theme-card .thumb').forEach((el,i)=>{const img=new Image();img.alt=`${themes[i].name} 实际浏览器截图`;img.draggable=false;img.onload=()=>el.replaceChildren(img);img.src=`previews/theme-${pad(i+1)}.png`;});
  $('#pagination').innerHTML=themes.map((_,i)=>`<button type="button" data-theme-index="${i}" aria-label="选择方案 ${i+1}" aria-current="${i===0}"></button>`).join('');
  let dragStart=null,wasDrag=false;
  const track=$('#theme-track');
  track.addEventListener('pointerdown',e=>{dragStart={x:e.clientX,y:e.clientY,scroll:track.scrollLeft,id:e.pointerId};wasDrag=false;dragging=true;stopForInteraction();});
  track.addEventListener('pointermove',e=>{if(!dragStart||e.pointerType==='touch')return;const dx=e.clientX-dragStart.x;if(Math.abs(dx)>6){wasDrag=true;track.scrollLeft=dragStart.scroll-dx;track.setPointerCapture(e.pointerId);}});
  function endDrag(e){if(dragStart&&e.pointerType==='touch'){const dx=e.clientX-dragStart.x,dy=e.clientY-dragStart.y;if(Math.abs(dx)>55&&Math.abs(dx)>Math.abs(dy)){wasDrag=true;selectTheme(themeIndex+(dx<0?1:-1));}}dragStart=null;dragging=false;updatePlayback();}
  track.addEventListener('pointerup',endDrag);track.addEventListener('pointercancel',()=>{dragStart=null;dragging=false;updatePlayback();});
  track.addEventListener('click',e=>{if(wasDrag){wasDrag=false;return;}const b=e.target.closest('[data-theme-index]');if(b)selectTheme(Number(b.dataset.themeIndex));});
  $('#pagination').addEventListener('click',e=>{const b=e.target.closest('button');if(b)selectTheme(Number(b.dataset.themeIndex));});
  $('#previous').addEventListener('click',()=>selectTheme(themeIndex-1));$('#next').addEventListener('click',()=>selectTheme(themeIndex+1));
  $('#autoplay').addEventListener('click',()=>{if(autoplay&&!userPaused){autoplay=false;userPaused=true;}else{autoplay=true;userPaused=false;}updatePlayback();});
  const pauseRegion=$('.studio');
  for(const el of [pauseRegion,$('#carousel-section')]){el.addEventListener('pointerenter',()=>{hoverPaused=true;updatePlayback();});el.addEventListener('pointerleave',()=>{hoverPaused=false;updatePlayback();});el.addEventListener('focusin',()=>{focusPaused=true;updatePlayback();});el.addEventListener('focusout',()=>{setTimeout(()=>{focusPaused=Boolean(document.activeElement.closest('.studio, #carousel-section'));updatePlayback();},0);});}
  document.addEventListener('visibilitychange',updatePlayback);
  reducedMotion.addEventListener('change',()=>{if(reducedMotion.matches){autoplay=false;updatePlayback();}});
  document.addEventListener('keydown',e=>{
    if(state.modal){if(e.key==='Escape'){e.preventDefault();closeModal();}if(e.key==='Tab'){const focusable=$$('.export-sheet button:not([disabled])');const first=focusable[0],last=focusable.at(-1);if(e.shiftKey&&(document.activeElement===first||document.activeElement===$('.export-sheet'))){e.preventDefault();last.focus();}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first.focus();}}return;}
    if(e.altKey||e.ctrlKey||e.metaKey||/INPUT|SELECT|TEXTAREA/.test(e.target.tagName))return;
    if(e.key==='ArrowLeft'||e.key==='ArrowRight'){e.preventDefault();selectTheme(themeIndex+(e.key==='ArrowRight'?1:-1));}
  });
  function resizePhone(){const mobile=innerWidth<=760;const heightLimit=mobile?Math.max(480,Math.min(650,innerHeight-205)):Math.max(350,innerHeight-355);const widthLimit=mobile?innerWidth-56:$('.phone-stage').clientWidth-25;const scale=Math.min(1,heightLimit/893,widthLimit/413);$('#phone-size').style.setProperty('--phone-scale',scale.toFixed(4));}
  addEventListener('resize',resizePhone);resizePhone();selectTheme(0,false);updatePlayback();
  // Simulation advances only in foreground, with an active record and a valid GPS fix.
  // No catch-up integration occurs after missing GPS or a hidden tab.
  setInterval(()=>{if(document.hidden||state.modal||state.session!=='recording')return;state.seconds++;if(state.gps==='fixed')state.distance+=5.24/1714;if(state.page==='run'){
    updateMotionMetrics();
  }},1000);
})();
