package de.prob2.jupyter;

import com.google.common.base.MoreObjects;

import org.jetbrains.annotations.NotNull;

/**
 * A string with a known position relative to a known origin or larger string.
 * This class is used while splitting/parsing commands and arguments to track the position of individual argument strings within the source code.
 */
public final class PositionedString {
	private final @NotNull String value;
	private final int startPosition;
	
	public PositionedString(final @NotNull String value, final int startPosition) {
		super();
		
		this.value = value;
		this.startPosition = startPosition;
	}
	
	/**
	 * Return this string's text value.
	 * 
	 * @return this string's text value
	 */
	public @NotNull String getValue() {
		return this.value;
	}
	
	/**
	 * Return the position of the start of this string.
	 * 
	 * @return the position of the start of this string relative to a known origin
	 */
	public int getStartPosition() {
		return this.startPosition;
	}
	
	/**
	 * Return the position of the end of this string.
	 * This is equivalent to the start position plus the length of the string.
	 * 
	 * @return the position of the end of this string
	 */
	public int getEndPosition() {
		return this.getStartPosition() + this.getValue().length();
	}
	
	/**
	 * <p>
	 * Return a substring of this string, with the position adjusted accordingly.
	 * This method behaves like {@link String#substring(int, int)}.
	 * </p>
	 * <p>
	 * {@code beginIndex} and {@code endIndex} are relative to the underlying text value and are not affected by this string's position information.
	 * </p>
	 * 
	 * @param beginIndex the beginning index of the substring, inclusive
	 * @param endIndex the end index of the substring, exclusive
	 * @return the substring of this string, from {@code beginIndex} to {@code endIndex}, with the position increased by {@code beginIndex}
	 */
	public PositionedString substring(final int beginIndex, final int endIndex) {
		return new PositionedString(this.getValue().substring(beginIndex, endIndex), this.getStartPosition() + beginIndex);
	}
	
	/**
	 * <p>
	 * Return a substring of this string, with the position adjusted accordingly.
	 * This method behaves like {@link String#substring(int)}.
	 * </p>
	 * <p>
	 * {@code beginIndex} is relative to the underlying text value and is not affected by this string's position information.
	 * </p>
	 *
	 * @param beginIndex the beginning index of the substring, inclusive
	 * @return the substring of this string, from {@code beginIndex} to the end of the string, with the position increased by {@code beginIndex}
	 */
	public PositionedString substring(final int beginIndex) {
		return this.substring(beginIndex, this.getValue().length());
	}
	
	/**
	 * Return a text representation of this string for debugging purposes.
	 * This method does <em>not</em> return the string's underyling text value -
	 * {@link #getValue()} should be used for that purpose.
	 * 
	 * @return a text representation of this string for debugging
	 */
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("value", this.getValue())
			.add("startPosition", this.getStartPosition())
			.toString();
	}
}
