# ROBO RACER 4WD — PHYSICAL WIRING & ASSEMBLY GUIDE

**Document Revision:** v1.2 — Physical Delivery Baseline  
**Hardware Target:** 4WD Robotic Car Kit | 4× AA Battery Pack (Direct Unswitched) | Dual-Inductor SEPIC DC-DC Converter | 2× L298N Motor Drivers | ESP32 DevKit V1 (30-pin) | 4× TT DC Geared Motors  
**Related Documents:**  
- [`robo-racer-full-wiring.svg`](robo-racer-full-wiring.svg) (Master Vector Schematic)  
- [`robo-racer-wire-by-wire.md`](robo-racer-wire-by-wire.md) (Wire-by-Wire Pinout Table)  
- [`robo-racer-electrical-test-procedure.md`](robo-racer-electrical-test-procedure.md) (Staged Commissioning Procedure)

---

## 1. Safety Overview & Mechanical Pre-Checks

Before beginning assembly, inspect the physical parts against the kit inventory:
1. **Remove Acrylic Protective Film:** The brownish/tan paper on the chassis plates is protective masking paper. Peel it off to expose the clear transparent or amber acrylic underneath.
2. **Tools Required:**
   - Small Phillips cross-head screwdriver (#1 and #0).
   - Small flat-blade precision screwdriver (2.0mm or 2.5mm for L298N and DC-DC screw terminals and 3296W trimpot).
   - Digital Multimeter with fine probe leads.
   - Soldering iron and flux-core solder (for attaching motor leads and 100nF decoupling capacitors).
   - Wire strippers and flush side-cutters.
   - Small zip ties (100mm) and double-sided foam tape or nylon standoffs (M3).

> [!CAUTION]
> **NO POWER SWITCH IS INSTALLED IN THIS KIT.**  
> Never install batteries into the tray while wiring or tightening terminal screws. The system must remain **100% de-energized** until Stage 1 of the electrical test procedure is completed.

---

## 2. Component Placement on Chassis

To maintain a low center of gravity, clean cable routing, and minimal electromagnetic interference (EMI), adhere to this physical arrangement:

```text
               [ FRONT OF VEHICLE ]
  [FL Motor]                          [FR Motor]
      │                                   │
      ├───[L298N Driver #1]   [L298N Driver #2]───┤
      │   (Left Side)             (Right Side)    │
      │                                           │
      │          [ESP32 DevKit V1]                │
      │           (Center Deck)                   │
      │                                           │
      │       [SEPIC DC-DC Converter]             │
      │          (Center Lower)                   │
      │                                           │
      ├───[Central Star GND & V+ Bus]─────────────┤
      │                                           │
      └───[4× AA Battery Holder Tray]─────────────┘
  [RL Motor]      (Rear Deck)         [RR Motor]
               [ REAR OF VEHICLE ]
```

### Component Mounting Recommendations:
1. **TT Motors (4×):** Mount under the lower acrylic plate using the long M3 machine screws and motor brackets provided in the kit. Ensure motor output shafts point outward and wires face inward toward the chassis center.
2. **L298N Driver #1 (Left Drivetrain):** Mount along the left perimeter using 4× M3 nylon standoffs (10–15mm height). Orient the heat sink toward the vehicle center and the logic pins toward the ESP32.
3. **L298N Driver #2 (Right Drivetrain):** Mount along the right perimeter using 4× M3 nylon standoffs. Orient the heat sink inward and logic pins toward the ESP32.
4. **ESP32 DevKit V1:** Mount along the vehicle centerline on the upper deck using adhesive nylon standoffs or a mini solderless breadboard. Ensure the Micro-USB port faces rearward or outward for easy firmware flashing and serial debugging.
5. **SEPIC DC-DC Converter:** Mount centered on the lower deck between the driver boards using M3 nylon standoffs. Orient the blue 3296W trimpot so the brass screw is easily accessible from above with a precision screwdriver.
6. **4× AA Battery Tray:** Mount longitudinally or transversely along the rear deck where its mass sits securely over the rear axle. Secure using dual-lock velcro or zip ties through the chassis perforations for quick removal.

---

## 3. Critical Jumper Configuration

Each red L298N motor driver board comes from the factory with three black shunt jumpers installed:
1. `5V-EN` (Logic Regulator Enable) jumper on the 3-pin header behind the screw terminals.
2. `ENA` (Channel A Enable) jumper tying ENA directly to onboard 5V.
3. `ENB` (Channel B Enable) jumper tying ENB directly to onboard 5V.

### Step-by-Step Jumper Removal on BOTH Boards:

```text
  L298N BOARD JUMPER LOCATIONS:

      [ HEATSINK ]
   
      ┌──────────┐
      │  5V-EN   │  <─── [ACTION: PULL AND REMOVE THIS JUMPER]
      └──────────┘       (Disables internal 78M05 regulator to protect it from 5V conflict)

      ┌────┐ ┌────┐
      │ENA │ │ENB │ <─── [ACTION: PULL AND REMOVE BOTH JUMPERS]
      └────┘ └────┘      (Allows ESP32 hardware PWM speed control on ENA and ENB)
      IN1 IN2 IN3 IN4
```

> [!IMPORTANT]
> **Why `5V-EN` must be REMOVED:**  
> When the 5V-EN jumper is present, the L298N's internal 78M05 linear regulator attempts to produce 5.0V from the motor supply rail. However, we are supplying regulated 5.00V from the SEPIC DC-DC converter directly into the `+5V` terminal. If the jumper is left installed, two independent voltage regulators will fight each other on the same rail, causing excessive heating, back-feeding, and regulator destruction!
>
> **Why `ENA` and `ENB` jumpers must be REMOVED:**  
> The factory jumpers hard-wire the H-bridge enables to logic HIGH (+5V). If left in place, the motors can only run at 100% full speed or full stop; speed ramping, reverse dwell, and smooth cornering PWM control will NOT work.

---

## 4. Phase-by-Phase Wiring Instructions

### Phase 1: Motor Connections & Noise Suppression
1. Solder a **100 nF (0.1 µF, code "104") ceramic disc capacitor** directly across the two metal soldering lugs of each TT motor. This suppresses brush arcing and high-frequency RF noise that could crash the ESP32.
2. Solder a red 22 AWG wire to the motor positive tab and a black 22 AWG wire to the negative tab. Twist the pair tightly (~2 twists per inch) to cancel magnetic field radiation.
3. Route the motor wire pairs to the respective L298N screw terminals:
   - Front-Left motor -> Left L298N `OUT1` (+) and `OUT2` (-).
   - Rear-Left motor  -> Left L298N `OUT3` (+) and `OUT4` (-).
   - Front-Right motor -> Right L298N `OUT1` (+) and `OUT2` (-).
   - Rear-Right motor  -> Right L298N `OUT3` (+) and `OUT4` (-).
4. Strip 6mm of insulation, insert into the green screw terminal cage, and tighten firmly. Tug gently on each wire to confirm mechanical retention.

### Phase 2: Central Power & Star Ground Buses
1. Establish a **Central V+ Distribution Point** (a 4-way WAGO connector, dual-row terminal barrier block, or soldered/heat-shrinked tap):
   - Battery Tray Positive lead (Red, 20 AWG).
   - Branch to DC-DC Converter `IN+` (Red, 20 AWG).
   - Branch to Left L298N `+12V` terminal (Red, 20 AWG).
   - Branch to Right L298N `+12V` terminal (Red, 20 AWG).
   - Branch to Voltage Divider Resistor $R_1$ (Red, 24 AWG).
2. Establish a **Central Star Ground Point**:
   - Battery Tray Negative lead (Black, 20 AWG).
   - DC-DC Converter `IN-` and `OUT-` (Black, 20 AWG).
   - Left L298N `GND` terminal (Black, 20 AWG).
   - Right L298N `GND` terminal (Black, 20 AWG).
   - ESP32 board `GND` pin (Black, 22 AWG).
   - Voltage Divider Resistor $R_2$ ground lead (Black, 24 AWG).

### Phase 3: DC-DC Output Calibration (MANDATORY BEFORE CONNECTING ESP32)
1. **DO NOT connect wires to ESP32 `VIN` or L298N `+5V` yet.**
2. Connect DC-DC Converter `IN+` to battery positive and `IN-` to star ground.
3. Insert 4 fresh AA batteries into the tray.
4. Attach digital multimeter probes in DC Volts mode across DC-DC converter `OUT+` and `OUT-`.
5. Using a small screwdriver, gently turn the brass adjustment screw on the blue 3296W trimpot:
   - Clockwise usually lowers voltage; counter-clockwise usually raises voltage (or vice-versa depending on manufacturer).
   - Adjust until the meter reads **exactly 5.00 V** (acceptable range: 4.95V to 5.05V).
6. Disconnect the battery pack.
7. Now connect:
   - DC-DC Converter `OUT+` -> ESP32 `VIN` pin (`PWR-11`).
   - DC-DC Converter `OUT+` -> Left L298N `+5V` terminal (`PWR-12`).
   - DC-DC Converter `OUT+` -> Right L298N `+5V` terminal (`PWR-13`).

### Phase 4: ESP32 Logic Signal Ribbons
Using two 6-conductor Dupont Female-to-Female ribbons (26–28 AWG):
1. **Left Ribbon (DRV-01 Control):**
   - ESP32 `GPIO 18` -> Left L298N `ENA` (Yellow)
   - ESP32 `GPIO 19` -> Left L298N `IN1` (Green)
   - ESP32 `GPIO 17` -> Left L298N `IN2` (Blue)
   - ESP32 `GPIO 16` -> Left L298N `IN3` (White)
   - ESP32 `GPIO 4`  -> Left L298N `IN4` (Purple)
   - ESP32 `GPIO 25` -> Left L298N `ENB` (Orange)
2. **Right Ribbon (DRV-02 Control):**
   - ESP32 `GPIO 23` -> Right L298N `ENA` (Yellow)
   - ESP32 `GPIO 22` -> Right L298N `IN1` (Green)
   - ESP32 `GPIO 21` -> Right L298N `IN2` (Blue)
   - ESP32 `GPIO 27` -> Right L298N `IN3` (White)
   - ESP32 `GPIO 14` -> Right L298N `IN4` (Purple)
   - ESP32 `GPIO 26` -> Right L298N `ENB` (Orange)

### Phase 5: Battery Telemetry Divider Installation
1. Construct the divider using two 1% metal film resistors:
   - $R_1 = 100\,\text{k}\Omega$ (Brown-Black-Black-Orange-Brown or Brown-Black-Yellow-Gold).
   - $R_2 = 20\,\text{k}\Omega$ (Red-Black-Black-Red-Brown or Red-Black-Orange-Gold).
2. Connect $R_1$ top to unswitched battery positive rail (`SNS-01`).
3. Connect $R_2$ bottom to Star Ground (`SNS-02`).
4. Solder a wire from the junction point between $R_1$ and $R_2$ (`SNS-03`) to ESP32 `GPIO 34`.
5. *(Recommended)* Solder a 100nF ceramic capacitor across $R_2$ (between GPIO 34 and GND) to smooth out ADC sampling noise.
6. Insulate the entire divider assembly in heat-shrink tubing to prevent accidental shorts against the chassis.

---

## 5. Cable Management & Noise Isolation Best Practices

1. **Physical Route Separation:**
   - Run the high-current motor power wires along the outer perimeter chassis edges.
   - Run the sensitive logic Dupont ribbons along the centerline chassis deck.
   - **Never zip-tie the motor wires together with the ESP32 GPIO ribbons.** High-frequency PWM spikes (20 kHz) and brush commutation transients can induce cross-talk into the logic lines.
2. **Twisted Pairs:**
   - Twist the two leads for each motor tightly.
   - Twist the battery positive and negative leads together between the battery tray and the distribution bus.
3. **Strain Relief:**
   - Secure the battery leads and motor leads to the chassis frame with small zip ties 20–30mm before they enter screw terminals. This prevents vibration from working the copper strands loose.
4. **Accessible Battery Disconnect:**
   - Position the inline battery connector (or the battery holder tray itself) so it is instantly reachable from the top or rear of the car without needing to flip the car upside down or use tools.

---

## 6. Pre-Power Checklist

Before connecting the battery pack for the first time, check off every item:

- [ ] All 4 TT motor wiring pairs twisted and fitted with 100nF suppression capacitors.
- [ ] Left L298N jumpers `5V-EN`, `ENA`, `ENB` confirmed **REMOVED**.
- [ ] Right L298N jumpers `5V-EN`, `ENA`, `ENB` confirmed **REMOVED**.
- [ ] DC-DC Converter output voltage calibrated to **5.00V ± 0.05V** with multimeter.
- [ ] Star Ground bus connected to Battery Negative, DC-DC `IN-`/`OUT-`, both L298N `GND` pins, ESP32 `GND`, and divider $R_2$.
- [ ] Resistance between Central V+ Bus and Star GND with no battery is > 10 kΩ (NO short circuits).
- [ ] Resistance between 5.0V Logic Rail and Star GND is > 1 kΩ (NO short circuits).
- [ ] Dupont signal ribbons seated firmly on ESP32 and L298N headers in correct pin order.
- [ ] Voltage divider center tap connected to ESP32 GPIO 34 (input only).
- [ ] Wheels elevated on a test stand (clear of desk surface).
