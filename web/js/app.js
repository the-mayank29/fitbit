/**
 * ============================================================================
 * GOOGLE FIT MINIMAL CONTROLLER
 * Full REST API integration, Material Design 3 rings, Journal feeds, FAB controls
 * ============================================================================
 */

class FitbitApp {
  constructor() {
    this.state = {
      users: [],
      activeUserId: 'user_1',
      dashboard: null,
      activities: [],
      measurements: [],
      vitals: [],
      nutrition: [],
      sleep: [],
      cycle: null,
      profile: null,
      activeTab: 'home'
    };

    this.isFabOpen = false;
    this.init();
  }

  async init() {
    this.bindEvents();
    await this.loadUsers();
    await this.refreshAll();
  }

  // ==========================================
  // MULTI-USER MANAGEMENT & SYNCHRONIZATION
  // ==========================================
  async loadUsers() {
    try {
      const res = await fetch('/api/users');
      if (res.ok) {
        const data = await res.json();
        this.state.users = data.users || [];
        this.state.activeUserId = data.activeUserId || (this.state.users[0]?.id || 'user_1');
        this.renderUserSelector();
      }
    } catch (err) {
      console.error("Failed to load users:", err);
    }
  }

  renderUserSelector() {
    const sel = document.getElementById('userSelector');
    if (!sel) return;
    sel.innerHTML = this.state.users.map(u =>
      `<option value="${u.id}">${escapeHtml(u.name)}</option>`
    ).join('');
    sel.value = this.state.activeUserId;

    const activeUser = this.state.users.find(u => u.id === this.state.activeUserId);
    if (activeUser) {
      const avatarEl = document.getElementById('avatarInitial');
      if (avatarEl) {
        avatarEl.innerText = activeUser.name ? activeUser.name.charAt(0).toUpperCase() : 'U';
        if (activeUser.avatarColor) {
          avatarEl.style.backgroundColor = activeUser.avatarColor;
        }
      }
    }
  }

