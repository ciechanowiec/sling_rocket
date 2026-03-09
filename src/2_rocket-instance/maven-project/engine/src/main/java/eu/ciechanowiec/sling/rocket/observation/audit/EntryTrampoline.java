package eu.ciechanowiec.sling.rocket.observation.audit;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Submits {@link Entry}-s for saving in {@link Storage} in batches. Batches are submitted for saving either when they
 * reach a size of 200 {@link Entry}-s or after 3 seconds since the last batch submission, whichever comes first.
 */
@Component(
    service = EntryTrampoline.class,
    immediate = true,
    configurationPolicy = ConfigurationPolicy.OPTIONAL
)
@Designate(ocd = EntryTrampolineConfig.class)
@Slf4j
@ServiceDescription("Submits audit entries for saving in storage in batches")
@ToString
public class EntryTrampoline {

    private static final int BATCH_SIZE = 200;
    private static final int FLUSH_INTERVAL_SECONDS = 3;

    private final AtomicReference<EntryTrampolineConfig> config;
    @ToString.Exclude
    private final JobManager jobManager;
    @ToString.Exclude
    private final BlockingQueue<Entry> buffer;
    @ToString.Exclude
    private final ScheduledExecutorService scheduler;

    /**
     * Constructs an instance of this class.
     *
     * @param jobManager {@link JobManager} used by the constructed object to submit batches of {@link Entry}-s for
     *                   saving in {@link Storage}
     * @param config     {@link EntryTrampolineConfig} used by the constructed instance
     */
    @Activate
    public EntryTrampoline(
        @Reference(cardinality = ReferenceCardinality.MANDATORY)
        JobManager jobManager,
        EntryTrampolineConfig config
    ) {
        this.jobManager = jobManager;
        this.config = new AtomicReference<>(config);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.buffer = new LinkedBlockingQueue<>();
        this.scheduler.scheduleAtFixedRate(
            this::flush, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS
        );
        log.info("Initialized {}", this);
    }

    /**
     * Shuts down the scheduler and flushes the buffer to submit any remaining {@link Entry}-s for saving in
     * {@link Storage}.
     */
    @Deactivate
    public void deactivate() {
        scheduler.shutdownNow();
        flush();
    }

    @Modified
    void configure(EntryTrampolineConfig config) {
        this.config.set(config);
        log.info("Reconfigured {}", this);
    }

    /**
     * Submits the given {@link Entry} for saving in {@link Storage}. The actual saving will be triggered either when
     * the number of {@link Entry}-s submitted through this method since the last batch submission reaches 200 or after
     * 3 seconds since the last batch submission, whichever comes first.
     *
     * @param entry {@link Entry} to be submitted for saving in {@link Storage}
     */
    public void submitForSaving(Entry entry) {
        if (!config.get().is$_$enabled()) {
            return;
        }
        boolean wasEntryOffered = buffer.offer(entry);
        log.trace("{} was successfully offered to the queue? Answer: {}", entry, wasEntryOffered);

        if (buffer.size() >= BATCH_SIZE) {
            // If we hit 200 entries before the 3-second timer, trigger an early flush.
            // Executed asynchronously to immediately free the calling thread.
            scheduler.execute(this::flush);
        }
    }

    // Synchronized to ensure the timed flush and size-based flush don't collide
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    private synchronized void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        List<Entry> entriesBatch = new ArrayList<>();
        buffer.drainTo(entriesBatch, BATCH_SIZE);
        List<Map<String, Object>> entriesBatchSerialized = new EntriesBatch(entriesBatch).asSlingJobProperties();
        Map<String, Object> jobProps = new ConcurrentHashMap<>();
        jobProps.put(EntriesBatch.PN_ENTRIES_BATCHED, entriesBatchSerialized);
        // Create exactly one Sling Job for the entire batch of up to 200 entries
        jobManager.addJob(Storage.JOB_TOPIC, jobProps);
        int numOfEntries = entriesBatch.size();
        log.trace("Submitted a batched job with {} entry/ies", numOfEntries);
    }
}
