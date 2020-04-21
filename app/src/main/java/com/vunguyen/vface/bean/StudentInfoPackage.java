/*
 * StudentInfoPackage.java
 */
package com.vunguyen.vface.bean;

import android.net.Uri;
import android.util.Pair;

/**
 * This class create an object that contains all the attributes of a student
 */
public class StudentInfoPackage
{
    private Pair<Pair<Student, Uri>, Integer> student;

    public StudentInfoPackage(Pair<Pair<Student, Uri>, Integer> student)
    {
        this.student = student;
    }

    public Pair<Pair<Student, Uri>, Integer> getStudent() {
        return student;
    }

    public void setStudent(Pair<Pair<Student, Uri>, Integer> student) {
        this.student = student;
    }
}
