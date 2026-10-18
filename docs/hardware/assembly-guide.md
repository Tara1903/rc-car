# Robo Racer 4WD â€” Mechanical & Electrical Assembly Guide

**Document:** `docs/hardware/assembly-guide.md`
**Chassis:** 4WD Double-Deck Acrylic Robotic Platform
**Target Architecture:** Dual L298N Drivers, 4Ã— TT Motors, 30-Pin ESP32 DevKit v1, Dual-Inductor Buck-Boost Converter
**Document Status:** Complete Engineering Build Manual

---

## 1. Tool & Workspace Prerequisites

Before beginning assembly, verify the following bench tools are at hand:
- **Precision Screwdrivers:** Phillips #1 (for M3 chassis/motor bolts) and Flathead 2.5 mm (for screw terminal blocks and potentiometer trimming).
- **Nut Driver or Pliers:** 5.5 mm socket or needle-nose pliers (for M3 hexagonal standoff nuts).
- **Digital Multimeter (DMM):** Capable of measuring DC Volts ($0 - 20\,\text{V}$) and Continuity (acoustic beeper).
- **Anti-Static Mat & Clean Surface:** Avoid working directly on synthetic rugs or carpets.
- **Wire Stripper & Flush Cutters:** (If customizing motor lead lengths).

---

## 2. Mechanical Assembly Procedure (Step-by-Step)

### Step 1: Chassis Plate Preparation & Protective Film Removal
- **Components:** 2Ã— Laser-cut acrylic plates (`MCH-01`).
- **Action:**
  1. Inspect both acrylic plates for laser cutting residue or shipping hairline fractures.
  2. Peel away the brown protective kraft paper from both sides of each plate. The underlying material is crystal-clear transparent acrylic (or amber).
  3. Identify the **Lower Deck** (contains motor bracket mounting slots on the underside) and the **Upper Deck** (contains mounting holes for the ESP32, dual L298N modules, and DC-DC converter).

### Step 2: TT Geared Motor & Speed Encoder Disc Sub-assembly
- **Components:** 4Ã— Yellow TT Motors (`ACT-01`), 4Ã— Black 20-slot Speed Encoder Discs (`MCH-05`), 4Ã— pairs of acrylic motor mounting brackets (`MCH-04`), 8Ã— M3 Ã— 30 mm long bolts, 8Ã— M3 hex nuts.
- **Action:**
  1. Press-fit one black 20-slot speed encoder disc onto the **inner output shaft** of each TT motor until flush.
  2. Solder red and black hookup wire leads (approx. 120 mm length) to the brass solder tabs of each TT motor canister. Ensure solder joints are trimmed and insulated with heat-shrink tubing to prevent shorting against the motor's metal canister.
  3. Position one inner and one outer acrylic mounting bracket around each motor gearbox.
  4. Pass 2Ã— M3 Ã— 30 mm bolts through the brackets and motor body.
  5. Mount the 4 motor assemblies to the underside of the **Lower Deck**:
     - Front-Left and Rear-Left motors mount on the $-X$ side with output shafts facing outward.
     - Front-Right and Rear-Right motors mount on the $+X$ side with output shafts facing outward.
  6. Tighten the M3 hex nuts firmly, but do not overtighten to prevent cracking the acrylic brackets.

### Step 3: Battery Holder Installation
- **Components:** 1Ã— 4-cell battery holder (`PWR-02`), 2Ã— M3 Ã— 8 mm countersunk/pan screws, 2Ã— M3 hex nuts.
- **Action:**
  1. Orient the battery holder in the lower deck central bay with its red and black wire leads routed toward the rear.
  2. Fasten the holder to the lower deck using M3 screws through the slotted mounting holes.
  3. Verify that the battery holder does not impede the inner motor canisters or encoder wheels.

### Step 4: Standoff Installation & Deck Union
- **Components:** 6Ã— M3 Ã— 30 mm Brass Hexagonal Standoffs (`MCH-02`), 12Ã— M3 Ã— 6 mm nickel pan screws.
- **Action:**
  1. Secure the 6 standoffs to the top face of the Lower Deck using 6Ã— M3 Ã— 6 mm screws from underneath:
     - 2 standoffs near the front bumper ($Y = +83.6\,\text{mm}$).
     - 2 standoffs at the mid-chassis balance point ($Y = 0.0\,\text{mm}$).
     - 2 standoffs near the rear bumper ($Y = -83.6\,\text{mm}$).
  2. Align the Upper Deck over the 6 standoffs.
  3. Fasten the Upper Deck with the remaining 6Ã— M3 Ã— 6 mm screws into the standoff top threads.
  4. Ensure both decks are parallel with exactly 30 mm vertical clearance.

### Step 5: Wheel Hub & Rubber Tire Installation
- **Components:** 4Ã— 66 mm Yellow Wheel Rims with Rubber Tires (`ACT-02`).
- **Action:**
  1. Align the D-shaped slot of each wheel rim with the dual-flat white output shaft of each TT motor.
  2. Firmly press the wheel onto the shaft until seated against the shoulder.
  3. Spin each wheel by hand to verify:
     - Minimum 8.5 mm lateral clearance between tire inner shoulder and acrylic chassis edge.
     - Free rotation with internal gear mesh resistance, with no binding against brackets or bolt ends.

---

## 3. Upper Deck Electronics Mounting

