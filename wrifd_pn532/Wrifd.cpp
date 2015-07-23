#include <Adafruit_PN532.h>

Wrifd::Wrifd(int irq, int res, int rx, int tx) {
  pn532 = Adafruit_PN532(irq,res);
}
