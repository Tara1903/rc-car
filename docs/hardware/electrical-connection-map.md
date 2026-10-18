# Robo Racer 4WD â€” Electrical Connection Map & Pin Specification

**Document:** `docs/hardware/electrical-connection-map.md`
**System:** Robo Racer 4WD Autonomous Robotic Vehicle
**Firmware Version Reference:** `firmware/src/config.h` (v1.1)
**Hardware Revision:** 2Ã— L298N Dual H-Bridge Drivers, 4Ã— TT DC Motors, 1Ã— Dual-Inductor DC-DC Converter, 1Ã— 30-Pin ESP32 DevKit v1

---

## 1. Master Pin-to-Pin Connection Specification

### 1.1 Left Drivetrain Driver (`DRV-01` â€” L298N Left Module)
Controls Front-Left Motor (`ACT-01_FL`) and Rear-Left Motor (`ACT-01_RL`).

| Conn ID | Source Component & Terminal | Destination Component & Terminal | ESP32 GPIO | Function / Signal Type | Wire Color | Operating Voltage / Current | Status / Evidence |
|---|---|---|:---:|---|---|---|:---:|
| **SIG-L01** | ESP32 Header Pin 18 | `DRV-01` Logic Header `ENA` | **GPIO 18** | Front-Left Speed (LEDC PWM Ch 0, 20 kHz) | Yellow | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-L02** | ESP32 Header Pin 19 | `DRV-01` Logic Header `IN1` | **GPIO 19** | Front-Left Direction (Forward = HIGH) | Green | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-L03** | ESP32 Header Pin 17 | `DRV-01` Logic Header `IN2` | **GPIO 17** | Front-Left Direction (Backward = HIGH) | Blue | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-L04** | ESP32 Header Pin 16 | `DRV-01` Logic Header `IN3` | **GPIO 16** | Rear-Left Direction (Forward = HIGH) | White | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-L05** | ESP32 Header Pin 4 | `DRV-01` Logic Header `IN4` | **GPIO 4** | Rear-Left Direction (Backward = HIGH) | Purple | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-L06** | ESP32 Header Pin 25 | `DRV-01` Logic Header `ENB` | **GPIO 25** | Rear-Left Speed (LEDC PWM Ch 1, 20 kHz) | Orange | 3.3V Logic, < 5 mA | **VERIFIED** |
| **MOT-L01** | `DRV-01` Terminal `OUT1` | Motor Front-Left Terminal (+) | â€” | Channel A Positive Motor Drive | Red | 0â€“6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-L02** | `DRV-01` Terminal `OUT2` | Motor Front-Left Terminal (-) | â€” | Channel A Negative Motor Drive | Black | 0â€“6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-L03** | `DRV-01` Terminal `OUT3` | Motor Rear-Left Terminal (+) | â€” | Channel B Positive Motor Drive | Red | 0â€“6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-L04** | `DRV-01` Terminal `OUT4` | Motor Rear-Left Terminal (-) | â€” | Channel B Negative Motor Drive | Black | 0â€“6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **PWR-L01** | Central V+ Distribution Bus | `DRV-01` Screw Terminal `+12V` (VS) | — | Unswitched Motor Supply Rail | Red (20 AWG) | ~6.0V Nominal (4.4V–6.4V DC), up to 2.2A stall | **VERIFIED** |
| **PWR-L02** | Central Star Ground Bus | `DRV-01` Screw Terminal `GND` | — | High-Current Motor Return | Black (20 AWG)| 0V Reference | **VERIFIED** |
| **PWR-L03** | DC-DC Converter `OUT+` (5.0V Logic) | `DRV-01` Screw Terminal `+5V` | — | Regulated Logic Gate Supply | Cyan (22 AWG) | 5.00V ± 0.05V DC, < 36 mA | **VERIFIED** |

