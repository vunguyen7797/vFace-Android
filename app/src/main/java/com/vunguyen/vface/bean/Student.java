/*
 * Student.java
 */
package com.vunguyen.vface.bean;

import java.io.Serializable;

/**
 * This class contains attributes of a Student object
 */
public class Student implements Serializable
{
    private int studentId;              // Student ID auto-generated in database
    private String courseServerId;      // Course unique ID string to work with server tasks
    private String studentName;         // Student name from input
    private String studentIdNumber;     // Real student ID number from input
    private String studentServerId;     // Student unique ID string to work with server tasks
    private String studentIdentifyFlag; // Indicator if the student is identified or not.

    public Student()
    {

    }

    public Student (String studentIdNumber, String courseServerId, String studentName, String studentServerId)
    {
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
    }

    public Student (String studentIdNumber, String courseServerId, String studentName, String studentServerId, String studentIdentifyFlag)
    {
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
        this.studentIdentifyFlag = studentIdentifyFlag;
    }

    public Student(int id, String courseServerId, String studentIdNumber, String studentName, String studentServerId)
    {
        this.studentId = id;
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
    }

    public String getCourseServerId()
    {
        return courseServerId;
    }

    public void setCourseServerId(String courseServerIdId)
    {
        this.courseServerId = courseServerIdId;
    }

    public String getStudentIdentifyFlag()
    {
        return studentIdentifyFlag;
    }

    public void setStudentIdentifyFlag(String studentIdentifyFlag)
    {
        this.studentIdentifyFlag = studentIdentifyFlag;
    }

    public int getStudentId()
    {
        return studentId;
    }

    public void setStudentId(int studentId)
    {
        this.studentId = studentId;
    }

    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    public String getStudentIdNumber()
    {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber)
    {
        this.studentIdNumber = studentIdNumber;
    }

    public void setStudentServerId(String studentServerId)
    {
        this.studentServerId = studentServerId;
    }

    public String getStudentServerId()
    {
        return studentServerId;
    }

    // This method will return the student id number if the object is requested to return string
    @Override
    public String toString()
    {
        return this.studentIdNumber + " - " + this.studentName;
    }
}
