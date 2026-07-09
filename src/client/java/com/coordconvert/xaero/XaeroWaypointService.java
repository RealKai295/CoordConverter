package com.coordconvert.xaero;

import com.coordconvert.conversion.BlockPosition;
import com.coordconvert.conversion.GameDimension;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;
import java.util.Optional;

public final class XaeroWaypointService {
	private XaeroWaypointService() {
	}

	public static boolean isXaeroLoaded() {
		return FabricLoader.getInstance().isModLoaded("xaerominimap")
			|| FabricLoader.getInstance().isModLoaded("xaerominimapfair");
	}

	public static List<String> listWaypointNames() {
		if (!isXaeroLoaded()) {
			return List.of();
		}
		return XaeroReflection.listWaypointNames();
	}

	public static Optional<WaypointMatch> resolveWaypoint(String waypointName) {
		if (!isXaeroLoaded()) {
			return Optional.empty();
		}
		return XaeroReflection.resolveWaypoint(waypointName);
	}

	public static Optional<String> createConvertedWaypoint(
		String sourceName,
		BlockPosition sourcePosition,
		GameDimension sourceDimension,
		GameDimension targetDimension
	) {
		if (!isXaeroLoaded()) {
			return Optional.of("Xaero's Minimap is required to create converted waypoints.");
		}
		return XaeroReflection.createConvertedWaypoint(sourceName, sourcePosition, sourceDimension, targetDimension);
	}
}
