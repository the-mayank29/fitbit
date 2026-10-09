package fitbit.analytics;

import fitbit.model.CycleLog;
import fitbit.model.UserProfile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CyclePredictor {

    public record CycleStatus(
        int currentCycleDay,
        String currentPhase,
        String phaseDescription,
        boolean isFertileWindow,
        boolean isOvulationToday,
        LocalDate nextPeriodDate,
        long daysUntilNextPeriod,
        LocalDate nextOvulationDate,
        long daysUntilOvulation,
        String recommendedFocus
    ) {}

    public static CycleStatus evaluateCycle(LocalDate lastCycleStartDate, UserProfile profile) {
        int cycleLength = profile != null && profile.getCycleLengthDays() > 0 ? profile.getCycleLengthDays() : 28;
        int periodLength = profile != null && profile.getPeriodLengthDays() > 0 ? profile.getPeriodLengthDays() : 5;

        if (lastCycleStartDate == null) {
            lastCycleStartDate = LocalDate.now().minusDays(10);
        }

        LocalDate today = LocalDate.now();
        long rawDays = ChronoUnit.DAYS.between(lastCycleStartDate, today);
        int currentDay = (int) (rawDays % cycleLength) + 1;
        if (currentDay <= 0) currentDay = 1;

        int ovulationDay = Math.max(1, cycleLength - 14);
        int fertileStartDay = Math.max(1, ovulationDay - 5);
        int fertileEndDay = ovulationDay + 1;

        String phase;
        String phaseDescription;
        String recommendedFocus;

        if (currentDay <= periodLength) {
            phase = "MENSTRUAL";
            phaseDescription = "Menstrual Phase (Rest & Replenish)";
            recommendedFocus = "Gentle yoga, stretching, hydration, iron-rich foods, and extra sleep.";
        } else if (currentDay < fertileStartDay) {
            phase = "FOLLICULAR";
            phaseDescription = "Follicular Phase (Energy Rising)";
            recommendedFocus = "Great time for strength training, cardio, learning new skills, and creative projects.";
        } else if (currentDay <= fertileEndDay) {
            phase = "OVULATORY";
            phaseDescription = "Ovulation Phase (Peak Energy & Fertility)";
            recommendedFocus = "Peak athletic performance, HIIT, social activities, and peak endurance.";
        } else {
            phase = "LUTEAL";
            phaseDescription = "Luteal Phase (Winding Down)";
            recommendedFocus = "Moderate cardio, pilates, complex carbs, magnesium, and stress reduction.";
        }

        boolean isFertile = (currentDay >= fertileStartDay && currentDay <= fertileEndDay);
        boolean isOvulationToday = (currentDay == ovulationDay);

        long cyclesPassed = rawDays / cycleLength;
        LocalDate currentCycleAnchor = lastCycleStartDate.plusDays(cyclesPassed * cycleLength);
        LocalDate nextPeriod = currentCycleAnchor.plusDays(cycleLength);
        LocalDate nextOvulation = nextPeriod.minusDays(14);

        long daysUntilNextPeriod = Math.max(0, ChronoUnit.DAYS.between(today, nextPeriod));
        long daysUntilOvulation = ChronoUnit.DAYS.between(today, nextOvulation);

        return new CycleStatus(
            currentDay,
            phase,
            phaseDescription,
            isFertile,
            isOvulationToday,
            nextPeriod,
            daysUntilNextPeriod,
            nextOvulation,
            daysUntilOvulation,
            recommendedFocus
        );
    }
}
