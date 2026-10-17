package com.habitTracker.HabitTracker;

import com.habitTracker.HabitTracker.datasync.DataSyncBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HabitTrackerApplication {

	public static void main(String[] args) {
		DataSyncBootstrap.pullLatest();
		SpringApplication.run(HabitTrackerApplication.class, args);
	}

}
