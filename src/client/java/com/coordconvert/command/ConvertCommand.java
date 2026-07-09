package com.coordconvert.command;

import com.coordconvert.conversion.BlockPosition;
import com.coordconvert.conversion.GameDimension;
import com.coordconvert.util.ConvertMessages;
import com.coordconvert.xaero.WaypointMatch;
import com.coordconvert.xaero.XaeroWaypointService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Optional;

public final class ConvertCommand {
	private static final SuggestionProvider<FabricClientCommandSource> DIMENSION_SUGGESTIONS = (context, builder) -> {
		String remaining = builder.getRemainingLowerCase();
		for (String suggestion : GameDimension.suggestionValues()) {
			if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				builder.suggest(suggestion);
			}
		}
		return builder.buildFuture();
	};

	private static final SuggestionProvider<FabricClientCommandSource> WAYPOINT_SUGGESTIONS = (context, builder) -> {
		String remaining = builder.getRemainingLowerCase();
		for (String waypointName : XaeroWaypointService.listWaypointNames()) {
			if (waypointName.toLowerCase(Locale.ROOT).startsWith(remaining)) {
				if (waypointName.indexOf(' ') >= 0) {
					builder.suggest(StringArgumentType.escapeIfRequired(waypointName));
				} else {
					builder.suggest(waypointName);
				}
			}
		}
		return builder.buildFuture();
	};

	private ConvertCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(
			ClientCommandManager.literal("convert")
				.then(
					ClientCommandManager.argument("x", IntegerArgumentType.integer())
						.then(
							ClientCommandManager.argument("y", IntegerArgumentType.integer())
								.then(
									ClientCommandManager.argument("z", IntegerArgumentType.integer())
										.then(
											ClientCommandManager.argument("dimension", StringArgumentType.word())
												.suggests(DIMENSION_SUGGESTIONS)
												.executes(ConvertCommand::convertCoordinates)
										)
								)
						)
				)
				.then(
					ClientCommandManager.argument("waypoint", StringArgumentType.string())
						.suggests(WAYPOINT_SUGGESTIONS)
						.then(
							ClientCommandManager.argument("dimension", StringArgumentType.word())
								.suggests(DIMENSION_SUGGESTIONS)
								.executes(ConvertCommand::convertWaypoint)
						)
				)
		);
	}

	private static int convertCoordinates(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
		int x = IntegerArgumentType.getInteger(context, "x");
		int y = IntegerArgumentType.getInteger(context, "y");
		int z = IntegerArgumentType.getInteger(context, "z");
		String dimensionArg = StringArgumentType.getString(context, "dimension");

		Optional<GameDimension> targetDimension = GameDimension.parse(dimensionArg);
		if (targetDimension.isEmpty()) {
			context.getSource().sendError(ConvertMessages.unknownDimension(dimensionArg));
			return 0;
		}

		Minecraft client = context.getSource().getClient();
		if (client.player == null || client.level == null) {
			context.getSource().sendError(ConvertMessages.error("You must be in a world", "join a world before converting coordinates"));
			return 0;
		}

		Optional<GameDimension> sourceDimension = GameDimension.fromLevelKey(client.level.dimension());
		if (sourceDimension.isEmpty()) {
			context.getSource().sendError(ConvertMessages.error("Unsupported dimension", "this world type is not supported yet"));
			return 0;
		}

		return executeConversion(
			context,
			"coordinates",
			new BlockPosition(x, y, z),
			sourceDimension.get(),
			targetDimension.get()
		);
	}

	private static int convertWaypoint(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {
		String waypointName = StringArgumentType.getString(context, "waypoint").trim();
		String dimensionArg = StringArgumentType.getString(context, "dimension").trim();

		if (waypointName.isEmpty()) {
			context.getSource().sendError(ConvertMessages.usage());
			return 0;
		}

		Optional<GameDimension> targetDimension = GameDimension.parse(dimensionArg);
		if (targetDimension.isEmpty()) {
			context.getSource().sendError(ConvertMessages.unknownDimension(dimensionArg));
			return 0;
		}

		if (!XaeroWaypointService.isXaeroLoaded()) {
			context.getSource().sendError(ConvertMessages.error(
				"Xaero's Minimap required",
				"install Xaero's Minimap to convert waypoints by name"
			));
			return 0;
		}

		Optional<WaypointMatch> waypoint = XaeroWaypointService.resolveWaypoint(waypointName);
		if (waypoint.isEmpty()) {
			context.getSource().sendError(ConvertMessages.error(
				"Waypoint not found",
				"\"" + waypointName + "\" is not in your current Xaero profile"
			));
			return 0;
		}

		WaypointMatch match = waypoint.get();
		return executeConversion(
			context,
			match.name(),
			match.position(),
			match.sourceDimension(),
			targetDimension.get()
		);
	}

	private static int executeConversion(
		CommandContext<FabricClientCommandSource> context,
		String sourceLabel,
		BlockPosition sourcePosition,
		GameDimension sourceDimension,
		GameDimension targetDimension
	) {
		if (sourceDimension == targetDimension) {
			context.getSource().sendError(ConvertMessages.error(
				"Same dimension",
				"pick a different target than " + sourceDimension.displayName()
			));
			return 0;
		}

		BlockPosition convertedPosition = sourcePosition.convertedTo(sourceDimension, targetDimension);
		Optional<String> creationError = XaeroWaypointService.createConvertedWaypoint(
			sourceLabel,
			sourcePosition,
			sourceDimension,
			targetDimension
		);

		boolean createdWaypoint = creationError.isEmpty();
		if (creationError.isPresent()) {
			context.getSource().sendFeedback(ConvertMessages.waypointCreationWarning(
				creationError.get(),
				true
			));
		}

		context.getSource().sendFeedback(
			ConvertMessages.conversionResult(
				sourceLabel,
				sourcePosition,
				sourceDimension,
				convertedPosition,
				targetDimension,
				createdWaypoint
			)
		);
		return 1;
	}
}
