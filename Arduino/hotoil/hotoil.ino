#include "utils.h"
#include "hotoil.h"
#include <Wire.h>
#include <I2Cdev.h>
#include <MPU6050.h>
#include <TimeLib.h>
#include <DS1307RTC.h>
#include <microDS18B20.h>

#define INTERNAL_VREF 1.024
#define HEATER_AMP_PIN A0
#define BATTERY_VD_PIN A7
#define RELAY_PIN A1
#define DS18B20_PIN 10


#define BINARY_TRANSFER_MAX 64
#define BUFFER_MAX (BINARY_TRANSFER_MAX + 1)

char buffer[BUFFER_MAX];
int buflen = 0;

#define SHAKE_PERIOD_MS 3000

MicroDS18B20<DS18B20_PIN> ds18;
MPU6050 mpu;
uint32_t shakeOnTime, shakeOffTime, shakeLastTime;
uint8_t shakeDataCount;
bool shakeDetectorEnabled;
int16_t ax, ay, az;
int16_t gx, gy, gz;
long ACC, GYR;
long ACC0, GYR0;
float trACC = 2, trGYR = 2;
uint32_t accMax = 500, gyrMax = 500;
float batV;
float heatI;
tmElements_t tm;
alarm_t timer;
uint8_t batteryLevelMin;
uint8_t temperatureMax;
config_t config;
status_t status;
uint32_t timePowerOn;
uint32_t waitTimeout;
uint32_t powerOnTimeStamp;

void shakeDetection() {
  if (!shakeDetectorEnabled) return;

  if (millis() - shakeLastTime > 10) {
    if (shakeDataCount++ == 32) {
      shakeDataCount = 0;
      shakeLastTime = millis();

      ACC >>= 5;  // = ACC / 32
      GYR >>= 5;  // = GYR / 32

      if (ACC0 == 0 && GYR0 == 0) {
        ACC0 = ACC;
        GYR0 = GYR;
      } else {
        uint32_t ACC_d = abs(ACC0 - ACC);
        uint32_t GYR_d = abs(GYR0 - GYR);

        if (ACC_d > accMax || GYR_d > gyrMax) {
          if (shakeOnTime == 0) shakeOnTime = millis();
          if (millis() - shakeOnTime > SHAKE_PERIOD_MS) status.shake = 1;
          shakeOffTime = 0;
        } else {
          if (shakeOffTime == 0) shakeOffTime = millis();
          if (millis() - shakeOffTime > SHAKE_PERIOD_MS) status.shake = 0;
          shakeOnTime = 0;
        }
        ACC0 = ACC;
        GYR0 = GYR;
        ACC = 0;
        GYR = 0;
      }
    } else {
      mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
      ACC += (abs(ax) + abs(ay) + abs(az));
      GYR += (abs(gx) + abs(gy) + abs(gz));
    }
  }
}

void initShakeDetection() {
  // GND
  pinMode(A7, OUTPUT);
  pinMode(A6, OUTPUT);
  digitalWrite(A7, LOW);
  digitalWrite(A6, LOW);
  delay(100);

  mpu.initialize();
  shakeDetectorEnabled = mpu.testConnection();
  mpu.setFullScaleAccelRange(MPU6050_ACCEL_FS_2);
  if (!shakeDetectorEnabled) {
    Serial.println("MPU6050 not enabled");
  }
}

void initADC() {
  ADCSRA = (ADCSRA & 0xF8) | 0x04;
}

void readBatteryVoltage(float *v) {
#if defined(__AVR_ATmega328P__)
  analogReference(DEFAULT);
  delay(10);
  *v = analogRead(BATTERY_VD_PIN) * 35.0 / 1024.0;  // koef = 7
#else
  analogReference(INTERNAL4V096);
  delay(10);
  *v = analogRead(BATTERY_VD_PIN) * 28.0 / 1000.0;  // koef = 7
#endif
}

void readHeaterAmperage(float *heatI) {
#if defined(__AVR_ATmega328P__)
  analogReference(INTERNAL);
  delay(10);
  *heatI = analogRead(HEATER_AMP_PIN) * 110.0 / 1024.0;  // I = heatV / 0.01
#else
  analogReference(INTERNAL1V024);
  delay(10);
  *heatI = analogRead(HEATER_AMP_PIN) / 10.0;  // I = heatV / 0.01
#endif
}

