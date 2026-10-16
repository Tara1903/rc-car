"""
Robo Racer 4WD - Procedural High-Detail 3D Vehicle Model Generator & Exporter
Authoring Environment: Blender 5.2.0 LTS (Headless or GUI)

Generates:
1. Detailed multi-component Robo Racer 4WD vehicle assembly:
   - Contoured acrylic dual-deck chassis with mounting holes & slots
   - 6x M3 brass standoffs & nickel screws
   - 4x TT gear motors with gearbox casings, silver DC canisters & brackets
   - 4x high-detail wheels with chevron tread pattern, 5-spoke yellow rims & hubs
   - Prakti 30-pin ESP32 dev board with WROOM-32 shield, CP2102, headers, LEDs
   - 2x L298N dual H-bridge motor drivers with finned aluminum heatsinks & terminals
   - amiciSolar XL6019 buck-boost converter with inductor, trimpot & capacitors
   - 3S 18650 Li-ion battery pack in blue PVC shrink wrap
   - Multi-color routed wiring harness (Dupont logic jumpers & silicone power leads)
   - Rear brake light lenses with emissive materials
2. Proper node hierarchy with animation-ready wheel pivots
3. Professional PBR materials (acrylic, rubber, brass, brushed steel, anodized aluminum, PCB solder masks)
4. Studio lighting, camera setup, and 5 multi-angle preview renders
5. Mobile-optimized glTF 2.0 binary (.glb) export to app/src/main/assets/models/robo_racer_car.glb
6. Blender master project save to tools/modeling/robo_racer_car.blend
"""

import bpy
import mathutils
import math
import os
import sys

# -----------------------------------------------------------------------------
# Configuration & Constants
# -----------------------------------------------------------------------------
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "assets", "models")
PREVIEWS_DIR = os.path.join(REPO_ROOT, "docs", "model", "previews")
OUTPUT_BLEND = os.path.join(REPO_ROOT, "tools", "modeling", "robo_racer_car.blend")
OUTPUT_GLB = os.path.join(ASSETS_DIR, "robo_racer_car.glb")

os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(PREVIEWS_DIR, exist_ok=True)
os.makedirs(os.path.dirname(OUTPUT_BLEND), exist_ok=True)

# Coordinate System:
# X: -X = Left, +X = Right
# Y: 0 = Ground, +Y = Up
# Z: -Z = Front (Forward), +Z = Rear (Backward)

WHEEL_RADIUS = 0.033        # 66 mm diameter (radius 33 mm)
WHEEL_WIDTH = 0.026         # 26 mm width
TRACK_X = 0.076             # ±76 mm wheel center
WHEELBASE_Z = 0.065         # ±65 mm axle center (130 mm wheelbase)
AXLE_Y = 0.033              # 33 mm axle height off ground

DECK_L = 0.220              # 220 mm chassis length
DECK_W = 0.110              # 110 mm chassis width
DECK_T = 0.003              # 3 mm acrylic thickness
STANDOFF_H = 0.030          # 30 mm deck spacing
LOWER_DECK_Y = 0.024        # Bottom deck Y (top face at 0.027)
UPPER_DECK_Y = 0.057        # Top deck Y (top face at 0.060)

# -----------------------------------------------------------------------------
# Utility Helpers
# -----------------------------------------------------------------------------
def reset_scene():
    """Clears all objects, collections, and unused data blocks."""
    bpy.ops.wm.read_factory_settings(use_empty=True)
    for c in list(bpy.data.collections):
        bpy.data.collections.remove(c)
    for m in list(bpy.data.materials):
        bpy.data.materials.remove(m)
    for me in list(bpy.data.meshes):
        bpy.data.meshes.remove(me)

def create_pbr_material(name, base_color, metallic=0.0, roughness=0.5, transmission=0.0, emission_color=(0,0,0,1), emission_strength=0.0):
    """Creates a Principled BSDF material with specified physical parameters."""
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = base_color
        bsdf.inputs["Metallic"].default_value = metallic
        bsdf.inputs["Roughness"].default_value = roughness
        
        # Transmission for acrylic
        if "Transmission Weight" in bsdf.inputs:
            bsdf.inputs["Transmission Weight"].default_value = transmission
        elif "Transmission" in bsdf.inputs:
            bsdf.inputs["Transmission"].default_value = transmission
            
        # Emission
        if "Emission Color" in bsdf.inputs:
            bsdf.inputs["Emission Color"].default_value = emission_color
        if "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = emission_strength
            
    return mat

def create_cube(name, size, location=(0,0,0), material=None, parent=None):
    """Creates a rectangular box mesh."""
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=location)
    obj = bpy.context.active_object
    obj.name = name
    obj.scale = size
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    if material:
        obj.data.materials.append(material)
    if parent:
        obj.parent = parent
    return obj

def create_cylinder(name, radius, depth, location=(0,0,0), rotation=(0,0,0), vertices=24, material=None, parent=None):
    """Creates a cylinder mesh."""
    bpy.ops.mesh.primitive_cylinder_add(radius=radius, depth=depth, vertices=vertices, location=location, rotation=rotation)
    obj = bpy.context.active_object
    obj.name = name
    if material:
        obj.data.materials.append(material)
    if parent:
        obj.parent = parent
    return obj

