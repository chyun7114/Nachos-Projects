package nachos.threads;

import nachos.machine.*;

/**
 * A KThread is a thread that can be used to execute Nachos kernel code. Nachos
 * allows multiple threads to run concurrently.
 *
 * To create a new thread of execution, first declare a class that implements
 * the <tt>Runnable</tt> interface. That class then implements the <tt>run</tt>
 * method. An instance of the class can then be allocated, passed as an
 * argument when creating <tt>KThread</tt>, and forked. For example, a thread
 * that computes pi could be written as follows:
 *
 * <p><blockquote><pre>
 * class PiRun implements Runnable {
 *     public void run() {
 *         // compute pi
 *         ...
 *     }
 * }
 * </pre></blockquote>
 * <p>The following code would then create a thread and start it running:
 *
 * <p><blockquote><pre>
 * PiRun p = new PiRun();
 * new KThread(p).fork();
 * </pre></blockquote>
 */
public class KThread {
    /**
     * Get the current thread.
     *
     * @return	the current thread.
     */
    public static KThread currentThread() {
		Lib.assertTrue(currentThread != null);
		return currentThread;
    }
    
    /**
     * Allocate a new <tt>KThread</tt>. If this is the first <tt>KThread</tt>,
     * create an idle thread as well.
     */
    public KThread() {
		if (currentThread != null) {
			tcb = new TCB();
		}
		else {
			readyQueue = ThreadedKernel.scheduler.newThreadQueue(false);
			readyQueue.acquire(this);

			currentThread = this;
			tcb = TCB.currentTCB();
			name = "main";
			restoreState();

			createIdleThread();
		}
    }

    /**
     * Allocate a new KThread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     */
    public KThread(Runnable target) {
		this();
		this.target = target;
    }

    /**
     * Set the target of this thread.
     *
     * @param	target	the object whose <tt>run</tt> method is called.
     * @return	this thread.
     */
    public KThread setTarget(Runnable target) {
		Lib.assertTrue(status == statusNew);

		this.target = target;
		return this;
    }

    /**
     * Set the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @param	name	the name to give to this thread.
     * @return	this thread.
     */
    public KThread setName(String name) {
		this.name = name;
		return this;
    }

    /**
     * Get the name of this thread. This name is used for debugging purposes
     * only.
     *
     * @return	the name given to this thread.
     */     
    public String getName() {
	return name;
    }

    /**
     * Get the full name of this thread. This includes its name along with its
     * numerical ID. This name is used for debugging purposes only.
     *
     * @return	the full name given to this thread.
     */
    public String toString() {
	return (name + " (#" + id + ")");
    }

    /**
     * Deterministically and consistently compare this thread to another
     * thread.
     */
    public int compareTo(Object o) {
		KThread thread = (KThread) o;

		if (id < thread.id)
			return -1;
		else if (id > thread.id)
			return 1;
		else
			return 0;
    }

    /**
     * Causes this thread to begin execution. The result is that two threads
     * are running concurrently: the current thread (which returns from the
     * call to the <tt>fork</tt> method) and the other thread (which executes
     * its target's <tt>run</tt> method).
     */
    public void fork() {
		// 복제를 하기 위해 새로 스레드가 만들어지는 상태에 있어야 실행
		// 즉, 현재 fork되지 않은 상태여야 fork 수행 가능
		Lib.assertTrue(status == statusNew);
		// fork() method를 실행하여 스레드를 복제하기 전에 복제 대상이 현재 실행 중인지 판단
		Lib.assertTrue(target != null);

		// 디버그 메시지를 띄웁니다
		// 앞으로 모든 Lib.debug함수는 디버그 메시지 출력 용도입니다.
		Lib.debug(dbgThread,
			  "Forking thread: " + toString() + " Runnable: " + target);
		
		// interrupt 발생 이후 원래 스레드를 정지시킴 => 이 행위를 위해 intStatus를 false로 설정
		// 머신의 현재 상태를 intStatus에 넣고 머신의 interrupt 발생을 false상태로 만든다.
		// interrupt class내의 disable함수는 기본적으로 enable이 false로 설정 되어있는것을 토대로
		// oldStatus에 false 저장됨 => 그러므로 intStatus의 값은 false로 저장된다
		boolean intStatus = Machine.interrupt().disable();
		
		// 복제된 스레드의 TCB를 실행 시킨다
		// 지정된 target이 thread에서 실행된다.
		tcb.start(new Runnable() {
			public void run() {
				runThread();
			}
		});
		
		// fork된 스레드를 ready 상태로 만들고
		// 그 스레드를 readyQueue에 집어 넣습니다.
		ready();
		
		// 인터럽트 intStatus의 상태에 따라 복구시킴
		Machine.interrupt().restore(intStatus);
    }

