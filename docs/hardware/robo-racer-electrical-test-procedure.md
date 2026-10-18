# ROBO RACER 4WD — ELECTRICAL COMMISSIONING & BENCH TEST PROCEDURE

**Document Revision:** v1.3 — Final Engineering Baseline
**Target Hardware:** 4WD Robotic Car Kit | 4× AA Battery Pack (~6.0V Nominal) | amiciSolar XL6019 DC-DC Module (BLOCKED — REGULATOR NOT SUITABLE OR NOT VERIFIED) | 2× L298N Drivers | ESP32 DevKit V1 (30-pin) | 4× TT DC Geared Motors
**Related Documents:**
- [`robo-racer-full-wiring.svg`](robo-racer-full-wiring.svg) (Master Vector Schematic)
- [`robo-racer-wire-by-wire.md`](robo-racer-wire-by-wire.md) (Wire-by-Wire Pinout Table)
- [`robo-racer-wiring-assembly-guide.md`](robo-racer-wiring-assembly-guide.md) (Physical Assembly & Jumper Guide)
- [`docs/hardware/wiring-implementation-plan.md`](wiring-implementation-plan.md) (Architecture & Risk Plan)

---

## 1. Safety Gate & Tooling Requirements

> [!CAUTION]
> **NEVER SKIP STAGE 1 OR STAGE 2.**
> - Plugging an uncalibrated DC-DC converter into an ESP32 board is the #1 cause of destroyed microcontrollers in DIY robotics.
> - All initial motor testing MUST be performed with the **car chassis elevated on a block** so all four wheels rotate freely in the air without touching the workbench or floor.

### Equipment Required:
1. **Digital Multimeter (DMM):** Capable of DC Voltage (mV to 20V range), DC Current (10A unfused/fused socket), and Resistance / Continuity (buzzer mode).
2. **Precision Screwdriver:** Small flat-blade (2.0mm) for 3296W trimpot adjustment and terminal screws.
3. **Chassis Support Block:** A small box, cup, or foam block (height ~50mm) to suspend the wheels.
4. **4× Fresh AA Alkaline Batteries:** Eveready / Duracell / Energizer (or fully charged 1.2V NiMH cells).
5. **USB Cable & Laptop:** Running Arduino IDE Serial Monitor (115200 baud) or the Robo Racer Android Controller app.

---

## 2. Staged Commissioning Protocol

```text
[ STAGE 1: Unpowered Short-Circuit Check ] ───▶ FAIL ──▶ STOP: Resolve wiring short
                     │ PASS
[ STAGE 2: DC-DC Multimeter Calibration ] ───▶ FAIL ──▶ STOP: Re-trim 3296W to 5.00V
                     │ PASS
[ STAGE 3: ESP32 Logic Power-Up ]         ───▶ FAIL ──▶ STOP: Check VIN/GND & LDO
                     │ PASS
[ STAGE 4: Driver Logic Interface Check ] ───▶ FAIL ──▶ STOP: Check Jumper & Dupont
                     │ PASS
[ STAGE 5: Elevated Single-Motor Spin ]   ───▶ FAIL ──▶ STOP: Check Phase/Direction
                     │ PASS
[ STAGE 6: 4-Motor Full-Load Sag Test ]   ───▶ PASS ──▶ VEHICLE COMMISSIONED!
```

---

### STAGE 1: Unpowered Continuity & Short-Circuit Verification

*Objective: Guarantee zero dead shorts between power rails, ground, and signal lines BEFORE any battery is inserted.*
*Status: **[MANDATORY BENCH CHECK - PRE-POWER]***

1. **Battery pack completely removed.** Disconnect any USB cable from ESP32.
2. Set DMM to **Resistance (Ohms / Continuity)** mode.
3. **Rail-to-Ground Measurements:**
   - Probe between **Central V+ Bus** (Red) and **Central Star GND** (Black):
     *Expected:* Reads open circuit or charging capacitor effect (> 10 kΩ).
     *Pass Criteria:* **> 5 kΩ**. If DMM buzzes or reads < 10 Ω, **STOP IMMEDIATELY**: there is a dead short between battery positive and ground!
   - Probe between **DC-DC OUT+ Terminal** and **Central Star GND**:
     *Pass Criteria:* **> 1 kΩ** (not shorted).
   - Probe between **ESP32 3V3 pin** and **GND**:
     *Pass Criteria:* **> 500 Ω** (ESP32 internal LDO not shorted).
