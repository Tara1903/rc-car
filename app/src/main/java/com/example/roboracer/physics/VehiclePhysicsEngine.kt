package com.example.roboracer.physics

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Surface friction and rolling resistance characteristics.
 */
enum class SurfacePreset(
    val displayName: String,
    val frictionCoefficient: Float,
    val rollingResistance: Float
) {
    SMOOTH_FLOOR("Smooth Floor", 0.85f, 0.018f),
    CARPET("Carpet", 0.52f, 0.075f)
}

/**
 * Operational mode of the 3D Digital Twin Viewport.
 */
enum class DigitalTwinMode {
    /**
     * Mode A: Offline interactive physics sandbox with simulated kinetics,
     * obstacle collisions, and ground dynamics.
     */
    SIMULATION,

    /**
     * Mode B: Live connected ESP32 vehicle visualization reflecting physical
     * telemetry frames and command predictions.
     */
    LIVE_TELEMETRY
}

/**
 * Axis-aligned bounding box static obstacle in the simulation arena.
 */
data class Obstacle(
    val name: String,
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float
) {
    fun intersectsCircle(cx: Float, cz: Float, radius: Float): Boolean {
        val closestX = cx.coerceIn(minX, maxX)
        val closestZ = cz.coerceIn(minZ, maxZ)
        val dx = cx - closestX
        val dz = cz - closestZ
        return (dx * dx + dz * dz) <= (radius * radius)
    }

    fun resolveCollision(cx: Float, cz: Float, radius: Float): CollisionResolution? {
        val closestX = cx.coerceIn(minX, maxX)
        val closestZ = cz.coerceIn(minZ, maxZ)
        val dx = cx - closestX
        val dz = cz - closestZ
        val distSq = dx * dx + dz * dz
        if (distSq > radius * radius) return null

        val dist = sqrt(distSq)
        val (nx, nz, penetration) = if (dist > 1e-4f) {
            val normalX = dx / dist
            val normalZ = dz / dist
            Triple(normalX, normalZ, radius - dist)
        } else {
            // Inside obstacle box, push out along shortest axis
            val distToMinX = abs(cx - minX)
            val distToMaxX = abs(maxX - cx)
            val distToMinZ = abs(cz - minZ)
            val distToMaxZ = abs(maxZ - cz)
            val minDist = minOf(distToMinX, distToMaxX, distToMinZ, distToMaxZ)
            when (minDist) {
                distToMinX -> Triple(-1.0f, 0.0f, radius + distToMinX)
                distToMaxX -> Triple(1.0f, 0.0f, radius + distToMaxX)
                distToMinZ -> Triple(0.0f, -1.0f, radius + distToMinZ)
                else -> Triple(0.0f, 1.0f, radius + distToMaxZ)
            }
        }
        return CollisionResolution(nx, nz, penetration)
    }
}

data class CollisionResolution(
    val normalX: Float,
    val normalZ: Float,
    val penetrationDepth: Float
)

/**
 * Real-time physics state snapshot of the Robo Racer vehicle.
 */
data class VehiclePhysicsState(
    val x: Float = 0.0f,
    val z: Float = 0.0f,
    val yawRad: Float = 0.0f,
    val yawDeg: Float = 0.0f,
    val vx: Float = 0.0f,
    val vz: Float = 0.0f,
    val linearSpeed: Float = 0.0f,
    val angularVelocity: Float = 0.0f,
    val wheelSpeedL: Float = 0.0f,
    val wheelSpeedR: Float = 0.0f,
    val wheelAngleL: Float = 0.0f,
    val wheelAngleR: Float = 0.0f,
    val slipRatioL: Float = 0.0f,
    val slipRatioR: Float = 0.0f,
    val pitchDeg: Float = 0.0f,
    val rollDeg: Float = 0.0f,
    val isColliding: Boolean = false,
    val surfacePreset: SurfacePreset = SurfacePreset.SMOOTH_FLOOR
)

/**
 * Deterministic, fixed-timestep 2D/3D differential-drive ground physics engine.
 * Computes vehicle kinetics, motor traction, Coulomb friction, rolling resistance,
 * slip ratio, simulated body-response cues (inertial pitch squat/dive and cornering roll),
 * and static obstacle collisions.
 *
 * NOTE ON ESTIMATED CONSTANTS:
 * All physical parameters (vehicle mass, yaw inertia, track width, wheelbase, wheel radius,
 * peak motor force, no-load wheel speed, collision radius, and friction coefficients) are
 * UNMEASURED engineering estimates derived from catalog component specifications and CAD scaling.
 * They are NOT calibrated against physical dynamometer or track data.
 *
 * NOTE ON BODY RESPONSE:
 * The physical vehicle features a rigid laser-cut acrylic chassis directly bolted to TT motors
 * with NO confirmed mechanical suspension. Pitch and roll angles represent restrained visual
 * body-response cues (tire rubber deflection and visual feedback) rather than mechanical suspension travel.
 */
