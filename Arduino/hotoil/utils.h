uint8_t parseInt(const char *args, uint16_t *value) {
  uint8_t size = 0;
  *value = 0;
  for (;;) {
    char ch = *args;
    if (ch >= '0' && ch <= '9') {
      *value = *value * 10 + (ch - '0');
    } else
      break;
    ++args;
    ++size;
  }
  if (*args != '\0' && *args != '-' && *args != ' ' && *args != '\t')
    return 0;
  return size;
}

uint8_t parseBytesArray(const char *args, uint8_t *value) {
  uint8_t size = 0, idx = 0;
  value[idx] = 0;
  for (;;) {
    char ch = *args;
    if (ch >= '0' && ch <= '9') {
      if (size > 0 && size % 2 == 0) value[++idx] = 0;
      value[idx] = (value[idx] * 10) + (ch - '0');
    } else
      break;
    ++size;
    ++args;
  }
  if (*args != '\0' && *args != '-' && *args != ' ' && *args != '\t')
    return 0;
  return (size + 1) / 2;
}

// Match a data-space string where the name comes from PROGMEM.
bool matchString(const char PROGMEM *name, const char *str, int len) {
  for (;;) {
    char ch1 = (char)(pgm_read_byte(name));
    if (ch1 == '\0')
      return len == 0;
    else if (len == 0)
      break;
    if (ch1 >= 'a' && ch1 <= 'z')
      ch1 = ch1 - 'a' + 'A';
    char ch2 = *str;
    if (ch2 >= 'a' && ch2 <= 'z')
      ch2 = ch2 - 'a' + 'A';
    if (ch1 != ch2)
      break;
    ++name;
    ++str;
    --len;
  }
  return false;
}

void print2digits(int number) {
  if (number >= 0 && number < 10) {
    Serial.write('0');
  }
  Serial.print(number);
}

byte crc8(byte *buffer, byte size) {
  byte crc = 0;
  for (byte i = 0; i < size; i++) {
    byte data = buffer[i];
    for (int j = 8; j > 0; j--) {
      crc = ((crc ^ data) & 1) ? (crc >> 1) ^ 0x8C : (crc >> 1);
      data >>= 1;
    }
  }
  return crc;
}