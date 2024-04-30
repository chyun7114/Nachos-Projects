package nachos.machine;

import nachos.security.Privilege;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * <tt>Interrupt</tt> Ŭ������ ������ ���ͷ�Ʈ �ϵ��� ���ķ���Ʈ�մϴ�. �ϵ����� ���ͷ�Ʈ�� Ȱ��ȭ�ϰų� ��Ȱ��ȭ�ϴ�
 * ���(<tt>setStatus()</tt>)�� �����մϴ�.
 *
 * <p>
 * �ϵ��� ���ķ���Ʈ�ϱ� ���ؼ��� �ϵ���� ��ġ�� �߻���ų �� �ִ� ��� ���� ���� ���ͷ�Ʈ�� �׵��� �߻��� ���� �����ؾ� �մϴ�.
 *
 * <p>
 * �� ����� �ùķ��̼ǵ� �ð��� �����մϴ�. �ùķ��̼ǵ� �ð��� ������ �߻��� ���� ����˴ϴ�:
 * <ul>
 * <li>������ ��Ȱ��ȭ�Ǿ��� ���ͷ�Ʈ�� Ȱ��ȭ�� ��
 * <li>MIPS ����� ����� ��
 * </ul>
 *
 * <p>
 * ���������, ���� �ϵ����� �޸� ���ͷ�Ʈ(Ÿ�� �����̽� ���ؽ�Ʈ ����ġ ����)�� ���ͷ�Ʈ�� Ȱ��ȭ�� ���� �ڵ� ��𿡼���
 * �߻��� �� ����, �ùķ��̼ǵ� �ð��� ����Ǵ� �ڵ��� Ư�� ��ġ������ �߻��մϴ� (�ϵ���� �ùķ��̼��� ���ͷ�Ʈ �ڵ鷯�� ȣ����
 * �ð��� �Ǳ� �����Դϴ�).
 *
 * <p>
 * �̴� �ùٸ��� �ʰ� ����ȭ�� �ڵ尡 �� �ϵ���� �ùķ��̼ǿ����� �� �۵��� �� ������(����ȭ�� Ÿ�� �����̽��� ����), ����
 * �ϵ������� �۵����� ���� �� ������ �ǹ��մϴ�. �׷��� ���ݽ�(Nachos)�� ������ ���α׷��� ������ ������ ���� �׻� ����������
 * �������� �ùٸ��� ����ȭ�� �ڵ带 �ۼ��ؾ� �մϴ�.
 */
public final class Interrupt {
	/**
	 * �� ���ͷ�Ʈ ��Ʈ�ѷ��� �Ҵ��մϴ�.
	 *
	 * @param	privilege      	���ʽ� ��迡 ���� �����ִ� ������ ĸ��ȭ�մϴ�.
	 */
	public Interrupt(Privilege privilege) {
		System.out.print("interrupt");

		this.privilege = privilege;
		privilege.interrupt = new InterruptPrivilege();

		enabled = false;
		pending = new TreeSet<PendingInterrupt>();
	}

	/**
	 * ���ͷ�Ʈ�� Ȱ��ȭ�մϴ�. �� �޼���� <tt>setStatus(true)</tt>�� ������ ȿ���� �����ϴ�.
	 */
	public void enable() {
		setStatus(true);
	}

	/**
	 * ���ͷ�Ʈ�� ��Ȱ��ȭ�ϰ� ������ ���ͷ�Ʈ ���¸� ��ȯ�մϴ�. �� �޼���� <tt>setStatus(false)</tt>�� ������ ȿ����
	 * �����ϴ�.
	 *
	 * @return	���ͷ�Ʈ�� Ȱ��ȭ�Ǿ� �־��ٸ� <tt>true</tt>�� ��ȯ�մϴ�.
	 */
	public boolean disable() {
		return setStatus(false);
	}

	/**
	 * ���ͷ�Ʈ�� ������ ���·� �����մϴ�. �� �޼���� <tt>setStatus(<i>status</i>)</tt>�� ������ ȿ���� �����ϴ�.
	 *
	 * @param	status	���ͷ�Ʈ�� Ȱ��ȭ�Ϸ��� <tt>true</tt>.
	 */
	public void restore(boolean status) {
		setStatus(status);
	}

	/**
	 * ���ͷ�Ʈ ���¸� Ȱ��ȭ(<tt>true</tt>) �Ǵ� ��Ȱ��ȭ(<tt>false</tt>)�� �����ϰ� ���� ���¸� ��ȯ�մϴ�. ���ͷ�Ʈ ���°�
	 * ��Ȱ��ȭ�� ���¿��� Ȱ��ȭ�� ���·� ����Ǹ� �ùķ��̼ǵ� �ð��� ����˴ϴ�.
	 *
	 * @param	status		���ͷ�Ʈ�� Ȱ��ȭ�Ϸ��� <tt>true</tt>.
	 * @return			���ͷ�Ʈ�� Ȱ��ȭ�Ǿ� �־��ٸ� <tt>true</tt>�� ��ȯ�մϴ�.
	 */
	public boolean setStatus(boolean status) {
		boolean oldStatus = enabled;
		enabled = status;

		if (oldStatus == false && status == true)
			tick(true);

		return oldStatus;
	}

	/**
	 * ���ͷ�Ʈ�� Ȱ��ȭ�Ǿ� �ִ��� �׽�Ʈ�մϴ�.
	 *
	 * @return	���ͷ�Ʈ�� Ȱ��ȭ�Ǿ� ������ <tt>true</tt>.
	 */
	public boolean enabled() {
		return enabled;
	}

	/**
	 * ���ͷ�Ʈ�� ��Ȱ��ȭ�Ǿ� �ִ��� �׽�Ʈ�մϴ�.
	 *
	 * @return ���ͷ�Ʈ�� ��Ȱ��ȭ�Ǿ� ������ <tt>true</tt>.
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