void setup() {
  Serial.begin(9600);
  Wire.begin();
  initShakeDetection();
  initADC();
  pinMode(RELAY_PIN, OUTPUT);

  byte first = readEEPROM(0);
  if (first == 0xFF) {
    writeEEPROM(0, 0);
    config.calibrationSeconds = 3;
    config.heaterAmpMin = 1;
    writeEEPROM(1, config.raw);
  } else {
    config.raw = readEEPROM(1);
  }
  status.bad_clock = !RTC.read(tm);
  status.mode = MODE_IDLE;
  powerOff();
}


void loop() {

  listenPort();

  switch (status.mode) {
    case MODE_IDLE:
      handleIdle();
      break;
    case MODE_WAIT:
      handleWait();
      break;
    case MODE_SHAKE_CALIBRATION:
      handleShakeCalibration();
      break;
    case MODE_MANUAL:
      handleManual();
      break;
  }
  shakeDetection();
}

void handleIdle() {
  if (millis() - waitTimeout > 1000) {
    if (status.on && !testForRun()) {
      powerOff();
    }
    waitTimeout = millis();
  }
  delay(50);
}

bool isTimeForPowerOn() {
  status.bad_clock = !RTC.read(tm);
  if (status.bad_clock) {
    return millis() >= powerOnTimeStamp;
  } else {
    return (tm.Hour == timer.hh && tm.Minute == timer.mm);
  }
}

bool isTimeForPowerOff() {
  status.bad_clock = !RTC.read(tm);
  if (status.bad_clock) {
    return millis() >= powerOnTimeStamp;
  } else {
    uint16_t start_minutes = (tm.Hour * 60 + tm.Minute) - (timer.hh * 60 + timer.mm);
    return start_minutes >= timer.delay_mm;
  }
}

void handleWait() {
  if (millis() - waitTimeout > 1000) {
    if (testForRun()) {
      if (status.on) {
        if (isTimeForPowerOff()) {
          powerOff();
          status.mode = MODE_IDLE;
        }
      } else {
        if (isTimeForPowerOn()) {
          powerOnTimeStamp += ((uint32_t)timer.delay_mm) * 60 * 1000;
          if (!powerOn())
            status.mode = MODE_IDLE;
        }
      }
    } else {
      status.mode = MODE_IDLE;
    }
    waitTimeout = millis();
  }
}

void handleManual() {
  if (!status.on) {
    powerOn();
    status.mode = MODE_IDLE;
  } else {
    powerOff();
    status.mode = MODE_IDLE;
  }
}

void handleShakeCalibration() {
  if (shakeDetectorEnabled) {
    uint32_t _t = millis();
    accMax = 0;
    gyrMax = 0;
    bool wait = true;
    while (wait) {

      if (shakeDataCount++ == 32) {
        shakeDataCount = 0;
        shakeLastTime = millis();

        ACC >>= 5;  // = ACC / 32
        GYR >>= 5;  // = GYR / 32

        if (ACC0 == 0 && GYR0 == 0) {
          ACC0 = ACC;
          GYR0 = GYR;
        } else {
          uint32_t ACC_d = abs(ACC0 - ACC);
          uint32_t GYR_d = abs(GYR0 - GYR);

          if (accMax == 0 && gyrMax == 0) {
            accMax = ACC_d;
            gyrMax = GYR_d;
          } else {
            accMax = (accMax + ACC_d) / 2;
            gyrMax = (gyrMax + GYR_d) / 2;
          }

          ACC0 = ACC;
          GYR0 = GYR;
          ACC = 0;
          GYR = 0;

          Serial.print(ACC_d);
          Serial.print(" ");
          Serial.print(GYR_d);
          Serial.println();

          delay(5);
        }
      } else {
        mpu.getMotion6(&ax, &ay, &az, &gx, &gy, &gz);
        ACC += (abs(ax) + abs(ay) + abs(az));
        GYR += (abs(gx) + abs(gy) + abs(gz));
      }

      if (config.calibrationSeconds > 0) {
        wait = millis() - _t < config.calibrationSeconds * 1000 * 5;
      } else {
        wait = Serial.available() > 0;
        if (wait) Serial.read();
      }
    }
  } else {
    sendError();
  }

  status.mode = MODE_IDLE;
}

