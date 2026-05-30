package com.ankita.freshfold.ui.profile;

public class FAQ {
    private String question;
    private String answer;
    private boolean expanded;

    public FAQ(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.expanded = false;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
