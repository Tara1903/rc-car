// ============================================================================
// ROBO RACER ESP32 ALL-IN-ONE ARDUINO SKETCH v1.1
// Board: "ESP32 Dev Module" in Arduino IDE
// Target Hardware: Prakti ESP32 (30-pin, CP2102) + Dual L298N + 4WD TT Motors
// ============================================================================

#include <Arduino.h>
#include <BluetoothSerial.h>

// DEVICE NAME
#define DEVICE_NAME "ROBO-RACER-ESP32"

// SAFE MOTOR GPIOs (Driver 1: Left Motors)
#define PIN_LEFT_ENA    18
#define PIN_LEFT_IN1    19
#define PIN_LEFT_IN2    17
#define PIN_LEFT_IN3    16
#define PIN_LEFT_IN4     4
#define PIN_LEFT_ENB    25

// SAFE MOTOR GPIOs (Driver 2: Right Motors)
#define PIN_RIGHT_ENA   23
#define PIN_RIGHT_IN1   22
#define PIN_RIGHT_IN2   21
#define PIN_RIGHT_IN3   27
#define PIN_RIGHT_IN4   14
#define PIN_RIGHT_ENB   26

// SENSORS & LED
#define PIN_BATTERY_ADC 34
#define PIN_STATUS_LED   2

// PWM SETUP
#define PWM_FREQ_HZ     20000
#define PWM_RESOLUTION      8
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

#define WATCHDOG_TIMEOUT_MS  500
#define TELEMETRY_INTERVAL_MS 50
#define MAX_DUTY_CYCLE_REV   160
#define MIN_START_DUTY        45

#define REVERSE_DWELL_MS     500

#define PROTOCOL_START_BYTE 0xAA
#define PROTOCOL_END_BYTE   0x55
#define PROTOCOL_VERSION    0x01
#define MSG_TYPE_COMMAND    0x01
#define MSG_TYPE_TELEMETRY  0x10

#define CMD_FRAME_SIZE      11
#define TELEM_FRAME_SIZE    13

enum MotorDir : uint8_t { DIR_STOP = 0, DIR_FWD = 1, DIR_REV = 2 };
enum SysState : uint8_t { STATE_IDLE = 0, STATE_RUNNING = 1, STATE_FAILSAFE = 2, STATE_LOW_BATT = 3 };

BluetoothSerial SerialBT;
bool bt_connected = false;
bool first_cmd = true;
uint8_t last_cmd_seq = 0;
unsigned long last_cmd_time = 0;
unsigned long last_telem_time = 0;
unsigned long last_ramp_time = 0;
unsigned long left_dwell_start = 0;
unsigned long right_dwell_start = 0;
uint8_t telem_seq = 0;

uint8_t cur_left_speed = 0, cur_left_dir = DIR_STOP;
uint8_t cur_right_speed = 0, cur_right_dir = DIR_STOP;
uint8_t tgt_left_speed = 0, tgt_left_dir = DIR_STOP;
uint8_t tgt_right_speed = 0, tgt_right_dir = DIR_STOP;

