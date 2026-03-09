package eu.ciechanowiec.sling.rocket.observation.audit;

import org.apache.jackrabbit.oak.api.jmx.Description;
import org.apache.jackrabbit.oak.api.jmx.Name;

import java.util.List;

/**
 * MBean for a {@link Storage}.
 */
@SuppressWarnings("WeakerAccess")
@Description(Storage.SERVICE_DESCRIPTION)
public interface StorageMBean {

    /**
     * Counts the number of {@link Entry}-s stored in the {@link Storage}.
     *
     * @return number of {@link Entry}-s stored in the {@link Storage}
     */
    @SuppressWarnings("unused")
    @Description("Counts the number of entries stored in the storage")
    long getCount();

    /**
     * Deletes all {@link Entry}-s stored in the {@link Storage}.
     */
    @SuppressWarnings("unused")
    @Description("Deletes all entries stored in the storage")
    void deleteAll();

    /**
     * Deletes all {@link Entry}-s stored in the {@link Storage} for the specified year.
     *
     * @param year year for which {@link Entry}-s should be deleted
     * @return {@code true} if entries were deleted; {@code false} otherwise
     */
    @SuppressWarnings("unused")
    @Description("Deletes all entries stored in the storage for the specified year")
    boolean delete(
        @Name("year")
        @Description("Year for which entries should be deleted")
        int year
    );

    /**
     * Retrieves all {@link Entry}-s stored in the {@link Storage} for the specified year, month, and day.
     *
     * @param year  year for which {@link Entry}-s should be retrieved
     * @param month month for which {@link Entry}-s should be retrieved
     * @param day   day for which {@link Entry}-s should be retrieved
     * @return all {@link Entry}-s stored in the {@link Storage} for the specified year, month, and day
     */
    @SuppressWarnings("unused")
    @Description("Retrieves all entries stored in the storage for the specified year, month, and day")
    List<Entry> entries(
        @Name("year")
        @Description("Year for which entries should be retrieved")
        int year,
        @Name("month")
        @Description("Month for which entries should be retrieved")
        int month,
        @Name("day")
        @Description("Day for which entries should be retrieved")
        int day
    );
}
