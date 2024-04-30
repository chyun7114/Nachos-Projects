package nachos.machine;

import nachos.security.Privilege;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * <tt>Interrupt</tt> 클래스는 저수준 인터럽트 하드웨어를 에뮬레이트합니다. 하드웨어는 인터럽트를 활성화하거나 비활성화하는
 * 방법(<tt>setStatus()</tt>)을 제공합니다.
 *
 * <p>
 * 하드웨어를 에뮬레이트하기 위해서는 하드웨어 장치가 발생시킬 수 있는 모든 보류 중인 인터럽트와 그들이 발생할 때를 추적해야 합니다.
 *
 * <p>
 * 이 모듈은 시뮬레이션된 시간을 추적합니다. 시뮬레이션된 시간은 다음에 발생할 때만 진행됩니다:
 * <ul>
 * <li>이전에 비활성화되었던 인터럽트가 활성화될 때
 * <li>MIPS 명령이 실행될 때
 * </ul>
 *
 * <p>
 * 결과적으로, 실제 하드웨어와 달리 인터럽트(타임 슬라이스 컨텍스트 스위치 포함)는 인터럽트가 활성화된 곳의 코드 어디에서든
 * 발생할 수 없고, 시뮬레이션된 시간이 진행되는 코드의 특정 위치에서만 발생합니다 (하드웨어 시뮬레이션이 인터럽트 핸들러를 호출할
 * 시간이 되기 때문입니다).
 *
 * <p>
 * 이는 올바르지 않게 동기화된 코드가 이 하드웨어 시뮬레이션에서는 잘 작동할 수 있지만(랜덤화된 타임 슬라이스도 포함), 실제
 * 하드웨어에서는 작동하지 않을 수 있음을 의미합니다. 그러나 나쵸스(Nachos)가 귀하의 프로그램이 실제로 실패할 때를 항상 감지하지는
 * 못할지라도 올바르게 동기화된 코드를 작성해야 합니다.
 */
public final class Interrupt {
	/**
	 * 새 인터럽트 컨트롤러를 할당합니다.
	 *
	 * @param	privilege      	나초스 기계에 대한 권한있는 접근을 캡슐화합니다.
	 */
	public Interrupt(Privilege privilege) {
		System.out.print("interrupt");

		this.privilege = privilege;
		privilege.interrupt = new InterruptPrivilege();

		enabled = false;
		pending = new TreeSet<PendingInterrupt>();
	}

	/**
	 * 인터럽트를 활성화합니다. 이 메서드는 <tt>setStatus(true)</tt>와 동일한 효과를 갖습니다.
	 */
	public void enable() {
		setStatus(true);
	}

	/**
	 * 인터럽트를 비활성화하고 이전의 인터럽트 상태를 반환합니다. 이 메서드는 <tt>setStatus(false)</tt>와 동일한 효과를
	 * 갖습니다.
	 *
	 * @return	인터럽트가 활성화되어 있었다면 <tt>true</tt>를 반환합니다.
	 */
	public boolean disable() {
		return setStatus(false);
	}

	/**
	 * 인터럽트를 지정된 상태로 복원합니다. 이 메서드는 <tt>setStatus(<i>status</i>)</tt>와 동일한 효과를 갖습니다.
	 *
	 * @param	status	인터럽트를 활성화하려면 <tt>true</tt>.
	 */
	public void restore(boolean status) {
		setStatus(status);
	}

	/**
	 * 인터럽트 상태를 활성화(<tt>true</tt>) 또는 비활성화(<tt>false</tt>)로 설정하고 이전 상태를 반환합니다. 인터럽트 상태가
	 * 비활성화된 상태에서 활성화된 상태로 변경되면 시뮬레이션된 시간이 진행됩니다.
	 *
	 * @param	status		인터럽트를 활성화하려면 <tt>true</tt>.
	 * @return			인터럽트가 활성화되어 있었다면 <tt>true</tt>를 반환합니다.
	 */
	public boolean setStatus(boolean status) {
		boolean oldStatus = enabled;
		enabled = status;

		if (oldStatus == false && status == true)
			tick(true);

		return oldStatus;
	}

