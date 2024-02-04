plugins {
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.preprocessorRoot)
}

preprocess {
    val fabric12004 = createNode("1.20.4-fabric", 12004, "yarn")
    val forge12004 = createNode("1.20.4-forge", 12004, "yarn")
    val neoforge12004 = createNode("1.20.4-neoforge", 12004, "yarn")

    forge12004.link(fabric12004)
    neoforge12004.link(forge12004, file("versions/forge-neoforge"))
}