#include "battery_monitor.h"

BatteryMonitor::BatteryMonitor() : smoothed_mv(11100), last_sample_time(0) {}

void BatteryMonitor::begin() {
    pinMode(PIN_BATTERY_ADC, INPUT);
    analogSetAttenuation(ADC_11db); // Full 0 - 3.3V range
    // Take initial reading
    uint32_t sum = 0;
    for (int i = 0; i < 8; i++) {
        sum += analogRead(PIN_BATTERY_ADC);
        delay(2);
    }
    float raw_v = (sum / 8.0f) * (BATT_ADC_VREF / BATT_ADC_MAX);
    smoothed_mv = (uint16_t)(raw_v * BATT_DIVIDER_RATIO * 1000.0f);
}

void BatteryMonitor::update() {
    unsigned long now = millis();
    if (now - last_sample_time < 50) return; // 20 Hz sampling
    last_sample_time = now;

    uint32_t raw_adc = analogRead(PIN_BATTERY_ADC);
    float pin_voltage = (raw_adc / BATT_ADC_MAX) * BATT_ADC_VREF;
    uint16_t current_mv = (uint16_t)(pin_voltage * BATT_DIVIDER_RATIO * 1000.0f);

    // Exponential moving average filter (alpha = 0.1)
    smoothed_mv = (uint16_t)(0.9f * smoothed_mv + 0.1f * current_mv);
}