bool powerOn() {
  if (testForRun()) {
    timePowerOn = millis();
    digitalWrite(RELAY_PIN, HIGH);
    status.on = 1;
    return true;
  }
  return false;
}

void powerOff() {
  digitalWrite(RELAY_PIN, LOW);
  status.on = 0;
  timePowerOn = 0;
}

bool testForRun() {
  // Temperature
  ds18.requestTemp();
  delay(1000);
  if (ds18.readTemp()) {
    if (ds18.getTemp() > temperatureMax) {
      status.hi_temperature = 1;
    } else {
      status.hi_temperature = 0;
    }
  }
  // Battery
  float v;
  readBatteryVoltage(&v);
  if (v < batteryLevelMin) {
    status.low_battery = 1;
  } else {
    status.low_battery = 0;
  }

  if (status.on && (millis() - timePowerOn > 3000)) {
    readHeaterAmperage(&v);
    if (config.heaterAmpMin > 0 && v < config.heaterAmpMin) {
      status.bad_heater = 1;
      return false;
    } else {
      status.bad_heater = 0;
    }
  }

  if (config.battery && status.low_battery) return false;
  if (config.temperature && status.hi_temperature) return false;
  if (config.shake && status.shake) return false;

  return true;
}

void cmdMode(const char *args) {
  uint8_t _m;
  if (parseBytesArray(args, &_m) > 0) {
    if (_m >= 0 && _m < 4) {
      status.mode = _m;
      sendOk();
      return;
    }
  } else {
    Serial.println(status.mode, DEC);
    return;
  }
  sendError();
}

void cmdState(const char *args) {
  Serial.println(status.raw, DEC);
}

void cmdStatem(const char *args) {
  Serial.print(F("ON="));
  Serial.println(status.on, DEC);
  Serial.print(F("MODE="));
  Serial.println(status.mode, DEC);
  Serial.print(F("Low battery="));
  Serial.println(status.low_battery, DEC);
  Serial.print(F("Hi temperature="));
  Serial.println(status.hi_temperature, DEC);
  Serial.print(F("Shake detected="));
  Serial.println(status.shake, DEC);
  Serial.print(F("Bad heater="));
  Serial.println(status.bad_heater, DEC);
  Serial.print(F("Bad clock="));
  Serial.println(status.bad_clock, DEC);
}

void cmdTime(const char *args) {
  uint8_t time[2];
  uint8_t l = parseBytesArray(args, time);
  if (RTC.read(tm)) {
    if (l > 1 && time[1] < 60) tm.Minute = time[1];
    if (l > 0 && time[0] < 24) tm.Hour = time[0];
    if (l > 0) {
      if (RTC.write(tm)) {
        status.bad_clock = 0;
        sendOk();
        return;
      }
    } else {
      print2digits(tm.Hour);
      print2digits(tm.Minute);
      Serial.println();
      return;
    }
  }
  status.bad_clock = 1;
  sendError();
}

void cmdTimeOn(const char *args) {
  uint8_t time[3];
  uint8_t l = parseBytesArray(args, time, 3);
  if (l == 0) {
    print2digits(timer.hh);
    print2digits(timer.mm);
    print2digits(timer.delay_mm);
    if (powerOnTimeStamp > millis()) {
      Serial.print((powerOnTimeStamp - millis()) / 1000, DEC);
    } else {
      Serial.print("0");
    }
    Serial.println();
    return;
  } else if (l == 3) {
    args += (l * 2);
    parseLong(args, &powerOnTimeStamp);
    powerOnTimeStamp *= 1000;
    powerOnTimeStamp += millis();
    timer.hh = time[0] < 24 ? time[0] : 0;
    timer.mm = time[1] < 60 ? time[1] : 0;
    timer.delay_mm = time[2] < 60 ? time[2] : 0;
    sendOk();
    return;
  }
  sendError();
}

void cmdBatteryLevel(const char *args) {
  float v;
  readBatteryVoltage(&v);
  Serial.println(v, 2);
}

void cmdBatteryLevelMin(const char *args) {
  uint8_t _v;
  if (parseBytesArray(args, &_v) > 0) {
    if (_v >= 0 && _v < 15) {
      batteryLevelMin = _v;
      sendOk();
      return;
    }
  } else {
    Serial.println(batteryLevelMin, DEC);
    return;
  }
  sendError();
}

