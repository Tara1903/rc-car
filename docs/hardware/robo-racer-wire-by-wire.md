# ROBO RACER 4WD — BEGINNER-FRIENDLY WIRE-BY-WIRE INTERCONNECT TABLE

**Document Revision:** v1.2 — Physical Delivery Baseline  
**Hardware Reference:** 4× AA Battery Pack (Direct Unswitched) | Dual-Inductor SEPIC DC-DC Converter | 2× L298N Drivers | ESP32 DevKit V1 (30-pin) | 4× TT DC Geared Motors  
**Related Vector Schematic:** [`robo-racer-full-wiring.svg`](robo-racer-full-wiring.svg)  
**Safety Classification Key:**  
- **[VERIFIED]**: Confirmed by firmware definitions (`config.h`), schematic netlists, and physical pinouts.  
- **[MEASURED]**: Requires direct digital multimeter measurement before connection.  
- **[ESTIMATED]**: Calculated nominal values based on 4× AA series chemistry (6.0V nominal, 6.4V fresh, 4.4V cutoff).  
- **[PENDING PHYSICAL DELIVERY]**: Real-world bench test required once physical hardware arrives.

---

## 1. Safety Rules Before Touching Any Wire

> [!CAUTION]
> **NO MASTER POWER SWITCH IS INSTALLED IN THIS VEHICLE.**  
> - Once the 4th AA battery is inserted or the battery pack is plugged in, the **battery rail is LIVE**.  
> - **DO NOT CONNECT THE ESP32 OR MOTOR DRIVER LOGIC TO THE DC-DC CONVERTER UNTIL ITS OUTPUT IS CALIBRATED WITH A MULTIMETER TO EXACTLY 5.00 V.** Supplying uncalibrated voltage (> 6V or reverse polarity) will permanently destroy the ESP32 microcontroller!  
> - Work with the **battery pack completely disconnected** (or one AA cell removed) during all wiring steps.

---

## 2. Power Distribution Wiring Table (PWR-01 to PWR-13)

