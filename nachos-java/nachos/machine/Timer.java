package nachos.machine;

import nachos.security.Privilege;

/**
 * �ϵ���� Ÿ�̸Ӵ� �뷫������ �� 500 Ŭ�� ƽ���� CPU Ÿ�̸� ���ͷ�Ʈ�� �����մϴ�. �̴� Ÿ�� �����̽��� �����ϰų�
 * �����带 Ư�� �Ⱓ ���� ����Ű�� �� ���� �� �ֽ��ϴ�.
 *
 * <p><tt>Timer</tt> Ŭ������ �뷫������ 500 Ŭ�� ƽ�� ����� ������ Ÿ�̸� ���ͷ�Ʈ�� �߻��ϵ��� �����Ͽ� �ϵ����
 * Ÿ�̸Ӹ� ���ķ���Ʈ�մϴ�. ���⿡�� �ణ�� ���������� �����Ƿ� ���ͷ�Ʈ�� ��Ȯ�� �� 500 ƽ���� �߻������� �ʽ��ϴ�.
 */
public final class Timer {
	/**
	 * �� Ÿ�̸Ӹ� �Ҵ��մϴ�.
	 *
	 * @param	privilege      	���ʽ� ��迡 ���� �����ִ� ������ ĸ��ȭ�մϴ�.
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
	 * Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�� ����� �ݹ��� �����մϴ�. Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�� �뷫������ �� 500 Ŭ�� ƽ���� ȣ��˴ϴ�.
	 *
	 * @param	handler		Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�Դϴ�.
	 */
	public void setInterruptHandler(Runnable handler) {
		this.handler = handler;
	}

	/**
	 * ���� �ð��� �����ɴϴ�.
	 *
	 * @return	���ʽ��� ������ ������ Ŭ�� ƽ ���Դϴ�.
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
