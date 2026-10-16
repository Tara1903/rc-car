# Robo Racer — ESP32 4WD Native Android Controller & Firmware

Robo Racer is a production-grade native Android application and ESP32 embedded firmware system designed to control a physical four-wheel-drive (4WD) robotic RC car. The system combines a cyberpunk landscape controller UI built with Jetpack Compose and OpenGL ES 3D rendering with hard-real-time motor control, safety interlocks, and Bluetooth Classic SPP communications.

---

## 1. System Architecture

```
+-----------------------------------------------------------------------------------+
|                            ROBO RACER ANDROID APP                                 |
|                                                                                   |
|  +--------------------+     +-------------------+     +------------------------+  |
|  | Multi-Touch HUD    | --> | Safety Interlock  | --> | Differential Drive     |  |
|  | - Green Gas Pedal  |     | - Brake Priority  |     |   Mixer                |  |
|  | - Red Brake Pedal  |     | - Spin Protection |     | - Steering sensitivity |  |
|  | - Left/Right Steer |     | - 500ms Dwell     |     | - Reverse cap          |  |
|  +--------------------+     +-------------------+     +------------------------+  |
|                                                                   |               |
|  +--------------------+     +-------------------+                 v               |
|  | 3D OpenGL Viewport |     | Reactive HUD Bar  |     +------------------------+  |
|  | - Double Acrylic   |     | - Honest Telemetry|     | Single-Writer Conflated|  |
|  | - ESP32 & L298N    |     | - Batt: --.-V/--% |     | Command Channel        |  |
|  | - 4 Spinning Wheels|     | - Speed: -- KM/H  |     | - Emergency Stop Flush |  |
|  +--------------------+     +-------------------+     +------------------------+  |
|                                                                   |               |
+-------------------------------------------------------------------|---------------+
                                                                    | Bluetooth SPP
                                                                    | (11-byte frame)
                                                                    v
+-----------------------------------------------------------------------------------+
|                            ESP32 ROBOT FIRMWARE                                   |
|                                                                                   |
|  +--------------------+     +-------------------+     +------------------------+  |
|  | SPP Byte Stream    | --> | Sequence & CRC8   | --> | 500ms Failsafe         |  |
|  | Parser             |     | Freshness Check   |     | Watchdog Engine        |  |
|  +--------------------+     +-------------------+     +------------------------+  |
|                                                                   |               |
|  +--------------------+     +-------------------+                 v               |
|  | 20 Hz Telemetry    | <-- | ADC Batt Monitor  |     +------------------------+  |
|  | 13-Byte SPP Frame  |     | (GPIO 34 Divider) |     | Speed Ramp Engine with |  |
|  +--------------------+     +-------------------+     | 500ms Reverse Dwell    |  |
|                                                                   |               |
|                                                                   v               |
|                                                       +------------------------+  |
|                                                       | Dual L298N H-Bridges   |  |
|                                                       | 4x TT DC Gear Motors   |  |
|                                                       +------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Hardware Specifications

- **Microcontroller:** Prakti ESP32 Development Board (30-pin, CP2102 USB-UART, ESP32-WROOM-32).
- **Chassis:** 4WD double-deck acrylic robotic chassis with 4x yellow TT DC gear motors (1:48 gear ratio, 66 mm diameter rubber tires).
- **Motor Drivers:** 2x L298N Dual H-Bridge modules with aluminum heatsinks.
  - Driver 1: Controls Front-Left and Rear-Left motors.
  - Driver 2: Controls Front-Right and Rear-Right motors.
- **Power Architecture:**
  - 3S Li-ion battery pack (11.1V nominal, 12.6V maximum).
  - Regulated down to 6.0V via amiciSolar XL6019 high-efficiency buck-boost DC-DC converter for motor power rails.
  - ESP32 powered via regulated 5V on VIN pin or micro-USB.
- **Battery Voltage Divider:**
  - $R_1 = 100\,\text{k}\Omega$, $R_2 = 20\,\text{k}\Omega$ (Divider ratio: $6.0:1$).
  - Connected to `GPIO 34` (ADC1 Channel 6).
  - At $12.6\,\text{V}$, pin voltage is $2.1\,\text{V}$, comfortably within the ESP32 ADC $0 - 3.3\,\text{V}$ input range.

---

## 3. Safe GPIO Pinout

All motor control and enable pins are mapped to safe, non-strapping general-purpose GPIOs.

| Subsystem | Signal Name | ESP32 GPIO | Description / Mode |
|---|---|---|---|
| **Driver 1 (Left Motors)** | `PIN_LEFT_ENA` | **GPIO 18** | LEDC PWM Channel 0 (20 kHz, 8-bit) |
| | `PIN_LEFT_IN1` | **GPIO 19** | Front-Left Forward Direction |
| | `PIN_LEFT_IN2` | **GPIO 17** | Front-Left Reverse Direction |
| | `PIN_LEFT_IN3` | **GPIO 16** | Rear-Left Forward Direction |
| | `PIN_LEFT_IN4` | **GPIO 4** | Rear-Left Reverse Direction |
| | `PIN_LEFT_ENB` | **GPIO 25** | LEDC PWM Channel 1 (20 kHz, 8-bit) |
| **Driver 2 (Right Motors)** | `PIN_RIGHT_ENA` | **GPIO 23** | LEDC PWM Channel 2 (20 kHz, 8-bit) |
| | `PIN_RIGHT_IN1` | **GPIO 22** | Front-Right Forward Direction |
| | `PIN_RIGHT_IN2` | **GPIO 21** | Front-Right Reverse Direction |
| | `PIN_RIGHT_IN3` | **GPIO 27** | Rear-Right Forward Direction |
| | `PIN_RIGHT_IN4` | **GPIO 14** | Rear-Right Reverse Direction |
| | `PIN_RIGHT_ENB` | **GPIO 26** | LEDC PWM Channel 3 (20 kHz, 8-bit) |
| **Telemetry & Indicators** | `PIN_BATTERY_ADC`| **GPIO 34** | ADC1_CH6 (Input-only, no internal pullup) |
| | `PIN_STATUS_LED` | **GPIO 2** | Onboard Blue Status LED |

### Critical Strapping Pin Note: GPIO 2
> [!IMPORTANT]
> `GPIO 2` is an ESP32 strapping/bootstrapping pin connected to an internal pulldown resistor that governs SPI flash programming mode at power-on reset. On the Prakti ESP32 board, `GPIO 2` is hardwired to the onboard blue LED.
> 
> In this design:
> 1. `GPIO 2` is used **exclusively as a diagnostic status LED** (`OUTPUT` mode initialized in `setup()`).
> 2. `GPIO 2` is **NEVER connected to motor drivers, H-bridge inputs, or enable pins**. Connecting strapping pins to motor drivers can cause spurious high pulses during boot, resulting in sudden motor jumps.
> 3. Other bootstrapping pins (`GPIO 0`, `GPIO 5`, `GPIO 12`, `GPIO 15`) are completely avoided for motor driving.

---

## 4. Communication Protocol Specification (v1.0)

Communications take place over standard Bluetooth Serial Port Profile (SPP / RFCOMM) using UUID `00001101-0000-1000-8000-00805F9B34FB`.

### Command Frame (Android to ESP32 — 11 Bytes, ~30 Hz)

| Byte Index | Field | Value / Range | Description |
|---|---|---|---|
| `0` | Start Byte | `0xAA` | Frame preamble |
| `1` | Protocol Version | `0x01` | Protocol version 1.0 |
| `2` | Message Type | `0x01` | Command frame identifier |
| `3` | Payload Length | `0x05` | 5 payload bytes follow |
| `4` | Left Speed | `0 - 255` | 8-bit PWM duty cycle |
| `5` | Left Direction | `0, 1, 2` | `0` = STOP, `1` = FORWARD, `2` = REVERSE |
| `6` | Right Speed | `0 - 255` | 8-bit PWM duty cycle |
| `7` | Right Direction | `0, 1, 2` | `0` = STOP, `1` = FORWARD, `2` = REVERSE |
| `8` | Sequence Number | `0 - 255` | Rolling sequence counter |
| `9` | CRC-8 | `0x00 - 0xFF` | CRC-8 checksum over bytes 0..8 (polynomial 0x07) |
| `10` | End Byte | `0x55` | Frame postamble |

### Telemetry Frame (ESP32 to Android — 13 Bytes, 20 Hz)

| Byte Index | Field | Value / Range | Description |
|---|---|---|---|
| `0` | Start Byte | `0xAA` | Frame preamble |
| `1` | Protocol Version | `0x01` | Protocol version 1.0 |
| `2` | Message Type | `0x10` | Telemetry frame identifier |
| `3` | Payload Length | `0x07` | 7 payload bytes follow |
| `4` | System State | `0, 1, 2, 3` | `0` = IDLE, `1` = RUNNING, `2` = FAILSAFE, `3` = LOW_BATT |
| `5` | Left Actual Speed | `0 - 255` | Current ramped PWM duty cycle |
| `6` | Right Actual Speed | `0 - 255` | Current ramped PWM duty cycle |
| `7` | Battery Volts MSB | `0x00 - 0xFF` | Millivolts high byte (e.g. `11800 mV` -> `0x2E18`) |
| `8` | Battery Volts LSB | `0x00 - 0xFF` | Millivolts low byte |
| `9` | Fault Bitmask | Bit flags | Bit 0: Watchdog timeout, Bit 1: Low battery |
| `10` | Firmware Counter | `0 - 255` | 20 Hz incremental heartbeat counter |
| `11` | CRC-8 | `0x00 - 0xFF` | CRC-8 checksum over bytes 0..10 (polynomial 0x07) |
| `12` | End Byte | `0x55` | Frame postamble |

---

## 5. Safety Systems & Fault Prevention

1. **Hardware Watchdog Failsafe (500 ms):**
   If no valid, fresh command frame is received within 500 ms, the ESP32 safety watchdog trips, immediately zeroing all motor duty cycles and asserting low on all H-bridge direction inputs (active electric brake).
2. **Sequence Freshness & Stale Frame Filtering:**
   Sequence numbers are checked modulo 256. Out-of-order, stale, or duplicate command frames are dropped and cannot reset the watchdog.
3. **Sensorless 500 ms Reverse Dwell Window:**
   To protect the L298N bipolar transistors from back-EMF shoot-through and safeguard the TT motor plastic gear teeth from stripping, forward-to-reverse direction changes enforce a mandatory 500 ms zero-speed dwell period before reverse polarity is applied.
4. **Brake Priority Over Acceleration:**
   Multi-touch safety interlock ensures holding the Brake pedal unconditionally overrides the Accelerate pedal, forcing vehicle deceleration to zero.
5. **Brake Blocks Spin-in-Place:**
   Holding the Brake pedal while simultaneously touching Left or Right steering strictly suppresses spin-in-place differential commands, ensuring the car remains stationary under active braking.
6. **Latched Emergency Stop:**
   Calling Emergency Stop immediately purges and drains any pending commands from the conflated queue and latches zero-speed stop frames until explicitly cleared.
7. **Reconnection Neutral Latch:**
   Reconnecting Bluetooth while fingers remain on the touchscreen latches neutral and requires releasing all touches before any active drive command can be transmitted.
8. **Honest Telemetry Policy:**
   - **Speedometer (`-- KM/H`):** Because TT gear motors lack optical/Hall wheel encoders, the app does not invent fake PWM-to-km/h conversions. Speed displays `-- KM/H` on real hardware. In simulator mode, simulated vehicle dynamics are displayed for UI demonstration.
   - **Battery Gauge (`--.-V` / `--%`):** Until an ADC reading $\ge 3000\,\text{mV}$ is validated, the battery display shows `--.-V` and `--%` with all 5 gauge segments unlit, preventing false battery readings when the divider is uncalibrated or disconnected.

---

## 6. Building & Flashing

### Android Application
- **Requirements:** Android SDK 35, JDK 17, Gradle Wrapper.
- **Build Debug APK:**
  ```powershell
  .\gradlew.bat assembleDebug
  ```
  Output APK: `app/build/outputs/apk/debug/app-debug.apk`
- **Run Unit Test Suite:**
  ```powershell
  .\gradlew.bat testDebugUnitTest
  ```

### ESP32 Firmware
Dual-build support is provided for Arduino IDE and PlatformIO:

#### Arduino IDE (Recommended for Beginners)
1. Open `firmware/RoboRacerFirmware/RoboRacerFirmware.ino` in Arduino IDE 2.x.
2. Select Board: **ESP32 Dev Module**.
3. Select Port: The COM port associated with CP2102.
4. Ensure ESP32 Arduino Core 2.x or 3.x is installed (dual-compatibility macros handle both API generations automatically).
5. Click **Upload**.

#### PlatformIO (Recommended for CLI / Automated Builds)
1. Navigate to firmware directory:
   ```powershell
   cd firmware
   pio run -e esp32dev
   ```
2. Upload to board:
   ```powershell
   pio run -e esp32dev --target upload
   ```

---

## 7. Physical Hardware Calibration & Verification Checklist

When physical components arrive:

- [ ] **Step 1: Bench Power Test (Motors Disconnected)**
  - Power ESP32 via USB and measure 5V and 3.3V rails with a digital multimeter (DMM).
  - Verify Bluetooth device advertises as `ROBO-RACER-ESP32`.
  - Connect with Android app and verify onboard blue LED changes from slow blink (1 Hz) to solid ON.
- [ ] **Step 2: Battery Divider Calibration**
  - Connect a variable power supply or known 3S Li-ion battery to the $100\,\text{k}\Omega / 20\,\text{k}\Omega$ divider.
  - Measure battery terminal voltage with DMM ($V_{\text{true}}$) and ADC pin 34 voltage ($V_{\text{pin}}$).
  - Confirm voltage divider ratio is approximately $6.0$ ($V_{\text{true}} / V_{\text{pin}}$).
  - Verify app displays calibrated voltage matching DMM within $\pm 0.1\,\text{V}$.
- [ ] **Step 3: Buck-Boost Regulator Calibration**
  - Disconnect motors from L298N output terminals.
  - Power XL6019 from 3S battery.
  - Adjust multi-turn brass potentiometer on XL6019 until output voltage measures exactly $6.0\,\text{V}$.
- [ ] **Step 4: Motor Direction & Phasing (Wheels Suspended Off Ground)**
  - Place robot chassis on a test stand so all 4 wheels rotate freely.
  - Hold neon green Accelerate pedal: verify all 4 wheels spin forward.
  - If any wheel spins backward, invert its motor wiring on the corresponding L298N terminal block.
  - Test steering left: left wheels should reverse or slow down; right wheels spin forward.
  - Test steering right: right wheels should reverse or slow down; left wheels spin forward.
- [ ] **Step 5: Failsafe & Range Test**
  - While running forward, power off phone Bluetooth or force-close app.
  - Verify motors stop within 500 ms (failsafe trip) and status LED begins fast strobe (5 Hz).
- [ ] **Step 6: Room Floor Driving**
  - Place robot on floor in an open room.
  - Test proportional steering trim, braking, and 500 ms reverse dwell transitions.
