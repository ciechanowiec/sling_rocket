package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("MagicNumber")
class CalendarRepositoryTest extends TestEnvironment {

    private CalendarRepository calendarRepository;

    CalendarRepositoryTest() {
        super(ResourceResolverType.JCR_OAK);
    }

    @BeforeEach
    void setup() {
        calendarRepository = new CalendarRepository(fullResourceAccess);
    }

    @Test
    void testCalendarsBasic() {
        new StagedCalendarNode(Year.of(1900), Year.of(1900), fullResourceAccess).save(
            new TargetJCRPath("/content/calendar-one-year")
        );
        assertEquals(365, totalAmountOfDays());
        new StagedCalendarNode(Year.of(1900), Year.of(2100), fullResourceAccess).save(
            new TargetJCRPath("/content/calendar-big")
        );
        new StagedCalendarNode(Year.of(1990), Year.of(1995), fullResourceAccess).save(
            new TargetJCRPath("/content/calendar-small")
        );
        assertEquals(75_605 + 365, totalAmountOfDays());
    }

    @SuppressWarnings({"resource", "PMD.CloseResource"})
    @Test
    void testSpecificDates() {
        CalendarNode calendar = new StagedCalendarNode(Year.of(2015), Year.of(2018), fullResourceAccess).save(
            new TargetJCRPath("/content/my-calendar")
        );
        ResourceResolver resourceResolver = context.resourceResolver();
        CalendarNode calendarModel = Optional.ofNullable(resourceResolver.getResource("/content/my-calendar"))
            .map(resource -> resource.adaptTo(CalendarNode.class))
            .orElseThrow();
        assertAll(
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), calendar.jcrPath()),
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), calendarModel.jcrPath()),
            () -> assertEquals(Year.of(2017), calendar.years().get(2).year()),
            () -> assertEquals(Year.of(2017), calendarModel.years().get(2).year()),
            () -> assertEquals(
                YearMonth.of(2017, 4), calendar.years().get(2).months().get(3).month()
            ),
            () -> assertEquals(
                YearMonth.of(2017, 4), calendarModel.years().get(2).months().get(3).month()
            ),
            () -> assertEquals(
                LocalDate.of(2017, 4, 20),
                calendar.years().get(2).months().get(3).days().get(19).day()
            ),
            () -> assertEquals(
                LocalDate.of(2017, 4, 20),
                calendarModel.years().get(2).months().get(3).days().get(19).day()
            ),
            () -> assertEquals(
                new TargetJCRPath("/content/my-calendar/2017/2017-04/2017-04-20"),
                calendar.years().get(2).months().get(3).days().get(19).jcrPath()
            ),
            () -> assertEquals(
                new TargetJCRPath("/content/my-calendar/2017/2017-04/2017-04-20"),
                calendarModel.years().get(2).months().get(3).days().get(19).jcrPath()
            )
        );
    }

    @Test
    void testWrongDates() {
        new StagedCalendarNode(Year.of(2015), Year.of(2010), fullResourceAccess).save(
            new TargetJCRPath("/content/my-calendar")
        );
        assertEquals(0, totalAmountOfDays());
    }

    @Test
    void testCantSave() {
        TargetJCRPath targetJCRPath = new TargetJCRPath("/content/my-calendar");
        new StagedCalendarNode(Year.of(2015), Year.of(2016), fullResourceAccess).save(
            targetJCRPath
        );
        StagedCalendarNode stagedCalendarNode = new StagedCalendarNode(
            Year.of(2015), Year.of(2017), fullResourceAccess
        );
        assertThrows(OccupiedJCRPathException.class, () -> stagedCalendarNode.save(targetJCRPath));
    }

    long totalAmountOfDays() {
        return calendarRepository.all().stream()
            .map(CalendarNode::years)
            .flatMap(Collection::stream)
            .map(YearNode::months)
            .flatMap(Collection::stream)
            .map(MonthNode::days)
            .mapToLong(Collection::size)
            .sum();
    }
}
