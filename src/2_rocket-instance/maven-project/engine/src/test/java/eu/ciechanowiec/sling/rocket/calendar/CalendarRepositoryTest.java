package eu.ciechanowiec.sling.rocket.calendar;

import eu.ciechanowiec.sling.rocket.identity.AuthIDUser;
import eu.ciechanowiec.sling.rocket.jcr.path.OccupiedJCRPathException;
import eu.ciechanowiec.sling.rocket.jcr.path.ParentJCRPath;
import eu.ciechanowiec.sling.rocket.jcr.path.TargetJCRPath;
import eu.ciechanowiec.sling.rocket.privilege.PrivilegeAdmin;
import eu.ciechanowiec.sling.rocket.test.TestEnvironment;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"MagicNumber", "MultipleStringLiterals", "PMD.AvoidDuplicateLiterals"})
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
        new StagedCalendarNode(Year.of(1900), Year.of(1930), fullResourceAccess).save(
            new TargetJCRPath("/content/calendar-big")
        );
        new StagedCalendarNode(Year.of(1990), Year.of(1995), fullResourceAccess).save(
            new TargetJCRPath("/content/calendar-small")
        );
        assertEquals(13_513 + 365, totalAmountOfDays());
    }

    @SuppressWarnings({"squid:S5778", "PMD.CloseResource", "MethodLength"})
    @Test
    void testSpecificDates() {
        CalendarNode calendar = new StagedCalendarNode(Year.of(2015), Year.of(2018), fullResourceAccess).save(
            new TargetJCRPath("/content/my-calendar")
        );
        ResourceResolver resourceResolver = context.resourceResolver();
        CalendarNode calendarModel = Optional.ofNullable(resourceResolver.getResource("/content/my-calendar"))
            .map(resource -> resource.adaptTo(CalendarNode.class))
            .orElseThrow();
        CalendarNode calendarFromPath = new CalendarNode(new TargetJCRPath("/content/my-calendar"), resourceResolver);
        assertAll(
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), calendar.jcrPath()),
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), calendarModel.jcrPath()),
            () -> assertEquals(Year.of(2017), calendar.years().get(2).year()),
            () -> assertEquals(Year.of(2017), calendarModel.years().get(2).year()),
            () -> assertEquals(Year.of(2017), calendarFromPath.years().get(2).year()),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> new CalendarNode(new TargetJCRPath("/content/no-calendar"), resourceResolver)
            ),
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
            ),
            () -> assertEquals(Year.of(2016), calendarModel.year(Year.of(2016)).orElseThrow().year()),
            () -> assertTrue(calendarModel.year(Year.of(2010)).isEmpty()),
            () -> assertEquals(
                YearMonth.of(2016, Month.AUGUST),
                calendarModel.month(YearMonth.of(2016, Month.AUGUST)).orElseThrow().month()
            ),
            () -> assertTrue(calendarModel.month(YearMonth.of(2010, Month.AUGUST)).isEmpty()),
            () -> assertEquals(
                LocalDate.of(2016, Month.AUGUST, 15),
                calendarModel.day(LocalDate.of(2016, Month.AUGUST, 15)).orElseThrow().day()
            ),
            () -> assertTrue(calendarModel.day(LocalDateTime.of(2010, Month.AUGUST, 15, 20, 20)).isEmpty())
        );
    }

    @SuppressWarnings({"resource", "PMD.CloseResource"})
    @Test
    void testWithPrivileges() {
        TargetJCRPath calendarPath = new TargetJCRPath("/content/my-calendar");
        new StagedCalendarNode(Year.of(2015), Year.of(2018), fullResourceAccess).save(calendarPath);
        PrivilegeAdmin privilegeAdmin = new PrivilegeAdmin(fullResourceAccess);
        AuthIDUser someUser = createOrGetUser(new AuthIDUser("some-user"));
        privilegeAdmin.allow(calendarPath, someUser, PrivilegeConstants.JCR_READ);
        TargetJCRPath year2017Path = new TargetJCRPath(new ParentJCRPath(calendarPath), "2017");
        privilegeAdmin.deny(
            year2017Path, someUser, PrivilegeConstants.JCR_READ
        );
        privilegeAdmin.allow(
            new TargetJCRPath(new ParentJCRPath(year2017Path), "2017-04"), someUser, PrivilegeConstants.JCR_READ
        );
        ResourceResolver adminRR = fullResourceAccess.acquireAccess();
        CalendarNode adminModel = Optional.ofNullable(adminRR.getResource("/content/my-calendar"))
            .map(resource -> resource.adaptTo(CalendarNode.class))
            .orElseThrow();
        ResourceResolver userRR = fullResourceAccess.acquireAccess(someUser);
        CalendarNode userModel = Optional.ofNullable(userRR.getResource("/content/my-calendar"))
            .map(resource -> resource.adaptTo(CalendarNode.class))
            .orElseThrow();
        assertAll(
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), adminModel.jcrPath()),
            () -> assertEquals(new TargetJCRPath("/content/my-calendar"), userModel.jcrPath()),
            () -> assertEquals(Year.of(2017), adminModel.years().get(2).year()),
            () -> assertEquals(Year.of(2018), userModel.years().get(2).year()),
            () -> assertEquals(
                YearMonth.of(2017, 4), adminModel.years().get(2).months().get(3).month()
            ),
            () -> assertEquals(
                YearMonth.of(2018, 4), userModel.years().get(2).months().get(3).month()
            ),
            () -> assertNotNull(userRR.getResource("/content/my-calendar/2017/2017-04"))
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
