package com.jzbrooks.guacamole.svg

import com.jzbrooks.guacamole.BaselineTest
import java.nio.file.Path

class SimpleHeartTests : BaselineTest(
        Path.of("src/integration-test/resources/simple_heart.svg"),
        Path.of("src/integration-test/resources/baseline/simple_heart_optimized.svg")
)