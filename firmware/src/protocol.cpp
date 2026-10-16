#include "protocol.h"
#include "crc8.h"
#include <string.h>

ProtocolParser::ProtocolParser() {
    reset();
}

void ProtocolParser::reset() {
    index = 0;
    in_frame = false;
    memset(buffer, 0, sizeof(buffer));
}

bool ProtocolParser::parse_byte(uint8_t byte, MotorCommand &out_cmd) {
    if (!in_frame) {
        if (byte == PROTOCOL_START_BYTE) {
            in_frame = true;
            buffer[0] = byte;
            index = 1;
        }
        return false;
    }

    buffer[index++] = byte;

    // Check header consistency early
    if (index == 2 && buffer[1] != PROTOCOL_VERSION) {
        reset();
        return false;
    }
    if (index == 3 && buffer[2] != MSG_TYPE_COMMAND) {
        reset();
        return false;
    }
    if (index == 4 && buffer[3] != 0x05) { // Payload length must be 5
        reset();
        return false;
    }

    // Full 11-byte frame accumulated
    if (index >= CMD_FRAME_SIZE) {
        in_frame = false;
        index = 0;

        // Verify end byte
        if (buffer[10] != PROTOCOL_END_BYTE) {
            return false;
        }

        // Verify CRC-8 over bytes 0..8
        uint8_t expected_crc = crc8_calculate(buffer, 9);
        if (expected_crc != buffer[9]) {
            return false;
        }

        // Extract payload
        out_cmd.left_speed  = buffer[4];
        out_cmd.left_dir    = buffer[5];
        out_cmd.right_speed = buffer[6];
        out_cmd.right_dir   = buffer[7];
        out_cmd.sequence    = buffer[8];
        return true;
    }

    return false;
}

size_t protocol_build_telemetry(const TelemetryData &telem, uint8_t *out_buf, size_t max_len) {
    if (max_len < TELEM_FRAME_SIZE || out_buf == nullptr) {
        return 0;
    }

    out_buf[0]  = PROTOCOL_START_BYTE;
    out_buf[1]  = PROTOCOL_VERSION;
    out_buf[2]  = MSG_TYPE_TELEMETRY;
    out_buf[3]  = 0x07; // Payload length = 7 bytes
    out_buf[4]  = telem.state;
    out_buf[5]  = telem.left_actual_speed;
    out_buf[6]  = telem.right_actual_speed;
    out_buf[7]  = (uint8_t)((telem.battery_mv >> 8) & 0xFF);
    out_buf[8]  = (uint8_t)(telem.battery_mv & 0xFF);
    out_buf[9]  = telem.fault_flags;
    out_buf[10] = telem.fw_counter;
    out_buf[11] = crc8_calculate(out_buf, 11); // CRC-8 over bytes 0..10
    out_buf[12] = PROTOCOL_END_BYTE;

    return TELEM_FRAME_SIZE;
}
