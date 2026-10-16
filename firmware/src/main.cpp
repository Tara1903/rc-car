#include <Arduino.h>
#include <BluetoothSerial.h>
#include "config.h"
#include "protocol.h"
#include "motor_controller.h"
#include "safety_watchdog.h"
#include "battery_monitor.h"

BluetoothSerial SerialBT;
ProtocolParser parser;
MotorController motors;
SafetyWatchdog watchdog;
BatteryMonitor battery;

static unsigned long last_telemetry_time = 0;
static uint8_t telemetry_counter = 0;
static bool bt_client_connected = false;

void bt_callback(esp_spp_cb_event_t event, esp_spp_cb_param_t *param) {
    if (event == ESP_SPP_SRV_OPEN_EVT) {
        bt_client_connected = true;
        watchdog.feed(); // Grace period upon connection
    } else if (event == ESP_SPP_CLOSE_EVT) {
        bt_client_connected = false;
        watchdog.reset();
        motors.emergency_stop();
    }
}

void setup() {
    Serial.begin(115200);
    pinMode(PIN_STATUS_LED, OUTPUT);
    digitalWrite(PIN_STATUS_LED, LOW);

    // Initialize subsystems
    motors.begin();
    battery.begin();

    // Register callback and start Bluetooth Serial
    SerialBT.register_callback(bt_callback);
    if (!SerialBT.begin(DEVICE_NAME)) {
        Serial.println("FATAL: Bluetooth initialization failed!");
        while (1) {
            digitalWrite(PIN_STATUS_LED, !digitalRead(PIN_STATUS_LED));
            delay(100);
        }
    }

    Serial.println("======================================");
    Serial.println("  ROBO RACER ESP32 FIRMWARE v1.1      ");
    Serial.println("  Ready for SPP Connection           ");
    Serial.print("  Device Name: ");
    Serial.println(DEVICE_NAME);
    Serial.println("======================================");
}

void loop() {
    unsigned long now = millis();

    // 1. Process incoming Bluetooth stream
    while (SerialBT.available()) {
        uint8_t byte = SerialBT.read();
        MotorCommand cmd;
        if (parser.parse_byte(byte, cmd)) {
            watchdog.feed();
            motors.apply_command(cmd);
        }
    }

    // 2. Safety Watchdog Check
    if (watchdog.is_expired()) {
        motors.emergency_stop();
    } else {
        motors.update_ramp();
    }

    // 3. Update battery sensor
    battery.update();

    // 4. Send Telemetry at 20 Hz
    if (now - last_telemetry_time >= TELEMETRY_INTERVAL_MS) {
        last_telemetry_time = now;
        telemetry_counter++;

        TelemetryData telem;
        telem.fw_counter = telemetry_counter;
        telem.left_actual_speed = motors.get_left_actual_speed();
        telem.right_actual_speed = motors.get_right_actual_speed();
        telem.battery_mv = battery.get_voltage_mv();
        telem.fault_flags = 0;

        if (watchdog.is_expired()) {
            telem.fault_flags |= FAULT_WATCHDOG_TIMEOUT;
            telem.state = STATE_FAILSAFE;
        } else if (battery.is_low_battery()) {
            telem.fault_flags |= FAULT_LOW_BATTERY;
            telem.state = STATE_LOW_BATTERY;
        } else if (telem.left_actual_speed > 0 || telem.right_actual_speed > 0) {
            telem.state = STATE_RUNNING;
        } else {
            telem.state = STATE_IDLE;
        }

        if (bt_client_connected) {
            uint8_t telem_buf[TELEM_FRAME_SIZE];
            size_t len = protocol_build_telemetry(telem, telem_buf, sizeof(telem_buf));
            if (len > 0) {
                SerialBT.write(telem_buf, len);
            }
        }
    }

    // 5. Status LED Indication
    if (!bt_client_connected) {
        // Slow blink (1 Hz): Waiting for connection
        digitalWrite(PIN_STATUS_LED, (now / 500) % 2);
    } else if (watchdog.is_expired()) {
        // Fast strobe (5 Hz): Failsafe / packet timeout
        digitalWrite(PIN_STATUS_LED, (now / 100) % 2);
    } else {
        // Solid ON: Connected and healthy
        digitalWrite(PIN_STATUS_LED, HIGH);
    }
}