// CRC-8 Lookup Table (Poly 0x07)
static const uint8_t crc8_table[256] = {
    0x00, 0x07, 0x0E, 0x09, 0x1C, 0x1B, 0x12, 0x15, 0x38, 0x3F, 0x36, 0x31, 0x24, 0x23, 0x2A, 0x2D,
    0x70, 0x77, 0x7E, 0x79, 0x6C, 0x6B, 0x62, 0x65, 0x48, 0x4F, 0x46, 0x41, 0x54, 0x53, 0x5A, 0x5D,
    0xE0, 0xE7, 0xEE, 0xE9, 0xFC, 0xFB, 0xF2, 0xF5, 0xD8, 0xDF, 0xD6, 0xD1, 0xC4, 0xC3, 0xCA, 0xCD,
    0x90, 0x97, 0x9E, 0x99, 0x8C, 0x8B, 0x82, 0x85, 0xA8, 0xAF, 0xA6, 0xA1, 0xB4, 0xB3, 0xBA, 0xBD,
    0xC7, 0xC0, 0xC9, 0xCE, 0xDB, 0xDC, 0xD5, 0xD2, 0xFF, 0xF8, 0xF1, 0xF6, 0xE3, 0xE4, 0xED, 0xEA,
    0xB7, 0xB0, 0xB9, 0xBE, 0xAB, 0xAC, 0xA5, 0xA2, 0x8F, 0x88, 0x81, 0x86, 0x93, 0x94, 0x9D, 0x9A,
    0x27, 0x20, 0x29, 0x2E, 0x3B, 0x3C, 0x35, 0x32, 0x1F, 0x18, 0x11, 0x16, 0x03, 0x04, 0x0D, 0x0A,
    0x57, 0x50, 0x59, 0x5E, 0x4B, 0x4C, 0x45, 0x42, 0x6F, 0x68, 0x61, 0x66, 0x73, 0x74, 0x7D, 0x7A,
    0x89, 0x8E, 0x87, 0x80, 0x95, 0x92, 0x9B, 0x9C, 0xB1, 0xB6, 0xBF, 0xB8, 0xAD, 0xAA, 0xA3, 0xA4,
    0xF9, 0xFE, 0xF7, 0xF0, 0xE5, 0xE2, 0xEB, 0xEC, 0xC1, 0xC6, 0xCF, 0xC8, 0xDD, 0xDA, 0xD3, 0xD4,
    0x69, 0x6E, 0x67, 0x60, 0x75, 0x72, 0x7B, 0x7C, 0x51, 0x56, 0x5F, 0x58, 0x4D, 0x4A, 0x43, 0x44,
    0x19, 0x1E, 0x17, 0x10, 0x05, 0x02, 0x0B, 0x0C, 0x21, 0x26, 0x2F, 0x28, 0x3D, 0x3A, 0x33, 0x34,
    0x4E, 0x49, 0x40, 0x47, 0x52, 0x55, 0x5C, 0x5B, 0x76, 0x71, 0x78, 0x7F, 0x6A, 0x6D, 0x64, 0x63,
    0x3E, 0x39, 0x30, 0x37, 0x22, 0x25, 0x2C, 0x2B, 0x06, 0x01, 0x08, 0x0F, 0x1A, 0x1D, 0x14, 0x13,
    0xAE, 0xA9, 0xA0, 0xA7, 0xB2, 0xB5, 0xBC, 0xBB, 0x96, 0x91, 0x98, 0x9F, 0x8A, 0x8D, 0x84, 0x83,
    0xDE, 0xD9, 0xD0, 0xD7, 0xC2, 0xC5, 0xCC, 0xCB, 0xE6, 0xE1, 0xE8, 0xEF, 0xFA, 0xFD, 0xF4, 0xF3
};

uint8_t calc_crc8(const uint8_t *data, size_t len) {
    uint8_t crc = 0x00;
    for (size_t i = 0; i < len; i++) crc = crc8_table[crc ^ data[i]];
    return crc;
}

void set_hw_left(uint8_t spd, uint8_t dir) {
    if (spd < MIN_START_DUTY || dir == DIR_STOP) {
        PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, 0);
        PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, 0);
        digitalWrite(PIN_LEFT_IN1, LOW);
        digitalWrite(PIN_LEFT_IN2, LOW);
        digitalWrite(PIN_LEFT_IN3, LOW);
        digitalWrite(PIN_LEFT_IN4, LOW);
        return;
    }
    digitalWrite(PIN_LEFT_IN1, (dir == DIR_FWD) ? HIGH : LOW);
    digitalWrite(PIN_LEFT_IN2, (dir == DIR_FWD) ? LOW : HIGH);
    digitalWrite(PIN_LEFT_IN3, (dir == DIR_FWD) ? HIGH : LOW);
    digitalWrite(PIN_LEFT_IN4, (dir == DIR_FWD) ? LOW : HIGH);
    PWM_LEDC_WRITE(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, spd);
    PWM_LEDC_WRITE(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, spd);
}

void set_hw_right(uint8_t spd, uint8_t dir) {
    if (spd < MIN_START_DUTY || dir == DIR_STOP) {
        PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, 0);
        PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, 0);
        digitalWrite(PIN_RIGHT_IN1, LOW);
        digitalWrite(PIN_RIGHT_IN2, LOW);
        digitalWrite(PIN_RIGHT_IN3, LOW);
        digitalWrite(PIN_RIGHT_IN4, LOW);
        return;
    }
    digitalWrite(PIN_RIGHT_IN1, (dir == DIR_FWD) ? HIGH : LOW);
    digitalWrite(PIN_RIGHT_IN2, (dir == DIR_FWD) ? LOW : HIGH);
    digitalWrite(PIN_RIGHT_IN3, (dir == DIR_FWD) ? HIGH : LOW);
    digitalWrite(PIN_RIGHT_IN4, (dir == DIR_FWD) ? LOW : HIGH);
    PWM_LEDC_WRITE(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, spd);
    PWM_LEDC_WRITE(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, spd);
}

