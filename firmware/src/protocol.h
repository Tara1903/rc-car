#pragma once
#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

#define PROTOCOL_START_BYTE 0xAA
#define PROTOCOL_END_BYTE   0x55
#define PROTOCOL_VERSION    0x01

#define MSG_TYPE_COMMAND    0x01
#define MSG_TYPE_TELEMETRY  0x10

#define CMD_FRAME_SIZE      11
#define TELEM_FRAME_SIZE    13

// Direction Enums
enum MotorDirection : uint8_t {
    DIR_STOP    = 0x00,
    DIR_FORWARD = 0x01,
    DIR_REVERSE = 0x02
};

// System Status Enums
enum SystemState : uint8_t {
    STATE_IDLE        = 0x00,
    STATE_RUNNING     = 0x01,
    STATE_FAILSAFE    = 0x02,
    STATE_LOW_BATTERY = 0x03
};

// Fault Bitmask Flags
#define FAULT_WATCHDOG_TIMEOUT (1 << 0)
#define FAULT_LOW_BATTERY      (1 << 1)
#define FAULT_CRC_ERROR        (1 << 2)
#define FAULT_MOTOR_STALL      (1 << 3)

struct MotorCommand {
    uint8_t left_speed;
    uint8_t left_dir;
    uint8_t right_speed;
    uint8_t right_dir;
    uint8_t sequence;
};

struct TelemetryData {
    uint8_t state;
    uint8_t left_actual_speed;
    uint8_t right_actual_speed;
    uint16_t battery_mv;
    uint8_t fault_flags;
    uint8_t fw_counter;
};

// Parser state machine
class ProtocolParser {
public:
    ProtocolParser();
    void reset();
    bool parse_byte(uint8_t byte, MotorCommand &out_cmd);

private:
    uint8_t buffer[CMD_FRAME_SIZE];
    uint8_t index;
    bool in_frame;
};

// Serializes telemetry into a 13-byte buffer
size_t protocol_build_telemetry(const TelemetryData &telem, uint8_t *out_buf, size_t max_len);
