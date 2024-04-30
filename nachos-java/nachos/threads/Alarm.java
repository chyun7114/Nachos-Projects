package nachos.threads;

import nachos.machine.Machine;

import java.util.*;

/**
 * �ϵ���� Ÿ�̸Ӹ� ����Ͽ� ������ �����ϰ� Ư�� �ð����� �����带 ����Ű�� Ŭ�����Դϴ�.
 */
public class Alarm {
    /**
     * ���ο� Alarm�� �Ҵ��մϴ�. ����� Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�� �� �˶��� �ݹ����� �����մϴ�.
     *
     * <p><b>����</b>: �� �� �̻��� �˶��� ���� ��� Nachos�� ���������� �۵����� �ʽ��ϴ�.
     */

    private Map<KThread, Long> blockedThreads = new HashMap<>();
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() { timerInterrupt(); }
        });
    }

    /**
     * Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�Դϴ�. �̴� ����� Ÿ�̸ӿ� ���� �ֱ������� ȣ��˴ϴ� (�뷫������ �� 500 Ŭ�� ƽ).
     * ���� �����带 �纸�մϴ�. �ٸ� �����尡 ����Ǿ�� �� ��� ���ؽ�Ʈ ����ġ�� �����˴ϴ�.
     */


    /*
        - waitUntil() �޼ҵ� �� timer ���ͷ�Ʈ �ڵ鷯�� �����ϸ� ��
        - block�� ��������� ����Ʈ�� ����� timer ���ͷ�Ʈ �߻��� ������ timer tick�� x��ŭ
        �������� �˻� �� ���� ������ ��� �ش� �����带 wake-up ��Ŵ
        - x Timer tick�� 0 �Ǵ� ������ ��� ��ٸ��� �ʰ� ��� ������
    */
    /*
        �̷��� �䱸 ���׿� ���� Alarm Ŭ������ waitUntil() �޼��带 �����ϴ� ����� ������ �����ϴ�.

        1. waitUntil() �޼��� ������ ���� �ð����� ����ؾ� �ϴ� �ð��� ��ٸ��� ���� �����带 ��Ͻ��Ѿ� �մϴ�.
        2. Ÿ�̸� ���ͷ�Ʈ �ڵ鷯�� �����Ͽ� �� Ÿ�̸� ���ͷ�Ʈ�� �߻��� ������ ���� �ð����� ����� �ð��� Ȯ���ϰ�,
            ���� ����� �ð��� x���� ũ�ų� ������ �ش� �����带 ������ �մϴ�.
        3. x Ÿ�̸� ƽ�� 0 ������ ��쿡�� ��� �����ؾ� �մϴ�.
    */
    public void waitUntil(long x) {
        // x Ÿ�̸� ƽ�� 0������ ��� ��� ����
        if(x <= 0)
            return;
        // wakeTime�� ������� ����� �ð����� xƽ��ŭ ���Ѵ�
        long wakeTime = Machine.timer().getTime() + x;
        // ���ͷ�Ʈ ���� ����
        boolean intStatus  = Machine.interrupt().disable();

        // ���� �����带 ��� ��Ű�� ������� �������� ����Ʈ�� ����ִ´�
        blockedThreads.put(KThread.currentThread(), wakeTime);
        KThread.sleep();
    
        // ���ͷ�Ʈ ���¸� �����Ѵ�
        Machine.interrupt().restore(intStatus);
    }

    public void timerInterrupt() {
        //  ���� �ð��� Ȯ���Ѵ�
        long currentTime = Machine.timer().getTime();

        // block�� �����尡 ������ Ÿ�̸� ���ͷ�Ʈ�� �߻��ص� �ƹ� �ϵ� �߻����� �ʴ´�
        if(blockedThreads.isEmpty())
            return;
        
        // ����Ʈ���� �����带 �ϳ��� Ȯ���Ѵ�
        // block�� ������� �߿��� ���� �ð��� ����� �͵��� ����
        for (Iterator<Map.Entry<KThread, Long>> iter = blockedThreads.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<KThread, Long> entry = iter.next();
            KThread thread = entry.getKey();
            Long wakeTime = entry.getValue();

            if (currentTime >= wakeTime) {
                thread.ready(); // �����带 ready ���·� ����
                iter.remove(); // ����Ʈ���� ����
            }
        }

        KThread.currentThread().yield();
    }

    public static class AlarmTest {
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
}
