# ROBO RACER ULTIMATE — HIGH-DETAIL 3D VEHICLE, REAL-TIME PHYSICS, COMPLETE ANIMATION SYSTEM & ANDROID DIGITAL TWIN
## MASTER EXECUTION & DIGITAL TWIN ENGINEERING PLAN

**Document Path:** `docs/finalization/FINAL_DIGITAL_TWIN_PLAN.md`  
**Date:** 2026-10-19  
**Repository:** `https://github.com/Tara1903/rc-car`  
**Working Branch:** `fix/robo-racer-integration-safety`  
**Status:** APPROVED FOR AUTONOMOUS EXECUTION

---

## 1. Executive Summary & Objective

The Robo Racer 4WD project couples an ESP32 embedded robot with hard-real-time motor safety controls, a native Android Jetpack Compose controller UI, and an interactive 3D digital twin rendered via custom OpenGL ES shaders.

This plan specifies the complete technical architecture and autonomous implementation steps to deliver:
1. **High-Detail Editable 3D Vehicle Master Model & Export Pipeline**: Blender 5.2.0 parametric model (`tools/modeling/robo_racer_car.blend`), repeatable script (`tools/modeling/generate_robo_racer_model.py`), and mobile-optimized glTF 2.0 binary (`app/src/main/assets/models/robo_racer_car.glb`).
2. **Real-Time Differential-Drive Ground Physics Engine**: A deterministic, fixed-timestep, pure-Kotlin 2D/3D physics simulator (`com.example.roboracer.physics`) with ground friction, rolling resistance, slip ratios, inertia, and 2D bounding-box obstacle collisions.
3. **Comprehensive Visual & Animation System**: Dynamic 4-wheel independent spin ($\omega = v / r$), chassis pitch squat/dive under acceleration/braking, cornering roll, skid-steer counter-rotation, and emissive brake lights.
4. **Interactive Android 3D Digital Twin Viewer**:
   - Touch-controlled camera: 1-finger orbit (azimuth/elevation), 2-finger pinch zoom, pan, and camera recenter/reset.
   - Two operational modes:
     - **Mode A (Simulation Sandbox)**: Drive offline, navigate an arena with obstacles, switch surface presets (Smooth Floor vs Carpet), pause/resume, reset pose, with all telemetry explicitly tagged `[SIMULATED]`.
     - **Mode B (Live Vehicle Visualization)**: Connected ESP32 mode reflecting real hardware telemetry frames (`[LIVE TELEMETRY]`) and command predictions (`[COMMAND PREDICTION]`), halting visualization on connection loss or failsafe.
5. **19 High-Resolution Engineering Renders**: Detailed documentation views in `docs/hardware/renders/` covering mechanical assembly, electronics closeups (ESP32, L298N, XL6019, TT motors), wiring paths, differential steering, and physics simulation scenarios.
6. **Full Automated Test Suite & Release Verification**: 15 physics unit tests, protocol suite, debug APK compilation, and clean git history.

---

## 2. Authoritative Hardware Inventory & Evidence Classification

Every component and subsystem is tracked under the established 7-tier verification standard:

