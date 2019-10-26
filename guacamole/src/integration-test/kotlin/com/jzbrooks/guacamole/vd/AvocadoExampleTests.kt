package com.jzbrooks.guacamole.vd

import com.jzbrooks.guacamole.BaselineTest
import java.nio.file.Path

class AvocadoExampleTests : BaselineTest(
        Path.of("src/integration-test/resources/avocado_example.xml"),
        Path.of("src/integration-test/resources/baseline/avocado_example_optimized.xml")
)