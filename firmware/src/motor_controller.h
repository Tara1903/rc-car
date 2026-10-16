#pragma once
#include <Arduino.h>
#include "config.h"
#include "protocol.h"

class MotorController {
public:
    MotorController();
    void begin();
    void apply_command(const MotorCommand &cmd);
    void emergency_stop();
    void update_ramp(); // Call in loop for smooth acceleration

    uint8_t get_left_actual_speed() const { return current_left_speed; }
    uint8_t get_right_actual_speed() const { return current_right_speed; }

private:
    void set_hardware_left(uint8_t speed, uint8_t dir);
    void set_hardware_right(uint8_t speed, uint8_t dir);

    uint8_t target_left_speed;
    uint8_t target_left_dir;
    uint8_t target_right_speed;
    uint8_t target_right_dir;

    uint8_t current_left_speed;
    uint8_t current_left_dir;
    uint8_t current_right_speed;
    uint8_t current_right_dir;

    unsigned long last_ramp_time;
    unsigned long left_dwell_start;
    unsigned long right_dwell_start;
};
