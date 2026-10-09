/**
 * ============================================================================
 * FITBIT 3D APPLICATION CONTROLLER
 * Full REST API integration, state management, modal forms & 3D HUD sync
 * ============================================================================
 */

class FitbitApp {
  constructor() {
    this.state = {
      dashboard: null,
      activities: [],
      measurements: [],
      vitals: [],
      nutrition: [],
      sleep: [],
      cycle: null,
      profile: null,
      activeTab: 'overview'
    };

    this.engine3d = null;
    this.selectedSymptoms = new Set();

    this.init();
  }

  async init() {
    // Initialize 3D Engine
    try {
      this.engine3d = new Engine3D('canvas3d');
    } catch (e) {
      console.error("Could not init 3D engine:", e);
    }

    this.bindEvents();
    await this.refreshAll();
  }

  // ==========================================
  // DATA FETCHING & SYNCHRONIZATION
  // ==========================================
  async refreshAll() {
    try {
      const [dashRes, actRes, bmRes, vitRes, nutRes, slpRes, cycRes] = await Promise.all([
        fetch('/api/dashboard').then(r => r.json()),
        fetch('/api/activities').then(r => r.json()),
        fetch('/api/measurements').then(r => r.json()),
        fetch('/api/vitals').then(r => r.json()),
        fetch('/api/nutrition').then(r => r.json()),
        fetch('/api/sleep').then(r => r.json()),
        fetch('/api/cycle').then(r => r.json())
      ]);

      this.state.dashboard = dashRes;
      this.state.activities = actRes || [];
      this.state.measurements = bmRes || [];
      this.state.vitals = vitRes || [];
      this.state.nutrition = nutRes || [];
      this.state.sleep = slpRes || [];
      this.state.cycle = cycRes || {};
      this.state.profile = dashRes.profile;

      this.renderAll();

      if (this.engine3d) {
        this.engine3d.updateMetrics(this.state.dashboard);
      }
    } catch (err) {
      console.error("Failed to load dashboard data:", err);
      this.showToast("Server connection error. Retrying...", "error");
    }
  }