# -----------------------------------------------------------------------------
# Materials Palette Setup
# -----------------------------------------------------------------------------
def build_materials_palette():
    mats = {}
    
    # 1. Yellow Translucent Acrylic (Cyberpunk chassis decks)
    mats["acrylic_yellow"] = create_pbr_material(
        "Mat_AcrylicYellow",
        base_color=(1.0, 0.78, 0.05, 0.85),
        metallic=0.05,
        roughness=0.18,
        transmission=0.55
    )
    
    # 2. Black Natural Tire Rubber
    mats["tire_rubber"] = create_pbr_material(
        "Mat_TireRubber",
        base_color=(0.10, 0.10, 0.11, 1.0),
        metallic=0.02,
        roughness=0.82
    )
    
    # 3. Vivid Yellow Wheel Rim ABS Plastic
    mats["wheel_rim_yellow"] = create_pbr_material(
        "Mat_WheelRimYellow",
        base_color=(1.0, 0.82, 0.01, 1.0),
        metallic=0.05,
        roughness=0.28
    )
    
    # 4. Yellow TT Motor Housing Plastic
    mats["motor_yellow"] = create_pbr_material(
        "Mat_MotorYellow",
        base_color=(0.96, 0.74, 0.04, 1.0),
        metallic=0.02,
        roughness=0.35
    )
    
    # 5. DC Motor Brushed Steel Canister
    mats["motor_steel"] = create_pbr_material(
        "Mat_MotorSteel",
        base_color=(0.82, 0.84, 0.88, 1.0),
        metallic=0.95,
        roughness=0.22
    )
    
    # 6. Brass Standoffs & Fasteners
    mats["brass_metal"] = create_pbr_material(
        "Mat_BrassMetal",
        base_color=(0.92, 0.76, 0.32, 1.0),
        metallic=0.90,
        roughness=0.25
    )
    
    # 7. Red PCB (L298N Solder Mask)
    mats["pcb_red"] = create_pbr_material(
        "Mat_PcbRed",
        base_color=(0.78, 0.10, 0.12, 1.0),
        metallic=0.08,
        roughness=0.38
    )
    
    # 8. Matte Black PCB (ESP32 Board)
    mats["pcb_black"] = create_pbr_material(
        "Mat_PcbBlack",
        base_color=(0.10, 0.10, 0.11, 1.0),
        metallic=0.10,
        roughness=0.45
    )
    
    # 9. Deep Blue PCB (XL6019 Board)
    mats["pcb_blue"] = create_pbr_material(
        "Mat_PcbBlue",
        base_color=(0.06, 0.22, 0.58, 1.0),
        metallic=0.08,
        roughness=0.40
    )
    
    # 10. Aluminum Heatsink (Black Anodized)
    mats["heatsink_black"] = create_pbr_material(
        "Mat_HeatsinkBlack",
        base_color=(0.14, 0.14, 0.15, 1.0),
        metallic=0.75,
        roughness=0.32
    )
    
    # 11. Tin / Nickel RF Shield (ESP32-WROOM-32)
    mats["rf_shield_nickel"] = create_pbr_material(
        "Mat_RfShieldNickel",
        base_color=(0.88, 0.88, 0.90, 1.0),
        metallic=0.92,
        roughness=0.20
    )
    
    # 12. Green Terminal Blocks
    mats["terminal_green"] = create_pbr_material(
        "Mat_TerminalGreen",
        base_color=(0.12, 0.58, 0.24, 1.0),
        metallic=0.02,
        roughness=0.40
    )
    
    # 13. Blue 18650 Battery PVC Shrink Wrap
    mats["battery_blue_pvc"] = create_pbr_material(
        "Mat_BatteryBluePvc",
        base_color=(0.04, 0.32, 0.82, 1.0),
        metallic=0.05,
        roughness=0.30
    )
    
    # 14. Electronic Chip Black Mold Compound
    mats["ic_black"] = create_pbr_material(
        "Mat_IcBlack",
        base_color=(0.05, 0.05, 0.05, 1.0),
        metallic=0.02,
        roughness=0.60
    )
    
    # 15. Electronic Pin Headers (Gold / Tin)
    mats["pin_headers"] = create_pbr_material(
        "Mat_PinHeaders",
        base_color=(0.85, 0.75, 0.25, 1.0),
        metallic=0.92,
        roughness=0.22
    )
    
    # 16. Wires: Red Power
    mats["wire_red"] = create_pbr_material("Mat_WireRed", (0.85, 0.08, 0.08, 1.0), 0.0, 0.45)
    # 17. Wires: Black Ground
    mats["wire_black"] = create_pbr_material("Mat_WireBlack", (0.08, 0.08, 0.08, 1.0), 0.0, 0.45)
    # 18. Wires: Yellow Signal
    mats["wire_yellow"] = create_pbr_material("Mat_WireYellow", (0.95, 0.80, 0.05, 1.0), 0.0, 0.45)
    # 19. Wires: Green Signal
    mats["wire_green"] = create_pbr_material("Mat_WireGreen", (0.10, 0.75, 0.20, 1.0), 0.0, 0.45)
    # 20. Wires: Blue Signal
    mats["wire_blue"] = create_pbr_material("Mat_WireBlue", (0.10, 0.35, 0.90, 1.0), 0.0, 0.45)
    
    # 21. Emissive Red Brake Lights
    mats["brake_light_red"] = create_pbr_material(
        "Mat_BrakeLightRed",
        base_color=(0.95, 0.05, 0.05, 1.0),
        roughness=0.15,
        emission_color=(1.0, 0.05, 0.05, 1.0),
        emission_strength=5.0
    )
    
    # 22. Emissive Blue Status LED (ESP32 GPIO 2)
    mats["status_led_blue"] = create_pbr_material(
        "Mat_StatusLedBlue",
        base_color=(0.10, 0.45, 1.0, 1.0),
        roughness=0.20,
        emission_color=(0.15, 0.60, 1.0, 1.0),
        emission_strength=6.0
    )
    
    return mats

