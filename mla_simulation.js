// ═══════════════════════════════════════════════════════
//  MLA HUNG ASSEMBLY SIMULATION — Java Backend Integration
//  Uses /api/mla-sim/* endpoints (MongoDB + BCrypt + Blockchain)
// ═══════════════════════════════════════════════════════

const urlParams = new URLSearchParams(window.location.search);
const API = urlParams.get('api') || 'http://localhost:4567';

async function api(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' },
    ...(body ? { body: JSON.stringify(body) } : {}) };
  const res = await fetch(API + path, opts);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || 'Request failed');
  return data;
}

const TOTAL_VOTERS = 100;
const MAJORITY = 51;
let simState = null;
let selectedCrossMla = null;
let pieInst = null, barInst = null;
let crossVoteLog = [];

// ═══════════════════════════════════════════════════════
//  RENDER FUNCTIONS
// ═══════════════════════════════════════════════════════

function renderMlaCards(mlas) {
  if (!mlas || !mlas.length) {
    document.getElementById('mlaGrid').innerHTML = '<div style="grid-column:1/-1;text-align:center;color:var(--muted);padding:30px">Click "Start Simulation" to seed data and begin</div>';
    return;
  }
  const maxVotes = Math.max(...mlas.map(m => m.votes));
  const leader = mlas.reduce((a, b) => b.votes > a.votes ? b : a);
  document.getElementById('mlaGrid').innerHTML = mlas.map(m => {
    const pct = (m.votes / TOTAL_VOTERS * 100).toFixed(1);
    const isLeading = m.mlaId === leader.mlaId && m.votes > 0;
    return `<div class="mla-card ${isLeading ? 'leading' : ''}">
      <div class="mla-avatar" style="background:${m.color}">${m.symbol}</div>
      <div class="mla-name">${m.name}</div>
      <div class="mla-party" style="color:${m.color}">${m.partyFull} (${m.party})</div>
      <div class="mla-email">${m.email}</div>
      <div class="mla-votes" style="color:${m.color}">${m.votes}</div>
      <div class="mla-pct">${pct}% of 100 votes</div>
      <div class="mla-bar"><div class="mla-bar-inner" style="width:${pct}%;background:${m.color}"></div></div>
      <div class="majority-line">51% majority line ━━━━━</div>
      ${m.votes >= MAJORITY ? '<div class="majority-marker marker-won" style="margin-top:8px">✅ MAJORITY</div>' : ''}
      ${isLeading && m.votes < MAJORITY ? '<div class="majority-marker marker-hung" style="margin-top:8px">👑 Leading</div>' : ''}
    </div>`;
  }).join('');
}

function renderCharts(mlas) {
  if (!mlas || !mlas.length) return;
  const labels = mlas.map(m => m.name + ' (' + m.party + ')');
  const vals = mlas.map(m => m.votes);
  const colors = mlas.map(m => m.color);
  Chart.defaults.color = 'rgba(238,238,245,0.5)';
  if (pieInst) pieInst.destroy();
  if (barInst) barInst.destroy();
  pieInst = new Chart(document.getElementById('simPie').getContext('2d'), {
    type:'doughnut', data:{ labels, datasets:[{ data:vals, backgroundColor:colors, borderColor:'rgba(7,7,15,0.8)', borderWidth:3 }] },
    options:{ plugins:{ legend:{ position:'bottom', labels:{ padding:10, font:{ size:10 } } } }, cutout:'55%' }
  });
  barInst = new Chart(document.getElementById('simBar').getContext('2d'), {
    type:'bar', data:{ labels:mlas.map(m=>m.party), datasets:[{ label:'Votes', data:vals, backgroundColor:colors, borderRadius:6, borderSkipped:false }] },
    options:{ plugins:{ legend:{ display:false } }, scales:{ x:{ grid:{ color:'rgba(255,255,255,0.05)' } }, y:{ beginAtZero:true, max:60, grid:{ color:'rgba(255,255,255,0.05)' }, ticks:{ stepSize:10 } } } }
  });
}

function renderCrossMlaSelect(mlas) {
  const sel = document.getElementById('cvMlaSelect');
  sel.innerHTML = mlas.map(m => `<div class="cv-mla-opt" id="cv-opt-${m.mlaId}" onclick="selectCrossMla(${m.mlaId})">
    <div style="font-size:1.4rem">${m.symbol}</div>
    <div class="opt-name">${m.name}</div>
    <div class="opt-party" style="color:${m.color}">${m.party}</div>
  </div>`).join('');
}

