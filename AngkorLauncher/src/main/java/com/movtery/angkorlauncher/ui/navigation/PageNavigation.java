package com.movtery.angkorlauncher.ui.navigation;

public final class PageNavigation {
    public static final int NO_DESTINATION = -1;

    private PageNavigation() {
    }

    public static int targetOrNone(int currentPage, int requestedPage, int itemCount) {
        if (itemCount <= 0) return NO_DESTINATION;
        int target = Math.max(0, Math.min(requestedPage, itemCount - 1));
        return target == currentPage ? NO_DESTINATION : target;
    }
}