    private void runThread() {
		// 스레드를 시작하기 위한 준비를 함
		begin();
		// 다른 스레드를 실행 시켜라
		target.run();
		// 스레드를 끝마침
		finish();
    }

    private void begin() {
		// 현재 스레드 정보 디버깅
		Lib.debug(dbgThread, "Beginning thread: " + toString());

		// 이 스레드가 현재 실행하기 위한 스레드인지 확인
		Lib.assertTrue(this == currentThread);
		
		// 인터럽트가 발생된 지점이나, 처음 상태에서 스레드를 시작시키기 위해 실행 지점을 복원함
		restoreState();
		
		// 현재 인터럽트가 발생 중인 상태로 전환
		Machine.interrupt().enable();
    }

    /**
     * Finish the current thread and schedule it to be destroyed when it is
     * safe to do so. This method is automatically called when a thread's
     * <tt>run</tt> method returns, but it may also be called directly.
     *
     * The current thread cannot be immediately destroyed because its stack and
     * other execution state are still in use. Instead, this thread will be
     * destroyed automatically by the next thread to run, when it is safe to
     * delete this thread.
     */
    public static void finish() {
		// 현재 작동중인 스레드 정보를 디버깅(출력)
		Lib.debug(dbgThread, "Finishing thread: " + currentThread.toString());

		// 머신의 인터럽트 발생을 불가능 하게 함
		Machine.interrupt().disable();

		// 현재 실행중인 스레드에 destroy를 가능 하게 한다.
		Machine.autoGrader().finishingCurrentThread();

		// destroy 가능한 스레드가 있는지 확인 없으면 오류 출력
		Lib.assertTrue(toBeDestroyed == null);
		// 현재 스레드를 destroy 예정으로 만든다
		toBeDestroyed = currentThread;

		// 현재 실행 중인 스레드의 status를 finish state로 만든다
		currentThread.status = statusFinished;

		// 현재 인터럽트가 발생 중인지를 체크해서, 현재 발생 중이 아니라면 다음에 실행할 스레드로 넘긴다.
		sleep();
    }

    /**
     * Relinquish the CPU if any other thread is ready to run. If so, put the
     * current thread on the ready queue, so that it will eventually be
     * rescheuled.
     *
     * <p>
     * Returns immediately if no other thread is ready to run. Otherwise
     * returns when the current thread is chosen to run again by
     * <tt>readyQueue.nextThread()</tt>.
     *
     * <p>
     * Interrupts are disabled, so that the current thread can atomically add
     * itself to the ready queue and switch to the next thread. On return,
     * restores interrupts to the previous state, in case <tt>yield()</tt> was
     * called with interrupts disabled.
     */
    public static void yield() {
		Lib.debug(dbgThread, "Yielding thread: " + currentThread.toString());

		// 현재 스레드가 실행 중인지 판단
		Lib.assertTrue(currentThread.status == statusRunning);

		// 현재 인터럽트를 비활성화 시키기기 전에 이 상태를 저장
		boolean intStatus = Machine.interrupt().disable();

		// 현재 실행중인 스레드가 readyQueue로 들어감
		currentThread.ready();

		// 양보받은 다음 스레드를 실행
		// 실행이 종료되면
		runNextThread();

		// 양보한 스레드가 인터럽트 발생 시점으로 되돌아가 스레드를 다시 실행한다.
		Machine.interrupt().restore(intStatus);
    }