Mount the electronic modules onto the upper acrylic deck before connecting wiring:

1. **ESP32 Development Board (`CTL-01`):**
   - Location: Front-center ($X = 0\,\text{mm}$, $Y = +45\,\text{mm}$).
   - Orientation: Micro-USB port faces rearward; the ESP-WROOM-32 metal RF shield and PCB trace antenna face forward past the front edge into free air for optimal Bluetooth signal propagation.
   - Secure using 4Ã— M2.5 nylon standoffs or high-strength double-sided VHB mounting tape.
2. **Left Motor Driver (`DRV-01` â€” L298N Module):**
   - Location: Mid-chassis left ($X = -26\,\text{mm}$, $Y = -15\,\text{mm}$).
   - Orientation: Heatsink faces inward toward vehicle centerline; OUT1/OUT2 and OUT3/OUT4 terminal blocks face outward toward the left motor wire entries.
3. **Right Motor Driver (`DRV-02` â€” L298N Module):**
   - Location: Mid-chassis right ($X = +26\,\text{mm}$, $Y = -15\,\text{mm}$).
   - Orientation: Heatsink faces inward toward vehicle centerline; terminal blocks face outward toward the right motor wire entries.
4. **Adjustable DC-DC Converter (`PWR-01`):**
   - Location: Rear-center ($X = 0\,\text{mm}$, $Y = -68\,\text{mm}$).
   - Orientation: Input terminals face rear (toward battery leads); output terminals face forward (toward L298N driver power terminals); trimmer adjustment screw accessible from above.

---

## 4. Electrical Wiring Procedure

Follow the strict electrical sequence below. Refer to `docs/hardware/electrical-connection-map.md` and `docs/hardware/wiring-schematic.svg` for exact terminal identities.

### Sub-step A: Motor-to-Driver Connections (High Current)
1. **Left Drivetrain:**
   - Front-Left Motor leads: Connect Red wire to `DRV-01` terminal `OUT1`, Black wire to `OUT2`.
   - Rear-Left Motor leads: Connect Red wire to `DRV-01` terminal `OUT3`, Black wire to `OUT4`.
2. **Right Drivetrain:**
   - Front-Right Motor leads: Connect Red wire to `DRV-02` terminal `OUT1`, Black wire to `OUT2`.
   - Rear-Right Motor leads: Connect Red wire to `DRV-02` terminal `OUT3`, Black wire to `OUT4`.

### Sub-step B: Logic Signal Ribbon Installation (ESP32 to Drivers)
1. **Remove Factory Jumpers:**
   - On `DRV-01` (Left Driver), remove the two black shorting jumpers over `ENA` and `ENB`.
   - On `DRV-02` (Right Driver), remove the two black shorting jumpers over `ENA` and `ENB`.
2. **Left Driver Logic Ribbon (6Ã— Dupont Jumpers):**
   - ESP32 **GPIO 18** -> `DRV-01` **ENA** (Yellow wire)
   - ESP32 **GPIO 19** -> `DRV-01` **IN1** (Green wire)
   - ESP32 **GPIO 17** -> `DRV-01` **IN2** (Blue wire)
   - ESP32 **GPIO 16** -> `DRV-01` **IN3** (White wire)
   - ESP32 **GPIO 4** -> `DRV-01` **IN4** (Purple wire)
   - ESP32 **GPIO 25** -> `DRV-01` **ENB** (Orange wire)
3. **Right Driver Logic Ribbon (6Ã— Dupont Jumpers):**
   - ESP32 **GPIO 23** -> `DRV-02` **ENA** (Yellow wire)
   - ESP32 **GPIO 22** -> `DRV-02` **IN1** (Green wire)
   - ESP32 **GPIO 21** -> `DRV-02` **IN2** (Blue wire)
   - ESP32 **GPIO 27** -> `DRV-02` **IN3** (White wire)
   - ESP32 **GPIO 14** -> `DRV-02` **IN4** (Purple wire)
   - ESP32 **GPIO 26** -> `DRV-02` **ENB** (Orange wire)

### Sub-step C: Voltage Divider Telemetry Tap
1. Solder resistor $R1 = 100\,\text{k}\Omega$ (1% metal film) from the switched battery positive lead to a tie point.
2. Solder resistor $R2 = 20\,\text{k}\Omega$ (1% metal film) from the tie point to common ground.
3. Connect a yellow jumper wire from the $R1 / R2$ junction directly to ESP32 **GPIO 34**.
4. Wrap the resistors in heat-shrink tubing to prevent contact with conductive chassis standoffs.

### Sub-step D: Power Rail & Common Star-Ground Distribution
> [!CAUTION]
> **DO NOT CONNECT THE BATTERY TO THE DRIVERS OR ESP32 YET.** The DC-DC converter output voltage must be calibrated with a multimeter prior to final electrical connection.

1. Connect the battery negative lead to the Common Ground Bus.
2. Connect DC-DC Converter `IN-` and `OUT-` to the Common Ground Bus.
3. Connect `DRV-01` `GND` terminal and `DRV-02` `GND` terminal to the Common Ground Bus.
4. Connect ESP32 `GND` pin to the Common Ground Bus.
5. Connect battery positive lead through the master toggle switch to DC-DC Converter `IN+`.
6. Leave DC-DC Converter `OUT+` disconnected from all boards until Stage 3 of the testing procedure is completed.
