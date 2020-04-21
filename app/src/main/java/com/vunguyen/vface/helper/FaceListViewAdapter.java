/*
 * FaceListViewAdapter.java
 */
package com.vunguyen.vface.helper;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.bean.StudentInfoPackage;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a customize adapter to display identified students
 * after detection and identification
 */
public class FaceListViewAdapter implements ListAdapter
{
    private List<Bitmap> faceThumbnails;
    private List<Uri> faceThumbnailsUri;
    private List<String> studentInfo;
    private List<String> studentAbsence;
    private Context context;
    private String request;
    private String mode;
    //private List<Student> student;
    private List<StudentInfoPackage> student;

    // when input photos are in bitmap format loading from memory
    public FaceListViewAdapter(List<Pair<Bitmap, String>> studentIdentityList, Context context)
    {
        this.context = context;
        this.mode = "normal";
        faceThumbnails = new ArrayList<>();
        studentInfo = new ArrayList<>();
        this.request = "bitmap";
        if (studentIdentityList != null)
        {
            for (Pair<Bitmap, String> pair : studentIdentityList)
            {
                faceThumbnails.add(pair.first);

                if (!pair.second.equalsIgnoreCase("UNKNOWN STUDENT"))
                {
                    int index = pair.second.indexOf('\n');
                    studentInfo.add(pair.second.substring(0, index));
                }
                else
                    studentInfo.add(pair.second);
            }
        }
    }

    // when input photos are in uri format loading from server database
    public FaceListViewAdapter(List<Pair<Uri, String>> studentIdentityList, String request, Context context)
    {
        this.context = context;
        this.mode = "normal";
        faceThumbnailsUri = new ArrayList<>();
        studentInfo = new ArrayList<>();
        this.request = request;

        if (studentIdentityList != null)
        {
            for (Pair<Uri, String> pair : studentIdentityList)
            {
                faceThumbnailsUri.add(pair.first);

                if (!pair.second.equalsIgnoreCase("UNKNOWN STUDENT"))
                {
                    int index = pair.second.indexOf('\n');
                    studentInfo.add(pair.second.substring(0, index));
                }
                else
                    studentInfo.add(pair.second);
            }
        }
    }

    public FaceListViewAdapter(List<Pair<Pair<Uri, String>, Pair<Student, Integer>>> studentIdentityList
            , String request, Context context, String mode)
    {
        Log.i("EXECUTE", "Display list size: " + studentIdentityList.size());
        this.context = context;
        this.request = request;
        this.mode = mode;
        faceThumbnailsUri = new ArrayList<>();
        studentInfo = new ArrayList<>();
        studentAbsence = new ArrayList<>();
        student = new ArrayList<>();

        for (Pair<Pair<Uri, String>, Pair<Student, Integer>> pair : studentIdentityList)
        {
            faceThumbnailsUri.add(pair.first.first);
            studentInfo.add(pair.first.second);
            Pair<Student, Uri> pair1 = new Pair<>(pair.second.first, pair.first.first);
            Pair<Pair<Student, Uri>, Integer> finalPair = new Pair<>(pair1, pair.second.second);
            StudentInfoPackage studentInfoPackage = new StudentInfoPackage(finalPair);
            student.add(studentInfoPackage);
            studentAbsence.add(pair.second.second.toString());
        }
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount()
    {
        if (request.equalsIgnoreCase("uri"))
            return faceThumbnailsUri.size();
        else
            return faceThumbnails.size();
    }

    @Override
    public Object getItem(int position)
    {
        if (request.equalsIgnoreCase("uri"))
            return student.get(position);
        else
            return student.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (this.mode.equalsIgnoreCase("withTotalAbsence")) {
                assert layoutInflater != null;
                convertView = layoutInflater.inflate(R.layout.item_face_with_description_2, parent, false);
            }
            else {
                assert layoutInflater != null;
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }
        }

        convertView.setId(position);

        if (request.equalsIgnoreCase("uri") || request.equalsIgnoreCase("absence"))
        {
            Picasso.get().load(faceThumbnailsUri.get(position)).into((ImageView) convertView.findViewById(R.id.face_thumbnail));
        }
        else
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(faceThumbnails.get(position));

        //set info
        ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setText(studentInfo.get(position));
        ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setTextColor(Color.WHITE);

        if (this.mode.equalsIgnoreCase("withTotalAbsence"))
        {
            //set info
            ((TextView) convertView.findViewById(R.id.tvAbsence)).setText(studentAbsence.get(position));
            ((TextView) convertView.findViewById(R.id.tvAbsence)).setTextColor(Color.WHITE);
        }

        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}