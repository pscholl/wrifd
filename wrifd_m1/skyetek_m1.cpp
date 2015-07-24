#include "skyetek_m1.h"
#include <Wire.h>

const char read_firmware[] = {4+2, 0x20, 0x22, 0x01, 0x01},
           toggle_sleep[]  = {5+2, 0x20, 0x42, 0x04, 0x01, 0x00},
           select_tag[]    = {3+2, 0x20, 0x14, 0x00};

// *dataP is a pointer to the byte array over which the CRC is to be calculated
// n is the number of bytes in the array pointed to by *dataP
//
unsigned int crc16( unsigned char * dataP, unsigned char n )
{
  unsigned char i, j; // byte counter, bit counter
  unsigned int crc_16; // calculation
  crc_16 = 0x0000; // PRESET value
  for (i = 0; i < n; i++) // check each byte in the array
  {
    crc_16 ^= *dataP++; //
    for (j = 0; j < 8; j++) // test each bit in the byte
    {
      if(crc_16 & 0x0001 ) //
      {
        crc_16 >>= 1;
        crc_16 ^= 0x8408; // POLYNOMIAL x'16 + x'12 + x'5 + 1
      }
      else
      {
        crc_16 >>= 1;
      }
    }
  }
  return( crc_16 ); // returns calculated crc (16 bits)
}

void m1write(const char buf[], size_t len)
{
  Serial.write(0x02);
  for(size_t i=0; i<len; i++) {
    Serial.write(buf[i]);
  }
  uint16_t crc = crc16((unsigned char*) buf, len);
  Serial.write((uint8_t) (crc>>8));
  Serial.write((uint8_t) crc);
}

uint8_t m1read(char buf[], uint8_t n, uint32_t timeout)
{
  bool intime = false;
  uint32_t now = millis();
  uint8_t stx, len;

  for (now=millis(); (intime = (millis()-now < timeout)) && !Serial.available();) ;
  if(!intime) return 0;
  stx = Serial.read();

  for (now=millis(); (intime = (millis()-now < timeout)) && !Serial.available();) ;
  if(!intime) return 0;
  len = Serial.read();

  for (uint8_t i=0; i<len && i<n; i++) {
    for (now=millis(); (intime = (millis()-now < timeout)) && !Serial.available();) ;
    if(!intime) return 0;
    buf[i] = Serial.read();
  }

  return len;
}

M1::M1 (int rx, int tx)
{
  Serial.begin(9600,rx,tx);
}

uint32_t M1::getFirmwareVersion()
{
  uint8_t buf[10], n;

  do {
    m1write(read_firmware, sizeof(read_firmware));
  } while( (n=m1read((char*) buf,sizeof(buf),5000)) == 0 );

  if (n==0 || !buf[0] == 0x22)
    return 0;

  return *((uint32_t*) buf[1]);
}

uint8_t M1::selectTag(uint8_t buf[], uint8_t len)
{
  uint8_t n;

  do {
    m1write(select_tag, sizeof(select_tag));
  } while ( (n=m1read((char*) buf, len, 500)) == 0 );

  if (n==0)
      return 0;

  if (buf[0] != 0x14)
    return 0;

  memmove(buf,&buf[1],n-3); // without code and crc fields!
  return n-3;
}
