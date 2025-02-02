#define MODE_IDLE 0
#define MODE_WAIT 1
#define MODE_SHAKE_CALIBRATION 2
#define MODE_MANUAL 3

typedef struct {
  uint8_t mm;
  uint8_t hh;
  uint8_t delay_mm;
} alarm_t;

typedef union {
  struct {
    uint8_t shake : 1;
    uint8_t temperature : 1;
    uint8_t battery : 1;
    uint8_t heaterAmpMin : 2; // 0, 1, 2, 3
    uint8_t calibrationSeconds : 3; // 0-till event, 5sec, 10sec, ... 35sec
  };
  uint8_t raw;
} config_t;

typedef union {
  struct {
    uint8_t on : 1; // 0 - off; 1 - on
    uint8_t mode : 2; // 0 - idle; 1 - wait; 2 - calibration; 3 - manual
    uint8_t low_battery : 1;
    uint8_t hi_temperature : 1;
    uint8_t shake : 1;
    uint8_t bad_heater : 1;
    uint8_t bad_clock : 1;
  };
  uint8_t raw;
} status_t;

// List of all commands that are understood by the programmer.
typedef void (*commandFunc)(const char *args);
typedef struct
{
  const char PROGMEM *name;
  commandFunc func;
} command_t;

const char s_cmdMode[] PROGMEM = "MODE";
const char s_cmdState[] PROGMEM = "STATE";
const char s_cmdStatem[] PROGMEM = "STATEM";
const char s_cmdTime[] PROGMEM = "TI";

// [HH:2][MM:2][TimeoutOnSec:2][powerOnTimeStampSec:0-8]
// 1200152800
const char s_cmdTimeOn[] PROGMEM = "TION";
const char s_cmdBatteryLevel[] PROGMEM = "VB";
const char s_cmdBatteryLevelMin[] PROGMEM = "VBMIN";
const char s_cmdTemperature[] PROGMEM = "TG";
const char s_cmdTemperatureMax[] PROGMEM = "TGMAX";
const char s_cmdShakeDetectorAccMax[] PROGMEM = "SHACC";
const char s_cmdShakeDetectorGyrMax[] PROGMEM = "SHGYR";
const char s_cmdHeaterAmperage[] PROGMEM = "HTI";
const char s_cmdConfig[] PROGMEM = "CONF";

// write calibration time, heater amperage, battery, temperature, shake 
// >> confm 0203010101
const char s_cmdConfigm[] PROGMEM = "CONFM";

void cmdMode(const char *args);
void cmdState(const char *args);
void cmdStatem(const char *args);
void cmdTime(const char *args);
void cmdTimeOn(const char *args);
void cmdBatteryLevel(const char *args);
void cmdBatteryLevelMin(const char *args);
void cmdTemperature(const char *args);
void cmdTemperatureMax(const char *args);
void cmdShakeDetectorAccMax(const char *args);
void cmdShakeDetectorGyrMax(const char *args);
void cmdHeaterAmperage(const char *args);
void cmdConfig(const char *args);
void cmdConfigm(const char *args);

const command_t commands[] PROGMEM = {
  { s_cmdMode, cmdMode },
  { s_cmdState, cmdState },
  { s_cmdStatem, cmdStatem },
  { s_cmdTime, cmdTime },
  { s_cmdTimeOn, cmdTimeOn },
  { s_cmdBatteryLevel, cmdBatteryLevel },
  { s_cmdBatteryLevelMin, cmdBatteryLevelMin },
  { s_cmdTemperature, cmdTemperature },
  { s_cmdTemperatureMax, cmdTemperatureMax },
  { s_cmdShakeDetectorAccMax, cmdShakeDetectorAccMax },
  { s_cmdShakeDetectorGyrMax, cmdShakeDetectorGyrMax },
  { s_cmdHeaterAmperage, cmdHeaterAmperage },
  { s_cmdConfig, cmdConfig },
  { s_cmdConfigm, cmdConfigm },
  { 0, 0 }
};
