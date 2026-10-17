# Robo Racer 4WD 3D Asset Specification & Hierarchy

This document provides the technical specification, node hierarchy, coordinate conventions, material assignments, and animation integration requirements for the Robo Racer 4WD vehicle model.

---

## 1. Asset Overview

- **Asset Path:** `app/src/main/assets/models/robo_racer_car.glb`
- **Master Blender File:** `tools/modeling/robo_racer_car.blend`
- **Generator Script:** `tools/modeling/generate_robo_racer_model.py`
- **Format:** glTF 2.0 Binary (`.glb`)
- **Total Asset Size:** ~564 KB
- **Authoring Tool:** Blender 5.2.0 LTS
- **Polygon / Primitive Count:** 282 component meshes, 300 scene nodes
- **Material Count:** 22 physically based materials (PBR metallic-roughness, transmission, emissive)

---

## 2. Coordinate Conventions

### Blender Modeling Space (Z-Up)
- **Origin $(0, 0, 0)$:** Ground level at center of vehicle chassis.
- **$X$-Axis:** Left ($-X$) / Right ($+X$). Wheel track spacing: $\pm 0.076\,\text{m}$ ($\pm 76\,\text{mm}$).
- **$Y$-Axis:** Forward / Front ($+Y$) / Rear ($-Y$). Wheelbase: $\pm 0.065\,\text{m}$ ($\pm 65\,\text{mm}$, total $130\,\text{mm}$).
- **$Z$-Axis:** Ground ($0.0\,\text{m}$) / Up ($+Z$). Wheel axle height: $0.033\,\text{m}$ ($33\,\text{mm}$).

### glTF 2.0 Export Space (Y-Up, standard `export_yup=True`)
- **$+Y$:** World Up.
- **$-Z$:** World Forward (Vehicle Heading / Direction of Travel).
- **$+Z$:** World Rear (Toward App Chase Camera).
- **$+X$:** Vehicle Right.
- **$-X$:** Vehicle Left.

---

## 3. Node Hierarchy & Animation Interface

The model is organized into a clean scene graph designed specifically for real-time mobile animation:

```
car_root [Root Node: World Transform & Position]
├── chassis_root [Chassis Root: Dynamic Pitch, Squat, Dive & Roll]
│   ├── chassis_bottom_deck [Lower Acrylic Plate with Bumpers]
│   ├── chassis_top_deck [Upper Acrylic Plate with Bumpers]
│   ├── chassis_standoffs [6x M3 Brass Standoffs with Screws]
│   ├── motor_left_front [TT Motor Body, Gearbox, Canister, Bracket]
│   ├── motor_left_rear [TT Motor Body, Gearbox, Canister, Bracket]
│   ├── motor_right_front [TT Motor Body, Gearbox, Canister, Bracket]
│   ├── motor_right_rear [TT Motor Body, Gearbox, Canister, Bracket]
│   ├── esp32_board [Prakti 30-Pin ESP32, RF Shield, CP2102, Buttons, LEDs]
│   ├── l298n_left [Left Dual H-Bridge Driver, Finned Heatsink, Terminals, Caps]
│   ├── l298n_right [Right Dual H-Bridge Driver, Finned Heatsink, Terminals, Caps]
│   ├── power_converter [XL6019 Buck-Boost Module, Inductor, Trimpot, Terminals]
│   ├── battery_pack [3S 18650 Li-ion Pack in Blue PVC with Silicone Leads]
│   ├── wiring_harness [Color-coded Dupont Jumpers & Heavy Gauge Power Cables]
│   └── rear_brake_lights [Dual Red Brake Light Lenses with Emissive Core]
├── front_left_wheel_pivot [Local Origin at Axle Center: (-0.076, 0.065, 0.033)]
│   └── [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub]
├── front_right_wheel_pivot [Local Origin at Axle Center: (+0.076, 0.065, 0.033)]
│   └── [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub]
├── rear_left_wheel_pivot [Local Origin at Axle Center: (-0.076, -0.065, 0.033)]
│   └── [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub]
└── rear_right_wheel_pivot [Local Origin at Axle Center: (+0.076, -0.065, 0.033)]
    └── [Rubber Tire, 18 Chevron Treads, 5-Spoke Yellow Rim, Center Hub]
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
| `Mat_PcbBlue` | $(0.06, 0.22, 0.58, 1.0)$ | 0.08 | 0.40 | None | XL6019 deep blue solder mask PCB |
| `Mat_HeatsinkBlack` | $(0.14, 0.14, 0.15, 1.0)$ | 0.75 | 0.32 | None | Black anodized aluminum cooling fins |
| `Mat_RfShieldNickel` | $(0.88, 0.88, 0.90, 1.0)$ | 0.92 | 0.20 | None | ESP32-WROOM-32 nickel RF shield |
| `Mat_TerminalGreen` | $(0.12, 0.58, 0.24, 1.0)$ | 0.02 | 0.40 | None | 5.08 mm screw terminal blocks |
| `Mat_BatteryBluePvc`| $(0.04, 0.32, 0.82, 1.0)$ | 0.05 | 0.30 | None | 3S 18650 blue PVC heat-shrink wrap |
| `Mat_BrakeLightRed` | $(0.95, 0.05, 0.05, 1.0)$ | 0.00 | 0.15 | Emissive: 5.0 | Rear brake light illumination |
| `Mat_StatusLedBlue` | $(0.10, 0.45, 1.0, 1.0)$ | 0.00 | 0.20 | Emissive: 6.0 | Prakti ESP32 GPIO 2 status indicator |

---

## 5. Regeneration Procedure

To rebuild the master Blender project, re-render all review angles, and re-export the binary GLB asset:

```powershell
& "C:\Program Files\Blender Foundation\Blender 5.2\blender.exe" --background --python tools/modeling/generate_robo_racer_model.py
```
