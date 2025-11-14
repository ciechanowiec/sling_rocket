package eu.ciechanowiec.sling.rocket.job;

import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import lombok.SneakyThrows;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchedulableJobConsumersTest extends TestEnvironment {

    @Mock
    private JobManager jobManager;

    @Mock
    private Scheduler scheduler;

    @Mock
    private ScheduleOptions scheduleOptionsWriter1;

    @Mock
    private ScheduleOptions scheduleOptionsWriter2;

    SchedulableJobConsumersTest() {
        super(ResourceResolverType.RESOURCERESOLVER_MOCK);
    }

    @Test
    @SuppressWarnings(
        {
            "ExecutableStatementCount", "MethodLength", "JavaNCSS", "VariableDeclarationUsageDistance", "squid:S5961",
            "OverlyLongMethod", "PMD.NcssCount"
        }
    )
    void generalTest() {
        //
        // SETUP
        //
        context.registerService(JobManager.class, jobManager);
        context.registerService(Scheduler.class, scheduler);
        context.registerInjectActivateService(SimpleScheduler.class);
        String writer1Cron = "0 * * * * ?";
        String writer2Cron = "0 */5 * * * ?";
        Writer1Config writer1Config = new Writer1Config() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Writer1Config.class;
            }

            @Override
            public String schedule$_$cycle_cron$_$expression() {
                return writer1Cron;
            }
        };
        SchedulableJobConsumer writer1 = context.registerService(
            SchedulableJobConsumer.class, new Writer1(
                writer1Config
            ), Map.of(
                JobConsumer.PROPERTY_TOPICS, new String[]{Writer1.JOB_TOPIC_AAA, Writer1.JOB_TOPIC_BBB},
                SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY, writer1Config.schedule$_$cycle_cron$_$expression(),
                Constants.SERVICE_PID, "%s~rocketus".formatted(Writer1.class.getName())
            )
        );
        SchedulableJobConsumer writer2 = context.registerService(
            SchedulableJobConsumer.class, new Writer2(), Map.of(
                JobConsumer.PROPERTY_TOPICS, Writer2.JOB_TOPIC,
                SchedulableJobConsumer.CRON_EXPRESSION_PROPERTY, writer2Cron,
                ComponentConstants.COMPONENT_NAME, Writer2.class.getName(),
                ComponentConstants.COMPONENT_ID, "615"
            )
        );
        when(scheduler.EXPR(writer1Cron)).thenReturn(scheduleOptionsWriter1);
        when(scheduler.EXPR(writer2Cron)).thenReturn(scheduleOptionsWriter2);
        ArgumentCaptor<SchedulableRunnable> runnableWriter1 = ArgumentCaptor.forClass(SchedulableRunnable.class);
        ArgumentCaptor<SchedulableRunnable> runnableWriter2 = ArgumentCaptor.forClass(SchedulableRunnable.class);
        when(scheduler.schedule(runnableWriter1.capture(), eq(scheduleOptionsWriter1))).thenAnswer(
            invocation -> {
                runnableWriter1.getValue().run();
                return true;
            }
        );
        when(scheduler.unschedule("eu.ciechanowiec.sling.rocket.job.Writer1~rocketus")).thenReturn(true);
        when(scheduler.schedule(runnableWriter2.capture(), eq(scheduleOptionsWriter2))).thenAnswer(
            invocation -> {
                runnableWriter2.getValue().run();
                return true;
            }
        );
        when(scheduler.unschedule("eu.ciechanowiec.sling.rocket.job.Writer2~615")).thenReturn(true);

        //
        // TRIGGER SCHEDULING
        //
        SchedulableJobConsumers schedulableJobConsumers = context.registerInjectActivateService(
            SchedulableJobConsumers.class
        );

        //
        // VERIFY INITIAL SCHEDULING
        //
        verify(scheduleOptionsWriter1, times(1)).name("eu.ciechanowiec.sling.rocket.job.Writer1~rocketus");
        verify(scheduleOptionsWriter2, times(1)).name("eu.ciechanowiec.sling.rocket.job.Writer2~615");
        verify(scheduleOptionsWriter1, times(1)).name(anyString());
        verify(scheduleOptionsWriter2, times(1)).name(anyString());
        verify(scheduler, times(1)).schedule(runnableWriter1.capture(), eq(scheduleOptionsWriter1));
        verify(scheduler, times(1)).schedule(runnableWriter2.capture(), eq(scheduleOptionsWriter2));
        verify(scheduler, times(2)).schedule(any(), any());
        assertAll(
            () -> assertEquals(writer1Cron, runnableWriter1.getValue().scheduleCycleCronExpression()),
            () -> assertEquals(writer2Cron, runnableWriter2.getValue().scheduleCycleCronExpression()),
            () -> assertEquals(
                "eu.ciechanowiec.sling.rocket.job.Writer1~rocketus", runnableWriter1.getValue().id().get()
            ),
            () -> assertEquals("eu.ciechanowiec.sling.rocket.job.Writer2~615", runnableWriter2.getValue().id().get())
        );
        verify(jobManager, times(1)).addJob(Writer1.JOB_TOPIC_AAA, Map.of());
        verify(jobManager, times(1)).addJob(Writer1.JOB_TOPIC_BBB, Map.of());
        verify(jobManager, times(1)).addJob(Writer2.JOB_TOPIC, Map.of());
        verify(jobManager, times(3)).addJob(any(), any());
        verify(scheduler, never()).unschedule(anyString());

        //
        // UPDATE SERVICE REFERENCE
        //
        schedulableJobConsumers.updatedSchedulableJobConsumers(writer1, writer1ServiceReference());
        schedulableJobConsumers.updatedSchedulableJobConsumers(writer2, writer2ServiceReference());

        //
        // VERIFY RESCHEDULING
        //
        verify(scheduleOptionsWriter1, times(2)).name("eu.ciechanowiec.sling.rocket.job.Writer1~rocketus");
        verify(scheduleOptionsWriter2, times(2)).name("eu.ciechanowiec.sling.rocket.job.Writer2~615");
        verify(scheduleOptionsWriter1, times(2)).name(anyString());
        verify(scheduleOptionsWriter2, times(2)).name(anyString());
        verify(scheduler, times(2)).schedule(runnableWriter1.capture(), eq(scheduleOptionsWriter1));
        verify(scheduler, times(2)).schedule(runnableWriter2.capture(), eq(scheduleOptionsWriter2));
        verify(scheduler, times(4)).schedule(any(), any());
        assertAll(
            () -> assertEquals(writer1Cron, runnableWriter1.getValue().scheduleCycleCronExpression()),
            () -> assertEquals(writer2Cron, runnableWriter2.getValue().scheduleCycleCronExpression()),
            () -> assertEquals(
                "eu.ciechanowiec.sling.rocket.job.Writer1~rocketus", runnableWriter1.getValue().id().get()
            ),
            () -> assertEquals("eu.ciechanowiec.sling.rocket.job.Writer2~615", runnableWriter2.getValue().id().get())
        );
        verify(jobManager, times(2)).addJob(Writer1.JOB_TOPIC_AAA, Map.of());
        verify(jobManager, times(2)).addJob(Writer1.JOB_TOPIC_BBB, Map.of());
        verify(jobManager, times(2)).addJob(Writer2.JOB_TOPIC, Map.of());
        verify(jobManager, times(6)).addJob(any(), any());
        verify(scheduler, times(1)).unschedule("eu.ciechanowiec.sling.rocket.job.Writer1~rocketus");
        verify(scheduler, times(1)).unschedule("eu.ciechanowiec.sling.rocket.job.Writer2~615");
        verify(scheduler, times(2)).unschedule(anyString());
    }

    @SneakyThrows
    private ServiceReference<SchedulableJobConsumer> writer1ServiceReference() {
        Collection<ServiceReference<SchedulableJobConsumer>> serviceReferences = context.bundleContext()
            .getServiceReferences(
                SchedulableJobConsumer.class,
                "(%s=%s)".formatted(Constants.SERVICE_PID, "%s~rocketus".formatted(Writer1.class.getName()))
            );
        assertEquals(1, serviceReferences.size());
        return serviceReferences.iterator().next();
    }

    @SneakyThrows
    private ServiceReference<SchedulableJobConsumer> writer2ServiceReference() {
        Collection<ServiceReference<SchedulableJobConsumer>> serviceReferences = context.bundleContext()
            .getServiceReferences(
                SchedulableJobConsumer.class,
                "(%s=%s)".formatted(ComponentConstants.COMPONENT_NAME, Writer2.class.getName())
            );
        assertEquals(1, serviceReferences.size());
        return serviceReferences.iterator().next();
    }
}
