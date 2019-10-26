package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.BaselineTest
import java.nio.file.Path

class SimpleHeartTests : BaselineTest(
        Path.of("src/integration-test/resources/simple_heart.xml"),
        Path.of("src/integration-test/resources/baseline/simple_heart_optimized.xml")
)