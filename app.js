// ═══════════════════════════════════════════════════════
//  CONFIG
// ═══════════════════════════════════════════════════════
const urlParams = new URLSearchParams(window.location.search);
const apiOverride = urlParams.get('api');
const API = apiOverride || 'http://localhost:4567';

// ═══════════════════════════════════════════════════════
//  API HELPER
// ═══════════════════════════════════════════════════════
async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' },
    ...(body ? { body: JSON.stringify(body) } : {}) };
  const res = await fetch(API + path, opts);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Request failed');
  return data;
}

// ═══════════════════════════════════════════════════════
//  SERVER HEALTH CHECK
// ═══════════════════════════════════════════════════════
async function checkServer() {
  const banner = document.getElementById('serverBanner');
  const msg = document.getElementById('serverMsg');
  try {
    await fetch(API + '/api/results', { method: 'GET' });
    banner.className = 'server-banner server-ok';
    msg.textContent = '✅ Server connected — Maharashtra Edition';
  } catch {
    banner.className = 'server-banner server-err';
    msg.textContent = '❌ Server offline — start the Java backend first';
  }
}
checkServer(); setInterval(checkServer, 5000);

// ═══════════════════════════════════════════════════════
//  STATE
// ═══════════════════════════════════════════════════════
let currentVoter = null, voterData = null, selectedCand = null;
let currentElection = null, currentConstituency = null;
let constituenciesCache = [], resultConstituenciesCache = [];
let pieInst = null, barInst = null, adminPieInst = null, adminBarInst = null;
let divisionsData = null, electionsCache = [];

// ── Empty state helper ──────────────────────────────────────────────────
function emptyState(icon, title, text, actionLabel, actionFn) {
  return `
    <div class="empty-state">
      <div class="empty-icon">${icon || 'ℹ️'}</div>
      <div class="empty-title">${title || 'No data yet'}</div>
      <div class="empty-text">${text || ''}</div>
      ${actionLabel ? `<button class="btn btn-primary btn-inline" onclick="${actionFn}">${actionLabel}</button>` : ''}
    </div>`;
}

// Map toggle (VS / LS)
function setMapLayer(type) {
  const vsMap = document.getElementById('map-vs-container');
  const lsMap = document.getElementById('map-ls-container');
  const vsBtn = document.querySelector('.map-badge.vs');
  const lsBtn = document.querySelector('.map-badge.ls');
  if (!vsMap || !lsMap) return;
  const isVS = type === 'vs';
  vsMap.classList.toggle('active', isVS);
  lsMap.classList.toggle('active', !isVS);
  if (vsBtn) vsBtn.classList.toggle('active', isVS);
  if (lsBtn) lsBtn.classList.toggle('active', !isVS);
}

// District center positions mapped to the image coordinates
// Image is 600x580, positioned at x=100, y=20 in SVG (800x650 viewBox)
// These coordinates are approximate centers of each district on the map image

// Division colors matching the legend
const DIVISION_COLORS = {
  'Konkan': '#FF9933',
  'Pune': '#00BFFF',
  'Nashik': '#9C27B0',
  'Chh. Sambhajinagar': '#FF5722',
  'Amravati': '#4CAF50',
  'Nagpur': '#FFD700'
};

const DISTRICT_POSITIONS = {
  // Konkan (brown, western coastal)
  'Palghar': { x: 145, y: 100 },
  'Thane': { x: 165, y: 160 },
  'Mumbai Suburban': { x: 145, y: 200 },
  'Mumbai City': { x: 135, y: 225 },
  'Raigad': { x: 145, y: 270 },
  'Ratnagiri': { x: 125, y: 350 },
  'Sindhudurg': { x: 115, y: 440 },
  
  // Nashik division (yellow, northern)
  'Nandurbar': { x: 220, y: 60 },
  'Dhule': { x: 270, y: 85 },
  'Jalgaon': { x: 330, y: 80 },
  'Nashik': { x: 250, y: 145 },
  'Ahmednagar': { x: 310, y: 200 },
  
  // Pune division (green, southwest)
  'Pune': { x: 240, y: 280 },
  'Satara': { x: 215, y: 360 },
  'Sangli': { x: 195, y: 430 },
  'Kolhapur': { x: 165, y: 500 },
  'Solapur': { x: 310, y: 400 },
  
  // Chh. Sambhajinagar division (blue, Marathwada)
  'Chhatrapati Sambhajinagar': { x: 340, y: 170 },
  'Jalna': { x: 350, y: 220 },
  'Beed': { x: 360, y: 270 },
  'Dharashiv': { x: 320, y: 330 },
  'Latur': { x: 360, y: 340 },
  'Nanded': { x: 400, y: 280 },
  'Parbhani': { x: 390, y: 230 },
  'Hingoli': { x: 410, y: 185 },
  
  // Amravati division (pink, Vidarbha west)
  'Buldhana': { x: 410, y: 110 },
  'Akola': { x: 440, y: 155 },
  'Amravati': { x: 480, y: 130 },
  'Washim': { x: 465, y: 210 },
  'Yavatmal': { x: 500, y: 260 },
  
  // Nagpur division (orange, Vidarbha east)
  'Wardha': { x: 520, y: 170 },
  'Nagpur': { x: 545, y: 230 },
  'Gondia': { x: 575, y: 175 },
  'Bhandara': { x: 595, y: 235 },
  'Chandrapur': { x: 540, y: 310 },
  'Gadchiroli': { x: 580, y: 340 }
};

