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
        displayCourses(courses, "subject " + subject.trim());
    }

    /**
     * Displays all sections of a course matching the requested subject and number.
     *
     * @param schedule the schedule to query
     * @param subject the required course subject code
     * @param courseNumber the required course number
     * @throws Exception if the schedule cannot retrieve the courses
     */
    public void displayCoursesBySubjectAndNumber(
        EasternSchedule schedule,
        String subject,
        String courseNumber
    ) throws Exception {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject must not be blank");
        }
        if (courseNumber == null || courseNumber.isBlank()) {
            throw new IllegalArgumentException("Course number must not be blank");
        }

        String normalizedSubject = subject.trim();
        String normalizedCourseNumber = courseNumber.trim();
        ArrayList<Course> courses = new ArrayList<>();
        for (Course course : schedule.queryCourseBySubject(normalizedSubject)) {
            if (course.subject() != null && course.courseNumber() != null
                && course.subject().equalsIgnoreCase(normalizedSubject)
                && course.courseNumber().equalsIgnoreCase(normalizedCourseNumber)) {
                courses.add(course);
            }
        }
        displayCourses(courses, normalizedSubject + " " + normalizedCourseNumber);
    }

    private void displayCourses(ArrayList<Course> courses, String searchDescription) {
        if (courses.isEmpty()) {
            System.out.println("No courses found for " + searchDescription + ".");
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
            MainMenu mainMenu = new MainMenu();
            EasternSchedule schedule = new EasternSchedule();
            System.out.println("1. Display all courses for a subject");
            System.out.println("2. Display courses by subject and number");
            System.out.print("Choose an option: ");
            String option = input.nextLine().trim();
            if ("1".equals(option)) {
                System.out.print("Enter a subject code: ");
                mainMenu.displayCoursesBySubject(schedule, input.nextLine());
            } else if ("2".equals(option)) {
                System.out.print("Enter a subject code: ");
                String subject = input.nextLine();
                System.out.print("Enter a course number: ");
                String courseNumber = input.nextLine();
                mainMenu.displayCoursesBySubjectAndNumber(schedule, subject, courseNumber);
            } else {
                throw new IllegalArgumentException("Choose 1 or 2");
            }
        }
    }
}
