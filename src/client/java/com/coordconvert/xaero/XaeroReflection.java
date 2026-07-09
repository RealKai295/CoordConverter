package com.coordconvert.xaero;

import com.coordconvert.CoordConvertClient;
import com.coordconvert.conversion.BlockPosition;
import com.coordconvert.conversion.GameDimension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

final class XaeroReflection {
	private static final int WAYPOINT_COLOR = 11;

	private XaeroReflection() {
	}

	static Optional<WaypointMatch> resolveWaypoint(String waypointName) {
		Object root = getCurrentRootContainer().orElse(null);
		if (root == null) {
			return Optional.empty();
		}

		String normalizedName = waypointName.trim();
		for (Object world : iterate(getAllWorlds(root))) {
			GameDimension sourceDimension = resolveDimension(world);
			if (sourceDimension == null) {
				continue;
			}

			for (Object waypointSet : iterate(invoke(world, "getIterableWaypointSets"))) {
				for (Object waypoint : iterate(invoke(waypointSet, "getWaypoints"))) {
					if (isWaypointSkipped(waypoint)) {
						continue;
					}

					String name = invokeString(waypoint, "getName");
					if (name != null && name.equalsIgnoreCase(normalizedName)) {
						return Optional.of(new WaypointMatch(
							name,
							new BlockPosition(
								invokeInt(waypoint, "getX"),
								invokeInt(waypoint, "getY"),
								invokeInt(waypoint, "getZ")
							),
							sourceDimension
						));
					}
				}
			}
		}

		return Optional.empty();
	}

	static List<String> listWaypointNames() {
		Object root = getCurrentRootContainer().orElse(null);
		if (root == null) {
			return List.of();
		}

		Set<String> names = new LinkedHashSet<>();
		for (Object world : iterate(getAllWorlds(root))) {
			for (Object waypointSet : iterate(invoke(world, "getIterableWaypointSets"))) {
				for (Object waypoint : iterate(invoke(waypointSet, "getWaypoints"))) {
					if (isWaypointSkipped(waypoint)) {
						continue;
					}

					String name = invokeString(waypoint, "getName");
					if (name != null && !name.isBlank()) {
						names.add(name);
					}
				}
			}
		}

		return List.copyOf(names);
	}

	static Optional<String> createConvertedWaypoint(
		String sourceName,
		BlockPosition sourcePosition,
		GameDimension sourceDimension,
		GameDimension targetDimension
	) {
		Object session = getMinimapSession().orElse(null);
		Object root = getCurrentRootContainer().orElse(null);
		if (session == null || root == null) {
			return Optional.of("Xaero's Minimap is not ready yet. Open your world and try again.");
		}

		Object targetWorld = findWorldForDimension(root, targetDimension);
		if (targetWorld == null) {
			return Optional.of("Could not find a Xaero sub-world for " + targetDimension.displayName() + ".");
		}

		BlockPosition converted = sourcePosition.convertedTo(sourceDimension, targetDimension);
		String waypointName = buildWaypointName(sourceName, targetDimension);
		Object waypointSet = invoke(targetWorld, "getCurrentWaypointSet");
		if (waypointSet == null) {
			return Optional.of("No active Xaero waypoint set is available for " + targetDimension.displayName() + ".");
		}

		try {
			Object waypoint = createWaypoint(
				converted.x(),
				converted.y(),
				converted.z(),
				waypointName,
				buildInitials(waypointName),
				WAYPOINT_COLOR
			);
			removeDuplicateWaypoint(waypointSet, waypointName);
			invoke(waypointSet, "add", waypoint);
			saveWorld(session, targetWorld);
		} catch (ReflectiveOperationException exception) {
			CoordConvertClient.LOGGER.error("Failed to create converted Xaero waypoint", exception);
			return Optional.of("Failed to create the Xaero waypoint. Check the log for details.");
		}

		return Optional.empty();
	}