4. **Motor Winding Resistance:**
   - Measure across the two terminals of each TT motor (FL, RL, FR, RR):
     *Pass Criteria:* **2.2 Ω to 5.0 Ω** per motor.
     *Fail Criteria:* 0 Ω (internal shorted winding) or ∞ Ω (broken wire/brush).
5. **Star Ground Continuity:**
   - Measure resistance from Battery Negative terminal to:
     - Left L298N `GND`: < 0.2 Ω
     - Right L298N `GND`: < 0.2 Ω
     - DC-DC Converter `IN-` and `OUT-`: < 0.2 Ω
     - ESP32 board `GND` pin: < 0.3 Ω
     - Voltage divider $R_2$ ground leg: < 0.3 Ω

---

### STAGE 2: DC-DC Identification & Output Calibration (Multimeter Prerequisite)

*Objective: Inspect switching controller markings under magnification, verify electrical topology, and characterize output under load.*
*Status: **[PROVISIONAL BENCH CHECK — METROLOGY GATE (BLOCKED — REGULATOR NOT SUITABLE OR NOT VERIFIED)]***

> [!CRITICAL]
> **DISCONNECT WIRE `PWR-11` (ESP32 VIN), `PWR-12` (Left L298N +5V), AND `PWR-13` (Right L298N +5V) BEFORE PERFORMING THIS STEP.**
> The ordered converter is identified from listing photos as the **amiciSolar XL6019**.
> **Electrical Incompatibility Warning:** XLSEMI XL6019 datasheet specifies $V_{IN} \ge 5.0\text{V}$, whereas 4× AA alkaline cells sag to $3.8\text{V}-4.5\text{V}$ under motor inrush, risking logic brownouts. Furthermore, as a boost-topology module, it cannot buck $6.4\text{V}$ fresh cells down to $5.0\text{V}$.
> Do NOT connect the converter to sensitive logic until its topology, no-load output voltage, and step-load stability are verified with a digital multimeter.

1. **Physical Component Inspection Under Magnification:**
   - Inspect the primary switching IC markings on the blue converter board using a magnifying glass or macro camera with bright angled lighting.
   - Confirm part number (e.g. XL6019).
   - Trace PCB copper connections: Check whether both inductors are active in a SEPIC/buck-boost circuit or if Inductor 2 is merely an output $LC$ post-filter on a standard boost regulator.
   - **Verification Gate:** If the module is a boost-only converter, its output cannot step down below input voltage ($V_{out} \ge V_{in}$). Connecting it to a fresh 6.4V alkaline pack would pass ~6.0V+ through to the ESP32 `VIN` pin without regulation. Only proceed if the module demonstrates true step-down capability when input exceeds 5.0V. If bench testing exhibits dropout under load or fail-to-buck at 6.4V, replace with a wide-input buck-boost ($V_{IN} \ge 2.5\text{V}$) or an isolated 5V power bank.
2. Connect DC-DC Converter `IN+` to battery positive and `IN-` to star ground.
3. Insert 4 fresh AA alkaline batteries into the tray (Duracell / Eveready).
4. Measure battery terminal voltage with DMM across `IN+` and `IN-`:
   *Expected:* **6.0V to 6.4V DC** for fresh alkaline cells. Record as $V_{pack}$.
5. Connect DMM test leads securely to DC-DC converter `OUT+` and `OUT-`. Set DMM to DC Volts (20V range).
6. Read no-load output voltage:
   - If output is not 5.00V, take a small flat-head screwdriver and turn the brass screw on the blue 3296W trimpot:
     - Clockwise / counter-clockwise adjusts the switching duty cycle.
     - Note: multiturn potentiometers require 5 to 15 full turns to transition between extremes.
   - Adjust steadily until DMM reads **5.00 V no-load** (acceptable tolerance window: **4.95 V to 5.05 V**).
   - **Note on Load Stability:** Never claim 5.00V ± 0.05V stability under load without physical measurements. Output voltage under dynamic motor inrush and battery sag remains unverified until Stage 6 bench testing.
