#pragma once
#include <Arduino.h>

// ============================================================================
// ROBO RACER FIRMWARE CONFIGURATION v1.2
// Hardware: Prakti ESP32 Dev Board (30-pin, CP2102)
// Chassis: 4WD Robot Kit with 4x TT DC Gear Motors
// Drivers: 2x L298N Dual H-Bridge Motor Drivers (Left & Right 2WD)
// Power: 4x AA Battery Pack (~6.0V nominal) via Auto Buck-Boost DC-DC to 5.0V Logic
// ============================================================================

// DEVICE BLUETOOTH IDENTITY
#define DEVICE_NAME "ROBO-RACER-ESP32"

// SAFE GPIO ALLOCATION
// IMPORTANT: Strapping pins (GPIO 0, 2, 5, 12, 15) are strictly avoided for motor control
// to prevent spurious motor bursts during ESP32 boot/reset.

// LEFT MOTORS (Driver 1: Front-Left & Rear-Left)
#define PIN_LEFT_ENA    18   // LEDC PWM Channel 0
#define PIN_LEFT_IN1    19   // FL Forward
#define PIN_LEFT_IN2    17   // FL Backward
#define PIN_LEFT_IN3    16   // RL Forward
#define PIN_LEFT_IN4     4   // RL Backward
#define PIN_LEFT_ENB    25   // LEDC PWM Channel 1

// RIGHT MOTORS (Driver 2: Front-Right & Rear-Right)
#define PIN_RIGHT_ENA   23   // LEDC PWM Channel 2
#define PIN_RIGHT_IN1   22   // FR Forward
#define PIN_RIGHT_IN2   21   // FR Backward
#define PIN_RIGHT_IN3   27   // RR Forward
#define PIN_RIGHT_IN4   14   // RR Backward
#define PIN_RIGHT_ENB   26   // LEDC PWM Channel 3

// TELEMETRY & SENSORS
#define PIN_BATTERY_ADC 34   // ADC1_CH6 (Input only, no internal pullup)
#define PIN_STATUS_LED   2   // Onboard Blue LED (Safe for status indicator)

// PWM CONFIGURATION (ESP32 LEDC)
#define PWM_FREQ_HZ     20000 // 20 kHz ultrasonic (silent operation, no audible motor whine)
#define PWM_RESOLUTION      8 // 8-bit resolution (0 - 255)
#define PWM_CHAN_LEFT_A     0
#define PWM_CHAN_LEFT_B     1
#define PWM_CHAN_RIGHT_A    2
#define PWM_CHAN_RIGHT_B    3

// Core 2.x vs Core 3.x dual-compatibility LEDC macros
#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
  #define PWM_LEDC_ATTACH(pin, chan, freq, res) ledcAttach((pin), (freq), (res))
  #define PWM_LEDC_WRITE(pin, chan, duty)       ledcWrite((pin), (duty))
#else
  #define PWM_LEDC_ATTACH(pin, chan, freq, res) do { ledcSetup((chan), (freq), (res)); ledcAttachPin((pin), (chan)); } while(0)
  #define PWM_LEDC_WRITE(pin, chan, duty)       ledcWrite((chan), (duty))
#endif

// SAFETY LIMITS & TIMING
// Note on L298N H-Bridge Stop State:
// When PWM duty is 0 (ENA/ENB disabled) and IN1..IN4 are LOW, the L298N outputs are in
// High-Z "Free Running Motor Stop" (unpowered coasting). This disconnects motor drive safely
// without thermal stress, but does NOT represent active dynamic or regenerative braking.
#define WATCHDOG_TIMEOUT_MS  500  // Cut motor power if no fresh command received for 500ms
#define TELEMETRY_INTERVAL_MS 50  // Send telemetry at 20 Hz
#define MAX_DUTY_CYCLE_FWD   255  // 100% duty cycle forward
#define MAX_DUTY_CYCLE_REV   160  // ~63% duty cycle reverse limit for mechanical stability
#define MIN_START_DUTY        45  // Minimum deadband duty cycle to overcome TT gearbox friction
#define REVERSE_DWELL_MS     500  // Zero-output electrical/mechanical settling window during direction reversal

// ============================================================================
// BATTERY VOLTAGE MONITORING & MULTIMETER CALIBRATION
// Target Chemistry: 4x AA Alkaline Cells (Duracell / Eveready) in Series
// Fresh open-circuit: ~1.60V/cell (6.40V pack max)
// Nominal open-circuit: ~1.50V/cell (6.00V pack nominal)
// Discharged cutoff: ~1.10V/cell (4.40V pack cutoff threshold)
//
// NOTE ON VOLTAGE SAG VS TRUE CAPACITY:
// Ordinary AA alkaline cells exhibit high internal resistance (Ri ≈ 0.15 - 0.30 Ω/cell,
// total pack Ri ≈ 0.6 - 1.2 Ω fresh, rising as cells deplete). Under 4-motor inrush
// or stall, loaded terminal voltage sags significantly below open-circuit voltage.
// ADC measurements reflect instantaneous terminal voltage, NOT coulomb-counted capacity.
//
// Voltage divider: R1 = 100kΩ, R2 = 20kΩ (Nominal Ratio: (100k + 20k) / 20k = 6.0:1)
// 6.4V max input -> ~1.067V at ADC pin (well inside ESP32 0 - 3.3V linear range)
//
// PHYSICAL MULTIMETER CALIBRATION PROCEDURE (MANDATORY BEFORE RELYING ON DATA):
// 1. Measure the battery terminal voltage directly with a calibrated digital multimeter:
//    Record as V_pack (e.g. 6.18 V).
// 2. Measure the voltage present between ESP32 pin GPIO 34 and GND:
//    Record as V_adc_pin (e.g. 1.030 V).
// 3. Calculate true divider ratio: RATIO = V_pack / V_adc_pin (e.g. 6.18 / 1.030 = 6.000).
// 4. Enter the computed ratio into BATT_DIVIDER_RATIO below.
// 5. Change BATT_CALIBRATION_CONFIRMED from 0 to 1.
//    (If set to 0, telemetry explicitly flags battery data as uncalibrated/invalid).
// ============================================================================
#define BATT_CALIBRATION_CONFIRMED 0    // Set to 1 ONLY after multimeter confirmation
#define BATT_DIVIDER_RATIO        6.0f  // Multimeter measured ratio: V_battery / V_adc_pin
#define BATT_ADC_VREF             3.3f
#define BATT_ADC_MAX              4095.0f

// Battery Cell Parameters (4x AA Alkaline Series Pack)
#define BATT_CELLS                4
#define BATT_CELL_MAX_MV          1600  // 1.60V fresh cell -> 6400 mV full charge pack
#define BATT_CELL_NOMINAL_MV      1500  // 1.50V nominal alkaline -> 6000 mV nominal pack
#define BATT_CELL_CUTOFF_MV       1100  // 1.10V per cell -> 4400 mV low-voltage cutoff threshold
#define BATT_MAX_VOLTAGE_MV       (BATT_CELLS * BATT_CELL_MAX_MV)
#define BATT_LOW_VOLTAGE_MV       (BATT_CELLS * BATT_CELL_CUTOFF_MV)

// Physical Sanity Limits for 4x AA Pack
#define BATT_MIN_PLAUSIBLE_MV     3500  // Readings < 3.5V indicate disconnected divider / floating pin
#define BATT_MAX_PLAUSIBLE_MV     7500  // Readings > 7.5V indicate incorrect divider, wrong battery pack, or overvoltage
