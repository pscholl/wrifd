#include <stdint.h>
#include <stdbool.h>

#ifndef __M1_H__
#define __M1_H__

class M1 {
  public:
  M1 (int rx=-1, int tx=-1);

  uint32_t getFirmwareVersion();
  uint8_t selectTag(uint8_t[], uint8_t);

  //void sleepmode(bool enable);
};

#endif
