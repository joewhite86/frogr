package de.whitefrog.frogr.jobs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import de.whitefrog.frogr.Service;
import de.whitefrog.frogr.exception.FrogrException;
import de.whitefrog.frogr.exception.JobInitializationException;
import de.whitefrog.frogr.helper.TimeUtils;
import it.sauronsoftware.cron4j.Scheduler;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlRootElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JobRunner {
  private static final Logger logger = LoggerFactory.getLogger(JobRunner.class);
  
  private final Scheduler scheduler = new Scheduler();
  private Map<String, Class> jobs = new HashMap<>();
  private final Service service;
  private Map<Long, Activity> history = new HashMap<>();

  @XmlRootElement
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Activity {
    Class job;
    UUID uuid;
    boolean success = false;
    boolean running = false;
    long startTime;
    long endTime;
    String errorMessage;
    String stackTrace;
  }
  
  public JobRunner(Service service) {
    this.service = service;
  }
  
  public Collection<Activity> history() { return history.values(); }
  
  public Map<String, Class> jobs() { return jobs; }

  public void run(String name, Object... args) {
    run(jobs.get(name), args);
  }
  
  public void run(Object instance, Object... args) {
    run(instance, instance.getClass(), args);
  }
  
  public void run(Class<?> clazz, Object... args) {
    Object instance;
    try {
      Constructor constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, new Class[]{Service.class});
      if(constructor == null) {
        // else we take the default constructor if available
        constructor = clazz.getConstructor();
        if(constructor == null) {
          throw new JobInitializationException("no matching constructor for " + clazz.getSimpleName() + " found");
        }
        instance = constructor.newInstance();
      } else {
        instance = constructor.newInstance(service);
      }
    } catch(ReflectiveOperationException e) {
      logger.error("job {} failed: {}", clazz.getSimpleName(), e.getMessage(), e);
      throw new FrogrException(e);
    }
    run(instance, clazz, args);
  }
  
  public void run(Object instance, Class<?> clazz, Object... args) {
    Activity activity = new Activity();
    activity.job = clazz;
    try {
      logger.info("running job {} ...", clazz.getSimpleName());
      Class<?>[] classes = new Class[args.length];
      for(int i = 0; i < args.length; i++) { classes[i] = String.class; }
      Method method = clazz.getDeclaredMethod("run", classes);
      
      activity.running = true;
      activity.uuid = UUID.randomUUID();
      activity.startTime = System.currentTimeMillis();
      history.put(activity.startTime, activity);
      method.invoke(instance, args);
      activity.success = true;
      activity.endTime = System.currentTimeMillis();
      logger.info("job {} done in {}", clazz.getSimpleName(), 
        TimeUtils.formatInterval(activity.endTime - activity.startTime));
    } catch(Exception e) {
      activity.errorMessage = e.getClass().getName() + ": " + e.getMessage();
      activity.stackTrace = ExceptionUtils.getStackTrace(e);
      logger.error("job {} failed: {}", clazz.getSimpleName(), e.getMessage(), e);
      throw new FrogrException(e);
    } finally {
      activity.endTime = System.currentTimeMillis();
      activity.running = false;
    }
  }

  public void initScheduler() {
    for(String pkg : service.registry()) {
      Reflections reflections = new Reflections(pkg);
      for(Class clazz : reflections.getTypesAnnotatedWith(Job.class)) {
        Job annotation = (Job) clazz.getAnnotation(Job.class);
        jobs.put(clazz.getSimpleName(), clazz);
        if(!annotation.value().equals("---")) {
          logger.debug("scheduling job {} for {}", clazz.getName(), annotation.value());
          scheduler.schedule(annotation.value(), () -> run(clazz));
        }
      }
    }
    scheduler.start();
  }
}
