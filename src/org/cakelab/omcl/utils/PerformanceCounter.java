package org.cakelab.omcl.utils;

public abstract class PerformanceCounter {

	public static class ResponseTimeMeasure extends PerformanceCounter {

		private long start;
		private long stop;
		private String name;

		public ResponseTimeMeasure(String name) {
			this.name = name;
			start();
		}

		public void start() {
			start = System.currentTimeMillis();
		}

		@Override
		public void stop() {
			stop = System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return name + " took " + (stop - start) + " ms";
		}

	}

	public static PerformanceCounter createReponseTimeMeasure(String name) {
		
		return new ResponseTimeMeasure(name);
	}

	
	public abstract void start();
	public abstract void stop();
	public abstract String toString();


	public String report() {
		stop();
		return toString();
	}

}
