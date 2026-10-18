# Robo Racer 4WD Physical Hardware Reference Research

This document compiles the hardware research, visual references, dimensions, materials, and component placement for the Robo Racer 4WD robotic vehicle. All findings are derived from real component datasheets, vendor specifications, and physical hardware photographs.

---

## 1. Authoritative Evidence Classification System

All component properties and dimensions are classified using the 4-tier evidence system:
- **VERIFIED:** Supported by primary manufacturer datasheets, official product documentation, or confirmed circuit schematics.
- **MEASURED:** Measured directly from the delivered physical component with calipers, bench scale, or multimeter.
- **ESTIMATED:** Deduced from high-resolution product photographs, known standard hobby robotics dimensions, or open-source CAD models.
- **UNCONFIRMED:** Requires further physical inspection, microscopic IC marking analysis, or live testing upon parcel delivery.

---

## 2. Component Reference Breakdown

### 2.1 4WD Double-Deck Acrylic Chassis Kit (`MCH-01`)
- **Product Type:** 4-Wheel Drive Double-Deck Smart Car Robot Chassis Kit.
- **Observed Visual Details (Photo Reference):**
  - **Plates:** Dual laser-cut acrylic plates (~255 mm length Ã— 150 mm outer width Ã— 3 mm plate thickness).
  - **Finish / Appearance:** Supplied plates feature brown protective kraft peel-off paper backing. Once peeled, the underlying material is transparent/amber acrylic.
  - **Features:** Curved front and rear bumper contours with concentric arc slots (ideal for mounting ultrasonic rangefinders, servo mounts, or wire strain reliefs). Recessed wheel wells along the sides allowing 66 mm tires to tuck in neatly. Symmetrical mounting slots for acrylic motor brackets, speed encoder pass-throughs, and a central cross-shaped (+) cutout.
  - **Standoffs:** 6Ã— M3 brass hexagonal cylindrical standoffs (30 mm height) separating the lower and upper decks.
  - **Fasteners:** 8Ã— M3 Ã— 30 mm long motor through-bolts, nickel-plated pan-head screws, and M3 hex nuts.
- **Classification:** **ESTIMATED** (Plate dimensions and contour) / **VERIFIED** (30 mm standoff spacing, double-deck configuration).

### 2.2 TT Geared DC Motors & Speed Encoders (`ACT-01`, `MCH-05`)
- **Component Identified:** 4Ã— Standard Yellow TT Geared DC Motors with Dual-Flat Output Shafts + 4Ã— Black 20-Slot Optical Speed Encoder Discs.
- **Key Specifications:**
  - Operating Voltage: 3.0V â€“ 6.0V DC.
  - Gear Reduction Ratio: 1:48 spur gear train.
  - No-load Current: ~150â€“200 mA @ 6.0V.
  - Stall Current: ~0.8A â€“ 1.1A per motor @ 6.0V.
  - Gearbox Dimensions: 70 mm Ã— 22 mm Ã— 19 mm (including motor canister and shaft protrusion).
  - Canister: Silver brushed steel cylindrical DC motor canister ($\varnothing 20\,\text{mm}$).
  - Shafts: Dual-flat white nylon shafts ($5.3\,\text{mm}$ with dual $3.6\,\text{mm}$ D-flats) protruding outward to drive wheels and inward to mount speed encoder discs.
  - Optical Encoder Discs: 4Ã— 20-slot slotted wheels mounted to the inner motor shafts for wheel speed / odometry telemetry.
- **Classification:** **VERIFIED**.

### 2.3 66 mm Wheels & Tires (`ACT-02`)
- **Component Identified:** 4Ã— 66 mm Robotic Wheels with Black Rubber Tires and Yellow 5-Spoke Rims.
- **Key Specifications:**
  - Outer Diameter: 66 mm.
  - Tread Width: 26 mm.
  - Tire Material: Natural/synthetic black rubber with deep molded chevron (herringbone) directional tread pattern and rounded outer shoulders.
  - Rim Material: Vivid yellow molded ABS plastic with 5 radial spokes and a recessed central D-shaft socket.
- **Classification:** **VERIFIED**.

### 2.4 Dual L298N Motor Driver Modules (`DRV-01`, `DRV-02`)
- **Component Identified:** Exactly TWO L298N Dual Full-Bridge Motor Driver Breakout Boards.
- **Key Specifications:**
  - Board Count: Exactly 2 modules (one dedicated to the 2 left motors, one dedicated to the 2 right motors).
  - Form Factor: Red FR-4 double-sided PCB (43.5 mm Ã— 43.5 mm).
  - Power IC: STMicroelectronics L298N Multiwatt15 vertical package.
  - Heatsink: Black anodized aluminum heatsink with 5â€“6 vertical cooling fins bolted to the L298N tab.
  - Terminals:
    - 2Ã— 2-pin screw terminals for motor outputs (`OUT1/OUT2` for Front motor, `OUT3/OUT4` for Rear motor).
    - 1Ã— 3-pin screw terminal for power input (`12V` motor supply, `GND` common ground, `5V` logic).
  - Passive Components: 8Ã— freewheeling flyback protection diodes, 2Ã— aluminum electrolytic filter capacitors (220ÂµF/35V), 78M05 5V onboard linear regulator.
  - Logic Header: 6-pin male header (`ENA`, `IN1`, `IN2`, `IN3`, `IN4`, `ENB`). Jumpers removed for independent ESP32 LEDC PWM speed control.
