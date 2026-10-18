# Robo Racer 4WD 3D Asset Specification & Hierarchy

This document provides the technical specification, node hierarchy, coordinate conventions, material assignments, and animation integration requirements for the Robo Racer 4WD vehicle model.

---

## 1. Asset Overview

- **Asset Path:** `app/src/main/assets/models/robo_racer_car.glb`
- **Master Blender File:** `tools/modeling/robo_racer_car.blend`
- **Generator Script:** `tools/modeling/generate_robo_racer_model.py`
- **Format:** glTF 2.0 Binary (`.glb`)
- **Total Asset Size:** ~673 KB (673,736 bytes)
- **Authoring Tool:** Blender 5.2.0 LTS
- **Scene Nodes:** ~348 scene nodes (clean hierarchy under `car_root`)
- **Material Count:** 24 physically based materials (PBR metallic-roughness, transmission, emissive)

---

## 2. Coordinate Conventions

### Blender Modeling Space (Z-Up)
- **Origin $(0, 0, 0)$:** Ground level directly beneath the geometrical center of the vehicle chassis.
- **$X$-Axis:** Left ($-X$) / Right ($+X$). Wheel track spacing: $\pm 0.076\,\text{m}$ ($\pm 76\,\text{mm}$).
- **$Y$-Axis:** Forward / Front ($+Y$) / Rear ($-Y$). Wheelbase: $\pm 0.065\,\text{m}$ ($\pm 65\,\text{mm}$, total $130\,\text{mm}$).
- **$Z$-Axis:** Ground ($0.0\,\text{m}$) / Up ($+Z$). Wheel axle height: $0.033\,\text{m}$ ($33\,\text{mm}$).

### glTF 2.0 Export Space (Y-Up, standard `export_yup=True`)
- **$+Y$:** World Up.
- **$-Z$:** World Forward (Vehicle Heading / Direction of Travel).
- **$+Z$:** World Rear (Toward Android Chase Camera).
- **$+X$:** Vehicle Right.
- **$-X$:** Vehicle Left.

---

## 3. Node Hierarchy & Animation Interface

The model is organized into a clean scene graph designed specifically for real-time mobile animation:

```
car_root [Root Node: World Transform & Position]
â”œâ”€â”€ chassis_root [Chassis Root: Dynamic Pitch, Squat, Dive & Roll]
â”‚   â”œâ”€â”€ chassis_bottom_deck [Lower Acrylic Plate with Bumpers & Side Tabs]
â”‚   â”œâ”€â”€ chassis_top_deck [Upper Acrylic Plate with Bumpers & Side Tabs]
â”‚   â”œâ”€â”€ chassis_standoffs [6x M3 Brass Standoffs with Top/Bottom Screws]
â”‚   â”œâ”€â”€ motor_left_front [TT Motor Body, Gearbox, Canister, Bracket, 20-Slot Encoder]
â”‚   â”œâ”€â”€ motor_left_rear [TT Motor Body, Gearbox, Canister, Bracket, 20-Slot Encoder]
â”‚   â”œâ”€â”€ motor_right_front [TT Motor Body, Gearbox, Canister, Bracket, 20-Slot Encoder]
â”‚   â”œâ”€â”€ motor_right_rear [TT Motor Body, Gearbox, Canister, Bracket, 20-Slot Encoder]
â”‚   â”œâ”€â”€ esp32_board [30-Pin ESP32, WROOM-32 Shield, CP2102, Buttons, LEDs]
â”‚   â”œâ”€â”€ l298n_left [Left Dual H-Bridge Driver, Finned Heatsink, Terminals, Caps]
â”‚   â”œâ”€â”€ l298n_right [Right Dual H-Bridge Driver, Finned Heatsink, Terminals, Caps]
â”‚   â”œâ”€â”€ power_converter [Dual-Inductor SEPIC Module, 2x 33uH Inductors, Trimpot, Caps]
â”‚   â”œâ”€â”€ battery_pack [4-Cell Molded Battery Holder Tray with Cells & Springs]
â”‚   â”œâ”€â”€ wiring_harness [Color-Coded Dupont Jumpers & High-Current Power Cables]
â”‚   â””â”€â”€ rear_brake_lights [Dual Red Brake Light Lenses with Emissive Core]
â”œâ”€â”€ front_left_wheel_pivot [Local Origin at Axle Center: (-0.076, 0.065, 0.033)]
â”‚   â””â”€â”€ [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub, Axle Nut]
â”œâ”€â”€ front_right_wheel_pivot [Local Origin at Axle Center: (+0.076, 0.065, 0.033)]
â”‚   â””â”€â”€ [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub, Axle Nut]
â”œâ”€â”€ rear_left_wheel_pivot [Local Origin at Axle Center: (-0.076, -0.065, 0.033)]
â”‚   â””â”€â”€ [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub, Axle Nut]
â””â”€â”€ rear_right_wheel_pivot [Local Origin at Axle Center: (+0.076, -0.065, 0.033)]
    â””â”€â”€ [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub, Axle Nut]
```

### Wheel Animation Rules
1. Each wheel pivot's local origin is situated exactly at the center of its axle $(0, 0, 0)$.
2. In glTF coordinate space (Y-up, -Z forward), rolling the vehicle forward requires rotating the wheel pivots about their local $X$-axis.
3. Left and right wheels are independently addressable, enabling realistic differential steering (opposite or differential rotation speeds during turns and pivot spins).

