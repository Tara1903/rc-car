#pragma once
#include <Arduino.h>
#include "config.h"

class BatteryMonitor {
public:
    BatteryMonitor();
    void begin();
    void update();
    uint16_t get_voltage_mv() const { return smoothed_mv; }
    bool is_measurement_valid() const { return is_valid; }
    bool is_low_battery() const { return is_valid && smoothed_mv < BATT_LOW_VOLTAGE_MV; }

private:
    void evaluate_reading(float raw_adc);

    uint16_t smoothed_mv;
    unsigned long last_sample_time;
    bool is_valid;
};
