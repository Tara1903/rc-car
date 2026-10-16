#include "motor_controller.h"

MotorController::MotorController()
    : target_left_speed(0), target_left_dir(DIR_STOP),
      target_right_speed(0), target_right_dir(DIR_STOP),
      current_left_speed(0), current_left_dir(DIR_STOP),
      current_right_speed(0), current_right_dir(DIR_STOP),
      last_ramp_time(0),
      left_dwell_start(0),
      right_dwell_start(0) {}

void MotorController::begin() {
    // Configure Direction GPIOs as Outputs
    pinMode(PIN_LEFT_IN1, OUTPUT);
    pinMode(PIN_LEFT_IN2, OUTPUT);
    pinMode(PIN_LEFT_IN3, OUTPUT);
    pinMode(PIN_LEFT_IN4, OUTPUT);

    pinMode(PIN_RIGHT_IN1, OUTPUT);
    pinMode(PIN_RIGHT_IN2, OUTPUT);
    pinMode(PIN_RIGHT_IN3, OUTPUT);
    pinMode(PIN_RIGHT_IN4, OUTPUT);

    // Initial low state for safety
    digitalWrite(PIN_LEFT_IN1, LOW);
    digitalWrite(PIN_LEFT_IN2, LOW);
    digitalWrite(PIN_LEFT_IN3, LOW);
    digitalWrite(PIN_LEFT_IN4, LOW);

    digitalWrite(PIN_RIGHT_IN1, LOW);
    digitalWrite(PIN_RIGHT_IN2, LOW);
    digitalWrite(PIN_RIGHT_IN3, LOW);
    digitalWrite(PIN_RIGHT_IN4, LOW);

    // Setup LEDC PWM Channels & Pins (20kHz, 8-bit)
    PWM_LEDC_ATTACH(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, PWM_FREQ_HZ, PWM_RESOLUTION);

    emergency_stop();
}

void MotorController::emergency_stop() {
    target_left_speed = 0;
    target_left_dir = DIR_STOP;
    target_right_speed = 0;
    target_right_dir = DIR_STOP;

    current_left_speed = 0;
    current_left_dir = DIR_STOP;
    current_right_speed = 0;
    current_right_dir = DIR_STOP;

    left_dwell_start = 0;
    right_dwell_start = 0;

    // Immediately cut all PWM duty cycles to 0
    PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, 0);
    PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, 0);
    PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, 0);
    PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, 0);

    // Active electric brake (LOW/LOW on H-bridge inputs)
    digitalWrite(PIN_LEFT_IN1, LOW);
    digitalWrite(PIN_LEFT_IN2, LOW);
    digitalWrite(PIN_LEFT_IN3, LOW);
    digitalWrite(PIN_LEFT_IN4, LOW);

    digitalWrite(PIN_RIGHT_IN1, LOW);
    digitalWrite(PIN_RIGHT_IN2, LOW);
    digitalWrite(PIN_RIGHT_IN3, LOW);
    digitalWrite(PIN_RIGHT_IN4, LOW);
}

void MotorController::apply_command(const MotorCommand &cmd) {
    // Validate directions
    uint8_t l_dir = (cmd.left_dir <= DIR_REVERSE) ? cmd.left_dir : DIR_STOP;
    uint8_t r_dir = (cmd.right_dir <= DIR_REVERSE) ? cmd.right_dir : DIR_STOP;

    uint8_t l_spd = cmd.left_speed;
    uint8_t r_spd = cmd.right_speed;

    // Clamp reverse speeds to safety limit
    if (l_dir == DIR_REVERSE && l_spd > MAX_DUTY_CYCLE_REV) {
        l_spd = MAX_DUTY_CYCLE_REV;
    }
    if (r_dir == DIR_REVERSE && r_spd > MAX_DUTY_CYCLE_REV) {
        r_spd = MAX_DUTY_CYCLE_REV;
    }

    target_left_speed = l_spd;
    target_left_dir = l_dir;
    target_right_speed = r_spd;
    target_right_dir = r_dir;

    // If stop commanded, apply immediately
    if (l_dir == DIR_STOP || l_spd == 0) {
        current_left_speed = 0;
        current_left_dir = DIR_STOP;
        left_dwell_start = 0;
        set_hardware_left(0, DIR_STOP);
    }
    if (r_dir == DIR_STOP || r_spd == 0) {
        current_right_speed = 0;
        current_right_dir = DIR_STOP;
        right_dwell_start = 0;
        set_hardware_right(0, DIR_STOP);
    }
}

