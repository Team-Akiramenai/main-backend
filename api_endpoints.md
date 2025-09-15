# Basic overview

All the API endpoints listed below will need to be prepended with `localhost:8080`.

The endpoints under `api/public` are accessible without any JWT auth token.
The endpoints under `api/private` are accessible with the help of a JWT auth token.

# Seeding the database with mock data

1) GET /api/public/seed

This will populate the database with the user:

```
Username: amanda
Email: amanda@gmail.com
Password: 12345678
```

It will also add 100 courses with names like **Course Title 1,2,3,...** and publish them in different timestamps.

## User Controller

1) POST api/public/register

Creates a user account of the requested type.

Request body's JSON:

```json
{
  "username": "shaggy",
  "password": "1234567890",
  "email": "shaggy@gmail.com",
  "accountType": "Learner"
}
```

Response: HTTP 201 with confirmation text

2) POST api/public/login

Logs in the user by sending the JWT auth token in the response and sets the JWT refresh token using HTTP-only cookie.
The cookie is inaccessible from the frontend JS.

Request body's JSON:

```json
{
  "email": "shaggy@gmail.com",
  "password": "1234567890"
}
```

Response: HTTP 200 with the following JSON response

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJzaGFnZ3lAZ21haWwuY29tIiwiaWF0IjoxNzU1ODc3NjMxLCJleHAiOjE3NTU4Nzc5MzEsImFjY291bnRUeXBlIjoiTGVhcm5lciIsInVzZXJJZCI6IjA0MjYwYTE2LWJlYWMtNDlkMy05MjI2LTQ3MTYyNjU0MGJmOCJ9.TdWALjIMMC4hiwH9ghMF489qXWVrybI58xmwIasErCJnhcHQyZIUz6keYbjxGmTq",
  "accountType": "Learner"
}
```

3) POST api/public/logout

Logs out the user by removing the JWT refresh token storing HTTP-only cookie. The frontend needs to drop the JWT auth
token stored
in memory too.

4) GET api/public/get/user-profile-picture/52ccb25e-930d-4036-b4b0-c6dad8d45084

Fetches the profile picture of the associated user.

Example request:
`GET api/public/get/user-profile-picture/52ccb25e-930d-4036-b4b0-c6dad8d45084`

Response: User's profile picture (PNG or JPEG).

5) POST api/protected/change-profile-picture

Changes the profile picture with the uploaded picture.

The picture needs to be uploaded as a form-data with the name `new-profile-picture`. We accept PNG and JPEG for now.
Make sure to set the appropriate content type for them.

5) POST api/protected/update-password

Updates the password to the newly provided password if the old password matches.

Request body's JSON:

```json
{
  "oldPassword": "12345678",
  "newPassword": "hell0kitty"
}
```

Response: HTTP 200 with confirmation text

6) POST api/public/update-username

Updates the user to the newly provided username.

Request body's JSON:

```json
{
  "newUsername": "sayonala"
}
```

Response: HTTP 200 with confirmation text

7) GET api/protected/get/user-info

Get current user's account information.

Response JSON:

```json
{
  "userId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
  "username": "Amanda",
  "email": "amanda@gmail.com",
  "userType": "Instructor",
  "pfpPath": "default-user-logo.png",
  "totalStorageInBytes": 11811160064,
  "usedStorageInBytes": 445575411
}
```

8) GET api/public/refresh-access-token

Get a new access token using the refresh token stored in the HTTP-only cookie.

Response JSON:

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhbWFuZGFAZ21haWwuY29tIiwiaWF0IjoxNzU2Mzg1Njc5LCJleHAiOjE3NTYzODU5NzksImFjY291bnRUeXBlIjoiSW5zdHJ1Y3RvciIsInVzZXJJZCI6IjNjZWE0ZTM3LTc0MjEtNGMxMi1hY2EyLWU0ZGQ1NWU5MjYwOCJ9.DwNyQn_jrqFETl7J8V6e01428xQC0V8gzO8sgqtN9A951OIoy0w9yjFAVGrWU36U"
}
```