7. Disconnect the battery pack.
8. Reconnect wires `PWR-11`, `PWR-12`, and `PWR-13` to their respective loads.

---

### STAGE 3: ESP32 Logic Power-Up & Firmware Boot Verification

*Objective: Verify that the ESP32 powers on cleanly, its onboard 3.3V LDO functions normally, and the firmware initializes.*
*Status: **[PENDING PHYSICAL DELIVERY]***

1. Disconnect both L298N motor supply wires (`PWR-03` and `PWR-04` from driver `+12V` terminals). This ensures motors cannot accidentally draw current while testing the microcontroller.
2. Connect the battery pack (or plug in inline connector).
3. **Power Rail Checks:**
   - Measure voltage at ESP32 `VIN` pin to `GND`: must read **4.90V to 5.05V**.
   - Measure voltage at ESP32 `3V3` pin to `GND`: must read **3.25V to 3.35V**.
4. **Visual & Serial Inspection:**
   - Onboard red power LED on ESP32 lights solid.
   - Onboard blue status LED (`GPIO 2`) begins slow blinking (1 Hz), indicating waiting for Bluetooth connection.
   - Open serial terminal (115200 baud) via USB:
     ```text
     ======================================
       ROBO RACER ESP32 FIRMWARE v1.1
       Ready for SPP Connection
       Device Name: ROBO-RACER-ESP32
     ======================================
     ```
5. Disconnect battery. Reconnect `PWR-03` and `PWR-04`.

---

### STAGE 4: Driver Logic & Jumper Verification

*Objective: Confirm L298N logic circuits are operational, jumpers are removed, and quiescent current is within spec.*
*Status: **[PENDING PHYSICAL DELIVERY]***

1. Confirm all 6 jumpers are **REMOVED** across both L298N boards (`5V-EN`, `ENA`, `ENB`).
2. Insert DMM in DC Current mode (200mA or 10A socket) inline between Battery Positive and Central V+ Bus.
3. Power up the system with motors stationary (car idle):
   - Total quiescent system current (ESP32 active + Bluetooth beacon + 2× L298N logic gates):
     *Expected Range:* **80 mA to 160 mA**.
   - If current exceeds 350 mA with motors off: **POWER OFF IMMEDIATELY**. Check for hot L298N heatsinks or inverted Dupont signal lines.
4. Measure voltage at L298N `+5V` terminal: must read **4.90V to 5.05V**.
5. Measure voltage at L298N `+12V` terminal: must equal battery voltage (**~6.0V**).
6. Remove DMM from inline current path and reconnect direct battery positive.

---

### STAGE 5: Individual Motor Direction Verification via Physical Isolation

*Objective: Verify each motor channel spins smoothly in the intended direction under firmware command, using physical isolation to test one channel at a time before pairing.*
*Status: **[PENDING PHYSICAL DELIVERY]***

> [!IMPORTANT]
> **FIRMWARE TANDEM DRIVE REALITY & PHYSICAL ISOLATION MANDATE:**
> The Robo Racer ESP32 firmware commands motors in side pairs:
> - `set_hardware_left()` drives both Front-Left (`OUT1/OUT2`) and Rear-Left (`OUT3/OUT4`) with the exact same PWM duty cycle and direction.
> - `set_hardware_right()` drives both Front-Right (`OUT1/OUT2`) and Rear-Right (`OUT3/OUT4`) with the exact same PWM duty cycle and direction.
>
> The firmware does NOT provide independent single-wheel testing via normal joystick or driving commands. If both motors are wired immediately and one has reversed polarity, the two motors will mechanically fight each other, stalling the drivetrain, drawing high inrush current, and potentially stripping the plastic TT spur gears.
>
> **Therefore, commissioning MUST follow this physical isolation sequence:**

1. Keep the vehicle elevated on a test stand (all four wheels completely clear of workbench).
2. Launch the **Robo Racer Android Controller App** and pair with `ROBO-RACER-ESP32` via Bluetooth Classic.
3. Verify connection: ESP32 Blue LED transitions from 1 Hz blinking to **solid ON**, and the app shows "CONNECTED".