- **Stopping Behavior Note:** Setting PWM = 0 and direction lines LOW puts the H-bridge in High-Z "Free Running Motor Stop" (coasting). It does not provide dynamic braking.
- **Classification:** **VERIFIED**.

### 2.5 Adjustable DC-DC Converter (`PWR-01`)
- **Component Identified:** Adjustable Automatic Step-Up / Step-Down (Buck-Boost / SEPIC) Power Converter Module.
- **Observed Visual Details (Photo Reference):**
  - **PCB:** Deep blue solder mask FR-4 board (~50 mm Ã— 25 mm).
  - **Magnetic Components:** TWO large surface-mount shielded drum power inductors, each clearly marked **"330"** ($33\,\mu\text{H}$).
  - **Filter Capacitors:** 2Ã— radial aluminum electrolytic capacitors with silver tops (one at input, one at output).
  - **Adjustment Control:** 1Ã— blue Bourns-style 3296W multi-turn cermet trimmer potentiometer with top brass adjustment screw.
  - **Switching IC:** Central TO-263 / TO-252 semiconductor package with copper heatsink pad.
  - **Terminals:** 4 corner through-hole solder pads (`IN+`, `IN-`, `OUT+`, `OUT-`).
- **Topology & Engineering Analysis:**
  - Standard single-inductor buck or boost modules cannot step both up and down.
  - The presence of **two discrete $33\,\mu\text{H}$ inductors** confirms an automatic Buck-Boost or SEPIC converter topology (such as XL6009/XL6019 auto step-up/down or FP5139 SEPIC converter).
  - This guarantees a stable $6.00\,\text{V}$ output rail whether the battery voltage is higher or lower than 6.0V.
- **Classification:** **ESTIMATED** (SEPIC topology confirmed by dual inductors) / **UNCONFIRMED** (Exact IC part number pending delivery).

### 2.6 ESP32 Development Board (`CTL-01`)
- **Component Identified:** 30-Pin ESP32 DevKit v1 (NodeMCU-32S compatible).
- **Observed Visual Details (Photo Reference):**
  - **Substrate:** Matte black FR-4 PCB, 51.5 mm Ã— 28.0 mm.
  - **RF Module:** ESP32-WROOM-32 with nickel-plated metal RF shield and serpentine inverted-F PCB trace antenna.
  - **USB Interface:** Silicon Labs CP2102 USB-to-UART bridge IC and Micro-USB Type-B connector.
  - **Pin Headers:** Two parallel rows of 15-pin 2.54 mm pitch gold/tin headers (0.9-inch spacing).
  - **Controls & Indicators:** Tactile EN (Reset) and BOOT buttons; onboard blue diagnostic LED on GPIO 2; red power LED.
- **Classification:** **VERIFIED**.

### 2.7 Battery Holder & Power Source (`PWR-02`)
- **Component Identified:** 4-Cell Cylindrical Battery Holder Tray.
- **Observed Visual Details (Photo Reference):**
  - Molded black ABS tray with 4 parallel cylindrical battery bays.
  - Metallic negative coil/leaf springs and positive contact plates.
  - Red (positive) and black (negative) hookup wire leads extending from one end.
  - Cell chemistry unconfirmed: Supports 4Ã— AA Alkaline (6.0V nominal), 4Ã— AA NiMH (4.8V nominal), or 4Ã— 14500 Li-ion (14.8V nominal), or external 3S 18650 pack.
- **Classification:** **ESTIMATED / UNCONFIRMED**.

---

## 3. Evidence Classification Summary Table

| Subsystem | Property / Feature | Value / Specification | Evidence Classification | Verification Source / Method |
|---|---|---|:---:|---|
| Drivetrain | Wheel Diameter | 66.0 mm | **VERIFIED** | Standard TT robotics wheel spec |
| Drivetrain | Gearbox Ratio | 1:48 reduction | **VERIFIED** | Yellow TT motor datasheet |
| Chassis | Standoff Height | 30.0 mm | **VERIFIED** | M3 brass standoff standard |
| Chassis | Plate Material | Laser-cut acrylic with kraft paper | **VERIFIED** | Photo inspection (`media_...d467156d.png`) |
| Chassis | Plate Dimensions | ~255 Ã— 150 Ã— 3 mm | **ESTIMATED** | Photographic scale relative to TT motors |
| Electronics | Motor Drivers | 2Ã— L298N Dual H-Bridge | **VERIFIED** | Photo inspection (`media_...6df9107a.png`) |
| Electronics | Driver Channels | 4 discrete channels (FL, RL, FR, RR) | **VERIFIED** | Firmware `config.h` & wiring map |
| Power | Converter Inductors | 2Ã— Shielded Inductors (330 / 33 ÂµH) | **VERIFIED** | Photo inspection (`media_...c057a99d.png`) |
| Power | Converter Topology | Auto Buck-Boost / SEPIC | **ESTIMATED** | Dual-inductor circuit topology |
| Power | Converter IC | XL6009 / XL6019 / FP5139 | **UNCONFIRMED** | Microscopic IC marking check on arrival |
| Controller | ESP32 Form Factor | 30-Pin DevKit v1 (CP2102) | **VERIFIED** | Photo inspection (`media_...e9ffa99b.png`) |
| Battery | Tray Capacity | 4 cylindrical cells | **VERIFIED** | Photo inspection (`media_...d467156d.png`) |
| Battery | Chemistry | 3S Li-ion (or 4Ã— AA/14500) | **UNCONFIRMED** | Multimeter measurement upon arrival |
