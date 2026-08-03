package com.movtery.angkorlauncher.ui.navigation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PageNavigationTest {
    private static final int ITEM_COUNT = 5;

    @Test
    public void everyVerticalDestinationNavigatesFromEveryOtherPage() {
        assertAllDestinationsNavigateOnce();
    }

    @Test
    public void everyHorizontalDestinationNavigatesFromEveryOtherPage() {
        assertAllDestinationsNavigateOnce();
    }

    @Test
    public void repeatedTapOnActiveDestinationDoesNothing() {
        for (int page = 0; page < ITEM_COUNT; page++) {
            assertEquals(PageNavigation.NO_DESTINATION,
                    PageNavigation.targetOrNone(page, page, ITEM_COUNT));
        }
    }

    @Test
    public void rapidDifferentDestinationsAreNotDebounced() {
        int current = 0;
        for (int requested : new int[] {1, 2, 3, 4, 0, 3}) {
            int target = PageNavigation.targetOrNone(current, requested, ITEM_COUNT);
            assertEquals(requested, target);
            current = target;
        }
    }

    private void assertAllDestinationsNavigateOnce() {
        for (int current = 0; current < ITEM_COUNT; current++) {
            for (int requested = 0; requested < ITEM_COUNT; requested++) {
                int expected = current == requested ? PageNavigation.NO_DESTINATION : requested;
                assertEquals(expected,
                        PageNavigation.targetOrNone(current, requested, ITEM_COUNT));
            }
        }
    }
}
