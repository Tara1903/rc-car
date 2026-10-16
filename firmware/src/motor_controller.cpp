#include "motor_controller.h"

MotorController::MotorController()
    : target_left_speed(0), target_left_dir(DIR_STOP),
      target_right_speed(0), target_right_dir(DIR_STOP),
      current_left_speed(0), current_left_dir(DIR_STOP),
      current_right_speed(0), current_right_dir(DIR_STOP),
      last_powered_dir_left(DIR_STOP),
      last_powered_dir_right(DIR_STOP),
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
    unsigned long now = millis();
    target_left_speed = 0;
    target_left_dir = DIR_STOP;
    target_right_speed = 0;
    target_right_dir = DIR_STOP;

    current_left_speed = 0;
    current_left_dir = DIR_STOP;
    current_right_speed = 0;
    current_right_dir = DIR_STOP;

    // Start/preserve dwell clock if motors were running
    if (last_powered_dir_left != DIR_STOP && left_dwell_start == 0) {
        left_dwell_start = now;
    }
    if (last_powered_dir_right != DIR_STOP && right_dwell_start == 0) {
        right_dwell_start = now;
    }

    // Immediately cut all PWM duty cycles to 0 (ENA/ENB disabled)
    // On L298N, this enters high-impedance unpowered coasting (Free Running Motor Stop)
    PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, 0);
    PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, 0);
    PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, 0);
    PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, 0);

    // Direction pins LOW (H-bridge inputs de-energized)
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
    unsigned long now = millis();

    // Validate directions
    uint8_t l_dir = (cmd.left_dir <= DIR_REVERSE) ? cmd.left_dir : DIR_STOP;
    uint8_t r_dir = (cmd.right_dir <= DIR_REVERSE) ? cmd.right_dir : DIR_STOP;

    uint8_t l_spd = cmd.left_speed;
    uint8_t r_spd = cmd.right_speed;

    // Clamp reverse speeds to mechanical stability limit
    if (l_dir == DIR_REVERSE && l_spd > MAX_DUTY_CYCLE_REV) l_spd = MAX_DUTY_CYCLE_REV;
    if (r_dir == DIR_REVERSE && r_spd > MAX_DUTY_CYCLE_REV) r_spd = MAX_DUTY_CYCLE_REV;

    target_left_speed = l_spd;
    target_left_dir = l_dir;
    target_right_speed = r_spd;
    target_right_dir = r_dir;

    // Left Motor: Check immediate stop or opposite direction reversal
    if (l_dir == DIR_STOP || l_spd == 0) {
        current_left_speed = 0;
        current_left_dir = DIR_STOP;
        if (last_powered_dir_left != DIR_STOP && left_dwell_start == 0) {
            left_dwell_start = now;
        }
        set_hardware_left(0, DIR_STOP);
    } else if (last_powered_dir_left != DIR_STOP && l_dir != last_powered_dir_left) {
        // Commanded opposite direction! Cut power immediately and ensure dwell clock is running
        current_left_speed = 0;
        current_left_dir = DIR_STOP;
        if (left_dwell_start == 0) {
            left_dwell_start = now;
        }
        set_hardware_left(0, DIR_STOP);
    }

    // Right Motor: Check immediate stop or opposite direction reversal
    if (r_dir == DIR_STOP || r_spd == 0) {
        current_right_speed = 0;
        current_right_dir = DIR_STOP;
        if (last_powered_dir_right != DIR_STOP && right_dwell_start == 0) {
            right_dwell_start = now;
        }
        set_hardware_right(0, DIR_STOP);
    } else if (last_powered_dir_right != DIR_STOP && r_dir != last_powered_dir_right) {
        // Commanded opposite direction! Cut power immediately and ensure dwell clock is running
        current_right_speed = 0;
        current_right_dir = DIR_STOP;
        if (right_dwell_start == 0) {
            right_dwell_start = now;
        }
        set_hardware_right(0, DIR_STOP);
    }
}

void MotorController::update_ramp() {
    unsigned long now = millis();
    if (now - last_ramp_time < 10) return; // 100 Hz ramp update
    last_ramp_time = now;

    // ------------------------------------------------------------------------
    // LEFT MOTOR DWELL & RAMP
    // ------------------------------------------------------------------------
    if (target_left_dir == DIR_STOP || target_left_speed == 0) {
        current_left_speed = 0;
        current_left_dir = DIR_STOP;
        set_hardware_left(0, DIR_STOP);
    } else if (last_powered_dir_left != DIR_STOP && target_left_dir != last_powered_dir_left) {
        // Transitioning to opposite direction: must enforce zero-output dwell
        current_left_speed = 0;
        current_left_dir = DIR_STOP;
        set_hardware_left(0, DIR_STOP);

        if (left_dwell_start == 0) {
            left_dwell_start = now;
        }

        if (now - left_dwell_start >= REVERSE_DWELL_MS) {
            // Dwell window complete! Safe to energize in new direction
            current_left_dir = target_left_dir;
            last_powered_dir_left = target_left_dir;
            left_dwell_start = 0;
        }
    } else {
        // Direction is safe (same direction or started from prolonged neutral)
        current_left_dir = target_left_dir;
        left_dwell_start = 0;

        if (current_left_speed < target_left_speed) {
            uint16_t next = current_left_speed + 12; // Accel step
            current_left_speed = (next > target_left_speed) ? target_left_speed : next;
        } else if (current_left_speed > target_left_speed) {
            uint16_t next = (current_left_speed > 20) ? (current_left_speed - 20) : 0; // Decel step
            current_left_speed = (next < target_left_speed) ? target_left_speed : next;
        }

        if (current_left_speed > 0) {
            last_powered_dir_left = current_left_dir;
        }
        set_hardware_left(current_left_speed, current_left_dir);
    }

    // ------------------------------------------------------------------------
    // RIGHT MOTOR DWELL & RAMP
    // ------------------------------------------------------------------------
    if (target_right_dir == DIR_STOP || target_right_speed == 0) {
        current_right_speed = 0;
        current_right_dir = DIR_STOP;
        set_hardware_right(0, DIR_STOP);
    } else if (last_powered_dir_right != DIR_STOP && target_right_dir != last_powered_dir_right) {
        // Transitioning to opposite direction: must enforce zero-output dwell
        current_right_speed = 0;
        current_right_dir = DIR_STOP;
        set_hardware_right(0, DIR_STOP);

        if (right_dwell_start == 0) {
            right_dwell_start = now;
        }

        if (now - right_dwell_start >= REVERSE_DWELL_MS) {
            // Dwell window complete! Safe to energize in new direction
            current_right_dir = target_right_dir;
            last_powered_dir_right = target_right_dir;
            right_dwell_start = 0;
        }
    } else {
        // Direction is safe (same direction or started from prolonged neutral)
        current_right_dir = target_right_dir;
        right_dwell_start = 0;

        if (current_right_speed < target_right_speed) {
            uint16_t next = current_right_speed + 12;
            current_right_speed = (next > target_right_speed) ? target_right_speed : next;
        } else if (current_right_speed > target_right_speed) {
            uint16_t next = (current_right_speed > 20) ? (current_right_speed - 20) : 0;
            current_right_speed = (next < target_right_speed) ? target_right_speed : next;
        }

        if (current_right_speed > 0) {
            last_powered_dir_right = current_right_dir;
        }
        set_hardware_right(current_right_speed, current_right_dir);
    }
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
