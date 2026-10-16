#pragma once
#include <Arduino.h>
#include "config.h"

class BatteryMonitor {
public:
    BatteryMonitor();
    void begin();
    void update();
    uint16_t get_voltage_mv() const { return smoothed_mv; }
    bool is_low_battery() const { return smoothed_mv > 5000 && smoothed_mv < BATT_LOW_VOLTAGE_MV; }

private:
    uint16_t smoothed_mv;
    unsigned long last_sample_time;
};
