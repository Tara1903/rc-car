# Robo Racer 4WD â€” Dimensions, Clearances & Modeling Assumptions

**Document:** `docs/hardware/dimensions-and-assumptions.md`
**Applicable CAD / Asset:** `tools/modeling/robo_racer_car.blend` / `robo_racer_car.glb`
**Classification System:**
- **VERIFIED:** Supported by manufacturer mechanical drawings or datasheets.
- **MEASURED:** Measured directly from physical hardware with calipers.
- **ESTIMATED:** Deduced from photographs, standard 4WD robot car CAD models, and component standards.
- **UNCONFIRMED:** Parameter requires physical verification upon parcel arrival.

---

## 1. Overall Vehicle Physical Envelope

| Dimension Parameter | Symbol | Value (mm) | Value (m) | Evidence | Engineering Rationale / Notes |
|---|:---:|:---:|:---:|:---:|---|
| **Overall Length** | $L_{\text{veh}}$ | 256.0 mm | 0.256 m | **ESTIMATED** | Front bumper curve to rear bumper curve including chassis overhang |
| **Overall Width** | $W_{\text{veh}}$ | 178.0 mm | 0.178 m | **ESTIMATED** | Outer face of left tires to outer face of right tires ($\pm 89\,\text{mm}$ from centerline) |
| **Overall Height** | $H_{\text{veh}}$ | 83.5 mm | 0.0835 m | **ESTIMATED** | Ground plane to top of L298N aluminum heatsink fins |
| **Wheelbase** | $L_{\text{wb}}$ | 130.0 mm | 0.130 m | **VERIFIED** | Distance between front axle center ($+65\,\text{mm}$) and rear axle center ($-65\,\text{mm}$) |
| **Wheel Track Spacing** | $W_{\text{tr}}$ | 152.0 mm | 0.152 m | **VERIFIED** | Distance between left wheel center ($-76\,\text{mm}$) and right wheel center ($+76\,\text{mm}$) |
| **Ground Clearance (Lower Deck)** | $Z_{\text{gc}}$ | 24.0 mm | 0.024 m | **VERIFIED** | Underside of bottom acrylic deck to ground plane |
| **Deck Separation Height** | $H_{\text{deck}}$ | 30.0 mm | 0.030 m | **VERIFIED** | Established by 6Ã— M3 brass hexagonal standoffs (30.0 mm) |
| **Axle Centerline Height** | $Z_{\text{axle}}$ | 33.0 mm | 0.033 m | **VERIFIED** | Exactly half of 66.0 mm tire outer diameter |

---

## 2. Component Dimensional Breakdown

### 2.1 Chassis Plates & Standoffs
- **Material:** 3.0 mm laser-cut acrylic (plexiglass) covered with brown peel-off kraft paper backing.
- **Lower Deck Plate:** $220.0\,\text{mm} \times 110.0\,\text{mm} \times 3.0\,\text{mm}$ core body, with extended front/rear bumper arcs ($256.0\,\text{mm}$ total length) and side cutouts for wheels.
  - Plate bottom face at $Z = 0.024\,\text{m}$ ($24.0\,\text{mm}$ above ground).
  - Plate top face at $Z = 0.027\,\text{m}$ ($27.0\,\text{mm}$ above ground).
- **Upper Deck Plate:** Identical contour to lower deck.
  - Plate bottom face at $Z = 0.057\,\text{m}$ ($57.0\,\text{mm}$ above ground).
  - Plate top face at $Z = 0.060\,\text{m}$ ($60.0\,\text{mm}$ above ground).
- **Standoff Positions (6Ã— M3 Brass):**
  - Symmetric pairs at $X = \pm 41.8\,\text{mm}$ ($\pm 0.38 \times W_{\text{deck}}$).
  - Along $Y$: Front pair at $Y = +83.6\,\text{mm}$, Middle pair at $Y = 0.0\,\text{mm}$, Rear pair at $Y = -83.6\,\text{mm}$.
  - Hexagonal pillar diameter: $5.5\,\text{mm}$ across flats (M3 thread).

### 2.2 TT DC Geared Motors (4Ã—)
- **Gearbox Body:** Yellow plastic housing $19.0\,\text{mm} \times 46.0\,\text{mm} \times 22.0\,\text{mm}$.
- **DC Motor Canister:** Brushed steel cylindrical can $\varnothing 20.0\,\text{mm} \times 24.0\,\text{mm}$ length, extending inward toward chassis centerline.
- **Output Shaft:** Dual-flat $5.3\,\text{mm}$ shaft ($3.6\,\text{mm}$ across flats), protruding $12.0\,\text{mm}$ outward to engage wheel hub.
- **Mounting Tab:** $3.0\,\text{mm}$ acrylic bracket secured with $2\times\text{M3} \times 30\,\text{mm}$ through-bolts.
- **Center Location:**
  - Front-Left: $X = -49.4\,\text{mm}$, $Y = +65.0\,\text{mm}$, $Z = 34.0\,\text{mm}$
  - Rear-Left: $X = -49.4\,\text{mm}$, $Y = -65.0\,\text{mm}$, $Z = 34.0\,\text{mm}$
  - Front-Right: $X = +49.4\,\text{mm}$, $Y = +65.0\,\text{mm}$, $Z = 34.0\,\text{mm}$
  - Rear-Right: $X = +49.4\,\text{mm}$, $Y = -65.0\,\text{mm}$, $Z = 34.0\,\text{mm}$

