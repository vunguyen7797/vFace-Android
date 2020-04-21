/*
 * Date.java
 */
package com.vunguyen.vface.bean;

/**
 * This class implements a customized date object attributes
 */
public class Date
{
    private String studentServerId;             // Student unique ID string to work with server tasks
    private String studentAttendanceStatus;     // Face unique ID string to work with server tasks
    private String courseServerId;              // Course unique ID string to work with server tasks
    private String student_date;                // Attendance date of student

    public Date(){}

    public Date(String courseServerId, String studentServerId, String student_date, String studentAttendanceStatus)
    {
        this.courseServerId = courseServerId;
        this.studentAttendanceStatus = studentAttendanceStatus;
        this.studentServerId = studentServerId;
        this.student_date = student_date;
    }

    public String getStudentServerId()
    {
        return studentServerId;
    }

    public void setStudentServerId(String studentServerId)
    {
        this.studentServerId = studentServerId;
    }

    public String getStudentAttendanceStatus()
    {
        return studentAttendanceStatus;
    }

    public void setStudentAttendanceStatus(String studentAttendanceStatus)
    {
        this.studentAttendanceStatus = studentAttendanceStatus;
    }

    public String getCourseServerId()
    {
        return courseServerId;
    }

    public void setCourseServerId(String courseServerId)
    {
        this.courseServerId = courseServerId;
    }

    public String getStudent_date()
    {
        return student_date;
    }

    public void setStudent_date(String student_date)
    {
        this.student_date = student_date;
    }
}
