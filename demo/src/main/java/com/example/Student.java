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
 * A student and the courses in which they are registered.
 */
public class Student {

    public static final int MAX_CREDITS = 17;

    private final String id;
    private final String firstName;
    private final String lastName;
    private final ArrayList<Course> courses = new ArrayList<>();

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

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    /**
     * Returns an immutable snapshot of the student's registered courses.
     */
    public List<Course> getCourses() {
        return Collections.unmodifiableList(new ArrayList<>(courses));
    }

    /**
     * Returns the total credits currently registered.
     */
    public double getTotalCredits() {
        return totalCredits(courses);
    }

    /**
     * Registers a course if it is available and fits the student's schedule and credit load.
     *
     * @return true when the course was added; false when it was full, duplicated,
     *         conflicting, or would exceed the credit limit
     */
    public boolean addCourse(Course course) {
        return addCourses(List.of(Objects.requireNonNull(course, "Course must not be null")));
    }

    /**
     * Registers all requested courses together, or none if any course cannot be added.
     *
     * @return true when all courses were added; false if any registration rule is violated
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
     * @return true when a registered course was removed
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
     * @return the number of courses removed
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
     * Drops every registered course.
     */
    public void dropAllCourses() {
        courses.clear();
    }

    private static boolean isFull(Course course) {
        return course.seatsAvailable() != null && course.seatsAvailable() <= 0
            || course.maximumEnrollment() != null
            && course.enrollment() != null
            && course.enrollment() >= course.maximumEnrollment();
    }

    private static boolean containsCourse(List<Course> existing, Course course) {
        return existing.stream().anyMatch(registered -> sameCourse(registered, course));
    }

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

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static double totalCredits(List<Course> registeredCourses) {
        double total = 0;
        for (Course course : registeredCourses) {
            total += courseCredits(course);
        }
        return total;
    }

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

    private static boolean shareDay(Course.MeetingTime first, Course.MeetingTime second) {
        return first.monday() && second.monday()
            || first.tuesday() && second.tuesday()
            || first.wednesday() && second.wednesday()
            || first.thursday() && second.thursday()
            || first.friday() && second.friday()
            || first.saturday() && second.saturday()
            || first.sunday() && second.sunday();
    }

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