### 2.3 Wheels & Tires (4Ã—)
- **Outer Diameter:** $66.0\,\text{mm}$ ($\text{Radius} = 33.0\,\text{mm}$).
- **Tread Width:** $26.0\,\text{mm}$.
- **Tire Rubber:** $4.0\,\text{mm}$ thick black synthetic/natural rubber casing with 18 molded chevron tread lugs ($1.8\,\text{mm}$ height).
- **Rim:** Molded yellow ABS plastic, $\varnothing 45.0\,\text{mm}$, 5 radial spokes, recessed central axle boss.
- **Wheel Pivot Centers:**
  - `front_left_wheel_pivot`: $(-0.076, +0.065, +0.033)\,\text{m}$
  - `front_right_wheel_pivot`: $(+0.076, +0.065, +0.033)\,\text{m}$
  - `rear_left_wheel_pivot`: $(-0.076, -0.065, +0.033)\,\text{m}$
  - `rear_right_wheel_pivot`: $(+0.076, -0.065, +0.033)\,\text{m}$
- **Tire-to-Chassis Clearance:** $8.5\,\text{mm}$ minimum lateral clearance between inside tire shoulder and chassis acrylic edge, preventing wheel rub during suspension vibration or skid turns.

### 2.4 Electronics Layout on Upper Deck (Top Face $Z = 0.060\,\text{m}$)
- **ESP32 DevKit v1:**
  - Dimensions: $52.0\,\text{mm} \times 28.0\,\text{mm} \times 12.0\,\text{mm}$ (including headers).
  - Center: $X = 0.0\,\text{mm}$, $Y = +45.0\,\text{mm}$, $Z = 60.0\,\text{mm}$.
  - Orientation: Micro-USB port faces rear; antenna points forward into free air for optimal RF range.
- **L298N Dual Motor Driver Modules (2Ã—):**
  - Dimensions: $43.5\,\text{mm} \times 43.5\,\text{mm} \times 26.0\,\text{mm}$ (including heatsink).
  - Left Driver (`DRV-01`): $X = -26.0\,\text{mm}$, $Y = -15.0\,\text{mm}$, $Z = 60.0\,\text{mm}$.
  - Right Driver (`DRV-02`): $X = +26.0\,\text{mm}$, $Y = -15.0\,\text{mm}$, $Z = 60.0\,\text{mm}$.
  - Heatsinks are positioned facing the center of the car, providing $14.0\,\text{mm}$ airflow gap between fins and short wire runs to motor terminals on the outer edges.
- **Adjustable DC-DC Converter (`PWR-01`):**
  - Dimensions: $50.0\,\text{mm} \times 25.0\,\text{mm} \times 14.0\,\text{mm}$.
  - Center: $X = 0.0\,\text{mm}$, $Y = -68.0\,\text{mm}$, $Z = 60.0\,\text{mm}$.
  - Orientation: Trimmer screw faces up-right for easy screwdriver calibration.
- **Battery Pack (`PWR-02`):**
  - Mounted on Lower Deck ($Z = 0.027\,\text{m}$) in central bay ($X = 0.0\,\text{mm}$, $Y = -5.0\,\text{mm}$).
  - Dimensions: $55.0\,\text{mm} \times 68.0\,\text{mm} \times 19.0\,\text{mm}$.
  - Centers the heaviest mass low in the chassis to prevent rollover during high-speed pivot turns.

---

## 3. Assumptions & Safety Allowances Table

| Parameter / Item | Assumption Made | Engineering Justification | Safety Margin |
|---|---|---|---|
| Plate Material Finish | Modelled as translucent amber/clear acrylic with yellow glow | Real kit uses clear acrylic with brown protective paper; translucent acrylic looks identical to high-end hobby kits while remaining transparent for component viewing | Non-structural aesthetic assumption |
| Motor Stall Current | Estimated at $0.8\,\text{A} - 1.1\,\text{A}$ per motor at $6.0\,\text{V}$ | Standard 1:48 TT motors draw ~180 mA no-load and ~1.0A stall. Two motors per L298N channel total $\le 2.0\,\text{A}$, matching L298N peak rating | 20 AWG wiring rated for 5A continuous |
| Converter Continuous Output | Estimated at $3.0\,\text{A}$ continuous ($4.0\,\text{A}$ burst) | Dual $33\,\mu\text{H}$ inductors and TO-263 switching IC comfortably supply $3.0\text{A}$ without forced cooling | Firmware limits reverse duty to 63% to mitigate inrush spikes |
| ESP32 Input Voltage | Powered via regulated $5.0\,\text{V}$ bus into VIN pin | ESP32 onboard AMS1117-3.3 regulates 5.0V down to 3.3V cleanly with low dropout ($1.1\,\text{V}$) | Prevents brownouts when motors accelerate |