| Wire ID | Source Component & Terminal | Destination Component & Terminal | Wire Gauge / Type | Color | Functional Purpose | Multimeter Pre-Connection Verification Step | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **PWR-01** | 4× AA Tray Positive (+) lead | Central V+ Distribution Bus / Barrier Strip | 20–22 AWG Stranded | **Red** | Unswitched raw battery positive rail (~6.0V nominal) | Measure with DMM in DC Volts mode across battery leads: 4× fresh alkaline must read **6.0V to 6.4V**. | **[MEASURED]** |
| **PWR-02** | Central V+ Distribution Bus | DC-DC Converter Terminal `IN+` | 20–22 AWG Stranded | **Red** | DC-DC converter raw input supply | DMM continuity check from battery positive to `IN+`: < 0.2 Ω. | **[VERIFIED]** |
| **PWR-03** | Central V+ Distribution Bus | Left L298N (#1) Terminal `+12V` (VS) | 20–22 AWG Stranded | **Red** | Motor supply rail for Left Drivetrain (FL & RL motors) | Check resistance between `+12V` and `GND` before powering: must be > 10 kΩ (not shorted). | **[VERIFIED]** |
| **PWR-04** | Central V+ Distribution Bus | Right L298N (#2) Terminal `+12V` (VS) | 20–22 AWG Stranded | **Red** | Motor supply rail for Right Drivetrain (FR & RR motors) | Check resistance between `+12V` and `GND` before powering: must be > 10 kΩ (not shorted). | **[VERIFIED]** |
| **PWR-05** | 4× AA Tray Negative (-) lead | Central Star Ground (GND) Bus Point | 20–22 AWG Stranded | **Black** | Main system battery ground return | Measure continuity from battery negative terminal to star bus: < 0.2 Ω. | **[MEASURED]** |
| **PWR-06** | Central Star Ground Bus | DC-DC Converter Terminal `IN-` | 20–22 AWG Stranded | **Black** | DC-DC converter input ground return | Verify direct low-resistance connection to star point (< 0.2 Ω). | **[VERIFIED]** |
| **PWR-07** | DC-DC Converter Terminal `OUT-` | Central Star Ground Bus | 20–22 AWG Stranded | **Black** | Regulated logic return to star ground | Check continuity between `IN-` and `OUT-` on converter (most modules share common ground plane). | **[VERIFIED]** |
| **PWR-08** | Left L298N (#1) Terminal `GND` | Central Star Ground Bus | 20–22 AWG Stranded | **Black** | Left motor driver high-current ground return | Inspect mechanical tightness in screw terminal; verify low impedance (< 0.2 Ω). | **[VERIFIED]** |
| **PWR-09** | Right L298N (#2) Terminal `GND` | Central Star Ground Bus | 20–22 AWG Stranded | **Black** | Right motor driver high-current ground return | Inspect mechanical tightness in screw terminal; verify low impedance (< 0.2 Ω). | **[VERIFIED]** |
| **PWR-10** | ESP32 DevKit Board Pin `GND` | Central Star Ground Bus | 22–24 AWG Stranded (Dupont-to-bus) | **Black** | Microcontroller reference ground | Verify continuity from ESP32 GND pin to Star Ground block (< 0.3 Ω). | **[VERIFIED]** |
| **PWR-11** | DC-DC Converter Terminal `OUT+` | ESP32 DevKit Pin `VIN` (5V Input) | 22 AWG Stranded (Dupont-to-bare) | **Cyan / Red** | Regulated 5.00V supply to ESP32 onboard LDO | **CRITICAL:** Connect multimeter to `OUT+` and `OUT-`. Adjust 3296W trimpot until voltage reads **5.00V ± 0.05V**. Do NOT plug into ESP32 until verified! | **[MEASURED]** |
| **PWR-12** | DC-DC Converter Terminal `OUT+` (or 5V Bus) | Left L298N (#1) Terminal `+5V` | 22–24 AWG Stranded | **Cyan / Blue** | Regulated 5.00V logic supply for Left L298 internal gates | Verify Left L298N **`5V-EN` jumper is REMOVED** before making this connection! | **[VERIFIED]** |
| **PWR-13** | DC-DC Converter Terminal `OUT+` (or 5V Bus) | Right L298N (#2) Terminal `+5V` | 22–24 AWG Stranded | **Cyan / Blue** | Regulated 5.00V logic supply for Right L298 internal gates | Verify Right L298N **`5V-EN` jumper is REMOVED** before making this connection! | **[VERIFIED]** |

---

## 3. Left Drivetrain Motor Wiring (MOT-01 to MOT-04)

Both left motors are controlled in parallel by L298N Module #1 (Channel A = Front-Left, Channel B = Rear-Left).

| Wire ID | Source Terminal | Destination Motor Terminal | Gauge / Type | Color | Motor & Motion Channel | Verification Check |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MOT-01** | Left L298N `OUT1` | Front-Left TT Motor Terminal (+) | 22 AWG Stranded | **Red** | Front-Left (FL) Forward Drive | Measure motor DC winding resistance: ~2.5 to 4.5 Ω. Confirm 100nF capacitor is soldered across terminals. |
| **MOT-02** | Left L298N `OUT2` | Front-Left TT Motor Terminal (-) | 22 AWG Stranded | **Black** | Front-Left (FL) Reverse Return | Elevated bench spin: When IN1=HIGH, IN2=LOW, FL wheel must rotate **forward**. Swap MOT-01/02 if backwards. |
| **MOT-03** | Left L298N `OUT3` | Rear-Left TT Motor Terminal (+) | 22 AWG Stranded | **Red** | Rear-Left (RL) Forward Drive | Measure motor DC winding resistance: ~2.5 to 4.5 Ω. Confirm 100nF capacitor is soldered across terminals. |
| **MOT-04** | Left L298N `OUT4` | Rear-Left TT Motor Terminal (-) | 22 AWG Stranded | **Black** | Rear-Left (RL) Reverse Return | Elevated bench spin: When IN3=HIGH, IN4=LOW, RL wheel must rotate **forward**. Swap MOT-03/04 if backwards. |

---

## 4. Right Drivetrain Motor Wiring (MOT-05 to MOT-08)

Both right motors are controlled in parallel by L298N Module #2 (Channel A = Front-Right, Channel B = Rear-Right).

| Wire ID | Source Terminal | Destination Motor Terminal | Gauge / Type | Color | Motor & Motion Channel | Verification Check |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MOT-05** | Right L298N `OUT1` | Front-Right TT Motor Terminal (+) | 22 AWG Stranded | **Red** | Front-Right (FR) Forward Drive | Measure motor DC winding resistance: ~2.5 to 4.5 Ω. Confirm 100nF capacitor is soldered across terminals. |
| **MOT-06** | Right L298N `OUT2` | Front-Right TT Motor Terminal (-) | 22 AWG Stranded | **Black** | Front-Right (FR) Reverse Return | Elevated bench spin: When IN1=HIGH, IN2=LOW, FR wheel must rotate **forward**. Swap MOT-05/06 if backwards. |
| **MOT-07** | Right L298N `OUT3` | Rear-Right TT Motor Terminal (+) | 22 AWG Stranded | **Red** | Rear-Right (RR) Forward Drive | Measure motor DC winding resistance: ~2.5 to 4.5 Ω. Confirm 100nF capacitor is soldered across terminals. |
| **MOT-08** | Right L298N `OUT4` | Rear-Right TT Motor Terminal (-) | 22 AWG Stranded | **Black** | Rear-Right (RR) Reverse Return | Elevated bench spin: When IN3=HIGH, IN4=LOW, RR wheel must rotate **forward**. Swap MOT-07/08 if backwards. |

---

## 5. Left Motor Control Signal Ribbon (SIG-L01 to SIG-L06)

Connects ESP32 GPIO outputs to Left L298N (#1) logic header. **Dupont Female-to-Female jumper ribbon (26–28 AWG).**

| Wire ID | ESP32 Pin | Left L298N Pin | Color | Function in Firmware (`config.h`) | Electrical Behavior |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SIG-L01** | `GPIO 18` | `ENA` | **Yellow** | `PIN_LEFT_ENA` (PWM Channel 0) | 20 kHz ultrasonic hardware PWM (0–255 duty cycle). Controls FL motor speed. **Jumper ENA MUST BE REMOVED.** |
| **SIG-L02** | `GPIO 19` | `IN1` | **Green** | `PIN_LEFT_IN1` (FL Forward) | Logic HIGH (3.3V) commands Front-Left motor forward. |
| **SIG-L03** | `GPIO 17` | `IN2` | **Blue** | `PIN_LEFT_IN2` (FL Reverse) | Logic HIGH (3.3V) commands Front-Left motor reverse. |
| **SIG-L04** | `GPIO 16` | `IN3` | **White** | `PIN_LEFT_IN3` (RL Forward) | Logic HIGH (3.3V) commands Rear-Left motor forward. |
| **SIG-L05** | `GPIO 4` | `IN4` | **Purple** | `PIN_LEFT_IN4` (RL Reverse) | Logic HIGH (3.3V) commands Rear-Left motor reverse. |
| **SIG-L06** | `GPIO 25` | `ENB` | **Orange** | `PIN_LEFT_ENB` (PWM Channel 1) | 20 kHz ultrasonic hardware PWM (0–255 duty cycle). Controls RL motor speed. **Jumper ENB MUST BE REMOVED.** |

---

## 6. Right Motor Control Signal Ribbon (SIG-R01 to SIG-R06)

Connects ESP32 GPIO outputs to Right L298N (#2) logic header. **Dupont Female-to-Female jumper ribbon (26–28 AWG).**

| Wire ID | ESP32 Pin | Right L298N Pin | Color | Function in Firmware (`config.h`) | Electrical Behavior |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **SIG-R01** | `GPIO 23` | `ENA` | **Yellow** | `PIN_RIGHT_ENA` (PWM Channel 2) | 20 kHz ultrasonic hardware PWM (0–255 duty cycle). Controls FR motor speed. **Jumper ENA MUST BE REMOVED.** |
| **SIG-R02** | `GPIO 22` | `IN1` | **Green** | `PIN_RIGHT_IN1` (FR Forward) | Logic HIGH (3.3V) commands Front-Right motor forward. |
| **SIG-R03** | `GPIO 21` | `IN2` | **Blue** | `PIN_RIGHT_IN2` (FR Reverse) | Logic HIGH (3.3V) commands Front-Right motor reverse. |
| **SIG-R04** | `GPIO 27` | `IN3` | **White** | `PIN_RIGHT_IN3` (RR Forward) | Logic HIGH (3.3V) commands Rear-Right motor forward. |
| **SIG-R05** | `GPIO 14` | `IN4` | **Purple** | `PIN_RIGHT_IN4` (RR Reverse) | Logic HIGH (3.3V) commands Rear-Right motor reverse. |
| **SIG-R06** | `GPIO 26` | `ENB` | **Orange** | `PIN_RIGHT_ENB` (PWM Channel 3) | 20 kHz ultrasonic hardware PWM (0–255 duty cycle). Controls RR motor speed. **Jumper ENB MUST BE REMOVED.** |

---

## 7. Battery Telemetry & Sensing Wiring (SNS-01 to SNS-03)

Monitors the unswitched 4× AA battery voltage safely via an external resistive divider connected to ESP32 ADC1 (GPIO 34).

| Wire ID | Source Component & Node | Destination Component & Node | Wire Gauge / Type | Color | Function | Multimeter Verification Step |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **SNS-01** | Central V+ Distribution Bus | Voltage Divider Resistor $R_1$ Top ($100\,\text{k}\Omega$) | 24 AWG Stranded | **Red** | Battery rail voltage pickoff | Verify continuity to unswitched battery positive rail (< 0.5 Ω). |
| **SNS-02** | Resistor $R_2$ Bottom ($20\,\text{k}\Omega$) | Central Star Ground Bus | 24 AWG Stranded | **Black** | Divider reference ground return | Measure total resistance across $R_1 + R_2$ with no power applied: must equal $120.0\,\text{k}\Omega \pm 2\%$. |
| **SNS-03** | Divider Center Tap (between $R_1$ and $R_2$) | ESP32 DevKit Pin `GPIO 34` (ADC1_CH6) | 26 AWG Dupont Female | **Cyan / Green** | Attenuated analog voltage signal (6:1 division ratio) | With fresh 6.0V batteries installed, measure DC voltage at center tap with DMM before connecting to ESP32: **must read 0.98V to 1.05V** (never exceed 3.3V!). |

---

## 8. Summary of Physical Jumper Positions

Before powering the car, verify all six jumpers across the two L298N boards:

```text
Left L298N Module (DRV-01):
├── 5V-EN Jumper:   [ REMOVED ]  <-- CRITICAL: Isolates internal 78M05 regulator
├── ENA Jumper:     [ REMOVED ]  <-- CRITICAL: Allows ESP32 GPIO 18 PWM control
└── ENB Jumper:     [ REMOVED ]  <-- CRITICAL: Allows ESP32 GPIO 25 PWM control

Right L298N Module (DRV-02):
├── 5V-EN Jumper:   [ REMOVED ]  <-- CRITICAL: Isolates internal 78M05 regulator
├── ENA Jumper:     [ REMOVED ]  <-- CRITICAL: Allows ESP32 GPIO 23 PWM control
└── ENB Jumper:     [ REMOVED ]  <-- CRITICAL: Allows ESP32 GPIO 26 PWM control
```

---

## 9. Safe Power Disconnection Procedure (No Switch Installed)

Because this chassis build does not include a toggle or slide power switch, operators must use one of the following safe isolation procedures:

1. **Primary Disconnect (Recommended):**  
   Install a 2-pin JST-XH or standard 2.1mm DC barrel connector inline between the battery pack leads and the central distribution bus. To power off the vehicle, unplug this connector.
2. **Secondary Disconnect (Zero Extra Hardware):**  
   Gently pull any single AA cell out of the battery holder. This breaks the series chain and completely de-energizes the vehicle with zero standby quiescent drain.
3. **Storage Rule:**  
   Always remove all 4 AA cells from the vehicle when stored for more than 24 hours to prevent alkaline electrolyte leakage.