class VehiclePhysicsEngine(
    var surfacePreset: SurfacePreset = SurfacePreset.SMOOTH_FLOOR,
    val vehicleMass: Float = 0.65f,                 // Estimated: 0.65 kg total physical mass
    val yawInertia: Float = 0.0028f,                // Estimated: kg*m^2 planar moment of inertia
    val trackWidth: Float = 0.152f,                 // Estimated: 152 mm lateral track separation
    val wheelbase: Float = 0.130f,                  // Estimated: 130 mm wheelbase
    val wheelRadius: Float = 0.033f,                // Estimated: 33 mm wheel radius (66 mm OD)
    val maxMotorForce: Float = 4.2f,                // Estimated: Newtons peak tractive force per side
    val maxNoLoadWheelSpeed: Float = 0.85f,         // Estimated: m/s free spinning wheel speed (~240 rpm)
    val collisionRadius: Float = 0.12f,             // Estimated: 120 mm vehicle bounding radius
    val gravity: Float = 9.81f
) {
    // Current state
    var state = VehiclePhysicsState(surfacePreset = surfacePreset)
        private set

    var isPaused: Boolean = false

    // Arena perimeter boundaries (default ±2.2m)
    var arenaMinX = -2.2f
    var arenaMaxX = 2.2f
    var arenaMinZ = -2.2f
    var arenaMaxZ = 2.2f

    val obstacles = mutableListOf<Obstacle>()

    init {
        // Setup default arena obstacles (crates/blocks)
        obstacles.add(Obstacle("Block_NW", -1.2f, -0.6f, -1.4f, -0.8f))
        obstacles.add(Obstacle("Block_SE", 0.6f, 1.2f, 0.8f, 1.4f))
    }

    fun reset() {
        state = VehiclePhysicsState(surfacePreset = surfacePreset)
    }

    fun togglePause(): Boolean {
        isPaused = !isPaused
        return isPaused
    }

    /**
     * Advances simulation by dt seconds with the given normalized inputs.
     * @param dt Timestep in seconds.
     * @param throttle Normalized longitudinal effort [-1.0, 1.0].
     * @param steering Normalized lateral steering effort [-1.0, 1.0] (negative = left, positive = right).
     * @param isBraking Active brake engagement.
     */
    fun step(
        dt: Float,
        throttle: Float,
        steering: Float,
        isBraking: Boolean
    ): VehiclePhysicsState {
        if (isPaused || dt <= 0f) return state

        // Substep for high numerical stability if dt > 0.02s
        val substepDt = 0.010f
        var remainingDt = dt.coerceIn(0.001f, 0.1f)
        while (remainingDt > 0f) {
            val stepTime = min(remainingDt, substepDt)
            integrateSubstep(stepTime, throttle, steering, isBraking)
            remainingDt -= stepTime
        }
        return state
    }

    private fun integrateSubstep(
        dt: Float,
        throttle: Float,
        steering: Float,
        isBraking: Boolean
    ) {
        val s = state
        val mu = surfacePreset.frictionCoefficient
        val crr = surfacePreset.rollingResistance

        // 1. Resolve normalized motor command efforts
        val leftEffort: Float
        val rightEffort: Float
        if (isBraking) {
            leftEffort = 0.0f
            rightEffort = 0.0f
        } else if (abs(throttle) < 0.02f && abs(steering) > 0.02f) {
            // Pure in-place pivot turn (skid-steer counter-rotation)
            // Left steer (steering < 0) -> Left side reverse, Right side forward
            // Right steer (steering > 0) -> Left side forward, Right side reverse
            leftEffort = steering
            rightEffort = -steering
        } else {
            leftEffort = (throttle + steering * 0.50f).coerceIn(-1.0f, 1.0f)
            rightEffort = (throttle - steering * 0.50f).coerceIn(-1.0f, 1.0f)
        }

        // 2. Motor dynamics & wheel surface velocities
        val targetWheelSpeedL = leftEffort * maxNoLoadWheelSpeed
        val targetWheelSpeedR = rightEffort * maxNoLoadWheelSpeed

        // Wheel response rate (rotational inertia of wheels + TT motor back-EMF)
        val wheelResponseRate = 18.0f // rad/s response time
        var newWheelSpeedL = s.wheelSpeedL + (targetWheelSpeedL - s.wheelSpeedL) * (wheelResponseRate * dt).coerceAtMost(1.0f)
        var newWheelSpeedR = s.wheelSpeedR + (targetWheelSpeedR - s.wheelSpeedR) * (wheelResponseRate * dt).coerceAtMost(1.0f)

        if (isBraking) {
            val brakeDecel = 12.0f * dt
            newWheelSpeedL = approachZero(newWheelSpeedL, brakeDecel)
            newWheelSpeedR = approachZero(newWheelSpeedR, brakeDecel)
        }

        // 3. Normal forces and Coulomb friction limits
        val normalForcePerSide = (vehicleMass * gravity) / 2.0f
        val maxFrictionForce = mu * normalForcePerSide

        // 4. Differential drive kinematics & ground traction
        // Current vehicle longitudinal speed along vehicle heading
        // In OpenGL frame: heading vector = (-sin(yaw), -cos(yaw))
        val fwdX = -sin(s.yawRad)
        val fwdZ = -cos(s.yawRad)
        val currentLinearSpeed = s.vx * fwdX + s.vz * fwdZ

        // Tractive forces generated by each side's wheel-ground contact
        // When vehicle turns left (angularVelocity > 0), right side swings forward (+omega * B/2), left side swings backward (-omega * B/2)
        val slipThreshold = 0.05f
        val diffL = newWheelSpeedL - (currentLinearSpeed - s.angularVelocity * trackWidth * 0.5f)
        val diffR = newWheelSpeedR - (currentLinearSpeed + s.angularVelocity * trackWidth * 0.5f)

        val rawForceL = diffL * 15.0f
        val rawForceR = diffR * 15.0f

        val tractiveForceL = rawForceL.coerceIn(-maxFrictionForce, maxFrictionForce)
        val tractiveForceR = rawForceR.coerceIn(-maxFrictionForce, maxFrictionForce)

        // Calculate slip ratios
        val slipRatioL = if (abs(newWheelSpeedL) > slipThreshold || abs(currentLinearSpeed) > slipThreshold) {
            abs(diffL) / max(abs(newWheelSpeedL), abs(currentLinearSpeed))
        } else 0.0f

        val slipRatioR = if (abs(newWheelSpeedR) > slipThreshold || abs(currentLinearSpeed) > slipThreshold) {
            abs(diffR) / max(abs(newWheelSpeedR), abs(currentLinearSpeed))
        } else 0.0f

        // 5. Total forces and torques acting on chassis
        val totalDriveForce = tractiveForceL + tractiveForceR

        // Resistances: Rolling resistance + Aerodynamic drag
        val rollingResistanceForce = crr * vehicleMass * gravity * if (abs(currentLinearSpeed) > 0.01f) {
            if (currentLinearSpeed > 0f) 1.0f else -1.0f
        } else 0.0f

        val aeroDrag = 0.35f * currentLinearSpeed * abs(currentLinearSpeed)
        val brakingForce = if (isBraking && abs(currentLinearSpeed) > 0.01f) {
            val brakeSign = if (currentLinearSpeed > 0f) 1.0f else -1.0f
            brakeSign * 2.0f * maxFrictionForce
        } else 0.0f

        val netLongitudinalForce = totalDriveForce - rollingResistanceForce - aeroDrag - brakingForce
        val longitudinalAccel = netLongitudinalForce / vehicleMass

        // Angular torque: Right side pushing forward harder produces positive yaw moment (counter-clockwise / left turn)
        val driveTorque = (tractiveForceR - tractiveForceL) * (trackWidth * 0.5f)
        val angularDamping = s.angularVelocity * (0.045f * (mu / 0.85f))
        val netTorque = driveTorque - angularDamping
        val angularAccel = netTorque / yawInertia

        // 6. Integrate velocities
        var newLinearSpeed = currentLinearSpeed + longitudinalAccel * dt
        if (isBraking && currentLinearSpeed * newLinearSpeed < 0f) {
            newLinearSpeed = 0.0f // Zero-crossing clamp under active braking
        }

        var newAngularVelocity = s.angularVelocity + angularAccel * dt

        // When no steering effort and vehicle nearly stopped, damp yaw
        if (abs(steering) < 0.05f && abs(newAngularVelocity) < 0.1f) {
            newAngularVelocity = approachZero(newAngularVelocity, 1.5f * dt)
        }
        if (abs(throttle) < 0.05f && !isBraking && abs(newLinearSpeed) < 0.02f) {
            newLinearSpeed = approachZero(newLinearSpeed, 0.4f * dt)
        }

        // 7. Integrate orientation and position
        var newYawRad = s.yawRad + newAngularVelocity * dt
        // Normalize yaw to [-PI, PI]
        while (newYawRad > Math.PI) newYawRad -= (2 * Math.PI).toFloat()
        while (newYawRad < -Math.PI) newYawRad += (2 * Math.PI).toFloat()

        val newFwdX = -sin(newYawRad)
        val newFwdZ = -cos(newYawRad)

        var newVx = newLinearSpeed * newFwdX
        var newVz = newLinearSpeed * newFwdZ

        var newX = s.x + newVx * dt
        var newZ = s.z + newVz * dt

        // 8. Obstacle & Arena Boundary Collision Resolution
        var isColliding = false

        // Check arena perimeter
        if (newX - collisionRadius < arenaMinX) {
            newX = arenaMinX + collisionRadius
            if (newVx < 0f) newVx = -newVx * 0.2f // Inelastic bounce
            isColliding = true
        } else if (newX + collisionRadius > arenaMaxX) {
            newX = arenaMaxX - collisionRadius
            if (newVx > 0f) newVx = -newVx * 0.2f
            isColliding = true
        }

        if (newZ - collisionRadius < arenaMinZ) {
            newZ = arenaMinZ + collisionRadius
            if (newVz < 0f) newVz = -newVz * 0.2f
            isColliding = true
        } else if (newZ + collisionRadius > arenaMaxZ) {
            newZ = arenaMaxZ - collisionRadius
            if (newVz > 0f) newVz = -newVz * 0.2f
            isColliding = true
        }

        // Check static internal obstacles
        for (obs in obstacles) {
            val res = obs.resolveCollision(newX, newZ, collisionRadius)
            if (res != null) {
                newX += res.normalX * res.penetrationDepth
                newZ += res.normalZ * res.penetrationDepth
                // Reflect velocity along collision normal
                val vDotN = newVx * res.normalX + newVz * res.normalZ
                if (vDotN < 0f) {
                    val restitution = 0.2f
                    newVx -= (1.0f + restitution) * vDotN * res.normalX
                    newVz -= (1.0f + restitution) * vDotN * res.normalZ
                }
                isColliding = true
            }
        }

        // Update linear speed after potential collision impulse
        val postColLinearSpeed = newVx * newFwdX + newVz * newFwdZ

        // 9. Integrate wheel rotation angles for 3D animation
        // 1 full revolution = 2 * PI * r = 0.2073m
        val degPerMeter = (360.0f / (2.0f * Math.PI.toFloat() * wheelRadius))
        val newWheelAngleL = (s.wheelAngleL + newWheelSpeedL * degPerMeter * dt) % 360.0f
        val newWheelAngleR = (s.wheelAngleR + newWheelSpeedR * degPerMeter * dt) % 360.0f

        // 10. Chassis Simulated Body-Response (Inertial Visual Cues)
        // Rigid double-deck acrylic car has NO mechanical suspension springs/dampers.
        // Pitch and roll effects are strictly visual inertial response cues (simulating tire rubber deflection
        // and visual motion cues) to improve digital twin feedback in the 3D viewport.
        val targetPitch = when {
            isBraking -> 3.2f // Visual dive cue under braking
            longitudinalAccel > 0.5f -> (-longitudinalAccel * 0.65f).coerceIn(-3.0f, 0.0f) // Visual squat cue
            longitudinalAccel < -0.5f -> (-longitudinalAccel * 0.55f).coerceIn(0.0f, 3.5f) // Reverse dive cue
            else -> 0.0f
        }

        val lateralAccel = postColLinearSpeed * newAngularVelocity
        val targetRoll = (-lateralAccel * 1.8f).coerceIn(-3.5f, 3.5f) // Visual cornering lean cue

        val pitchDamp = (12.0f * dt).coerceIn(0.0f, 1.0f)
        val rollDamp = (12.0f * dt).coerceIn(0.0f, 1.0f)
        val rawPitch = s.pitchDeg + (targetPitch - s.pitchDeg) * pitchDamp
        val rawRoll = s.rollDeg + (targetRoll - s.rollDeg) * rollDamp

        val newPitchDeg = if (rawPitch.isNaN() || rawPitch.isInfinite()) 0.0f else rawPitch.coerceIn(-5.0f, 5.0f)
        val newRollDeg = if (rawRoll.isNaN() || rawRoll.isInfinite()) 0.0f else rawRoll.coerceIn(-5.0f, 5.0f)

        // Store updated state
        state = VehiclePhysicsState(
            x = newX,
            z = newZ,
            yawRad = newYawRad,
            yawDeg = Math.toDegrees(newYawRad.toDouble()).toFloat(),
            vx = newVx,
            vz = newVz,
            linearSpeed = postColLinearSpeed,
            angularVelocity = newAngularVelocity,
            wheelSpeedL = newWheelSpeedL,
            wheelSpeedR = newWheelSpeedR,
            wheelAngleL = newWheelAngleL,
            wheelAngleR = newWheelAngleR,
            slipRatioL = slipRatioL,
            slipRatioR = slipRatioR,
            pitchDeg = newPitchDeg,
            rollDeg = newRollDeg,
            isColliding = isColliding,
            surfacePreset = surfacePreset
        )
    }

    private fun approachZero(value: Float, step: Float): Float {
        return when {
            value > step -> value - step
            value < -step -> value + step
            else -> 0.0f
        }
    }
}
