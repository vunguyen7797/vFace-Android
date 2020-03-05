/*
 * Course.java
 */
package com.vunguyen.vface.bean;

import java.io.Serializable;

/**
 * The class contains attributes of a course object
 */
public class Course implements Serializable
{
    private int courseId;           // Course ID auto-generated in database
    private String courseName;      // Course name from input
    private String courseIdNumber;  // Real course ID number from input
    private String courseServerId;  // Course unique ID string to work with server tasks.

    public Course()
    {

    }

    public Course(String courseIdNumber, String courseName, String courseServerId)
    {
        this.courseIdNumber = courseIdNumber;
        this.courseName = courseName;
        this.courseServerId = courseServerId;
    }

    public Course(int id, String courseIdNumber, String courseName, String courseServerId)
    {
        this.courseId = id;
        this.courseIdNumber = courseIdNumber;
        this.courseName = courseName;
        this.courseServerId = courseServerId;
    }

    public int getCourseId()
    {
        return courseId;
    }

    public void setCourseId(int courseId)
    {
        this.courseId = courseId;
    }

    public String getCourseName()
    {
        return courseName;
    }

    public void setCourseName(String courseName)
    {
        this.courseName = courseName;
    }

    public String getCourseIdNumber()
    {
        return courseIdNumber;
    }

    public void setCourseIdNumber(String courseIdNumber)
    {
        this.courseIdNumber = courseIdNumber;
    }

    public String getCourseServerId()
    {
        return courseServerId;
    }

    public void setCourseServerId(String courseServerId)
    {
        this.courseServerId = courseServerId;
    }

    // This method will return course name if the object is requested to return string
    @Override
    public String toString()
    {
        return this.courseName;
    }
}
