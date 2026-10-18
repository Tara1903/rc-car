# ROBO RACER 4WD — ELECTRICAL COMMISSIONING & BENCH TEST PROCEDURE

**Document Revision:** v1.2 — Physical Delivery Baseline  
**Target Hardware:** 4WD Robotic Car Kit | 4× AA Battery Pack (~6.0V Nominal) | SEPIC Auto Buck-Boost DC-DC Module | 2× L298N Drivers | ESP32 DevKit V1 (30-pin) | 4× TT DC Geared Motors  
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

### STAGE 2: DC-DC Output Calibration (Multimeter Prerequisite)

*Objective: Adjust the SEPIC converter output to exactly 5.00 V with no sensitive loads attached.*  
*Status: **[MANDATORY BENCH CHECK - METROLOGY GATE]***

> [!CRITICAL]
> **DISCONNECT WIRE `PWR-11` (ESP32 VIN), `PWR-12` (Left L298N +5V), AND `PWR-13` (Right L298N +5V) BEFORE PERFORMING THIS STEP.**

1. Connect DC-DC Converter `IN+` to battery positive and `IN-` to star ground.
2. Insert 4 fresh AA batteries into the tray.
3. Measure battery terminal voltage with DMM across `IN+` and `IN-`:  
   *Expected:* **6.0V to 6.4V DC** for alkaline (or ~5.0V to 5.2V for NiMH). Record as $V_{pack}$.
4. Connect DMM test leads securely to DC-DC converter `OUT+` and `OUT-`. Set DMM to DC Volts (20V range).
5. Read output voltage:
   - If output is not 5.00V, take a small flat-head screwdriver and turn the brass screw on the blue 3296W trimpot:
     - Clockwise / counter-clockwise adjusts the switching duty cycle.
     - Note: multiturn potentiometers require 5 to 15 full turns to transition between extremes.
   - Adjust steadily until DMM reads **5.00 V** (acceptable tolerance window: **4.95 V to 5.05 V**).
6. Disconnect the battery pack.
7. Reconnect wires `PWR-11`, `PWR-12`, and `PWR-13` to their respective loads.

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

### STAGE 5: Elevated Single-Motor Rotation & Direction Verification

*Objective: Verify each motor channel spins smoothly in the intended direction under firmware command.*  
*Status: **[PENDING PHYSICAL DELIVERY]***

> [!IMPORTANT]
> Keep the vehicle elevated on the test stand. Do NOT let wheels touch the ground.

1. Launch the **Robo Racer Android Controller App** and pair with `ROBO-RACER-ESP32` via Bluetooth Classic.
2. Verify connection:
   - ESP32 Blue LED transitions from 1 Hz blinking to **solid ON**.
   - App dashboard shows "CONNECTED" and displays telemetry frames.
3. **Individual Channel Direction Test:**
   - **Front-Left Motor (FL):** Push throttle forward slightly (or command Forward slow).  
     *Visual Check:* Front-left wheel must rotate **forward** (top of wheel rolling toward front of car).  
     *Action if backwards:* Swap wires `MOT-01` and `MOT-02` in Left L298N `OUT1`/`OUT2` terminals.
   - **Rear-Left Motor (RL):**  
     *Visual Check:* Rear-left wheel must rotate **forward**.  
     *Action if backwards:* Swap wires `MOT-03` and `MOT-04` in Left L298N `OUT3`/`OUT4` terminals.
   - **Front-Right Motor (FR):**  
     *Visual Check:* Front-right wheel must rotate **forward**.  
     *Action if backwards:* Swap wires `MOT-05` and `MOT-06` in Right L298N `OUT1`/`OUT2` terminals.
   - **Rear-Right Motor (RR):**  
     *Visual Check:* Rear-right wheel must rotate **forward**.  
     *Action if backwards:* Swap wires `MOT-07` and `MOT-08` in Right L298N `OUT3`/`OUT4` terminals.
4. **Reverse Test:**
   - Pull throttle backward. Confirm all 4 wheels rotate backwards smoothly.
   - Verify that firmware reverse dwell (500ms deadband) prevents instant shock-reversal.
5. **Failsafe Watchdog Test:**
   - While wheels are rotating, toggle Bluetooth OFF on the Android phone (or close the app).
   - *Pass Criteria:* All 4 motors must instantly stop spinning within **500 ms** (`WATCHDOG_TIMEOUT_MS`). The ESP32 blue LED transitions to rapid failsafe strobing (5 Hz).

---

### STAGE 6: 4-Motor Simultaneous Load & Voltage Sag Test

*Objective: Validate that the 4× AA alkaline battery pack and SEPIC DC-DC converter can power all 4 motors without triggering brownout resets.*  
*Status: **[PENDING PHYSICAL DELIVERY]***

1. Reconnect Bluetooth connection with vehicle elevated on test stand.
2. Connect DMM Channel 1 (or handheld DMM) across the **4× AA Battery Terminals** (Raw $V_{pack}$).
3. Connect DMM Channel 2 across the **DC-DC Converter Output Terminals** ($V_{logic}$).
4. Command **100% Full Forward Throttle** for 5 seconds:
   - **Battery Pack Voltage ($V_{pack}$):**
     *Unloaded voltage:* ~6.2 V.
     *Full-throttle loaded voltage:* Will sag to **~4.0 V to 4.8 V** due to AA internal resistance ($R_i \approx 0.6\,\Omega - 1.0\,\Omega$).
     *Pass Criteria:* Voltage must remain > 3.8 V during continuous elevated run.
   - **Regulated Logic Rail ($V_{logic}$):**
     *Pass Criteria:* Must remain stable at **4.95 V to 5.05 V**. The SEPIC converter automatically boosts the sagging 4.0V input up to 5.00V!
   - **ESP32 Stability:**
     *Pass Criteria:* ESP32 must **NOT reset or brown out**. The Bluetooth connection must remain continuous without dropping packets.
5. If the ESP32 resets (brownout detector triggers):
   - Check that the SEPIC converter inductors are warm (not scalding).
   - Add a $470\,\mu\text{F}$ 10V electrolytic buffer capacitor across the DC-DC `OUT+` and `OUT-` terminals to absorb motor inrush spikes.
   - Verify AA battery quality (replace stale carbon-zinc cells with high-drain alkaline or low-ESR NiMH).

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
