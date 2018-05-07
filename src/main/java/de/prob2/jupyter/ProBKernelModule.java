package de.prob2.jupyter;

import com.google.inject.AbstractModule;

import de.prob.MainModule;

public final class ProBKernelModule extends AbstractModule {
	@Override
	protected void configure() {
		install(new MainModule());
	}
}
