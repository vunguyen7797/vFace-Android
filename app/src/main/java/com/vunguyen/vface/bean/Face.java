/*
 * Face.java
 */
package com.vunguyen.vface.bean;

import java.io.Serializable;

/**
 * The class contains attributes of a Face object
 */
public class Face implements Serializable
{
    private String studentServerId;         // Student unique ID string to work with server tasks
    private String studentNumberId;                    // Face ID auto-generated in database
    private String studentFaceServerId;     // Face unique ID string to work with server tasks
    private String studentFaceImageUri;     // Face image as a URI string
    private String courseServerId;

    public Face()
    {

    }

    public Face (String courseServerId, String studentNumberId, String studentServerId,
                 String studentFaceServerId, String studentFaceImageUri)
    {
        this.courseServerId = courseServerId;
        this.studentNumberId = studentNumberId;
        this.studentServerId = studentServerId;
        this.studentFaceServerId = studentFaceServerId;
        this.studentFaceImageUri = studentFaceImageUri;
    }

    public String getStudentFaceServerId()
    {
        return studentFaceServerId;
    }

    public void setStudentFaceUri(String studentFaceImageUri)
    {
        this.studentFaceImageUri = studentFaceImageUri;
    }

    public String getStudentFaceUri()
    {
        return studentFaceImageUri;
    }

    public void setStudentServerId(String studentServerId)
    {
        this.studentServerId = studentServerId;
    }

    public String getStudentServerId()
    {
        return studentServerId;
    }

    public String getStudentNumberId() {
        return studentNumberId;
    }

    public void setStudentNumberId(String studentNumberId) {
        this.studentNumberId = studentNumberId;
    }

    public String getCourseServerId() {
        return courseServerId;
    }

    public void setCourseServerId(String courseServerId) {
        this.courseServerId = courseServerId;
    }
}
