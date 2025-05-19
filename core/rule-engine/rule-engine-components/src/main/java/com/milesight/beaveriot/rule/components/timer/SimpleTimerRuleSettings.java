package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpression;
import lombok.*;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.cronutils.model.field.expression.FieldExpressionFactory.always;
import static com.cronutils.model.field.expression.FieldExpressionFactory.on;
import static com.cronutils.model.field.expression.FieldExpressionFactory.questionMark;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerRuleSettings {

    private Integer minute;

    private Integer hour;

    private List<DayOfWeekOptions> daysOfWeek;

    public Cron toCron() {
        var builder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING))
                .withSecond(on(0))
                .withDoM(questionMark())
                .withMonth(always());

        if (hour == null) {
            hour = 0;
        }
        builder.withHour(on(hour));

        if (minute == null) {
            minute = 0;
        }
        builder.withMinute(on(minute));

        Optional.ofNullable(daysOfWeek)
                .orElse(Collections.emptyList())
                .stream()
                .map(DayOfWeekOptions::getDaysOfWeek)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .distinct()
                .map(DayOfWeek::getValue)
                .sorted()
                .map(dayOfWeek -> (FieldExpression) on(dayOfWeek))
                .reduce(FieldExpression::and)
                .ifPresentOrElse(
                        builder::withDoW,
                        () -> builder.withDoW(always())
                );

        return builder.instance();
    }

}
