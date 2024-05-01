package nachos.threads;

import nachos.machine.Lib;
import nachos.machine.Machine;

import java.util.*;

/**
 * 하드웨어 타이머를 사용하여 선점을 제공하고 특정 시간까지 스레드를 대기시키는 클래스입니다.
 */
public class Alarm {
    /**
     * 새로운 Alarm을 할당합니다. 기계의 타이머 인터럽트 핸들러를 이 알람의 콜백으로 설정합니다.
     *
     * <p><b>주의</b>: 두 개 이상의 알람이 있을 경우 Nachos는 정상적으로 작동하지 않습니다.
     */

    private Map<KThread, Long> blockedThreads = new HashMap<>();
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() { timerInterrupt(); }
        });
    }

    /**
     * 타이머 인터럽트 핸들러입니다. 이는 기계의 타이머에 의해 주기적으로 호출됩니다 (대략적으로 매 500 클록 틱).
     * 현재 스레드를 양보합니다. 다른 스레드가 실행되어야 할 경우 컨텍스트 스위치가 강제됩니다.
     */


    /*
        - waitUntil() 메소드 및 timer 인터럽트 핸들러만 수정하면 됨
        - block된 쓰레드들의 리스트를 만들고 timer 인터럽트 발생할 때마다 timer tick이 x만큼
        지났는지 검사 후 만약 지났을 경우 해당 쓰레드를 wake-up 시킴
        - x Timer tick이 0 또는 음수인 경우 기다리지 않고 즉시 리턴함
    */
    /*
        이러한 요구 사항에 따라 Alarm 클래스의 waitUntil() 메서드를 구현하는 방법은 다음과 같습니다.

        1. waitUntil() 메서드 내에서 현재 시간에서 경과해야 하는 시간을 기다리는 동안 쓰레드를 블록시켜야 합니다.
        2. 타이머 인터럽트 핸들러를 수정하여 매 타이머 인터럽트가 발생할 때마다 현재 시간에서 경과한 시간을 확인하고,
            만약 경과한 시간이 x보다 크거나 같으면 해당 쓰레드를 깨워야 합니다.
        3. x 타이머 틱이 0 이하인 경우에는 즉시 리턴해야 합니다.
    */
    public void waitUntil(long x) {
        // x 타이머 틱이 0이하인 경우 즉시 리턴
        if(x <= 0)
            return;
        // wakeTime을 현재까지 경과한 시간에서 x틱만큼 더한다
        long wakeTime = Machine.timer().getTime() + x;
        // 인터럽트 상태 설정
        boolean intStatus  = Machine.interrupt().disable();

        // 현재 스레드를 블록 시키고 대기중인 스레드의 리스트에 집어넣는다
        blockedThreads.put(KThread.currentThread(), wakeTime);
        KThread.sleep();

        // 인터럽트 상태를 복원한다
        Machine.interrupt().restore(intStatus);
    }

    public void timerInterrupt() {
        //  현재 시간을 확인한다
        long currentTime = Machine.timer().getTime();

        // block된 스레드가 없으면 타이머 인터럽트가 발생해도 아무 일도 발생하지 않는다
//        if(blockedThreads.isEmpty())
//            return;

        // 리스트에서 스레드를 하나씩 확인한다
        // block된 쓰레드들 중에서 현재 시간이 경과한 것들을 깨움
        for (Iterator<Map.Entry<KThread, Long>> iter = blockedThreads.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<KThread, Long> entry = iter.next();
            KThread thread = entry.getKey();
            Long wakeTime = entry.getValue();

            if (currentTime >= wakeTime) {
                thread.ready(); // 쓰레드를 ready 상태로 전이
                iter.remove(); // 리스트에서 제거
            }
        }

        KThread.currentThread().yield();
    }


    private static class AlarmTest implements Runnable {
        private long delay;
        AlarmTest(long delay) {
            this.delay = delay;
        }

        public void run() {
            long startTime, nowTime;

            startTime = Machine.timer().getTime();
            long wakeUpTime = startTime + delay;

            System.out.println("execution time : " + startTime);
            System.out.println("wake-up time : " + wakeUpTime);

            ThreadedKernel.alarm.waitUntil(delay);
            ThreadedKernel.alarm.timerInterrupt();

            nowTime = Machine.timer().getTime();

            String checkTime = "(" + nowTime + " >= " + wakeUpTime +" (" + startTime + " + " + delay +"))";
            System.out.println("wake-up time " + nowTime + checkTime);
        }
    }
    public static void selfTest(){
        KThread[] t = new KThread[10];
        long[] durations = {1000, 1500, 2000, 2500, 3000};
        for(int i = 0; i < 10; i++){
            int j = (Lib.random(5));
            t[i] = new KThread(new AlarmTest((long)(durations[j]))).setName("forked thread");
            t[i].fork();
        }
        ThreadedKernel.alarm.waitUntil(100000);
    }
}
