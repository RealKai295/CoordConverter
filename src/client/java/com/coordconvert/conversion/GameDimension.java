package com.coordconvert.conversion;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum GameDimension {
	OVERWORLD(Level.OVERWORLD, "Overworld", 1.0D),
	NETHER(Level.NETHER, "Nether", 8.0D),
	END(Level.END, "The End", 1.0D);

	private final ResourceKey<Level> dimensionKey;
	private final String displayName;
	private final double coordinateScale;

	GameDimension(ResourceKey<Level> dimensionKey, String displayName, double coordinateScale) {
		this.dimensionKey = dimensionKey;
		this.displayName = displayName;
		this.coordinateScale = coordinateScale;
	}

	public ResourceKey<Level> dimensionKey() {
		return this.dimensionKey;
	}

	public String displayName() {
		return this.displayName;
	}

	public double coordinateScale() {
		return this.coordinateScale;
	}

	public static List<String> suggestionValues() {
		return List.of("overworld", "nether", "end", "ow", "n", "e");
	}

	public static Optional<GameDimension> parse(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}

		String normalized = raw.trim().toLowerCase(Locale.ROOT);

		return switch (normalized) {
			case "overworld", "ow", "world", "0", "dim0", "dim%0" -> Optional.of(OVERWORLD);
			case "nether", "n", "hell", "-1", "dim-1", "dim%-1" -> Optional.of(NETHER);
			case "end", "e", "the_end", "theend", "1", "dim1", "dim%1" -> Optional.of(END);
			default -> {
				if (normalized.startsWith("minecraft:")) {
					yield fromId(normalized);
				}
				yield fromId("minecraft:" + normalized);
			}
		};
	}

	public static Optional<GameDimension> fromLevelKey(ResourceKey<Level> dimensionKey) {
		for (GameDimension dimension : values()) {
			if (dimension.dimensionKey.equals(dimensionKey)) {
				return Optional.of(dimension);
			}
		}
		return Optional.empty();
	}

	private static Optional<GameDimension> fromId(String id) {
		return switch (id) {
			case "minecraft:overworld" -> Optional.of(OVERWORLD);
			case "minecraft:the_nether" -> Optional.of(NETHER);
			case "minecraft:the_end" -> Optional.of(END);
			default -> Optional.empty();
		};
	}
}