// Render constituency markers on the map image
function renderConstituencyMarkers() {
  if (!constituenciesCache.length) return;

  // VS constituencies
  const vsGroup = document.getElementById('vs-constituencies');
  if (vsGroup) {
    vsGroup.innerHTML = '';
    const vsConsts = constituenciesCache.filter(c => c.type === 'VIDHAN_SABHA');
    vsConsts.forEach(c => {
      const center = DISTRICT_POSITIONS[c.district];
      if (!center) return;
      
      // Add slight random offset so multiple constituencies in same district don't overlap
      const offsetX = (Math.random() - 0.5) * 30;
      const offsetY = (Math.random() - 0.5) * 30;
      
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', center.x + offsetX);
      circle.setAttribute('cy', center.y + offsetY);
      circle.setAttribute('r', '4');
      circle.setAttribute('fill', DIVISION_COLORS[c.division] || '#888');
      circle.setAttribute('opacity', '0.8');
      circle.setAttribute('data-const-id', c.constituencyId);
      circle.setAttribute('data-name', c.name);
      circle.style.cursor = 'pointer';
      circle.addEventListener('mouseenter', (e) => showConstituencyTooltip(e, c, center.x + offsetX, center.y + offsetY));
      circle.addEventListener('mouseleave', hideTooltip);
      vsGroup.appendChild(circle);
    });
  }

  // LS constituencies (larger markers with labels)
  const lsGroup = document.getElementById('ls-constituencies');
  if (lsGroup) {
    lsGroup.innerHTML = '';
    const lsConsts = constituenciesCache.filter(c => c.type === 'LOK_SABHA');
    lsConsts.forEach(c => {
      const center = DISTRICT_POSITIONS[c.district];
      if (!center) return;
      
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', center.x);
      circle.setAttribute('cy', center.y);
      circle.setAttribute('r', '8');
      circle.setAttribute('fill', DIVISION_COLORS[c.division] || '#888');
      circle.setAttribute('opacity', '0.85');
      circle.setAttribute('data-const-id', c.constituencyId);
      circle.setAttribute('data-name', c.name);
      circle.style.cursor = 'pointer';
      circle.addEventListener('mouseenter', (e) => showConstituencyTooltip(e, c, center.x, center.y));
      circle.addEventListener('mouseleave', hideTooltip);
      lsGroup.appendChild(circle);
      
      // Label for LS
      const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      text.setAttribute('x', center.x);
      text.setAttribute('y', center.y - 14);
      text.setAttribute('fill', '#fff');
      text.setAttribute('font-size', '8');
      text.setAttribute('text-anchor', 'middle');
      text.setAttribute('font-family', 'DM Sans, sans-serif');
      text.setAttribute('opacity', '0.9');
      text.setAttribute('pointer-events', 'none');
      text.textContent = c.name.length > 14 ? c.name.substring(0, 12) + '…' : c.name;
      lsGroup.appendChild(text);
    });
  }
}

// Render the interactive map
function renderInteractiveMap() {
  renderConstituencyMarkers();
}

function showConstituencyTooltip(event, constituency, cx, cy) {
  const tooltip = document.getElementById('map-tooltip');
  const content = document.getElementById('tooltip-content');
  if (!tooltip || !content) return;

  content.innerHTML = `
    <div class="tt-name">${constituency.name}</div>
    <div class="tt-detail">No. ${constituency.number} · ${constituency.type === 'VIDHAN_SABHA' ? 'Vidhan Sabha' : 'Lok Sabha'}</div>
    <div class="tt-detail">District: ${constituency.district}</div>
    <div class="tt-detail">Reservation: ${constituency.reservationCategory}</div>
    <div class="tt-division">${constituency.division} Division</div>
  `;

  tooltip.style.display = 'block';
  // Position tooltip relative to the circle position in SVG coordinates
  // SVG is 800x650, image is 600x580 at offset 100,20
  const mapContainer = document.querySelector('.mh-svg-map.active');
  if (!mapContainer) return;
  
  const svg = mapContainer.querySelector('svg');
  const rect = svg.getBoundingClientRect();
  
  // Convert SVG coordinates to pixel coordinates
  const svgWidth = 800;
  const svgHeight = 650;
  const scaleX = rect.width / svgWidth;
  const scaleY = rect.height / svgHeight;
  
  let left = cx * scaleX + 15;
  let top = cy * scaleY - 10;
  
  // Keep tooltip within bounds
  if (left + 220 > rect.width) left = left - 240;
  if (top < 0) top = 10;
  
  tooltip.style.left = left + 'px';
  tooltip.style.top = top + 'px';
}

function hideTooltip() {
  const tooltip = document.getElementById('map-tooltip');
  if (tooltip) tooltip.style.display = 'none';
}

const PARTY_COLORS = {
  'BJP':'#FF9933','INC':'#00BFFF','SHS':'#FF6600','SSUBT':'#F57C00',
  'NCP':'#004D40','NCPSP':'#1B5E20','BSP':'#2196F3','MNS':'#FF5722',
  'VBA':'#9C27B0','IND':'#9E9E9E','NOTA':'#F44336'
};
const COLORS = Object.values(PARTY_COLORS);

const DIVISION_DISTRICTS = {
  'Konkan':['Mumbai City','Mumbai Suburban','Thane','Palghar','Raigad','Ratnagiri','Sindhudurg'],
  'Pune':['Pune','Satara','Sangli','Solapur','Kolhapur'],
  'Nashik':['Nashik','Dhule','Nandurbar','Jalgaon','Ahmednagar'],
  'Chh. Sambhajinagar':['Chh. Sambhajinagar','Jalna','Beed','Dharashiv','Latur','Nanded','Parbhani','Hingoli'],
  'Amravati':['Amravati','Akola','Yavatmal','Buldhana','Washim'],
  'Nagpur':['Nagpur','Wardha','Bhandara','Gondia','Chandrapur','Gadchiroli']
};

// ═══════════════════════════════════════════════════════
//  ANIMATED COUNTER
// ═══════════════════════════════════════════════════════
function animateCounter(el, target, duration = 1500) {
  const start = parseInt(el.textContent) || 0;
  const diff = target - start;
  if (diff === 0) return;
  const startTime = performance.now();
  function step(now) {
    const elapsed = now - startTime;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.round(start + diff * eased);
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

// Load home stats on page load
async function loadHomeStats() {
  try {
    const data = await api('GET', '/api/stats');
    animateCounter(document.getElementById('hs-vs'), data.vsConstituencies || 288);
    animateCounter(document.getElementById('hs-ls'), data.lsConstituencies || 48);
    animateCounter(document.getElementById('hs-voters'), data.totalVoters || 0);
    animateCounter(document.getElementById('hs-blocks'), data.totalBlocks || 0);
  } catch {
    animateCounter(document.getElementById('hs-vs'), 288);
    animateCounter(document.getElementById('hs-ls'), 48);
  }
}
setTimeout(loadHomeStats, 500);
// Try to load map data on page load if server is available
setTimeout(async () => {
  try {
    await loadAllConstituenciesForMap();
  } catch (e) {
    // Server may not be running, map will load when user seeds data
  }
}, 1000);

// ═══════════════════════════════════════════════════════
//  CONFETTI
// ═══════════════════════════════════════════════════════
function launchConfetti() {
  const canvas = document.getElementById('confettiCanvas');
  const ctx = canvas.getContext('2d');
  canvas.width = window.innerWidth;
  canvas.height = window.innerHeight;
  const particles = [];
  const colors = ['#FF9933','#138808','#003087','#FFD700','#FF5252','#00E676','#fff'];
  for (let i = 0; i < 150; i++) {
    particles.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height - canvas.height,
      w: Math.random() * 10 + 5, h: Math.random() * 6 + 3,
      color: colors[Math.floor(Math.random() * colors.length)],
      vx: (Math.random() - 0.5) * 4,
      vy: Math.random() * 3 + 2,
      rot: Math.random() * 360,
      rotSpeed: (Math.random() - 0.5) * 10,
      opacity: 1
    });
  }
  let frame = 0;
  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    let alive = false;
    particles.forEach(p => {
      p.x += p.vx; p.y += p.vy; p.rot += p.rotSpeed;
      p.vy += 0.05; p.opacity -= 0.003;
      if (p.opacity <= 0) return;
      alive = true;
      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.rot * Math.PI / 180);
      ctx.globalAlpha = p.opacity;
      ctx.fillStyle = p.color;
      ctx.fillRect(-p.w/2, -p.h/2, p.w, p.h);
      ctx.restore();
    });
    frame++;
    if (alive && frame < 300) requestAnimationFrame(draw);
    else ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
  requestAnimationFrame(draw);
}