*Note on Jumpers:*
1. **`5V-EN` Jumper:** **REMOVED** on `DRV-01` to isolate internal 78M05 linear regulator and prevent reverse-feeding the 5.0V logic rail.
2. **`ENA` Jumper:** **REMOVED** to enable ESP32 GPIO 18 hardware PWM speed control.
3. **`ENB` Jumper:** **REMOVED** to enable ESP32 GPIO 25 hardware PWM speed control.

---

### 1.2 Right Drivetrain Driver (`DRV-02` — L298N Right Module)
Controls Front-Right Motor (`ACT-01_FR`) and Rear-Right Motor (`ACT-01_RR`).

| Conn ID | Source Component & Terminal | Destination Component & Terminal | ESP32 GPIO | Function / Signal Type | Wire Color | Operating Voltage / Current | Status / Evidence |
|---|---|---|:---:|---|---|---|:---:|
| **SIG-R01** | ESP32 Header Pin 23 | `DRV-02` Logic Header `ENA` | **GPIO 23** | Front-Right Speed (LEDC PWM Ch 2, 20 kHz) | Yellow | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-R02** | ESP32 Header Pin 22 | `DRV-02` Logic Header `IN1` | **GPIO 22** | Front-Right Direction (Forward = HIGH) | Green | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-R03** | ESP32 Header Pin 21 | `DRV-02` Logic Header `IN2` | **GPIO 21** | Front-Right Direction (Backward = HIGH) | Blue | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-R04** | ESP32 Header Pin 27 | `DRV-02` Logic Header `IN3` | **GPIO 27** | Rear-Right Direction (Forward = HIGH) | White | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-R05** | ESP32 Header Pin 14 | `DRV-02` Logic Header `IN4` | **GPIO 14** | Rear-Right Direction (Backward = HIGH) | Purple | 3.3V Logic, < 5 mA | **VERIFIED** |
| **SIG-R06** | ESP32 Header Pin 26 | `DRV-02` Logic Header `ENB` | **GPIO 26** | Rear-Right Speed (LEDC PWM Ch 3, 20 kHz) | Orange | 3.3V Logic, < 5 mA | **VERIFIED** |
| **MOT-R01** | `DRV-02` Terminal `OUT1` | Motor Front-Right Terminal (+) | — | Channel A Positive Motor Drive | Red | 0–6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-R02** | `DRV-02` Terminal `OUT2` | Motor Front-Right Terminal (-) | — | Channel A Negative Motor Drive | Black | 0–6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-R03** | `DRV-02` Terminal `OUT3` | Motor Rear-Right Terminal (+) | — | Channel B Positive Motor Drive | Red | 0–6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **MOT-R04** | `DRV-02` Terminal `OUT4` | Motor Rear-Right Terminal (-) | — | Channel B Negative Motor Drive | Black | 0–6.0V PWM, up to 1.1A stall | **VERIFIED** |
| **PWR-R01** | Central V+ Distribution Bus | `DRV-02` Screw Terminal `+12V` (VS) | — | Unswitched Motor Supply Rail | Red (20 AWG) | ~6.0V Nominal (4.4V–6.4V DC), up to 2.2A stall | **VERIFIED** |
| **PWR-R02** | Central Star Ground Bus | `DRV-02` Screw Terminal `GND` | — | High-Current Motor Return | Black (20 AWG)| 0V Reference | **VERIFIED** |
| **PWR-R03** | DC-DC Converter `OUT+` (5.0V Logic) | `DRV-02` Screw Terminal `+5V` | — | Regulated Logic Gate Supply | Cyan (22 AWG) | 5.00V ± 0.05V DC, < 36 mA | **VERIFIED** |

*Note on Jumpers:*
1. **`5V-EN` Jumper:** **REMOVED** on `DRV-02` to isolate internal 78M05 linear regulator.
2. **`ENA` Jumper:** **REMOVED** to enable ESP32 GPIO 23 hardware PWM speed control.
3. **`ENB` Jumper:** **REMOVED** to enable ESP32 GPIO 26 hardware PWM speed control.

