# vFACE: Checking attendance by face recognition

To keep track of the attendance of students, face recognition is the fastest way to identify students with high accuracy based on the class data. This app was made to assist the teacher, professor checking attendance just in few seconds by taking a photo of the whole classroom at the beginning of the class by using the smartphone camera. The app will automatically detect students' faces, identify them by student ID number, and display the list of absence students. There is also a basic attendance statistic feature that calculates the total absences of a student in the semester so that the professor can easily make the decision on a student/s grade if attendance plays an important role in the students' success.

## Developer
* Vu Nguyen

## Features:
* Checking attendance by a group photo via Group-check mode
* Keeping track of the class attendance situation via Attendance manager
* Adding courses and students data including faces via Data manager

## Upcoming features:
* Checking attendance one by one via Self-check mode
* Changing languages + timezones via Settings
* UI customizing via Settings.
* Notifications, etc.

## Getting Started
These instructions will get you a copy of the project up and runiing on your local machine for testing purposes.
### Prerequisites
```
* Android Studio with API level 22 or later (Android OS must be Android 5.1 or higher).
* Min SDK version 26 (required by Face client library)
* A smartphone with camera front and back (Recommended). The virtual phone camera will not be able to capture your face or a group of people.
```
### Build the project
```
* First, you must obtain a free trial Face API subscription key 
via this link https://azure.microsoft.com/en-us/try/cognitive-services/?api=face-api
* After opening the project using Android Studio, take a look at the left panel, 
if you are in "Android" mode, go to "res" directory -> "values" -> "string.xml", and find the line with name "subscription_key". Put your subscription key from the first step between the tags.
* Then select menu "Build -> Make Project" to build the project.
```
### Run the app
* Once the project is executed and launched on your phone, go to Data Manager to add a course and add some students.
* Then go to group check to take a photo of a group of people including some students who were added in databases to check attendance.

![Image description](https://i.ibb.co/42bKRmX/Screenshot-2020-03-16-14-15-00-127-com-example-vface.jpg)

### Timeline:
 * 1/15/2020: Come up with the idea, finish the prototypes.
 * 2/1/2020 - 2/15/2020: Started working on User Interface design. Finish the UI based on the prototypes.
 * 2/15/2020 - 2/29/2020: Working on the main features, implementing face detection API, database.
 * 2/29/2020 - 3/15/2020: Most of the main features are working except for Self-check - has not been implemented yet.
 Creating Readme Github, update comments and organizing code to get ready for peer review.
 * 3/15/2020 - 3/30/2020: Implementing Self-check mode, and improve other features. Get ready for the first complete version.