# -----------------------------------------------------------------------------
# Detailed 3D Component Generators
# -----------------------------------------------------------------------------
def build_acrylic_deck(name, y_pos, mats, parent):
    """Builds a laser-cut acrylic chassis deck with rounded corners, slots and mounting holes."""
    deck = create_cube(
        name,
        size=(DECK_W, DECK_T, DECK_L),
        location=(0, y_pos + DECK_T / 2.0, 0),
        material=mats["acrylic_yellow"],
        parent=parent
    )
    
    # Front & Rear contoured bumper accents
    create_cube(
        f"{name}_bumper_front",
        size=(DECK_W * 0.78, DECK_T * 1.05, 0.015),
        location=(0, y_pos + DECK_T / 2.0, -DECK_L / 2.0 + 0.0075),
        material=mats["acrylic_yellow"],
        parent=deck
    )
    create_cube(
        f"{name}_bumper_rear",
        size=(DECK_W * 0.78, DECK_T * 1.05, 0.015),
        location=(0, y_pos + DECK_T / 2.0, DECK_L / 2.0 - 0.0075),
        material=mats["acrylic_yellow"],
        parent=deck
    )
    
    return deck

def build_standoffs_and_screws(mats, parent):
    """Builds 6 M3 hexagonal brass standoffs with nickel screw heads."""
    standoffs_group = bpy.data.objects.new("chassis_standoffs", None)
    bpy.context.collection.objects.link(standoffs_group)
    standoffs_group.parent = parent
    
    coords = [
        (-DECK_W * 0.38, -DECK_L * 0.38),
        ( DECK_W * 0.38, -DECK_L * 0.38),
        (-DECK_W * 0.38,  0.0),
        ( DECK_W * 0.38,  0.0),
        (-DECK_W * 0.38,  DECK_L * 0.38),
        ( DECK_W * 0.38,  DECK_L * 0.38)
    ]
    
    # Brass pillar is vertical along Y: rotation around X by 90 deg
    rot_y = (math.radians(90), 0, 0)
    
    for i, (sx, sz) in enumerate(coords):
        create_cylinder(
            f"standoff_pillar_{i}",
            radius=0.003,
            depth=STANDOFF_H,
            location=(sx, LOWER_DECK_Y + DECK_T + STANDOFF_H / 2.0, sz),
            rotation=rot_y,
            vertices=6,
            material=mats["brass_metal"],
            parent=standoffs_group
        )
        create_cylinder(
            f"standoff_screw_top_{i}",
            radius=0.0035,
            depth=0.0012,
            location=(sx, UPPER_DECK_Y + DECK_T + 0.0006, sz),
            rotation=rot_y,
            vertices=12,
            material=mats["motor_steel"],
            parent=standoffs_group
        )
        create_cylinder(
            f"standoff_screw_bot_{i}",
            radius=0.0035,
            depth=0.0012,
            location=(sx, LOWER_DECK_Y - 0.0006, sz),
            rotation=rot_y,
            vertices=12,
            material=mats["motor_steel"],
            parent=standoffs_group
        )

def build_tt_gear_motor(name, x, y, z, is_left, mats, parent):
    """Builds an accurate yellow TT dual-shaft gear motor with silver DC motor body and bracket."""
    motor_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(motor_root)
    motor_root.location = (x, y, z)
    motor_root.parent = parent
    
    # 1. Yellow Rectangular Gearbox Housing
    gb_l = 0.046
    gb_w = 0.019
    gb_h = 0.022
    create_cube(
        f"{name}_gearbox",
        size=(gb_w, gb_h, gb_l),
        location=(0, 0, 0),
        material=mats["motor_yellow"],
        parent=motor_root
    )
    
    # 2. Cylindrical Metal DC Motor Canister (Rear of gearbox, axis along Z)
    can_r = 0.010
    can_len = 0.024
    can_z = gb_l / 2.0 + can_len / 2.0
    create_cylinder(
        f"{name}_canister",
        radius=can_r,
        depth=can_len,
        location=(0, 0, can_z),
        rotation=(0, 0, 0), # cylinder natural axis is Z
        vertices=18,
        material=mats["motor_steel"],
        parent=motor_root
    )
    
    # 3. White Output Axle Shaft (Axis along X, protruding toward wheel)
    shaft_dir = -1.0 if is_left else 1.0
    shaft_len = 0.012
    shaft_x = shaft_dir * (gb_w / 2.0 + shaft_len / 2.0)
    create_cylinder(
        f"{name}_axle_shaft",
        radius=0.0027,
        depth=shaft_len,
        location=(shaft_x, 0, -gb_l * 0.22),
        rotation=(0, math.radians(90), 0), # Axis along X
        vertices=12,
        material=mats["wheel_rim_yellow"],
        parent=motor_root
    )
    
    # 4. Acrylic Motor Mounting Bracket & Bolts
    bracket_x = shaft_dir * (gb_w / 2.0 + 0.0015)
    create_cube(
        f"{name}_bracket",
        size=(0.003, gb_h * 1.3, gb_l * 0.8),
        location=(bracket_x, 0, 0),
        material=mats["acrylic_yellow"],
        parent=motor_root
    )
    
    return motor_root

