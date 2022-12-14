package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

public class AlertRabbit {
    public static void main(String[] args) {
        Properties config = getProperties();
        try (Connection store = getConnection(config)) {
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("store", store);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            int interval = Integer.parseInt(config.getProperty("rabbit.interval"));
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(interval)
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            int sleep = Integer.parseInt(config.getProperty("rabbit.sleep"));
            Thread.sleep(sleep);
            scheduler.shutdown();
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    public static class Rabbit implements Job {
        public Rabbit() {
            System.out.println(hashCode());
        }

        @Override
        public void execute(JobExecutionContext context) {
            System.out.println("Rabbit runs here ...");
            Connection store = (Connection) context.getJobDetail().getJobDataMap().get("store");
            try (PreparedStatement statement =
                         store.prepareStatement("insert into rabbit(created_date) values (?)",
                                 Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static Properties getProperties() {
        try (
                InputStream in =
                        AlertRabbit.class.getClassLoader().getResourceAsStream("rabbit.properties")
        ) {
            Properties config = new Properties();
            config.load(in);
            return config;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Connection getConnection(Properties config) throws SQLException, ClassNotFoundException {
        Class.forName(config.getProperty("rabbit.driver"));
        String url = config.getProperty("rabbit.url");
        String login = config.getProperty("rabbit.username");
        String password = config.getProperty("rabbit.password");
        return DriverManager.getConnection(url, login, password);

    }
}