// ═══════════════════════════════════════════════════════
//  SCREEN ROUTING
// ═══════════════════════════════════════════════════════
function show(id) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  window.scrollTo(0, 0);
}
function updateNav(label) {
  const r = document.getElementById('navRight');
  if (label) { r.style.display = 'flex'; document.getElementById('navPill').textContent = label; }
  else r.style.display = 'none';
}
function logout() {
  currentVoter = null; voterData = null; selectedCand = null;
  currentElection = null; currentConstituency = null;
  updateNav(null); show('s-home'); loadHomeStats();
}
function showErr(id, msg) {
  const el = document.getElementById(id);
  el.textContent = msg; el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 5000);
}
function showOverlay(t) { document.getElementById('overlayText').textContent = t; document.getElementById('overlay').classList.add('show'); }
function hideOverlay() { document.getElementById('overlay').classList.remove('show'); }

// ═══════════════════════════════════════════════════════
//  VOTER LOGIN
// ═══════════════════════════════════════════════════════
async function voterLogin() {
  const voterId = document.getElementById('vl-id').value.trim().toUpperCase();
  const password = document.getElementById('vl-pw').value;
  if (!voterId || !password) return showErr('vl-err', 'Enter Voter ID and Password');
  showOverlay('Authenticating...');
  try {
    const data = await api('POST', '/api/voter/login', { voterId, password });
    currentVoter = data.voterId; voterData = data;
    updateNav('🗳️ ' + data.voterId + ' · ' + (data.district || ''));
    await loadVoterDash(); show('s-voter-dash');
  } catch (e) { showErr('vl-err', e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  VOTER DASHBOARD
// ═══════════════════════════════════════════════════════
async function loadVoterDash() {
  document.getElementById('vd-welcome').textContent = 'Welcome, ' + (voterData.name || currentVoter) + '!';
  document.getElementById('vd-id').textContent = currentVoter;
  document.getElementById('vd-district').textContent = voterData.district || '';
  document.getElementById('vd-division').textContent = voterData.division || '';
  try {
    const data = await api('GET', '/api/voter/elections?voterId=' + currentVoter);
    const el = document.getElementById('vd-elections');
    const warn = document.getElementById('vd-warn');
    warn.classList.remove('show'); warn.textContent = '';
    if (!data.elections || data.elections.length === 0) {
      el.innerHTML = '<div style="color:var(--muted);padding:20px;text-align:center">No active elections. Ask admin to seed Maharashtra data first.</div>';
      return;
    }
    const hasMissing = data.elections.some(e => !e.constituencyId);
    if (hasMissing) {
      warn.textContent = '⚠️ Your voter profile has no constituency assigned. Please use MH001–MH030 demo voters or ask admin to register you with constituency details.';
      warn.classList.add('show');
    }
    el.innerHTML = data.elections.map(e => `
      <div class="election-card ${e.hasVoted ? '' : (e.constituencyId ? 'active-election' : 'disabled-election')}"
           onclick="${e.hasVoted ? `loadResults('${e.electionId}','voter')` : (e.constituencyId ? `startVoting('${e.electionId}','${e.constituencyId}','${e.type}')` : `missingConstituency('${e.type}')`)}">
        <img class="election-visual" src="${e.type === 'VIDHAN_SABHA' ? 'https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?auto=format&fit=crop&w=900&q=80' : 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80'}" alt="${e.type} election visual"/>
        <h3>${e.title}</h3>
        <p>${e.type === 'VIDHAN_SABHA' ? 'MLA — Vidhan Sabha' : 'MP — Lok Sabha'} · Constituency: <strong style="color:${e.constituencyId ? 'var(--saffron)' : 'var(--muted)'}">${e.constituencyId || 'Not Assigned'}</strong></p>
        <span class="status-badge ${e.hasVoted ? 'badge-completed' : (e.constituencyId ? 'badge-active' : 'badge-upcoming')}" style="margin-top:8px">
          ${e.hasVoted ? '✅ Voted' : (e.constituencyId ? '🗳️ Vote Now' : '⚠️ Action Required')}
        </span>
      </div>
    `).join('');
  } catch (e) {
    document.getElementById('vd-elections').innerHTML = '<div class="alert alert-error show">' + e.message + '</div>';
  }
}

function missingConstituency(type) {
  const label = type === 'VIDHAN_SABHA' ? 'Vidhan Sabha' : 'Lok Sabha';
  alert(`Constituency not assigned for ${label}. Use MH001–MH030 demo voters or ask admin to register your constituency.`);
}

// ═══════════════════════════════════════════════════════
//  VOTING
// ═══════════════════════════════════════════════════════
async function startVoting(electionId, constituencyId, type) {
  currentElection = electionId; currentConstituency = constituencyId; selectedCand = null;
  showOverlay('Loading ballot for ' + constituencyId + '...');
  try {
    const data = await api('GET', '/api/constituencies/' + constituencyId);
    document.getElementById('vc-title').textContent = 'Cast Your Vote — ' + data.name;
    document.getElementById('vc-sub').textContent =
      `${type === 'VIDHAN_SABHA' ? 'Vidhan Sabha (MLA)' : 'Lok Sabha (MP)'} · #${data.number} · ${data.district}, ${data.division} · ${data.reservationCategory !== 'GENERAL' ? '(' + data.reservationCategory + ' Reserved)' : 'General'}`;
    document.getElementById('vc-const-label').textContent = data.candidates.length + ' Candidates';
    const list = document.getElementById('vc-cand-list');
    list.innerHTML = data.candidates.map((c, i) => `
      <div class="cand-card" id="cc-${i}" onclick="selectCandidate(${i},'${c.candidateId}','${c.name.replace(/'/g,"\\'")}','${c.party}')">
        <span class="cand-symbol">${c.symbol}</span>
        <div class="cand-info">
          <div class="cand-name">${c.name}</div>
          <div class="cand-party" style="color:${c.color}">${c.partyFull} (${c.party})</div>
        </div>
        <div class="cand-radio"><div class="cand-dot"></div></div>
      </div>
    `).join('');
    show('s-vote-cast');
  } catch (e) { alert('Error: ' + e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  ADMIN DASHBOARD
// ═══════════════════════════════════════════════════════
async function loadAdminDashboard() {
  showOverlay('Loading admin dashboard...');
  try {
    const [chainData, voterData, resultsData, electionsData, statsData] = await Promise.all([
      api('GET', '/api/admin/blockchain'),
      api('GET', '/api/admin/voters'),
      api('GET', '/api/results'),
      api('GET', '/api/elections'),
      api('GET', '/api/stats').catch(() => null),
    ]);
    electionsCache = electionsData.elections || [];

    // Badge
    const badge = document.getElementById('chain-badge');
    badge.className = 'status-badge ' + (chainData.isValid ? 'badge-secure' : 'badge-tampered');
    badge.textContent = chainData.isValid ? '🔒 Blockchain Secure' : '⚠️ Tampered';

    const tv = resultsData.totalVotes;
    const vc = voterData.voters.length;
    const ec = electionsCache.length;
    document.getElementById('admin-sub').textContent = chainData.totalBlocks + ' blocks · ' + tv + ' votes · ' + vc + ' voters · ' + ec + ' elections';

    // Admin alerts
    const alerts = [];
    if (ec === 0) {
      alerts.push({
        icon: '🌱',
        title: 'No elections seeded',
        text: 'Seed Maharashtra data to load elections, constituencies, and demo voters.',
        action: 'seedMaharashtra()',
        label: 'Seed Maharashtra Data'
      });
    }
    if (chainData.totalBlocks <= 1) {
      alerts.push({
        icon: '⛓️',
        title: 'Blockchain has genesis only',
        text: 'Cast votes to create blocks and populate results.',
        action: '',
        label: ''
      });
    }
    if (vc === 0) {
      alerts.push({
        icon: '👥',
        title: 'No registered voters',
        text: 'Register voters or seed demo voters for testing.',
        action: '',
        label: ''
      });
    }
    document.getElementById('admin-alerts').innerHTML = alerts.map(a => `
      <div class="alert-card">
        <div class="alert-icon">${a.icon}</div>
        <div>
          <div class="alert-title">${a.title}</div>
          <div class="alert-text">${a.text}</div>
          ${a.label ? `<div class="alert-actions"><button class="btn btn-primary btn-inline" onclick="${a.action}">${a.label}</button></div>` : ''}
        </div>
      </div>
    `).join('');

    const turnout = vc > 0 ? Math.round(tv / vc * 100) : 0;
    document.getElementById('admin-stats').innerHTML =
      [{ n: tv, l: 'Total Votes' }, { n: vc, l: 'Registered Voters' }, { n: chainData.totalBlocks, l: 'Blocks Mined' }, { n: turnout + '%', l: 'Voter Turnout' }]
        .map(s => `<div class="stat-box"><div class="stat-num">${s.n}</div><div class="stat-lbl">${s.l}</div></div>`).join('');

    // Division stats
    const divGrid = document.getElementById('division-stats-grid');
    if (statsData && statsData.byDivision && Object.keys(statsData.byDivision).length) {
      divGrid.innerHTML = Object.entries(statsData.byDivision).map(([div, d]) => {
        const pct = d.total > 0 ? Math.round(d.voted / d.total * 100) : 0;
        const label = div === 'Unknown' ? 'Unassigned' : div;
        return `<div class="division-card">
          <h4>${label}</h4>
          <p>${d.total} voters · ${d.voted} voted</p>
          <div class="div-bar"><div class="div-bar-inner" style="width:${pct}%"></div></div>
          <p style="margin-top:4px;font-size:.72rem;color:var(--saffron)">${pct}% turnout</p>
        </div>`;
      }).join('');
    } else {
      divGrid.innerHTML = emptyState('📍', 'No division data yet', 'Seed Maharashtra data to see division‑wise turnout.', 'Seed Maharashtra Data', 'seedMaharashtra()');
    }

    // Charts
    const entries = Object.entries(resultsData.results || {});
    if (adminPieInst) adminPieInst.destroy();
    if (adminBarInst) adminBarInst.destroy();
    const chartsGrid = document.querySelector('#tab-overview .charts-grid');
    if (entries.length === 0) {
      chartsGrid.innerHTML = `
        <div class="chart-box" style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:220px">
          ${emptyState('🗳️', 'No votes yet', 'Cast votes to populate analytics.', '', '')}
        </div>
        <div class="chart-box" style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:220px">
          ${emptyState('📊', 'No data yet', 'Seed elections and cast votes to see charts.', 'Seed Maharashtra Data', 'seedMaharashtra()')}
        </div>`;
    } else {
      if (!document.getElementById('adminPie')) {
        chartsGrid.innerHTML = `<div class="chart-box"><div class="chart-title">Vote Distribution</div><canvas id="adminPie" height="220"></canvas></div>
          <div class="chart-box"><div class="chart-title">Comparison</div><canvas id="adminBar" height="220"></canvas></div>`;
      }
      const labels = entries.map(([k]) => k.length > 15 ? k.substring(0, 15) + '..' : k);
      const vals = entries.map(([, v]) => v);
      Chart.defaults.color = 'rgba(238,238,245,0.6)';
      adminPieInst = new Chart(document.getElementById('adminPie').getContext('2d'), {
        type: 'doughnut', data: { labels, datasets: [{ data: vals, backgroundColor: COLORS.slice(0, vals.length), borderColor: 'rgba(7,7,15,0.8)', borderWidth: 3 }] },
        options: { plugins: { legend: { position: 'bottom', labels: { padding: 10, font: { size: 10 } } } }, cutout: '55%' }
      });
      adminBarInst = new Chart(document.getElementById('adminBar').getContext('2d'), {
        type: 'bar', data: { labels, datasets: [{ label: 'Votes', data: vals, backgroundColor: COLORS.slice(0, vals.length), borderRadius: 6, borderSkipped: false }] },
        options: { plugins: { legend: { display: false } }, scales: { x: { grid: { color: 'rgba(255,255,255,0.06)' } }, y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.06)' }, ticks: { stepSize: 1 } } } }
      });
    }

    // Elections tab — with START/STOP buttons
    if (electionsCache.length) {
      document.getElementById('elections-list').innerHTML = electionsCache.map(e => `
        <div class="election-card">
          <img class="election-visual" src="${e.type === 'VIDHAN_SABHA' ? 'https://images.unsplash.com/photo-1540910419892-4a36d2c3266c?auto=format&fit=crop&w=900&q=80' : 'https://images.unsplash.com/photo-1469474968028-56623f02e42e?auto=format&fit=crop&w=900&q=80'}" alt="${e.type} election visual"/>
          <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap">
            <div><h3>${e.title}</h3><p>${e.type} · ${e.totalSeats} seats · ${e.startDate} to ${e.endDate}</p></div>
            <span class="status-badge badge-${e.status.toLowerCase()}">${e.status}</span>
          </div>
          <div class="election-controls">
            ${e.status === 'UPCOMING' ? `<button class="btn btn-green btn-inline" onclick="changeElectionStatus('${e.electionId}','ACTIVE')">▶️ Start Election</button>` : ''}
            ${e.status === 'ACTIVE' ? `<button class="btn btn-danger btn-inline" onclick="changeElectionStatus('${e.electionId}','COMPLETED')">⏹️ Stop Election</button>` : ''}
            ${e.status === 'COMPLETED' ? `<button class="btn btn-secondary btn-inline" onclick="loadResults('${e.electionId}','admin')">📊 View Results</button>` : ''}
            ${e.status === 'ACTIVE' ? `<button class="btn btn-secondary btn-inline" onclick="loadResults('${e.electionId}','admin')">📊 Live Results</button>` : ''}
          </div>
        </div>
      `).join('');

      // Populate results & const election filters
      const resSel = document.getElementById('res-election-filter');
      resSel.innerHTML = '<option value="">Select Election</option>' + electionsCache.map(e => `<option value="${e.electionId}">${e.title} (${e.totalSeats})</option>`).join('');
      const constSel = document.getElementById('const-election-filter');
      constSel.innerHTML = '<option value="">Select Election</option>' + electionsCache.map(e => `<option value="${e.electionId}">${e.title} (${e.totalSeats})</option>`).join('');
    } else {
      document.getElementById('elections-list').innerHTML = emptyState('🗳️', 'No elections found', 'Seed Maharashtra data to create elections and constituencies.', 'Seed Maharashtra Data', 'seedMaharashtra()');
      document.getElementById('res-election-filter').innerHTML = '<option value="">Select Election</option>';
      document.getElementById('const-election-filter').innerHTML = '<option value="">Select Election</option>';
    }

    // Division filter
    try {
      divisionsData = await api('GET', '/api/divisions');
      const divSel = document.getElementById('const-division-filter');
      divSel.innerHTML = '<option value="">All Divisions</option>' + divisionsData.divisions.map(d => `<option value="${d.name}">${d.name} (${d.districts.length} districts)</option>`).join('');
    } catch {}

    // Blockchain timeline
    if (chainData.blocks.length <= 1) {
      document.getElementById('chain-timeline').innerHTML = emptyState('⛓️', 'Genesis block only', 'Cast votes to add blocks to the chain.', '', '');
    } else {
      document.getElementById('chain-timeline').innerHTML = chainData.blocks.map((b, i) => `
      <div class="chain-item">
        <div class="chain-connector">
          <div class="chain-dot ${b.index === 0 ? 'genesis' : ''}"></div>
          ${i < chainData.blocks.length - 1 ? '<div class="chain-line"></div>' : ''}
        </div>
        <div class="chain-card">
          <div class="chain-title">${b.index === 0 ? '🌱 Genesis Block' : 'Block #' + b.index}
            <span style="font-weight:400;color:var(--muted);font-size:.68rem;margin-left:8px">${b.timestamp}</span></div>
          ${b.index > 0 ? `<div class="chain-meta"><span>Candidate</span><span style="color:var(--saffron)">${b.candidateName}</span></div>
            <div class="chain-meta"><span>Nonce</span><span>${b.nonce}</span></div>
            <div class="chain-meta"><span>Mining</span><span>${b.miningTime}ms</span></div>` : ''}
          <div class="chain-hash">🔑 ${b.currentHash}</div>
          ${b.index > 0 ? `<div class="chain-hash prev">🔗 ${b.previousHash}</div>` : ''}
        </div>
      </div>`).join('');
    }

    // Voters table
    renderVoterTable(voterData.voters);

  } catch (e) { alert('Dashboard error: ' + e.message); }
  finally { hideOverlay(); }
}

function renderVoterTable(voters) {
  document.getElementById('voter-count').textContent = voters.length + ' voters';
  if (!voters.length) {
    document.getElementById('voter-tbody').innerHTML = `
      <tr><td colspan="6" style="padding:18px;text-align:center;color:var(--muted)">
        No voters found. Seed Maharashtra data or register new voters.
      </td></tr>`;
    return;
  }
  document.getElementById('voter-tbody').innerHTML = voters.map(v => `
    <tr>
      <td><strong style="color:var(--saffron)">${v.voterId}</strong></td>
      <td>${v.name || '—'}</td>
      <td style="font-size:.78rem">${v.district || '—'}</td>
      <td style="font-size:.78rem">${v.division || '—'}</td>
      <td style="font-size:.72rem;color:var(--muted)">${v.constituencyVS || '—'}</td>
      <td><span class="voted-chip ${v.hasVoted ? 'chip-yes' : 'chip-no'}">${v.hasVoted ? '✅ Voted' : '⏳ Pending'}</span></td>
    </tr>`).join('');
}

function refreshAdmin() { loadAdminDashboard(); }
function switchTab(name, btn) {
  document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  btn.classList.add('active');
  document.getElementById('tab-' + name).classList.add('active');
}

// ═══════════════════════════════════════════════════════
//  ELECTION STATUS CONTROL
// ═══════════════════════════════════════════════════════
async function changeElectionStatus(electionId, status) {
  const action = status === 'ACTIVE' ? 'START' : 'STOP';
  if (!confirm(`Are you sure you want to ${action} election ${electionId}?`)) return;
  showOverlay(`${action === 'START' ? 'Starting' : 'Stopping'} election...`);
  try {
    await api('POST', '/api/admin/election-status', { electionId, status });
    alert(`✅ Election ${electionId} is now ${status}`);
    await loadAdminDashboard();
  } catch (e) { alert('Error: ' + e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  VOTER SEARCH
// ═══════════════════════════════════════════════════════
let searchTimeout = null;
async function searchVoters() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(async () => {
    const q = document.getElementById('voter-search').value.trim();
    const division = document.getElementById('voter-div-filter').value;
    try {
      let url = '/api/admin/voters/search?';
      if (q) url += 'q=' + encodeURIComponent(q) + '&';
      if (division) url += 'division=' + encodeURIComponent(division);
      const data = await api('GET', url);
      renderVoterTable(data.voters || []);
    } catch (e) { console.error(e); }
  }, 300);
}

// ═══════════════════════════════════════════════════════
//  ADMIN RESULTS TAB
// ═══════════════════════════════════════════════════════
async function loadAdminResults() {
  const electionId = document.getElementById('res-election-filter').value;
  if (!electionId) {
    document.getElementById('seat-tally-container').innerHTML = emptyState('📊', 'Select an election', 'Choose an election to see seat tally and results.', '', '');
    document.getElementById('division-results-grid').innerHTML = '';
    document.getElementById('res-const-list').innerHTML = '';
    return;
  }
  showOverlay('Loading election results...');
  try {
    // Seat tally
    const seats = await api('GET', '/api/results/seats?electionId=' + electionId);
    if (!seats.totalVotes) {
      document.getElementById('seat-tally-container').innerHTML = emptyState('🗳️', 'No votes yet', 'Cast votes to populate seat tally and results.', '', '');
    } else {
      document.getElementById('seat-tally-container').innerHTML = `
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;flex-wrap:wrap">
          <div><strong style="color:var(--text)">${seats.seatsDecided}</strong><span style="color:var(--muted);font-size:.82rem"> / ${seats.totalSeats} seats decided</span></div>
          <div style="color:var(--muted);font-size:.82rem">${seats.totalVotes} total votes</div>
        </div>
        <div class="seat-tally">${(seats.seatTally || []).map(s => `
          <div class="seat-card">
            <div class="seat-party" style="color:${PARTY_COLORS[s.party] || '#999'}">${s.party}</div>
            <div class="seat-count" style="color:${PARTY_COLORS[s.party] || '#999'}">${s.seats}</div>
            <div class="seat-votes">${s.votes} votes</div>
          </div>`).join('')}</div>`;
    }

    // Division results
    const divisions = ['Konkan','Pune','Nashik','Chh. Sambhajinagar','Amravati','Nagpur'];
    const divResults = await Promise.all(divisions.map(d =>
      api('GET', '/api/results/division?electionId=' + electionId + '&division=' + encodeURIComponent(d)).catch(() => null)
    ));
    document.getElementById('division-results-grid').innerHTML = divisions.map((d, i) => {
      const r = divResults[i];
      if (!r) return `<div class="division-card"><h4>${d}</h4><p style="color:var(--muted)">No data</p></div>`;
      const tally = Object.entries(r.seatTally || {}).sort((a, b) => b[1] - a[1]);
      const top = tally.slice(0, 3);
      return `<div class="division-card">
        <h4>${d}</h4>
        <p>${r.totalConstituencies} seats · ${r.totalVotes} votes</p>
        ${top.map(([p, s]) => `<div style="display:flex;justify-content:space-between;font-size:.72rem;margin-top:4px"><span style="color:${PARTY_COLORS[p] || '#999'}">${p}</span><strong style="color:${PARTY_COLORS[p] || '#999'}">${s}</strong></div>`).join('')}
      </div>`;
    }).join('');

    // Constituency list for drilldown
    const constData = await api('GET', '/api/constituencies?electionId=' + electionId);
    resultConstituenciesCache = constData.constituencies || [];
    renderResultConstituencies(resultConstituenciesCache);

  } catch (e) { alert('Results error: ' + e.message); }
  finally { hideOverlay(); }
}

function renderResultConstituencies(list) {
  if (!list.length) {
    document.getElementById('res-const-list').innerHTML = emptyState('📍', 'No constituencies yet', 'Seed Maharashtra data to view constituency results.', 'Seed Maharashtra Data', 'seedMaharashtra()');
    return;
  }
  document.getElementById('res-const-list').innerHTML = list.map(c => {
    const tagClass = c.reservationCategory === 'SC' ? 'tag-sc' : c.reservationCategory === 'ST' ? 'tag-st' : 'tag-gen';
    return `<div class="const-item" onclick="showConstResults('${c.constituencyId}')">
      <span class="const-num">#${c.number}</span>
      <span class="const-name">${c.name}</span>
      <span class="const-district">${c.district}</span>
      <span class="reservation-tag ${tagClass}">${c.reservationCategory}</span>
    </div>`;
  }).join('');
}
function filterResultConstituencies() {
  const q = document.getElementById('res-const-search').value.toLowerCase();
  renderResultConstituencies(resultConstituenciesCache.filter(c => c.name.toLowerCase().includes(q) || c.district.toLowerCase().includes(q)));
}

// ═══════════════════════════════════════════════════════
//  CONSTITUENCY DRILLDOWN MODAL
// ═══════════════════════════════════════════════════════
async function showConstResults(constituencyId) {
  const electionId = document.getElementById('res-election-filter').value;
  showOverlay('Loading constituency results...');
  try {
    const data = await api('GET', '/api/results/constituency?constituencyId=' + constituencyId + (electionId ? '&electionId=' + electionId : ''));
    document.getElementById('modal-const-title').textContent = data.constituencyName + ' (#' + constituencyId + ')';
    let body = `<div style="margin-bottom:12px;font-size:.85rem;color:var(--muted)">${data.totalVotes} total votes</div>`;
    if (data.winner) {
      body += `<div class="leader-banner"><div class="leader-title">🏆 Winner</div><div class="leader-name">${data.winner} (${data.winnerParty})</div><div class="leader-votes">Margin: ${data.margin} votes</div></div>`;
    }
    body += '<div style="margin-top:12px">';
    (data.candidates || []).forEach((c, i) => {
      const pct = parseFloat(c.percentage) || 0;
      const isWinner = i === 0 && data.totalVotes > 0;
      body += `<div class="party-bar" style="padding:10px 0">
        <div style="width:120px;flex-shrink:0"><div style="font-size:.85rem;font-weight:600;color:${isWinner ? 'var(--saffron)' : 'var(--text)'}">${c.name}</div><div style="font-size:.7rem;color:${PARTY_COLORS[c.party] || '#999'}">${c.party}</div></div>
        <div class="party-bar-fill"><div class="party-bar-inner" style="width:${pct}%;background:${PARTY_COLORS[c.party] || '#666'}"></div></div>
        <div style="width:60px;text-align:right;flex-shrink:0"><div style="font-size:.85rem;font-weight:700;color:var(--text)">${c.votes}</div><div style="font-size:.68rem;color:var(--muted)">${pct}%</div></div>
      </div>`;
    });
    body += '</div>';
    document.getElementById('modal-const-body').innerHTML = body;
    document.getElementById('constResultModal').classList.add('show');
  } catch (e) { alert('Error: ' + e.message); }
  finally { hideOverlay(); }
}
function closeConstModal() { document.getElementById('constResultModal').classList.remove('show'); }

// ═══════════════════════════════════════════════════════
//  CONSTITUENCIES TAB
// ═══════════════════════════════════════════════════════
async function loadConstituencies() {
  const electionId = document.getElementById('const-election-filter').value;
  const division = document.getElementById('const-division-filter').value;
  if (!electionId) { document.getElementById('const-list-container').innerHTML = emptyState('📍', 'Select an election', 'Choose an election to view constituencies.', '', ''); return; }
  try {
    let url = '/api/constituencies?electionId=' + electionId;
    if (division) url += '&division=' + encodeURIComponent(division);
    const data = await api('GET', url);
    constituenciesCache = data.constituencies || [];
    renderConstituencies(constituenciesCache);
    // Render the interactive map with loaded data
    renderInteractiveMap();
  } catch (e) { alert(e.message); }
}
function renderConstituencies(list) {
  document.getElementById('const-count').textContent = list.length + ' constituencies';
  if (!list.length) {
    document.getElementById('const-list-container').innerHTML = emptyState('📍', 'No constituencies found', 'Try a different division or seed Maharashtra data.', 'Seed Maharashtra Data', 'seedMaharashtra()');
    return;
  }
  document.getElementById('const-list-container').innerHTML = list.map(c => {
    const tagClass = c.reservationCategory === 'SC' ? 'tag-sc' : c.reservationCategory === 'ST' ? 'tag-st' : 'tag-gen';
    return `<div class="const-item">
      <span class="const-num">#${c.number}</span>
      <span class="const-name">${c.name}</span>
      <span class="const-district">${c.district}</span>
      <span class="reservation-tag ${tagClass}">${c.reservationCategory}</span>
    </div>`;
  }).join('');
}
function filterConstituencies() {
  const q = document.getElementById('const-search').value.toLowerCase();
  renderConstituencies(constituenciesCache.filter(c => c.name.toLowerCase().includes(q) || c.district.toLowerCase().includes(q)));
}

// ═══════════════════════════════════════════════════════
//  REGISTER VOTER — ENHANCED WITH CASCADE
// ═══════════════════════════════════════════════════════
function onRegDivisionChange() {
  const division = document.getElementById('reg-division').value;
  const distSel = document.getElementById('reg-district');
  const vsSel = document.getElementById('reg-vs');
  distSel.innerHTML = '<option value="">Select District</option>';
  vsSel.innerHTML = '<option value="">Select VS Constituency</option>';
  if (!division || !DIVISION_DISTRICTS[division]) return;
  DIVISION_DISTRICTS[division].forEach(d => {
    distSel.innerHTML += `<option value="${d}">${d}</option>`;
  });
}

function getVSElectionId() {
  if (electionsCache.length) {
    const active = electionsCache.find(e => e.type === 'VIDHAN_SABHA' && e.status === 'ACTIVE');
    if (active) return active.electionId;
    const any = electionsCache.find(e => e.type === 'VIDHAN_SABHA');
    if (any) return any.electionId;
  }
  return 'MH-VS-2024';
}

async function onRegDistrictChange() {
  const district = document.getElementById('reg-district').value;
  const vsSel = document.getElementById('reg-vs');
  vsSel.innerHTML = '<option value="">Select VS Constituency</option>';
  if (!district) return;

  const electionId = getVSElectionId();
  if (!electionId) {
    vsSel.innerHTML = '<option value="">Seed Maharashtra data first</option>';
    return;
  }

  try {
    vsSel.innerHTML = '<option value="">Loading VS constituencies...</option>';
    const data = await api('GET', '/api/constituencies?electionId=' + encodeURIComponent(electionId) +
      '&district=' + encodeURIComponent(district));
    const list = data.constituencies || [];
    if (!list.length) {
      vsSel.innerHTML = '<option value="">No VS constituencies found</option>';
      return;
    }
    vsSel.innerHTML = '<option value="">Select VS Constituency</option>' + list.map(c =>
      `<option value="${c.constituencyId}">${c.constituencyId} — ${c.name}</option>`).join('');
  } catch (e) {
    vsSel.innerHTML = '<option value="">Select VS Constituency</option>';
    showErr('reg-err', e.message);
  }
}

async function registerVoter() {
  const voterId = document.getElementById('reg-id').value.trim();
  const name = document.getElementById('reg-name').value.trim();
  const password = document.getElementById('reg-pw').value;
  const district = document.getElementById('reg-district').value;
  const division = document.getElementById('reg-division').value;
  const vs = document.getElementById('reg-vs').value;
  const errEl = document.getElementById('reg-err');
  const okEl = document.getElementById('reg-ok');
  errEl.classList.remove('show'); okEl.classList.remove('show');
  if (!voterId || !name || !password) { errEl.textContent = 'ID, Name & Password required'; errEl.classList.add('show'); return; }
  if (!division || !district || !vs) { errEl.textContent = 'Division, District, and VS Constituency are required'; errEl.classList.add('show'); return; }
  try {
    await api('POST', '/api/admin/register-voter-full', {
      voterId, name, password, district, division, constituencyVS: vs, constituencyLS: ''
    });
    okEl.textContent = '✅ Voter ' + voterId.toUpperCase() + ' registered!';
    okEl.classList.add('show');
    ['reg-id','reg-name','reg-pw'].forEach(id => document.getElementById(id).value = '');
    document.getElementById('reg-division').value = '';
    document.getElementById('reg-district').innerHTML = '<option value="">Select District</option>';
    document.getElementById('reg-vs').innerHTML = '<option value="">Select VS Constituency</option>';
    loadAdminDashboard();
  } catch (e) { errEl.textContent = '❌ ' + e.message; errEl.classList.add('show'); }
}
function selectCandidate(idx, candidateId, name, party) {
  selectedCand = { idx, candidateId, name, party };
  document.querySelectorAll('#vc-cand-list .cand-card').forEach((c, i) => c.classList.toggle('selected', i === idx));
}
async function submitVote() {
  if (!selectedCand) return showErr('vc-err', 'Please select a candidate');
  showOverlay('Mining vote block on blockchain...');
  try {
    const data = await api('POST', '/api/voter/cast-vote-election', {
      voterId: currentVoter, candidateName: selectedCand.name,
      candidateId: selectedCand.candidateId, party: selectedCand.party,
      electionId: currentElection, constituencyId: currentConstituency
    });
    document.getElementById('cf-voter').textContent = currentVoter;
    document.getElementById('cf-election').textContent = currentElection;
    document.getElementById('cf-const').textContent = currentConstituency;
    document.getElementById('cf-cand').textContent = selectedCand.name + ' (' + selectedCand.party + ')';
    document.getElementById('cf-index').textContent = data.blockIndex;
    document.getElementById('cf-hash').textContent = data.currentHash ? data.currentHash.substring(0, 24) + '...' : '–';
    document.getElementById('cf-time').textContent = data.miningTime + ' ms';
    show('s-vote-confirm');
    launchConfetti();
  } catch (e) { showErr('vc-err', e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  RESULTS
// ═══════════════════════════════════════════════════════
async function loadResults(electionId, from) {
  showOverlay('Loading results...');
  try {
    const data = await api('GET', '/api/results/election?electionId=' + electionId);
    document.getElementById('res-sub').textContent = data.totalVotes + ' total votes — ' + electionId;
    document.getElementById('res-back').onclick = () => from === 'admin' ? show('s-admin-dash') : show('s-voter-dash');
    const partyEntries = Object.entries(data.partyResults || {}).sort((a, b) => b[1] - a[1]);
    const maxVotes = partyEntries.length > 0 ? partyEntries[0][1] : 1;
    document.getElementById('party-bars').innerHTML = partyEntries.map(([party, count]) => `
      <div class="party-bar"><div class="party-bar-name">${party}</div>
        <div class="party-bar-fill"><div class="party-bar-inner" style="width:${(count/maxVotes*100).toFixed(1)}%;background:${PARTY_COLORS[party]||'#666'}"></div></div>
        <div class="party-bar-count">${count}</div></div>
    `).join('');
    const entries = Object.entries(data.results || {});
    if (entries.length > 0) {
      const leader = entries.reduce((a, b) => b[1] > a[1] ? b : a);
      document.getElementById('leader-box').style.display = 'block';
      document.getElementById('leader-name').textContent = leader[0];
      document.getElementById('leader-votes').textContent = leader[1] + ' votes';
    } else {
      document.getElementById('leader-box').style.display = 'none';
    }
    const labels = entries.map(([k]) => k.length > 20 ? k.substring(0, 20) + '...' : k);
    const vals = entries.map(([, v]) => v);
    if (pieInst) pieInst.destroy(); if (barInst) barInst.destroy();
    if (entries.length) {
      const grid = document.querySelector('#s-results .charts-grid');
      if (!document.getElementById('pieChart')) {
        grid.innerHTML = `<div class="chart-box"><div class="chart-title">Vote Distribution</div><canvas id="pieChart" height="220"></canvas></div>
          <div class="chart-box"><div class="chart-title">Party-wise Votes</div><canvas id="barChart" height="220"></canvas></div>`;
      }
      Chart.defaults.color = 'rgba(238,238,245,0.5)';
      pieInst = new Chart(document.getElementById('pieChart').getContext('2d'), {
        type: 'doughnut', data: { labels, datasets: [{ data: vals, backgroundColor: COLORS.slice(0, vals.length), borderColor: 'rgba(7,7,15,0.8)', borderWidth: 3 }] },
        options: { plugins: { legend: { position: 'bottom', labels: { padding: 10, font: { size: 10 } } } }, cutout: '55%' }
      });
      barInst = new Chart(document.getElementById('barChart').getContext('2d'), {
        type: 'bar', data: { labels: partyEntries.map(p => p[0]), datasets: [{ label: 'Votes', data: partyEntries.map(p => p[1]), backgroundColor: partyEntries.map(p => PARTY_COLORS[p[0]] || '#666'), borderRadius: 6, borderSkipped: false }] },
        options: { plugins: { legend: { display: false } }, scales: { x: { grid: { color: 'rgba(255,255,255,0.05)' } }, y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { stepSize: 1 } } } }
      });
    } else {
      const grid = document.querySelector('#s-results .charts-grid');
      grid.innerHTML = `
        <div class="chart-box" style="display:flex;align-items:center;justify-content:center;min-height:220px">
          ${emptyState('🗳️', 'No votes yet', 'Cast votes to populate results.', '', '')}
        </div>
        <div class="chart-box" style="display:flex;align-items:center;justify-content:center;min-height:220px">
          ${emptyState('📊', 'No data yet', 'Results will appear once voting begins.', '', '')}
        </div>`;
    }
    show('s-results');
  } catch (e) { alert('Results error: ' + e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  ADMIN LOGIN
// ═══════════════════════════════════════════════════════
async function adminLogin() {
  const username = document.getElementById('al-user').value.trim();
  const password = document.getElementById('al-pw').value;
  if (!username || !password) return showErr('al-err', 'Enter credentials');
  showOverlay('Authenticating...');
  try {
    await api('POST', '/api/admin/login', { username, password });
    updateNav('🔐 Election Commission');
    await loadAdminDashboard(); show('s-admin-dash');
  } catch (e) { showErr('al-err', e.message); }
  finally { hideOverlay(); }
}

// ═══════════════════════════════════════════════════════
//  SEED
// ═══════════════════════════════════════════════════════
async function seedMaharashtra() {
  if (!confirm('This will seed all 288 Vidhan Sabha + 48 Lok Sabha constituencies + 30 demo voters. Continue?')) return;
  showOverlay('Seeding Maharashtra electoral data...\n288 VS + 48 LS constituencies');
  try {
    const data = await api('POST', '/api/admin/seed-maharashtra');
    alert('✅ ' + data.message);
    await loadAdminDashboard();
    // Load constituencies and render the map after seeding
    await loadAllConstituenciesForMap();
  } catch (e) { alert('Seed error: ' + e.message); }
  finally { hideOverlay(); }
}

// Load all constituencies specifically for map rendering
async function loadAllConstituenciesForMap() {
  try {
    // Load VS constituencies
    const vsData = await api('GET', '/api/constituencies?electionId=MH-VS-2024');
    const vsConsts = vsData.constituencies || [];
    // Load LS constituencies
    const lsData = await api('GET', '/api/constituencies?electionId=MH-LS-2024');
    const lsConsts = lsData.constituencies || [];
    // Combine and cache
    constituenciesCache = [...vsConsts, ...lsConsts];
    // Render the map
    renderInteractiveMap();
  } catch (e) {
    console.log('Map load error:', e);
  }
}