| Ref ID | Subsystem | Component Description | Manufacturer / Listing Reference | Classification | Evidence & Engineering Rationale |
|---|---|---|---|:---:|---|
| **CTL-01** | Controller | ESP32 DevKit V1 (30-Pin) | NodeMCU-32S / ESP-WROOM-32 | **VERIFIED** / **DOCUMENTED** | Verified from user photo `media_1792340893404_e9ffa99b.png`. 30-pin dual-row header, CP2102 USB bridge, PCB antenna. Firmware mapping in `config.h`. (Physical bench: **NOT TESTED**). |
| **DRV-01** | Drivetrain | L298N Dual H-Bridge Driver (Left) | STMicroelectronics L298N Module | **VERIFIED** / **DOCUMENTED** | Verified from photo `media_1792340893405_6df9107a.png`. Red PCB, Multiwatt15 package, aluminum heatsink, 78M05 regulator. Drives FL & RL motors. (Physical bench: **NOT TESTED**). |
| **DRV-02** | Drivetrain | L298N Dual H-Bridge Driver (Right) | STMicroelectronics L298N Module | **VERIFIED** / **DOCUMENTED** | Identical module. Drives FR & RR motors. Jumpers `5V-EN`, `ENA`, `ENB` removed. (Physical bench: **NOT TESTED**). |
| **ACT-01** | Drivetrain | 4× Yellow TT Geared DC Motors | Zhengke / Standard TT 1:48 | **VERIFIED** / **DOCUMENTED** | Verified from kit photo `media_1792340893363_d467156d.png`. Yellow plastic gearbox canister, dual flat shafts, 3–6V DC. (Physical bench: **NOT TESTED**). |
| **ACT-02** | Drivetrain | 4× Rubber Tires & Yellow Hubs | 66 mm OD × 26 mm width | **VERIFIED** / **DOCUMENTED** | Verified in kit photo. Chevron tread profile, yellow 5-spoke ABS rims. (Physical bench: **NOT TESTED**). |
| **PWR-01** | Power | Adjustable DC-DC Converter | amiciSolar XL6019 / Generic Blue Module | **BLOCKED — REGULATOR NOT SUITABLE OR NOT VERIFIED** | Verified from user photo `media_1792340893405_c057a99d.png` and Flipkart listing. 2× shielded "330" inductors, 3296W trimpot, 2× electrolytic caps. **Critical Block:** XL6019 datasheet mandates $V_{IN} \ge 5.0\text{V}$; under 4-motor inrush, 4× AA alkaline pack sags to $3.8\text{V}-4.5\text{V}$, potentially causing regulator dropout/UVLO and ESP32 brownout. Output connections remain **PROVISIONAL**. |
| **PWR-02** | Power | 4-Cell AA Battery Holder Tray | Standard 4-slot cylindrical tray | **VERIFIED** / **DOCUMENTED** | Verified in kit photo. 4 slots with leaf springs and red/black leads. Intended cells: 4× AA Alkaline (Duracell / Eveready). Nominal 6.0V, 6.4V fresh open-circuit, 4.4V cutoff. (Physical bench: **NOT TESTED**). |
| **MCH-01** | Chassis | 4WD Double-Deck Acrylic Plates | Laser-cut acrylic with kraft paper | **VERIFIED** / **ESTIMATED** | Verified in kit photo. Contoured front/rear bumpers, side wheel wells, cross-shaped cutouts. Dimensions estimated at ~220 × 110 × 3 mm based on standard kit scale. |
| **MCH-02** | Chassis | 6× Brass Standoffs & Fasteners | M3 × 30 mm brass pillars + M3 screws | **VERIFIED** | Visible in kit photo. Establishes 30 mm deck clearance. |
| **MCH-03** | Chassis | 4× Optical Encoder Discs | 20-slot slotted discs | **VERIFIED** | Visible in kit photo. Mounted to inner motor shafts. |
| **SW-00** | Power | Master Power Switch | **NONE AVAILABLE / NONE INSTALLED** | **VERIFIED** | User explicitly confirmed no switch is available. Direct unswitched battery wiring; manual disconnect mandatory. |
| **FUS-00** | Power | Inline 3A Fuse | Mini blade fuse | **DOCUMENTED ONLY (NOT INSTALLED)** | Documented as recommended safety upgrade; NOT modeled as installed hardware. |

---

## 3. Physics Simulation Engine Mathematical Specification

The vehicle physics engine (`com.example.roboracer.physics.VehiclePhysicsEngine`) is a deterministic, 2D/3D planar rigid-body ground dynamics model executing with fixed time step $\Delta t = 0.016\text{s}$ (60 Hz).

### 3.1 Vehicle Dimensions & Mass Properties
- Total vehicle mass: $m = 0.65\,\text{kg}$
- Planar yaw moment of inertia: $I_z = \frac{1}{12} m (L_{chassis}^2 + W_{chassis}^2) \approx 0.0028\,\text{kg}\cdot\text{m}^2$
- Effective track width (lateral axle separation): $B = 0.152\,\text{m}$ (152 mm)
- Wheelbase (longitudinal axle separation): $L = 0.130\,\text{m}$ (130 mm)
- Wheel radius: $r = 0.033\,\text{m}$ (33 mm)
- Gear ratio: $48:1$

### 3.2 Differential Drive Kinematics & Skid-Steer Dynamics
Given left and right wheel surface linear velocities $v_L$ and $v_R$:
$$\text{Chassis Linear Velocity: } v = \frac{v_R + v_L}{2}$$
$$\text{Chassis Yaw Angular Velocity: } \omega = \frac{v_R - v_L}{B}$$

Where individual wheel angular velocities are $\omega_L = \frac{v_L}{r}$ and $\omega_R = \frac{v_R}{r}$.

### 3.3 Motor Torque & Tractive Force Model
Each motor produces torque governed by the motor PWM duty cycle $u \in [-1.0, 1.0]$ and back-EMF limit:
$$\tau_{motor} = \tau_{stall} \cdot \left( u - \frac{\omega_{wheel}}{\omega_{no\_load}} \right)$$
$$F_{drive, side} = 2 \times \frac{\tau_{motor} \cdot \eta_{gear}}{r}$$

