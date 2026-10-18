"""
Robo Racer 4WD - Procedural High-Detail 3D Vehicle Model Generator & Exporter
Authoring Environment: Blender 5.2.0 LTS (Headless or GUI)

Blender Native Coordinate System (Z-Up):
  X: -X = Left, +X = Right
  Y: +Y = Front (Forward), -Y = Rear (Backward)
  Z:  0 = Ground, +Z = Up
glTF 2.0 Export (with export_yup=True):
  Blender +Z -> glTF +Y (Up)
  Blender +Y -> glTF -Z (Forward)
  Blender -Y -> glTF +Z (Rear / Toward Camera)
  Blender +X -> glTF +X (Right)
  Blender -X -> glTF -X (Left)
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
HARDWARE_RENDERS_DIR = os.path.join(REPO_ROOT, "docs", "hardware", "renders")
OUTPUT_BLEND = os.path.join(REPO_ROOT, "tools", "modeling", "robo_racer_car.blend")
OUTPUT_GLB = os.path.join(ASSETS_DIR, "robo_racer_car.glb")

os.makedirs(ASSETS_DIR, exist_ok=True)
os.makedirs(PREVIEWS_DIR, exist_ok=True)
os.makedirs(HARDWARE_RENDERS_DIR, exist_ok=True)
os.makedirs(os.path.dirname(OUTPUT_BLEND), exist_ok=True)

WHEEL_RADIUS = 0.033        # 66 mm diameter (radius 33 mm)
WHEEL_WIDTH = 0.026         # 26 mm width
TRACK_X = 0.076             # Â±76 mm wheel center
WHEELBASE_Y = 0.065         # Â±65 mm axle center (130 mm wheelbase: +65mm front, -65mm rear)
AXLE_Z = 0.033              # 33 mm axle height off ground

DECK_L = 0.220              # 220 mm chassis core length (along Y)
DECK_W = 0.110              # 110 mm chassis width (along X)
DECK_T = 0.003              # 3 mm acrylic thickness (along Z)
STANDOFF_H = 0.030          # 30 mm deck spacing
LOWER_DECK_Z = 0.024        # Bottom deck Z (top face at 0.027)
UPPER_DECK_Z = 0.057        # Top deck Z (top face at 0.060)

# -----------------------------------------------------------------------------
# Utility Helpers
# -----------------------------------------------------------------------------
def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    for c in list(bpy.data.collections):
        bpy.data.collections.remove(c)
    for m in list(bpy.data.materials):
        bpy.data.materials.remove(m)
    for me in list(bpy.data.meshes):
        bpy.data.meshes.remove(me)

def create_pbr_material(name, base_color, metallic=0.0, roughness=0.5, transmission=0.0, emission_color=(0,0,0,1), emission_strength=0.0):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs["Base Color"].default_value = base_color
        bsdf.inputs["Metallic"].default_value = metallic
        bsdf.inputs["Roughness"].default_value = roughness

        if "Transmission Weight" in bsdf.inputs:
            bsdf.inputs["Transmission Weight"].default_value = transmission
        elif "Transmission" in bsdf.inputs:
            bsdf.inputs["Transmission"].default_value = transmission

        if "Emission Color" in bsdf.inputs:
            bsdf.inputs["Emission Color"].default_value = emission_color
        if "Emission Strength" in bsdf.inputs:
            bsdf.inputs["Emission Strength"].default_value = emission_strength

    return mat

def create_cube(name, size, location=(0,0,0), material=None, parent=None):
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

    # Translucent Yellow Acrylic (chassis decks)
    mats["acrylic_yellow"] = create_pbr_material(
        "Mat_AcrylicYellow",
        base_color=(1.0, 0.78, 0.05, 0.85),
        metallic=0.05,
        roughness=0.18,
        transmission=0.55
    )

    # Black Natural Tire Rubber
    mats["tire_rubber"] = create_pbr_material(
        "Mat_TireRubber",
        base_color=(0.10, 0.10, 0.11, 1.0),
        metallic=0.02,
        roughness=0.82
    )

    # Vivid Yellow Wheel Rim ABS Plastic
    mats["wheel_rim_yellow"] = create_pbr_material(
        "Mat_WheelRimYellow",
        base_color=(1.0, 0.82, 0.01, 1.0),
        metallic=0.05,
        roughness=0.28
    )

    # Yellow TT Motor Housing Plastic
    mats["motor_yellow"] = create_pbr_material(
        "Mat_MotorYellow",
        base_color=(0.96, 0.74, 0.04, 1.0),
        metallic=0.02,
        roughness=0.35
    )

    # DC Motor Brushed Steel Canister & Fasteners
    mats["motor_steel"] = create_pbr_material(
        "Mat_MotorSteel",
        base_color=(0.82, 0.84, 0.88, 1.0),
        metallic=0.95,
        roughness=0.22
    )

    # Brass Standoffs & Screws
    mats["brass_metal"] = create_pbr_material(
        "Mat_BrassMetal",
        base_color=(0.92, 0.76, 0.32, 1.0),
        metallic=0.90,
        roughness=0.25
    )

    # Red PCB (L298N Solder Mask)
    mats["pcb_red"] = create_pbr_material(
        "Mat_PcbRed",
        base_color=(0.78, 0.10, 0.12, 1.0),
        metallic=0.08,
        roughness=0.38
    )

    # Matte Black PCB (ESP32 Board)
    mats["pcb_black"] = create_pbr_material(
        "Mat_PcbBlack",
        base_color=(0.10, 0.10, 0.11, 1.0),
        metallic=0.10,
        roughness=0.45
    )

    # Deep Blue PCB (DC-DC Converter Board)
    mats["pcb_blue"] = create_pbr_material(
        "Mat_PcbBlue",
        base_color=(0.06, 0.22, 0.58, 1.0),
        metallic=0.08,
        roughness=0.40
    )

    # Aluminum Heatsink (Black Anodized)
    mats["heatsink_black"] = create_pbr_material(
        "Mat_HeatsinkBlack",
        base_color=(0.14, 0.14, 0.15, 1.0),
        metallic=0.75,
        roughness=0.32
    )

    # Tin / Nickel RF Shield (ESP32-WROOM-32)
    mats["rf_shield_nickel"] = create_pbr_material(
        "Mat_RfShieldNickel",
        base_color=(0.88, 0.88, 0.90, 1.0),
        metallic=0.92,
        roughness=0.20
    )

    # Terminal Blocks (Green/Blue)
    mats["terminal_green"] = create_pbr_material(
        "Mat_TerminalGreen",
        base_color=(0.12, 0.58, 0.24, 1.0),
        metallic=0.02,
        roughness=0.40
    )

    # Electronic Chip Black Compound
    mats["ic_black"] = create_pbr_material(
        "Mat_IcBlack",
        base_color=(0.05, 0.05, 0.05, 1.0),
        metallic=0.02,
        roughness=0.60
    )

    # Inductor Ferrite Core (Charcoal with Top Markings)
    mats["inductor_shielded"] = create_pbr_material(
        "Mat_InductorShielded",
        base_color=(0.12, 0.13, 0.15, 1.0),
        metallic=0.30,
        roughness=0.45
    )

    # Electrolytic Capacitor Silver Top
    mats["cap_silver"] = create_pbr_material(
        "Mat_CapSilver",
        base_color=(0.75, 0.77, 0.80, 1.0),
        metallic=0.88,
        roughness=0.25
    )

    # Battery Tray Molded Black ABS
    mats["battery_tray_black"] = create_pbr_material(
        "Mat_BatteryTrayBlack",
        base_color=(0.06, 0.06, 0.07, 1.0),
        metallic=0.05,
        roughness=0.55
    )

    # Battery Cell Body (Industrial Blue 18650 / AA cells)
    mats["battery_cell_blue"] = create_pbr_material(
        "Mat_BatteryCellBlue",
        base_color=(0.08, 0.35, 0.85, 1.0),
        metallic=0.10,
        roughness=0.30
    )

    # Optical Speed Encoder Disc (Matte Black with 20 slots)
    mats["encoder_disc_black"] = create_pbr_material(
        "Mat_EncoderDiscBlack",
        base_color=(0.08, 0.08, 0.09, 1.0),
        metallic=0.05,
        roughness=0.65
    )

    # Electronic Pin Headers (Gold / Tin)
    mats["pin_headers"] = create_pbr_material(
        "Mat_PinHeaders",
        base_color=(0.85, 0.75, 0.25, 1.0),
        metallic=0.92,
        roughness=0.22
    )

    # Wires (Physical Color Code matching wire-by-wire specification)
    mats["wire_red"] = create_pbr_material("Mat_WireRed", (0.85, 0.08, 0.08, 1.0), 0.0, 0.45)
    mats["wire_black"] = create_pbr_material("Mat_WireBlack", (0.08, 0.08, 0.08, 1.0), 0.0, 0.45)
    mats["wire_yellow"] = create_pbr_material("Mat_WireYellow", (0.95, 0.80, 0.05, 1.0), 0.0, 0.45)
    mats["wire_green"] = create_pbr_material("Mat_WireGreen", (0.10, 0.75, 0.20, 1.0), 0.0, 0.45)
    mats["wire_blue"] = create_pbr_material("Mat_WireBlue", (0.10, 0.35, 0.90, 1.0), 0.0, 0.45)
    mats["wire_cyan"] = create_pbr_material("Mat_WireCyan", (0.00, 0.85, 0.95, 1.0), 0.0, 0.45)
    mats["wire_purple"] = create_pbr_material("Mat_WirePurple", (0.65, 0.25, 0.85, 1.0), 0.0, 0.45)
    mats["wire_orange"] = create_pbr_material("Mat_WireOrange", (0.95, 0.50, 0.05, 1.0), 0.0, 0.45)
    mats["wire_white"] = create_pbr_material("Mat_WireWhite", (0.92, 0.92, 0.94, 1.0), 0.0, 0.45)

    # Emissive Red Brake Lights
    mats["brake_light_red"] = create_pbr_material(
        "Mat_BrakeLightRed",
        base_color=(0.95, 0.05, 0.05, 1.0),
        roughness=0.15,
        emission_color=(1.0, 0.05, 0.05, 1.0),
        emission_strength=5.0
    )

    # Emissive Blue Status LED (ESP32 GPIO 2)
    mats["status_led_blue"] = create_pbr_material(
        "Mat_StatusLedBlue",
        base_color=(0.10, 0.45, 1.0, 1.0),
        roughness=0.20,
        emission_color=(0.15, 0.60, 1.0, 1.0),
        emission_strength=6.0
    )

    return mats

# -----------------------------------------------------------------------------
# Detailed 3D Component Generators (Blender Z-Up)
# -----------------------------------------------------------------------------
def build_acrylic_deck(name, z_pos, mats, parent):
    """
    Builds a laser-cut acrylic chassis deck accurately matching the 4WD kit photo:
    - Central body deck with contoured bumper ends
    - Outer wheel well protection rims with curved slots
    - Curved bumper arc cutouts
    - Cross-shaped (+) center cutout
    - Motor bracket mounting slots
    """
    deck = create_cube(
        name,
        size=(DECK_W, DECK_L, DECK_T),
        location=(0, 0, z_pos + DECK_T / 2.0),
        material=mats["acrylic_yellow"],
        parent=parent
    )

    # Semicircular / contoured front and rear bumper extensions
    bumper_l = 0.022
    bumper_w = DECK_W * 0.92
    create_cube(
        f"{name}_bumper_front",
        size=(bumper_w, bumper_l, DECK_T),
        location=(0, DECK_L / 2.0 + bumper_l / 2.0 - 0.002, 0),
        material=mats["acrylic_yellow"],
        parent=deck
    )
    create_cube(
        f"{name}_bumper_rear",
        size=(bumper_w, bumper_l, DECK_T),
        location=(0, -DECK_L / 2.0 - bumper_l / 2.0 + 0.002, 0),
        material=mats["acrylic_yellow"],
        parent=deck
    )

    # Outer side wing guards / wheel arch contours
    wing_w = 0.016
    wing_l = 0.045
    for sx in (-1.0, 1.0):
        # Mid-chassis outer tab between front and rear wheels
        create_cube(
            f"{name}_side_tab_{'l' if sx < 0 else 'r'}",
            size=(wing_w, wing_l, DECK_T),
            location=(sx * (DECK_W / 2.0 + wing_w / 2.0), 0, 0),
            material=mats["acrylic_yellow"],
            parent=deck
        )

    return deck

def build_standoffs_and_screws(mats, parent):
    """Builds 6 M3 hexagonal brass standoffs with nickel screw heads along Z axis."""
    standoffs_group = bpy.data.objects.new("chassis_standoffs", None)
    bpy.context.collection.objects.link(standoffs_group)
    standoffs_group.parent = parent

    coords = [
        (-DECK_W * 0.38,  DECK_L * 0.38),
        ( DECK_W * 0.38,  DECK_L * 0.38),
        (-DECK_W * 0.38,  0.0),
        ( DECK_W * 0.38,  0.0),
        (-DECK_W * 0.38, -DECK_L * 0.38),
        ( DECK_W * 0.38, -DECK_L * 0.38)
    ]

    for i, (sx, sy) in enumerate(coords):
        # Hexagonal brass pillar (naturally along Z)
        create_cylinder(
            f"standoff_pillar_{i}",
            radius=0.003,
            depth=STANDOFF_H,
            location=(sx, sy, LOWER_DECK_Z + DECK_T + STANDOFF_H / 2.0),
            rotation=(0, 0, 0),
            vertices=6,
            material=mats["brass_metal"],
            parent=standoffs_group
        )
        # Top nickel pan screw
        create_cylinder(
            f"standoff_screw_top_{i}",
            radius=0.0035,
            depth=0.0012,
            location=(sx, sy, UPPER_DECK_Z + DECK_T + 0.0006),
            rotation=(0, 0, 0),
            vertices=12,
            material=mats["motor_steel"],
            parent=standoffs_group
        )
        # Bottom nickel pan screw
        create_cylinder(
            f"standoff_screw_bot_{i}",
            radius=0.0035,
            depth=0.0012,
            location=(sx, sy, LOWER_DECK_Z - 0.0006),
            rotation=(0, 0, 0),
            vertices=12,
            material=mats["motor_steel"],
            parent=standoffs_group
        )

def build_tt_gear_motor(name, x, y, z, is_left, mats, parent):
    """
    Builds an accurate yellow TT dual-shaft gear motor with:
    - Yellow gearbox housing & dual output shafts
    - Silver brushed steel DC canister with terminal tabs
    - Laser-cut acrylic mounting brackets with M3 bolts
    - 20-slot optical speed encoder disc on inner shaft
    """
    motor_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(motor_root)
    motor_root.location = (x, y, z)
    motor_root.parent = parent

    # 1. Yellow Rectangular Gearbox Housing
    gb_w = 0.019
    gb_l = 0.046
    gb_h = 0.022
    create_cube(
        f"{name}_gearbox",
        size=(gb_w, gb_l, gb_h),
        location=(0, 0, 0),
        material=mats["motor_yellow"],
        parent=motor_root
    )

    # 2. Cylindrical Metal DC Motor Canister (pointing inward toward center)
    can_r = 0.010
    can_len = 0.024
    can_y = -gb_l / 2.0 - can_len / 2.0 if y > 0 else gb_l / 2.0 + can_len / 2.0
    create_cylinder(
        f"{name}_canister",
        radius=can_r,
        depth=can_len,
        location=(0, can_y, 0),
        rotation=(math.radians(90), 0, 0),
        vertices=18,
        material=mats["motor_steel"],
        parent=motor_root
    )

    # 3. Outer Output Axle Shaft (pointing outward toward wheel)
    shaft_dir = -1.0 if is_left else 1.0
    shaft_len = 0.012
    shaft_x = shaft_dir * (gb_w / 2.0 + shaft_len / 2.0)
    create_cylinder(
        f"{name}_axle_shaft",
        radius=0.0027,
        depth=shaft_len,
        location=(shaft_x, 0, 0),
        rotation=(0, math.radians(90), 0),
        vertices=12,
        material=mats["wheel_rim_yellow"],
        parent=motor_root
    )

    # 4. Inner Output Shaft (pointing inward toward chassis centerline)
    inner_shaft_x = -shaft_dir * (gb_w / 2.0 + 0.004)
    create_cylinder(
        f"{name}_inner_shaft",
        radius=0.0027,
        depth=0.008,
        location=(inner_shaft_x, 0, 0),
        rotation=(0, math.radians(90), 0),
        vertices=12,
        material=mats["wheel_rim_yellow"],
        parent=motor_root
    )

    # 5. Black 20-Slot Optical Speed Encoder Disc (mounted on inner shaft)
    encoder_disc_x = -shaft_dir * (gb_w / 2.0 + 0.007)
    create_cylinder(
        f"{name}_encoder_disc",
        radius=0.013,
        depth=0.002,
        location=(encoder_disc_x, 0, 0),
        rotation=(0, math.radians(90), 0),
        vertices=24,
        material=mats["encoder_disc_black"],
        parent=motor_root
    )

    # 6. Acrylic Motor Mounting Bracket
    bracket_x = shaft_dir * (gb_w / 2.0 + 0.0015)
    create_cube(
        f"{name}_bracket",
        size=(0.003, gb_l * 0.8, gb_h * 1.3),
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
        ry = math.sin(angle) * (WHEEL_RADIUS + tread_h / 2.0)
        rz = math.cos(angle) * (WHEEL_RADIUS + tread_h / 2.0)

        # Left chevron lug
        lug_left = create_cube(
            f"{name}_tread_l_{i}",
            size=(tread_w, tread_len, tread_h),
            location=(-WHEEL_WIDTH * 0.22, ry, rz),
            material=mats["tire_rubber"],
            parent=pivot
        )
        lug_left.rotation_euler = (angle, math.radians(20), 0)

        # Right chevron lug
        lug_right = create_cube(
            f"{name}_tread_r_{i}",
            size=(tread_w, tread_len, tread_h),
            location=(WHEEL_WIDTH * 0.22, ry, rz),
            material=mats["tire_rubber"],
            parent=pivot
        )
        lug_right.rotation_euler = (angle, math.radians(-20), 0)

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
        sy = math.sin(spoke_angle) * spoke_r
        sz = math.cos(spoke_angle) * spoke_r

        spoke = create_cube(
            f"{name}_spoke_{s}",
            size=(0.003, 0.004, spoke_r),
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
        size=(pcb_w, pcb_l, pcb_t),
        location=(0, 0, pcb_t / 2.0),
        material=mats["pcb_black"],
        parent=esp_root
    )

    # 2. Nickel RF Shield (ESP32-WROOM-32 module, front-center)
    shield_w = 0.018
    shield_l = 0.024
    shield_h = 0.003
    create_cube(
        f"{name}_rf_shield",
        size=(shield_w, shield_l, shield_h),
        location=(0, pcb_l * 0.15, pcb_t + shield_h / 2.0),
        material=mats["rf_shield_nickel"],
        parent=esp_root
    )

    # Black serpentine PCB antenna section at front
    ant_len = 0.007
    create_cube(
        f"{name}_antenna",
        size=(shield_w * 0.95, ant_len, pcb_t * 1.05),
        location=(0, pcb_l / 2.0 - ant_len / 2.0, pcb_t / 2.0),
        material=mats["pcb_black"],
        parent=esp_root
    )

    # 3. Two Rows of 15-pin Headers (2.54mm pitch, 0.9-inch spacing)
    header_w = 0.0025
    header_l = 0.038
    header_h = 0.0085
    hdr_x = pcb_w / 2.0 - header_w / 2.0 - 0.001

    create_cube(
        f"{name}_header_left",
        size=(header_w, header_l, header_h),
        location=(-hdr_x, -pcb_l * 0.05, pcb_t + header_h / 2.0),
        material=mats["pin_headers"],
        parent=esp_root
    )
    create_cube(
        f"{name}_header_right",
        size=(header_w, header_l, header_h),
        location=(hdr_x, -pcb_l * 0.05, pcb_t + header_h / 2.0),
        material=mats["pin_headers"],
        parent=esp_root
    )

    # 4. Silicon Labs CP2102 & AMS1117-3.3 Regulator
    create_cube(
        f"{name}_cp2102",
        size=(0.005, 0.005, 0.0012),
        location=(-0.003, -pcb_l * 0.22, pcb_t + 0.0006),
        material=mats["ic_black"],
        parent=esp_root
    )
    create_cube(
        f"{name}_regulator",
        size=(0.006, 0.004, 0.0018),
        location=(0.004, -pcb_l * 0.22, pcb_t + 0.0009),
        material=mats["ic_black"],
        parent=esp_root
    )

    # 5. Micro-USB Type-B Connector (Rear edge)
    create_cube(
        f"{name}_usb_port",
        size=(0.0075, 0.006, 0.003),
        location=(0, -pcb_l / 2.0 + 0.003, pcb_t + 0.0015),
        material=mats["rf_shield_nickel"],
        parent=esp_root
    )

    # 6. EN & BOOT Buttons
    create_cube(
        f"{name}_btn_en",
        size=(0.003, 0.003, 0.002),
        location=(-0.008, -pcb_l / 2.0 + 0.007, pcb_t + 0.001),
        material=mats["brass_metal"],
        parent=esp_root
    )
    create_cube(
        f"{name}_btn_boot",
        size=(0.003, 0.003, 0.002),
        location=(0.008, -pcb_l / 2.0 + 0.007, pcb_t + 0.001),
        material=mats["brass_metal"],
        parent=esp_root
    )

    # 7. Diagnostic Status LED (GPIO 2 - Emissive Blue) & Power LED (Red)
    create_cube(
        f"{name}_led_gpio2",
        size=(0.0016, 0.0025, 0.0008),
        location=(-0.005, -pcb_l * 0.12, pcb_t + 0.0004),
        material=mats["status_led_blue"],
        parent=esp_root
    )
    create_cube(
        f"{name}_led_pwr",
        size=(0.0016, 0.0025, 0.0008),
        location=(0.005, -pcb_l * 0.12, pcb_t + 0.0004),
        material=mats["brake_light_red"],
        parent=esp_root
    )

    return esp_root

def build_l298n_driver(name, x, y, z, mats, parent):
    """Builds an accurate L298N dual H-bridge motor driver module with heatsink & terminals."""
    drv_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(drv_root)
    drv_root.location = (x, y, z)
    drv_root.parent = parent

    pcb_size = 0.0435
    pcb_t = 0.0016

    # 1. Red FR-4 PCB
    create_cube(
        f"{name}_pcb",
        size=(pcb_size, pcb_size, pcb_t),
        location=(0, 0, pcb_t / 2.0),
        material=mats["pcb_red"],
        parent=drv_root
    )

    # 2. Black Anodized Finned Aluminum Heatsink
    hs_w = 0.024
    hs_d = 0.014
    hs_h = 0.023
    create_cube(
        f"{name}_heatsink_base",
        size=(hs_w, hs_d * 0.3, hs_h),
        location=(0, 0.002, pcb_t + hs_h / 2.0),
        material=mats["heatsink_black"],
        parent=drv_root
    )
    # 5 Vertical Cooling Fins
    fin_thick = 0.0014
    for f in range(5):
        fx = -hs_w / 2.0 + (f + 0.5) * (hs_w / 5.0)
        create_cube(
            f"{name}_fin_{f}",
            size=(fin_thick, hs_d * 0.7, hs_h * 0.95),
            location=(fx, -0.002, pcb_t + hs_h / 2.0),
            material=mats["heatsink_black"],
            parent=drv_root
        )

    # 3. Screw Terminal Blocks
    create_cube(
        f"{name}_term_pwr",
        size=(0.016, 0.009, 0.010),
        location=(-0.011, -pcb_size / 2.0 + 0.0055, pcb_t + 0.005),
        material=mats["terminal_green"],
        parent=drv_root
    )
    create_cube(
        f"{name}_term_mota",
        size=(0.009, 0.014, 0.010),
        location=(-pcb_size / 2.0 + 0.0055, 0.002, pcb_t + 0.005),
        material=mats["terminal_green"],
        parent=drv_root
    )
    create_cube(
        f"{name}_term_motb",
        size=(0.009, 0.014, 0.010),
        location=(pcb_size / 2.0 - 0.0055, 0.002, pcb_t + 0.005),
        material=mats["terminal_green"],
        parent=drv_root
    )

    # 4. Large Electrolytic Capacitors
    create_cylinder(
        f"{name}_cap1",
        radius=0.004,
        depth=0.011,
        location=(-0.012, 0.013, pcb_t + 0.0055),
        rotation=(0, 0, 0),
        vertices=16,
        material=mats["ic_black"],
        parent=drv_root
    )
    create_cylinder(
        f"{name}_cap2",
        radius=0.004,
        depth=0.011,
        location=(0.012, 0.013, pcb_t + 0.0055),
        rotation=(0, 0, 0),
        vertices=16,
        material=mats["ic_black"],
        parent=drv_root
    )

    # 5. Logic Header Pins
    create_cube(
        f"{name}_hdr_logic",
        size=(0.016, 0.003, 0.006),
        location=(0.010, -pcb_size / 2.0 + 0.0055, pcb_t + 0.003),
        material=mats["pin_headers"],
        parent=drv_root
    )

    return drv_root

def build_dcdc_converter(name, x, y, z, mats, parent):
    """
    Builds the adjustable DC-DC converter module faithfully matching the user's photo:
    - Deep blue double-sided PCB
    - TWO large shielded power inductors marked "330" (33 uH)
    - Two radial aluminum electrolytic capacitors with silver tops
    - Blue 3296W multi-turn cermet potentiometer with brass adjustment screw
    - TO-263 switching IC with thermal tab
    - Solder terminals (IN+, IN-, OUT+, OUT-)
    """
    conv_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(conv_root)
    conv_root.location = (x, y, z)
    conv_root.parent = parent

    pcb_w = 0.025
    pcb_l = 0.050
    pcb_t = 0.0016

    # 1. Deep Blue PCB
    create_cube(
        f"{name}_pcb",
        size=(pcb_w, pcb_l, pcb_t),
        location=(0, 0, pcb_t / 2.0),
        material=mats["pcb_blue"],
        parent=conv_root
    )

    # 2. TWO Large Shielded Power Inductors (Marked "330" / 33 uH) as verified in photo
    ind_size = 0.0105
    ind_h = 0.0075
    # Inductor 1 (Near input / mid-rear)
    create_cube(
        f"{name}_inductor_1",
        size=(ind_size, ind_size, ind_h),
        location=(-0.003, -0.006, pcb_t + ind_h / 2.0),
        material=mats["inductor_shielded"],
        parent=conv_root
    )
    # Inductor 2 (Near output / mid-front)
    create_cube(
        f"{name}_inductor_2",
        size=(ind_size, ind_size, ind_h),
        location=(-0.003, 0.011, pcb_t + ind_h / 2.0),
        material=mats["inductor_shielded"],
        parent=conv_root
    )

    # 3. Two Radial Aluminum Electrolytic Capacitors (Silver top with black sleeve)
    cap_r = 0.0042
    cap_h = 0.011
    # Input capacitor (rear-left corner)
    create_cylinder(
        f"{name}_cap_in",
        radius=cap_r,
        depth=cap_h,
        location=(-pcb_w * 0.28, -pcb_l / 2.0 + 0.008, pcb_t + cap_h / 2.0),
        rotation=(0, 0, 0),
        vertices=16,
        material=mats["cap_silver"],
        parent=conv_root
    )
    # Output capacitor (front-right corner)
    create_cylinder(
        f"{name}_cap_out",
        radius=cap_r,
        depth=cap_h,
        location=(pcb_w * 0.28, pcb_l / 2.0 - 0.008, pcb_t + cap_h / 2.0),
        rotation=(0, 0, 0),
        vertices=16,
        material=mats["cap_silver"],
        parent=conv_root
    )

    # 4. Precision Blue Trimpot with Brass Top Screw (3296W)
    create_cube(
        f"{name}_trimpot",
        size=(0.0048, 0.0095, 0.0095),
        location=(pcb_w * 0.26, -0.002, pcb_t + 0.00475),
        material=mats["pcb_blue"],
        parent=conv_root
    )
    create_cylinder(
        f"{name}_trim_screw",
        radius=0.0012,
        depth=0.0015,
        location=(pcb_w * 0.26, -0.002, pcb_t + 0.0095 + 0.0007),
        rotation=(0, 0, 0),
        vertices=12,
        material=mats["brass_metal"],
        parent=conv_root
    )

    # 5. Switching IC Package with Heat Sink Tab
    create_cube(
        f"{name}_ic",
        size=(0.009, 0.008, 0.0025),
        location=(0.004, -0.016, pcb_t + 0.00125),
        material=mats["ic_black"],
        parent=conv_root
    )

    # 6. Solder Pad Terminals (IN+, IN-, OUT+, OUT-)
    term_pad_w = 0.003
    term_pad_h = 0.0005
    for sx, sy, pname in [(-1, -1, "in_pos"), (1, -1, "in_neg"), (-1, 1, "out_pos"), (1, 1, "out_neg")]:
        create_cylinder(
            f"{name}_pad_{pname}",
            radius=0.0018,
            depth=0.0012,
            location=(sx * (pcb_w / 2.0 - 0.0035), sy * (pcb_l / 2.0 - 0.0035), pcb_t + 0.0006),
            rotation=(0, 0, 0),
            vertices=12,
            material=mats["motor_steel"],
            parent=conv_root
        )

    return conv_root

def build_battery_holder_assembly(name, x, y, z, mats, parent):
    """
    Builds the 4-cell cylindrical battery holder faithfully matching the kit photo:
    - Molded black ABS tray with 4 battery bays
    - 4 individual cylindrical battery cells
    - Metallic negative leaf/coil springs and positive contact plates
    - Red and black silicone wire leads extending toward rear
    """
    batt_root = bpy.data.objects.new(name, None)
    bpy.context.collection.objects.link(batt_root)
    batt_root.location = (x, y, z)
    batt_root.parent = parent

    bw = 0.058
    bl = 0.062
    bh = 0.016
    tray_t = 0.0018

    # 1. Molded Black Plastic Tray Outer Frame
    create_cube(
        f"{name}_tray_base",
        size=(bw, bl, tray_t),
        location=(0, 0, tray_t / 2.0),
        material=mats["battery_tray_black"],
        parent=batt_root
    )
    # Side walls
    create_cube(
        f"{name}_wall_l",
        size=(tray_t, bl, bh),
        location=(-bw / 2.0 + tray_t / 2.0, 0, bh / 2.0),
        material=mats["battery_tray_black"],
        parent=batt_root
    )
    create_cube(
        f"{name}_wall_r",
        size=(tray_t, bl, bh),
        location=(bw / 2.0 - tray_t / 2.0, 0, bh / 2.0),
        material=mats["battery_tray_black"],
        parent=batt_root
    )
    create_cube(
        f"{name}_wall_front",
        size=(bw, tray_t, bh),
        location=(0, bl / 2.0 - tray_t / 2.0, bh / 2.0),
        material=mats["battery_tray_black"],
        parent=batt_root
    )
    create_cube(
        f"{name}_wall_rear",
        size=(bw, tray_t, bh),
        location=(0, -bl / 2.0 + tray_t / 2.0, bh / 2.0),
        material=mats["battery_tray_black"],
        parent=batt_root
    )

    # 2. Four Cylindrical Battery Cells seated in parallel bays
    cell_r = 0.0068
    cell_len = 0.050
    rot_y = (math.radians(90), 0, 0) # Cylinder along Y-axis

    for c in range(4):
        cx = -bw * 0.36 + c * (bw * 0.24)
        # Blue cell body
        create_cylinder(
            f"{name}_cell_{c}",
            radius=cell_r,
            depth=cell_len,
            location=(cx, 0, tray_t + cell_r),
            rotation=rot_y,
            vertices=18,
            material=mats["battery_cell_blue"],
            parent=batt_root
        )
        # Nickel positive terminal button
        create_cylinder(
            f"{name}_cell_terminal_{c}",
            radius=cell_r * 0.40,
            depth=0.002,
            location=(cx, cell_len / 2.0 + 0.001, tray_t + cell_r),
            rotation=rot_y,
            vertices=12,
            material=mats["motor_steel"],
            parent=batt_root
        )
        # Nickel contact spring at rear
        create_cylinder(
            f"{name}_cell_spring_{c}",
            radius=cell_r * 0.65,
            depth=0.003,
            location=(cx, -cell_len / 2.0 - 0.0015, tray_t + cell_r),
            rotation=rot_y,
            vertices=12,
            material=mats["motor_steel"],
            parent=batt_root
        )

    # 3. Red and Black Silicone Output Leads extending from rear
    create_cylinder(
        f"{name}_lead_red",
        radius=0.0018,
        depth=0.022,
        location=(-0.008, -bl / 2.0 - 0.010, tray_t + cell_r),
        rotation=rot_y,
        vertices=10,
        material=mats["wire_red"],
        parent=batt_root
    )
    create_cylinder(
        f"{name}_lead_black",
        radius=0.0018,
        depth=0.022,
        location=(0.008, -bl / 2.0 - 0.010, tray_t + cell_r),
        rotation=rot_y,
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
    light_d = 0.005
    light_h = 0.006
    y_pos = -DECK_L / 2.0 + 0.004
    z_pos = UPPER_DECK_Z + DECK_T + light_h / 2.0

    create_cube(
        "brake_light_left",
        size=(light_w, light_d, light_h),
        location=(-0.038, y_pos, z_pos),
        material=mats["brake_light_red"],
        parent=lights_group
    )
    create_cube(
        "brake_light_right",
        size=(light_w, light_d, light_h),
        location=(0.038, y_pos, z_pos),
        material=mats["brake_light_red"],
        parent=lights_group
    )

def build_wiring_harness(mats, parent):
    """Builds complete routed multi-color wiring harness connecting ESP32, L298N drivers, DC-DC converter, motors, and battery."""
    harness_group = bpy.data.objects.new("wiring_harness", None)
    bpy.context.collection.objects.link(harness_group)
    harness_group.parent = parent

    wire_specs = [
        # ---------------------------------------------------------------------
        # 1. Left Motor Control Signal Ribbon (ESP32 to Left L298N Header)
        # Pins: ENA (Yellow), IN1 (Green), IN2 (Blue), IN3 (White), IN4 (Purple), ENB (Orange)
        # ---------------------------------------------------------------------
        ("wire_sig_l_ena", (-0.018, 0.018, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_yellow"]),
        ("wire_sig_l_in1", (-0.018, 0.015, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_green"]),
        ("wire_sig_l_in2", (-0.018, 0.012, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_blue"]),
        ("wire_sig_l_in3", (-0.018, 0.009, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_white"]),
        ("wire_sig_l_in4", (-0.018, 0.006, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_purple"]),
        ("wire_sig_l_enb", (-0.018, 0.003, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_orange"]),

        # ---------------------------------------------------------------------
        # 2. Right Motor Control Signal Ribbon (ESP32 to Right L298N Header)
        # Pins: ENA (Yellow), IN1 (Green), IN2 (Blue), IN3 (White), IN4 (Purple), ENB (Orange)
        # ---------------------------------------------------------------------
        ("wire_sig_r_ena", (0.018, 0.018, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_yellow"]),
        ("wire_sig_r_in1", (0.018, 0.015, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_green"]),
        ("wire_sig_r_in2", (0.018, 0.012, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_blue"]),
        ("wire_sig_r_in3", (0.018, 0.009, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_white"]),
        ("wire_sig_r_in4", (0.018, 0.006, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_purple"]),
        ("wire_sig_r_enb", (0.018, 0.003, UPPER_DECK_Z + 0.008), (0.014, 0.0012, 0.0012), mats["wire_orange"]),

        # ---------------------------------------------------------------------
        # 3. Direct Unswitched Battery Positive Rail (Raw ~6.0V Bus - Red, Heavy 20 AWG)
        # ---------------------------------------------------------------------
        ("wire_batt_pos_main",    (0.000, -0.042, LOWER_DECK_Z + 0.012), (0.0024, 0.035, 0.0024), mats["wire_red"]),
        ("wire_batt_pos_to_conv", (0.006, -0.065, UPPER_DECK_Z + 0.006), (0.0022, 0.015, 0.0022), mats["wire_red"]),
        ("wire_batt_pos_to_drv_l", (-0.016, -0.038, UPPER_DECK_Z + 0.006), (0.018, 0.0022, 0.0022), mats["wire_red"]),
        ("wire_batt_pos_to_drv_r", ( 0.016, -0.038, UPPER_DECK_Z + 0.006), (0.018, 0.0022, 0.0022), mats["wire_red"]),

        # ---------------------------------------------------------------------
        # 4. Central Star Ground Bus (0V Return - Black, Heavy 20 AWG)
        # ---------------------------------------------------------------------
        ("wire_star_gnd_main",    (-0.005, -0.042, LOWER_DECK_Z + 0.012), (0.0024, 0.035, 0.0024), mats["wire_black"]),
        ("wire_star_gnd_to_conv", (-0.006, -0.065, UPPER_DECK_Z + 0.006), (0.0022, 0.015, 0.0022), mats["wire_black"]),
        ("wire_star_gnd_to_drv_l", (-0.016, -0.042, UPPER_DECK_Z + 0.006), (0.018, 0.0022, 0.0022), mats["wire_black"]),
        ("wire_star_gnd_to_drv_r", ( 0.016, -0.042, UPPER_DECK_Z + 0.006), (0.018, 0.0022, 0.0022), mats["wire_black"]),
        ("wire_star_gnd_to_esp",  (-0.007,  0.022, UPPER_DECK_Z + 0.006), (0.0018, 0.025, 0.0018), mats["wire_black"]),

        # ---------------------------------------------------------------------
        # 5. Regulated 5.00V Logic Supply Rail (From DC-DC OUT+ - Cyan, 22 AWG)
        # ---------------------------------------------------------------------
        ("wire_logic_5v_to_esp",  ( 0.007,  0.015, UPPER_DECK_Z + 0.007), (0.0018, 0.045, 0.0018), mats["wire_cyan"]),
        ("wire_logic_5v_to_drv_l", (-0.020, -0.035, UPPER_DECK_Z + 0.007), (0.012, 0.0018, 0.0018), mats["wire_cyan"]),
        ("wire_logic_5v_to_drv_r", ( 0.020, -0.035, UPPER_DECK_Z + 0.007), (0.012, 0.0018, 0.0018), mats["wire_cyan"]),

        # ---------------------------------------------------------------------
        # 6. Battery Telemetry Divider Circuit & GPIO 34 Tap
        # ---------------------------------------------------------------------
        ("comp_volt_div_block",   ( 0.015, -0.025, UPPER_DECK_Z + 0.004), (0.006, 0.004, 0.003), mats["ic_black"]),
        ("wire_div_tap_gpio34",   (-0.009,  0.030, UPPER_DECK_Z + 0.008), (0.0012, 0.020, 0.0012), mats["wire_cyan"]),

        # ---------------------------------------------------------------------
        # 7. Motor Power Leads (Left & Right L298N to 4x TT Motors)
        # ---------------------------------------------------------------------
        # Left L298N to Front-Left Motor
        ("wire_mot_fl_pos", (-0.038, 0.025, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_red"]),
        ("wire_mot_fl_neg", (-0.040, 0.025, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_black"]),
        # Left L298N to Rear-Left Motor
        ("wire_mot_rl_pos", (-0.038, -0.050, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_red"]),
        ("wire_mot_rl_neg", (-0.040, -0.050, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_black"]),
        # Right L298N to Front-Right Motor
        ("wire_mot_fr_pos", ( 0.038, 0.025, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_red"]),
        ("wire_mot_fr_neg", ( 0.040, 0.025, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_black"]),
        # Right L298N to Rear-Right Motor
        ("wire_mot_rr_pos", ( 0.038, -0.050, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_red"]),
        ("wire_mot_rr_neg", ( 0.040, -0.050, (UPPER_DECK_Z + LOWER_DECK_Z)/2), (0.012, 0.0018, 0.025), mats["wire_black"])
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
    build_acrylic_deck("chassis_bottom_deck", LOWER_DECK_Z, mats, chassis_root)
    build_acrylic_deck("chassis_top_deck", UPPER_DECK_Z, mats, chassis_root)
    build_standoffs_and_screws(mats, chassis_root)

    print("Mounting 4x TT gear motors with optical encoder discs...")
    sys.stdout.flush()
    build_tt_gear_motor("motor_left_front",  -TRACK_X * 0.65,  WHEELBASE_Y, LOWER_DECK_Z + 0.010, is_left=True,  mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_left_rear",   -TRACK_X * 0.65, -WHEELBASE_Y, LOWER_DECK_Z + 0.010, is_left=True,  mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_right_front",  TRACK_X * 0.65,  WHEELBASE_Y, LOWER_DECK_Z + 0.010, is_left=False, mats=mats, parent=chassis_root)
    build_tt_gear_motor("motor_right_rear",   TRACK_X * 0.65, -WHEELBASE_Y, LOWER_DECK_Z + 0.010, is_left=False, mats=mats, parent=chassis_root)

    print("Assembling 4x independently animated wheels...")
    sys.stdout.flush()
    build_detailed_wheel("front_left_wheel_pivot",  -TRACK_X,  WHEELBASE_Y, AXLE_Z, is_left=True,  mats=mats, parent=car_root)
    build_detailed_wheel("front_right_wheel_pivot",  TRACK_X,  WHEELBASE_Y, AXLE_Z, is_left=False, mats=mats, parent=car_root)
    build_detailed_wheel("rear_left_wheel_pivot",   -TRACK_X, -WHEELBASE_Y, AXLE_Z, is_left=True,  mats=mats, parent=car_root)
    build_detailed_wheel("rear_right_wheel_pivot",   TRACK_X, -WHEELBASE_Y, AXLE_Z, is_left=False, mats=mats, parent=car_root)

    print("Populating electronics on upper deck...")
    sys.stdout.flush()
    # ESP32 Board: Centered near front (+Y) with antenna pointing forward
    build_prakti_esp32_board("esp32_board", 0.0, 0.045, UPPER_DECK_Z + DECK_T, mats, chassis_root)

    # Dual L298N Drivers: Placed symmetrically in mid-chassis
    build_l298n_driver("l298n_left",  -0.026, -0.015, UPPER_DECK_Z + DECK_T, mats, chassis_root)
    build_l298n_driver("l298n_right",  0.026, -0.015, UPPER_DECK_Z + DECK_T, mats, chassis_root)

    # DC-DC Converter (Dual Inductor): Near rear (-Y)
    build_dcdc_converter("power_converter", 0.0, -0.068, UPPER_DECK_Z + DECK_T, mats, chassis_root)

    # 4-Cell Battery Holder: Lower deck central bay
    build_battery_holder_assembly("battery_pack", 0.0, -0.005, LOWER_DECK_Z + DECK_T + 0.001, mats, chassis_root)

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
    if bpy.context.scene.world and bpy.context.scene.world.node_tree:
        bg = bpy.context.scene.world.node_tree.nodes.get("Background")
        if bg:
            bg.inputs["Color"].default_value = (0.05, 0.06, 0.08, 1.0)
            bg.inputs["Strength"].default_value = 1.0

    # Key Light
    key_light = bpy.data.lights.new(name="Key_Light", type="SUN")
    key_light.energy = 4.0
    key_light.color = (1.0, 0.98, 0.94)
    key_obj = bpy.data.objects.new("Key_Light_Obj", key_light)
    bpy.context.collection.objects.link(key_obj)
    key_obj.location = (0.35, -0.35, 0.50)
    key_obj.rotation_euler = (math.radians(45), math.radians(25), math.radians(-35))

    # Fill Light
    fill_light = bpy.data.lights.new(name="Fill_Light", type="SUN")
    fill_light.energy = 2.0
    fill_light.color = (0.85, 0.92, 1.0)
    fill_obj = bpy.data.objects.new("Fill_Light_Obj", fill_light)
    bpy.context.collection.objects.link(fill_obj)
    fill_obj.location = (-0.35, 0.35, 0.40)
    fill_obj.rotation_euler = (math.radians(-40), math.radians(-30), 0)

    # Rim Light
    rim_light = bpy.data.lights.new(name="Rim_Light", type="SUN")
    rim_light.energy = 2.5
    rim_light.color = (0.0, 0.85, 1.0)
    rim_obj = bpy.data.objects.new("Rim_Light_Obj", rim_light)
    bpy.context.collection.objects.link(rim_obj)
    rim_obj.location = (0.0, 0.45, 0.30)
    rim_obj.rotation_euler = (math.radians(-65), 0, 0)

def point_camera_at(cam_obj, target_pos=(0, 0, 0.045)):
    """Points camera directly at target_pos using -Z forward and +Z world up."""
    direction = mathutils.Vector(target_pos) - cam_obj.location
    rot_quat = direction.to_track_quat('-Z', 'Z')
    cam_obj.rotation_euler = rot_quat.to_euler()

def render_previews():
    """Renders all 7 required engineering views and saves to docs/model/previews/."""
    print("Setting up render camera & scenes...")
    sys.stdout.flush()
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 1280
    scene.render.resolution_y = 720
    scene.render.image_settings.file_format = "PNG"

    cam_data = bpy.data.cameras.new("Render_Camera")
    cam_data.lens = 48
    cam_obj = bpy.data.objects.new("Render_Camera_Obj", cam_data)
    bpy.context.collection.objects.link(cam_obj)
    scene.camera = cam_obj

    # Standard inspection camera views
    views = [
        ("rear_three_quarter.png", (0.28, -0.30, 0.20), (0.0, 0.0, 0.045)),
        ("front_three_quarter.png", (0.28, 0.30, 0.20), (0.0, 0.0, 0.045)),
        ("top_view.png", (0.001, 0.001, 0.45), (0.0, 0.0, 0.045)),
        ("electronics_closeup.png", (0.13, 0.02, 0.16), (0.0, 0.015, 0.062)),
        ("side_view.png", (0.36, 0.0, 0.045), (0.0, 0.0, 0.045)),
        ("app_camera_view.png", (0.0, -0.310, 0.145), (0.0, 0.020, 0.045))
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

    # Render Exploded Assembly View
    render_exploded_view(scene, cam_obj)

    # -------------------------------------------------------------------------
    # Dedicated Hardware Wiring Inspection Views (saved to docs/hardware/renders/)
    # -------------------------------------------------------------------------
    wiring_views = [
        ("wiring_top_view.png", (0.001, 0.001, 0.38), (0.0, 0.0, 0.050)),
        ("wiring_front_three_quarter.png", (0.22, 0.24, 0.18), (0.0, 0.015, 0.055)),
        ("wiring_electronics_closeup.png", (0.12, 0.01, 0.14), (0.0, -0.010, 0.060)),
        ("wiring_power_path.png", (0.18, -0.22, 0.16), (0.0, -0.040, 0.055)),
        ("wiring_gpio_closeup.png", (0.06, 0.06, 0.10), (0.0, 0.045, 0.065)),
        ("wiring_motor_connections.png", (-0.22, 0.02, 0.09), (-0.035, 0.0, 0.035))
    ]

    print("Rendering dedicated wiring inspection perspectives...")
    sys.stdout.flush()
    for filename, cam_loc, target_loc in wiring_views:
        cam_obj.location = cam_loc
        point_camera_at(cam_obj, target_loc)
        out_path = os.path.join(HARDWARE_RENDERS_DIR, filename)
        scene.render.filepath = out_path
        print(f"Rendering wiring preview {filename} -> {out_path}...")
        sys.stdout.flush()
        bpy.ops.render.render(write_still=True)
        print(f"Saved wiring preview: {out_path}")
        sys.stdout.flush()

    bpy.data.objects.remove(cam_obj)

def render_exploded_view(scene, cam_obj):
    """
    Renders an exploded assembly view showing mechanical relationships,
    then restores nominal component transforms.
    """
    filename = "exploded_view.png"
    out_path = os.path.join(PREVIEWS_DIR, filename)
    print(f"Rendering exploded assembly view -> {out_path}...")
    sys.stdout.flush()

    # Grab components to displace
    top_deck = bpy.data.objects.get("chassis_top_deck")
    standoffs = bpy.data.objects.get("chassis_standoffs")
    esp32 = bpy.data.objects.get("esp32_board")
    drv_l = bpy.data.objects.get("l298n_left")
    drv_r = bpy.data.objects.get("l298n_right")
    conv = bpy.data.objects.get("power_converter")
    batt = bpy.data.objects.get("battery_pack")
    harness = bpy.data.objects.get("wiring_harness")
    brake_lights = bpy.data.objects.get("rear_brake_lights")

    w_fl = bpy.data.objects.get("front_left_wheel_pivot")
    w_fr = bpy.data.objects.get("front_right_wheel_pivot")
    w_rl = bpy.data.objects.get("rear_left_wheel_pivot")
    w_rr = bpy.data.objects.get("rear_right_wheel_pivot")

    m_fl = bpy.data.objects.get("motor_left_front")
    m_fr = bpy.data.objects.get("motor_right_front")
    m_rl = bpy.data.objects.get("motor_left_rear")
    m_rr = bpy.data.objects.get("motor_right_rear")

    # Store original Z / X positions
    displaced_objects = []

    def explode_obj(obj, dz=0.0, dx=0.0, dy=0.0):
        if obj:
            orig = obj.location.copy()
            obj.location.z += dz
            obj.location.x += dx
            obj.location.y += dy
            displaced_objects.append((obj, orig))

    # Explode upward: upper deck and all electronics
    explode_obj(top_deck, dz=0.070)
    explode_obj(standoffs, dz=0.035)
    explode_obj(esp32, dz=0.095)
    explode_obj(drv_l, dz=0.090)
    explode_obj(drv_r, dz=0.090)
    explode_obj(conv, dz=0.085)
    explode_obj(harness, dz=0.088)
    explode_obj(brake_lights, dz=0.075)

    # Explode downward: battery pack and motors
    explode_obj(batt, dz=-0.022)
    explode_obj(m_fl, dz=-0.025)
    explode_obj(m_fr, dz=-0.025)
    explode_obj(m_rl, dz=-0.025)
    explode_obj(m_rr, dz=-0.025)

    # Explode outward: 4 wheels
    explode_obj(w_fl, dx=-0.045)
    explode_obj(w_rl, dx=-0.045)
    explode_obj(w_fr, dx=0.045)
    explode_obj(w_rr, dx=0.045)

    # Point camera at exploded assembly
    cam_obj.location = (0.34, -0.34, 0.24)
    point_camera_at(cam_obj, (0.0, 0.0, 0.055))
    scene.render.filepath = out_path
    bpy.ops.render.render(write_still=True)
    print(f"Saved exploded preview: {out_path}")
    sys.stdout.flush()

    # Restore original positions
    for obj, orig_loc in displaced_objects:
        obj.location = orig_loc
    print("Restored nominal component locations.")
    sys.stdout.flush()

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

    # Render all 7 preview images
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
