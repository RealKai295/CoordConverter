package com.coordconvert.xaero;

import com.coordconvert.conversion.BlockPosition;
import com.coordconvert.conversion.GameDimension;

public record WaypointMatch(String name, BlockPosition position, GameDimension sourceDimension) {
}
