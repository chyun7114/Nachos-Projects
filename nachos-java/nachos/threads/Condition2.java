package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * 인터럽트 비활성화/활성화를 통해 구현한 조건 변수 클래스.
 */
public class Condition2 {
    private Lock conditionLock;
    private LinkedList<KThread> waitQueue;

    /**
     * 새로운 조건 변수를 생성합니다.
     *
     * @param conditionLock 이 조건 변수와 연결된 락. 현재 스레드는 sleep(), wake(), wakeAll()을 호출할 때 이 락을 소유해야 합니다.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        waitQueue = new LinkedList<>();
    }

    /**
     * 조건 변수를 사용하여 락을 원자적으로 해제하고 다른 스레드가 wake()을 호출할 때까지 현재 스레드를 슬립 상태로 만듭니다.
     * 현재 스레드는 이 메서드를 호출할 때 락을 소유해야 합니다. 슬립이 끝나면 락을 자동으로 다시 획득합니다.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();

        waitQueue.add(KThread.currentThread());
        conditionLock.release();
        KThread.sleep();

        conditionLock.acquire();
        Machine.interrupt().restore(intStatus);
    }

    /**
     * 조건 변수에서 슬립 상태인 스레드 중 최대 하나를 깨웁니다. 현재 스레드는 이 메서드를 호출할 때 락을 소유해야 합니다.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();

        if (!waitQueue.isEmpty()) {
            KThread thread = waitQueue.removeFirst();
            if (thread != null) {
                thread.ready();
            }
        }

        Machine.interrupt().restore(intStatus);
    }

    /**
     * 조건 변수에서 슬립 상태인 모든 스레드를 깨웁니다. 현재 스레드는 이 메서드를 호출할 때 락을 소유해야 합니다.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        while (!waitQueue.isEmpty()) {
            wake();
        }
    }

    // Place Condition2 testing code in the Condition2 class.

    // Example of the "interlock" pattern where two threads strictly
    // alternate their execution with each other using a condition
    // variable.

    private static class InterlockTest {
        private static Lock lock;
        private static Condition2 cv;

        private static class Interlocker implements Runnable {
            public void run () {
                lock.acquire();
                for (int i = 0; i < 10; i++) {
                    System.out.println(KThread.currentThread().getName());
                    cv.wake();   // signal
                    cv.sleep();  // wait
                }
                lock.release();
            }
        }

        public InterlockTest () {
            lock = new Lock();
            cv = new Condition2(lock);

            KThread ping = new KThread(new Interlocker());
            ping.setName("ping");
            KThread pong = new KThread(new Interlocker());
            pong.setName("pong");

            ping.fork();
            pong.fork();

            // We need to wait for ping to finish, and the proper way
            // to do so is to join on ping.  (Note that, when ping is
            // done, pong is sleeping on the condition variable; if we
            // were also to join on pong, we would block forever.)
            // For this to work, join must be implemented.  If you
            // have not implemented join yet, then comment out the
            // call to join and instead uncomment the loop with
            // yields; the loop has the same effect, but is a kludgy
            // way to do it.
            ping.join();
            // for (int i = 0; i < 50; i++) { KThread.currentThread().yield(); }
        }
    }

    // Invoke Condition2.selfTest() from ThreadedKernel.selfTest()

    public static void selfTest() {
        new InterlockTest();
    }
}