package com.jzbrooks.guacamole.svg

import com.jzbrooks.guacamole.BaselineTest
import java.nio.file.Path

class GuacamoleTests : BaselineTest(
        Path.of("src/integration-test/resources/guacamole.svg"),
        Path.of("src/integration-test/resources/baseline/guacamole_optimized.svg")
)