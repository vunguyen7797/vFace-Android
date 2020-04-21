/*
 * Course.java
 */
package com.vunguyen.vface.bean;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * The class contains attributes of a course object
 */
public class Course implements Serializable
{
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
    @NotNull
    @Override
    public String toString()
    {
        return this.courseName;
    }
}
