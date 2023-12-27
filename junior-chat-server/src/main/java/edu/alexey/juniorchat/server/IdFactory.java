package edu.alexey.juniorchat.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

// lazy init singleton
public class IdFactory implements IntSupplier {

	public static IdFactory instance() {
		return Holder.instance;
	}

	private AtomicInteger nextId;

	private IdFactory() {
		this.nextId = new AtomicInteger(1);
	}

	@Override
	public int getAsInt() {
		return nextId.getAndIncrement();
	}

	private static class Holder {
		static final IdFactory instance = new IdFactory();
	}
}
