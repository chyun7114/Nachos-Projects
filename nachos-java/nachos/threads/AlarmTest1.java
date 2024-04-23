package nachos.threads;    // Add Alarm testing code to the Alarm class

import nachos.machine.Machine;
import nachos.threads.ThreadedKernel;

public class AlarmTest1 {
	public static void alarmTest1 () {
		int durations[] = {1000, 10 * 1000, 100 * 1000};
		long t0, t1;

		for (int d : durations) {
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil(d);
			t1 = Machine.timer().getTime();
			System.out.println("alarmTest1: waited for " + (t1 - t0) + " ticks");
		}
	}

	// Implement more test methods here ...

	// Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
	public static void selfTest () {
		alarmTest1();
		// Invoke your other test methods here ...
	}
}