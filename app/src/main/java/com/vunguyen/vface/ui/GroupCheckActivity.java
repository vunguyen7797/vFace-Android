package com.vunguyen.vface.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.vunguyen.vface.R;
import com.vunguyen.vface.bean.Course;
import com.vunguyen.vface.bean.Student;
import com.vunguyen.vface.helper.ApiConnector;
import com.vunguyen.vface.helper.ImageEditor;
import com.vunguyen.vface.helper.MyDatabaseHelperCourse;
import com.vunguyen.vface.helper.MyDatabaseHelperStudent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GroupCheckActivity extends AppCompatActivity
{

    // Background task of face identification.
    private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]>
    {
        private boolean succeed = true;
        String courseServerId;
        int identifyTurn;   // Index of  this task being executed
        int totalTurn;      // How many times this task being executed
        List<Pair<Bitmap, String>> studentIdentity = new ArrayList<>();

        IdentificationTask(String courseServerId, int identifyTurn, int totalTurn)
        {
            this.courseServerId = courseServerId;
            this.identifyTurn = identifyTurn;
            Log.i("EXECUTE" ," IDENTIFY TURN: " + identifyTurn);
            this.totalTurn = totalTurn;
        }

        @Override
        protected IdentifyResult[] doInBackground(UUID... params)
        {
            String logString = "Request: Identifying faces ";
            for (UUID faceId: params) {
                logString += faceId.toString() + ", ";
            }
            logString += " in group " + courseServerId;
            Log.i("EXECUTE", logString);

            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try
            {
                publishProgress("Getting course status...");

                TrainingStatus trainingStatus = faceServiceClient.getLargePersonGroupTrainingStatus(
                        this.courseServerId);
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Course training status is " + trainingStatus.status);
                    succeed = false;
                    return null;
                }


                publishProgress("Identifying...");
                Log.i("EXECUTE", "IDENTIFYING...");
                // Start identification.
                return faceServiceClient.identityInLargePersonGroup(
                        this.courseServerId,   /* personGroupId */
                        params,                  /* faceIds */
                        20);  /* maxNumOfCandidatesReturned */
            }
            catch (Exception e)
            {
                succeed = false;
                publishProgress(e.getMessage());
                Log.i("EXECUTE", "IDENTIFY ERROR: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute()
        {
            startProgressDialog();
        }


        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(IdentifyResult[] result)
        {
            if (result != null) {
                identifyResultsList.add(result);

                for (IdentifyResult identifyResult : result) {
                    DecimalFormat formatter = new DecimalFormat("#0.00");
                    if (identifyResult.candidates.size() > 0) {
                        String studentServerId =
                                identifyResult.candidates.get(0).personId.toString();

                        Student student = db_student.getAStudentWithId(studentServerId);

                        String studentName = student.getStudentName();
                        String studentIdNumber = student.getStudentIdNumber();
                        String course = spinCourses.getSelectedItem().toString();
                        String identity = "Student: " + studentName.toUpperCase() + "\n"
                                + "Student ID: " + studentIdNumber + "\n"
                                + "Course: " + course;

                        detectedDetailsList.add(identity);
                    } else
                        detectedDetailsList.add("UNKNOWN STUDENT");
                }

                if (identifyTurn == totalTurn) {
                    int i = 0;
                    for (String info : detectedDetailsList) {
                        Pair<Bitmap, String> pair = new Pair<Bitmap, String>(detectedFacesList.get(i), info);
                        studentIdentity.add(pair);
                        i++;
                    }

                    studentIdentityList = studentIdentity;
                    setUiAfterIdentification(succeed, studentIdentity);
                }
            }
            else
            {
                Log.i("EXECUTE", "ERROR IDENTIFY....");
            }
        }

        private void setUiAfterIdentification(boolean succeed, List<Pair<Bitmap, String>> studentIdentityList)
        {

            if (succeed)
            {
                progressDialog.dismiss();
                listViewAdapter = new FaceListViewAdapter(studentIdentityList);

                ListView listView = findViewById(R.id.lvIdentifiedFaces);
                listView.setAdapter(listViewAdapter);
            }
        }
    }

    private class FaceListViewAdapter implements ListAdapter
    {
        List<Bitmap> faceThumbnails;
        List<String> studentInfo;
        public FaceListViewAdapter()
        {
        }

        public FaceListViewAdapter(List<Pair<Bitmap, String>> studentIdentityList)
        {
            faceThumbnails = new ArrayList<>();
            studentInfo = new ArrayList<>();

            if (studentIdentityList != null)
            {
                for (Pair<Bitmap, String> pair : studentIdentityList)
                {
                    if (!pair.second.equalsIgnoreCase("UNKNOWN STUDENT"))
                    {
                        faceThumbnails.add(pair.first);
                        studentInfo.add(pair.second);
                    }

                }
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
        public int getCount() {
            return faceThumbnails.size();
        }

        @Override
        public Object getItem(int position) {
            return faceThumbnails.get(position);
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
            if (convertView == null)
            {
                LayoutInflater layoutInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }

            convertView.setId(position);

            // set face
            ((ImageView) convertView.findViewById(R.id.face_thumbnail)).setImageBitmap(faceThumbnails.get(position));
            //set info
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setText(studentInfo.get(position));
            ((TextView) convertView.findViewById(R.id.tvDetectedFace)).setTextColor(Color.WHITE);

            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return faceThumbnails.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }


    // Background task of face detection.
    private class DetectionTask extends AsyncTask<InputStream, String, Face[]> {
        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            FaceServiceClient faceServiceClient = ApiConnector.getFaceServiceClient();
            try{
                publishProgress("Detecting...");
                Log.i("EXECUTE", "DETECTING FACE");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        false,       /* Whether to return face landmarks */
                        null);
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute()
        {
            startProgressDialog();
        }


        @Override
        protected void onProgressUpdate(String... values) {
            // Show the status of background detection task on screen.a
            duringTaskProgressDialog(values[0]);
        }

        @Override
        protected void onPostExecute(Face[] result) {

            if (result != null)
            {
                for (Face face : result)
                {
                    try
                    {
                        detectedFacesList.add(ImageEditor.generateFaceThumbnail(bitmapImage, face.faceRectangle));

                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                facesList = Arrays.asList(result);

                if (result.length == 0) {
                    detected = false;
                    Log.i("EXECUTE", "NO FACE DETECTED!");
                }
                else
                {
                        Log.i("EXECUTE", "FACE DETECTED!");
                    detected = true;
                    progressDialog.dismiss();
                    identify();
                }

            }
            else
            {
                detected = false;
            }
        }
    }

    TextView tvDate;
    Spinner spinCourses;
    private ImageView ivClass;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int GALLERY_REQUEST_CODE = 1;
    private static final int PERMISSION_CODE = 1000;
    Uri uriImage;
    ListView lvIdentifiedFaces;
    Bitmap bitmapImage; // face image thumbnail
    private final List<Course> courseList = new ArrayList<>();
    boolean detected;
    String courseServerId;

    MyDatabaseHelperStudent db_student;
    ArrayAdapter<Course> spinnerArrayAdapter;

    List<IdentifyResult[]> identifyResultsList; // Store list of identify results after identify tasks
    List<String> detectedDetailsList;   // store the students' information after detection
    List<Bitmap> detectedFacesList;     // store the students' face in Bitmap after detection
    List<Pair<Bitmap, String>> studentIdentityList; // store the students' face and information after identification
    List<Face> facesList; // store face objects from the task results;
    FaceListViewAdapter listViewAdapter;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set no notification bar on activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_group_check);

        identifyResultsList = new ArrayList<>();
        detectedDetailsList = new ArrayList<>();
        detectedFacesList = new ArrayList<>();
        studentIdentityList = new ArrayList<>();
        facesList = new ArrayList<>();

        tvDate = findViewById(R.id.tvDate);
        setDate(tvDate);

        spinCourses = findViewById(R.id.spinClass);

        MyDatabaseHelperCourse db = new MyDatabaseHelperCourse(this);
        List<Course> listCourses=  db.getAllCourses();
        this.courseList.addAll(listCourses);

        spinnerArrayAdapter = new ArrayAdapter<Course>(this, R.layout.spinner_item, listCourses);
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinCourses.setAdapter(spinnerArrayAdapter);



        // display list of students on course selection
        spinCourses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                Course course = (Course) parent.getItemAtPosition(position);
                courseServerId = course.getCourseServerId();    // get course id on server
                Log.i("EXECUTE", "Course Selected: " + courseServerId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                spinCourses.setSelection(0);
                Course course = (Course) spinCourses.getItemAtPosition(0);
                courseServerId = course.getCourseServerId();
            }
        });

        ivClass = findViewById(R.id.ivClassImage);
        lvIdentifiedFaces = findViewById(R.id.lvIdentifiedFaces);

        db_student = new MyDatabaseHelperStudent(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("V.FACE");
    }

    private void startProgressDialog()
    {
        progressDialog.show();
    }

    private void duringTaskProgressDialog(String progress)
    {
        progressDialog.setMessage(progress);
    }

    public void setDate(TextView view)
    {
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
        String date = formatter.format(today);
        view.setText(date);
    }

    public void takePicture(View view)
    {
        detectedDetailsList.clear();
        detectedFacesList.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

                requestPermissions(permission, PERMISSION_CODE);
            }
            else
            {
                openCamera();
            }
        }
        else
        {
            openCamera();
        }
    }

    private void openCamera()
    {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        uriImage = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openCamera();
                else
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void pickImage(View view)
    {

        detectedDetailsList.clear();
        detectedFacesList.clear();
        Intent intPickImage = new Intent(Intent.ACTION_PICK);
        intPickImage.setType("image/*");

        String[] mimeTypes = {"image/jpeg", "image/png"};
        intPickImage.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intPickImage, GALLERY_REQUEST_CODE);

    }


    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {
        int MAX_HEIGHT = 1920;
        int MAX_WIDTH = 1920;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, selectedImage);
        return img;
    }

    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     *                  method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        detected = false;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            try
            {
                bitmapImage = handleSamplingAndRotationBitmap(this, uriImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bitmapImage != null) {
                // Show the image on screen.

                ivClass.setImageBitmap(bitmapImage);
                detect(bitmapImage);
            }
        }

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK)
        {
            Uri selectedImage = data.getData();
            try
            {
                bitmapImage = handleSamplingAndRotationBitmap(this, selectedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bitmapImage != null) {
                // Show the image on screen.

                ivClass.setImageBitmap(bitmapImage);
                detect(bitmapImage);
            }
        }

        if (resultCode != RESULT_OK)
        {
            Log.i("EXECUTE", "NO IMAGE CHOSEN");
            if (Build.VERSION.SDK_INT >= 11)
            {
                recreate();
            }
            else
            {
                Intent intent = getIntent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finish();
                overridePendingTransition(0, 0);

                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        }


    }


    // Start detecting in image.
    private void detect(Bitmap bitmap)
    {
        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Start a background task to detect faces in the image.
        new DetectionTask().execute(inputStream);
        Log.i("EXECUTE","Detected value: " + detected);

    }

    private void identify()
    {
        if (detected && courseServerId != null)
        {
            Log.i("EXECUTE", "DETECTED IDENTIFY = " + detected);
            int count = 0;  // counter for each faceId set
            int totalTurn = 0;  // counter the numbers of set added to the list

            List<List<UUID>> faceIdList = new ArrayList<>(); // contain multiples faces set of 10 for identify

            // initialize the maximum number of faces can be identified at one time is 100
            for (int i = 0; i < 10; i++)
            {
                List<UUID> faceIds = new ArrayList<>();
                faceIdList.add(faceIds);
            }

            for (Face face : facesList)
            {
                // add 10 faces as an element in faceIds List, since Microsoft limits 10 faces can be identified at one
                if (count < 10)
                {
                    Log.i("EXECUTE" ," FACES NUMBER LESS THAN 10");
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
                else if (count == 10)
                {
                    Log.i("EXECUTE" ," FACES NUMBER REACH 10");
                    totalTurn++;    // one set added, move to the next set if necessary
                    count = 0;      // reset count counter
                    faceIdList.get(totalTurn).add(face.faceId);
                    count++;
                }
            }

            // Execute multiple identification tasks for each set of 10 faces
            for (int i = 0; i < totalTurn + 1; i++)
            {
                new IdentificationTask(courseServerId, i, totalTurn)
                        .execute(faceIdList.get(i).toArray(new UUID[faceIdList.get(i).size()]));
            }
        }
        else
        {
            // Not detected or person group exists.
            Log.i("EXECUTE","Please select an image and create course first.");
        }
    }
}
