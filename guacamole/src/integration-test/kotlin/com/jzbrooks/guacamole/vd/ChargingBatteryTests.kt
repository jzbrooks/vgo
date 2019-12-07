package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.BaselineTest
import org.junit.jupiter.api.Disabled
import java.nio.file.Path

@Disabled("Float precision causes the optimized asset to be larger. Re-enable after that's fixed.")
class ChargingBatteryTests : BaselineTest(
        Path.of("src/integration-test/resources/charging_battery.xml"),
        Path.of("src/integration-test/resources/baseline/charging_battery_optimized.xml")
)