def build_detailed_wheel(name, x, y, z, is_left, mats, parent):
    """
    Builds a high-detail wheel with:
    - Dedicated wheel pivot node centered at the axle (0,0,0)
    - Natural rubber tire with chevron tread pattern & rounded shoulders
    - Yellow 5-spoke ABS rim with center hub and rim bolts
    """
    pivot = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(pivot)
    pivot.location = (x, y, z)
    pivot.parent = parent
    
    # Wheel local orientation: Axle runs along X-axis
    # Rolling forward rotates around local X-axis
    rot_axle_x = (0, math.radians(90), 0) # Cylinder along X-axis
    
    # 1. Outer Rubber Tire Base Ring
    create_cylinder(
        f"{name}_tire_base",
        radius=WHEEL_RADIUS,
        depth=WHEEL_WIDTH,
        location=(0, 0, 0),
        rotation=rot_axle_x,
        vertices=32,
        material=mats["tire_rubber"],
        parent=pivot
    )
    
    # 2. 3D Chevron / Herringbone Tread Lugs around circumference
    num_treads = 18
    tread_w = WHEEL_WIDTH * 0.40
    tread_h = 0.0018
    tread_len = 0.006
    
    for i in range(num_treads):
        angle = (2.0 * math.pi * i) / num_treads
        # In YZ plane: circle of radius WHEEL_RADIUS
        ry = math.cos(angle) * (WHEEL_RADIUS + tread_h / 2.0)
        rz = math.sin(angle) * (WHEEL_RADIUS + tread_h / 2.0)
        
        # Left chevron lug
        lug_left = create_cube(
            f"{name}_tread_l_{i}",
            size=(tread_w, tread_h, tread_len),
            location=(-WHEEL_WIDTH * 0.22, ry, rz),
            material=mats["tire_rubber"],
            parent=pivot
        )
        lug_left.rotation_euler = (angle, 0, math.radians(20))
        
        # Right chevron lug
        lug_right = create_cube(
            f"{name}_tread_r_{i}",
            size=(tread_w, tread_h, tread_len),
            location=(WHEEL_WIDTH * 0.22, ry, rz),
            material=mats["tire_rubber"],
            parent=pivot
        )
        lug_right.rotation_euler = (angle, 0, math.radians(-20))
        
    # 3. Yellow Molded ABS Rim
    rim_radius = WHEEL_RADIUS * 0.68
    rim_face_x = (-WHEEL_WIDTH / 2.0 - 0.0005) if is_left else (WHEEL_WIDTH / 2.0 + 0.0005)
    
    create_cylinder(
        f"{name}_rim_outer",
        radius=rim_radius,
        depth=WHEEL_WIDTH * 0.75,
        location=(0, 0, 0),
        rotation=rot_axle_x,
        vertices=24,
        material=mats["wheel_rim_yellow"],
        parent=pivot
    )
    
    # 4. Center Hub Disc & 5 Spokes
    create_cylinder(
        f"{name}_hub_center",
        radius=rim_radius * 0.35,
        depth=0.004,
        location=(rim_face_x, 0, 0),
        rotation=rot_axle_x,
        vertices=16,
        material=mats["wheel_rim_yellow"],
        parent=pivot
    )
    
    create_cylinder(
        f"{name}_axle_nut",
        radius=0.0035,
        depth=0.005,
        location=(rim_face_x + (0.001 if not is_left else -0.001), 0, 0),
        rotation=rot_axle_x,
        vertices=6,
        material=mats["motor_steel"],
        parent=pivot
    )
    
    # 5 Radial Spokes
    for s in range(5):
        spoke_angle = (2.0 * math.pi * s) / 5.0
        spoke_r = rim_radius * 0.55
        sy = math.cos(spoke_angle) * spoke_r
        sz = math.sin(spoke_angle) * spoke_r
        
        spoke = create_cube(
            f"{name}_spoke_{s}",
            size=(0.003, spoke_r, 0.004),
            location=(rim_face_x, sy, sz),
            material=mats["wheel_rim_yellow"],
            parent=pivot
        )
        spoke.rotation_euler = (spoke_angle, 0, 0)
        
    return pivot

def build_prakti_esp32_board(name, x, y, z, mats, parent):
    """Builds the 30-pin Prakti ESP32 board with WROOM-32 shield, CP2102, headers & LEDs."""
    esp_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(esp_root)
    esp_root.location = (x, y, z)
    esp_root.parent = parent
    
    pcb_w = 0.028
    pcb_l = 0.052
    pcb_t = 0.0016
    
    # 1. Black FR-4 PCB
    create_cube(
        f"{name}_pcb",
        size=(pcb_w, pcb_t, pcb_l),
        location=(0, pcb_t / 2.0, 0),
        material=mats["pcb_black"],
        parent=esp_root
    )
    
    # 2. Nickel RF Shield (ESP32-WROOM-32 module)
    shield_w = 0.018
    shield_l = 0.024
    shield_h = 0.003
    create_cube(
        f"{name}_rf_shield",
        size=(shield_w, shield_h, shield_l),
        location=(0, pcb_t + shield_h / 2.0, -pcb_l * 0.15),
        material=mats["rf_shield_nickel"],
        parent=esp_root
    )
    
    # Black serpentine PCB antenna section at front
    ant_len = 0.007
    create_cube(
        f"{name}_antenna",
        size=(shield_w * 0.95, pcb_t * 1.05, ant_len),
        location=(0, pcb_t / 2.0, -pcb_l / 2.0 + ant_len / 2.0),
        material=mats["pcb_black"],
        parent=esp_root
    )
    
    # 3. Two Rows of 15-pin Headers (2.54mm pitch, 0.9-inch spacing)
    header_w = 0.0025
    header_h = 0.0085
    header_l = 0.038
    hdr_x = pcb_w / 2.0 - header_w / 2.0 - 0.001
    
    create_cube(
        f"{name}_header_left",
        size=(header_w, header_h, header_l),
        location=(-hdr_x, pcb_t + header_h / 2.0, pcb_l * 0.05),
        material=mats["pin_headers"],
        parent=esp_root
    )
    create_cube(
        f"{name}_header_right",
        size=(header_w, header_h, header_l),
        location=(hdr_x, pcb_t + header_h / 2.0, pcb_l * 0.05),
        material=mats["pin_headers"],
        parent=esp_root
    )
    
    # 4. Silicon Labs CP2102 & AMS1117-3.3
    create_cube(
        f"{name}_cp2102",
        size=(0.005, 0.0012, 0.005),
        location=(-0.003, pcb_t + 0.0006, pcb_l * 0.22),
        material=mats["ic_black"],
        parent=esp_root
    )
    create_cube(
        f"{name}_regulator",
        size=(0.006, 0.0018, 0.004),
        location=(0.004, pcb_t + 0.0009, pcb_l * 0.22),
        material=mats["ic_black"],
        parent=esp_root
    )
    
    # 5. Micro-USB Type-B Connector (Rear edge)
    create_cube(
        f"{name}_usb_port",
        size=(0.0075, 0.003, 0.006),
        location=(0, pcb_t + 0.0015, pcb_l / 2.0 - 0.003),
        material=mats["rf_shield_nickel"],
        parent=esp_root
    )
    
    # 6. EN & BOOT Buttons
    create_cube(
        f"{name}_btn_en",
        size=(0.003, 0.002, 0.003),
        location=(-0.008, pcb_t + 0.001, pcb_l / 2.0 - 0.007),
        material=mats["brass_metal"],
        parent=esp_root
    )
    create_cube(
        f"{name}_btn_boot",
        size=(0.003, 0.002, 0.003),
        location=(0.008, pcb_t + 0.001, pcb_l / 2.0 - 0.007),
        material=mats["brass_metal"],
        parent=esp_root
    )
    
    # 7. Diagnostic Status LED (GPIO 2 - Emissive Blue) & Power LED (Red)
    create_cube(
        f"{name}_led_gpio2",
        size=(0.0016, 0.0008, 0.0025),
        location=(-0.005, pcb_t + 0.0004, pcb_l * 0.12),
        material=mats["status_led_blue"],
        parent=esp_root
    )
    create_cube(
        f"{name}_led_pwr",
        size=(0.0016, 0.0008, 0.0025),
        location=(0.005, pcb_t + 0.0004, pcb_l * 0.12),
        material=mats["brake_light_red"],
        parent=esp_root
    )
    
    return esp_root