#### Step 5A: Left Drivetrain Channel-by-Channel Isolation
1. **Front-Left Motor (FL) Alone:**
   - Connect ONLY wires `MOT-01` and `MOT-02` to Left L298N `OUT1` and `OUT2`.
   - Leave `OUT3` and `OUT4` completely empty (disconnected).
   - In the Android app, push the throttle forward slightly (10–20% low-speed jog).
   - *Visual Check:* The FL wheel must rotate **forward** (top of tire rolling toward front bumper).
   - *Correction if backwards:* Swap `MOT-01` and `MOT-02` in `OUT1/OUT2` screw terminals.
2. **Rear-Left Motor (RL) Alone:**
   - Unplug/disconnect `MOT-01` and `MOT-02` from `OUT1/OUT2`.
   - Connect ONLY wires `MOT-03` and `MOT-04` to Left L298N `OUT3` and `OUT4`.
   - Command low forward throttle in the app.
   - *Visual Check:* The RL wheel must rotate **forward**.
   - *Correction if backwards:* Swap `MOT-03` and `MOT-04` in `OUT3/OUT4` screw terminals.
3. **Paired Left Verification:**
   - Reconnect `MOT-01` and `MOT-02` back into `OUT1/OUT2`. Both FL and RL are now wired.
   - Command low forward throttle: verify **both left wheels spin smoothly forward in unison**.

#### Step 5B: Right Drivetrain Channel-by-Channel Isolation
1. **Front-Right Motor (FR) Alone:**
   - Connect ONLY wires `MOT-05` and `MOT-06` to Right L298N `OUT1` and `OUT2`.
   - Leave `OUT3` and `OUT4` empty.
   - Command low forward throttle in the app.
   - *Visual Check:* The FR wheel must rotate **forward**.
   - *Correction if backwards:* Swap `MOT-05` and `MOT-06` in `OUT1/OUT2`.
2. **Rear-Right Motor (RR) Alone:**
   - Disconnect `MOT-05` and `MOT-06`.
   - Connect ONLY wires `MOT-07` and `MOT-08` to Right L298N `OUT3` and `OUT4`.
   - Command low forward throttle in the app.
   - *Visual Check:* The RR wheel must rotate **forward**.
   - *Correction if backwards:* Swap `MOT-07` and `MOT-08` in `OUT3/OUT4`.
3. **Paired Right Verification:**
   - Reconnect `MOT-05` and `MOT-06` back into `OUT1/OUT2`. Both FR and RR are now wired.
   - Command low forward throttle: verify **both right wheels spin forward in unison**.

#### Step 5C: 4-Wheel Direction & Safety Safeguard Checks
1. Command forward: all 4 wheels spin forward.
2. Command reverse: all 4 wheels spin backward smoothly after the 500ms zero-speed dwell deadband.
3. **Watchdog Interlock Test:** While wheels are rotating, disable phone Bluetooth or close the app.
   - *Pass Criteria:* All 4 motors must cut power within **500 ms** (`WATCHDOG_TIMEOUT_MS`). Status LED enters fast 5 Hz strobe.

---

### STAGE 6: Four-Motor Power Budget & Voltage Sag Validation

*Objective: Validate the power distribution system under 4-motor simultaneous load, characterize battery pack voltage sag, and confirm microcontroller logic rail stability.*
*Status: **[PENDING PHYSICAL DELIVERY]***

#### 6.1 Four-Motor Electrical Power Budget

| Parameter | Per Motor (TT DC) | Total 4-Motor Drivetrain | System Notes & Constraints |
|---|:---:|:---:|---|
| **No-Load Running Current** | $150 - 250\,\text{mA}$ | $0.6 - 1.0\,\text{A}$ | Elevated wheels spinning freely in air |
| **Typical Floor Running Current** | $300 - 500\,\text{mA}$ | $1.2 - 2.0\,\text{A}$ | Smooth floor driving, steady speed |
| **Skid-Steer Pivot / Carpet Inrush** | $600 - 800\,\text{mA}$ | $2.4 - 3.2\,\text{A}$ | High friction turning on carpet |
| **Theoretical Stall Surge Current** | $0.8 - 1.2\,\text{A}$ | **$3.2 - 4.8\,\text{A}$** | Momentary inrush during rapid reversal or blocked wheels |
| **L298N Darlington Voltage Drop ($V_{drop}$)** | $1.8 - 2.5\,\text{V}$ | $1.8 - 2.5\,\text{V}$ | Bipolar $V_{CE(sat)}$ across upper & lower internal transistors |
| **L298N Heat Dissipation ($P_{loss}$)** | $0.5 - 1.5\,\text{W}$ | **$2.0 - 4.0\,\text{W}$** | Split across 2 driver heatsinks; boards will get warm |
| **ESP32 + Bluetooth Logic Current** | — | $120 - 260\,\text{mA}$ | Spikes up to 350 mA during Bluetooth radio transmission bursts |