  // ==========================================
  // EVENT BINDINGS
  // ==========================================
  bindEvents() {
    // Tab switching
    document.querySelectorAll('.nav-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        const target = tab.dataset.tab;
        this.switchTab(target);
      });
    });

    // 3D Mode switching
    document.querySelectorAll('.mode-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.mode-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        const mode = btn.dataset.mode;
        if (this.engine3d) {
          this.engine3d.setMode(mode);
        }
      });
    });

    // 3D Controls
    document.querySelectorAll('.ctrl-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        const action = btn.dataset.action;
        if (!this.engine3d) return;

        if (action === 'autorotate') {
          const active = this.engine3d.toggleAutoRotate();
          btn.classList.toggle('active', active);
        } else {
          this.engine3d.setPreset(action);
        }
      });
    });

    // Symptom pills selector in cycle modal
    document.querySelectorAll('.symptom-tag').forEach(tag => {
      tag.addEventListener('click', () => {
        const val = tag.dataset.symptom;
        if (this.selectedSymptoms.has(val)) {
          this.selectedSymptoms.delete(val);
          tag.classList.remove('selected');
        } else {
          this.selectedSymptoms.add(val);
          tag.classList.add('selected');
        }
      });
    });

    // Form Submissions
    document.getElementById('formActivity')?.addEventListener('submit', (e) => this.handleActivitySubmit(e));
    document.getElementById('formMeasurement')?.addEventListener('submit', (e) => this.handleMeasurementSubmit(e));
    document.getElementById('formVital')?.addEventListener('submit', (e) => this.handleVitalSubmit(e));
    document.getElementById('formNutrition')?.addEventListener('submit', (e) => this.handleNutritionSubmit(e));
    document.getElementById('formSleep')?.addEventListener('submit', (e) => this.handleSleepSubmit(e));
    document.getElementById('formCycle')?.addEventListener('submit', (e) => this.handleCycleSubmit(e));
    document.getElementById('formProfile')?.addEventListener('submit', (e) => this.handleProfileSubmit(e));
  }

  switchTab(tabId) {
    this.state.activeTab = tabId;
    document.querySelectorAll('.nav-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('.view-section').forEach(sec => {
      sec.classList.toggle('active', sec.id === `view-${tabId}`);
    });

    // Automatically switch 3D stage mode to match selected tab for amazing visual continuity!
    if (this.engine3d) {
      if (tabId === 'activities') this.set3DMode('rings');
      else if (tabId === 'measurements') this.set3DMode('body');
      else if (tabId === 'vitals') this.set3DMode('heart');
      else if (tabId === 'sleep') this.set3DMode('sleep');
      else if (tabId === 'cycle') this.set3DMode('cycle');
    }

    // Refresh charts on tab switch
    setTimeout(() => this.renderCharts(), 50);
  }

  set3DMode(mode) {
    document.querySelectorAll('.mode-btn').forEach(b => {
      b.classList.toggle('active', b.dataset.mode === mode);
    });
    if (this.engine3d) this.engine3d.setMode(mode);
  }

  // ==========================================
  // RENDERING ALL MODULES
  // ==========================================
  renderAll() {
    this.renderHeader();
    this.renderOverview();
    this.renderActivities();
    this.renderMeasurements();
    this.renderVitals();
    this.renderNutrition();
    this.renderSleep();
    this.renderCycle();
    this.renderCharts();
  }

  renderHeader() {
    const prof = this.state.profile;
    const dash = this.state.dashboard;
    if (prof) {
      document.getElementById('userNameBadge').innerText = prof.name;
    }
    if (dash && dash.wellnessScore !== undefined) {
      document.getElementById('wellnessScoreVal').innerText = `${dash.wellnessScore}/100`;
    }
  }

  renderOverview() {
    const d = this.state.dashboard;
    if (!d) return;

    // Steps
    const stepGoal = (d.profile && d.profile.dailyStepGoal) || 10000;
    const steps = d.todaySteps || 0;
    document.getElementById('cardTodaySteps').innerText = steps.toLocaleString();
    document.getElementById('cardStepGoal').innerText = `Goal: ${stepGoal.toLocaleString()}`;
    const stepPct = Math.min(100, Math.round((steps / stepGoal) * 100));
    document.getElementById('cardStepProgress').style.width = `${stepPct}%`;

    // Calories
    const calGoal = (d.profile && d.profile.dailyCalorieGoal) || 600;
    const cals = d.todayCaloriesBurned || 0;
    document.getElementById('cardTodayCals').innerText = `${cals} kcal`;
    document.getElementById('cardCalGoal').innerText = `Goal: ${calGoal} kcal`;
    const calPct = Math.min(100, Math.round((cals / calGoal) * 100));
    document.getElementById('cardCalProgress').style.width = `${calPct}%`;

    // Latest Vitals
    if (d.latestVital) {
      document.getElementById('cardHeartRate').innerText = `${d.latestVital.heartRateBpm} BPM`;
      document.getElementById('cardBloodPressure').innerText = `${d.latestVital.systolicBp}/${d.latestVital.diastolicBp} mmHg`;
      document.getElementById('cardSpO2').innerText = `${d.latestVital.spO2Percent}%`;
    }

    // Sleep
    if (d.latestSleep) {
      document.getElementById('cardSleepScore').innerText = `${d.latestSleep.sleepScore}/100`;
      const hrs = Math.floor(d.latestSleep.totalMinutes / 60);
      const mins = d.latestSleep.totalMinutes % 60;
      document.getElementById('cardSleepDuration').innerText = `${hrs}h ${mins}m (${d.latestSleep.quality})`;
    }

    // Cycle Status
    if (d.cycleStatus) {
      document.getElementById('cardCyclePhase').innerText = `Day ${d.cycleStatus.currentCycleDay} • ${d.cycleStatus.currentPhase}`;
      document.getElementById('cardCycleFertile').innerText = d.cycleStatus.isFertileWindow ? "✨ Fertile Window" : "Low Fertility";
      document.getElementById('cardCycleNext').innerText = `Next period in ${d.cycleStatus.daysUntilNextPeriod} days`;
    }

    // Nutrition & Hydration
    if (d.todayNutrition) {
      document.getElementById('cardNutrCals').innerText = `${d.todayNutrition.calories} kcal`;
      document.getElementById('cardWaterIntake').innerText = `${d.todayNutrition.waterMl} ml`;
      const waterGoal = (d.profile && d.profile.dailyWaterGoalMl) || 2500;
      const waterPct = Math.min(100, Math.round((d.todayNutrition.waterMl / waterGoal) * 100));
      document.getElementById('cardWaterProgress').style.width = `${waterPct}%`;
    }

    // 3D HUD Sync
    document.getElementById('hudSteps').innerText = steps.toLocaleString();
    document.getElementById('hudBpm').innerText = d.latestVital ? `${d.latestVital.heartRateBpm} BPM` : '68 BPM';
    document.getElementById('hudWeight').innerText = d.latestMeasurement ? `${d.latestMeasurement.weightKg} kg` : '62.4 kg';
    document.getElementById('hudCycle').innerText = d.cycleStatus ? `Day ${d.cycleStatus.currentCycleDay}` : 'Day 11';
  }

  renderActivities() {
    const list = this.state.activities;
    const tbody = document.getElementById('activitiesTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px; color:#64748b;">No activities logged yet. Click "+ Log Workout" to add one.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(a => `
      <tr>
        <td><strong>${escapeHtml(a.name)}</strong></td>
        <td><span class="badge badge-info">${escapeHtml(a.type)}</span></td>
        <td>${a.durationMinutes} mins</td>
        <td>${a.distanceKm > 0 ? a.distanceKm + ' km' : '—'}</td>
        <td><span style="color:var(--accent-rose); font-weight:700;">${a.caloriesBurned} kcal</span></td>
        <td><span class="badge ${a.intensity === 'HIGH' || a.intensity === 'EXTREME' ? 'badge-warning' : 'badge-normal'}">${a.intensity || 'MODERATE'}</span></td>
        <td>
          <button class="action-btn" title="Delete" onclick="window.app.deleteActivity('${a.id}')">
            🗑
          </button>
        </td>
      </tr>
    `).join('');
  }

  renderMeasurements() {
    const list = this.state.measurements;
    const tbody = document.getElementById('measurementsTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:20px; color:#64748b;">No measurements recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(m => `
      <tr>
        <td>${m.timestamp}</td>
        <td><strong>${m.weightKg} kg</strong></td>
        <td>${m.heightCm} cm</td>
        <td><span class="badge ${m.bmiCategory === 'NORMAL' ? 'badge-normal' : 'badge-elevated'}">${m.bmi} (${m.bmiCategory})</span></td>
        <td>${m.bodyFatPercent ? m.bodyFatPercent + '%' : '—'}</td>
        <td>${m.waistCm ? m.waistCm + ' cm' : '—'}</td>
        <td>${m.hipsCm ? m.hipsCm + ' cm' : '—'}</td>
        <td>
          <button class="action-btn" onclick="window.app.deleteMeasurement('${m.id}')">🗑</button>
        </td>
      </tr>
    `).join('');

    // Update highlight cards
    if (list.length > 0) {
      const latest = list[0];
      document.getElementById('bmLatestWeight').innerText = `${latest.weightKg} kg`;
      document.getElementById('bmLatestBmi').innerText = `${latest.bmi} (${latest.bmiCategory})`;
      document.getElementById('bmLatestFat').innerText = latest.bodyFatPercent ? `${latest.bodyFatPercent}%` : '—';
      document.getElementById('bmLatestWhr').innerText = latest.waistToHipRatio ? latest.waistToHipRatio : '—';
    }
  }

  renderVitals() {
    const list = this.state.vitals;
    const tbody = document.getElementById('vitalsTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px; color:#64748b;">No vitals recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(v => `
      <tr>
        <td>${v.timestamp ? v.timestamp.replace('T', ' ').substring(0, 16) : '—'}</td>
        <td><span style="color:var(--accent-rose); font-weight:700;">${v.heartRateBpm} BPM</span></td>
        <td>${v.restingHeartRateBpm ? v.restingHeartRateBpm + ' BPM' : '—'}</td>
        <td><span class="badge ${v.bpCategory === 'NORMAL' ? 'badge-normal' : 'badge-warning'}">${v.systolicBp}/${v.diastolicBp}</span></td>
        <td>${v.spO2Percent}%</td>
        <td>${v.bloodGlucoseMgDl ? v.bloodGlucoseMgDl + ' mg/dL' : '—'}</td>
        <td>
          <button class="action-btn" onclick="window.app.deleteVital('${v.id}')">🗑</button>
        </td>
      </tr>
    `).join('');
  }

  renderNutrition() {
    const list = this.state.nutrition;
    const tbody = document.getElementById('nutritionTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px; color:#64748b;">No meals logged today.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(n => `
      <tr>
        <td><span class="badge badge-purple">${n.mealType}</span></td>
        <td><strong>${escapeHtml(n.foodName)}</strong></td>
        <td>${n.calories} kcal</td>
        <td>${n.proteinGrams}g</td>
        <td>${n.carbsGrams}g</td>
        <td>${n.fatGrams}g</td>
        <td>
          <button class="action-btn" onclick="window.app.deleteNutrition('${n.id}')">🗑</button>
        </td>
      </tr>
    `).join('');
  }

  renderSleep() {
    const list = this.state.sleep;
    const tbody = document.getElementById('sleepTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:20px; color:#64748b;">No sleep sessions logged.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(s => `
      <tr>
        <td>${s.sleepStart ? s.sleepStart.substring(0, 10) : '—'}</td>
        <td>${Math.floor(s.totalMinutes / 60)}h ${s.totalMinutes % 60}m</td>
        <td><span class="badge ${s.sleepScore >= 80 ? 'badge-normal' : 'badge-elevated'}">${s.sleepScore} (${s.quality})</span></td>
        <td>${s.deepMinutes}m (${Math.round((s.deepMinutes/s.totalMinutes)*100)}%)</td>
        <td>${s.remMinutes}m (${Math.round((s.remMinutes/s.totalMinutes)*100)}%)</td>
        <td>${s.efficiencyPercent}%</td>
        <td>
          <button class="action-btn" onclick="window.app.deleteSleep('${s.id}')">🗑</button>
        </td>
      </tr>
    `).join('');

    if (list.length > 0) {
      const top = list[0];
      Charts.drawSleepHypnogram('canvasHypnogram', top.deepMinutes, top.lightMinutes, top.remMinutes, top.awakeMinutes);
    }
  }

  renderCycle() {
    const cyc = this.state.cycle;
    if (!cyc || !cyc.status) return;

    const st = cyc.status;
    document.getElementById('cycleCurrentDay').innerText = `Day ${st.currentCycleDay}`;
    document.getElementById('cyclePhaseName').innerText = st.phaseDescription;
    document.getElementById('cycleFocusAdvice').innerText = st.recommendedFocus;
    document.getElementById('cycleFertilityStatus').innerText = st.isFertileWindow ? "✨ High Fertility Window" : "Low Fertility";
    document.getElementById('cycleNextPeriodDays').innerText = `${st.daysUntilNextPeriod} Days`;

    const phaseTag = document.getElementById('cyclePhasePill');
    phaseTag.className = `phase-pill phase-${st.currentPhase.toLowerCase()}`;
    phaseTag.innerText = st.currentPhase;

    // Render cycle logs table
    const tbody = document.getElementById('cycleTableBody');
    if (tbody && cyc.logs) {
      tbody.innerHTML = cyc.logs.map(c => `
        <tr>
          <td>${c.logDate}</td>
          <td>Day ${c.cycleDay}</td>
          <td><span class="badge badge-purple">${c.phase}</span></td>
          <td>${c.flow || 'NONE'}</td>
          <td>${c.symptoms && c.symptoms.length ? c.symptoms.join(', ') : '—'}</td>
          <td>${c.mood || '—'}</td>
          <td>
            <button class="action-btn" onclick="window.app.deleteCycleLog('${c.id}')">🗑</button>
          </td>
        </tr>
      `).join('');
    }
  }

  renderCharts() {
    // 1. Steps Bar Chart (from activities)
    const recentActs = [...this.state.activities].slice(0, 6).reverse();
    const actLabels = recentActs.map(a => a.name.split(' ')[0]);
    const actSteps = recentActs.map(a => a.steps || (a.caloriesBurned * 12));
    if (actSteps.length > 0) {
      Charts.drawBarChart('canvasStepsChart', actLabels, actSteps, '#00f2fe');
    }

    // 2. Weight Progress Line Chart
    const recentBm = [...this.state.measurements].slice(0, 7).reverse();
    const bmLabels = recentBm.map(m => m.timestamp.substring(5));
    const bmWeights = recentBm.map(m => m.weightKg);
    if (bmWeights.length > 0) {
      Charts.drawLineChart('canvasWeightChart', bmLabels, bmWeights, '#10b981');
    }
  }

  // ==========================================
  // MODAL CONTROLS & FORM SUBMISSION
  // ==========================================
  openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
  }

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
  }

  openMeasurementModal(part) {
    this.openModal('modalMeasurement');
    if (part) {
      document.getElementById('inputMeasNotes').value = `Inspected 3D ${part.toUpperCase()}`;
    }
  }

  async quickAddWater(amountMl) {
    try {
      const payload = {
        mealType: "WATER",
        foodName: `Hydration Refill (${amountMl}ml)`,
        calories: 0,
        proteinGrams: 0,
        carbsGrams: 0,
        fatGrams: 0,
        fiberGrams: 0,
        waterMl: amountMl,
        notes: "Quick Hydration Tracker"
      };
      await fetch('/api/nutrition', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      this.showToast(`+${amountMl}ml logged! Keep hydrating! 💧`, "success");
      await this.refreshAll();
    } catch (e) {
      this.showToast("Failed to log water", "error");
    }
  }

  async handleActivitySubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      name: form.actName.value,
      type: form.actType.value,
      durationMinutes: parseFloat(form.actDuration.value),
      distanceKm: parseFloat(form.actDistance.value) || 0,
      caloriesBurned: parseInt(form.actCalories.value),
      steps: parseInt(form.actSteps.value) || 0,
      avgHeartRate: parseInt(form.actHr.value) || 0,
      intensity: form.actIntensity.value,
      notes: form.actNotes.value
    };

    const res = await fetch('/api/activities', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalActivity');
      form.reset();
      this.showToast("Workout activity logged! 🏃‍♂️", "success");
      await this.refreshAll();
    }
  }

  async handleMeasurementSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      weightKg: parseFloat(form.bmWeight.value),
      heightCm: parseFloat(form.bmHeight.value),
      bodyFatPercent: parseFloat(form.bmBodyFat.value) || 0,
      muscleMassKg: parseFloat(form.bmMuscle.value) || 0,
      chestCm: parseFloat(form.bmChest.value) || 0,
      waistCm: parseFloat(form.bmWaist.value) || 0,
      hipsCm: parseFloat(form.bmHips.value) || 0,
      bicepsCm: parseFloat(form.bmBiceps.value) || 0,
      thighsCm: parseFloat(form.bmThighs.value) || 0,
      notes: form.bmNotes.value
    };

    const res = await fetch('/api/measurements', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalMeasurement');
      form.reset();
      this.showToast("Body measurements & 3D avatar updated! 📐", "success");
      await this.refreshAll();
    }
  }

  async handleVitalSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      heartRateBpm: parseInt(form.vitHr.value),
      restingHeartRateBpm: parseInt(form.vitRestingHr.value) || 0,
      systolicBp: parseInt(form.vitSystolic.value) || 0,
      diastolicBp: parseInt(form.vitDiastolic.value) || 0,
      spO2Percent: parseFloat(form.vitSpO2.value) || 0,
      bloodGlucoseMgDl: parseFloat(form.vitGlucose.value) || 0,
      bodyTempC: parseFloat(form.vitTemp.value) || 0,
      notes: form.vitNotes.value
    };

    const res = await fetch('/api/vitals', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalVital');
      form.reset();
      this.showToast("Vitals saved & 3D heart synced! ❤️", "success");
      await this.refreshAll();
    }
  }

  async handleNutritionSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      mealType: form.nutrMealType.value,
      foodName: form.nutrFood.value,
      calories: parseInt(form.nutrCals.value),
      proteinGrams: parseFloat(form.nutrProtein.value) || 0,
      carbsGrams: parseFloat(form.nutrCarbs.value) || 0,
      fatGrams: parseFloat(form.nutrFat.value) || 0,
      fiberGrams: parseFloat(form.nutrFiber.value) || 0,
      waterMl: parseInt(form.nutrWater.value) || 0,
      notes: form.nutrNotes.value
    };

    const res = await fetch('/api/nutrition', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalNutrition');
      form.reset();
      this.showToast("Meal and nutrition saved! 🥗", "success");
      await this.refreshAll();
    }
  }

  async handleSleepSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      sleepStart: form.slpStart.value,
      sleepEnd: form.slpEnd.value,
      deepMinutes: parseInt(form.slpDeep.value) || 0,
      lightMinutes: parseInt(form.slpLight.value) || 0,
      remMinutes: parseInt(form.slpRem.value) || 0,
      awakeMinutes: parseInt(form.slpAwake.value) || 0,
      notes: form.slpNotes.value
    };

    const res = await fetch('/api/sleep', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalSleep');
      form.reset();
      this.showToast("Sleep session saved & score calculated! 🛌", "success");
      await this.refreshAll();
    }
  }

  async handleCycleSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      logDate: form.cycDate.value,
      cycleDay: parseInt(form.cycDay.value),
      flow: form.cycFlow.value,
      mood: form.cycMood.value,
      basalBodyTempC: parseFloat(form.cycTemp.value) || 0,
      notes: form.cycNotes.value,
      symptoms: Array.from(this.selectedSymptoms)
    };

    const res = await fetch('/api/cycle', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalCycle');
      form.reset();
      this.selectedSymptoms.clear();
      document.querySelectorAll('.symptom-tag').forEach(t => t.classList.remove('selected'));
      this.showToast("Cycle entry logged & ovulation updated! 🌸", "success");
      await this.refreshAll();
    }
  }

  async handleProfileSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      name: form.profName.value,
      age: parseInt(form.profAge.value),
      gender: form.profGender.value,
      heightCm: parseFloat(form.profHeight.value),
      targetWeightKg: parseFloat(form.profWeight.value),
      dailyStepGoal: parseInt(form.profSteps.value),
      dailyCalorieGoal: parseInt(form.profCalories.value),
      dailyWaterGoalMl: parseInt(form.profWater.value),
      cycleLengthDays: parseInt(form.profCycleLength.value),
      periodLengthDays: parseInt(form.profPeriodLength.value)
    };

    const res = await fetch('/api/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalProfile');
      this.showToast("Profile & fitness goals updated! 🎯", "success");
      await this.refreshAll();
    }
  }

  // ==========================================
  // DELETION & RESET HANDLERS
  // ==========================================
  async deleteActivity(id) {
    if (confirm("Delete this activity?")) {
      await fetch(`/api/activities?id=${id}`, { method: 'DELETE' });
      this.showToast("Activity removed", "info");
      await this.refreshAll();
    }
  }

  async deleteMeasurement(id) {
    if (confirm("Delete this body measurement?")) {
      await fetch(`/api/measurements?id=${id}`, { method: 'DELETE' });
      this.showToast("Measurement removed", "info");
      await this.refreshAll();
    }
  }

  async deleteVital(id) {
    if (confirm("Delete this vitals log?")) {
      await fetch(`/api/vitals?id=${id}`, { method: 'DELETE' });
      this.showToast("Vitals log removed", "info");
      await this.refreshAll();
    }
  }

  async deleteNutrition(id) {
    if (confirm("Delete this meal entry?")) {
      await fetch(`/api/nutrition?id=${id}`, { method: 'DELETE' });
      this.showToast("Meal removed", "info");
      await this.refreshAll();
    }
  }

  async deleteSleep(id) {
    if (confirm("Delete this sleep record?")) {
      await fetch(`/api/sleep?id=${id}`, { method: 'DELETE' });
      this.showToast("Sleep record removed", "info");
      await this.refreshAll();
    }
  }

  async deleteCycleLog(id) {
    if (confirm("Delete this cycle log?")) {
      await fetch(`/api/cycle?id=${id}`, { method: 'DELETE' });
      this.showToast("Cycle log removed", "info");
      await this.refreshAll();
    }
  }

  async resetDemoData() {
    if (confirm("Reset application to fresh demo sample data?")) {
      await fetch('/api/reset-demo', { method: 'POST' });
      this.showToast("Demo data reloaded! ✨", "success");
      await this.refreshAll();
    }
  }

  showToast(msg, type = "info") {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerText = msg;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 3500);
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

window.addEventListener('DOMContentLoaded', () => {
  window.app = new FitbitApp();
});