  async switchUser(userId) {
    if (!userId || userId === this.state.activeUserId) return;
    try {
      const res = await fetch(`/api/users/switch?id=${encodeURIComponent(userId)}`, {
        method: 'POST'
      });
      if (res.ok) {
        const data = await res.json();
        this.state.activeUserId = userId;
        this.renderUserSelector();
        await this.refreshAll();
        const userName = data.activeUser?.name || "User";
        this.showToast(`Switched account to ${userName}`, "info");
      } else {
        this.showToast("Failed to switch user account", "error");
      }
    } catch (err) {
      console.error("Error switching user:", err);
      this.showToast("Network error switching user", "error");
    }
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
    } catch (err) {
      console.error("Failed to load dashboard data:", err);
      this.showToast("Connection issue with server", "error");
    }
  }

  // ==========================================
  // EVENT BINDINGS
  // ==========================================
  bindEvents() {
    // User Switcher
    const userSel = document.getElementById('userSelector');
    if (userSel) {
      userSel.addEventListener('change', async (e) => {
        await this.switchUser(e.target.value);
      });
    }

    // Tab switching
    document.querySelectorAll('.gf-tab').forEach(tab => {
      tab.addEventListener('click', () => {
        const target = tab.dataset.tab;
        this.switchTab(target);
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
    document.getElementById('formCreateUser')?.addEventListener('submit', (e) => this.handleCreateUserSubmit(e));
  }

  toggleFab() {
    this.isFabOpen = !this.isFabOpen;
    const menu = document.getElementById('fabMenu');
    const btn = document.getElementById('fabBtn');
    if (menu) menu.classList.toggle('active', this.isFabOpen);
    if (btn) btn.classList.toggle('active', this.isFabOpen);
  }

  switchTab(tabId) {
    this.state.activeTab = tabId;
    document.querySelectorAll('.gf-tab').forEach(t => {
      t.classList.toggle('active', t.dataset.tab === tabId);
    });
    document.querySelectorAll('.view-section').forEach(sec => {
      sec.classList.toggle('active', sec.id === `view-${tabId}`);
    });

    // Close FAB if open
    if (this.isFabOpen) this.toggleFab();

    // Re-render charts on tab change
    setTimeout(() => this.renderCharts(), 50);
  }

  // ==========================================
  // RENDERING
  // ==========================================
  renderAll() {
    this.renderHeader();
    this.renderHome();
    this.renderJournal();
    this.renderBody();
    this.renderVitals();
    this.renderNutrition();
    this.renderSleep();
    this.renderCycle();
    this.renderCharts();
  }

  renderHeader() {
    const prof = this.state.profile;
    if (prof) {
      const avatarEl = document.getElementById('avatarInitial');
      if (avatarEl) {
        avatarEl.innerText = prof.name ? prof.name.charAt(0).toUpperCase() : 'U';
        if (prof.avatarColor) {
          avatarEl.style.backgroundColor = prof.avatarColor;
        }
      }
      const badge = document.getElementById('userNameBadge');
      if (badge && prof.name) {
        badge.innerText = prof.name;
      }
      const userSel = document.getElementById('userSelector');
      if (userSel && prof.id) {
        userSel.value = prof.id;
      }
    }
  }

  renderHome() {
    const d = this.state.dashboard;
    if (!d) return;

    const stepGoal = (d.profile && d.profile.dailyStepGoal) || 10000;
    const steps = d.todaySteps || 0;
    const cals = d.todayCaloriesBurned || 0;
    const mins = Math.round(d.todayActiveMinutes || 0);

    // Calculate distance (km) based on steps or activities
    let distKm = 0;
    this.state.activities.forEach(a => { distKm += a.distanceKm || 0; });
    if (distKm === 0 && steps > 0) distKm = Math.round((steps * 0.00075) * 10) / 10;

    // Heart Points calculation (e.g. 1 pt per active min)
    const heartPts = Math.min(100, Math.round(mins * 1.2));

    // Update Hero Ring text
    document.getElementById('heroSteps').innerText = steps.toLocaleString();
    document.getElementById('heroHeartPts').innerText = `${heartPts} Heart Pts`;
    document.getElementById('heroCalories').innerText = cals.toLocaleString();
    document.getElementById('heroActiveMins').innerText = mins;
    document.getElementById('heroDistance').innerText = distKm.toFixed(1);

    // Animate Concentric SVG Rings
    // Steps Ring: circumference = 2 * PI * 66 = 414.69
    const stepCirc = 414.69;
    const stepRatio = Math.min(1.0, steps / stepGoal);
    const ringSteps = document.getElementById('ringSteps');
    if (ringSteps) {
      ringSteps.style.strokeDashoffset = stepCirc * (1 - stepRatio);
    }

    // Heart Points Ring: circumference = 2 * PI * 48 = 301.59
    const heartCirc = 301.59;
    const heartGoal = 50;
    const heartRatio = Math.min(1.0, heartPts / heartGoal);
    const ringHeart = document.getElementById('ringHeart');
    if (ringHeart) {
      ringHeart.style.strokeDashoffset = heartCirc * (1 - heartRatio);
    }

    // Vitals Highlight Card
    if (d.latestVital) {
      const v = d.latestVital;
      document.getElementById('homeHeartRate').innerText = `${v.heartRateBpm} BPM`;
      document.getElementById('homeBpSub').innerText = `Blood pressure: ${v.systolicBp}/${v.diastolicBp} mmHg`;
      document.getElementById('homeVitalCategory').innerText = v.bpCategory || 'Optimal';
      document.getElementById('homeVitalExtra').innerText =
        `Resting: ${v.restingHeartRateBpm || '--'} BPM • SpO2: ${v.spO2Percent}% • Temp: ${v.bodyTempC}°C`;
    }

    // Body Measurements Highlight Card
    if (d.latestMeasurement) {
      const m = d.latestMeasurement;
      document.getElementById('homeWeight').innerText = `${m.weightKg} kg`;
      document.getElementById('homeBmiSub').innerText = `BMI: ${m.bmi} (${m.bmiCategory}) • Body fat: ${m.bodyFatPercent || '--'}%`;
      document.getElementById('homeBmiBadge').innerText = m.bmiCategory || 'Normal';
      document.getElementById('homeMeasurementsSub').innerText =
        `Waist: ${m.waistCm || '--'} cm • Hips: ${m.hipsCm || '--'} cm • WHR: ${m.waistToHipRatio || '--'}`;
    }

    // Sleep Highlight Card
    if (d.latestSleep) {
      const s = d.latestSleep;
      const hrs = Math.floor(s.totalMinutes / 60);
      const minsRem = s.totalMinutes % 60;
      document.getElementById('homeSleepDuration').innerText = `${hrs} hr ${minsRem} min`;
      document.getElementById('homeSleepScore').innerText = `Score ${s.sleepScore}`;
      document.getElementById('homeSleepSub').innerText = `Deep sleep: ${s.deepMinutes}m • REM: ${s.remMinutes}m (${s.quality})`;
    }

    // Nutrition & Hydration Highlight Card
    if (d.todayNutrition) {
      const n = d.todayNutrition;
      document.getElementById('homeNutrCals').innerText = `${n.calories} kcal`;
      document.getElementById('homeNutrMacros').innerText = `Protein: ${n.protein}g • Carbs: ${n.carbs}g • Fat: ${n.fat}g`;
      document.getElementById('homeWaterVal').innerText = `${n.waterMl} ml`;
    }

    // Cycle Highlight Card
    if (d.cycleStatus) {
      const c = d.cycleStatus;
      document.getElementById('homeCycleDay').innerText = `Day ${c.currentCycleDay}`;
      document.getElementById('homeCyclePhaseBadge').innerText = c.currentPhase;
      document.getElementById('homeCycleSub').innerText = `Next period in ${c.daysUntilNextPeriod} days`;
      document.getElementById('homeFertileAlert').innerText = c.isFertileWindow ? "✨ Fertile window is open" : "Low fertility phase";
    }
  }

  renderJournal() {
    const list = this.state.activities;
    const tbody = document.getElementById('activitiesTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:24px; color:#5f6368;">No workouts recorded yet. Tap "+ Log workout" to begin.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(a => `
      <tr>
        <td><strong>${escapeHtml(a.name)}</strong></td>
        <td><span class="gf-badge gf-badge-blue">${escapeHtml(a.type)}</span></td>
        <td>${a.durationMinutes} min</td>
        <td>${a.distanceKm > 0 ? a.distanceKm + ' km' : '—'}</td>
        <td style="color:var(--gf-red); font-weight:700;">${a.caloriesBurned} kcal</td>
        <td><span class="gf-badge ${a.intensity === 'HIGH' || a.intensity === 'EXTREME' ? 'gf-badge-red' : 'gf-badge-green'}">${a.intensity || 'MODERATE'}</span></td>
        <td style="text-align:right;">
          <button class="gf-delete-btn" title="Delete" onclick="window.app.deleteActivity('${a.id}')">✕</button>
        </td>
      </tr>
    `).join('');
  }

  renderBody() {
    const list = this.state.measurements;
    const tbody = document.getElementById('measurementsTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:24px; color:#5f6368;">No body measurements recorded.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(m => `
      <tr>
        <td>${m.timestamp}</td>
        <td><strong>${m.weightKg} kg</strong></td>
        <td>${m.heightCm} cm</td>
        <td><span class="gf-badge ${m.bmiCategory === 'NORMAL' ? 'gf-badge-green' : 'gf-badge-yellow'}">${m.bmi} (${m.bmiCategory})</span></td>
        <td>${m.bodyFatPercent ? m.bodyFatPercent + '%' : '—'}</td>
        <td>${m.waistCm ? m.waistCm + ' cm' : '—'}</td>
        <td>${m.hipsCm ? m.hipsCm + ' cm' : '—'}</td>
        <td style="text-align:right;">
          <button class="gf-delete-btn" onclick="window.app.deleteMeasurement('${m.id}')">✕</button>
        </td>
      </tr>
    `).join('');

    if (list.length > 0) {
      const top = list[0];
      document.getElementById('bmWeightCard').innerText = `${top.weightKg} kg`;
      document.getElementById('bmBmiCard').innerText = `${top.bmi}`;
      document.getElementById('bmFatCard').innerText = top.bodyFatPercent ? `${top.bodyFatPercent}%` : '—';
      document.getElementById('bmWhrCard').innerText = top.waistToHipRatio ? `${top.waistToHipRatio}` : '—';
    }
  }

  renderVitals() {
    const list = this.state.vitals;
    const tbody = document.getElementById('vitalsTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:24px; color:#5f6368;">No vitals recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(v => `
      <tr>
        <td>${v.timestamp ? v.timestamp.replace('T', ' ').substring(0, 16) : '—'}</td>
        <td style="font-weight:700; color:var(--gf-green);">${v.heartRateBpm} BPM</td>
        <td>${v.restingHeartRateBpm ? v.restingHeartRateBpm + ' BPM' : '—'}</td>
        <td><span class="gf-badge ${v.bpCategory === 'NORMAL' ? 'gf-badge-green' : 'gf-badge-red'}">${v.systolicBp}/${v.diastolicBp} mmHg</span></td>
        <td>${v.spO2Percent}%</td>
        <td>${v.bloodGlucoseMgDl ? v.bloodGlucoseMgDl + ' mg/dL' : '—'}</td>
        <td style="text-align:right;">
          <button class="gf-delete-btn" onclick="window.app.deleteVital('${v.id}')">✕</button>
        </td>
      </tr>
    `).join('');
  }

  renderNutrition() {
    const list = this.state.nutrition;
    const tbody = document.getElementById('nutritionTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; padding:24px; color:#5f6368;">No meals logged today.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(n => `
      <tr>
        <td><span class="gf-badge gf-badge-purple">${n.mealType}</span></td>
        <td><strong>${escapeHtml(n.foodName)}</strong></td>
        <td>${n.calories} kcal</td>
        <td>${n.proteinGrams}g</td>
        <td>${n.carbsGrams}g</td>
        <td>${n.fatGrams}g</td>
        <td style="text-align:right;">
          <button class="gf-delete-btn" onclick="window.app.deleteNutrition('${n.id}')">✕</button>
        </td>
      </tr>
    `).join('');
  }

  renderSleep() {
    const list = this.state.sleep;
    const tbody = document.getElementById('sleepTableBody');
    if (!tbody) return;

    if (list.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:24px; color:#5f6368;">No sleep logs recorded.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map(s => `
      <tr>
        <td>${s.sleepStart ? s.sleepStart.substring(0, 10) : '—'}</td>
        <td>${Math.floor(s.totalMinutes / 60)}h ${s.totalMinutes % 60}m</td>
        <td><span class="gf-badge ${s.sleepScore >= 80 ? 'gf-badge-green' : 'gf-badge-yellow'}">${s.sleepScore}</span></td>
        <td>${s.deepMinutes}m</td>
        <td>${s.lightMinutes}m</td>
        <td>${s.remMinutes}m</td>
        <td>${s.efficiencyPercent}%</td>
        <td style="text-align:right;">
          <button class="gf-delete-btn" onclick="window.app.deleteSleep('${s.id}')">✕</button>
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
    document.getElementById('cycleStatusBadge').innerText = st.currentPhase;
    document.getElementById('cycleDayHeadline').innerText = `Day ${st.currentCycleDay} • ${st.phaseDescription}`;
    document.getElementById('cycleAdviceHeadline').innerText = st.recommendedFocus;
    document.getElementById('cycleDaysToNext').innerText = `${st.daysUntilNextPeriod} Days`;

    const tbody = document.getElementById('cycleTableBody');
    if (tbody && cyc.logs) {
      tbody.innerHTML = cyc.logs.map(c => `
        <tr>
          <td>${c.logDate}</td>
          <td>Day ${c.cycleDay}</td>
          <td><span class="gf-badge gf-badge-purple">${c.phase}</span></td>
          <td>${c.flow || 'None'}</td>
          <td>${c.symptoms && c.symptoms.length ? c.symptoms.join(', ') : '—'}</td>
          <td>${c.mood || '—'}</td>
          <td style="text-align:right;">
            <button class="gf-delete-btn" onclick="window.app.deleteCycleLog('${c.id}')">✕</button>
          </td>
        </tr>
      `).join('');
    }
  }

  renderCharts() {
    // Weekly Steps Bar Chart
    const recentActs = [...this.state.activities].slice(0, 7).reverse();
    const actLabels = recentActs.map(a => a.name.split(' ')[0]);
    const actSteps = recentActs.map(a => a.steps || (a.caloriesBurned * 12));
    if (actSteps.length > 0) {
      Charts.drawBarChart('canvasStepsChart', actLabels, actSteps, '#1a73e8');
    }

    // Weight Progress Line Chart
    const recentBm = [...this.state.measurements].slice(0, 7).reverse();
    const bmLabels = recentBm.map(m => m.timestamp.substring(5));
    const bmWeights = recentBm.map(m => m.weightKg);
    if (bmWeights.length > 0) {
      Charts.drawLineChart('canvasWeightChart', bmLabels, bmWeights, '#00875a');
    }
  }

  // ==========================================
  // MODALS & QUICK ACTIONS
  // ==========================================
  openModal(modalId) {
    if (modalId === 'modalProfile' && this.state.profile) {
      const p = this.state.profile;
      const f = document.getElementById('formProfile');
      if (f) {
        if (f.profName && p.name) f.profName.value = p.name;
        if (f.profEmail && p.email) f.profEmail.value = p.email;
        if (f.profAge && p.age) f.profAge.value = p.age;
        if (f.profGender && p.gender) f.profGender.value = p.gender;
        if (f.profHeight && p.heightCm) f.profHeight.value = p.heightCm;
        if (f.profWeight && p.targetWeightKg) f.profWeight.value = p.targetWeightKg;
        if (f.profSteps && p.dailyStepGoal) f.profSteps.value = p.dailyStepGoal;
        if (f.profCalories && p.dailyCalorieGoal) f.profCalories.value = p.dailyCalorieGoal;
        if (f.profWater && p.dailyWaterGoalMl) f.profWater.value = p.dailyWaterGoalMl;
        if (f.profColor && p.avatarColor) f.profColor.value = p.avatarColor;
      }
    }
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add('active');
  }

  closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove('active');
  }

  async quickAddWater(amountMl) {
    try {
      const payload = {
        mealType: "WATER",
        foodName: `Hydration (${amountMl}ml)`,
        calories: 0,
        proteinGrams: 0,
        carbsGrams: 0,
        fatGrams: 0,
        fiberGrams: 0,
        waterMl: amountMl,
        notes: "Quick Water Add"
      };
      await fetch('/api/nutrition', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      this.showToast(`+${amountMl}ml added to water intake`);
      await this.refreshAll();
    } catch (e) {
      this.showToast("Could not record water", "error");
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
      this.showToast("Workout added to Journal");
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
      this.showToast("Body metrics updated");
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
      bodyTempC: parseFloat(form.vitTemp.value) || 0
    };

    const res = await fetch('/api/vitals', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalVital');
      form.reset();
      this.showToast("Vitals saved");
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
      waterMl: parseInt(form.nutrWater.value) || 0
    };

    const res = await fetch('/api/nutrition', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalNutrition');
      form.reset();
      this.showToast("Meal recorded");
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
      awakeMinutes: parseInt(form.slpAwake.value) || 0
    };

    const res = await fetch('/api/sleep', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalSleep');
      form.reset();
      this.showToast("Sleep session saved");
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
      basalBodyTempC: parseFloat(form.cycTemp.value) || 0
    };

    const res = await fetch('/api/cycle', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalCycle');
      form.reset();
      this.showToast("Cycle entry recorded");
      await this.refreshAll();
    }
  }

  async handleProfileSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      name: form.profName.value.trim(),
      email: form.profEmail ? form.profEmail.value.trim() : "",
      age: parseInt(form.profAge.value),
      gender: form.profGender.value,
      heightCm: parseFloat(form.profHeight.value),
      targetWeightKg: parseFloat(form.profWeight.value),
      dailyStepGoal: parseInt(form.profSteps.value),
      dailyCalorieGoal: parseInt(form.profCalories.value),
      dailyWaterGoalMl: parseInt(form.profWater.value),
      avatarColor: form.profColor ? form.profColor.value : "#1a73e8"
    };

    const res = await fetch('/api/profile', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    });

    if (res.ok) {
      this.closeModal('modalProfile');
      this.showToast("Profile updated");
      await this.loadUsers();
      await this.refreshAll();
    }
  }

  async handleCreateUserSubmit(e) {
    e.preventDefault();
    const form = e.target;
    const data = {
      name: form.newUserName.value.trim(),
      email: form.newUserEmail.value.trim(),
      age: parseInt(form.newUserAge.value) || 28,
      gender: form.newUserGender.value,
      heightCm: parseFloat(form.newUserHeight.value) || 170.0,
      targetWeightKg: parseFloat(form.newUserWeight.value) || 70.0,
      dailyStepGoal: parseInt(form.newUserSteps.value) || 10000,
      dailyCalorieGoal: parseInt(form.newUserCalories.value) || 650,
      dailyWaterGoalMl: parseInt(form.newUserWater.value) || 2500,
      avatarColor: form.newUserColor ? form.newUserColor.value : "#1a73e8"
    };

    try {
      const res = await fetch('/api/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });

      if (res.ok) {
        const created = await res.json();
        this.closeModal('modalCreateUser');
        form.reset();
        await this.loadUsers();
        await this.switchUser(created.id);
        this.showToast(`Account created for ${created.name}!`, "info");
      } else {
        this.showToast("Failed to create user account", "error");
      }
    } catch (err) {
      console.error("Error creating user:", err);
      this.showToast("Network error creating user", "error");
    }
  }

  // Deletions
  async deleteActivity(id) {
    if (confirm("Delete this workout?")) {
      await fetch(`/api/activities?id=${id}`, { method: 'DELETE' });
      this.showToast("Workout deleted");
      await this.refreshAll();
    }
  }

  async deleteMeasurement(id) {
    if (confirm("Delete this measurement?")) {
      await fetch(`/api/measurements?id=${id}`, { method: 'DELETE' });
      this.showToast("Measurement deleted");
      await this.refreshAll();
    }
  }

  async deleteVital(id) {
    if (confirm("Delete this vitals record?")) {
      await fetch(`/api/vitals?id=${id}`, { method: 'DELETE' });
      this.showToast("Vitals record deleted");
      await this.refreshAll();
    }
  }

  async deleteNutrition(id) {
    if (confirm("Delete this meal?")) {
      await fetch(`/api/nutrition?id=${id}`, { method: 'DELETE' });
      this.showToast("Meal deleted");
      await this.refreshAll();
    }
  }

  async deleteSleep(id) {
    if (confirm("Delete this sleep record?")) {
      await fetch(`/api/sleep?id=${id}`, { method: 'DELETE' });
      this.showToast("Sleep record deleted");
      await this.refreshAll();
    }
  }

  async deleteCycleLog(id) {
    if (confirm("Delete this cycle log?")) {
      await fetch(`/api/cycle?id=${id}`, { method: 'DELETE' });
      this.showToast("Cycle record deleted");
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
      setTimeout(() => toast.remove(), 250);
    }, 2800);
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
