package Server;

import java.util.TimerTask;

public class TimeChecker extends TimerTask {

    private boolean needsQuit;
    private long timeToCompare;

    public TimeChecker(long timeStamp){
        this.needsQuit = false;
        this.timeToCompare = timeStamp;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        int minutes = (int) (now - timeToCompare) / (1000 * 60);
        if( minutes > 5 ){
           this.needsQuit = true;
        }
    }

    public boolean needsQuit(){
        return this.needsQuit;
    }
}
