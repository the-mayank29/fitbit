/**
 * ============================================================================
 * FITBIT 3D ENGINE - Pure WebGL 3D Biometric Visualizer
 * Zero external libraries, 100% offline, 60 FPS hardware accelerated
 * ============================================================================
 */

class Engine3D {
  constructor(canvasId) {
    this.canvas = document.getElementById(canvasId);
    this.gl = this.canvas.getContext('webgl', { antialias: true, alpha: true }) ||
              this.canvas.getContext('experimental-webgl');

    if (!this.gl) {
      console.warn("WebGL not supported, falling back to 2D canvas");
      return;
    }

    this.mode = 'body'; // 'body', 'heart', 'rings', 'sleep', 'cycle'
    this.autoRotate = true;
    this.rotationSpeed = 0.008;

    // Camera state
    this.cameraDistance = 5.2;
    this.cameraAngleX = 0.15;
    this.cameraAngleY = 0.0;
    this.cameraTarget = [0, 0, 0];
    this.targetCameraDistance = 5.2;
    this.targetAngleX = 0.15;
    this.targetAngleY = 0.0;

    // Mouse interaction
    this.isDragging = false;
    this.isRightDragging = false;
    this.lastMouseX = 0;
    this.lastMouseY = 0;
    this.hoveredPart = null;

    // Animation & metrics state
    this.time = 0;
    this.bpm = 68;
    this.measurements = {
      weight: 62.4,
      height: 168,
      waist: 69.8,
      hips: 95.0,
      chest: 87.0,
      biceps: 28.0,
      thighs: 53.2
    };
    this.activityProgress = { steps: 0.68, cals: 0.70, mins: 0.75 };
    this.sleepData = { deep: 115, light: 260, rem: 105, awake: 15, score: 91 };
    this.cycleData = { day: 11, totalDays: 28, phase: 'FOLLICULAR', isFertile: true };

    this.initShaders();
    this.buildMeshes();
    this.initEvents();
    this.resize();

    window.addEventListener('resize', () => this.resize());
    this.animate = this.animate.bind(this);
    requestAnimationFrame(this.animate);
  }

  resize() {
    if (!this.canvas) return;
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.canvas.width = rect.width * (window.devicePixelRatio || 1);
    this.canvas.height = rect.height * (window.devicePixelRatio || 1);
    if (this.gl) {
      this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
    }
  }

  // ==========================================
  // SHADER PROGRAM INITIALIZATION
  // ==========================================
  initShaders() {
    const gl = this.gl;

    const vsSource = `
      attribute vec3 aPosition;
      attribute vec3 aNormal;
      attribute vec4 aColor;

      uniform mat4 uModel;
      uniform mat4 uView;
      uniform mat4 uProjection;
      uniform mat3 uNormalMatrix;

      varying vec3 vNormal;
      varying vec3 vFragPos;
      varying vec4 vColor;

      void main() {
        vec4 worldPos = uModel * vec4(aPosition, 1.0);
        vFragPos = worldPos.xyz;
        vNormal = normalize(uNormalMatrix * aNormal);
        vColor = aColor;
        gl_Position = uProjection * uView * worldPos;
      }
    `;

    const fsSource = `
      precision mediump float;

      varying vec3 vNormal;
      varying vec3 vFragPos;
      varying vec4 vColor;

      uniform vec3 uLightPos;
      uniform vec3 uLightColor;
      uniform vec3 uViewPos;
      uniform float uGlow;

      void main() {
        // Ambient
        vec3 ambient = 0.28 * vColor.rgb;

        // Diffuse
        vec3 norm = normalize(vNormal);
        vec3 lightDir = normalize(uLightPos - vFragPos);
        float diff = max(dot(norm, lightDir), 0.0);
        vec3 diffuse = diff * uLightColor * vColor.rgb * 0.85;

        // Specular
        vec3 viewDir = normalize(uViewPos - vFragPos);
        vec3 reflectDir = reflect(-lightDir, norm);
        float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
        vec3 specular = vec3(0.4) * spec;

        // Rim Light / Holographic Edge
        float rim = 1.0 - max(dot(viewDir, norm), 0.0);
        vec3 rimLight = vec3(0.0, 0.95, 1.0) * pow(rim, 3.0) * 0.45;

        vec3 finalColor = ambient + diffuse + specular + rimLight + (vColor.rgb * uGlow);
        gl_FragColor = vec4(finalColor, vColor.a);
      }
    `;

    const vertShader = this.compileShader(gl.VERTEX_SHADER, vsSource);
    const fragShader = this.compileShader(gl.FRAGMENT_SHADER, fsSource);

    this.program = gl.createProgram();
    gl.attachShader(this.program, vertShader);
    gl.attachShader(this.program, fragShader);
    gl.linkProgram(this.program);

    if (!gl.getProgramParameter(this.program, gl.LINK_STATUS)) {
      console.error("Shader link error:", gl.getProgramInfoLog(this.program));
      return;
    }

    this.attribs = {
      position: gl.getAttribLocation(this.program, 'aPosition'),
      normal: gl.getAttribLocation(this.program, 'aNormal'),
      color: gl.getAttribLocation(this.program, 'aColor')
    };

    this.uniforms = {
      model: gl.getUniformLocation(this.program, 'uModel'),
      view: gl.getUniformLocation(this.program, 'uView'),
      projection: gl.getUniformLocation(this.program, 'uProjection'),
      normalMatrix: gl.getUniformLocation(this.program, 'uNormalMatrix'),
      lightPos: gl.getUniformLocation(this.program, 'uLightPos'),
      lightColor: gl.getUniformLocation(this.program, 'uLightColor'),
      viewPos: gl.getUniformLocation(this.program, 'uViewPos'),
      glow: gl.getUniformLocation(this.program, 'uGlow')
    };
  }