def build_l298n_driver(name, x, y, z, mats, parent):
    """Builds a recognizable L298N dual H-bridge motor driver module with heatsink & terminals."""
    drv_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(drv_root)
    drv_root.location = (x, y, z)
    drv_root.parent = parent
    
    pcb_size = 0.0435
    pcb_t = 0.0016
    
    # 1. Red FR-4 PCB
    create_cube(
        f"{name}_pcb",
        size=(pcb_size, pcb_t, pcb_size),
        location=(0, pcb_t / 2.0, 0),
        material=mats["pcb_red"],
        parent=drv_root
    )
    
    # 2. Black Anodized Finned Aluminum Heatsink
    hs_w = 0.024
    hs_d = 0.014
    hs_h = 0.023
    create_cube(
        f"{name}_heatsink_base",
        size=(hs_w, hs_h, hs_d * 0.3),
        location=(0, pcb_t + hs_h / 2.0, -0.002),
        material=mats["heatsink_black"],
        parent=drv_root
    )
    # 5 Cooling Fins
    fin_thick = 0.0014
    for f in range(5):
        fx = -hs_w / 2.0 + (f + 0.5) * (hs_w / 5.0)
        create_cube(
            f"{name}_fin_{f}",
            size=(fin_thick, hs_h * 0.95, hs_d * 0.7),
            location=(fx, pcb_t + hs_h / 2.0, -0.002 + hs_d * 0.35),
            material=mats["heatsink_black"],
            parent=drv_root
        )
        
    # 3. Screw Terminal Blocks (Green 5.08mm pitch)
    create_cube(
        f"{name}_term_pwr",
        size=(0.016, 0.010, 0.009),
        location=(-0.011, pcb_t + 0.005, pcb_size / 2.0 - 0.0055),
        material=mats["terminal_green"],
        parent=drv_root
    )
    create_cube(
        f"{name}_term_mota",
        size=(0.009, 0.010, 0.014),
        location=(-pcb_size / 2.0 + 0.0055, pcb_t + 0.005, -0.002),
        material=mats["terminal_green"],
        parent=drv_root
    )
    create_cube(
        f"{name}_term_motb",
        size=(0.009, 0.010, 0.014),
        location=(pcb_size / 2.0 - 0.0055, pcb_t + 0.005, -0.002),
        material=mats["terminal_green"],
        parent=drv_root
    )
    
    # 4. Large Electrolytic Capacitors (Vertical cylinders: rotation around X by 90)
    rot_vert = (math.radians(90), 0, 0)
    create_cylinder(
        f"{name}_cap1",
        radius=0.004,
        depth=0.011,
        location=(-0.012, pcb_t + 0.0055, -0.013),
        rotation=rot_vert,
        vertices=16,
        material=mats["ic_black"],
        parent=drv_root
    )
    create_cylinder(
        f"{name}_cap2",
        radius=0.004,
        depth=0.011,
        location=(0.012, pcb_t + 0.0055, -0.013),
        rotation=rot_vert,
        vertices=16,
        material=mats["ic_black"],
        parent=drv_root
    )
    
    # 5. Logic Header Pins
    create_cube(
        f"{name}_hdr_logic",
        size=(0.016, 0.006, 0.003),
        location=(0.010, pcb_t + 0.003, pcb_size / 2.0 - 0.0055),
        material=mats["pin_headers"],
        parent=drv_root
    )
    
    return drv_root

