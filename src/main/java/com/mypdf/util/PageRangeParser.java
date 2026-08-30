package com.mypdf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PageRangeParser {
    private static final Pattern ITEM = Pattern.compile("^(\\d+)(?:\\s*-\\s*(\\d+))?$");

    private PageRangeParser() {
    }

    public static List<Integer> parse(String input, int maximumPage) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("페이지 범위를 입력해주세요.");
        }
        if (maximumPage < 1) {
            throw new IllegalArgumentException("PDF에 페이지가 없습니다.");
        }

        List<Integer> pages = new ArrayList<>();
        for (String rawItem : input.split(",", -1)) {
            String item = rawItem.trim();
            Matcher matcher = ITEM.matcher(item);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("잘못된 페이지 형식입니다: " + item);
            }
            int start = number(matcher.group(1), maximumPage);
            if (matcher.group(2) == null) {
                pages.add(start);
                continue;
            }
            int end = number(matcher.group(2), maximumPage);
            if (start > end) {
                throw new IllegalArgumentException("페이지 범위의 시작은 끝보다 클 수 없습니다: " + item);
            }
            for (int page = start; page <= end; page++) {
                pages.add(page);
            }
        }
        return List.copyOf(pages);
    }

    private static int number(String text, int maximumPage) {
        final int page;
        try {
            page = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("페이지 번호가 너무 큽니다: " + text);
        }
        if (page < 1) {
            throw new IllegalArgumentException("페이지 번호는 1 이상이어야 합니다.");
        }
        if (page > maximumPage) {
            throw new IllegalArgumentException("전체 페이지 수(" + maximumPage + ")보다 큰 페이지입니다: " + page);
        }
        return page;
    }
}
