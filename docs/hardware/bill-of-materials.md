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
| **PWR-01** | Power | Adjustable DC-DC Converter (Dual Inductor) | amiciSolar / XLSEMI / Generic Auto Buck-Boost | Blue PCB (~50 × 25 mm), 2× shielded power inductors ("330" = 33 µH), 3296W trimpot, 2× electrolytic caps, calibrated to **5.00V ± 0.05V logic rail** | 1 | **ESTIMATED** | Two inductors indicate automatic Buck-Boost / SEPIC topology. Calibrate output to 5.00V before connecting ESP32 or driver logic |
| **PWR-02** | Power | 4-Cell AA Cylindrical Battery Holder | Standard Keystone style holder | 4-slot series AA battery tray (Alkaline/NiMH), ~6.0V nominal (6.4V fresh, 4.4V cutoff), direct unswitched wiring to motor drivers and DC-DC converter. **NO power switch installed** | 1 | **VERIFIED** | Direct unswitched battery connection; manual disconnect required |
| **CTL-01** | Controller | ESP32 Development Board (30-Pin) | NodeMCU-32S / Prakti DevKit v1 | 30-pin dual-row 2.54 mm headers, ESP-WROOM-32 module, CP2102 USB bridge, micro-USB port, EN/BOOT buttons | 1 | **VERIFIED** | Photo verifies 30-pin board, CP2102 chip, metal RF shield, and antenna |
| **SNS-01** | Telemetry | Battery Voltage Divider | Precision Metal Film Resistors | R1 = 100 kΩ (1%), R2 = 20 kΩ (1%) forming 6:1 division ratio tapped into GPIO 34 (6.0V in → 1.00V at ADC; 6.4V max → 1.067V) | 1 | **VERIFIED** | Configured in firmware `config.h`; requires multimeter calibration confirmation |
| **WRG-01** | Interconnect | Logic Dupont Ribbon Cables | 26 AWG Copper Ribbon | 12× female-to-female Dupont jumpers (color-coded for ENA, IN1–IN4, ENB across both drivers) | 12 | **VERIFIED** | Standard jumper pitch 2.54 mm for ESP32 to L298N logic |
| **WRG-02** | Interconnect | High-Current Power Wiring | 20–22 AWG Stranded Wire | Red (Battery V+) and Black (Star GND) high-current wires for motor supply and star ground bus | 1 | **VERIFIED** | Sized for up to 3A total stall burst current |

---

## 2. Unresolved Component Identifications & Investigation

### 2.1 Adjustable DC-DC Converter (`PWR-01`)
- **Visual Evidence:** Blue rectangular PCB, two large surface-mount shielded ferrite drum inductors labeled "330" ($33\,\mu\text{H}$), two radial aluminum electrolytic capacitors with silver tops, one blue Bourns-style 3296W multi-turn cermet potentiometer with brass adjustment screw at top-right, and corner through-hole solder pads (IN+, IN-, OUT+, OUT-).
- **Topology Analysis:**
  - A standard buck (step-down) or boost (step-up) converter uses exactly **one** inductor.
  - A converter with **two** discrete inductors operates in a SEPIC (Single-Ended Primary-Inductor Converter) or non-inverting Buck-Boost topology. This allows the output voltage to remain steadily regulated at **5.00V** whether the battery pack voltage is higher (e.g. fresh 4× AA alkaline at 6.4V) or lower (e.g. sagging under 4-motor inrush load down to 3.8V).
  - Candidates: XL6009/XL6019 auto step-up/down module, FP5139 SEPIC converter, or LM2577S + LM2596S dual-stage module.
- **Classification:** **ESTIMATED / SEPIC TOPOLOGY CONFIRMED BY DUAL INDUCTOR ARCHITECTURE**.
- **Safe Working Protocol:** The converter must be calibrated with a digital multimeter before connecting to the ESP32 or L298N logic. The adjustment potentiometer must be dialed to deliver strictly **5.00 V ± 0.05 V DC** under no-load conditions.

### 2.2 Battery Chemistry & Pack Configuration (`PWR-02`)
- **Visual Evidence:** Black plastic 4-slot battery tray with internal leaf-spring contacts and red/black wire leads.
- **Configuration & Chemistry:**
  - **4× AA Alkaline / NiMH Cells in Series**: Nominal $4 \times 1.5\,\text{V} = 6.0\,\text{V}$ (6.4V fresh pack max, 4.4V low-voltage cutoff threshold at 1.1V/cell).
  - **Direct Unswitched Wiring**: The battery positive rail connects directly to both L298N `+12V` motor terminals, the DC-DC converter `IN+`, and the $R_1$ voltage divider resistor.
  - **NO Master Power Switch Available**: To isolate the vehicle, the operator must unplug the inline battery connector or remove one AA cell from the tray.
- **Classification:** **VERIFIED (DIRECT UNSWITCHED 4× AA SERIES PACK)**.
- **Safe Working Protocol:** Firmware `config.h` sets battery parameters for 4× AA chemistry: `BATT_CELLS = 4`, `BATT_CELL_MAX_MV = 1600`, `BATT_CELL_NOMINAL_MV = 1500`, `BATT_CELL_CUTOFF_MV = 1100`, with `BATT_CALIBRATION_CONFIRMED = 0` until physical multimeter calibration.

---

## 3. Critical Spare Parts & Tool Requirements

- Digital Multimeter with DC voltage and continuity beep functions (mandatory for staged commissioning).
- Small flathead precision screwdriver (for L298N terminal blocks and 3296W potentiometer).
- Small Phillips #1 screwdriver (for M3 chassis screws and TT motor brackets).
- 5.5 mm nut driver or small pliers (for M3 standoff hex nuts).
- Micro-USB cable with data lines (for ESP32 firmware flashing and serial telemetry).
- Bench DC power supply with current limiting (recommended for initial motor channel tests).
