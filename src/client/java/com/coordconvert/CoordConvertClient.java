package com.coordconvert;

import com.coordconvert.command.ConvertCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoordConvertClient implements ClientModInitializer {
	public static final String MOD_ID = "coordconvert";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> ConvertCommand.register(dispatcher));
		LOGGER.info("CoordConvert loaded. Use /convert <waypoint> <dimension> or /convert <x> <y> <z> <dimension>.");
	}
}
