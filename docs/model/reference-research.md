# Robo Racer 4WD Physical Hardware Reference Research

This document compiles the hardware research, visual references, dimensions, materials, and component placement for the Robo Racer 4WD robotic RC vehicle. All findings are derived from real component datasheets, vendor specifications, and physical hardware kits.

---

## Component Reference Breakdown

### 1. Chassis and Wheel Assembly

- **Component Identified:** 4WD Double-Deck Acrylic Smart Car Chassis Kit with TT DC Gear Motors and 66 mm Rubber Wheels.
- **Reference Sources:**
  - Robu.in 4WD Smart Car Platform: `https://robu.in/product/4-wheel-drive-4wd-smart-car-chassis-kit/`
  - Kitsguru 66mm Wheel / TT Motor Specifications: `https://kitsguru.com/product/yellow-dc-gear-motor-with-wheel/`
  - Digikey Smart Robot Car Chassis: `https://www.digikey.com/en/products/detail/seeed-technology-co-ltd/110090035/5482563`
- **Observed Visual Details:**
  - **Plates:** Dual-deck laser-cut transparent/amber acrylic plates (approx. 255 mm length × 150 mm outer width × 3 mm plate thickness). Contoured front and rear edges with cutouts for motor mounting brackets, encoder disks, and wire pass-throughs.
  - **Standoffs:** 6× M3 brass hexagonal cylindrical standoffs (30 mm height) separating the lower and upper decks with M3 nickel-plated pan-head screws.
  - **Motors (4×):** Yellow plastic gearbox housing (~70 mm × 23 mm × 18 mm) with a silver cylindrical DC motor casing mounted to the rear of the gearbox. White plastic dual-flat output shaft (5.3 mm with dual D-flats) protruding outward. Motor brackets are black or clear laser-cut acrylic tabs secured with long M3 through-bolts and nuts.
  - **Wheels (4×):** 66 mm outer diameter × 27 mm tread width. Outer tire is black natural rubber with deep chevron/herringbone tread pattern and rounded outer shoulders. Rim is vivid yellow molded ABS plastic with 5 spokes and a recessed central hub containing the D-shaft socket.
- **Confirmed vs. Inferred:**
  - *Confirmed:* 66 mm diameter, 27 mm width, 1:48 gear ratio yellow TT motors, 30 mm deck standoff height, double-deck acrylic construction.
  - *Inferred:* Specific contour cutouts on the acrylic perimeter match standard open-source 4WD robotic chassis CAD patterns.
- **Unresolved Variations:** Some kit vendors supply 200 mm × 140 mm plates while others supply 255 mm × 150 mm plates. The 255 mm × 150 mm platform accommodates the dual L298N drivers, ESP32, XL6019 buck-boost, and 3S battery without component overlap.

---

### 2. ESP32 Development Board (Prakti 30-Pin CP2102)

- **Component Identified:** Prakti ESP32 DevKit v1 (30-pin, CP2102 USB-to-UART, ESP32-WROOM-32).
- **Reference Sources:**
  - Espressif ESP32-WROOM-32 Datasheet: `https://www.espressif.com/sites/default/files/documentation/esp32-wroom-32_datasheet_en.pdf`
  - NodeMCU-32S / ESP32 30-Pin CP2102 Reference: `https://www.theengineerstore.in/products/esp32-development-board-cp2102-30-pin`
  - RhydoLabz ESP32 30-Pin Layout: `https://www.rhydolabz.com/wireless-bluetooth-c-130_133/esp32-devkit-v1-30-pin-cp2102-p-2486.html`
- **Observed Visual Details:**
  - **PCB:** Dark matte black/deep blue FR-4 substrate, approx. 51.5 mm length × 28.0 mm width × 1.6 mm thickness.
  - **Shield:** Prominent nickel-plated metal RF shield (ESP32-WROOM-32, 18 mm × 25.5 mm) with engraved Espressif logo and regulatory text. Distinct black serpentine PCB trace antenna extending past the shield at the top edge.
  - **Headers:** Two parallel rows of 15-pin 2.54 mm pitch gold/tin headers (0.9-inch / 22.86 mm row-to-row spacing).
  - **Interface & ICs:** Micro-USB Type-B connector centered at the bottom edge; Silicon Labs CP2102 square QFN package adjacent to the USB connector; AMS1117-3.3 SOT-223 voltage regulator; two micro-tactile pushbuttons (EN/Reset on left, BOOT on right); blue onboard SMD diagnostic LED at GPIO 2 and red power LED.
- **Confirmed vs. Inferred:**
  - *Confirmed:* 30-pin layout, CP2102 chip, ESP32-WROOM-32 module, GPIO 2 hardwired to onboard blue LED.
  - *Inferred:* Micro-USB versus USB-C depends on production batch; standard DevKit v1 uses micro-USB.

---

### 3. Dual L298N Motor Driver Modules