---

### 1.3 Power Distribution Network & Voltage Telemetry (Direct Unswitched Architecture)

> [!NOTE]
> **NO POWER SWITCH INSTALLED**: The vehicle uses direct unswitched battery connections. Safe manual isolation is accomplished by unplugging the inline battery connector or removing one AA cell.

| Conn ID | Source Component & Terminal | Destination Component & Terminal | ESP32 GPIO | Function / Power Rail | Wire Color | Voltage / Current Rating | Status / Evidence |
|---|---|---|:---:|---|---|---|:---:|
| **PWR-01** | 4× AA Battery Tray Positive Lead | Central V+ Distribution Bus | — | Unswitched Raw Battery Rail | Red (20 AWG) | 4.4V–6.4V DC, 5A burst | **VERIFIED** |
| **PWR-02** | 4× AA Battery Tray Negative Lead | Central Star Ground (GND) Bus | — | Main Battery Ground Return | Black (20 AWG)| 0V Reference | **VERIFIED** |
| **PWR-03** | Central V+ Distribution Bus | DC-DC Converter Terminal `IN+` | — | DC-DC Converter Raw Supply | Red (20 AWG) | 4.4V–6.4V DC, ~1.5A peak | **VERIFIED** |
| **PWR-04** | Central Star Ground Bus | DC-DC Converter Terminal `IN-` / `OUT-` | — | Converter Input & Output Return | Black (20 AWG)| 0V Reference | **VERIFIED** |
| **PWR-05** | DC-DC Converter Terminal `OUT+` | ESP32 Header Pin `VIN` (5V Input) | — | Regulated Microcontroller Supply | Cyan (22 AWG) | 5.00V ± 0.05V DC, ~250 mA peak | **VERIFIED** |
| **PWR-06** | Central Star Ground Bus | ESP32 Header Pin `GND` | — | Microcontroller Reference Ground | Black (22 AWG)| 0V Reference | **VERIFIED** |
| **TEL-01** | Central V+ Distribution Bus | Resistor R1 (100 kΩ) Side A | — | High Side of Voltage Divider | Red (24 AWG) | 4.4V–6.4V DC | **VERIFIED** |
| **TEL-02** | Resistor R1 / R2 Center Tap | ESP32 Header Pin 34 | **GPIO 34** | Attenuated ADC Telemetry Input | Cyan (26 AWG) | 0.73V–1.07V DC (Safe < 3.3V) | **VERIFIED** |
| **TEL-03** | Resistor R2 (20 kΩ) Side B | Central Star Ground Bus | — | Low Side of Voltage Divider | Black (24 AWG)| 0V Reference | **VERIFIED** |

---

## 2. Pin Protection & Boot Strapping Verification

The ESP32 microcontroller features several strapping pins that exhibit specialized behavior during bootloader execution:
- **GPIO 0:** Strapping pin (Boot mode selection). Left floating/unconnected for normal flash boot.
- **GPIO 2:** Strapping pin (Download boot mode). Connected to onboard blue diagnostic status LED. Not used for motor drive.
- **GPIO 5:** Strapping pin (SDIO timing / high during boot). Avoided for motor drive to prevent motor glitches at power-up.
- **GPIO 12:** Strapping pin (Flash voltage selection â€” MTDI). If pulled HIGH at boot, flash voltage switches to 1.8V, permanently preventing boot. Strictly avoided.
- **GPIO 15:** Strapping pin (MTDO / boot debug log output). Avoided.
- **GPIO 34:** Input-only pin (ADC1_CH6). Has no internal pullup/pulldown, making it the ideal analog telemetry pin for the battery voltage divider.

All 12 allocated motor control pins (`GPIO 4, 14, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27`) are regular general-purpose output pins with clean, glitch-free startup states when initialized to `LOW` in `firmware/src/motor_controller.cpp`.
