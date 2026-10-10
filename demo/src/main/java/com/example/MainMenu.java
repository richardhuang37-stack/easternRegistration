package com.example;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Provides the user-facing course search menu.
 */
public class MainMenu {

    /**
     * Displays every course for the requested subject, including each meeting.
     *
     * @param schedule the schedule to query
     * @param subject the course subject code
     * @throws Exception if the schedule cannot retrieve the courses
     */
    public void displayCoursesBySubject(EasternSchedule schedule, String subject) throws Exception {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject must not be blank");
        }

        ArrayList<Course> courses = schedule.queryCourseBySubject(subject.trim());
        if (courses.isEmpty()) {
            System.out.println("No courses found for subject " + subject.trim() + ".");
            return;
        }

        for (Course course : courses) {
            String courseLabel = course.subject() + " " + course.courseNumber();
            if (course.sequenceNumber() != null && !course.sequenceNumber().isBlank()
                && !course.courseNumber().endsWith("-" + course.sequenceNumber().trim())) {
                courseLabel += "-" + course.sequenceNumber().trim();
            }

            String creditValue = course.creditHours();
            if (creditValue == null || creditValue.isBlank()) {
                creditValue = formatCreditRange(course.creditHourLow(), course.creditHourHigh());
            }
            String credits = creditValue == null
                ? "Credits unavailable"
                : creditValue + (creditValue.equals("1") ? " credit" : " credits");
            String title = course.courseTitle() == null || course.courseTitle().isBlank()
                ? "Title unavailable"
                : decodeHtmlEntities(course.courseTitle().trim());
            String details = courseLabel + ", " + credits + ", " + title;

            if (course.meetingsFaculty() == null || course.meetingsFaculty().isEmpty()) {
                System.out.println(details + ", Meeting time unavailable");
                continue;
            }
            for (Course.MeetingFaculty meeting : course.meetingsFaculty()) {
                String meetingDetails = meeting == null || meeting.meetingTime() == null
                    ? "Meeting time unavailable"
                    : meeting.meetingTime().toString();
                System.out.println(details + ", " + meetingDetails);
            }
        }
    }

    private static String formatCreditRange(Double low, Double high) {
        if (low == null && high == null) {
            return null;
        }
        if (low == null) {
            return formatCredit(high);
        }
        if (high == null || low.equals(high)) {
            return formatCredit(low);
        }
        return formatCredit(low) + "-" + formatCredit(high);
    }

    private static String formatCredit(Double credits) {
        return credits == Math.rint(credits)
            ? Long.toString(credits.longValue())
            : credits.toString();
    }

    private static String decodeHtmlEntities(String text) {
        return text.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    /**
     * Starts the menu and prompts for a subject to display.
     *
     * @param args not used
     * @throws Exception if the schedule cannot retrieve the courses
     */
    public static void main(String[] args) throws Exception {
        try (Scanner input = new Scanner(System.in)) {
            System.out.print("Enter a subject code to list its courses: ");
            String subject = input.nextLine();
            new MainMenu().displayCoursesBySubject(new EasternSchedule(), subject);
        }
    }
}
