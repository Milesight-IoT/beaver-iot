package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.builder.CronBuilder;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.field.expression.FieldExpression;
import lombok.*;

import java.time.DayOfWeek;
import java.util.List;

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

    private List<DayOfWeek> daysOfWeek;

    public Cron toCron() {
        var builder = CronBuilder.cron(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING))
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

        FieldExpression doW = null;
        if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
            for (var dayOfWeek : daysOfWeek) {
                if (dayOfWeek == null) {
                    continue;
                }
                var dayOfWeekIndex = dayOfWeek.getValue();
                if (doW == null) {
                    doW = on(dayOfWeekIndex);
                } else {
                    doW = doW.and(on(dayOfWeekIndex));
                }
            }
            builder.withDoW(doW);
        }
        if (doW == null) {
            builder.withDoW(always());
        }

        return builder.instance();
    }

}
