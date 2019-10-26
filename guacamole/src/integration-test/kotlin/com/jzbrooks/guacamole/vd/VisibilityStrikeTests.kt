package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.BaselineTest
import java.nio.file.Path

class VisibilityStrikeTests : BaselineTest(
        Path.of("src/integration-test/resources/visibility_strike.xml"),
        Path.of("src/integration-test/resources/baseline/visibility_strike_optimized.xml")
)