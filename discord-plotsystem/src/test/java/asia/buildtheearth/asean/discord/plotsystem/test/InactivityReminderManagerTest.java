package asia.buildtheearth.asean.discord.plotsystem.test;

import asia.buildtheearth.asean.discord.plotsystem.core.system.InactivityReminderManager;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSnapshotData;
import com.alpsbte.plotsystem.api.model.PlotDifficulty;
import com.alpsbte.plotsystem.api.model.PlotReviewOutcome;
import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import com.alpsbte.plotsystem.api.model.PlotStatus;
import com.alpsbte.plotsystem.api.model.PlotType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InactivityReminderManagerTest {
    private static final ZoneId UTC = ZoneId.of("UTC");

    @Test
    void schedulesAtConfiguredTimeAfterInactivityThreshold() {
        Instant activity = Instant.parse("2026-01-01T09:00:00Z");

        Optional<Instant> reminder = InactivityReminderManager.calculateReminderAt(
            activity, null, 7, LocalTime.NOON, UTC
        );

        assertEquals(Instant.parse("2026-01-08T12:00:00Z"), reminder.orElseThrow());
    }

    @Test
    void doesNotScheduleBeforeFullInactivityDaysHavePassed() {
        Instant activity = Instant.parse("2026-01-01T23:00:00Z");

        Optional<Instant> reminder = InactivityReminderManager.calculateReminderAt(
            activity, null, 7, LocalTime.NOON, UTC
        );

        assertEquals(Instant.parse("2026-01-09T12:00:00Z"), reminder.orElseThrow());
    }

    @Test
    void skipsReminderAfterPlotSystemAbandonmentDeadline() {
        Instant activity = Instant.parse("2026-01-01T09:00:00Z");
        Instant deadline = Instant.parse("2026-01-08T11:00:00Z");

        Optional<Instant> reminder = InactivityReminderManager.calculateReminderAt(
            activity, deadline, 7, LocalTime.NOON, UTC
        );

        assertTrue(reminder.isEmpty());
    }

    @Test
    void stillSchedulesIntegrationReminderWithoutAbandonmentPolicyDeadline() {
        Instant activity = Instant.parse("2026-01-01T09:00:00Z");

        Optional<Instant> reminder = InactivityReminderManager.calculateReminderAt(
            activity, null, 7, LocalTime.NOON, UTC
        );

        assertTrue(reminder.isPresent());
    }

    @Test
    void schedulesUsingConfiguredTimezone() {
        Instant activity = Instant.parse("2026-01-01T23:30:00Z");
        ZoneId berlin = ZoneId.of("Europe/Berlin");

        Optional<Instant> reminder = InactivityReminderManager.calculateReminderAt(
            activity, null, 7, LocalTime.NOON, berlin
        );

        assertEquals(ZonedDateTime.of(2026, 1, 9, 12, 0, 0, 0, berlin).toInstant(), reminder.orElseThrow());
    }

    @Test
    void snapshotAdapterUsesCoordinatesAndExplicitReviewOutcome() {
        PlotSnapshot snapshot = PlotSnapshot.builder()
            .id(42)
            .owner(UUID.randomUUID())
            .status(PlotStatus.COMPLETED)
            .type(PlotType.FOCUS_MODE)
            .difficulty(PlotDifficulty.EASY)
            .cityProjectId("city")
            .countryCode("country")
            .lastActivity(LocalDate.now())
            .longitude(103.8)
            .latitude(1.3)
            .reviewOutcome(PlotReviewOutcome.REJECTED)
            .lastActivityAt(Instant.parse("2026-01-01T09:00:00Z"))
            .build();

        PlotSnapshotData data = PlotSnapshotData.from(snapshot);

        assertEquals(PlotSnapshotData.PlotStatusValue.REJECTED, data.status());
        assertArrayEquals(new double[] { 103.8, 1.3 }, data.geoCoordinates());
    }
}