## Course Controller

0) GET api/public/get/course?courseId=<<Item-ID>>

Returns the information about the specified course.

Example GET request:

```
GET api/public/get/course?courseId=cbdc0b4a-6f0b-4964-9964-24a7f4775207
```

Response JSON:

```json
{
  "id": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
  "title": "Course Title #1",
  "description": "Course Description #1",
  "thumbnailImageId": null,
  "courseItemIds": [
    "QZ_2bb1c9a4-33ba-4cc8-983c-a1a6d41402e1",
    "QZ_e873ef10-5e29-4c82-b70a-5572b8cfa8b4",
    "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
    "CT_26cc5297-4aa4-4a6c-b179-b8563df21766",
    "QZ_ac206c1a-19ea-4a09-974c-dbf674af82ba",
    "QZ_00505192-b460-46aa-a572-a6525996561f"
  ],
  "price": 4.2,
  "rating": 0.0,
  "createdAt": "2025-08-23T15:27:15.355931",
  "lastModifiedAt": "2025-08-23T15:27:15.391495"
}
```

### 1) GET api/public/get/course-thumbnail/<<User-ID>>

Returns associated course thumbnail (PNG or JPEG).

Example request:

`GET /api/public/get/course-thumbnail/5c9f3cf4-f0af-462b-84f6-9f5bdba2b03e`

Response: Course thumbnail (PNG or JPEG)

### 2) POST api/protected/change-course-thumbnail

Updates/changes the course's thumbnail.

Example request:

`POST api/protected/change-course-thumbnail`

**The POST request must use form-data to send both `course-id` and `new-thumbnail` for it to be a valid request.
We accept PNG or JPEG images. Make sure to set the appropriate `Content-Type` for them.**

Response JSON:

```json
{
  "itemId": "2eceef0e-0e76-409b-98df-ba72bbe6d8d9"
}
```

### 3) GET api/public/get/courses?page=0&page-size=1&sorting=DESC

Returns a paginated list of published courses in JSON.

Explanation of the URL parameters:
a) page -> Zero indexed page number for the course list (default: 0)
b) page-size -> The number of courses per page (default: 5)
c) sorting -> DESC <OR> ASC (default: ASC)

So, the following requests are the same:

GET api/public/get/courses

GET api/public/get/courses?page=0&page-size=5&sorting=ASC

Response JSON with HTTP 200 response code:

```json
{
  "retrievedCourseCount": 5,
  "retrievedCourses": [
    {
      "id": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #1",
      "description": "Course Description #1",
      "thumbnailImageId": null,
      "courseItemIds": [
        "QZ_2bb1c9a4-33ba-4cc8-983c-a1a6d41402e1",
        "QZ_e873ef10-5e29-4c82-b70a-5572b8cfa8b4",
        "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
        "CT_26cc5297-4aa4-4a6c-b179-b8563df21766",
        "QZ_ac206c1a-19ea-4a09-974c-dbf674af82ba",
        "QZ_00505192-b460-46aa-a572-a6525996561f"
      ],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.355931",
      "lastModifiedAt": "2025-08-23T15:27:15.391495"
    },
    {
      "id": "a77c0490-650f-4412-90a1-c416486876ee",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #2",
      "description": "Course Description #2",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.399178",
      "lastModifiedAt": "2025-08-23T15:27:15.405334"
    },
    {
      "id": "8810d20f-6261-4dc6-941a-2c1b38e4c64c",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #3",
      "description": "Course Description #3",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.408870",
      "lastModifiedAt": "2025-08-23T15:27:15.414616"
    },
    {
      "id": "fddd945f-e136-4906-92fb-517e84218075",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #4",
      "description": "Course Description #4",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.417930",
      "lastModifiedAt": "2025-08-23T15:27:15.423536"
    },
    {
      "id": "659d9734-c0bc-46c4-9571-1d562fcfd9d1",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #5",
      "description": "Course Description #5",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.426569",
      "lastModifiedAt": "2025-08-23T15:27:15.432221"
    }
  ],
  "pageNumber": 0,
  "pageSize": 5,
  "totalPaginatedPages": 20
}
```

