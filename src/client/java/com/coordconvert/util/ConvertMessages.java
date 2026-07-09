package com.coordconvert.util;

import com.coordconvert.conversion.BlockPosition;
import com.coordconvert.conversion.GameDimension;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ConvertMessages {
	private static final Style BOLD = Style.EMPTY.withBold(true);

	private ConvertMessages() {
	}

	public static Component error(String message) {
		return Component.literal(message);
	}

	public static Component error(String label, String detail) {
		return Component.literal(label + ": " + detail);
	}

	public static Component usage() {
		return Component.literal("Usage: /convert <waypoint> <dimension> or /convert <x> <y> <z> <dimension>");
	}

	public static Component unknownDimension(String dimensionArg) {
		return Component.literal("Unknown dimension \"" + dimensionArg + "\". Use overworld, nether, or end.");
	}

	public static Component conversionResult(
		String sourceLabel,
		BlockPosition sourcePosition,
		GameDimension sourceDimension,
		BlockPosition convertedPosition,
		GameDimension targetDimension,
		boolean createdWaypoint
	) {
		MutableComponent message = Component.literal("Converted ")
			.append(bold(sourceLabel))
			.append(": ")
			.append(sourceDimension.displayName())
			.append(" ")
			.append(bold(sourcePosition.formatted()))
			.append(" -> ")
			.append(targetDimension.displayName())
			.append(" ")
			.append(bold(convertedPosition.formatted()));

		if (createdWaypoint) {
			message.append(". Added waypoint ")
				.append(bold(sourceLabel + " (" + targetDimension.displayName() + ")"));
		}

		return message;
	}

	public static Component waypointCreationWarning(String detail, boolean showingCoordinates) {
		String message = "Could not save waypoint: " + detail;
		if (showingCoordinates) {
			message += ". Converted coordinates shown below.";
		}
		return Component.literal(message);
	}

	private static MutableComponent bold(String value) {
		return Component.literal(value).withStyle(BOLD);
	}
}
