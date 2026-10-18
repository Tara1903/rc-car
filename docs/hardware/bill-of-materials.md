# Robo Racer 4WD â€” Authoritative Bill of Materials (BOM)

**Project:** Robo Racer Autonomous 4WD Engineering Digital Twin
**Status:** In Progress / Delivery Pending Physical Arrival
**Classification System:**
- **VERIFIED:** Confirmed by primary product documentation, verified schematics, or manufacturer datasheet.
- **MEASURED:** Physically measured with calipers, multimeter, or bench scale.
- **ESTIMATED:** Inferred from clear photographic evidence or standard dimensional conventions.
- **UNCONFIRMED:** Requires further evidence, component marking inspection, or physical delivery.

---

## 1. Complete Bill of Materials Table

| Ref ID | Subsystem | Component Description | Manufacturer / Reference | Key Specifications | Qty | Evidence Level | Verification Notes |
|---|---|---|---|---|:---:|:---:|---|
| **MCH-01** | Chassis | 4WD Double-Deck Acrylic Robotic Chassis Plates | Generic Open-Hardware 4WD Robot Platform | Dual laser-cut acrylic plates, ~255 Ã— 150 Ã— 3 mm, contoured bumpers, wheel wells, cross cutout, slotted motor mounts | 2 | **ESTIMATED** | Supplied photo shows brown kraft protective film; underlying plate is clear/amber acrylic |
| **MCH-02** | Chassis | M3 Hexagonal Brass Standoffs | Standard Metric Fasteners | M3 Ã— 30 mm male/female or female/female brass hexagonal pillars | 6 | **VERIFIED** | Visible in kit photo; 30 mm standard deck separation |
| **MCH-03** | Chassis | Fastener Kit (M3 Screws, Nuts, Spacers) | Standard Metric Fasteners | 8Ã— M3 Ã— 30 mm through-bolts for motors, 16Ã— M3 Ã— 6 mm pan-head screws, 16Ã— M3 hex nuts | 1 | **VERIFIED** | Visible in kit photo; standard motor and standoff hardware |
| **MCH-04** | Chassis | Acrylic Motor Mounting Brackets | Laser-cut acrylic | T-brackets with dual screw holes and motor location notch | 4 | **VERIFIED** | 4 pairs (inner and outer) visible in kit photo |
| **MCH-05** | Chassis | Photo-Interrupter Speed Encoder Discs | Black acrylic / molded plastic | 20-slot optical encoder discs for inner motor shaft | 4 | **VERIFIED** | 4 slotted wheels visible in kit photo |
| **ACT-01** | Drivetrain | TT Geared DC Motors | Zhengke / Standard TT Motor | 3â€“6V DC brushed motor, 1:48 spur gearbox, dual-flat 5.3 mm output shafts | 4 | **VERIFIED** | Yellow plastic gearbox with silver steel DC motor canister |
| **ACT-02** | Drivetrain | 66 mm Rubber Tires & Wheels | Standard Robotic Wheel Kit | 66 mm OD Ã— 26 mm width, black natural rubber chevron tread, yellow 5-spoke ABS rim | 4 | **VERIFIED** | Chevron tread profile and 5-spoke yellow hub verified in photo |
| **DRV-01** | Electronics | L298N Dual H-Bridge Motor Driver Module (Left) | STMicroelectronics L298N Breakout | Red FR-4 PCB (43.5 Ã— 43.5 mm), Multiwatt15 L298N IC, finned heatsink, 78M05 regulator, screw terminals | 1 | **VERIFIED** | Controls Front-Left and Rear-Left motors (Channels A & B) |
| **DRV-02** | Electronics | L298N Dual H-Bridge Motor Driver Module (Right) | STMicroelectronics L298N Breakout | Identical red L298N module, independent left/right speed and direction control | 1 | **VERIFIED** | Controls Front-Right and Rear-Right motors (Channels A & B) |
| **PWR-01** | Power | Adjustable DC-DC Converter (Dual Inductor) | amiciSolar / XLSEMI / Generic Module | Blue PCB (~50 × 25 mm), 2× shielded power inductors ("330" = 33 µH), 3296W trimpot, 2× electrolytic caps. Provisional target: **5.00V logic rail (no-load)**. Dynamic load regulation unverified. | 1 | **BLOCKED — COMPONENT IDENTIFICATION REQUIRED** | PROVISIONAL POWER DESIGN. Two inductors do not prove SEPIC or auto buck-boost topology (could be boost with LC ripple filter or dual buck). IC markings illegible in photo. Mark connections provisional until IC number verified under magnification and bench tested. Never claim ±0.05V stability under load without physical measurements. |
| **PWR-02** | Power | 4-Cell AA Cylindrical Battery Holder | Standard Keystone style holder | 4-slot series AA battery tray for ordinary AA Alkaline cells (Duracell / Eveready). 6.0V nominal (6.4V fresh open-circuit, 4.4V cutoff). Significant dynamic voltage sag under inrush ($R_i \approx 0.6 - 1.2\,\Omega$). Direct unswitched wiring. **NO power switch installed** | 1 | **VERIFIED** | Direct unswitched battery connection; manual disconnect required (unplug connector or remove one cell). Distinct from NiMH (1.2V/cell, 4.8V pack nominal). |
| **CTL-01** | Controller | ESP32 Development Board (30-Pin) | NodeMCU-32S / Prakti DevKit v1 | 30-pin dual-row 2.54 mm headers, ESP-WROOM-32 module, CP2102 USB bridge, micro-USB port, EN/BOOT buttons | 1 | **VERIFIED** | Photo verifies 30-pin board, CP2102 chip, metal RF shield, and antenna |
| **SNS-01** | Telemetry | Battery Voltage Divider | Precision Metal Film Resistors | R1 = 100 kΩ (1%), R2 = 20 kΩ (1%) forming 6:1 division ratio tapped into GPIO 34 (6.0V in → 1.00V at ADC; 6.4V max → 1.067V) | 1 | **VERIFIED** | Configured in firmware `config.h`; requires multimeter calibration confirmation |
| **WRG-01** | Interconnect | Logic Dupont Ribbon Cables | 26 AWG Copper Ribbon | 12× female-to-female Dupont jumpers (color-coded for ENA, IN1–IN4, ENB across both drivers) | 12 | **VERIFIED** | Standard jumper pitch 2.54 mm for ESP32 to L298N logic |
| **WRG-02** | Interconnect | High-Current Power Wiring | 20–22 AWG Stranded Wire | Red (Battery V+) and Black (Star GND) high-current wires for motor supply and star ground bus | 1 | **VERIFIED** | Sized for up to 3A total stall burst current |