2) GET api/protected/get/my-courses?page=0&page-size=5&sorting=ASC

Returns the paginated list of -
a) courses published by the instructor in JSON (if an instructor account sends the request)
b) courses purchased by the learner in JSON (if a learner account sends the request)

Explanation of the URL parameters:
a) page -> Zero indexed page number for the course list (default: 0)
b) page-size -> The number of courses per page (default: 5)
c) sorting -> DESC <OR> ASC (default: ASC)

So, the following requests are the same:

GET api/protected/get/my-courses
GET api/protected/get/my-courses?page=0&page-size=5&sorting=ASC

Response JSON with HTTP 200 response:

```json
{
  "retrievedCourseCount": 5,
  "retrievedCourses": [
    {
      "id": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #1",
      "description": "Course Description #1",
      "thumbnailImageId": null,
      "courseItemIds": [
        "QZ_2bb1c9a4-33ba-4cc8-983c-a1a6d41402e1",
        "QZ_e873ef10-5e29-4c82-b70a-5572b8cfa8b4",
        "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
        "CT_26cc5297-4aa4-4a6c-b179-b8563df21766",
        "QZ_ac206c1a-19ea-4a09-974c-dbf674af82ba",
        "QZ_00505192-b460-46aa-a572-a6525996561f"
      ],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.355931",
      "lastModifiedAt": "2025-08-23T15:27:15.391495"
    },
    {
      "id": "a77c0490-650f-4412-90a1-c416486876ee",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #2",
      "description": "Course Description #2",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.399178",
      "lastModifiedAt": "2025-08-23T15:27:15.405334"
    },
    {
      "id": "8810d20f-6261-4dc6-941a-2c1b38e4c64c",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #3",
      "description": "Course Description #3",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.408870",
      "lastModifiedAt": "2025-08-23T15:27:15.414616"
    },
    {
      "id": "fddd945f-e136-4906-92fb-517e84218075",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #4",
      "description": "Course Description #4",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.417930",
      "lastModifiedAt": "2025-08-23T15:27:15.423536"
    },
    {
      "id": "659d9734-c0bc-46c4-9571-1d562fcfd9d1",
      "instructorId": "3cea4e37-7421-4c12-aca2-e4dd55e92608",
      "title": "Course Title #5",
      "description": "Course Description #5",
      "thumbnailImageId": null,
      "courseItemIds": [],
      "price": 4.2,
      "rating": 0.0,
      "createdAt": "2025-08-23T15:27:15.426569",
      "lastModifiedAt": "2025-08-23T15:27:15.432221"
    }
  ],
  "pageNumber": 0,
  "pageSize": 5,
  "totalPaginatedPages": 20
}
```

3) POST api/protected/add/course

Adds the requested course but doesn't publish it yet.

Request body's JSON:

```json
{
  "title": "Git Tutorial",
  "description": "A Git course that gently guides you through the world of version control",
  "price": "1.23"
}
```

Response JSON with HTTP 201 response:

```json
{
  "itemId": "04f2d96a-3b6e-4185-9cba-385d380ce4fc"
}
```

This is the ID of the course that just got added.

4) POST api/protected/modify/course

Modifies the requested course. You can omit the fields that you don't want to modify.
You can update the title, description and price fields.

Example request:

```json
{
  "courseId": "2a8f9641-9b78-4f0a-8bb1-52e549f5b730",
  "title": "A much more saner title"
}
```

Response JSON:

```json
{
  "itemId": "2a8f9641-9b78-4f0a-8bb1-52e549f5b730"
}
```

5) POST api/protected/remove/course

Removes/deletes the provided course given it hasn't been published yet.
Published courses can't be deleted.

Example request:

```json
{
  "courseId": "2a8f9641-9b78-4f0a-8bb1-52e549f5b730"
}
```

Response JSON:

