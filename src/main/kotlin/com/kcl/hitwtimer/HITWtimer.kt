package com.kcl.hitwtimer

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object HITWtimer : ModInitializer {
    private val logger = LoggerFactory.getLogger("hitwtimer")

	override fun onInitialize() {
		logger.info("HITWtimer initialized (server side stub, main logic is client-only)")
	}
}