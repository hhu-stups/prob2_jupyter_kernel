package de.prob2.jupyter;

import de.prob.exception.ProBError;

import org.jetbrains.annotations.NotNull;

public final class WithSourceCodeException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	private final @NotNull String sourceCode;
	
	public WithSourceCodeException(final @NotNull String sourceCode, final @NotNull ProBError cause) {
		super(cause.toString(), cause);
		
		this.sourceCode = sourceCode;
	}
	
	public @NotNull String getSourceCode() {
		return this.sourceCode;
	}
	
	@Override
	public synchronized @NotNull ProBError getCause() {
		return (ProBError)super.getCause();
	}
}
