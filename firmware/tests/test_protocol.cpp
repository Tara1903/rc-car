#include <cassert>
#include <cstdio>
#include <cstring>
#include <cstdint>
#include "../src/protocol.h"
#include "../src/crc8.h"

// Forward sequence freshness validation replicating main.cpp / RoboRacerFirmware.ino
static bool check_freshness(uint8_t seq, uint8_t &last_seq, bool &is_first) {
    if (is_first) {
        is_first = false;
        last_seq = seq;
        return true;
    }
    uint8_t diff = (uint8_t)(seq - last_seq);
    if (diff > 0 && diff < 128) {
        last_seq = seq;
        return true;
    }
    return false; // Stale or duplicate
}

void test_crc8() {
    printf("[TEST 1/7] CRC8 algorithm and test vectors...\n");
    // Vector 1: Empty
    assert(crc8_calculate(nullptr, 0) == 0x00);

    // Vector 2: Single byte 0x01 -> 0x07
    uint8_t single = 0x01;
    assert(crc8_calculate(&single, 1) == 0x07);

    // Vector 3: Standard ATM/SMBus vector "123456789" -> 0xF4
    const char *str = "123456789";
    assert(crc8_calculate((const uint8_t*)str, 9) == 0xF4);

    printf("  PASS: CRC8 verification successful.\n");
}

void test_protocol_parser_valid() {
    printf("[TEST 2/7] ProtocolParser valid command frame...\n");
    ProtocolParser parser;
    MotorCommand cmd = {};

    uint8_t frame[11];
    frame[0] = PROTOCOL_START_BYTE;
    frame[1] = PROTOCOL_VERSION;
    frame[2] = MSG_TYPE_COMMAND;
    frame[3] = 0x05;
    frame[4] = 200;
    frame[5] = DIR_FORWARD;
    frame[6] = 180;
    frame[7] = DIR_FORWARD;
    frame[8] = 42;
    frame[9] = crc8_calculate(frame, 9);
    frame[10] = PROTOCOL_END_BYTE;

    bool parsed = false;
    for (int i = 0; i < 11; i++) {
        parsed = parser.parse_byte(frame[i], cmd);
    }

    assert(parsed == true);
    assert(cmd.left_speed == 200);
    assert(cmd.left_dir == DIR_FORWARD);
    assert(cmd.right_speed == 180);
    assert(cmd.right_dir == DIR_FORWARD);
    assert(cmd.sequence == 42);

    printf("  PASS: Valid command frame parsed successfully.\n");
}

void test_protocol_parser_corrupted_crc() {
    printf("[TEST 3/7] ProtocolParser corrupted CRC rejection...\n");
    ProtocolParser parser;
    MotorCommand cmd = {};

    uint8_t frame[11];
    frame[0] = PROTOCOL_START_BYTE;
    frame[1] = PROTOCOL_VERSION;
    frame[2] = MSG_TYPE_COMMAND;
    frame[3] = 0x05;
    frame[4] = 200;
    frame[5] = DIR_FORWARD;
    frame[6] = 180;
    frame[7] = DIR_FORWARD;
    frame[8] = 42;
    uint8_t real_crc = crc8_calculate(frame, 9);
    frame[9] = real_crc ^ 0xFF; // Guarantees mismatch
    frame[10] = PROTOCOL_END_BYTE;

    bool parsed = false;
    for (int i = 0; i < 11; i++) {
        if (parser.parse_byte(frame[i], cmd)) {
            parsed = true;
        }
    }

    assert(parsed == false);
    printf("  PASS: Corrupted CRC rejected properly (real=0x%02X, corrupted=0x%02X).\n", real_crc, frame[9]);
}

void test_protocol_parser_bad_end_byte() {
    printf("[TEST 4/7] ProtocolParser bad end byte rejection...\n");
    ProtocolParser parser;
    MotorCommand cmd = {};

    uint8_t frame[11];
    frame[0] = PROTOCOL_START_BYTE;
    frame[1] = PROTOCOL_VERSION;
    frame[2] = MSG_TYPE_COMMAND;
    frame[3] = 0x05;
    frame[4] = 100;
    frame[5] = DIR_FORWARD;
    frame[6] = 100;
    frame[7] = DIR_FORWARD;
    frame[8] = 1;
    frame[9] = crc8_calculate(frame, 9);
    frame[10] = 0xEE; // Bad end byte!

    bool parsed = false;
    for (int i = 0; i < 11; i++) {
        if (parser.parse_byte(frame[i], cmd)) {
            parsed = true;
        }
    }

    assert(parsed == false);
    printf("  PASS: Bad end byte rejected properly.\n");
}

