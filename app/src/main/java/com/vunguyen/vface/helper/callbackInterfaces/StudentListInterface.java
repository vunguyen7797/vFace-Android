package com.vunguyen.vface.helper.callbackInterfaces;

import com.vunguyen.vface.bean.Student;

import java.io.IOException;
import java.util.List;

public interface StudentListInterface
{
    void getStudentList(List<Student> studentList) throws IOException;
}
