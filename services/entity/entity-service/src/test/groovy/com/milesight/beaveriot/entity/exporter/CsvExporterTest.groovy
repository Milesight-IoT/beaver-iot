package com.milesight.beaveriot.entity.exporter


import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CsvExporterTest extends Specification {

    def test_export_csv_data() {
        given:
        def outputStream = new ByteArrayOutputStream()
        def pageIterator = { i ->
            if (i > 1) {
                return null
            }
            return [
                    new TestExportCsvData(field: "a_" + i, splitByComma: "b_" + i),
                    new TestExportCsvData(field: "c_" + i, splitByComma: "d_" + i),
            ]
        } as ChunkIterator<TestExportCsvData>

        when:
        CsvExporter.newInstance(TestExportCsvData)
                .export(outputStream, pageIterator)

        then:
        outputStream.toString(StandardCharsets.UTF_8) == """"field","split,by,comma","parent_property"
"a_0","b_0","0"
"c_0","d_0","0"
"a_1","b_1","0"
"c_1","d_1","0"
"""
    }

    class TestExportCsvData extends TestParentExportCsvData {

        @ExportField
        private String field

        @ExportField("split,by,comma")
        private String splitByComma

    }

    class TestParentExportCsvData {

        @ExportField
        private String parent_property = "0"

    }

}