function selectCrossMla(id) {
  selectedCrossMla = id;
  document.querySelectorAll('.cv-mla-opt').forEach(el => el.classList.remove('selected'));
  document.getElementById('cv-opt-' + id).classList.add('selected');
}

function showErr(id, msg) {
  const el = document.getElementById(id);
  el.textContent = msg; el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 4000);
}
function showOk(id, msg) {
  const el = document.getElementById(id);
  el.textContent = msg; el.classList.add('show');
  setTimeout(() => el.classList.remove('show'), 4000);
}

function setPhase(num) {
  const badge = document.getElementById('phaseBadge');
  const status = document.getElementById('simStatus');
  const phases = [
    ['📊 Phase 1 — Initial Vote Distribution', 'Phase 1: Initial Voting', 'phase-1'],
    ['⚠️ Phase 2 — Hung Assembly Declared', 'Phase 2: Hung Assembly', 'phase-2'],
    ['🤝 Phase 3 — Party Merger', 'Phase 3: Party Merger', 'phase-2'],
    ['🗳️ Phase 4 — Cross-Voting Open', 'Phase 4: Cross-Voting', 'phase-2'],
    ['🏆 Final — Result Declared', 'Final Result', 'phase-3'],
    ['📊 Audit — End-to-End Report', 'Audit Report', 'phase-3']
  ];
  const p = phases[Math.min(num, phases.length - 1)];
  badge.textContent = p[0]; badge.className = 'phase-badge ' + p[2];
  status.textContent = p[1];
  for (let i = 1; i <= 6; i++) {
    const el = document.getElementById('tl-' + i);
    if (el) el.className = 'tl-step' + (i <= num ? ' done' : '') + (i === num + 1 ? ' active' : '');
  }
}

// ═══════════════════════════════════════════════════════
//  SIMULATION — Calls Java backend to seed data
// ═══════════════════════════════════════════════════════

