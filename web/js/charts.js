/**
 * ============================================================================
 * GOOGLE FIT CHARTS - Minimal Retina Canvas Charts
 * Clean, minimal, crisp typography and material colors
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

  drawBarChart(canvasId, labels, values, color = '#1a73e8') {
    const setup = this.setupCanvas(canvasId);
    if (!setup) return;
    const { ctx, width, height } = setup;

    ctx.clearRect(0, 0, width, height);

    const padBottom = 24;
    const padTop = 16;
    const padSide = 16;
    const chartHeight = height - padBottom - padTop;
    const maxVal = Math.max(...values, 1000) * 1.15;
    const barWidth = Math.min(32, (width - padSide * 2) / values.length - 10);

    values.forEach((val, i) => {
      const x = padSide + i * ((width - padSide * 2) / values.length) + 6;
      const barH = (val / maxVal) * chartHeight;
      const y = height - padBottom - barH;

      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.roundRect(x, y, barWidth, barH, [4, 4, 0, 0]);
      ctx.fill();

      // Label
      ctx.fillStyle = '#5f6368';
      ctx.font = '11px -apple-system, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(labels[i], x + barWidth / 2, height - 6);
    });
  },

  drawLineChart(canvasId, labels, values, strokeColor = '#00875a') {
    const setup = this.setupCanvas(canvasId);
    if (!setup) return;
    const { ctx, width, height } = setup;

    ctx.clearRect(0, 0, width, height);
    if (values.length === 0) return;

    const pad = 24;
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

    // Subtle area
    const areaGrad = ctx.createLinearGradient(0, pad, 0, height - pad);
    areaGrad.addColorStop(0, 'rgba(0, 135, 90, 0.12)');
    areaGrad.addColorStop(1, 'rgba(0, 135, 90, 0.0)');

    ctx.beginPath();
    ctx.moveTo(pts[0].x, height - pad);
    pts.forEach(p => ctx.lineTo(p.x, p.y));
    ctx.lineTo(pts[pts.length - 1].x, height - pad);
    ctx.closePath();
    ctx.fillStyle = areaGrad;
    ctx.fill();

    // Line
    ctx.beginPath();
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].x, pts[i].y);
    }
    ctx.strokeStyle = strokeColor;
    ctx.lineWidth = 2.5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();

    // Data points
    pts.forEach((p, idx) => {
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = strokeColor;
      ctx.lineWidth = 2;
      ctx.stroke();

      ctx.fillStyle = '#5f6368';
      ctx.font = '11px -apple-system, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(labels[idx] || '', p.x, height - 6);
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
      { name: 'Deep', val: deepMin, color: '#1a73e8' },
      { name: 'Light', val: lightMin, color: '#8ab4f8' },
      { name: 'REM', val: remMin, color: '#9334e6' },
      { name: 'Awake', val: awakeMin, color: '#f28b82' }
    ];

    let startX = 12;
    const barWidth = width - 24;
    const barHeight = 20;
    const y = 14;

    stages.forEach(st => {
      const segW = (st.val / total) * barWidth;
      ctx.fillStyle = st.color;
      ctx.beginPath();
      ctx.rect(startX, y, segW, barHeight);
      ctx.fill();
      startX += segW;
    });

    // Legend
    let legendX = 12;
    stages.forEach(st => {
      ctx.fillStyle = st.color;
      ctx.fillRect(legendX, y + 28, 8, 8);
      ctx.fillStyle = '#5f6368';
      ctx.font = '11px -apple-system, sans-serif';
      ctx.textAlign = 'left';
      ctx.fillText(`${st.name}: ${st.val}m`, legendX + 12, y + 36);
      legendX += 120;
    });
  }
};

window.Charts = Charts;