### 3.4 Ground Friction & Traction Limits
Maximum tractive force per side before slip occurs is bounded by Coulomb friction:
$$F_{max, side} = \mu \cdot \left(\frac{m \cdot g}{2}\right)$$
If $|F_{drive, side}| > F_{max, side}$, wheel slip occurs:
$$s = \frac{v_{wheel} - v_x}{\max(|v_{wheel}|, |v_x|, 0.01)}$$

**Surface Presets:**
1. **`SMOOTH_FLOOR` (Workshop Tile / Hardwood):**
   - Friction coefficient $\mu = 0.85$
   - Rolling resistance coefficient $C_{rr} = 0.015$
   - High traction, sharp pivot turns, crisp stopping response.
2. **`CARPET` (Medium Pile Carpet):**
   - Friction coefficient $\mu = 0.52$
   - Rolling resistance coefficient $C_{rr} = 0.075$
   - Increased drag, higher wheel slip during pivot turns, damped top speed.

### 3.5 Aerodynamic & Rolling Resistances
$$F_{drag} = -\frac{1}{2} \rho C_d A \cdot v |v| - C_{rr} \cdot m \cdot g \cdot \text{sign}(v)$$
$$\tau_{damp} = -\beta_{rot} \cdot \omega$$

### 3.6 Planar Position Integration (OpenGL Coordinate Frame)
In OpenGL standard view, $-Z$ is forward, $+X$ is right, $+Y$ is up:
$$\dot{x} = v \cdot \sin(\theta)$$
$$\dot{z} = -v \cdot \cos(\theta)$$
$$\dot{\theta} = \omega$$

### 3.7 Chassis Pitch & Roll Dynamics (Suspension Compliance Emulation)
Even with a rigid chassis, tire compliance and weight transfer induce dynamic squat, dive, and body roll:
$$\text{Pitch Angle: } \theta_{pitch} = -K_{pitch} \cdot a_{longitudinal} = -K_{pitch} \cdot \frac{dv}{dt}$$
- Acceleration ($a > 0$): Rear squat (positive nose up / negative rotation).
- Braking ($a < 0$): Front dive (positive nose down).
$$\text{Roll Angle: } \phi_{roll} = -K_{roll} \cdot a_{lateral} = -K_{roll} \cdot (v \cdot \omega)$$
- Cornering into left turn ($\omega > 0$): Outward right roll.

### 3.8 Static Obstacle Collision Model
Static obstacles in the simulation arena are modeled as 2D Axis-Aligned and Oriented Bounding Boxes:
- Arena boundary: $[-2.5, 2.5]\,\text{m} \times [-2.5, 2.5]\,\text{m}$ perimeter walls.
- Interior obstacles: e.g. obstacle crates at $(0.75, -0.75)$, $(-0.8, -1.2)$.
- Vehicle collision boundary: Bounding circle with radius $R_{col} = 0.12\,\text{m}$ or oriented box $0.24\,\text{m} \times 0.17\,\text{m}$.
- Penetration resolution:
  $$\vec{p}_{new} = \vec{p} + \vec{n}_{collision} \cdot \text{depth}$$
  $$\vec{v}_{restituted} = \vec{v}_{\parallel} - e \cdot \vec{v}_{\perp}$$
  where restitution coefficient $e = 0.20$ (inelastic bumper strike).

---

## 4. Interactive 3D Android Viewer Architecture

### 4.1 Spherical Orbit Camera Model
The camera is parameterized in spherical coordinates relative to a focal target point $\vec{T}$:
$$\vec{P}_{cam} = \vec{T} + \begin{bmatrix} R \sin(\phi) \sin(\theta_{cam}) \\ R \cos(\phi) \\ R \sin(\phi) \cos(\theta_{cam}) \end{bmatrix}$$
- Distance $R \in [1.2\,\text{m}, 6.0\,\text{m}]$, default $2.8\,\text{m}$.
- Azimuth $\theta_{cam} \in [-\pi, \pi]$.
- Elevation $\phi \in [15^\circ, 85^\circ]$ (prevents flipping underneath the floor).
- Target $\vec{T}$:
  - In Mode A (Simulation): Follows vehicle center of mass $(x_{veh}, 0.15, z_{veh})$ with smooth interpolation.
  - In Mode B (Live Telemetry): Centered at vehicle origin $(0.0, 0.15, 0.0)$.

