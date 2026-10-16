#pragma once
#include <Arduino.h>
#include "config.h"

class SafetyWatchdog {
public:
    SafetyWatchdog();
    void feed();
    bool is_expired() const;
    void reset();

private:
    unsigned long last_feed_time;
};
