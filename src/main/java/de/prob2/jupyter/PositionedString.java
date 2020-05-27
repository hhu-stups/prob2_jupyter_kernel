package de.prob2.jupyter;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

public final class PositionedString {
	private final @NotNull String value;
	private final int startPosition;
	
	public PositionedString(final @NotNull String value, final int startPosition) {
		super();
		
		this.value = value;
		this.startPosition = startPosition;
	}
	
	public @NotNull String getValue() {
		return this.value;
	}
	
	public int getStartPosition() {
		return this.startPosition;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("value", this.getValue())
			.add("startPosition", this.getStartPosition())
			.toString();
	}
}
