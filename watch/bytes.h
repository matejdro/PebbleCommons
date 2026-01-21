#pragma once
#include <pebble.h>

uint16_t read_uint16_from_byte_array(const uint8_t* bytes, size_t offset);
uint32_t read_uint32_from_byte_array(const uint8_t* bytes, size_t offset);