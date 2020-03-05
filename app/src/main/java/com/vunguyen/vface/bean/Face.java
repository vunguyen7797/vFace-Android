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
    private int faceId;                     // Face ID auto-generated in database
    private String studentFaceServerId;     // Face unique ID string to work with server tasks
    private String studentFaceImageUri;     // Face image as a URI string

    public Face()
    {

    }

    public Face (String studentServerId, String studentFaceServerId, String studentFaceImageUri)
    {
        this.studentServerId = studentServerId;
        this.studentFaceServerId = studentFaceServerId;
        this.studentFaceImageUri = studentFaceImageUri;
    }

    public Face(int id, String studentServerId, String studentFaceServerId, String studentFaceImageUri)
    {
        this.faceId = id;
        this.studentServerId = studentServerId;
        this.studentFaceServerId = studentFaceServerId;
        this.studentFaceImageUri = studentFaceImageUri;
    }

    public void setFaceId(int faceId)
    {
        this.faceId = faceId;
    }

    public int getFaceId()
    {
        return faceId;
    }
    public void setStudentFaceServerId(String studentFaceServerId)
    {
        this.studentFaceServerId = studentFaceServerId;
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
}
