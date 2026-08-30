package com.mypdf.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageRangeParserTest {
    @Test
    void parsesRangesAndSinglePagesInInputOrder() {
        assertEquals(List.of(1, 2, 3, 7, 10, 11, 12), PageRangeParser.parse("1-3,7,10-12", 12));
    }

    @Test
    void rejectsInvalidInputs() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("abc", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("1--5", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("0", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("-1", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("6", 5)),
                () -> assertThrows(IllegalArgumentException.class, () -> PageRangeParser.parse("4-2", 5))
        );
    }
}
