#include <RFduinoBLE.h>
#include <Wire.h>
#include <Adafruit_PN532.h>

boolean connected = false, already_sent = false;
Adafruit_PN532 nfc(2,3);

void setup()
{
  RFduinoBLE.deviceName = "wrifd";

  RFduinoBLE.begin();
  nfc.begin();

  Serial.begin(9600);
  uint32_t versiondata = nfc.getFirmwareVersion();
  if (! versiondata) {
    Serial.print("Didn't find PN53x board");
    while (1); // halt
  }

  // Set the max number of retry attempts to read from a card
  // This prevents us from waiting forever for a card, which is
  // the default behaviour of the PN532.
  nfc.setPassiveActivationRetries(0x02);

  // configure board to read RFID tags
  nfc.SAMConfig();

  Serial.println("Waiting for an ISO14443A card");
}

void check_for_tag()
{
  uint8_t uid[] = { 0, 0, 0, 0, 0, 0, 0 },
      len_uid   = 0;

  boolean success = nfc.readPassiveTargetID(PN532_MIFARE_ISO14443A, &uid[0], &len_uid);

  //Serial.print("checked for tag: ");
  //Serial.println(success);

  RFduinoBLE.send((char*) uid,len_uid);
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