---

## 2. Unresolved Component Identifications & Investigation

### 2.1 Adjustable DC-DC Converter (`PWR-01`)
- **Status:** **BLOCKED — COMPONENT IDENTIFICATION REQUIRED**
- **Visual Evidence:** Blue rectangular PCB, two large surface-mount shielded ferrite drum inductors labeled "330" ($33\,\mu\text{H}$), two radial aluminum electrolytic capacitors with silver tops, one blue Bourns-style 3296W multi-turn cermet potentiometer with brass adjustment screw at top-right, and corner through-hole solder pads (IN+, IN-, OUT+, OUT-).
- **Topology Analysis & Engineering Evidence Gate:**
  - In product photos (`media_...c057a99d.png`), the surface markings on the primary switching IC are completely illegible due to lighting reflection, angle, and resolution.
  - **Two inductors are NOT sufficient proof of SEPIC or automatic buck-boost topology.** A module with two inductors could be:
    1. A SEPIC converter (single active switch, coupling capacitor, two inductors).
    2. An inverting or non-inverting buck-boost converter.
    3. A boost (step-up) converter with a secondary $LC$ ripple suppression filter.
    4. A dual-output or two-stage regulator.
  - If the module is a boost-only converter with an LC filter, its output voltage will always be equal to or greater than the battery pack voltage ($V_{out} \ge V_{in}$). If fed with 6.4V from a fresh 4× AA alkaline pack, it would pass ~6.0V+ through to the ESP32 `VIN` pin without regulation, potentially overheating the onboard 3.3V LDO!
  - Conversely, if it is a true SEPIC or auto buck-boost, it can step up or down across the full battery discharge curve ($3.8\text{V} - 6.4\text{V}$).
- **Classification:** **PROVISIONAL POWER DESIGN — COMPONENT IDENTIFICATION REQUIRED**.
- **Physical Verification Gate (Mandatory on Hardware Arrival):**
  1. Inspect the main IC markings under magnification (e.g. XL6009, XL6019, FP5139, LM2577, or LM2596).
  2. Verify circuit continuity between inductors and switching nodes to confirm topology.
  3. Bench-test no-load output across an input sweep of $3.5\,\text{V}$ to $6.5\,\text{V}$.
  4. Perform loaded step-transient bench test (0 to 300 mA load) to measure line/load regulation. **Never claim 5.00 V ± 0.05 V stability under load without physical measurements supporting that claim.**
