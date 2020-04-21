/*
 * Student.java
 */
package com.vunguyen.vface.bean;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * This class contains attributes of a Student object
 */
public class Student implements Serializable
{
    private String courseServerId;      // Course unique ID string to work with server tasks
    private String studentName;         // Student name from input
    private String studentIdNumber;     // Real student ID number from input
    private String studentServerId;     // Student unique ID string to work with server tasks
    private String studentIdentifyFlag; // Indicator if the student is identified or not.
    private String studentServerIdImport; // Original student server id
    private int numberOfFaces;            // Number of faces

    public Student()
    {

    }

    public Student (String studentIdNumber, String courseServerId, String studentName, String studentServerId)
    {
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
        this.studentServerIdImport = "";
    }

    public Student (String studentIdNumber, String courseServerId, String studentName, String studentServerId, String studentIdentifyFlag)
    {
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
        this.studentIdentifyFlag = studentIdentifyFlag;
        this.studentServerIdImport = "";
    }

    public Student (String studentServerIdImport, String studentIdNumber, String courseServerId
            , String studentName, String studentServerId, String studentIdentifyFlag, int numberOfFaces)
    {
        this.courseServerId = courseServerId;
        this.studentIdNumber = studentIdNumber;
        this.studentName = studentName;
        this.studentServerId = studentServerId;
        this.studentIdentifyFlag = studentIdentifyFlag;
        this.studentServerIdImport = studentServerIdImport;
        this.numberOfFaces = numberOfFaces;
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
    @NotNull
    @Override
    public String toString()
    {
        return this.studentName + " (" + this.studentIdNumber + ")";
    }

    public String getStudentServerIdImport() {
        return studentServerIdImport;
    }

    public void setStudentServerIdImport(String studentServerIdImport) {
        this.studentServerIdImport = studentServerIdImport;
    }

    public int getNumberOfFaces() {
        return numberOfFaces;
    }

    public void setNumberOfFaces(int numberOfFaces) {
        this.numberOfFaces = numberOfFaces;
    }
}