```json
{
  "itemId": "2a8f9641-9b78-4f0a-8bb1-52e549f5b730"
}
```

6) POST api/protected/set/publish-course

Publishes the provided course. An instructor can only publish their unreleased course.

Request body's JSON:

```json
{
  "courseId": "04f2d96a-3b6e-4185-9cba-385d380ce4fc"
}
```

Response: HTTP 200.

7) POST api/protected/rate-course

Casts a vote that counts towards the rating of the course. A rating can be any integer between 1 and 5 (including).

Request body's JSON:

```json
{
  "courseId": "0c2023c3-0f6a-4eb5-a850-dc10c297e4fd",
  "rating": 2
}
```

Response: HTTP 200.

8) POST api/protected/set/course-item-order

Reorganize the ordering of the course items (videos, quizzes, coding tests, terminal tests).

Request body's JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "orderOfItemIds": [
    "QZ_2bb1c9a4-33ba-4cc8-983c-a1a6d41402e1",
    "QZ_e873ef10-5e29-4c82-b70a-5572b8cfa8b4",
    "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
    "CT_26cc5297-4aa4-4a6c-b179-b8563df21766",
    "QZ_ac206c1a-19ea-4a09-974c-dbf674af82ba",
    "QZ_00505192-b460-46aa-a572-a6525996561f"
  ]
}
```

Response: HTTP 200.

Response JSON:

```json
{
  "itemId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207"
}
```

In the above request, the 3 course items have been reordered in the sequence provided.

## Quiz Controller

1) POST api/protected/add/quiz

Adds a quiz to a given course.

Request JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Why are you?",
  "o1": "Aa",
  "o2": "Bb",
  "o3": "Cc",
  "o4": "Dd",
  "correctOption": 4
}
```

Response JSON:

```json
{
  "itemId": "QZ_ab466fdf-b90b-4d3e-b801-805c51039ece"
}
```

2) POST api/protected/modify/quiz

Modify a given quiz in a given course.
Only `courseId` and `quizId` is mandatory. The other fields are optional.

Request JSON:

```json
{
  "itemId": "QZ_ab466fdf-b90b-4d3e-b801-805c51039ece",
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Chicken or egg?",
  "o1": "aaa",
  "o2": "bbb",
  "o3": "ccc",
  "o4": "ddd",
  "correctOption": 3
}
```

Response JSON:

```json
{
  "itemId": "QZ_ab466fdf-b90b-4d3e-b801-805c51039ece"
}
```

3) POST api/protected/remove/quiz

Delete a given quiz in a given course.

Request JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "itemId": "QZ_ab466fdf-b90b-4d3e-b801-805c51039ece"
}
```

Response JSON:

```json
{
  "itemId": "QZ_ab466fdf-b90b-4d3e-b801-805c51039ece"
}
```

## Coding Test Controller

1) POST api/protected/add/coding-test

Add a coding test challenge.

Request JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Which came before: Chicken? Or the egg?",
  "description": "Lorem ipsum dolores....",
  "expectedStdout": "The big beautiful bill..."
}
```

Response JSON:

```json
{
  "itemId": "CT_26cc5297-4aa4-4a6c-b179-b8563df21766"
}
```

2) POST api/protected/modify/coding-test

Modify a coding test challenge.