#### 6.2 4× AA Alkaline Battery Internal Resistance & Voltage Sag Analysis

- **Pack Internal Resistance ($R_{i,pack}$):**
  - Fresh AA alkaline cells (Duracell / Eveready): $R_i \approx 0.15\,\Omega - 0.30\,\Omega$ per cell.
  - 4 cells in series: $R_{i,pack} \approx 0.60\,\Omega - 1.20\,\Omega$ fresh (increasing to $> 2.0\,\Omega$ as cells deplete).
- **Loaded Terminal Voltage Equation:**
  $$V_{pack\_loaded} = V_{open\_circuit} - (I_{total} \times R_{i,pack})$$
  - At fresh open-circuit ($6.2\,\text{V}$) under $2.5\,\text{A}$ inrush load ($R_{i,pack} = 0.8\,\Omega$):
    $$V_{pack\_loaded} = 6.2\,\text{V} - (2.5\,\text{A} \times 0.8\,\Omega) = 6.2\,\text{V} - 2.0\,\text{V} = \mathbf{4.2\,\text{V}}$$
- **Net Voltage Delivered to TT Motor Terminals:**
  $$V_{motor} = V_{pack\_loaded} - V_{drop\_L298N} \approx 4.2\,\text{V} - 2.0\,\text{V} = \mathbf{2.2\,\text{V}}$$
  - *Engineering Insight:* Under heavy motor load, each TT motor receives only $\sim 2.0\,\text{V} - 2.5\,\text{V}$. The car will drive slower than theoretical 6V calculations suggest, which naturally limits stall current and protects motor windings from burning.

> [!CAUTION]
> **EXPLICIT PHYSICAL MEASUREMENT GATE — NEVER DELIBERATELY STALL MOTORS:**
> - **DO NOT clamp wheels with pliers or hold wheels stationary to measure stall current directly.** Stalling TT motors will shear the internal plastic gear teeth, strip output shafts, and overheat motor brushes.
> - **Safe Measurement Protocol:**
>   1. Measure unpowered DC winding resistance with DMM: $R_{winding} \approx 3.0\,\Omega - 5.0\,\Omega$.
>   2. Calculate theoretical stall current: $I_{stall\_calc} = V_{loaded} / R_{winding}$.
>   3. Measure running currents with wheels elevated on test stand.

#### 6.3 Simultaneous Load Test Execution
1. Reconnect Bluetooth connection with vehicle elevated on test stand.
2. Connect DMM Channel 1 across **4× AA Battery Terminals** (Raw $V_{pack}$).
3. Connect DMM Channel 2 across **DC-DC Converter Output Terminals** ($V_{logic}$).
4. Command **100% Full Forward Throttle** for 5 seconds:
   - **Battery Pack Voltage ($V_{pack}$):**
     *Unloaded voltage:* ~6.2 V.
     *Loaded voltage:* Will sag to **~4.2 V to 5.0 V** due to AA internal resistance.
     *Pass Criteria:* Pack terminal voltage remains > 3.8 V during elevated run.
   - **Provisional Logic Rail ($V_{logic}$):**
     *Evaluation Criteria:* Observe whether the converter successfully maintains $\ge 4.5\,\text{V}$ into the ESP32 `VIN` pin across the 5-second burst.
   - **ESP32 Stability:**
     *Pass Criteria:* ESP32 must **NOT reset or brown out**. Bluetooth connection must remain unbroken.