	/**
	 * 인터럽트가 활성화되어 있는지 테스트합니다.
	 *
	 * @return	인터럽트가 활성화되어 있으면 <tt>true</tt>.
	 */
	public boolean enabled() {
		return enabled;
	}

	/**
	 * 인터럽트가 비활성화되어 있는지 테스트합니다.
	 *
	 * @return 인터럽트가 비활성화되어 있으면 <tt>true</tt>.
	 */
	public boolean disabled() {
		return !enabled;
	}

	private void schedule(long when, String type, Runnable handler) {
		Lib.assertTrue(when > 0);

		long time = privilege.stats.totalTicks + when;
		PendingInterrupt toOccur = new PendingInterrupt(time, type, handler);

		Lib.debug(dbgInt,
				"Scheduling the " + type +
						" interrupt handler at time = " + time);

		pending.add(toOccur);
	}

	private void tick(boolean inKernelMode) {
		Stats stats = privilege.stats;

		if (inKernelMode) {
			stats.kernelTicks += Stats.KernelTick;
			stats.totalTicks += Stats.KernelTick;
		}
		else {
			stats.userTicks += Stats.UserTick;
			stats.totalTicks += Stats.UserTick;
		}

		if (Lib.test(dbgInt))
			System.out.println("== Tick " + stats.totalTicks + " ==");

		enabled = false;
		checkIfDue();
		enabled = true;
	}

	private void checkIfDue() {
		long time = privilege.stats.totalTicks;

		Lib.assertTrue(disabled());

		if (Lib.test(dbgInt))
			print();

		if (pending.isEmpty())
			return;

		if (((PendingInterrupt) pending.first()).time > time)
			return;

		Lib.debug(dbgInt, "Invoking interrupt handlers at time = " + time);

		while (!pending.isEmpty() &&
				((PendingInterrupt) pending.first()).time <= time) {
			PendingInterrupt next = (PendingInterrupt) pending.first();
			pending.remove(next);

			Lib.assertTrue(next.time <= time);

			if (privilege.processor != null)
				privilege.processor.flushPipe();

			Lib.debug(dbgInt, "  " + next.type);

			next.handler.run();
		}

		Lib.debug(dbgInt, "  (end of list)");
	}

	private void print() {
		System.out.println("Time: " + privilege.stats.totalTicks
				+ ", interrupts " + (enabled ? "on" : "off"));
		System.out.println("Pending interrupts:");

		for (Iterator i = pending.iterator(); i.hasNext(); ) {
			PendingInterrupt toOccur = (PendingInterrupt) i.next();
			System.out.println("  " + toOccur.type +
					", scheduled at " + toOccur.time);
		}

		System.out.println("  (end of list)");
	}

	private class PendingInterrupt implements Comparable {
		PendingInterrupt(long time, String type, Runnable handler) {
			this.time = time;
			this.type = type;
			this.handler = handler;
			this.id = numPendingInterruptsCreated++;
		}

		public int compareTo(Object o) {
			PendingInterrupt toOccur = (PendingInterrupt) o;

			// can't return 0 for unequal objects, so check all fields
			if (time < toOccur.time)
				return -1;
			else if (time > toOccur.time)
				return 1;
			else if (id < toOccur.id)
				return -1;
			else if (id > toOccur.id)
				return 1;
			else
				return 0;
		}

		long time;
		String type;
		Runnable handler;

		private long id;
	}

	private long numPendingInterruptsCreated = 0;

	private Privilege privilege;

	private boolean enabled;
	private TreeSet<PendingInterrupt> pending;

	private static final char dbgInt = 'i';

	private class InterruptPrivilege implements Privilege.InterruptPrivilege {
		public void schedule(long when, String type, Runnable handler) {
			Interrupt.this.schedule(when, type, handler);
		}

		public void tick(boolean inKernelMode) {
			Interrupt.this.tick(inKernelMode);
		}
	}
}