void test_parser_stream_resynchronization() {
    printf("[TEST 5/7] ProtocolParser noise rejection and stream resynchronization...\n");
    ProtocolParser parser;
    MotorCommand cmd = {};

    // Feed junk noise bytes that include spurious 0xAA markers
    uint8_t noise[] = { 0x12, 0x34, 0xAA, 0x99, 0xAA, 0x01, 0x55, 0xFE, 0x00 };
    for (size_t i = 0; i < sizeof(noise); i++) {
        bool res = parser.parse_byte(noise[i], cmd);
        assert(res == false); // Noise must never yield a valid command
    }

    // Now feed a pristine 11-byte command immediately after noise
    uint8_t valid_frame[11];
    valid_frame[0] = PROTOCOL_START_BYTE;
    valid_frame[1] = PROTOCOL_VERSION;
    valid_frame[2] = MSG_TYPE_COMMAND;
    valid_frame[3] = 0x05;
    valid_frame[4] = 150;
    valid_frame[5] = DIR_FORWARD;
    valid_frame[6] = 150;
    valid_frame[7] = DIR_FORWARD;
    valid_frame[8] = 99;
    valid_frame[9] = crc8_calculate(valid_frame, 9);
    valid_frame[10] = PROTOCOL_END_BYTE;

    bool parsed = false;
    for (int i = 0; i < 11; i++) {
        if (parser.parse_byte(valid_frame[i], cmd)) {
            parsed = true;
        }
    }

    assert(parsed == true);
    assert(cmd.left_speed == 150);
    assert(cmd.sequence == 99);
    printf("  PASS: Stream resynchronized successfully after noise.\n");
}

void test_sequence_freshness_and_wraparound() {
    printf("[TEST 6/7] Sequence freshness, duplicate rejection, and 255->0 wraparound...\n");
    uint8_t last_seq = 0;
    bool is_first = true;

    // 1. First frame accepted
    assert(check_freshness(0, last_seq, is_first) == true);
    assert(last_seq == 0);

    // 2. Incremental frames accepted within valid forward window (< 128)
    assert(check_freshness(1, last_seq, is_first) == true);
    assert(check_freshness(2, last_seq, is_first) == true);
    assert(check_freshness(50, last_seq, is_first) == true);
    assert(check_freshness(120, last_seq, is_first) == true);
    assert(check_freshness(180, last_seq, is_first) == true);

    // 3. Duplicate rejected
    assert(check_freshness(180, last_seq, is_first) == false);

    // 4. Stale (older) frame rejected
    assert(check_freshness(170, last_seq, is_first) == false);

    // 5. Advance to 254
    assert(check_freshness(254, last_seq, is_first) == true);

    // 6. Advance to 255
    assert(check_freshness(255, last_seq, is_first) == true);

    // 7. Wraparound from 255 -> 0 must be accepted as fresh!
    assert(check_freshness(0, last_seq, is_first) == true);
    assert(last_seq == 0);

    // 8. Wraparound jump 0 -> 5 accepted
    assert(check_freshness(5, last_seq, is_first) == true);
    assert(last_seq == 5);

    // 9. Stale packet from before wraparound (e.g. 250) must be rejected
    assert(check_freshness(250, last_seq, is_first) == false);

    // 10. Frame with diff >= 128 (e.g. 135 when last_seq is 5 -> diff=130) treated as stale
    assert(check_freshness(135, last_seq, is_first) == false);

    printf("  PASS: Sequence freshness and 255->0 wraparound verified.\n");
}

void test_protocol_build_telemetry() {
    printf("[TEST 7/7] protocol_build_telemetry serialization & battery validity flag...\n");
    TelemetryData telem = {};
    telem.state = STATE_RUNNING;
    telem.left_actual_speed = 150;
    telem.right_actual_speed = 160;
    telem.battery_mv = 11800; // 0x2E18
    telem.fault_flags = 0;
    telem.fw_counter = 105;

    uint8_t tx[13];
    size_t written = protocol_build_telemetry(telem, tx, sizeof(tx));

    assert(written == 13);
    assert(tx[0] == PROTOCOL_START_BYTE);
    assert(tx[1] == PROTOCOL_VERSION);
    assert(tx[2] == MSG_TYPE_TELEMETRY);
    assert(tx[3] == 0x07);
    assert(tx[4] == STATE_RUNNING);
    assert(tx[5] == 150);
    assert(tx[6] == 160);
    assert(tx[7] == 0x2E); // 11800 >> 8
    assert(tx[8] == 0x18); // 11800 & 0xFF
    assert(tx[9] == 0x00);
    assert(tx[10] == 105);
    assert(tx[11] == crc8_calculate(tx, 11));
    assert(tx[12] == PROTOCOL_END_BYTE);

    // Test with FAULT_BATT_INVALID (uncalibrated / disconnected ADC)
    telem.fault_flags = FAULT_BATT_INVALID;
    telem.battery_mv = 0;
    written = protocol_build_telemetry(telem, tx, sizeof(tx));
    assert(written == 13);
    assert(tx[7] == 0x00);
    assert(tx[8] == 0x00);
    assert(tx[9] == FAULT_BATT_INVALID);
    assert(tx[11] == crc8_calculate(tx, 11));

    printf("  PASS: Telemetry serialization and FAULT_BATT_INVALID matched spec.\n");
}

int main() {
    printf("=====================================================\n");
    printf("  ROBO RACER FIRMWARE PROTOCOL & SAFETY REGRESSION   \n");
    printf("=====================================================\n");
    test_crc8();
    test_protocol_parser_valid();
    test_protocol_parser_corrupted_crc();
    test_protocol_parser_bad_end_byte();
    test_parser_stream_resynchronization();
    test_sequence_freshness_and_wraparound();
    test_protocol_build_telemetry();
    printf("=====================================================\n");
    printf("  ALL FIRMWARE TESTS PASSED (7/7)                    \n");
    printf("=====================================================\n");
    return 0;
}
