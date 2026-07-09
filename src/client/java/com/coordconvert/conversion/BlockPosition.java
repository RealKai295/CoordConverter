package com.coordconvert.conversion;

public record BlockPosition(int x, int y, int z) {
	public BlockPosition convertedTo(GameDimension source, GameDimension target) {
		double factor = source.coordinateScale() / target.coordinateScale();
		return new BlockPosition(
			(int) Math.floor(this.x * factor),
			this.y,
			(int) Math.floor(this.z * factor)
		);
	}

	public String formatted() {
		return this.x + " " + this.y + " " + this.z;
	}
}
