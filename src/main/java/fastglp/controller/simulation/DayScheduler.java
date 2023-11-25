package fastglp.controller.simulation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DayScheduler {
    @Autowired
    private DayController dayController;

    @Scheduled(fixedRate = 300000)
    public void scheduleTask(){
        dayController.execute();
    }
}
