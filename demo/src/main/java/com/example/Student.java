package com.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a student and manages the courses in which they are registered.
 */
public class Student {

    /** Maximum number of credits a student may register for. */
    public static final int MAX_CREDITS = 17;

    private final String id;
    private final String firstName;
    private final String lastName;
    private final ArrayList<Course> courses = new ArrayList<>();

    /**
     * Creates a student with the supplied identity.
     *
     * @param id the student's ID
     * @param firstName the student's first name
     * @param lastName the student's last name
     * @throws IllegalArgumentException if any value is null or blank
     */
    public Student(String id, String firstName, String lastName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Student ID must not be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name must not be blank");
        }
        this.id = id.trim();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    /**
     * Returns the student's ID.
     *
     * @return the student ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the student's first name.
     *
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the student's last name.
     *
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns an immutable snapshot of the student's registered courses.
     *
     * @return an unmodifiable copy of the registered courses
     */
    public List<Course> getCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courses));
    }

    /**
     * Returns the total credits currently registered.
     *
     * @return the sum of the registered course credits
     */
    public double getTotalCredits() {
        return totalCredits(courses);
    }

    /**
     * Registers a course if it is available and fits the student's schedule and credit load.
     *
     * @param course the course section to register for
     * @return true when the course was added; false when it was full, duplicated,
     *         conflicting, or would exceed the credit limit
     * @throws NullPointerException if {@code course} is null
     */
    public boolean addCourse(Course course) {
        return addCourses(List.of(Objects.requireNonNull(course, "Course must not be null")));
    }

    /**
     * Registers all requested courses together, or none if any course cannot be added.
     *
     * @param requestedCourses the course sections to register for
     * @return true when all courses were added; false if any registration rule is violated
     * @throws NullPointerException if {@code requestedCourses} is null
     * @throws IllegalArgumentException if the collection contains null
     */
    public boolean addCourses(Collection<Course> requestedCourses) {
        Objects.requireNonNull(requestedCourses, "Courses must not be null");
        ArrayList<Course> additions = new ArrayList<>(requestedCourses);
        if (additions.isEmpty()) {
            return true;
        }
        if (additions.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Courses must not contain null");
        }

        ArrayList<Course> proposed = new ArrayList<>(courses);
        for (Course addition : additions) {
            if (isFull(addition) || containsCourse(proposed, addition)) {
                return false;
            }
            for (Course registered : proposed) {
                if (hasTimeConflict(addition, registered)) {
                    return false;
                }
            }
            proposed.add(addition);
        }

        if (totalCredits(proposed) > MAX_CREDITS) {
            return false;
        }

        courses.addAll(additions);
        return true;
    }

    /**
     * Drops a course matching the specified section.
     *
     * @param course the course section to drop
     * @return true when a registered course was removed
     * @throws NullPointerException if {@code course} is null
     */
    public boolean dropCourse(Course course) {
        Objects.requireNonNull(course, "Course must not be null");
        for (int index = 0; index < courses.size(); index++) {
            if (sameCourse(courses.get(index), course)) {
                courses.remove(index);
                return true;
            }
        }
        return false;
    }

    /**
     * Drops each specified course that is currently registered.
     *
     * @param coursesToDrop the course sections to drop
     * @return the number of courses removed
     * @throws NullPointerException if {@code coursesToDrop} is null
     * @throws IllegalArgumentException if the collection contains null
     */
    public int dropCourses(Collection<Course> coursesToDrop) {
        Objects.requireNonNull(coursesToDrop, "Courses must not be null");
        int dropped = 0;
        for (Course course : coursesToDrop) {
            if (course == null) {
                throw new IllegalArgumentException("Courses must not contain null");
            }
            if (dropCourse(course)) {
                dropped++;
            }
        }
        return dropped;
    }

    /**
     * Drops every registered course from the student's schedule.
     */
    public void dropAllCourses() {
        courses.clear();
    }

    /**
     * Determines whether a section has no remaining seats.
     *
     * @param course the section to check
     * @return {@code true} if the section is full
     */
    private static boolean isFull(Course course) {
        return course.seatsAvailable() != null && course.seatsAvailable() <= 0
            || course.maximumEnrollment() != null
            && course.enrollment() != null
            && course.enrollment() >= course.maximumEnrollment();
    }

    /**
     * Checks whether a section is already in a course list.
     *
     * @param existing the list to inspect
     * @param course the section to find
     * @return {@code true} if the list contains the section
     */
    private static boolean containsCourse(List<Course> existing, Course course) {
        return existing.stream().anyMatch(registered -> sameCourse(registered, course));
    }

    /**
     * Compares course sections using their registration identifier when available.
     *
     * @param first the first section
     * @param second the second section
     * @return {@code true} if both values identify the same section
     */
    private static boolean sameCourse(Course first, Course second) {
        if (notBlank(first.courseReferenceNumber()) && notBlank(second.courseReferenceNumber())) {
            return first.courseReferenceNumber().equals(second.courseReferenceNumber());
        }
        if (notBlank(first.id()) && notBlank(second.id())) {
            return first.id().equals(second.id());
        }
        return Objects.equals(first.subject(), second.subject())
            && Objects.equals(first.courseNumber(), second.courseNumber())
            && Objects.equals(first.sequenceNumber(), second.sequenceNumber());
    }

    /**
     * Tests whether a string contains a non-whitespace character.
     *
     * @param value the string to check
     * @return {@code true} if the value is not null or blank
     */
    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * Sums the credit values for a collection of courses.
     *
     * @param registeredCourses the courses to total
     * @return the summed credit value
     */
    private static double totalCredits(List<Course> registeredCourses) {
        double total = 0;
        for (Course course : registeredCourses) {
            total += courseCredits(course);
        }
        return total;
    }

    /**
     * Resolves a course's credit value from its available credit fields.
     *
     * @param course the course to inspect
     * @return the course credit value
     * @throws IllegalArgumentException if no valid credit value is available
     */
    private static double courseCredits(Course course) {
        if (notBlank(course.creditHours())) {
            try {
                return Double.parseDouble(course.creditHours().trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                    "Invalid credit value for " + course, exception
                );
            }
        }
        if (course.creditHourHigh() != null) {
            return course.creditHourHigh();
        }
        if (course.creditHourLow() != null) {
            return course.creditHourLow();
        }
        throw new IllegalArgumentException("Credit value is unavailable for " + course);
    }

    /**
     * Checks all meeting pairs for an overlap between two courses.
     *
     * @param first the first course
     * @param second the second course
     * @return {@code true} if any pair of meetings conflicts
     */
    private static boolean hasTimeConflict(Course first, Course second) {
        if (first.meetingsFaculty() == null || second.meetingsFaculty() == null) {
            return false;
        }
        for (Course.MeetingFaculty firstMeeting : first.meetingsFaculty()) {
            if (firstMeeting == null || firstMeeting.meetingTime() == null) {
                continue;
            }
            for (Course.MeetingFaculty secondMeeting : second.meetingsFaculty()) {
                if (secondMeeting != null && secondMeeting.meetingTime() != null
                    && meetingsConflict(firstMeeting.meetingTime(), secondMeeting.meetingTime())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks whether two meetings overlap on at least one day and time.
     *
     * @param first the first meeting
     * @param second the second meeting
     * @return {@code true} if their time intervals overlap
     */
    private static boolean meetingsConflict(
        Course.MeetingTime first,
        Course.MeetingTime second
    ) {
        if (!shareDay(first, second)) {
            return false;
        }

        LocalTime firstStart = parseTime(first.beginTime());
        LocalTime firstEnd = parseTime(first.endTime());
        LocalTime secondStart = parseTime(second.beginTime());
        LocalTime secondEnd = parseTime(second.endTime());
        return firstStart != null && firstEnd != null
            && secondStart != null && secondEnd != null
            && firstStart.isBefore(secondEnd)
            && secondStart.isBefore(firstEnd);
    }

    /**
     * Checks whether two meetings share a day of the week.
     *
     * @param first the first meeting
     * @param second the second meeting
     * @return {@code true} if both meetings occur on at least one common day
     */
    private static boolean shareDay(Course.MeetingTime first, Course.MeetingTime second) {
        return first.monday() && second.monday()
            || first.tuesday() && second.tuesday()
            || first.wednesday() && second.wednesday()
            || first.thursday() && second.thursday()
            || first.friday() && second.friday()
            || first.saturday() && second.saturday()
            || first.sunday() && second.sunday();
    }

    /**
     * Parses supported schedule time formats.
     *
     * @param value the time string to parse
     * @return the parsed time, or {@code null} if it is unavailable or unsupported
     */
    private static LocalTime parseTime(String value) {
        if (!notBlank(value)) {
            return null;
        }
        String time = value.trim();
        try {
            if (time.matches("\\d{3,4}")) {
                String padded = time.length() == 3 ? "0" + time : time;
                return LocalTime.of(
                    Integer.parseInt(padded.substring(0, 2)),
                    Integer.parseInt(padded.substring(2))
                );
            }
            if (time.matches("\\d{1,2}:\\d{2}\\s*[aApP][mM]")) {
                return LocalTime.parse(
                    time.replaceAll("\\s+", " ").toUpperCase(java.util.Locale.ROOT),
                    DateTimeFormatter.ofPattern("h:mm a")
                );
            }
            if (time.matches("\\d{1,2}:\\d{2}")) {
                return LocalTime.parse(time, DateTimeFormatter.ofPattern("H:mm"));
            }
            return null;
        } catch (DateTimeParseException | NumberFormatException exception) {
            return null;
        }
    }
}
