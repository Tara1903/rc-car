#pragma once
#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// Computes CRC-8 with polynomial 0x07 (x^8 + x^2 + x + 1), initial value 0x00.
// Matches Android implementation exactly.
uint8_t crc8_calculate(const uint8_t *data, size_t length);

#ifdef __cplusplus
}
#endif