    /**
     * Relinquish the CPU, because the current thread has either finished or it
     * is blocked. This thread must be the current thread.
     *
     * <p>
     * If the current thread is blocked (on a synchronization primitive, i.e.
     * a <tt>Semaphore</tt>, <tt>Lock</tt>, or <tt>Condition</tt>), eventually
     * some thread will wake this thread up, putting it back on the ready queue
     * so that it can be rescheduled. Otherwise, <tt>finish()</tt> should have
     * scheduled this thread to be destroyed by the next thread to run.
     */
    public static void sleep() {
		Lib.debug(dbgThread, "Sleeping thread: " + currentThread.toString());

		// 현재 인터럽트가 비활성화 되어있는지 확인
		Lib.assertTrue(Machine.interrupt().disabled());
		
		// 종료된 스레드를 block상태로 만든다
		if (currentThread.status != statusFinished)
			currentThread.status = statusBlocked;

		runNextThread();
    }

    /**
     * Moves this thread to the ready state and adds this to the scheduler's
     * ready queue.
     */
    public void ready() {
		Lib.debug(dbgThread, "Ready thread: " + toString());

		// 인터럽트가 현재 비활성화 상태인지 확인
		Lib.assertTrue(Machine.interrupt().disabled());
		// 레디 상태가 아니어야 레디 상태로 스레드를 만들 수 있다
		Lib.assertTrue(status != statusReady);

		// 상태를 레디 상태로 만든 뒤
		status = statusReady;
		// 이 스레드가 idle스레드가 아니면
		if (this != idleThread)
			// 레디 큐에 레디 상태로 만든 스레드를 집어넣는다
			readyQueue.waitForAccess(this);

		Machine.autoGrader().readyThread(this);
    }

    /**
     * Waits for this thread to finish. If this thread is already finished,
     * return immediately. This method must only be called once; the second
     * call is not guaranteed to return. This thread must not be the current
     * thread.
     */
    public void join() {
		// 스레드에서 join은 다른 스레드가 종료될 때까지 기다리는 메소드이다
		Lib.debug(dbgThread, "Joining to thread: " + toString());

		// 만약 현재 실행중인 스레드가 자기 자신인 경우
		// 자기 자신이 끝날 때까지 기다리는 것은 말이 안되기 때문에
		// 현재 실행중인 스레드가 join메소드를 일으킨 스레드라면 오류를 발생시키고
		// 아니라면 join메소드를 일으킨 스레드가 현재 실행중인 스레드가 종료될 때까지 기다린다.
		Lib.assertTrue(this != currentThread);
    }

    /**
     * Create the idle thread. Whenever there are no threads ready to be run,
     * and <tt>runNextThread()</tt> is called, it will run the idle thread. The
     * idle thread must never block, and it will only be allowed to run when
     * all other threads are blocked.
     *
     * <p>
     * Note that <tt>ready()</tt> never adds the idle thread to the ready set.
     */
    private static void createIdleThread() {
		Lib.assertTrue(idleThread == null);

		idleThread = new KThread(new Runnable() {
			public void run() { while (true) yield(); }
		});
		idleThread.setName("idle");

		Machine.autoGrader().setIdleThread(idleThread);

		idleThread.fork();
    }
    
    /**
     * Determine the next thread to run, then dispatch the CPU to the thread
     * using <tt>run()</tt>.
     */
    private static void runNextThread() {
		// 레디 큐에서 다음에 실행할 스레드를 빼온다
		KThread nextThread = readyQueue.nextThread();
		// 만약에 레디큐가 비어있다면
		if (nextThread == null)
			// 비어있는 kThread객체를 다음 스레드로 한 뒤
			nextThread = idleThread;
		
		// 다음에 실행하고자하는 스레드를 작동시킨다
		nextThread.run();
    }

