#include <cassert>
#include <cstdio>
#include <cstring>
#include "../src/protocol.h"
#include "../src/crc8.h"

void test_crc8() {
    printf("[TEST] CRC8 algorithm and test vectors...\n");
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
    printf("[TEST] ProtocolParser valid command frame...\n");
    ProtocolParser parser;
    MotorCommand cmd = {};

    // 11-byte Command:
    // [0]=0xAA, [1]=0x01, [2]=0x01, [3]=0x05, [4]=200, [5]=DIR_FORWARD, [6]=180, [7]=DIR_FORWARD, [8]=42, [9]=CRC, [10]=0x55
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
    printf("[TEST] ProtocolParser corrupted CRC rejection...\n");
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
    printf("[TEST] ProtocolParser bad end byte rejection...\n");
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
        parsed = parser.parse_byte(frame[i], cmd);
    }

    assert(parsed == false);
    printf("  PASS: Bad end byte rejected properly.\n");
}

void test_protocol_build_telemetry() {
    printf("[TEST] protocol_build_telemetry serialization...\n");
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

    printf("  PASS: Telemetry serialization matched spec.\n");
}

int main() {
    printf("=== ROBO RACER FIRMWARE PROTOCOL UNIT TESTS ===\n");
    test_crc8();
    test_protocol_parser_valid();
    test_protocol_parser_corrupted_crc();
    test_protocol_parser_bad_end_byte();
    test_protocol_build_telemetry();
    printf("=== ALL FIRMWARE TESTS PASSED (5/5) ===\n");
    return 0;
}