### 4.2 Multi-Touch Gesture Handling in Viewport
- **One-Finger Drag:** Increments azimuth $\theta_{cam}$ and elevation $\phi$.
- **Two-Finger Pinch:** Scales distance $R$ (smooth zoom).
- **Two-Finger Drag:** Offsets target position $\vec{T}$ (panning).
- **Reset Button:** Instantly snaps camera back to canonical rear $3/4$ perspective $(R = 2.8\text{m}, \theta = 25^\circ, \phi = 55^\circ)$.

### 4.3 Mode Separation & Telemetry Integrity
1. **Mode A — Simulation Sandbox (`DigitalTwinMode.SIMULATION`):**
   - Active when offline or explicitly enabled.
   - User inputs drive the local `VehiclePhysicsEngine`.
   - Vehicle moves dynamically across the arena floor grid, collides with obstacles.
   - Telemetry indicators explicitly display `[SIMULATED]` in cyan font.
   - Controls available: Surface Preset toggle (Tile vs Carpet), Pause/Resume, Reset Pose.
2. **Mode B — Live Vehicle (`DigitalTwinMode.LIVE_TELEMETRY`):**
   - Active when Bluetooth SPP transport is connected.
   - Vehicle pose remains anchored at center of viewport.
   - Wheel rotations reflect actual reported encoder PWM / telemetry ticks (`leftActualSpeed`, `rightActualSpeed`).
   - If telemetry is delayed, command prediction smoothly estimates wheel spin based on unacknowledged commands (`[PREDICTED]`).
   - If disconnected or failsafe trips, prediction stops immediately (`0 RPM`).
   - Telemetry displays `[LIVE]` in amber/green font.

---

## 5. 19 High-Resolution Engineering Renders Specification

The Blender script `tools/modeling/generate_robo_racer_model.py` renders 19 high-resolution ($1920 \times 1080$) perspectives into `docs/hardware/renders/`:

1. `final_front_three_quarter.png`: Classic isometric engineering hero shot.
2. `final_rear_three_quarter.png`: View highlighting battery tray, heatsinks, wiring channels.
3. `final_top_view.png`: Orthographic top view showing component distribution and clearances.
4. `final_side_view.png`: Orthographic elevation view showing dual-deck spacing and wheel height.
5. `final_bottom_view.png`: Underside view of lower chassis, motor mounts, encoder discs.
6. `final_electronics_closeup.png`: High-angle detailed view of ESP32, L298N, XL6019.
7. `final_wheel_motor_detail.png`: Macro detail of TT motor gearbox, M3 brackets, wheel hub, rubber tread.
8. `final_esp32_detail.png`: Macro detail of 30-pin ESP32 DevKit, pin labels, CP2102 chip, antenna.
9. `final_l298n_detail.png`: Macro detail of L298N driver, aluminum heatsink, screw terminals, jumpers.
10. `final_xl6019_detail.png`: Macro detail of XL6019 board, dual shielded inductors, 3296W trimpot.
11. `final_power_path.png`: Highlighted power routing from 4× AA pack to L298N and provisional converter.
12. `final_gpio_wiring.png`: Highlighted logic jumper lines from ESP32 GPIOs to L298N control headers.
13. `final_motor_connections.png`: Highlighted terminal block wiring to the 4× TT motors.
14. `final_exploded_assembly.png`: Exploded vertical stack showing mechanical assembly order.
15. `final_differential_steering.png`: Skid-steer rotation vectors showing counter-rotation in pivot.
16. `final_physics_forward_motion.png`: Motion visualization with forward acceleration vector and pitch squat.
17. `final_physics_pivot_turn.png`: Dynamic visualization of in-place yaw rate and wheel velocity vectors.
18. `final_physics_collision.png`: Obstacle impact scenario showing bounding box and collision normal.
19. `final_android_viewer.png`: Exact matching viewport perspective rendered with workshop lighting.

---

## 6. Verification & Test Plan

1. **Deterministic Physics Unit Tests (`VehiclePhysicsEngineTest.kt`):**
   - 15 specific test scenarios verifying all kinematic, friction, slip, collision, and mode requirements.
2. **Firmware Protocol Suite:**
   - Run `firmware/tests/test_protocol.exe` (7/7 PASS).
3. **Android Application Build & Test:**
   - Execute `.\gradlew.bat testDebugUnitTest` (all unit and physics tests PASS).
   - Execute `.\gradlew.bat assembleDebug` (compile valid `app-debug.apk`).
4. **Git Delivery:**
   - Verify `git diff --check`.
   - Fast-forward commit and push to `origin/fix/robo-racer-integration-safety`.