void MotorController::update_ramp() {
    unsigned long now = millis();
    if (now - last_ramp_time < 10) return; // 100 Hz ramp update
    last_ramp_time = now;

    // Ramp Left Speed with Sensorless Reverse Dwell Window
    if (current_left_dir != target_left_dir) {
        if (current_left_speed > 15) {
            current_left_speed -= 15;
            left_dwell_start = 0;
        } else {
            current_left_speed = 0;
            if (left_dwell_start == 0) {
                left_dwell_start = now;
            }
            if (now - left_dwell_start >= REVERSE_DWELL_MS) {
                current_left_dir = target_left_dir;
                left_dwell_start = 0;
            }
        }
    } else {
        left_dwell_start = 0;
        if (current_left_speed < target_left_speed) {
            uint16_t next = current_left_speed + 12; // Accel step
            current_left_speed = (next > target_left_speed) ? target_left_speed : next;
        } else if (current_left_speed > target_left_speed) {
            uint16_t next = (current_left_speed > 20) ? (current_left_speed - 20) : 0; // Decel step
            current_left_speed = (next < target_left_speed) ? target_left_speed : next;
        }
    }
    set_hardware_left(current_left_speed, current_left_dir);

    // Ramp Right Speed with Sensorless Reverse Dwell Window
    if (current_right_dir != target_right_dir) {
        if (current_right_speed > 15) {
            current_right_speed -= 15;
            right_dwell_start = 0;
        } else {
            current_right_speed = 0;
            if (right_dwell_start == 0) {
                right_dwell_start = now;
            }
            if (now - right_dwell_start >= REVERSE_DWELL_MS) {
                current_right_dir = target_right_dir;
                right_dwell_start = 0;
            }
        }
    } else {
        right_dwell_start = 0;
        if (current_right_speed < target_right_speed) {
            uint16_t next = current_right_speed + 12;
            current_right_speed = (next > target_right_speed) ? target_right_speed : next;
        } else if (current_right_speed > target_right_speed) {
            uint16_t next = (current_right_speed > 20) ? (current_right_speed - 20) : 0;
            current_right_speed = (next < target_right_speed) ? target_right_speed : next;
        }
    }
    set_hardware_right(current_right_speed, current_right_dir);
}

void MotorController::set_hardware_left(uint8_t speed, uint8_t dir) {
    if (speed < MIN_START_DUTY || dir == DIR_STOP) {
        PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, 0);
        PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, 0);
        digitalWrite(PIN_LEFT_IN1, LOW);
        digitalWrite(PIN_LEFT_IN2, LOW);
        digitalWrite(PIN_LEFT_IN3, LOW);
        digitalWrite(PIN_LEFT_IN4, LOW);
        return;
    }

    if (dir == DIR_FORWARD) {
        // FL Forward (IN1=HIGH, IN2=LOW) & RL Forward (IN3=HIGH, IN4=LOW)
        digitalWrite(PIN_LEFT_IN1, HIGH);
        digitalWrite(PIN_LEFT_IN2, LOW);
        digitalWrite(PIN_LEFT_IN3, HIGH);
        digitalWrite(PIN_LEFT_IN4, LOW);
    } else {
        // Reverse (IN1=LOW, IN2=HIGH) & (IN3=LOW, IN4=HIGH)
        digitalWrite(PIN_LEFT_IN1, LOW);
        digitalWrite(PIN_LEFT_IN2, HIGH);
        digitalWrite(PIN_LEFT_IN3, LOW);
        digitalWrite(PIN_LEFT_IN4, HIGH);
    }

    PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, speed);
    PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, speed);
}

void MotorController::set_hardware_right(uint8_t speed, uint8_t dir) {
    if (speed < MIN_START_DUTY || dir == DIR_STOP) {
        PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, 0);
        PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, 0);
        digitalWrite(PIN_RIGHT_IN1, LOW);
        digitalWrite(PIN_RIGHT_IN2, LOW);
        digitalWrite(PIN_RIGHT_IN3, LOW);
        digitalWrite(PIN_RIGHT_IN4, LOW);
        return;
    }

    if (dir == DIR_FORWARD) {
        // FR Forward (IN1=HIGH, IN2=LOW) & RR Forward (IN3=HIGH, IN4=LOW)
        digitalWrite(PIN_RIGHT_IN1, HIGH);
        digitalWrite(PIN_RIGHT_IN2, LOW);
        digitalWrite(PIN_RIGHT_IN3, HIGH);
        digitalWrite(PIN_RIGHT_IN4, LOW);
    } else {
        // Reverse (IN1=LOW, IN2=HIGH) & (IN3=LOW, IN4=HIGH)
        digitalWrite(PIN_RIGHT_IN1, LOW);
        digitalWrite(PIN_RIGHT_IN2, HIGH);
        digitalWrite(PIN_RIGHT_IN3, LOW);
        digitalWrite(PIN_RIGHT_IN4, HIGH);
    }

    PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, speed);
    PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, speed);
}
