#include <RFduinoBLE.h>
#include <Wire.h>
#include "skyetek_m1.h"

boolean connected = false;
M1 nfc(5,6);

void setup()
{
  RFduinoBLE.deviceName = "wrifd_m1";
  RFduinoBLE.begin();

  uint32_t versiondata = nfc.getFirmwareVersion();
  if (! versiondata) {
    while (1); // halt
  }
}

void check_for_tag()
{
  uint8_t uid[80], len=0;

  if (len=nfc.selectTag(uid,sizeof(uid)))
    RFduinoBLE.send((char*) &uid[1], len-1); // first byte is tag type
}

void loop()
{
  RFduino_ULPDelay(500);

  if (connected)
    check_for_tag();
}

void RFduinoBLE_onConnect()
{
  connected = true;
  check_for_tag();
}

void RFduinoBLE_onDisconnect()
{
  connected = false;
}
