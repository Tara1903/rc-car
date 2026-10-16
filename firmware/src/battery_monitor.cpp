#include "battery_monitor.h"

BatteryMonitor::BatteryMonitor()
    : smoothed_mv(0), last_sample_time(0), is_valid(false) {}

void BatteryMonitor::begin() {
    pinMode(PIN_BATTERY_ADC, INPUT);
    analogSetAttenuation(ADC_11db); // Full 0 - 3.3V range

    // Average 16 initial samples for stable power-on baseline
    uint32_t sum = 0;
    for (int i = 0; i < 16; i++) {
        sum += analogRead(PIN_BATTERY_ADC);
        delay(2);
    }
    evaluate_reading(sum / 16.0f);
}

void BatteryMonitor::update() {
    unsigned long now = millis();
    if (now - last_sample_time < 50) return; // 20 Hz sampling
    last_sample_time = now;

    uint32_t raw_adc = analogRead(PIN_BATTERY_ADC);
    evaluate_reading((float)raw_adc);
}

void BatteryMonitor::evaluate_reading(float raw_adc) {
#if (BATT_CALIBRATION_CONFIRMED == 0)
    // Multimeter verification has not yet been performed by the operator.
    // Explicitly flag measurement as uncalibrated/invalid to prevent displaying unverified voltage.
    is_valid = false;
    smoothed_mv = 0;
    return;
#endif

    // Physical validity limits:
    // 1. Floating/disconnected pin: raw ADC < 150 (~0.12V at pin)
    // 2. Saturated pin: raw ADC >= 4090 (~3.29V at pin)
    if (raw_adc < 150.0f || raw_adc >= 4090.0f) {
        is_valid = false;
        smoothed_mv = 0;
        return;
    }

    float pin_voltage = (raw_adc / BATT_ADC_MAX) * BATT_ADC_VREF;
    uint16_t current_mv = (uint16_t)(pin_voltage * BATT_DIVIDER_RATIO * 1000.0f);

    // Range validity check for configured battery pack chemistry (3S Li-ion: 6.0V to 13.5V)
    if (current_mv < BATT_MIN_PLAUSIBLE_MV || current_mv > BATT_MAX_PLAUSIBLE_MV) {
        is_valid = false;
        smoothed_mv = 0;
        return;
    }

    if (!is_valid) {
        smoothed_mv = current_mv;
        is_valid = true;
    } else {
        // Exponential moving average filter (alpha = 0.1)
        smoothed_mv = (uint16_t)(0.9f * smoothed_mv + 0.1f * current_mv);
    }
}