void stop_all_motors() {
    tgt_left_speed = cur_left_speed = 0;
    tgt_right_speed = cur_right_speed = 0;
    tgt_left_dir = cur_left_dir = DIR_STOP;
    tgt_right_dir = cur_right_dir = DIR_STOP;
    left_dwell_start = 0;
    right_dwell_start = 0;
    set_hw_left(0, DIR_STOP);
    set_hw_right(0, DIR_STOP);
}

void bt_event_cb(esp_spp_cb_event_t event, esp_spp_cb_param_t *param) {
    if (event == ESP_SPP_SRV_OPEN_EVT) {
        bt_connected = true;
        first_cmd = true;
        last_cmd_time = millis();
    } else if (event == ESP_SPP_CLOSE_EVT) {
        bt_connected = false;
        first_cmd = true;
        last_cmd_time = 0;
        stop_all_motors();
    }
}

void setup() {
    Serial.begin(115200);
    pinMode(PIN_STATUS_LED, OUTPUT);
    digitalWrite(PIN_STATUS_LED, LOW);

    pinMode(PIN_BATTERY_ADC, INPUT);
    analogSetAttenuation(ADC_11db);

    pinMode(PIN_LEFT_IN1, OUTPUT); pinMode(PIN_LEFT_IN2, OUTPUT);
    pinMode(PIN_LEFT_IN3, OUTPUT); pinMode(PIN_LEFT_IN4, OUTPUT);
    pinMode(PIN_RIGHT_IN1, OUTPUT); pinMode(PIN_RIGHT_IN2, OUTPUT);
    pinMode(PIN_RIGHT_IN3, OUTPUT); pinMode(PIN_RIGHT_IN4, OUTPUT);

    PWM_LEDC_ATTACH(PIN_LEFT_ENA, PWM_CHAN_LEFT_A, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_LEFT_ENB, PWM_CHAN_LEFT_B, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_RIGHT_ENA, PWM_CHAN_RIGHT_A, PWM_FREQ_HZ, PWM_RESOLUTION);
    PWM_LEDC_ATTACH(PIN_RIGHT_ENB, PWM_CHAN_RIGHT_B, PWM_FREQ_HZ, PWM_RESOLUTION);

    stop_all_motors();

    SerialBT.register_callback(bt_event_cb);
    SerialBT.begin(DEVICE_NAME);
    Serial.println("Robo Racer Ready.");
}

uint8_t rx_buf[16];
uint8_t rx_idx = 0;

