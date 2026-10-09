/**
 * ============================================================================
 * FITBIT CHARTS - Lightweight Canvas Charting Engine
 * Zero dependencies, pixel-perfect crisp retina display
 * ============================================================================
 */

const Charts = {
  setupCanvas(canvasId) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return null;
    const ctx = canvas.getContext('2d');
    const rect = canvas.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);
    return { ctx, width: rect.width, height: rect.height };
  },

  drawBarChart(canvasId, labels, values, color = '#00f2fe') {
    const setup = this.setupCanvas(canvasId);
    if (!setup) return;
    const { ctx, width, height } = setup;

    ctx.clearRect(0, 0, width, height);

    const padBottom = 26;
    const padTop = 16;
    const padSide = 20;
    const chartHeight = height - padBottom - padTop;
    const maxVal = Math.max(...values, 1000) * 1.15;
    const barWidth = Math.min(36, (width - padSide * 2) / values.length - 12);

    values.forEach((val, i) => {
      const x = padSide + i * ((width - padSide * 2) / values.length) + 6;
      const barH = (val / maxVal) * chartHeight;
      const y = height - padBottom - barH;

      // Draw rounded bar
      const grad = ctx.createLinearGradient(0, y, 0, height - padBottom);
      grad.addColorStop(0, color);
      grad.addColorStop(1, 'rgba(0, 242, 254, 0.2)');

      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.roundRect(x, y, barWidth, barH, [6, 6, 0, 0]);
      ctx.fill();

      // Value label
      ctx.fillStyle = '#94a3b8';
      ctx.font = '10px -apple-system, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(labels[i], x + barWidth / 2, height - 8);
    });
  },

  drawLineChart(canvasId, labels, values, strokeColor = '#10b981') {
    const setup = this.setupCanvas(canvasId);
    if (!setup) return;
    const { ctx, width, height } = setup;

    ctx.clearRect(0, 0, width, height);
    if (values.length === 0) return;

    const pad = 28;
    const chartWidth = width - pad * 2;
    const chartHeight = height - pad * 2;

    const minVal = Math.min(...values) * 0.98;
    const maxVal = Math.max(...values) * 1.02;
    const range = (maxVal - minVal) || 1;

    const pts = values.map((val, i) => {
      const x = pad + (i / Math.max(1, values.length - 1)) * chartWidth;
      const y = height - pad - ((val - minVal) / range) * chartHeight;
      return { x, y, val };
    });

    // Draw gradient area under line
    const areaGrad = ctx.createLinearGradient(0, pad, 0, height - pad);
    areaGrad.addColorStop(0, strokeColor + '44');
    areaGrad.addColorStop(1, strokeColor + '00');

    ctx.beginPath();
    ctx.moveTo(pts[0].x, height - pad);
    pts.forEach(p => ctx.lineTo(p.x, p.y));
    ctx.lineTo(pts[pts.length - 1].x, height - pad);
    ctx.closePath();
    ctx.fillStyle = areaGrad;
    ctx.fill();

    // Draw Line
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].x, pts[i].y);
    }
    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();

    // Draw dots
    pts.forEach((p, idx) => {
      ctx.fillStyle = '#fff';
      ctx.beginPath();
      ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = 2;
      ctx.stroke();

      // Label
      ctx.fillStyle = '#94a3b8';
      ctx.font = '10px -apple-system, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(labels[idx] || '', p.x, height - 8);
    });
  },

  drawSleepHypnogram(canvasId, deepMin, lightMin, remMin, awakeMin) {
    const setup = this.setupCanvas(canvasId);
    if (!setup) return;
    const { ctx, width, height } = setup;

    ctx.clearRect(0, 0, width, height);

    const total = deepMin + lightMin + remMin + awakeMin;
    if (total === 0) return;

    const stages = [
      { name: 'Deep', val: deepMin, color: '#38bdf8' },
      { name: 'Light', val: lightMin, color: '#818cf8' },
      { name: 'REM', val: remMin, color: '#c084fc' },
      { name: 'Awake', val: awakeMin, color: '#fb7185' }
    ];

    let startX = 14;
    const barWidth = width - 28;
    const barHeight = 24;
    const y = 30;

    stages.forEach(st => {
      const segW = (st.val / total) * barWidth;
      ctx.fillStyle = st.color;
      ctx.beginPath();
      ctx.rect(startX, y, segW, barHeight);
      ctx.fill();
      startX += segW;
    });

    // Legend
    let legendX = 14;
    stages.forEach(st => {
      ctx.fillStyle = st.color;
      ctx.fillRect(legendX, y + 36, 10, 10);
      ctx.fillStyle = '#94a3b8';
      ctx.font = '11px -apple-system, sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText(`${st.name}: ${st.val}m (${Math.round((st.val / total) * 100)}%)`, legendX + 16, y + 45);
      legendX += 135;
    });
  }
};

window.Charts = Charts;
