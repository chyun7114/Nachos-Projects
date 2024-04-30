package nachos.machine;

import nachos.security.Privilege;

/**
 * 하드웨어 타이머는 대략적으로 매 500 클록 틱마다 CPU 타이머 인터럽트를 생성합니다. 이는 타임 슬라이싱을 구현하거나
 * 스레드를 특정 기간 동안 대기시키는 데 사용될 수 있습니다.
 *
 * <p><tt>Timer</tt> 클래스는 대략적으로 500 클록 틱이 경과할 때마다 타이머 인터럽트가 발생하도록 예약하여 하드웨어
 * 타이머를 에뮬레이트합니다. 여기에는 약간의 무작위성이 있으므로 인터럽트가 정확히 매 500 틱마다 발생하지는 않습니다.
 */
public final class Timer {
	/**
	 * 새 타이머를 할당합니다.
	 *
	 * @param	privilege      	나초스 기계에 대한 권한있는 접근을 캡슐화합니다.
	 */
	public Timer(Privilege privilege) {
		System.out.print("timer");

		this.privilege = privilege;

		timerInterrupt = new Runnable() {
			public void run() { timerInterrupt(); }
		};

		autoGraderInterrupt = new Runnable() {
			public void run() {
				Machine.autoGrader().timerInterrupt(Timer.this.privilege,
						lastTimerInterrupt);
			}
		};

		scheduleInterrupt();
	}

	/**
	 * 타이머 인터럽트 핸들러로 사용할 콜백을 설정합니다. 타이머 인터럽트 핸들러는 대략적으로 매 500 클록 틱마다 호출됩니다.
	 *
	 * @param	handler		타이머 인터럽트 핸들러입니다.
	 */
	public void setInterruptHandler(Runnable handler) {
		this.handler = handler;
	}

	/**
	 * 현재 시간을 가져옵니다.
	 *
	 * @return	나초스를 시작한 이후의 클록 틱 수입니다.
	 */
	public long getTime() {
		return privilege.stats.totalTicks;
	}

	private void timerInterrupt() {
		scheduleInterrupt();
		scheduleAutoGraderInterrupt();

		lastTimerInterrupt = getTime();

		if (handler != null)
			handler.run();
	}

	private void scheduleInterrupt() {
		int delay = Stats.TimerTicks;
		int rand = Lib.random(delay/10);
		delay +=  rand - (delay/20);

		privilege.interrupt.schedule(delay, "timer", timerInterrupt);
	}

	private void scheduleAutoGraderInterrupt() {
		privilege.interrupt.schedule(1, "timerAG", autoGraderInterrupt);
	}

	private long lastTimerInterrupt;
	private Runnable timerInterrupt;
	private Runnable autoGraderInterrupt;

	private Privilege privilege;
	private Runnable handler = null;
}