void loop() {
    unsigned long now = millis();

    // 1. Process Bluetooth incoming stream
    while (SerialBT.available()) {
        uint8_t b = SerialBT.read();
        if (rx_idx == 0) {
            if (b == PROTOCOL_START_BYTE) rx_buf[rx_idx++] = b;
        } else {
            rx_buf[rx_idx++] = b;
            if (rx_idx == CMD_FRAME_SIZE) {
                rx_idx = 0;
                if (rx_buf[1] == PROTOCOL_VERSION && rx_buf[2] == MSG_TYPE_COMMAND && rx_buf[10] == PROTOCOL_END_BYTE) {
                    if (calc_crc8(rx_buf, 9) == rx_buf[9]) {
                        uint8_t seq = rx_buf[8];
                        bool is_fresh = false;
                        if (first_cmd) {
                            first_cmd = false;
                            last_cmd_seq = seq;
                            is_fresh = true;
                        } else {
                            uint8_t diff = seq - last_cmd_seq;
                            if (diff > 0 && diff < 128) {
                                last_cmd_seq = seq;
                                is_fresh = true;
                            }
                        }

                        if (is_fresh) {
                            last_cmd_time = now;
                            tgt_left_speed  = rx_buf[4];
                            tgt_left_dir    = rx_buf[5];
                            tgt_right_speed = rx_buf[6];
                            tgt_right_dir   = rx_buf[7];
                            if (tgt_left_dir == DIR_REV && tgt_left_speed > MAX_DUTY_CYCLE_REV) tgt_left_speed = MAX_DUTY_CYCLE_REV;
                            if (tgt_right_dir == DIR_REV && tgt_right_speed > MAX_DUTY_CYCLE_REV) tgt_right_speed = MAX_DUTY_CYCLE_REV;
                            if (tgt_left_dir == DIR_STOP || tgt_left_speed == 0) {
                                cur_left_speed = 0;
                                cur_left_dir = DIR_STOP;
                                left_dwell_start = 0;
                                set_hw_left(0, DIR_STOP);
                            }
                            if (tgt_right_dir == DIR_STOP || tgt_right_speed == 0) {
                                cur_right_speed = 0;
                                cur_right_dir = DIR_STOP;
                                right_dwell_start = 0;
                                set_hw_right(0, DIR_STOP);
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Safety Watchdog & Ramp with 500ms Reverse Dwell Window
    bool timeout = (last_cmd_time == 0 || (now - last_cmd_time > WATCHDOG_TIMEOUT_MS));
    if (timeout) {
        stop_all_motors();
    } else {
        if (now - last_ramp_time >= 10) {
            last_ramp_time = now;

            // Left motor dwell and ramp
            if (cur_left_dir != tgt_left_dir) {
                if (cur_left_speed > 15) {
                    cur_left_speed -= 15;
                    left_dwell_start = 0;
                } else {
                    cur_left_speed = 0;
                    if (left_dwell_start == 0) left_dwell_start = now;
                    if (now - left_dwell_start >= REVERSE_DWELL_MS) {
                        cur_left_dir = tgt_left_dir;
                        left_dwell_start = 0;
                    }
                }
            } else {
                left_dwell_start = 0;
                if (cur_left_speed < tgt_left_speed) {
                    uint16_t next = cur_left_speed + 12;
                    cur_left_speed = (next > tgt_left_speed) ? tgt_left_speed : next;
                } else if (cur_left_speed > tgt_left_speed) {
                    uint16_t next = (cur_left_speed > 20) ? (cur_left_speed - 20) : 0;
                    cur_left_speed = (next < tgt_left_speed) ? tgt_left_speed : next;
                }
            }
            set_hw_left(cur_left_speed, cur_left_dir);

            // Right motor dwell and ramp
            if (cur_right_dir != tgt_right_dir) {
                if (cur_right_speed > 15) {
                    cur_right_speed -= 15;
                    right_dwell_start = 0;
                } else {
                    cur_right_speed = 0;
                    if (right_dwell_start == 0) right_dwell_start = now;
                    if (now - right_dwell_start >= REVERSE_DWELL_MS) {
                        cur_right_dir = tgt_right_dir;
                        right_dwell_start = 0;
                    }
                }
            } else {
                right_dwell_start = 0;
                if (cur_right_speed < tgt_right_speed) {
                    uint16_t next = cur_right_speed + 12;
                    cur_right_speed = (next > tgt_right_speed) ? tgt_right_speed : next;
                } else if (cur_right_speed > tgt_right_speed) {
                    uint16_t next = (cur_right_speed > 20) ? (cur_right_speed - 20) : 0;
                    cur_right_speed = (next < tgt_right_speed) ? tgt_right_speed : next;
                }
            }
            set_hw_right(cur_right_speed, cur_right_dir);
        }
    }

    // 3. Telemetry Output (20 Hz)
    if (now - last_telem_time >= TELEMETRY_INTERVAL_MS) {
        last_telem_time = now;
        telem_seq++;

        float adc_v = (analogRead(PIN_BATTERY_ADC) / 4095.0f) * 3.3f;
        uint16_t batt_mv = (uint16_t)(adc_v * 6.0f * 1000.0f);

        uint8_t tx[13];
        tx[0] = PROTOCOL_START_BYTE;
        tx[1] = PROTOCOL_VERSION;
        tx[2] = MSG_TYPE_TELEMETRY;
        tx[3] = 0x07;
        tx[4] = timeout ? STATE_FAILSAFE : ((cur_left_speed > 0 || cur_right_speed > 0) ? STATE_RUNNING : STATE_IDLE);
        tx[5] = cur_left_speed;
        tx[6] = cur_right_speed;
        tx[7] = (batt_mv >> 8) & 0xFF;
        tx[8] = batt_mv & 0xFF;
        tx[9] = timeout ? 0x01 : 0x00;
        tx[10] = telem_seq;
        tx[11] = calc_crc8(tx, 11);
        tx[12] = PROTOCOL_END_BYTE;

        if (bt_connected) SerialBT.write(tx, 13);
    }

    // 4. Status LED
    if (!bt_connected) digitalWrite(PIN_STATUS_LED, (now / 500) % 2);
    else if (timeout) digitalWrite(PIN_STATUS_LED, (now / 100) % 2);
    else digitalWrite(PIN_STATUS_LED, HIGH);
}
