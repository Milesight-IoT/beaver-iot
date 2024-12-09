package com.milesight.beaveriot.entity.exporter;


import com.milesight.beaveriot.base.utils.StringUtils;
import lombok.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A simple component for exporting data to CSV format
 *
 * @param <T> type of the data to export
 */
public class CsvExporter<T> {

    public static final String SEPARATOR = ",";

    public static final String CSV_VALUE_FORMAT = "\"%s\"";

    private final String headerLine;

    private final List<Function<T, String>> fieldsGetter = new ArrayList<>();

    private CsvExporter(Class<T> model) {
        val headerLineBuilder = new StringBuilder();
        forEachField(model, field -> {
            val fieldAnno = field.getAnnotation(ExportField.class);
            if (fieldAnno == null) {
                return;
            }
            var header = StringUtils.isEmpty(fieldAnno.value()) ? fieldAnno.header() : fieldAnno.value();
            if (StringUtils.isEmpty(header)) {
                header = field.getName();
            }
            header = String.format(CSV_VALUE_FORMAT, header);
            if (headerLineBuilder.isEmpty()) {
                headerLineBuilder.append(header);
            } else {
                headerLineBuilder.append(SEPARATOR);
                headerLineBuilder.append(header);
            }
            field.setAccessible(true);
            fieldsGetter.add(obj -> getStringValue(obj, field));
        });
        headerLineBuilder.append("\n");
        headerLine = headerLineBuilder.toString();
    }

    public static <T> CsvExporter<T> newInstance(Class<T> model) {
        return new CsvExporter<>(model);
    }

    private static void forEachField(Class<?> clazz, Consumer<Field> consumer) {
        while (clazz != null) {
            val declaredFields = clazz.getDeclaredFields();
            for (val field : declaredFields) {
                consumer.accept(field);
            }
            clazz = clazz.getSuperclass();
        }
    }

    @SneakyThrows
    private String getStringValue(T obj, Field field) {
        val value = field.get(obj);
        return value == null ? "" : String.format(CSV_VALUE_FORMAT, value);
    }

    public void export(OutputStream outputStream, ChunkIterator<T> chunkIterator) throws IOException {
        outputStream.write(headerLine.getBytes(StandardCharsets.UTF_8));
        int i = 0;
        var chunk = chunkIterator.get(i++);
        while (chunk != null && !chunk.isEmpty()) {
            val content = chunk;
            for (T t : content) {
                if (t == null) {
                    continue;
                }
                val values = new ArrayList<String>();
                fieldsGetter.forEach(getter -> values.add(getter.apply(t)));
                val line = String.join(SEPARATOR, values) + "\n";
                outputStream.write(line.getBytes(StandardCharsets.UTF_8));
            }
            chunk = chunkIterator.get(i++);
        }

    }

}
