// Fall + environment monitoring — MCU side (Arduino UNO Q)
// ADXL345 (I2C) + MQ-2 gas (A0) + DHT11 temp (D2), exposed to Python via the Bridge.
// Libraries needed: Arduino_RouterBridge, Adafruit ADXL345, Adafruit Unified Sensor,
//                   DHT sensor library (Adafruit)

#include "Arduino_RouterBridge.h"
#include <Wire.h>
#include <Adafruit_ADXL345_U.h>
#include <DHT.h>

#define MQ2_PIN   A0
#define DHT_PIN   2
#define DHT_TYPE  DHT11

Adafruit_ADXL345_Unified accel = Adafruit_ADXL345_Unified(12345);
DHT dht(DHT_PIN, DHT_TYPE);

bool accelOk = false;
int  gasRaw  = -1;      // latest MQ-2 analog reading
int  tempDeciC = -1000; // latest temp in tenths of a Celsius; -1000 = no reading yet
unsigned long lastDhtMs = 0;

// Returns "x,y,z,gas,temp" — accel in milli-(m/s^2), gas raw ADC, temp in 0.1 C.
const char* get_data() {
  static char buf[64];
  long x = 0, y = 0, z = 0;

  if (accelOk) {
    sensors_event_t event;
    accel.getEvent(&event);
    x = (long)(event.acceleration.x * 1000.0f);
    y = (long)(event.acceleration.y * 1000.0f);
    z = (long)(event.acceleration.z * 1000.0f);
  }

  snprintf(buf, sizeof(buf), "%ld,%ld,%ld,%d,%d", x, y, z, gasRaw, tempDeciC);
  return buf;
}

// Python calls this to light the on-board LED on any alert.
void set_alert(int on) {
  digitalWrite(LED_BUILTIN, on ? LOW : HIGH);  // active LOW
}

// Python pushes each status line (model result + decision) here
// so it also shows in the Serial Monitor.
void show_status(const char* msg) {
  Monitor.println(msg);
}

void setup() {
  Bridge.begin();
  Monitor.begin();

  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH);  // LED off
  pinMode(MQ2_PIN, INPUT);

  dht.begin();

  Monitor.println("Initializing ADXL345...");
  if (accel.begin()) {
    accel.setRange(ADXL345_RANGE_2_G);
    accelOk = true;
    Monitor.println("ADXL345 found and initialized.");
  } else {
    Monitor.println("ADXL345 not detected. Check wiring!");
  }

  Bridge.provide_safe("get_data", get_data);
  Bridge.provide_safe("set_alert", set_alert);
  Bridge.provide_safe("show_status", show_status);
  Monitor.println("Bridge ready");
}

unsigned long lastPrintMs = 0;

void loop() {
  // Gas: cheap analog read, refresh often
  gasRaw = analogRead(MQ2_PIN);

  // DHT11 is slow — read at most every 2 s (its maximum rate)
  unsigned long now = millis();
  if (now - lastDhtMs >= 2000) {
    lastDhtMs = now;
    float t = dht.readTemperature();
    if (!isnan(t)) tempDeciC = (int)(t * 10.0f);
  }

  // Print raw sensor readings on the Serial Monitor once per second
  if (now - lastPrintMs >= 1000) {
    lastPrintMs = now;
    Monitor.print("RAW  | ");
    Monitor.print(get_data());
    Monitor.println("  (x,y,z milli-m/s^2, gas, temp x0.1C)");
  }

  delay(50);
}