### Chassis Dynamics Rules
1. Rotating `chassis_root` in pitch simulates rear squat under acceleration and front dive under braking.
2. Rotating `chassis_root` in roll simulates body lean during high-speed cornering.
3. Because wheel pivots are direct children of `car_root`, pitching or rolling the chassis does not displace wheels off the ground plane.

---

## 4. Materials Palette

| Material Name | Base Color (RGBA) | Metallic | Roughness | Transmission / Emission | Description |
|---|---|---|---|---|---|
| `Mat_AcrylicYellow` | $(1.0, 0.78, 0.05, 0.85)$ | 0.05 | 0.18 | Trans: 0.55 | Dual-deck laser cut acrylic plates |
| `Mat_TireRubber` | $(0.10, 0.10, 0.11, 1.0)$ | 0.02 | 0.82 | None | Black natural rubber tire with treads |
| `Mat_WheelRimYellow` | $(1.0, 0.82, 0.01, 1.0)$ | 0.05 | 0.28 | None | Vivid yellow ABS 5-spoke wheel rim |
| `Mat_MotorYellow` | $(0.96, 0.74, 0.04, 1.0)$ | 0.02 | 0.35 | None | TT DC gear motor yellow plastic housing |
| `Mat_MotorSteel` | $(0.82, 0.84, 0.88, 1.0)$ | 0.95 | 0.22 | None | Brushed steel DC canister & fasteners |
| `Mat_BrassMetal` | $(0.92, 0.76, 0.32, 1.0)$ | 0.90 | 0.25 | None | M3 brass hexagonal standoffs |
| `Mat_PcbRed` | $(0.78, 0.10, 0.12, 1.0)$ | 0.08 | 0.38 | None | L298N red solder mask FR-4 PCB |
| `Mat_PcbBlack` | $(0.10, 0.10, 0.11, 1.0)$ | 0.10 | 0.45 | None | ESP32 matte black FR-4 PCB |
| `Mat_PcbBlue` | $(0.06, 0.22, 0.58, 1.0)$ | 0.08 | 0.40 | None | DC-DC converter deep blue PCB |
| `Mat_HeatsinkBlack` | $(0.14, 0.14, 0.15, 1.0)$ | 0.75 | 0.32 | None | Black anodized aluminum cooling fins |
| `Mat_RfShieldNickel` | $(0.88, 0.88, 0.90, 1.0)$ | 0.92 | 0.20 | None | ESP32-WROOM-32 nickel RF shield |
| `Mat_InductorShielded`| $(0.12, 0.13, 0.15, 1.0)$ | 0.30 | 0.45 | None | Dual shielded power inductors ("330") |
| `Mat_CapSilver` | $(0.75, 0.77, 0.80, 1.0)$ | 0.88 | 0.25 | None | Radial electrolytic capacitor tops |
| `Mat_BatteryTrayBlack`| $(0.06, 0.06, 0.07, 1.0)$ | 0.05 | 0.55 | None | 4-cell molded ABS battery tray |
| `Mat_BatteryCellBlue` | $(0.08, 0.35, 0.85, 1.0)$ | 0.10 | 0.30 | None | Cylindrical battery cells |
| `Mat_EncoderDiscBlack`| $(0.08, 0.08, 0.09, 1.0)$ | 0.05 | 0.65 | None | 20-slot optical speed encoder discs |
| `Mat_TerminalGreen` | $(0.12, 0.58, 0.24, 1.0)$ | 0.02 | 0.40 | None | 5.08 mm screw terminal blocks |
| `Mat_BrakeLightRed` | $(0.95, 0.05, 0.05, 1.0)$ | 0.00 | 0.15 | Emissive: 5.0 | Rear brake light illumination |
| `Mat_StatusLedBlue` | $(0.10, 0.45, 1.0, 1.0)$ | 0.00 | 0.20 | Emissive: 6.0 | Prakti ESP32 GPIO 2 status indicator |

---

## 5. Rendered Inspection Views

All 7 preview images are rendered directly from Blender 5.2.0 and saved to `docs/model/previews/`:

1. `rear_three_quarter.png` â€” Rear 3/4 perspective showing wheel treads, heatsinks, and wiring.
2. `front_three_quarter.png` â€” Front 3/4 perspective showing ESP32 antenna, front wheels, and chassis bumper contour.
3. `top_view.png` â€” Plan view showing full electronics layout, standoff alignment, and dual L298N placement.
4. `electronics_closeup.png` â€” High-magnification shot of ESP32, dual inductors, trimpot, and L298N terminals.
5. `side_view.png` â€” Side elevation showing ground clearance, deck separation, axle alignment, and tire clearance.
6. `app_camera_view.png` â€” View matching the Android controller camera framing (40Â° FOV, rear-upper chase).
7. `exploded_view.png` â€” Exploded assembly view revealing mechanical layering and component relationships.

---

## 6. Regeneration Procedure

To rebuild the master Blender project, re-render all 7 review angles, and re-export the binary GLB asset:

```powershell
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" --background --python tools/modeling/generate_robo_racer_model.py
```