void cmdTemperature(const char *args) {
  ds18.requestTemp();
  delay(1000);
  if (ds18.readTemp())
    Serial.println(ds18.getTemp(), 2);
  else sendError();
}

void cmdTemperatureMax(const char *args) {
  uint8_t _v;
  if (parseBytesArray(args, &_v) > 0) {
    if (_v >= 0 && _v < 5) {
      temperatureMax = _v;
      sendOk();
      return;
    }
  } else {
    Serial.println(temperatureMax, DEC);
    return;
  }
  sendError();
}

void cmdShakeDetectorAccMax(const char *args) {
  uint16_t v;
  uint8_t l = parseInt(args, &v);
  if (l > 0) {
    accMax = v;
    sendOk();
  } else {
    Serial.println(accMax, DEC);
  }
}

void cmdShakeDetectorGyrMax(const char *args) {
  uint16_t v;
  uint8_t l = parseInt(args, &v);
  if (l > 0) {
    gyrMax = v;
    sendOk();
  } else {
    Serial.println(gyrMax, DEC);
  }
}

void cmdHeaterAmperage(const char *args) {
  float v;
  readHeaterAmperage(&v);
  Serial.println(v, 2);
}

void cmdConfig(const char *args) {
  uint16_t _v;
  if (parseInt(args, &_v) > 0) {
    config.raw = _v;
    writeEEPROM(1, config.raw);
    sendOk();
    return;
  } else {
    Serial.println(config.raw, DEC);
    return;
  }
  sendError();
}

void cmdConfigm(const char *args) {
  uint8_t c[5];
  if (parseBytesArray(args, c) == 5) {
    config.shake = c[4];
    config.temperature = c[3];
    config.battery = c[2];
    config.heaterAmpMin = c[1];
    config.calibrationSeconds = c[0];
    writeEEPROM(1, config.raw);
    sendOk();
  } else {
    Serial.print(F("Shake detection="));
    Serial.println(config.shake, DEC);
    Serial.print(F("Temperature="));
    Serial.println(config.temperature, DEC);
    Serial.print(F("Battery="));
    Serial.println(config.battery, DEC);
    Serial.print(F("Heater amperage minimum [A]="));
    Serial.println(config.heaterAmpMin, DEC);
    Serial.print(F("Calibration time [seconds]="));
    Serial.println(config.calibrationSeconds, DEC);
  }
}

void sendError() {
  Serial.println("ERROR");
}

void sendOk() {
  Serial.println(status.raw, DEC);
}

void listenPort() {
  if (Serial.available()) {
    // Process serial input for commands from the host.
    int ch = Serial.read();
    if (ch == 0x0A || ch == 0x0D) {
      // End of the current command.  Blank lines are ignored.
      if (buflen > 0) {
        buffer[buflen] = '\0';
        buflen = 0;
        processCommand(buffer);
      }
    } else if (ch == 0x08) {
      // Backspace over the last character.
      if (buflen > 0)
        --buflen;
    } else if (buflen < (BUFFER_MAX - 1)) {
      // Add the character to the buffer after forcing to upper case.
      if (ch >= 'a' && ch <= 'z')
        buffer[buflen++] = ch - 'a' + 'A';
      else
        buffer[buflen++] = ch;
    }
  }
}

// Process commands from the host.
void processCommand(const char *buf) {
  // Skip white space at the start of the command.
  while (*buf == ' ' || *buf == '\t')
    ++buf;
  if (*buf == '\0')
    return;  // Ignore blank lines.

  // Extract the command portion of the line.
  const char *cmd = buf;
  int len = 0;
  for (;;) {
    char ch = *buf;
    if (ch == '\0' || ch == ' ' || ch == '\t')
      break;
    ++buf;
    ++len;
  }

  // Skip white space after the command name and before the arguments.
  while (*buf == ' ' || *buf == '\t')
    ++buf;

  // Find the command and execute it.
  int index = 0;
  for (;;) {
    const char *name = (const char *)(pgm_read_word(&(commands[index].name)));
    if (!name)
      break;
    if (matchString(name, cmd, len)) {
      commandFunc func = (commandFunc)(pgm_read_word(&(commands[index].func)));
      (*func)(buf);
      return;
    }
    ++index;
  }
  sendError();
}