Request JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "itemId": "CT_26cc5297-4aa4-4a6c-b179-b8563df21766",
  "question": "Nani?",
  "expectedStdout": "The big beautiful bill..."
}
```

Response JSON:

```json
{
  "itemId": "CT_26cc5297-4aa4-4a6c-b179-b8563df21766"
}
```

3) POST api/protected/remove/coding-test

Delete a coding test challenge.

Request JSON:

```json
{
  "courseId": "255e37f3-5473-4c51-99f4-1d0335780f3a",
  "itemId": "CT_8a2ce190-e5d2-4310-b5b8-fe2bf1c85bf8"
}
```

Response JSON:

```json
{
  "itemId": "CT_8a2ce190-e5d2-4310-b5b8-fe2bf1c85bf8"
}
```

## Course Item Controller

1) GET api/protected/get/course-item?itemId=<courseItemId>

Retrieve all the information related to a course item (video metadata, quiz, coding test and terminal test).

Example request:

```
GET api/protected/get/course-item?itemId=CT_90a22596-4ce0-41c8-94d0-bd3cb2487368
```

Response JSON:

```json
{
  "itemId": "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Which came before: Chicken? Or the egg?",
  "description": "Lorem ipsum dolores....",
  "expectedStdout": "The big beautiful bill...",
  "isCompleted": false
}
```

2) POST api/protected/add/completed/course-item

Mark a course item as completed.

Example request:

```json
{
  "courseId": "b4a362a9-5294-47f0-b90c-584e1fd54a0b",
  "itemId": "CT_b7a30066-7f0d-4733-a9e3-f716ecb9a795"
}
```

Response JSON:

```json
{
  "itemId": "CT_b7a30066-7f0d-4733-a9e3-f716ecb9a795"
}
```

## Comments Controller

1) GET api/protected/get/video/comments?video-id=<item-id>&page=<int>&page-size=<int>&sorting=<String>

This endpoint can be used to get the comments of a video.

Example request:

```
GET api/protected/get/video/comments?video-id=VM_b932066d-c18f-4f8f-804d-f97c9d44e609&page-size=2&page=0&sorting=DESC
```

JSON Response:

```json
{
  "retrievedCommentCount": 2,
  "retrievedComments": [
    {
      "commentId": "5c0a91a4-bb09-4fd0-b988-5b497ff4782d",
      "authorName": "Amanda",
      "authorProfilePicture": "default-user-logo.png",
      "content": "Just a simple test comment to see if it works...",
      "createdAt": "2025-09-11T14:35:51.677726",
      "lastModifiedAt": "2025-09-11T14:35:51.677726"
    },
    {
      "commentId": "267330c4-734a-4983-9c0c-a405f0beb703",
      "authorName": "Amanda",
      "authorProfilePicture": "default-user-logo.png",
      "content": "Just a simple test comment to see if it works...",
      "createdAt": "2025-09-11T13:52:44.448964",
      "lastModifiedAt": "2025-09-11T13:52:44.448964"
    }
  ],
  "pageNumber": 0,
  "pageSize": 2
}
```

2) GET api/protected/get/my/comments?page=<int>&page-size=<int>&sorting=<String>

This endpoint can be used to get the comments of a user.

Example request:

```
GET api/protected/get/my/comments?page-size=2&page=0&sorting=DESC
```

JSON Response:

```json
{
  "retrievedCommentCount": 2,
  "retrievedComments": [
    {
      "commentId": "5c0a91a4-bb09-4fd0-b988-5b497ff4782d",
      "authorName": "Amanda",
      "authorProfilePicture": "default-user-logo.png",
      "content": "Just a simple test comment to see if it works...",
      "createdAt": "2025-09-11T14:35:51.677726",
      "lastModifiedAt": "2025-09-11T14:35:51.677726"
    },
    {
      "commentId": "267330c4-734a-4983-9c0c-a405f0beb703",
      "authorName": "Amanda",
      "authorProfilePicture": "default-user-logo.png",
      "content": "Just a simple test comment to see if it works...",
      "createdAt": "2025-09-11T13:52:44.448964",
      "lastModifiedAt": "2025-09-11T13:52:44.448964"
    }
  ],
  "pageNumber": 0,
  "pageSize": 2
}
```

3) POST api/protected/set/comment

This endpoint can be used to add comments for a specific video.

Example request:

```json
{
  "videoMetadataId": "VM_b932066d-c18f-4f8f-804d-f97c9d44e609",
  "content": "Just a simple test comment to see if it works..."
}
```

Response JSON:

```json
{
  "itemId": "5c0a91a4-bb09-4fd0-b988-5b497ff4782d"
}
```

4) POST api/protected/modify/comment

This endpoint can be used to modify the contents of a previously added comment.

Example request:

```json
{
  "commentId": "48bd7040-c2fe-40b2-965c-d1ffbcfdf563",
  "content": "FYI, it works!"
}
```

Response JSON:

```json
{
  "itemId": "48bd7040-c2fe-40b2-965c-d1ffbcfdf563"
}
```

5) POST api/protected/remove/comment

This endpoint can be used to remove/delete a previously added comment.

Example request:

```json
{
  "commentId": "48bd7040-c2fe-40b2-965c-d1ffbcfdf563"
}
```

Response JSON:

```json
{
  "itemId": "48bd7040-c2fe-40b2-965c-d1ffbcfdf563"
}
```

## Purchase Controller

1) POST api/protected/purchase/course

Get the purchase link for the course. The frontend will redirect to the provided link, on which the user will confirm
the purchase or deny it.
Upon successful purchase, Stripe will redirect the user to the pre-configured link. The backend will add the course to
the learner's account
if the purchase was successful indicated by Stripe's web hook events.

Request JSON:

```json
{
  "courseId": "fbb34f58-ac82-4b60-8ecd-dd272e62aa63"
}
```

Response JSON:

```json
{
  "status": "OK",
  "msg": "Payment created.",
  "sessionId": "cs_test_a1ZjiYI9JVt2Z6jnxzXkiENIn9BrpzSOFWf7aFCtjt2gnxlaDmJ4gC3Oo7",
  "sessionUrl": "https://checkout.stripe.com/c/pay/cs_test_a1ZjiYI9JVt2Z6jnxzXkiENIn9BrpzSOFWf7aFCtjt2gnxlaDmJ4gC3Oo7#fidkdWxOYHwnPyd1blpxYHZxWjA0V2xcU11NY2E9Y19ET1Vmc1R8cDZAQE5rRFJJf39BcEhKXUQyVTV9Z3BucG9DNmdiXGdGRlAwSkZzS39hQDZMdldjYldgQXBmMDJjTWF%2FMlFmSjxjU1BNNTVPfDV1a3BjVCcpJ2N3amhWYHdzYHcnP3F3cGApJ2lkfGpwcVF8dWAnPyd2bGtiaWBabHFgaCcpJ2BrZGdpYFVpZGZgbWppYWB3dic%2FcXdwYHgl"
}
```

2) POST api/protected/purchase/storage

Get the purchase link for the requested storage amount. The valid storage amounts are integers between 1 to 20. Each
integer
represents a gigabyte of storage. After getting the purchase link, the frontend will redirect to the provided link, on
which
the user will confirm the purchase or deny it. Upon successful purchase, Stripe will redirect the user to the
pre-configured link.
The backend will add the storage purchased to the instructor's account if the purchase was successful indicated by
Stripe's web hook events.

Request JSON:

```json
{
  "amountInGBs": 5
}
```

Response JSON:

```json
{
  "status": "OK",
  "msg": "Payment created.",
  "sessionId": "cs_test_a1NSq9HDa9bGyCmqUUK5CXCpOS4uTTYNVGbFSmd3TjxmOuX89dDftGg4h8",
  "sessionUrl": "https://checkout.stripe.com/c/pay/cs_test_a1NSq9HDa9bGyCmqUUK5CXCpOS4uTTYNVGbFSmd3TjxmOuX89dDftGg4h8#fidkdWxOYHwnPyd1blpxYHZxWjA0V2xcU11NY2E9Y19ET1Vmc1R8cDZAQE5rRFJJf39BcEhKXUQyVTV9Z3BucG9DNmdiXGdGRlAwSkZzS39hQDZMdldjYldgQXBmMDJjTWF%2FMlFmSjxjU1BNNTVPfDV1a3BjVCcpJ2N3amhWYHdzYHcnP3F3cGApJ2lkfGpwcVF8dWAnPyd2bGtiaWBabHFgaCcpJ2BrZGdpYFVpZGZgbWppYWB3dic%2FcXdwYHgl"
}
```

## Search Controller

1) GET api/public/search/courses?query=\<string\>&page-size=\<int\>&page-number=\<int\>

This endpoint can be used to search for courses. The search is done over the course title and description.
(This endpoints also searches over tags. But, tags haven't been added yet.)

Also, the `page-number` uses 1-based indexing.

Example request: GET api/public/search/courses?query=DevOps&page-size=1&page-number=2

Response JSON:

```json
{
  "totalHits": 2,
  "hitsPerPage": 1,
  "page": 2,
  "totalPages": 2,
  "hits": [
    {
      "id": "3",
      "title": "Kubernetes - Beginner to Advanced",
      "tags": [
        "K8s",
        "DevOps",
        "SRE"
      ]
    }
  ],
  "facetDistribution": null,
  "facetStats": null,
  "processingTimeMs": 0,
  "query": "DevOps"
}
```

## Analytics Controller

### 1) GET api/protected/get/learner-analytics

Used to receive learner account analytics.

The `loginStreak` tells the number of consecutive days the user visited the site logged in.

The `activityThisMonth` array tells indicates the days user logged in.
Three values are used (0, 1, -1) in the array and their explanation is given below:

```
0 -> Didn't log in that day (or it could also mean that that day is in the future)
1 -> User logged in that day
-1 -> This is usually the last day in February in non-leap years. This indicates that this day is invalid.
```

Example request: `GET api/protected/get/learner-analytics`

Response JSON:

```json
{
  "loginStreak": 1,
  "activityThisMonth": [
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    -1
  ]
}
```

### 2) GET api/protected/get/user/login-activity?year=\<int\>&month=\<int\>

Used to receive learner account login activity for the specific month + year combo. You can omit the month and
only send the year to get the login activity of that year.

Example request:

`GET api/protected/get/user/login-activity?year=2025&month=12`

Response JSON:

```json
{
  "activityInMonth": [
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0
  ]
}
```

### 2) GET api/protected/get/instructor-analytics

Used to receive learner account analytics.

The storage related fields (`totalAvailableStorage` and `usedStorage`) are given in bytes.

The `loginStreak` tells the number of consecutive days the user visited the site logged in.

The `activityThisMonth` array tells indicates the days user logged in.
Three values are used (0, 1, -1) in the array and their explanation is given below:

```
0 -> Didn't log in that day (or it could also mean that that day is in the future)
1 -> User logged in that day
-1 -> This is usually the last day in February in non-leap years. This indicates that this day is invalid.
```

Example request: `GET api/protected/get/instructor-analytics`

Response JSON:

```json
{
  "loginStreak": 1,
  "loginActivity": [
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0,
    0
  ],
  "accountBalance": 21.0,
  "totalCoursesSold": 5,
  "totalAvailableStorage": 1073741824,
  "usedStorage": 109853229
}
```

## Transcription Controller

### 1) GET api/protected/get/transcription/{video-metadata-id}

Used to retrieve a video's transcription. The `Content-Type` of the file sent will be `text/vtt`.

Example request: `GET api/protected/get/transcription/VM_1c52b153-b875-417b-9ba9-2ae3d61fac5d`

Response VTT:

```vtt
WEBVTT

NOTE
This is from a talk Silvia gave about WebVTT.

Slide 1
00:00:00.000 --> 00:00:10.700
Title Slide

Slide 2
00:00:10.700 --> 00:00:47.600
Introduction by Naomi Black

Slide 3
00:00:47.600 --> 00:01:50.100
Impact of Captions on the Web

Slide 4
00:01:50.100 --> 00:03:33.000
Requirements of a Video text format

```

### 2) POST api/protected/modify/transcription/{video-metadata-id}

Used to update/modify a video's VTT transcription.

Example request: `POST api/protected/modify/transcription/VM_1c52b153-b875-417b-9ba9-2ae3d61fac5d`

The request body will have to be in `form-data` and there will have to be a VTT file with the name `modified-vtt`
attached to the request.

HTTP response code: `HTTP 201`
