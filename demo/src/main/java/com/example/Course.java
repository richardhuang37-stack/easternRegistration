package com.example;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Course record to store select course information
 * @param id course id
 * @param subject the course subject
 * @param courseNumber the course number
 * @param meetingsFaculty a list of meetingFaculty objects (see @link com.example.MeetingFaculty)
 * @param courseTitle the course title
 * @param creditHours the displayed course credit value, when available
 * @param sequenceNumber the course section number
 * @param creditHourLow the minimum course credits
 * @param creditHourHigh the maximum course credits
 * @param courseReferenceNumber the section's registration identifier
 * @param maximumEnrollment the maximum section enrollment
 * @param enrollment the current section enrollment
 * @param seatsAvailable the currently available seats
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
    String id,
    String subject,
    String courseNumber,
    List<MeetingFaculty> meetingsFaculty,
    String courseTitle,
    String creditHours,
    String sequenceNumber,
    Double creditHourLow,
    Double creditHourHigh,
    String courseReferenceNumber,
    Integer maximumEnrollment,
    Integer enrollment,
    Integer seatsAvailable
) {

    public Course(String id, String subject, String courseNumber, List<MeetingFaculty> meetingsFaculty) {
        this(
            id, subject, courseNumber, meetingsFaculty, null, null, null, null, null,
            null, null, null, null
        );
    }

    public Course(
        String id,
        String subject,
        String courseNumber,
        List<MeetingFaculty> meetingsFaculty,
        String courseTitle,
        String creditHours,
        String sequenceNumber,
        Double creditHourLow,
        Double creditHourHigh
    ) {
        this(
            id, subject, courseNumber, meetingsFaculty, courseTitle, creditHours,
            sequenceNumber, creditHourLow, creditHourHigh, null, null, null, null
        );
    }

    /**
     * 
     * MeetingFaculty record to store select meetingFaculty information
     * @param meetingTime a meetingTime object (see @link com.example.MeetingTime)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingFaculty(MeetingTime meetingTime) {

       /**  
        * Returns a string representation of the meeting faculty objet     
        * @return the formatted meeting faculty string
        */
        @Override 
        public String toString() {
            return meetingTime.toString();
        }
    }
       
    /**
     * 
     * MeetingTime record to store select meetingTime information
     * @param monday true if meeting time is on this day; false otherwise
     * @param tuesday true if meeting time is on this day; false otherwise
     * @param wednesday true if meeting time is on this day; false otherwise
     * @param thursday true if meeting time is on this day; false otherwise
     * @param friday true if meeting time is on this day; false otherwise
     * @param saturday true if meeting time is on this day; false otherwise
     * @param sunday true if meeting time is on this day; false otherwise
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeetingTime(
        boolean monday, boolean tuesday, boolean wednesday,
        boolean thursday, boolean friday, boolean saturday, boolean sunday,
        String beginTime, String endTime
    ){

        public MeetingTime(
            boolean monday, boolean tuesday, boolean wednesday,
            boolean thursday, boolean friday, boolean saturday, boolean sunday
        ) {
            this(monday, tuesday, wednesday, thursday, friday, saturday, sunday, null, null);
        }

        /**  
        * Returns a string representation of the meeting time     
        * @return the formatted meeting time string
        */
        @Override 
        public String toString() {
            StringBuilder days = new StringBuilder();
            appendDay(days, "M", monday);
            appendDay(days, "T", tuesday);
            appendDay(days, "W", wednesday);
            appendDay(days, "R", thursday);
            appendDay(days, "F", friday);
            appendDay(days, "S", saturday);
            appendDay(days, "U", sunday);

            String meetingDays = days.length() == 0 ? "Days unavailable" : days.toString();
            String meetingHours = formatTimeRange(beginTime, endTime);
            return meetingHours.isEmpty()
                ? meetingDays
                : meetingDays + ", " + meetingHours;
        }

        private static void appendDay(StringBuilder days, String abbreviation, boolean occurs) {
            if (occurs) {
                days.append(abbreviation);
            }
        }

        private static String formatTimeRange(String begin, String end) {
            String formattedBegin = normalizeTime(begin);
            String formattedEnd = normalizeTime(end);
            if (formattedBegin.isEmpty() || formattedEnd.isEmpty()) {
                return "";
            }
            String beginPeriod = periodOf(formattedBegin);
            String endPeriod = periodOf(formattedEnd);
            if (!beginPeriod.isEmpty() && beginPeriod.equals(endPeriod)) {
                formattedBegin = formattedBegin.substring(0, formattedBegin.length() - 3).trim();
            }
            return formattedBegin + " - " + formattedEnd;
        }

        private static String periodOf(String time) {
            String upper = time.toUpperCase(java.util.Locale.ROOT);
            if (upper.endsWith(" AM") || upper.endsWith(" PM")) {
                return upper.substring(upper.length() - 2);
            }
            return "";
        }

        private static String normalizeTime(String time) {
            if (time == null || time.isBlank()) {
                return "";
            }

            String trimmed = time.trim();
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d{1,2}):(\\d{2})\\s*([aApP][mM])$")
                .matcher(trimmed);
            if (matcher.matches()) {
                return Integer.parseInt(matcher.group(1)) + ":" + matcher.group(2)
                    + " " + matcher.group(3).toUpperCase(java.util.Locale.ROOT);
            }

            try {
                java.time.LocalTime parsed;
                if (trimmed.matches("\\d{3,4}")) {
                    String padded = trimmed.length() == 3 ? "0" + trimmed : trimmed;
                    parsed = java.time.LocalTime.of(
                        Integer.parseInt(padded.substring(0, 2)),
                        Integer.parseInt(padded.substring(2))
                    );
                } else if (trimmed.matches("\\d{1,2}:\\d{2}")) {
                    parsed = java.time.LocalTime.parse(
                        trimmed,
                        java.time.format.DateTimeFormatter.ofPattern("H:mm")
                    );
                } else {
                    return trimmed;
                }
                return parsed.format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "h:mm a",
                        java.util.Locale.US
                    )
                );
            } catch (java.time.DateTimeException | NumberFormatException exception) {
                return trimmed;
            }
        }
    }; 

    /**  
     * Returns a string representation of the course     
     * @return the formatted course string
     */
    @Override 
     public String toString() {
        return subject + "-" + courseNumber;
    }
}
    

   