  compileShader(type, src) {
    const gl = this.gl;
    const shader = gl.createShader(type);
    gl.shaderSource(shader, src);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      console.error("Shader compile error:", gl.getShaderInfoLog(shader));
    }
    return shader;
  }

  // ==========================================
  // PROCEDURAL 3D MESH GENERATION
  // ==========================================
  buildMeshes() {
    this.meshCache = {
      cube: this.createCubeBuffer(),
      cylinder: this.createCylinderBuffer(20),
      sphere: this.createSphereBuffer(16, 16),
      torus: this.createTorusBuffer(24, 16, 1.0, 0.08)
    };
  }

  createCubeBuffer() {
    const p = [
      -1,-1, 1,   1,-1, 1,   1, 1, 1,  -1, 1, 1, // front
      -1,-1,-1,  -1, 1,-1,   1, 1,-1,   1,-1,-1, // back
      -1, 1,-1,  -1, 1, 1,   1, 1, 1,   1, 1,-1, // top
      -1,-1,-1,   1,-1,-1,   1,-1, 1,  -1,-1, 1, // bottom
       1,-1,-1,   1, 1,-1,   1, 1, 1,   1,-1, 1, // right
      -1,-1,-1,  -1,-1, 1,  -1, 1, 1,  -1, 1,-1  // left
    ];
    const n = [
      0,0,1, 0,0,1, 0,0,1, 0,0,1,
      0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,
      0,1,0, 0,1,0, 0,1,0, 0,1,0,
      0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0,
      1,0,0, 1,0,0, 1,0,0, 1,0,0,
      -1,0,0, -1,0,0, -1,0,0, -1,0,0
    ];
    const idx = [
      0,1,2, 0,2,3,    4,5,6, 4,6,7,
      8,9,10, 8,10,11, 12,13,14, 12,14,15,
      16,17,18, 16,18,19, 20,21,22, 20,22,23
    ];
    return this.uploadMesh(p, n, idx);
  }

  createSphereBuffer(latBands, longBands) {
    const p = [], n = [], idx = [];
    for (let lat = 0; lat <= latBands; lat++) {
      const theta = lat * Math.PI / latBands;
      const sinTheta = Math.sin(theta);
      const cosTheta = Math.cos(theta);
      for (let lon = 0; lon <= longBands; lon++) {
        const phi = lon * 2 * Math.PI / longBands;
        const x = Math.cos(phi) * sinTheta;
        const y = cosTheta;
        const z = Math.sin(phi) * sinTheta;
        p.push(x, y, z);
        n.push(x, y, z);
      }
    }
    for (let lat = 0; lat < latBands; lat++) {
      for (let lon = 0; lon < longBands; lon++) {
        const first = (lat * (longBands + 1)) + lon;
        const second = first + longBands + 1;
        idx.push(first, second, first + 1);
        idx.push(second, second + 1, first + 1);
      }
    }
    return this.uploadMesh(p, n, idx);
  }

  createCylinderBuffer(segments) {
    const p = [], n = [], idx = [];
    for (let i = 0; i <= segments; i++) {
      const angle = i * 2 * Math.PI / segments;
      const x = Math.cos(angle);
      const z = Math.sin(angle);
      // top
      p.push(x, 1, z); n.push(x, 0, z);
      // bottom
      p.push(x, -1, z); n.push(x, 0, z);
    }
    for (let i = 0; i < segments; i++) {
      const base = i * 2;
      idx.push(base, base + 1, base + 2);
      idx.push(base + 1, base + 3, base + 2);
    }
    return this.uploadMesh(p, n, idx);
  }

  createTorusBuffer(segmentsR, segmentsr, R, r) {
    const p = [], n = [], idx = [];
    for (let i = 0; i <= segmentsR; i++) {
      const u = i * 2 * Math.PI / segmentsR;
      for (let j = 0; j <= segmentsr; j++) {
        const v = j * 2 * Math.PI / segmentsr;
        const x = (R + r * Math.cos(v)) * Math.cos(u);
        const y = r * Math.sin(v);
        const z = (R + r * Math.cos(v)) * Math.sin(u);
        p.push(x, y, z);
        const nx = Math.cos(v) * Math.cos(u);
        const ny = Math.sin(v);
        const nz = Math.cos(v) * Math.sin(u);
        n.push(nx, ny, nz);
      }
    }
    for (let i = 0; i < segmentsR; i++) {
      for (let j = 0; j < segmentsr; j++) {
        const a = i * (segmentsr + 1) + j;
        const b = (i + 1) * (segmentsr + 1) + j;
        idx.push(a, b, a + 1);
        idx.push(b, b + 1, a + 1);
      }
    }
    return this.uploadMesh(p, n, idx);
  }

  uploadMesh(positions, normals, indices) {
    const gl = this.gl;
    const posBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, posBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STATIC_DRAW);

    const normBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, normBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(normals), gl.STATIC_DRAW);

    const idxBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, idxBuffer);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new Uint16Array(indices), gl.STATIC_DRAW);

    return {
      posBuffer,
      normBuffer,
      idxBuffer,
      count: indices.length
    };
  }

  // ==========================================
  // EVENT LISTENERS & RAYCAST HOVER
  // ==========================================
  initEvents() {
    const c = this.canvas;
    c.addEventListener('mousedown', (e) => {
      this.isDragging = true;
      this.isRightDragging = (e.button === 2);
      this.lastMouseX = e.clientX;
      this.lastMouseY = e.clientY;
    });

    window.addEventListener('mouseup', () => {
      this.isDragging = false;
      this.isRightDragging = false;
    });

    c.addEventListener('contextmenu', (e) => e.preventDefault());

    c.addEventListener('mousemove', (e) => {
      if (this.isDragging) {
        const dx = e.clientX - this.lastMouseX;
        const dy = e.clientY - this.lastMouseY;
        if (this.isRightDragging) {
          // Pan
          this.cameraTarget[0] -= dx * 0.005;
          this.cameraTarget[1] += dy * 0.005;
        } else {
          // Orbit
          this.targetAngleY += dx * 0.008;
          this.targetAngleX = Math.max(-1.4, Math.min(1.4, this.targetAngleX + dy * 0.008));
        }
        this.lastMouseX = e.clientX;
        this.lastMouseY = e.clientY;
      } else {
        this.checkHover(e);
      }
    });

    c.addEventListener('wheel', (e) => {
      e.preventDefault();
      this.targetCameraDistance = Math.max(2.2, Math.min(10.0, this.targetCameraDistance + e.deltaY * 0.005));
    }, { passive: false });

    c.addEventListener('click', (e) => {
      if (this.hoveredPart && window.app) {
        window.app.openMeasurementModal(this.hoveredPart);
      }
    });
  }

  checkHover(e) {
    if (this.mode !== 'body') {
      this.hideTooltip();
      this.hoveredPart = null;
      return;
    }
    const rect = this.canvas.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
    const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);

    // Simple height-based screen projection check
    let part = null;
    let label = "";
    if (Math.abs(x) < 0.25) {
      if (y > 0.08 && y < 0.32) {
        part = "chest";
        label = `Chest: ${this.measurements.chest} cm (Click to log)`;
      } else if (y > -0.15 && y <= 0.08) {
        part = "waist";
        label = `Waist: ${this.measurements.waist} cm (Click to log)`;
      } else if (y > -0.42 && y <= -0.15) {
        part = "hips";
        label = `Hips: ${this.measurements.hips} cm (Click to log)`;
      } else if (y <= -0.42 && y > -0.85) {
        part = "thighs";
        label = `Thighs: ${this.measurements.thighs} cm (Click to log)`;
      }
    } else if (Math.abs(x) >= 0.25 && Math.abs(x) < 0.55 && y > -0.15 && y < 0.25) {
      part = "biceps";
      label = `Biceps: ${this.measurements.biceps} cm (Click to log)`;
    }

    this.hoveredPart = part;
    if (part) {
      this.showTooltip(e.clientX, e.clientY, label);
    } else {
      this.hideTooltip();
    }
  }

  showTooltip(x, y, text) {
    const tip = document.getElementById('hudTooltip');
    if (tip) {
      tip.style.display = 'block';
      tip.style.left = `${x}px`;
      tip.style.top = `${y}px`;
      tip.innerText = text;
    }
  }

  hideTooltip() {
    const tip = document.getElementById('hudTooltip');
    if (tip) tip.style.display = 'none';
  }

  setMode(mode) {
    this.mode = mode;
    // reset camera target and distance smoothly per mode
    if (mode === 'body') {
      this.targetCameraDistance = 5.2;
      this.targetAngleX = 0.15;
      this.cameraTarget = [0, 0.1, 0];
    } else if (mode === 'heart') {
      this.targetCameraDistance = 3.6;
      this.targetAngleX = 0.2;
      this.cameraTarget = [0, 0, 0];
    } else if (mode === 'rings') {
      this.targetCameraDistance = 4.2;
      this.targetAngleX = 0.45;
      this.cameraTarget = [0, 0, 0];
    } else if (mode === 'sleep') {
      this.targetCameraDistance = 3.8;
      this.targetAngleX = 0.25;
      this.cameraTarget = [0, 0, 0];
    } else if (mode === 'cycle') {
      this.targetCameraDistance = 4.0;
      this.targetAngleX = 0.6;
      this.cameraTarget = [0, 0, 0];
    }
  }

  setPreset(preset) {
    switch (preset) {
      case 'front': this.targetAngleY = 0; this.targetAngleX = 0.05; break;
      case 'side': this.targetAngleY = Math.PI / 2; this.targetAngleX = 0.05; break;
      case 'back': this.targetAngleY = Math.PI; this.targetAngleX = 0.05; break;
      case 'torso':
        this.targetAngleY = 0;
        this.targetAngleX = 0.1;
        this.targetCameraDistance = 3.2;
        this.cameraTarget = [0, 0.1, 0];
        break;
      case 'reset':
        this.setMode(this.mode);
        this.targetAngleY = 0;
        break;
    }
  }

  toggleAutoRotate() {
    this.autoRotate = !this.autoRotate;
    return this.autoRotate;
  }

  updateMetrics(data) {
    if (data.latestMeasurement) {
      const m = data.latestMeasurement;
      if (m.weightKg) this.measurements.weight = m.weightKg;
      if (m.heightCm) this.measurements.height = m.heightCm;
      if (m.waistCm) this.measurements.waist = m.waistCm;
      if (m.hipsCm) this.measurements.hips = m.hipsCm;
      if (m.chestCm) this.measurements.chest = m.chestCm;
      if (m.bicepsCm) this.measurements.biceps = m.bicepsCm;
      if (m.thighsCm) this.measurements.thighs = m.thighsCm;
    }
    if (data.latestVital) {
      this.bpm = data.latestVital.heartRateBpm || 68;
    }
    if (data.todaySteps && data.profile) {
      this.activityProgress.steps = Math.min(1.0, data.todaySteps / Math.max(1, data.profile.dailyStepGoal));
    }
    if (data.todayCaloriesBurned && data.profile) {
      this.activityProgress.cals = Math.min(1.0, data.todayCaloriesBurned / Math.max(1, data.profile.dailyCalorieGoal));
    }
    if (data.todayActiveMinutes) {
      this.activityProgress.mins = Math.min(1.0, data.todayActiveMinutes / 60.0);
    }
    if (data.latestSleep) {
      this.sleepData = {
        deep: data.latestSleep.deepMinutes || 90,
        light: data.latestSleep.lightMinutes || 240,
        rem: data.latestSleep.remMinutes || 100,
        awake: data.latestSleep.awakeMinutes || 20,
        score: data.latestSleep.sleepScore || 85
      };
    }
    if (data.cycleStatus) {
      this.cycleData = {
        day: data.cycleStatus.currentCycleDay || 1,
        totalDays: (data.profile && data.profile.cycleLengthDays) || 28,
        phase: data.cycleStatus.currentPhase || 'FOLLICULAR',
        isFertile: data.cycleStatus.isFertileWindow || false
      };
    }
  }

  // ==========================================
  // MAIN ANIMATION & RENDER LOOP
  // ==========================================
  animate() {
    this.time += 0.016;

    // Smooth camera interpolation
    this.cameraDistance += (this.targetCameraDistance - this.cameraDistance) * 0.1;
    this.cameraAngleX += (this.targetAngleX - this.cameraAngleX) * 0.1;
    this.cameraAngleY += (this.targetAngleY - this.cameraAngleY) * 0.1;

    if (this.autoRotate && !this.isDragging) {
      this.targetAngleY += this.rotationSpeed;
    }

    this.render();
    requestAnimationFrame(this.animate);
  }

  render() {
    const gl = this.gl;
    if (!gl) return;

    gl.enable(gl.DEPTH_TEST);
    gl.depthFunc(gl.LEQUAL);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

    gl.clearColor(0.04, 0.06, 0.11, 0.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    gl.useProgram(this.program);

    // Camera setup
    const aspect = this.canvas.width / this.canvas.height;
    const projMatrix = this.perspective(45 * Math.PI / 180, aspect, 0.1, 100.0);

    const eyeX = this.cameraTarget[0] + this.cameraDistance * Math.sin(this.cameraAngleY) * Math.cos(this.cameraAngleX);
    const eyeY = this.cameraTarget[1] + this.cameraDistance * Math.sin(this.cameraAngleX);
    const eyeZ = this.cameraTarget[2] + this.cameraDistance * Math.cos(this.cameraAngleY) * Math.cos(this.cameraAngleX);

    const viewMatrix = this.lookAt([eyeX, eyeY, eyeZ], this.cameraTarget, [0, 1, 0]);

    gl.uniformMatrix4fv(this.uniforms.projection, false, projMatrix);
    gl.uniformMatrix4fv(this.uniforms.view, false, viewMatrix);
    gl.uniform3f(this.uniforms.lightPos, 5.0, 8.0, 6.0);
    gl.uniform3f(this.uniforms.lightColor, 1.0, 1.0, 1.0);
    gl.uniform3f(this.uniforms.viewPos, eyeX, eyeY, eyeZ);

    // Draw Cyber floor grid
    this.drawFloorGrid();

    // Render active 3D mode
    switch (this.mode) {
      case 'body': this.renderBodyScene(); break;
      case 'heart': this.renderHeartScene(); break;
      case 'rings': this.renderRingsScene(); break;
      case 'sleep': this.renderSleepScene(); break;
      case 'cycle': this.renderCycleScene(); break;
    }
  }

  // ==========================================
  // 3D SCENE 1: ANATOMICAL BODY MANNEQUIN
  // Dynamic scaling based on weight/chest/waist/hips
  // ==========================================
  renderBodyScene() {
    // Morph scale factors relative to baseline
    const waistScale = (this.measurements.waist / 70.0) * 0.28;
    const hipsScale = (this.measurements.hips / 95.0) * 0.32;
    const chestScale = (this.measurements.chest / 88.0) * 0.34;
    const armScale = (this.measurements.biceps / 28.0) * 0.08;
    const thighScale = (this.measurements.thighs / 53.0) * 0.13;

    const baseColor = [0.18, 0.35, 0.55, 0.95];
    const highlightColor = [0.0, 0.95, 1.0, 1.0];
    const scanRingColor = [0.0, 0.95, 1.0, 0.6];

    // 1. Head
    this.drawMesh('sphere', [0, 1.65, 0], [0.18, 0.22, 0.18], [0.22, 0.45, 0.7, 0.95]);

    // 2. Neck
    this.drawMesh('cylinder', [0, 1.42, 0], [0.08, 0.08, 0.08], baseColor);

    // 3. Chest / Thorax
    const chestColor = (this.hoveredPart === 'chest') ? highlightColor : [0.15, 0.38, 0.62, 0.95];
    const chestGlow = (this.hoveredPart === 'chest') ? 0.6 : 0.05;
    this.drawMesh('cylinder', [0, 1.15, 0], [chestScale, 0.2, chestScale * 0.75], chestColor, chestGlow);

    // 4. Waist / Abdomen
    const waistColor = (this.hoveredPart === 'waist') ? highlightColor : [0.12, 0.32, 0.55, 0.95];
    const waistGlow = (this.hoveredPart === 'waist') ? 0.6 : 0.05;
    this.drawMesh('cylinder', [0, 0.82, 0], [waistScale, 0.16, waistScale * 0.72], waistColor, waistGlow);

    // 5. Pelvis / Hips
    const hipsColor = (this.hoveredPart === 'hips') ? highlightColor : [0.16, 0.36, 0.6, 0.95];
    const hipsGlow = (this.hoveredPart === 'hips') ? 0.6 : 0.05;
    this.drawMesh('cylinder', [0, 0.54, 0], [hipsScale, 0.16, hipsScale * 0.76], hipsColor, hipsGlow);

    // 6. Shoulders & Arms
    const bicepColor = (this.hoveredPart === 'biceps') ? highlightColor : baseColor;
    const bicepGlow = (this.hoveredPart === 'biceps') ? 0.5 : 0.0;
    // Left arm
    this.drawMesh('sphere', [-0.44, 1.25, 0], [0.1, 0.1, 0.1], baseColor);
    this.drawMesh('cylinder', [-0.47, 0.95, 0], [armScale, 0.22, armScale], bicepColor, bicepGlow);
    this.drawMesh('cylinder', [-0.49, 0.52, 0], [0.065, 0.22, 0.065], baseColor);
    // Right arm
    this.drawMesh('sphere', [0.44, 1.25, 0], [0.1, 0.1, 0.1], baseColor);
    this.drawMesh('cylinder', [0.47, 0.95, 0], [armScale, 0.22, armScale], bicepColor, bicepGlow);
    this.drawMesh('cylinder', [0.49, 0.52, 0], [0.065, 0.22, 0.065], baseColor);

    // 7. Legs & Thighs
    const thighColor = (this.hoveredPart === 'thighs') ? highlightColor : baseColor;
    const thighGlow = (this.hoveredPart === 'thighs') ? 0.5 : 0.0;
    // Left leg
    this.drawMesh('cylinder', [-0.17, 0.12, 0], [thighScale, 0.32, thighScale], thighColor, thighGlow);
    this.drawMesh('cylinder', [-0.17, -0.52, 0], [0.09, 0.34, 0.09], baseColor);
    this.drawMesh('cube', [-0.17, -0.92, 0.06], [0.08, 0.06, 0.16], [0.1, 0.25, 0.45, 0.95]);
    // Right leg
    this.drawMesh('cylinder', [0.17, 0.12, 0], [thighScale, 0.32, thighScale], thighColor, thighGlow);
    this.drawMesh('cylinder', [0.17, -0.52, 0], [0.09, 0.34, 0.09], baseColor);
    this.drawMesh('cube', [0.17, -0.92, 0.06], [0.08, 0.06, 0.16], [0.1, 0.25, 0.45, 0.95]);

    // 8. Holographic Biometric Scan Ring (Animated scanning up and down)
    const scanY = 0.5 + Math.sin(this.time * 2.2) * 0.85;
    this.drawMesh('torus', [0, scanY, 0], [0.55, 0.55, 0.55], scanRingColor, 0.8, [Math.PI / 2, 0, 0]);

    // Marker rings around chest, waist, hips
    this.drawMesh('torus', [0, 1.15, 0], [chestScale * 1.15, chestScale * 1.15, chestScale * 1.15],
      (this.hoveredPart === 'chest') ? [0, 1, 1, 0.9] : [0, 0.8, 1, 0.3], 0.5, [Math.PI / 2, 0, 0]);
    this.drawMesh('torus', [0, 0.82, 0], [waistScale * 1.15, waistScale * 1.15, waistScale * 1.15],
      (this.hoveredPart === 'waist') ? [0, 1, 1, 0.9] : [0, 0.8, 1, 0.3], 0.5, [Math.PI / 2, 0, 0]);
    this.drawMesh('torus', [0, 0.54, 0], [hipsScale * 1.15, hipsScale * 1.15, hipsScale * 1.15],
      (this.hoveredPart === 'hips') ? [0, 1, 1, 0.9] : [0, 0.8, 1, 0.3], 0.5, [Math.PI / 2, 0, 0]);
  }

  // ==========================================
  // 3D SCENE 2: BEATING VITALS HEART
  // Synced with BPM in real time!
  // ==========================================
  renderHeartScene() {
    // Pulse frequency synced with user's BPM
    const bps = Math.max(40, Math.min(200, this.bpm)) / 60.0;
    const pulseCycle = (this.time * bps * Math.PI * 2) % (Math.PI * 2);
    // Double pulse heartbeat rhythm (lub-dub)
    const beat = (Math.sin(pulseCycle) > 0.6) ? 1.18 : (Math.sin(pulseCycle + 0.6) > 0.7 ? 1.09 : 1.0);

    const heartRed = [0.95, 0.15, 0.32, 0.95];
    const aortaRed = [0.75, 0.1, 0.25, 0.95];
    const veinCyan = [0.0, 0.85, 1.0, 0.9];

    // Left Ventricle (Scaled sphere)
    this.drawMesh('sphere', [-0.22 * beat, 0.1 * beat, 0], [0.45 * beat, 0.55 * beat, 0.4 * beat], heartRed, 0.35, [0, 0, 0.3]);
    // Right Ventricle
    this.drawMesh('sphere', [0.22 * beat, 0.1 * beat, 0], [0.42 * beat, 0.52 * beat, 0.38 * beat], heartRed, 0.35, [0, 0, -0.3]);
    // Lower Apex
    this.drawMesh('cylinder', [0, -0.45 * beat, 0], [0.38 * beat, 0.35 * beat, 0.35 * beat], heartRed, 0.4, [0, 0, 0]);

    // Aortic Arch & Arteries (Upper tubes)
    this.drawMesh('cylinder', [-0.15, 0.65, 0.1], [0.12, 0.28, 0.12], aortaRed, 0.2, [0, 0, 0.25]);
    this.drawMesh('cylinder', [0.12, 0.70, -0.05], [0.14, 0.30, 0.14], aortaRed, 0.2, [0, 0, -0.2]);
    // Pulmonary artery / Superior Vena Cava
    this.drawMesh('cylinder', [0.32, 0.62, 0.08], [0.11, 0.24, 0.11], veinCyan, 0.4, [0, 0, -0.35]);

    // Glowing EKG Wave Torus Ring pulsing outwards
    const waveRadius = 1.0 + ((this.time * 1.5) % 1.5);
    const waveAlpha = Math.max(0.0, 1.0 - (waveRadius - 1.0) / 1.5);
    this.drawMesh('torus', [0, 0, 0], [waveRadius, waveRadius, waveRadius], [0.0, 0.95, 1.0, waveAlpha * 0.7], 0.8, [Math.PI / 2, 0, 0]);
  }

  // ==========================================
  // 3D SCENE 3: CONCENTRIC 3D ACTIVITY RINGS
  // Steps, Calories, Active Minutes
  // ==========================================
  renderRingsScene() {
    // Ring 1 (Outer, Cyan): Steps
    const r1 = 1.35;
    const p1 = Math.min(1.0, this.activityProgress.steps);
    this.drawMesh('torus', [0, 0, 0], [r1, r1, r1], [0.0, 0.95, 1.0, 0.9], 0.6, [Math.PI / 2 + 0.2 * Math.sin(this.time), 0, this.time * 0.5]);

    // Ring 2 (Middle, Rose): Calories
    const r2 = 1.05;
    this.drawMesh('torus', [0, 0, 0], [r2, r2, r2], [0.96, 0.25, 0.4, 0.9], 0.6, [Math.PI / 2 + 0.15 * Math.cos(this.time), 0, -this.time * 0.6]);

    // Ring 3 (Inner, Amber): Active Minutes
    const r3 = 0.75;
    this.drawMesh('torus', [0, 0, 0], [r3, r3, r3], [0.96, 0.65, 0.1, 0.9], 0.6, [Math.PI / 2 + 0.1 * Math.sin(this.time), 0, this.time * 0.8]);

    // Central Glowing Wellness Core
    const coreBeat = 0.25 + 0.04 * Math.sin(this.time * 3.0);
    this.drawMesh('sphere', [0, 0, 0], [coreBeat, coreBeat, coreBeat], [1.0, 1.0, 1.0, 0.95], 0.9);

    // Orbiting particle satellites
    for (let i = 0; i < 4; i++) {
      const angle = this.time * 1.5 + (i * Math.PI / 2);
      const px = Math.cos(angle) * 1.55;
      const pz = Math.sin(angle) * 1.55;
      const py = Math.sin(angle * 2.0) * 0.2;
      this.drawMesh('sphere', [px, py, pz], [0.05, 0.05, 0.05], [0.0, 1.0, 0.8, 0.9], 0.9);
    }
  }

  // ==========================================
  // 3D SCENE 4: CELESTIAL SLEEP ORB
  // Deep, Light, REM sleep stages in 3D glowing layers
  // ==========================================
  renderSleepScene() {
    // 1. Deep Sleep Core (Dense Midnight Blue)
    const deepR = 0.55 + 0.02 * Math.sin(this.time * 1.5);
    this.drawMesh('sphere', [0, 0, 0], [deepR, deepR, deepR], [0.15, 0.25, 0.7, 0.95], 0.5);

    // 2. Light Sleep Mantle (Luminescent Cyan)
    const lightR = 0.85 + 0.03 * Math.sin(this.time * 1.0 + 1.0);
    this.drawMesh('sphere', [0, 0, 0], [lightR, lightR, lightR], [0.0, 0.8, 0.95, 0.45], 0.4);

    // 3. REM Sleep Planetary Ring (Violet / Orchid)
    this.drawMesh('torus', [0, 0, 0], [1.3, 1.3, 1.3], [0.75, 0.35, 0.95, 0.85], 0.7, [Math.PI / 3, 0, this.time * 0.4]);

    // 4. Awakening Auroras (Glowing particles)
    for (let i = 0; i < 6; i++) {
      const a = this.time * 0.8 + (i * Math.PI / 3);
      const sx = Math.sin(a) * 1.1;
      const sz = Math.cos(a) * 1.1;
      const sy = Math.cos(a * 2.0) * 0.45;
      this.drawMesh('sphere', [sx, sy, sz], [0.04, 0.04, 0.04], [1.0, 0.8, 0.3, 0.9], 0.8);
    }
  }

  // ==========================================
  // 3D SCENE 5: MENSTRUAL CYCLE WHEEL & LUNAR DISC
  // 4 Phases, current cycle day cursor & fertile glow
  // ==========================================
  renderCycleScene() {
    // Rotating 3D Cycle Base Disc
    this.drawMesh('cylinder', [0, 0, 0], [1.35, 0.04, 1.35], [0.12, 0.16, 0.28, 0.85], 0.1);

    // Segment Ring
    this.drawMesh('torus', [0, 0.05, 0], [1.25, 1.25, 1.25], [0.95, 0.4, 0.6, 0.9], 0.5, [Math.PI / 2, 0, 0]);

    // Fertile Window Arc Highlight (Golden Glow)
    if (this.cycleData.isFertile) {
      const pulseFertile = 0.7 + 0.3 * Math.sin(this.time * 4.0);
      this.drawMesh('torus', [0, 0.08, 0], [1.3, 1.3, 1.3], [1.0, 0.75, 0.1, pulseFertile], 0.8, [Math.PI / 2, 0, 0]);
    }

    // Current Day Cursor Pin (Orbiting around the disc)
    const dayRatio = (this.cycleData.day % this.cycleData.totalDays) / this.cycleData.totalDays;
    const pinAngle = dayRatio * Math.PI * 2;
    const px = Math.cos(pinAngle) * 1.25;
    const pz = Math.sin(pinAngle) * 1.25;

    // Glowing Day Marker Pin
    this.drawMesh('sphere', [px, 0.25, pz], [0.12, 0.12, 0.12], [0.0, 1.0, 0.85, 1.0], 0.9);
    this.drawMesh('cylinder', [px, 0.12, pz], [0.03, 0.12, 0.03], [0.0, 1.0, 0.85, 1.0], 0.7);

    // Central Lunar Orb
    const moonPhase = 0.45 + 0.05 * Math.sin(this.time * 1.5);
    this.drawMesh('sphere', [0, 0.25, 0], [moonPhase, moonPhase, moonPhase], [0.9, 0.92, 1.0, 0.95], 0.6);
  }

  // ==========================================
  // CYBER FLOOR GRID
  // ==========================================
  drawFloorGrid() {
    const gl = this.gl;
    // Circular cyber grid ring
    this.drawMesh('torus', [0, -1.05, 0], [2.2, 2.2, 2.2], [0.0, 0.95, 1.0, 0.15], 0.1, [Math.PI / 2, 0, 0]);
    this.drawMesh('torus', [0, -1.05, 0], [1.5, 1.5, 1.5], [0.0, 0.95, 1.0, 0.12], 0.1, [Math.PI / 2, 0, 0]);
  }

  // ==========================================
  // PRIMITIVE MESH RENDER HELPER
  // ==========================================
  drawMesh(type, pos, scale, color, glow = 0.0, rotation = [0, 0, 0]) {
    const gl = this.gl;
    const mesh = this.meshCache[type];
    if (!mesh) return;

    let m = this.identity();
    m = this.translate(m, pos[0], pos[1], pos[2]);
    if (rotation[0]) m = this.rotateX(m, rotation[0]);
    if (rotation[1]) m = this.rotateY(m, rotation[1]);
    if (rotation[2]) m = this.rotateZ(m, rotation[2]);
    m = this.scale(m, scale[0], scale[1], scale[2]);

    gl.uniformMatrix4fv(this.uniforms.model, false, m);

    const normalMat = this.computeNormalMatrix(m);
    gl.uniformMatrix3fv(this.uniforms.normalMatrix, false, normalMat);
    gl.uniform1f(this.uniforms.glow, glow);

    // Bind Buffers
    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.posBuffer);
    gl.vertexAttribPointer(this.attribs.position, 3, gl.FLOAT, false, 0, 0);
    gl.enableVertexAttribArray(this.attribs.position);

    gl.bindBuffer(gl.ARRAY_BUFFER, mesh.normBuffer);
    gl.vertexAttribPointer(this.attribs.normal, 3, gl.FLOAT, false, 0, 0);
    gl.enableVertexAttribArray(this.attribs.normal);

    // Solid color via vertex attrib (repeat color across all vertices)
    gl.vertexAttrib4fv(this.attribs.color, color);

    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, mesh.idxBuffer);
    gl.drawElements(gl.TRIANGLES, mesh.count, gl.UNSIGNED_SHORT, 0);
  }

  // ==========================================
  // 3D MATRIX MATH
  // ==========================================
  identity() {
    return new Float32Array([
      1,0,0,0,
      0,1,0,0,
      0,0,1,0,
      0,0,0,1
    ]);
  }

  translate(m, x, y, z) {
    const out = new Float32Array(m);
    out[12] = m[0]*x + m[4]*y + m[8]*z + m[12];
    out[13] = m[1]*x + m[5]*y + m[9]*z + m[13];
    out[14] = m[2]*x + m[6]*y + m[10]*z + m[14];
    out[15] = m[3]*x + m[7]*y + m[11]*z + m[15];
    return out;
  }

  scale(m, x, y, z) {
    const out = new Float32Array(m);
    out[0] *= x; out[1] *= x; out[2] *= x; out[3] *= x;
    out[4] *= y; out[5] *= y; out[6] *= y; out[7] *= y;
    out[8] *= z; out[9] *= z; out[10] *= z; out[11] *= z;
    return out;
  }

  rotateX(m, rad) {
    const s = Math.sin(rad), c = Math.cos(rad);
    const out = new Float32Array(m);
    out[4] = m[4]*c + m[8]*s;
    out[5] = m[5]*c + m[9]*s;
    out[6] = m[6]*c + m[10]*s;
    out[7] = m[7]*c + m[11]*s;
    out[8] = m[8]*c - m[4]*s;
    out[9] = m[9]*c - m[5]*s;
    out[10] = m[10]*c - m[6]*s;
    out[11] = m[11]*c - m[7]*s;
    return out;
  }

  rotateY(m, rad) {
    const s = Math.sin(rad), c = Math.cos(rad);
    const out = new Float32Array(m);
    out[0] = m[0]*c - m[8]*s;
    out[1] = m[1]*c - m[9]*s;
    out[2] = m[2]*c - m[10]*s;
    out[3] = m[3]*c - m[11]*s;
    out[8] = m[0]*s + m[8]*c;
    out[9] = m[1]*s + m[9]*c;
    out[10] = m[2]*s + m[10]*c;
    out[11] = m[3]*s + m[11]*c;
    return out;
  }

  rotateZ(m, rad) {
    const s = Math.sin(rad), c = Math.cos(rad);
    const out = new Float32Array(m);
    out[0] = m[0]*c + m[4]*s;
    out[1] = m[1]*c + m[5]*s;
    out[2] = m[2]*c + m[6]*s;
    out[3] = m[3]*c + m[7]*s;
    out[4] = m[4]*c - m[0]*s;
    out[5] = m[5]*c - m[1]*s;
    out[6] = m[6]*c - m[2]*s;
    out[7] = m[7]*c - m[3]*s;
    return out;
  }

  perspective(fovy, aspect, near, far) {
    const f = 1.0 / Math.tan(fovy / 2);
    const nf = 1 / (near - far);
    return new Float32Array([
      f / aspect, 0, 0, 0,
      0, f, 0, 0,
      0, 0, (far + near) * nf, -1,
      0, 0, (2 * far * near) * nf, 0
    ]);
  }

  lookAt(eye, center, up) {
    let z0 = eye[0] - center[0], z1 = eye[1] - center[1], z2 = eye[2] - center[2];
    let len = 1 / Math.hypot(z0, z1, z2);
    z0 *= len; z1 *= len; z2 *= len;

    let x0 = up[1] * z2 - up[2] * z1, x1 = up[2] * z0 - up[0] * z2, x2 = up[0] * z1 - up[1] * z0;
    len = 1 / Math.hypot(x0, x1, x2);
    x0 *= len; x1 *= len; x2 *= len;

    let y0 = z1 * x2 - z2 * x1, y1 = z2 * x0 - z0 * x2, y2 = z0 * x1 - z1 * x0;

    return new Float32Array([
      x0, y0, z0, 0,
      x1, y1, z1, 0,
      x2, y2, z2, 0,
      -(x0 * eye[0] + x1 * eye[1] + x2 * eye[2]),
      -(y0 * eye[0] + y1 * eye[1] + y2 * eye[2]),
      -(z0 * eye[0] + z1 * eye[1] + z2 * eye[2]),
      1
    ]);
  }

  computeNormalMatrix(m) {
    return new Float32Array([
      m[0], m[1], m[2],
      m[4], m[5], m[6],
      m[8], m[9], m[10]
    ]);
  }
}

window.Engine3D = Engine3D;
