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
        ArrayList<Course> courses = coursesBySubjectAndNumber(
            schedule, normalizedSubject, normalizedCourseNumber
        );
        displayCourses(courses, normalizedSubject + " " + normalizedCourseNumber);
    }

    private void displayCourses(ArrayList<Course> courses, String searchDescription) {
        if (courses.isEmpty()) {
            System.out.println("No courses found for " + searchDescription + ".");
            return;
        }

        for (Course course : courses) {
            displayCourse(course);
        }
    }

    private void displayCourse(Course course) {
        String details = courseSummary(course);
        if (course.meetingsFaculty() == null || course.meetingsFaculty().isEmpty()) {
            System.out.println(details + ", Meeting time unavailable");
            return;
        }
        for (Course.MeetingFaculty meeting : course.meetingsFaculty()) {
            String meetingDetails = meeting == null || meeting.meetingTime() == null
                ? "Meeting time unavailable"
                : meeting.meetingTime().toString();
            System.out.println(details + ", " + meetingDetails);
        }
    }

    private String courseSummary(Course course) {
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
        return courseLabel + ", " + credits + ", " + title;
    }

    /**
     * Displays the student's current registrations.
     */
    public void displayStudentSchedule(Student student) {
        System.out.println("Schedule for " + student.getFirstName() + " "
            + student.getLastName() + " (ID: " + student.getId() + ")");
        if (student.getCourses().isEmpty()) {
            System.out.println("No courses registered.");
            return;
        }
        for (Course course : student.getCourses()) {
            displayCourse(course);
        }
        System.out.println("Total credits: " + formatCredit(student.getTotalCredits()));
    }

    /**
     * Adds the selected 1-based result from a subject and course-number search.
     *
     * @return true if the course was registered
     */
    public boolean addCourseToStudent(
        EasternSchedule schedule,
        Student student,
        String subject,
        String courseNumber,
        int sectionChoice
    ) throws Exception {
        if (subject == null || subject.isBlank() || courseNumber == null || courseNumber.isBlank()) {
            throw new IllegalArgumentException("Subject and course number must not be blank");
        }
        String normalizedSubject = subject.trim();
        String normalizedNumber = courseNumber.trim();
        ArrayList<Course> matches = coursesBySubjectAndNumber(
            schedule, normalizedSubject, normalizedNumber
        );
        if (sectionChoice < 1 || sectionChoice > matches.size()) {
            throw new IllegalArgumentException("Section choice is out of range");
        }

        Course selected = matches.get(sectionChoice - 1);
        boolean added = student.addCourse(selected);
        if (added) {
            System.out.println("Added " + courseSummary(selected) + ".");
        } else {
            System.out.println("Could not add " + courseSummary(selected)
                + ": the course may be full, conflict with your schedule, already registered, "
                + "or exceed the credit limit.");
        }
        return added;
    }

    /**
     * Drops a course from the student's displayed schedule using its 1-based position.
     *
     * @return true if a registered course was dropped
     */
    public boolean dropCourseFromStudent(Student student, int courseChoice) {
        if (courseChoice < 1 || courseChoice > student.getCourses().size()) {
            throw new IllegalArgumentException("Course choice is out of range");
        }
        Course course = student.getCourses().get(courseChoice - 1);
        boolean dropped = student.dropCourse(course);
        if (dropped) {
            System.out.println("Dropped " + courseSummary(course) + ".");
        }
        return dropped;
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
     * Starts the menu and prompts for course searches and student registration actions.
     *
     * @param args not used
     * @throws Exception if the schedule cannot retrieve the courses
     */
    public static void main(String[] args) throws Exception {
        try (Scanner input = new Scanner(System.in)) {
            MainMenu mainMenu = new MainMenu();
            EasternSchedule schedule = new EasternSchedule();
            System.out.print("Enter student ID: ");
            String id = input.nextLine();
            System.out.print("Enter first name: ");
            String firstName = input.nextLine();
            System.out.print("Enter last name: ");
            String lastName = input.nextLine();
            Student student = new Student(id, firstName, lastName);

            boolean running = true;
            while (running) {
                System.out.println();
                System.out.println("1. Display all courses for a subject");
                System.out.println("2. Display courses by subject and number");
                System.out.println("3. Add a course");
                System.out.println("4. Drop a course");
                System.out.println("5. Drop all courses");
                System.out.println("6. Display my schedule");
                System.out.println("0. Exit");
                System.out.print("Choose an option: ");
                String option = input.nextLine().trim();
                switch (option) {
                    case "1" -> {
                        System.out.print("Enter a subject code: ");
                        mainMenu.displayCoursesBySubject(schedule, input.nextLine());
                    }
                    case "2" -> {
                        System.out.print("Enter a subject code: ");
                        String subject = input.nextLine();
                        System.out.print("Enter a course number: ");
                        String courseNumber = input.nextLine();
                        mainMenu.displayCoursesBySubjectAndNumber(schedule, subject, courseNumber);
                    }
                    case "3" -> {
                        System.out.print("Enter a subject code: ");
                        String subject = input.nextLine();
                        System.out.print("Enter a course number: ");
                        String courseNumber = input.nextLine();
                        ArrayList<Course> matches = coursesBySubjectAndNumber(
                            schedule, subject, courseNumber
                        );
                        if (matches.isEmpty()) {
                            System.out.println("No matching course sections found.");
                            break;
                        }
                        for (int index = 0; index < matches.size(); index++) {
                            System.out.print((index + 1) + ". ");
                            mainMenu.displayCourse(matches.get(index));
                        }
                        System.out.print("Choose a section: ");
                        int choice = Integer.parseInt(input.nextLine().trim());
                        mainMenu.addCourseToStudent(schedule, student, subject, courseNumber, choice);
                    }
                    case "4" -> {
                        if (student.getCourses().isEmpty()) {
                            System.out.println("No courses to drop.");
                            break;
                        }
                        mainMenu.displayStudentSchedule(student);
                        System.out.print("Choose a course number to drop (0 to cancel): ");
                        int choice = Integer.parseInt(input.nextLine().trim());
                        if (choice != 0) {
                            mainMenu.dropCourseFromStudent(student, choice);
                        }
                    }
                    case "5" -> {
                        student.dropAllCourses();
                        System.out.println("All courses dropped.");
                    }
                    case "6" -> mainMenu.displayStudentSchedule(student);
                    case "0" -> running = false;
                    default -> System.out.println("Choose a valid menu option.");
                }
            }
        }
    }

    private static ArrayList<Course> coursesBySubjectAndNumber(
        EasternSchedule schedule,
        String subject,
        String courseNumber
    ) throws Exception {
        if (subject == null || subject.isBlank() || courseNumber == null || courseNumber.isBlank()) {
            throw new IllegalArgumentException("Subject and course number must not be blank");
        }
        ArrayList<Course> matches = new ArrayList<>();
        for (Course course : schedule.queryCourseBySubject(subject.trim())) {
            if (course.subject() != null && course.courseNumber() != null
                && course.subject().equalsIgnoreCase(subject.trim())
                && course.courseNumber().equalsIgnoreCase(courseNumber.trim())) {
                matches.add(course);
            }
        }
        return matches;
    }
}
