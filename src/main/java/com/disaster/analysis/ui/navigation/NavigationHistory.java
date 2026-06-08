package com.disaster.analysis.ui.navigation;

import java.util.Stack;


public class NavigationHistory {
    private final Stack<View> history;


    public NavigationHistory() {
        this.history = new Stack<>();
    }


    public void push(View view) {
        if (view != null) {
            history.push(view);
        }
    }


    public View pop() {
        if (!history.isEmpty()) {
            return history.pop();
        }
        return null;
    }


    public View peek() {
        if (!history.isEmpty()) {
            return history.peek();
        }
        return null;
    }


    public boolean canGoBack() {
        return !history.isEmpty();
    }


    public void clear() {
        history.clear();
    }
}