5. **Corrective Actions if Brownout Occurs:**
   - If the ESP32 resets when motors start, the DC-DC converter cannot step up dynamically during the steep inrush dip.
   - *Mitigation A:* Add a $470\,\mu\text{F} - 1000\,\mu\text{F}$ 10V low-ESR electrolytic buffer capacitor across DC-DC `OUT+` and `OUT-` to hold the logic rail during motor current spikes.
   - *Mitigation B:* Replace weak AA cells with high-drain alkaline cells or low-impedance NiMH rechargeable cells ($R_i \approx 0.05\,\Omega/\text{cell}$).
   - *Mitigation C:* If the unverified converter proves to be a boost-only module or cannot maintain regulation, decouple logic power by powering the ESP32 via a separate small 5V USB battery bank.

---

## 3. Physical Multimeter Calibration Procedure (ADC Divider)

To enable live calibrated battery telemetry in the Android application:

1. Measure battery pack voltage directly with DMM: record as $V_{pack}$ (e.g. `6.18 V`).
2. Measure DC voltage between ESP32 pin `GPIO 34` and `GND`: record as $V_{pin}$ (e.g. `1.030 V`).
3. Compute exact ratio:
   $$\text{RATIO} = \frac{V_{pack}}{V_{pin}} = \frac{6.18}{1.030} = 6.000$$
4. In `firmware/src/config.h` (and `firmware/RoboRacerFirmware/RoboRacerFirmware.ino`):
   - Update `#define BATT_DIVIDER_RATIO 6.000f` with your computed ratio.
   - Change `#define BATT_CALIBRATION_CONFIRMED 1`.
5. Re-flash the ESP32. The Android controller app will now display live battery voltage and percentage accurately!

---

## 4. Failure Isolation & Troubleshooting Matrix

| Symptom / Observation | Probable Root Cause | Isolation Test & Verification | Corrective Action |
| :--- | :--- | :--- | :--- |
| **ESP32 resets / reboots whenever throttle is pushed** | Battery voltage sags below converter dropout, causing brownout reset | Measure voltage at `VIN` pin while commanding throttle. If it dips below 4.5V, pack is collapsing. | 1. Replace weak AA cells with high-drain alkaline or NiMH.<br>2. Add 470µF low-ESR capacitor across DC-DC `OUT+` and `GND`.<br>3. Verify SEPIC converter 5V trimpot is correctly calibrated. |
| **Motors do not spin, but ESP32 is connected and LEDs respond** | 1. `ENA` / `ENB` jumpers left on or PWM wiring wrong.<br>2. Direct battery wire `PWR-03`/`04` disconnected from L298N `+12V`. | Check DC voltage across L298N `+12V` and `GND` with DMM. Measure PWM voltage on `ENA` with scope/DMM. | 1. Confirm `ENA`/`ENB` jumpers are REMOVED.<br>2. Tighten `+12V` and `GND` screw terminals.<br>3. Verify Dupont wires match `config.h` GPIO mapping. |
| **Motors spin at 100% full speed immediately on power-up** | `ENA` and/or `ENB` jumpers were left installed on L298N board | Inspect the 2-pin headers next to the logic header. | **REMOVE ENA and ENB jumpers** so ESP32 controls speed via PWM. |
| **L298N heat sink gets extremely hot even with motors stopped** | `5V-EN` jumper was left installed, causing 78M05 regulator to backfeed DC-DC converter | Touch L298N regulator. Measure voltage on `+5V` terminal. | **REMOVE 5V-EN jumper immediately** on both driver modules! |
| **One side of car spins backwards during forward command** | Motor terminal polarity inverted on that channel | Visual check of wheel rotation direction. | Swap the two motor output wires (`OUT1`/`OUT2` or `OUT3`/`OUT4`) for that motor. |
| **App shows "LOW BATTERY" or "INVALID BATT" warning** | Divider not calibrated or pack voltage below 4.4V cutoff threshold | Measure battery terminal voltage and pin 34 voltage. | 1. Replace depleted AA cells if $V_{pack} < 4.4\text{ V}$.<br>2. Calibrate $R_1/R_2$ divider and set `BATT_CALIBRATION_CONFIRMED = 1`. |
| **Bluetooth disconnects randomly after 1–2 seconds of running** | Motor brush electrical noise resetting ESP32 radio | Check if 100nF suppression capacitors are soldered across motor terminals. | Solder 100nF ceramic capacitors directly across motor lugs. Separate motor wiring from ESP32 ribbon. |
