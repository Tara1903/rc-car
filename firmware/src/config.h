#pragma once
#include <Arduino.h>

// ============================================================================
// ROBO RACER FIRMWARE CONFIGURATION v1.1
// Hardware: Prakti ESP32 Dev Board (30-pin, CP2102)
// Chassis: 4WD Robot Kit with 4x TT DC Gear Motors
// Drivers: 2x L298N Dual H-Bridge Motor Drivers
// Power: 3S Li-ion (11.1V - 12.6V) via amiciSolar XL6019 Buck-Boost to 6.0V
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
#define WATCHDOG_TIMEOUT_MS  500  // Cut motor power if no command received for 500ms
#define TELEMETRY_INTERVAL_MS 50  // Send telemetry at 20 Hz
#define MAX_DUTY_CYCLE_FWD   255  // 100% duty cycle forward
#define MAX_DUTY_CYCLE_REV   160  // ~63% duty cycle reverse limit for mechanical stability
#define MIN_START_DUTY        45  // Minimum deadband duty cycle to overcome TT gearbox friction
#define REVERSE_DWELL_MS     500  // Sensorless 500ms zero-power dwell window during forward-to-reverse transitions

// BATTERY VOLTAGE MONITORING
// Voltage divider: R1 = 100kΩ, R2 = 20kΩ (Ratio: 6.0:1)
// 12.6V max input -> 2.1V at ADC pin (well inside ESP32 0-3.3V range)
#define BATT_DIVIDER_RATIO  6.0f
#define BATT_ADC_VREF       3.3f
#define BATT_ADC_MAX        4095.0f
#define BATT_LOW_VOLTAGE_MV 9600  // 9.6V (3.2V/cell for 3S Li-ion) -> Trigger low batt fault