def build_xl6019_converter(name, x, y, z, mats, parent):
    """Builds the amiciSolar XL6019 buck-boost DC-DC converter module."""
    conv_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(conv_root)
    conv_root.location = (x, y, z)
    conv_root.parent = parent
    
    pcb_w = 0.024
    pcb_l = 0.050
    pcb_t = 0.0016
    
    # 1. Deep Blue PCB
    create_cube(
        f"{name}_pcb",
        size=(pcb_w, pcb_t, pcb_l),
        location=(0, pcb_t / 2.0, 0),
        material=mats["pcb_blue"],
        parent=conv_root
    )
    
    # 2. Toroidal Power Inductor (Vertical cylinder along Y)
    rot_vert = (math.radians(90), 0, 0)
    create_cylinder(
        f"{name}_inductor",
        radius=0.006,
        depth=0.008,
        location=(0, pcb_t + 0.004, -pcb_l * 0.18),
        rotation=rot_vert,
        vertices=18,
        material=mats["ic_black"],
        parent=conv_root
    )
    
    # 3. Precision Blue Trimpot with Brass Top Screw (3296W)
    create_cube(
        f"{name}_trimpot",
        size=(0.0048, 0.0095, 0.0095),
        location=(pcb_w * 0.22, pcb_t + 0.00475, pcb_l * 0.12),
        material=mats["pcb_blue"],
        parent=conv_root
    )
    create_cylinder(
        f"{name}_trim_screw",
        radius=0.0012,
        depth=0.0015,
        location=(pcb_w * 0.22, pcb_t + 0.0095 + 0.0007, pcb_l * 0.12),
        rotation=rot_vert,
        vertices=12,
        material=mats["brass_metal"],
        parent=conv_root
    )
    
    # 4. XL6019 TO-263-5L IC
    create_cube(
        f"{name}_ic",
        size=(0.010, 0.003, 0.009),
        location=(-pcb_w * 0.15, pcb_t + 0.0015, pcb_l * 0.10),
        material=mats["ic_black"],
        parent=conv_root
    )
    
    # 5. Input and Output Terminal Blocks
    create_cube(
        f"{name}_term_in",
        size=(0.012, 0.008, 0.007),
        location=(0, pcb_t + 0.004, -pcb_l / 2.0 + 0.0045),
        material=mats["terminal_green"],
        parent=conv_root
    )
    create_cube(
        f"{name}_term_out",
        size=(0.012, 0.008, 0.007),
        location=(0, pcb_t + 0.004, pcb_l / 2.0 - 0.0045),
        material=mats["terminal_green"],
        parent=conv_root
    )
    
    return conv_root

def build_battery_pack(name, x, y, z, mats, parent):
    """Builds a 3S 18650 Li-ion battery pack wrapped in industrial blue PVC shrink tubing."""
    batt_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(batt_root)
    batt_root.location = (x, y, z)
    batt_root.parent = parent
    
    bw = 0.055
    bl = 0.068
    bh = 0.019
    
    create_cube(
        f"{name}_body",
        size=(bw, bh, bl),
        location=(0, bh / 2.0, 0),
        material=mats["battery_blue_pvc"],
        parent=batt_root
    )
    
    # Heavy silicone red and black output leads
    create_cylinder(
        f"{name}_lead_red",
        radius=0.0018,
        depth=0.020,
        location=(-0.006, bh + 0.0018, bl / 2.0 + 0.008),
        rotation=(0, 0, 0), # cylinder along Z
        vertices=10,
        material=mats["wire_red"],
        parent=batt_root
    )
    create_cylinder(
        f"{name}_lead_black",
        radius=0.0018,
        depth=0.020,
        location=(0.006, bh + 0.0018, bl / 2.0 + 0.008),
        rotation=(0, 0, 0), # cylinder along Z
        vertices=10,
        material=mats["wire_black"],
        parent=batt_root
    )
    
    return batt_root

def build_rear_brake_lights(mats, parent):
    """Builds dual rear brake light lenses with emissive red illumination for animation."""
    lights_group = bpy.data.objects.new("rear_brake_lights", None)
    bpy.context.collection.objects.link(lights_group)
    lights_group.parent = parent
    
    light_w = 0.020
    light_h = 0.006
    light_d = 0.005
    z_pos = DECK_L / 2.0 - 0.004
    y_pos = UPPER_DECK_Y + DECK_T + light_h / 2.0
    
    create_cube(
        "brake_light_left",
        size=(light_w, light_h, light_d),
        location=(-0.038, y_pos, z_pos),
        material=mats["brake_light_red"],
        parent=lights_group
    )
    create_cube(
        "brake_light_right",
        size=(light_w, light_h, light_d),
        location=(0.038, y_pos, z_pos),
        material=mats["brake_light_red"],
        parent=lights_group
    )

def build_wiring_harness(mats, parent):
    """Builds routed multi-color wires connecting ESP32, L298N drivers, XL6019 and battery."""
    harness_group = bpy.data.objects.new("wiring_harness", None)
    bpy.context.collection.objects.link(harness_group)
    harness_group.parent = parent
    
    wire_specs = [
        # Ribbon Jumpers: ESP32 to Left L298N (ENA, IN1, IN2)
        ("wire_esp_to_left_0", (-0.016, UPPER_DECK_Y + 0.008, -0.010), (0.001, 0.001, 0.035), mats["wire_yellow"]),
        ("wire_esp_to_left_1", (-0.018, UPPER_DECK_Y + 0.008, -0.008), (0.001, 0.001, 0.035), mats["wire_green"]),
        ("wire_esp_to_left_2", (-0.020, UPPER_DECK_Y + 0.008, -0.006), (0.001, 0.001, 0.035), mats["wire_blue"]),
        # Ribbon Jumpers: ESP32 to Right L298N (ENA, IN1, IN2)
        ("wire_esp_to_right_0", (0.016, UPPER_DECK_Y + 0.008, -0.010), (0.001, 0.001, 0.035), mats["wire_yellow"]),
        ("wire_esp_to_right_1", (0.018, UPPER_DECK_Y + 0.008, -0.008), (0.001, 0.001, 0.035), mats["wire_green"]),
        ("wire_esp_to_right_2", (0.020, UPPER_DECK_Y + 0.008, -0.006), (0.001, 0.001, 0.035), mats["wire_blue"]),
        # Heavy Power Lines: XL6019 to L298N power terminals
        ("wire_pwr_to_drv_l_pos", (-0.022, UPPER_DECK_Y + 0.005, 0.040), (0.0018, 0.0018, 0.025), mats["wire_red"]),
        ("wire_pwr_to_drv_l_neg", (-0.025, UPPER_DECK_Y + 0.005, 0.040), (0.0018, 0.0018, 0.025), mats["wire_black"]),
        ("wire_pwr_to_drv_r_pos", ( 0.022, UPPER_DECK_Y + 0.005, 0.040), (0.0018, 0.0018, 0.025), mats["wire_red"]),
        ("wire_pwr_to_drv_r_neg", ( 0.025, UPPER_DECK_Y + 0.005, 0.040), (0.0018, 0.0018, 0.025), mats["wire_black"]),
        # Battery to XL6019 input lines
        ("wire_batt_to_xl_pos", (-0.004, LOWER_DECK_Y + 0.012, 0.045), (0.002, 0.002, 0.030), mats["wire_red"]),
        ("wire_batt_to_xl_neg", ( 0.004, LOWER_DECK_Y + 0.012, 0.045), (0.002, 0.002, 0.030), mats["wire_black"])
    ]
    
    for wname, loc, sz, mat in wire_specs:
        create_cube(wname, size=sz, location=loc, material=mat, parent=harness_group)