async function startSimulation() {
  const btn = document.getElementById('btnStartSim');
  btn.disabled = true;
  btn.textContent = '⏳ Seeding 100 voters into MongoDB (BCrypt hashing)...';
  
  try {
    // Step 1: Seed data via Java backend (BCrypt + MongoDB)
    await api('POST', '/api/mla-sim/seed');
    btn.textContent = '⏳ Loading state from MongoDB...';
    
    // Step 2: Get state from backend
    simState = await api('GET', '/api/mla-sim/state');
    
    // Step 3: Animate the cards loading
    setPhase(1);
    renderMlaCards(simState.mlas);
    renderCharts(simState.mlas);
    
    await sleep(600);
    
    // Step 4: Check for hung assembly
    if (!simState.majorityAchieved) {
      setPhase(2);
      const leader = simState.mlas.reduce((a, b) => b.votes > a.votes ? b : a);
      document.getElementById('hungBanner').style.display = 'block';
      document.getElementById('hungBanner').innerHTML = `<div class="hung-banner">
        <h2>⚠️ HUNG ASSEMBLY DECLARED</h2>
        <p>No MLA achieved 51% majority (${MAJORITY} votes needed out of 100)</p>
        <p style="margin-top:8px;color:var(--text)">Leading: <strong style="color:${leader.color}">${leader.name} (${leader.party})</strong> with ${leader.votes} votes — still ${MAJORITY - leader.votes} short</p>
        <p style="margin-top:8px;font-size:.75rem;color:var(--muted)">📦 Data stored in MongoDB Atlas · 🔐 Passwords BCrypt hashed · ⛓️ Cross-votes mined on blockchain</p>
        <p style="margin-top:12px;font-size:.78rem;color:var(--muted)">Cross-voting portal is now open. Voters authenticate with email + password (verified server-side via BCrypt).</p>
      </div>`;
      
      await sleep(400);
      // Show MERGER section first (before individual cross-voting)
      setPhase(2);
      document.getElementById('mergerSection').style.display = 'block';
      renderMergerUI(simState.mlas);
      btn.style.display = 'none';
    }
  } catch (e) {
    alert('Error: ' + e.message + '\n\nMake sure the Java backend is running on ' + API);
    btn.disabled = false;
    btn.textContent = '▶️ Start Initial Voting Simulation';
  }
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

// ═══════════════════════════════════════════════════════
//  CROSS-VOTING — Calls Java backend (BCrypt auth + blockchain mining)
// ═══════════════════════════════════════════════════════

async function submitCrossVote() {
  const email = document.getElementById('cv-email').value.trim().toLowerCase();
  const pw = document.getElementById('cv-pw').value;
  
  if (!email || !pw) return showErr('cv-err', '❌ Email and password are required');
  if (!selectedCrossMla) return showErr('cv-err', '❌ Select an MLA to give your vote to');
  
  // Disable button during processing
  const submitBtn = document.querySelector('#crossVoteSection .btn-green');
  submitBtn.disabled = true;
  submitBtn.textContent = '⏳ Verifying password (BCrypt) & mining block...';
  
  try {
    // Call Java backend — BCrypt password check + SHA-256 PoW mining
    const result = await api('POST', '/api/mla-sim/cross-vote', {
      email: email,
      password: pw,
      targetMlaId: String(selectedCrossMla)
    });
    
    // Success — update UI
    showOk('cv-ok', `✅ Vote transferred: ${result.fromMla} (${result.fromParty}) → ${result.toMla} (${result.toParty}) | Block #${result.blockIndex} mined in ${result.miningTime}ms`);
    
    // Add to live log
    crossVoteLog.unshift(result);
    const log = document.getElementById('cvLog');
    log.innerHTML = crossVoteLog.map(r => `<div class="log-item">
      <div class="log-icon">🗳️</div>
      <div class="log-text"><strong>${r.voterName}</strong> transferred vote: <span style="color:${getColorForParty(r.fromParty)}">${r.fromParty}</span> → <span style="color:${getColorForParty(r.toParty)};font-weight:700">${r.toParty}</span>
        <div style="font-size:.65rem;color:var(--muted);margin-top:2px">Block #${r.blockIndex} · Hash: ${r.blockHash.substring(0,16)}... · ${r.miningTime}ms</div>
      </div>
      <div class="log-time">${r.timestamp}</div>
    </div>`).join('');
    
    // Refresh state from backend
    simState = await api('GET', '/api/mla-sim/state');
    renderMlaCards(simState.mlas);
    renderCharts(simState.mlas);
    
    // Clear form
    document.getElementById('cv-email').value = '';
    document.getElementById('cv-pw').value = '';
    selectedCrossMla = null;
    document.querySelectorAll('.cv-mla-opt').forEach(el => el.classList.remove('selected'));
    
    // Check majority
    if (simState.majorityAchieved) {
      setPhase(4);
      const winner = simState.mlas.reduce((a, b) => b.votes > a.votes ? b : a);
      document.getElementById('hungBanner').innerHTML = `<div class="hung-banner" style="border-color:rgba(0,230,118,0.5);background:linear-gradient(135deg,rgba(0,230,118,0.15),rgba(255,215,0,0.15))">
        <h2 style="color:#00e676">🏆 MAJORITY ACHIEVED!</h2>
        <p style="color:var(--text);font-size:1.1rem"><strong style="color:${winner.color}">${winner.name} (${winner.party})</strong> wins with <strong>${winner.votes}</strong> votes!</p>
        <p style="margin-top:6px;color:var(--muted)">${winner.votes}% — Crossed ${MAJORITY}% majority threshold</p>
      </div>`;
      
      setTimeout(async () => {
        setPhase(5);
        await loadReport();
        document.getElementById('reportSection').style.display = 'block';
        document.getElementById('reportSection').scrollIntoView({ behavior:'smooth' });
      }, 1000);
    }
  } catch (e) {
    showErr('cv-err', '❌ ' + e.message);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = '✅ Submit Cross-Vote';
  }
}

function getColorForParty(party) {
  const colors = { BJP:'#FF9933', INC:'#00BFFF', SHS:'#FF6600', NCP:'#004D40' };
  return colors[party] || '#999';
}

// ═══════════════════════════════════════════════════════
//  PARTY MERGER FUNCTIONS
// ═══════════════════════════════════════════════════════

let mergerToMla = null;

function renderMergerUI(mlas) {
  const toList = document.getElementById('mergerToList');
  toList.innerHTML = mlas.map(m => `<div class="merger-mla-card" id="merge-to-${m.mlaId}" onclick="selectMergerTo(${m.mlaId})">
    <div style="font-size:1.6rem">${m.symbol}</div>
    <div style="font-weight:700;color:var(--text)">${m.name}</div>
    <div style="font-size:.75rem;color:${m.color};font-weight:600">${m.party} — ${m.votes} votes</div>
  </div>`).join('');
}

function selectMergerTo(id) {
  mergerToMla = id;
  document.querySelectorAll('#mergerToList .merger-mla-card').forEach(el => el.classList.remove('selected'));
  document.getElementById('merge-to-' + id).classList.add('selected');
}

async function executeMerger() {
  const email = document.getElementById('merge-email').value.trim().toLowerCase();
  const pw = document.getElementById('merge-pw').value;
  
  if (!email || !pw) return showErr('merge-err', '❌ Enter MLA email and password');
  if (!mergerToMla) return showErr('merge-err', '❌ Select the target MLA to receive votes');
  
  const btn = document.getElementById('btnMerge');
  btn.disabled = true;
  btn.textContent = '⏳ Verifying MLA credentials & mining blockchain...';
  
  try {
    const result = await api('POST', '/api/mla-sim/merge', {
      email: email,
      password: pw,
      toMlaId: String(mergerToMla)
    });
    
    showOk('merge-ok', `✅ MERGER COMPLETE: ${result.fromMla} (${result.fromParty}) → ${result.toMla} (${result.toParty}) — ${result.mergedVotes} votes transferred!`);
    
    // Show merger result
    const resDiv = document.getElementById('mergerResult');
    resDiv.style.display = 'block';
    resDiv.innerHTML = `<h3 style="color:#00e676;margin:0 0 8px">🤝 Merger Executed Successfully!</h3>
      <p style="font-size:.9rem;color:var(--text)"><strong style="color:${getColorForParty(result.fromParty)}">${result.fromMla} (${result.fromParty})</strong> merged all <strong>${result.mergedVotes} votes</strong> into <strong style="color:${getColorForParty(result.toParty)}">${result.toMla} (${result.toParty})</strong></p>
      <p style="font-size:.75rem;color:var(--muted);margin-top:6px">Each vote mined as a blockchain block · MLA authenticated via BCrypt · Timestamp: ${result.timestamp}</p>`;
    
    // Clear form
    document.getElementById('merge-email').value = '';
    document.getElementById('merge-pw').value = '';
    mergerToMla = null;
    document.querySelectorAll('#mergerToList .merger-mla-card').forEach(el => el.classList.remove('selected'));
    
    // Refresh state
    simState = await api('GET', '/api/mla-sim/state');
    renderMlaCards(simState.mlas);
    renderCharts(simState.mlas);
    renderMergerUI(simState.mlas);
    
    // Check if majority now achieved
    if (simState.majorityAchieved) {
      setPhase(4);
      const winner = simState.mlas.reduce((a, b) => b.votes > a.votes ? b : a);
      document.getElementById('hungBanner').innerHTML = `<div class="hung-banner" style="border-color:rgba(0,230,118,0.5);background:linear-gradient(135deg,rgba(0,230,118,0.15),rgba(255,215,0,0.15))">
        <h2 style="color:#00e676">🏆 MAJORITY ACHIEVED VIA MERGER!</h2>
        <p style="color:var(--text);font-size:1.1rem"><strong style="color:${winner.color}">${winner.name} (${winner.party})</strong> wins with <strong>${winner.votes}</strong> votes after merger!</p>
        <p style="margin-top:6px;color:var(--muted)">${winner.votes}% — Crossed ${MAJORITY}% majority threshold</p>
      </div>`;
      setTimeout(async () => {
        setPhase(5);
        await loadReport();
        document.getElementById('reportSection').style.display = 'block';
        document.getElementById('reportSection').scrollIntoView({ behavior:'smooth' });
      }, 1000);
    }
  } catch (e) {
    showErr('merge-err', '❌ ' + e.message);
  } finally {
    btn.disabled = false;
    btn.textContent = '🤝 Execute Party Merger';
  }
}

function skipToIndividualCrossVoting() {
  setPhase(3);
  document.getElementById('crossVoteSection').style.display = 'block';
  renderCrossMlaSelect(simState.mlas);
  document.getElementById('crossVoteSection').scrollIntoView({ behavior:'smooth' });
}

// ═══════════════════════════════════════════════════════
//  AUDIT REPORT — Loaded from Java backend (MongoDB)
// ═══════════════════════════════════════════════════════

async function loadReport() {
  try {
    const report = await api('GET', '/api/mla-sim/report');
    
    // Summary
    const tallyEntries = Object.entries(report.tally);
    document.getElementById('reportSummary').innerHTML = `
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px">
        ${tallyEntries.map(([name, votes]) => `<div class="stat-box">
          <div class="stat-num" style="color:${getColorForName(name)}">${votes}</div>
          <div class="stat-lbl">${name}</div>
        </div>`).join('')}
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px">
        <div class="stat-box"><div class="stat-num">${report.totalVoters}</div><div class="stat-lbl">Total Voters</div></div>
        <div class="stat-box"><div class="stat-num" style="color:#2196F3">${report.totalCrossVotes}</div><div class="stat-lbl">Cross-Votes Cast</div></div>
        <div class="stat-box"><div class="stat-num" style="color:${report.majorityAchieved?'#00e676':'#ff5252'}">${report.majorityAchieved?'YES':'NO'}</div><div class="stat-lbl">Majority Achieved</div></div>
      </div>`;
    
    // Table
    document.getElementById('reportBody').innerHTML = report.voters.map((v, i) => `<tr>
      <td>${v.id}</td>
      <td><strong>${v.name}</strong></td>
      <td style="font-family:'JetBrains Mono',monospace;font-size:.7rem">${v.email}</td>
      <td>${v.initialVote}</td>
      <td>${v.crossVote || '<span style="color:var(--muted)">—</span>'}</td>
      <td><strong>${v.finalVote}</strong></td>
      <td style="font-size:.7rem;color:var(--muted)">${v.crossVoteTime || '—'}</td>
      <td>${v.hasCrossVoted ? '<span class="voted-chip chip-yes">Cross-Voted</span>' : '<span class="voted-chip chip-no">Original</span>'}</td>
    </tr>`).join('');
  } catch (e) {
    console.error('Report load error:', e);
  }
}

function getColorForName(name) {
  if (name.includes('Patil')) return '#FF9933';
  if (name.includes('Deshmukh')) return '#00BFFF';
  if (name.includes('Shinde')) return '#FF6600';
  if (name.includes('Jadhav')) return '#004D40';
  return '#999';
}

function exportReport() {
  // Trigger report load then export
  api('GET', '/api/mla-sim/report').then(report => {
    let html = `<!DOCTYPE html><html><head><meta charset="UTF-8"><title>MLA Hung Assembly - Audit Report</title>
<style>body{font-family:Arial,sans-serif;padding:30px;background:#fff;color:#222}
h1{color:#FF9933;border-bottom:3px solid #FF9933;padding-bottom:8px}
h2{color:#333;margin-top:24px}
table{width:100%;border-collapse:collapse;margin:16px 0;font-size:13px}
th{background:#FF9933;color:#fff;padding:10px 8px;text-align:left}
td{padding:8px;border-bottom:1px solid #ddd}
tr:nth-child(even){background:#f9f9f9}
.sbox{display:inline-block;width:23%;background:#f5f5f5;border:1px solid #ddd;border-radius:8px;padding:16px;text-align:center;margin:4px}
.sbox h3{font-size:28px;margin:0;color:#FF9933}
.sbox p{margin:4px 0 0;font-size:12px;color:#666}
.stamp{text-align:center;margin-top:30px;padding:20px;border:2px dashed #ccc;color:#999;font-size:12px}
</style></head><body>
<h1>🏛️ MLA Hung Assembly — End-to-End Audit Report</h1>
<p><strong>Generated:</strong> ${new Date().toLocaleString('en-IN',{timeZone:'Asia/Kolkata'})}</p>
<p><strong>Backend:</strong> Java (Spark) + MongoDB Atlas + BCrypt Password Hashing + SHA-256 Blockchain PoW</p>
<h2>📊 Final Tally</h2>
<div>${Object.entries(report.tally).map(([n,v])=>`<div class="sbox"><h3>${v}</h3><p>${n}</p></div>`).join('')}</div>
<p><strong>Majority:</strong> ${report.majorityAchieved ? 'YES — Achieved' : 'NO — Hung Assembly'} | <strong>Cross-Votes:</strong> ${report.totalCrossVotes} of ${report.totalVoters}</p>
<h2>📋 Complete Voter Trail (${report.totalVoters} voters)</h2>
<table><thead><tr><th>#</th><th>Voter</th><th>Email</th><th>Initial Vote</th><th>Cross-Vote</th><th>Final Vote</th><th>Time</th><th>Status</th></tr></thead><tbody>`;
    
    report.voters.forEach(v => {
      html += `<tr><td>${v.id}</td><td>${v.name}</td><td>${v.email}</td><td>${v.initialVote}</td><td>${v.crossVote||'—'}</td><td>${v.finalVote}</td><td>${v.crossVoteTime||'—'}</td><td>${v.hasCrossVoted?'Cross-Voted':'Original'}</td></tr>`;
    });
    
    if (report.crossVoteLog && report.crossVoteLog.length) {
      html += `</tbody></table><h2>⛓️ Blockchain Cross-Vote Blocks</h2><table><thead><tr><th>Block#</th><th>Voter</th><th>From</th><th>To</th><th>Hash</th><th>Mining</th><th>Time</th></tr></thead><tbody>`;
      report.crossVoteLog.forEach(l => {
        html += `<tr><td>${l.blockIndex}</td><td>${l.voterName}</td><td>${l.fromMla} (${l.fromParty})</td><td>${l.toMla} (${l.toParty})</td><td style="font-size:10px">${l.blockHash.substring(0,20)}...</td><td>${l.miningTime}ms</td><td>${l.timestamp}</td></tr>`;
      });
    }
    
    html += `</tbody></table>
<div class="stamp">VoteChain — Maharashtra E-Voting System<br>Java Backend · MongoDB Atlas · BCrypt Auth · SHA-256 PoW Blockchain<br>${new Date().toISOString()}</div></body></html>`;
    
    const blob = new Blob([html], { type:'text/html' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'MLA_Hung_Assembly_Audit_Report.html';
    a.click();
  }).catch(e => alert('Export error: ' + e.message));
}

// ═══════════════════════════════════════════════════════
//  RESET
// ═══════════════════════════════════════════════════════

async function resetSimulation() {
  simState = null;
  crossVoteLog = [];
  selectedCrossMla = null;
  mergerToMla = null;
  setPhase(0);
  document.getElementById('hungBanner').style.display = 'none';
  document.getElementById('mergerSection').style.display = 'none';
  document.getElementById('crossVoteSection').style.display = 'none';
  document.getElementById('reportSection').style.display = 'none';
  document.getElementById('mergerResult').style.display = 'none';
  const btn = document.getElementById('btnStartSim');
  btn.style.display = '';
  btn.disabled = false;
  btn.textContent = '▶️ Start Initial Voting Simulation';
  document.getElementById('cvLog').innerHTML = '<div style="text-align:center;color:var(--muted);padding:30px;font-size:.82rem">No cross-votes yet.</div>';
  renderMlaCards([]);
  renderCharts([]);
}

// ═══════════════════════════════════════════════════════
//  INIT — Try to load existing state from backend
// ═══════════════════════════════════════════════════════
async function init() {
  try {
    simState = await api('GET', '/api/mla-sim/state');
    if (simState.totalVoters > 0) {
      renderMlaCards(simState.mlas);
      renderCharts(simState.mlas);
      if (!simState.majorityAchieved) {
        setPhase(2);
        const leader = simState.mlas.reduce((a, b) => b.votes > a.votes ? b : a);
        document.getElementById('hungBanner').style.display = 'block';
        document.getElementById('hungBanner').innerHTML = `<div class="hung-banner">
          <h2>⚠️ HUNG ASSEMBLY — Merger / Cross-Voting Open</h2>
          <p>Leading: <strong style="color:${leader.color}">${leader.name} (${leader.party})</strong> with ${leader.votes} votes — ${MAJORITY - leader.votes} short of majority</p>
        </div>`;
        document.getElementById('mergerSection').style.display = 'block';
        renderMergerUI(simState.mlas);
        document.getElementById('crossVoteSection').style.display = 'block';
        renderCrossMlaSelect(simState.mlas);
        document.getElementById('btnStartSim').style.display = 'none';
      } else {
        setPhase(5);
        renderMlaCards(simState.mlas);
        await loadReport();
        document.getElementById('reportSection').style.display = 'block';
        document.getElementById('btnStartSim').style.display = 'none';
      }
    } else {
      renderMlaCards([]);
    }
  } catch {
    renderMlaCards([]);
  }
}
init();