    /**
     * Dispatch the CPU to this thread. Save the state of the current thread,
     * switch to the new thread by calling <tt>TCB.contextSwitch()</tt>, and
     * load the state of the new thread. The new thread becomes the current
     * thread.
     *
     * <p>
     * If the new thread and the old thread are the same, this method must
     * still call <tt>saveState()</tt>, <tt>contextSwitch()</tt>, and
     * <tt>restoreState()</tt>.
     *
     * <p>
     * The state of the previously running thread must already have been
     * changed from running to blocked or ready (depending on whether the
     * thread is sleeping or yielding).
     *
     * @param	finishing	<tt>true</tt> if the current thread is
     *				finished, and should be destroyed by the new
     *				thread.
     */
    private void run() {
		// 인터럽트가 발생하지 않은 상태인지 체크
		Lib.assertTrue(Machine.interrupt().disabled());
		
		// 실행 중인 스레드가 다른 스레드의 실행을 위해 양보함
		Machine.yield();
		
		// 현재 실행중인 스레드는 나중에 복원을 위해 현재 상태를 저장하고
		currentThread.saveState();

		Lib.debug(dbgThread, "Switching from: " + currentThread.toString()
			  + " to: " + toString());
		
		// 현재 스레드를 이 메소드를 호출한 스레드로 변경
		currentThread = this;
		
		// this와 원래 currentThread 사이에 contextSwitch가 일어나게 한 다음
		tcb.contextSwitch();
		
		// this, 즉 실행하고자 하는 스레드의 상태를 running으로 만든다.
		currentThread.restoreState();
    }

    /**
     * Prepare this thread to be run. Set <tt>status</tt> to
     * <tt>statusRunning</tt> and check <tt>toBeDestroyed</tt>.
     */
    protected void restoreState() {
		Lib.debug(dbgThread, "Running thread: " + currentThread.toString());

		// 스레드의 상태를 체크
		// 인터럽트가 비활성화 상태인지 체크하고
		// 현재 실행중인 스레드와 메소드를 호출한 스레드가 일치하는지 체크하고
		// tcb가 일치하는지 확인한다.
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(this == currentThread);
		Lib.assertTrue(tcb == TCB.currentTCB());

		// current스레드의 상태를 수정한다
		Machine.autoGrader().runningThread(this);
		status = statusRunning;
		
		
		// toBeDestroyed가 널이 아니면
		// 없어져야 하는 스레드가 running으로 바뀌면 안되기 때문에
		// tobedestroyed에 올라온 스레드의 해당 정보를 다 없앤다
		if (toBeDestroyed != null) {
			toBeDestroyed.tcb.destroy();
			toBeDestroyed.tcb = null;
			toBeDestroyed = null;
		}
    }

    /**
     * Prepare this thread to give up the processor. Kernel threads do not
     * need to do anything here.
     */
    protected void saveState() {
		Lib.assertTrue(Machine.interrupt().disabled());
		// 상태 복원을 위해 현재 실행중인 스레드가 이 메소드를 호출한 스레드인지 체크함
		// 만약 맞다면 현재 상태가 저장 될 것이고, 아니면 오류 출력
		Lib.assertTrue(this == currentThread);
    }

    private static class PingTest implements Runnable {
		PingTest(int which) {
			this.which = which;
		}
	
		public void run() {
			for (int i = 0; i < 10; i++) {
				System.out.println("*** thread " + which + " looped "
						   + i + " times");
				currentThread.yield();
			}
		}

		private int which;
    }

    /**
     * Tests whether this module is working.
     */
    public static void selfTest() {
		Lib.debug(dbgThread, "Enter KThread.selfTest");

		new KThread(new PingTest(1)).setName("forked thread").fork();
		new PingTest(0).run();
    }

    private static final char dbgThread = 't';

    /**
     * Additional state used by schedulers.
     *
     * @see	nachos.threads.PriorityScheduler.ThreadState
     */
    public Object schedulingState = null;

    private static final int statusNew = 0;
    private static final int statusReady = 1;
    private static final int statusRunning = 2;
    private static final int statusBlocked = 3;
    private static final int statusFinished = 4;

    /**
     * The status of this thread. A thread can either be new (not yet forked),
     * ready (on the ready queue but not running), running, or blocked (not
     * on the ready queue and not running).
     */
    private int status = statusNew;
    private String name = "(unnamed thread)";
    private Runnable target;
    private TCB tcb;

    /**
     * Unique identifer for this thread. Used to deterministically compare
     * threads.
     */
    private int id = numCreated++;
    /** Number of times the KThread constructor was called. */
    private static int numCreated = 0;

    private static ThreadQueue readyQueue = null;
    private static KThread currentThread = null;
    private static KThread toBeDestroyed = null;
    private static KThread idleThread = null;
}