- **Component Identified:** L298N Dual H-Bridge Motor Driver Breakout Board (2 boards installed).
- **Reference Sources:**
  - STMicroelectronics L298 Datasheet: `https://www.st.com/resource/en/datasheet/l298.pdf`
  - Components101 L298N Module Guide: `https://components101.com/modules/l298n-motor-driver-module`
  - HandsOnTec L298N Technical Manual: `https://handsontec.com/index.php/product/l298n-dual-h-bridge-motor-driver/`
- **Observed Visual Details:**
  - **PCB:** Red solder mask FR-4 board, 43.5 mm × 43.5 mm with four M3 corner mounting holes.
  - **Heatsink:** Large black anodized aluminum heatsink (~24 mm height × 23 mm width × 12 mm depth) with 6 vertical cooling fins, fastened with a silver screw and nut to the Multiwatt15 vertical L298N IC.
  - **Terminals:** 
    - 1× 3-position green/blue screw terminal block (5.08 mm pitch) for power (12V, GND, 5V).
    - 2× 2-position green/blue screw terminal blocks on opposite sides for Motor A (OUT1/OUT2) and Motor B (OUT3/OUT4).
  - **Components:** 8× SMD or axial freewheeling flyback diodes surrounding the IC; 1× 78M05 5V regulator; 2× large cylindrical radial aluminum electrolytic filter capacitors (35V, black sleeve with white stripe); 6-pin male logic header (ENA, IN1, IN2, IN3, IN4, ENB) with black shorting jumpers; red power indicator LED.
- **Confirmed vs. Inferred:**
  - *Confirmed:* Red PCB, 43 mm square form factor, Multiwatt15 package with finned heatsink, dual 2-pin motor terminals, 3-pin power terminal, 2 independent boards for left and right wheels.
  - *Inferred:* Heatsink orientation facing toward the center of the chassis to optimize wire clearance.

---

### 4. XL6019 Buck-Boost DC-DC Converter

- **Component Identified:** amiciSolar XL6019 High-Efficiency Buck-Boost / Boost Step-Up/Down Converter Module.
- **Reference Sources:**
  - XLSEMI XL6019 180kHz 60V 5A Switching Regulator: `http://www.xlsemi.com/datasheet/XL6019%20datasheet.pdf`
  - Amicikart XL6019 Step-Up/Step-Down Power Module: `https://www.amicikart.com/`
  - Joy-IT XL6019 DC-DC Module Reference: `https://joy-it.net/en/products/COM-KN-XL6019`
- **Observed Visual Details:**
  - **PCB:** Deep blue or black double-sided PCB, approx. 50 mm length × 25 mm width × 14 mm total height.
  - **Key Components:**
    - TO-263-5L surface mount XL6019 IC soldered with thermal dissipation copper pad.
    - Large 47 µH toroidal or shielded ferrite core power inductor (black cylindrical drum, ~12 mm diameter).
    - Blue 3296W multi-turn cermet trimmer potentiometer with top brass adjustment screw.
    - 2× radial aluminum electrolytic capacitors (input and output filtering).
    - 2-pin screw terminals or through-hole solder lugs at each end (IN+/IN- on input side, OUT+/OUT- on output side).
- **Confirmed vs. Inferred:**
  - *Confirmed:* XL6019 IC, blue trimpot with brass adjustment screw, power inductor, electrolytic capacitors, terminal blocks.
  - *Inferred:* Board mounted on lower deck central bay to feed the two L298N drivers with shortest high-current cable runs.

---

### 5. Battery Pack and Wiring Architecture

- **Component Identified:** 3S 18650 Li-ion Battery Pack (11.1V nominal, 12.6V fully charged, 3S1P or 3S2P configuration).
- **Reference Sources:**
  - Roboman 3S 11.1V 18650 Pack: `https://roboman.in/product/3s-11-1v-18650-li-ion-battery-pack/`
  - Standard Robotics 18650 Blue PVC Pack Specifications: 3-cell inline or triangular bundle wrapped in blue industrial PVC heat-shrink tubing (~68 mm × 55 mm × 20 mm).
- **Wiring Layout:**
  - Heavy 18–20 AWG red/black silicone wires connecting battery to rocker switch and XL6019 input.
  - Regulated 6.0V output from XL6019 branching into thick dual red/black power lines entering the 12V/GND terminals of both L298N drivers.
  - Multi-colored ribbon jumper wires (26 AWG Dupont lines) connecting ESP32 GPIOs to L298N logic headers:
    - Left Driver: GPIO 18 (ENA), GPIO 19 (IN1), GPIO 17 (IN2), GPIO 16 (IN3), GPIO 4 (IN4), GPIO 25 (ENB).
    - Right Driver: GPIO 23 (ENA), GPIO 22 (IN1), GPIO 21 (IN2), GPIO 27 (IN3), GPIO 14 (IN4), GPIO 26 (ENB).
  - Motor pairs wired to L298N terminal blocks (yellow/green and red/black twisted pairs from motors).
  - Precision two-resistor voltage divider (100kΩ / 20kΩ) tapped from battery positive to GPIO 34.