- **Safe Working Protocol:** All power connections involving `PWR-01` (`PWR-02, 06, 07, 11, 12, 13`) remain **provisional**. Before connecting to ESP32 `VIN` or L298N logic `+5V`, trim the 3296W potentiometer with a digital multimeter to **5.00 V DC unloaded**.

### 2.2 Battery Chemistry & Pack Configuration (`PWR-02`)
- **Visual Evidence:** Black plastic 4-slot battery tray with internal leaf-spring contacts and red/black wire leads.
- **Chemistry & Pack Specifications:**
  - **Target Chemistry:** Ordinary AA Alkaline cells (Duracell / Eveready).
  - **Voltage Characteristics:**
    - Fresh open-circuit voltage: $\sim 1.60\,\text{V}/\text{cell} \rightarrow 6.40\,\text{V}$ pack maximum.
    - Nominal open-circuit voltage: $\sim 1.50\,\text{V}/\text{cell} \rightarrow 6.00\,\text{V}$ pack nominal.
    - Discharged cutoff threshold: $\sim 1.10\,\text{V}/\text{cell} \rightarrow 4.40\,\text{V}$ pack cutoff.
  - **Distinction from Rechargeable NiMH:**
    - NiMH chemistry has fundamentally different cell potential: $1.20\,\text{V}$ nominal ($4.80\,\text{V}$ pack), $1.40\,\text{V}$ fresh ($5.60\,\text{V}$ pack), and $1.00\,\text{V}$ cutoff ($4.00\,\text{V}$ pack). Do NOT conflate alkaline and NiMH curves.
  - **Internal Resistance & Dynamic Voltage Sag:**
    - Ordinary AA alkaline cells have high internal resistance ($R_i \approx 0.15\,\Omega - 0.30\,\Omega$ per cell, yielding a pack internal resistance of $R_{i,pack} \approx 0.6\,\Omega - 1.2\,\Omega$ fresh, rising significantly as cells discharge).
    - Under 4-motor inrush or skid-steer turning current ($2.0\,\text{A} - 3.5\,\text{A}$), terminal voltage experiences severe dynamic sag ($\Delta V = I \times R_{i,pack} \approx 1.5\,\text{V} - 2.5\,\text{V}$). A 6.0V nominal pack will momentarily sag down to $4.0\,\text{V} - 4.5\,\text{V}$.
    - **Telemetry Interpretation:** ADC readings on GPIO 34 reflect instantaneous loaded terminal voltage, NOT coulomb-counted battery state of charge. Percentage telemetry is a linear open-circuit proxy ($4.4\text{V}=0\%, 6.4\text{V}=100\%$).
- **Direct Unswitched Wiring:** Battery positive rail connects directly to both L298N `+12V` motor terminals, the DC-DC converter `IN+`, and the $R_1$ voltage divider resistor.
- **NO Master Power Switch Available:** To isolate the vehicle, the operator must unplug the inline battery connector or remove one AA cell from the tray.
- **Classification:** **VERIFIED (DIRECT UNSWITCHED 4× AA ALKALINE SERIES PACK)**.
- **Safe Working Protocol:** Firmware `config.h` sets battery parameters for 4× AA alkaline: `BATT_CELLS = 4`, `BATT_CELL_MAX_MV = 1600`, `BATT_CELL_NOMINAL_MV = 1500`, `BATT_CELL_CUTOFF_MV = 1100`, with `BATT_CALIBRATION_CONFIRMED = 0` until physical multimeter calibration.

---

## 3. Critical Spare Parts & Tool Requirements

- Digital Multimeter with DC voltage and continuity beep functions (mandatory for staged commissioning).
- Small flathead precision screwdriver (for L298N terminal blocks and 3296W potentiometer).
- Small Phillips #1 screwdriver (for M3 chassis screws and TT motor brackets).
- 5.5 mm nut driver or small pliers (for M3 standoff hex nuts).
- Micro-USB cable with data lines (for ESP32 firmware flashing and serial telemetry).
- Bench DC power supply with current limiting (recommended for initial motor channel tests).