	private static Optional<Object> getMinimapSession() {
		try {
			Class<?> builtInHudModules = Class.forName("xaero.hud.minimap.BuiltInHudModules");
			Field minimapField = builtInHudModules.getField("MINIMAP");
			Object hudModule = minimapField.get(null);
			if (hudModule == null) {
				return Optional.empty();
			}

			Object session = invoke(hudModule, "getCurrentSession");
			if (session != null) {
				return Optional.of(session);
			}
		} catch (ReflectiveOperationException exception) {
			CoordConvertClient.LOGGER.debug("BuiltInHudModules session lookup failed, trying legacy API", exception);
		}

		try {
			Class<?> legacySessionClass = Class.forName("xaero.common.XaeroMinimapSession");
			Method getCurrentSession = legacySessionClass.getMethod("getCurrentSession");
			Object session = getCurrentSession.invoke(null);
			return Optional.ofNullable(session);
		} catch (ReflectiveOperationException exception) {
			CoordConvertClient.LOGGER.debug("Legacy Xaero session lookup failed", exception);
			return Optional.empty();
		}
	}

	private static Optional<Object> getCurrentRootContainer() {
		Object session = getMinimapSession().orElse(null);
		if (session == null) {
			return Optional.empty();
		}

		Object worldManager = invoke(session, "getWorldManager");
		if (worldManager != null) {
			Object root = invoke(worldManager, "getCurrentRootContainer");
			if (root != null) {
				return Optional.of(root);
			}
		}

		Object legacyManager = invoke(session, "getWaypointsManager");
		if (legacyManager == null) {
			return Optional.empty();
		}

		Object currentWorld = invoke(legacyManager, "getCurrentWorld");
		if (currentWorld == null) {
			return Optional.empty();
		}

		Object container = invoke(currentWorld, "getContainer");
		if (container == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(invoke(container, "getRoot"));
	}

	private static Iterable<?> getAllWorlds(Object root) {
		Iterable<?> worlds = asIterable(invoke(root, "getAllWorldsIterable"));
		if (worlds != null) {
			return worlds;
		}

		List<Object> collected = new ArrayList<>();
		Iterable<?> subContainers = asIterable(invoke(root, "getSubContainers"));
		if (subContainers != null) {
			for (Object subContainer : subContainers) {
				Iterable<?> containerWorlds = asIterable(invoke(subContainer, "getWorlds"));
				if (containerWorlds != null) {
					for (Object world : containerWorlds) {
						collected.add(world);
					}
				}
			}
		}

		return collected;
	}

	private static Object findWorldForDimension(Object root, GameDimension dimension) {
		for (Object world : iterate(getAllWorlds(root))) {
			GameDimension worldDimension = resolveDimension(world);
			if (worldDimension == dimension) {
				return world;
			}
		}
		return null;
	}

	private static GameDimension resolveDimension(Object world) {
		Object dimensionKey = invoke(world, "getDimId");
		if (!(dimensionKey instanceof ResourceKey<?> resourceKey)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		ResourceKey<Level> levelKey = (ResourceKey<Level>) resourceKey;
		return GameDimension.fromLevelKey(levelKey).orElse(null);
	}

	private static boolean isWaypointSkipped(Object waypoint) {
		if (invokeBoolean(waypoint, "isEffectivelyDeleted")) {
			return true;
		}

		return invokeBoolean(waypoint, "isThirdPartyDeleted");
	}

	private static void removeDuplicateWaypoint(Object waypointSet, String waypointName) throws ReflectiveOperationException {
		for (Object existing : iterate(invoke(waypointSet, "getWaypoints"))) {
			String name = invokeString(existing, "getName");
			if (name != null && name.equalsIgnoreCase(waypointName)) {
				invoke(waypointSet, "remove", existing);
				return;
			}
		}
	}

	private static Object createWaypoint(int x, int y, int z, String name, String initials, int color)
		throws ReflectiveOperationException {
		Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
		Constructor<?> constructor = waypointClass.getConstructor(
			int.class,
			int.class,
			int.class,
			String.class,
			String.class,
			int.class
		);
		return constructor.newInstance(x, y, z, name, initials, color);
	}

	private static void saveWorld(Object session, Object world) throws ReflectiveOperationException {
		Object worldManagerIo = invoke(session, "getWorldManagerIO");
		if (worldManagerIo != null) {
			invoke(worldManagerIo, "saveWorld", world);
			return;
		}

		Object settings = invokeStatic(Class.forName("xaero.minimap.XaeroMinimap"), "instance");
		if (settings != null) {
			Object modSettings = invoke(settings, "getSettings");
			Object waypointsManager = invoke(session, "getWaypointsManager");
			if (modSettings != null && waypointsManager != null) {
				invoke(modSettings, "saveAllWaypoints", waypointsManager);
			}
		}
	}

	private static Iterable<?> iterate(Object value) {
		return asIterable(value) == null ? List.of() : asIterable(value);
	}

	private static Iterable<?> asIterable(Object value) {
		return value instanceof Iterable<?> iterable ? iterable : null;
	}

	private static Object invoke(Object target, String methodName, Object... args) {
		if (target == null) {
			return null;
		}

		try {
			Class<?>[] parameterTypes = new Class<?>[args.length];
			for (int index = 0; index < args.length; index++) {
				parameterTypes[index] = args[index].getClass();
			}

			Method method = target.getClass().getMethod(methodName, parameterTypes);
			return method.invoke(target, args);
		} catch (ReflectiveOperationException exception) {
			try {
				for (Method method : target.getClass().getMethods()) {
					if (!method.getName().equals(methodName) || method.getParameterCount() != args.length) {
						continue;
					}

					return method.invoke(target, args);
				}
			} catch (ReflectiveOperationException ignored) {
				CoordConvertClient.LOGGER.debug("Failed reflective call {} on {}", methodName, target.getClass().getName(), exception);
			}
		}

		return null;
	}

	private static Object invokeStatic(Class<?> targetClass, String methodName) {
		try {
			Method method = targetClass.getMethod(methodName);
			return method.invoke(null);
		} catch (ReflectiveOperationException exception) {
			CoordConvertClient.LOGGER.debug("Failed static reflective call {} on {}", methodName, targetClass.getName(), exception);
			return null;
		}
	}

	private static String invokeString(Object target, String methodName) {
		Object value = invoke(target, methodName);
		return value instanceof String stringValue ? stringValue : null;
	}

	private static int invokeInt(Object target, String methodName) {
		Object value = invoke(target, methodName);
		return value instanceof Number number ? number.intValue() : 0;
	}

	private static boolean invokeBoolean(Object target, String methodName) {
		try {
			Method method = target.getClass().getMethod(methodName);
			Object value = method.invoke(target);
			return value instanceof Boolean booleanValue && booleanValue;
		} catch (NoSuchMethodException ignored) {
			return false;
		} catch (ReflectiveOperationException exception) {
			CoordConvertClient.LOGGER.debug("Failed reflective boolean call {} on {}", methodName, target.getClass().getName(), exception);
			return false;
		}
	}

	private static String buildWaypointName(String sourceName, GameDimension targetDimension) {
		String suffix = " (" + targetDimension.displayName() + ")";
		String trimmedSource = sourceName == null ? "Converted" : sourceName.trim();
		if (trimmedSource.isEmpty()) {
			trimmedSource = "Converted";
		}

		String combined = trimmedSource + suffix;
		if (combined.length() <= 64) {
			return combined;
		}
		return trimmedSource.substring(0, Math.max(1, 64 - suffix.length())) + suffix;
	}

	private static String buildInitials(String name) {
		StringBuilder initials = new StringBuilder();
		for (int index = 0; index < name.length() && initials.length() < 2; index++) {
			char current = name.charAt(index);
			if (Character.isLetterOrDigit(current)) {
				initials.append(Character.toUpperCase(current));
			}
		}

		if (initials.isEmpty()) {
			return "CV";
		}
		return initials.toString();
	}
}
