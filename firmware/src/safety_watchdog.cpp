#include "safety_watchdog.h"

SafetyWatchdog::SafetyWatchdog() : last_feed_time(0) {}

void SafetyWatchdog::feed() {
    last_feed_time = millis();
}

bool SafetyWatchdog::is_expired() const {
    if (last_feed_time == 0) return true;
    return (millis() - last_feed_time) > WATCHDOG_TIMEOUT_MS;
}

void SafetyWatchdog::reset() {
    last_feed_time = 0;
}