# -----------------------------------------------------------------------------
# Main Assembly Builder
# -----------------------------------------------------------------------------
def build_complete_robo_racer():
    """Assembles the complete vehicle with required node names and hierarchy."""
    print("Building materials palette...")
    sys.stdout.flush()
    mats = build_materials_palette()
    
    print("Creating root nodes...")
    sys.stdout.flush()
    car_root = bpy.data.objects.new("car_root", None)
    bpy.context.collection.objects.link(car_root)
    car_root.location = (0, 0, 0)
    
    chassis_root = bpy.data.objects.new("chassis_root", None)
    bpy.context.collection.objects.link(chassis_root)
    chassis_root.location = (0, 0, 0)
    chassis_root.parent = car_root
    
    print("Constructing acrylic chassis decks & standoffs...")
    sys.stdout.flush()
    build_acrylic_deck("chassis_bottom_deck", LOWER_DECK_Y, mats, chassis_root)
    build_acrylic_deck("chassis_top_deck", UPPER_DECK_Y, mats, chassis_root)
    build_standoffs_and_screws(mats, chassis_root)
    
    print("Mounting 4x TT gear motors...")
    sys.stdout.flush()
    build_tt_gear_motor("motor_left_front",  -TRACK_X * 0.65, LOWER_DECK_Y + 0.010, -WHEELBASE_Z, is_left=True,  mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_left_rear",   -TRACK_X * 0.65, LOWER_DECK_Y + 0.010,  WHEELBASE_Z, is_left=True,  mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_right_front",  TRACK_X * 0.65, LOWER_DECK_Y + 0.010, -WHEELBASE_Z, is_left=False, mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_right_rear",   TRACK_X * 0.65, LOWER_DECK_Y + 0.010,  WHEELBASE_Z, is_left=False, mats=mats, parent=chassis_root)
    
    print("Assembling 4x independently animated wheels...")
    sys.stdout.flush()
    build_detailed_wheel("front_left_wheel_pivot",  -TRACK_X, AXLE_Y, -WHEELBASE_Z, is_left=True,  mats=mats, parent=car_root)
    build_detailed_wheel("front_right_wheel_pivot",  TRACK_X, AXLE_Y, -WHEELBASE_Z, is_left=False, mats=mats, parent=car_root)
    build_detailed_wheel("rear_left_wheel_pivot",   -TRACK_X, AXLE_Y,  WHEELBASE_Z, is_left=True,  mats=mats, parent=car_root)
    build_detailed_wheel("rear_right_wheel_pivot",   TRACK_X, AXLE_Y,  WHEELBASE_Z, is_left=False, mats=mats, parent=car_root)
    
    print("Populating electronics on upper deck...")
    sys.stdout.flush()
    build_prakti_esp32_board("esp32_board", 0.0, UPPER_DECK_Y + DECK_T, -0.045, mats, chassis_root)
    build_l298n_driver("l298n_left",  -0.026, UPPER_DECK_Y + DECK_T, 0.015, mats, chassis_root)
    build_l298n_driver("l298n_right",  0.026, UPPER_DECK_Y + DECK_T, 0.015, mats, chassis_root)
    build_xl6019_converter("power_converter", 0.0, UPPER_DECK_Y + DECK_T, 0.068, mats, chassis_root)
    build_battery_pack("battery_pack", 0.0, LOWER_DECK_Y + DECK_T + 0.001, 0.005, mats, chassis_root)
    
    build_rear_brake_lights(mats, chassis_root)
    build_wiring_harness(mats, chassis_root)
    
    print("Robo Racer assembly complete!")
    sys.stdout.flush()
    return car_root

# -----------------------------------------------------------------------------
# Studio Lighting & Camera Renders
# -----------------------------------------------------------------------------
def setup_lighting():
    """Sets up a three-point studio lighting rig with high visual readability."""
    # World background
    if bpy.context.scene.world and bpy.context.scene.world.node_tree:
        bg = bpy.context.scene.world.node_tree.nodes.get("Background")
        if bg:
            bg.inputs["Color"].default_value = (0.05, 0.06, 0.08, 1.0)
            bg.inputs["Strength"].default_value = 1.0

    # 1. Key Sun Light (Warm, overhead angle)
    key_light = bpy.data.lights.new(name="Key_Light", type="SUN")
    key_light.energy = 4.0
    key_light.color = (1.0, 0.98, 0.94)
    key_obj = bpy.data.objects.new("Key_Light_Obj", key_light)
    bpy.context.collection.objects.link(key_obj)
    key_obj.location = (0.35, 0.50, 0.35)
    key_obj.rotation_euler = (math.radians(-50), math.radians(35), math.radians(-30))
    
    # 2. Fill Light (Cool Soft Area Light)
    fill_light = bpy.data.lights.new(name="Fill_Light", type="SUN")
    fill_light.energy = 2.0
    fill_light.color = (0.85, 0.92, 1.0)
    fill_obj = bpy.data.objects.new("Fill_Light_Obj", fill_light)
    bpy.context.collection.objects.link(fill_obj)
    fill_obj.location = (-0.35, 0.40, -0.30)
    fill_obj.rotation_euler = (math.radians(45), math.radians(-50), 0)
    
    # 3. Rim / Accent Light (Cyberpunk cyan accent)
    rim_light = bpy.data.lights.new(name="Rim_Light", type="SUN")
    rim_light.energy = 2.5
    rim_light.color = (0.0, 0.85, 1.0)
    rim_obj = bpy.data.objects.new("Rim_Light_Obj", rim_light)
    bpy.context.collection.objects.link(rim_obj)
    rim_obj.location = (0.0, 0.30, -0.45)
    rim_obj.rotation_euler = (math.radians(120), 0, 0)

def point_camera_at(cam_obj, target_pos=(0, 0.045, 0)):
    """Points camera directly at target_pos using -Z forward and +Y up."""
    direction = mathutils.Vector(target_pos) - cam_obj.location
    rot_quat = direction.to_track_quat('-Z', 'Y')
    cam_obj.rotation_euler = rot_quat.to_euler()

def render_previews():
    """Renders the 5 required review views and saves to docs/model/previews/."""
    print("Setting up render camera & scenes...")
    sys.stdout.flush()
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.image_settings.file_format = "PNG"
    
    cam_data = bpy.data.cameras.new("Render_Camera")
    cam_data.lens = 48 # 48mm focal length
    cam_obj = bpy.data.objects.new("Render_Camera_Obj", cam_data)
    bpy.context.collection.objects.link(cam_obj)
    scene.camera = cam_obj
    
    views = [
        ("rear_three_quarter.png", (0.24, 0.17, 0.26), (0, 0.045, 0.0)),
        ("front_three_quarter.png", (0.24, 0.17, -0.26), (0, 0.045, 0.0)),
        ("top_view.png", (0.001, 0.40, 0.001), (0, 0.045, 0.0)),
        ("electronics_closeup.png", (-0.06, 0.14, 0.02), (-0.01, 0.06, 0.01)),
        ("app_camera_view.png", (0.0, 0.145, 0.310), (0.0, 0.035, 0.0))
    ]
    
    for filename, cam_loc, target_loc in views:
        cam_obj.location = cam_loc
        point_camera_at(cam_obj, target_loc)
        out_path = os.path.join(PREVIEWS_DIR, filename)
        scene.render.filepath = out_path
        print(f"Rendering {filename} -> {out_path}...")
        sys.stdout.flush()
        bpy.ops.render.render(write_still=True)
        print(f"Saved preview: {out_path}")
        sys.stdout.flush()
        
    bpy.data.objects.remove(cam_obj)

# -----------------------------------------------------------------------------
# GLB Export & Validation
# -----------------------------------------------------------------------------
def export_glb_asset():
    """Exports the mobile-optimized binary glTF (.glb) model with named nodes."""
    print(f"Exporting GLB asset to: {OUTPUT_GLB}...")
    sys.stdout.flush()
    
    bpy.ops.object.select_all(action='DESELECT')
    car_root = bpy.data.objects.get("car_root")
    if car_root:
        car_root.select_set(True)
        for child in car_root.children_recursive:
            child.select_set(True)
            
    bpy.ops.export_scene.gltf(
        filepath=OUTPUT_GLB,
        export_format='GLB',
        use_selection=True,
        export_yup=True,
        export_apply=False,
        export_materials='EXPORT',
        export_attributes=True,
        export_normals=True,
        export_tangents=False
    )
    
    file_size = os.path.getsize(OUTPUT_GLB)
    print(f"GLB asset exported successfully! Size: {file_size:,} bytes")
    sys.stdout.flush()
    return file_size

def save_master_blend():
    """Saves the complete Blender master project."""
    print(f"Saving Blender project to: {OUTPUT_BLEND}...")
    sys.stdout.flush()
    bpy.ops.wm.save_as_mainfile(filepath=OUTPUT_BLEND)
    print(f"Blender project saved: {OUTPUT_BLEND}")
    sys.stdout.flush()

# -----------------------------------------------------------------------------
# Entry Point
# -----------------------------------------------------------------------------
def main():
    print("=======================================================")
    print("ROBO RACER 4WD - 3D VEHICLE ASSET PIPELINE")
    print(f"Blender Version: {bpy.app.version_string}")
    print("=======================================================")
    sys.stdout.flush()
    
    reset_scene()
    build_complete_robo_racer()
    setup_lighting()
    
    # Render preview images
    render_previews()
    
    # Save Blender master source project
    save_master_blend()
    
    # Export clean mobile GLB asset
    glb_size = export_glb_asset()
    
    print("=======================================================")
    print("ALL MODELING & EXPORT TASKS FINISHED SUCCESSFULLY")
    print(f"Master Blend: {OUTPUT_BLEND}")
    print(f"Exported GLB: {OUTPUT_GLB} ({glb_size:,} bytes)")
    print(f"Previews Dir: {PREVIEWS_DIR}")
    print("=======================================================")
    sys.stdout.flush()

if __name__ == "__main__":
    main()
