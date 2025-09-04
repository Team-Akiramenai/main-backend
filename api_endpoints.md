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

4) POST api/protected/change-profile-picture

Changes the profile picture with the uploaded picture.

The picture needs to be uploaded as a form-data with the name `new-profile-picture`. We accept PNG for now. Make sure to
set the content type
to `image/png`.

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

0) GET api/public/get/course

Return the information about the specified course.

Request JSON:

```json
{
  "itemId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207"
}
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

1) GET api/public/get/courses?page=0&page-size=1&sorting=DESC

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
  "pageSize": 5
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
  "pageSize": 5
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

4) POST api/protected/set/publish-course

Publishes the provided course. An instructor can only publish their unreleased course.

Request body's JSON:

```json
{
  "courseId": "04f2d96a-3b6e-4185-9cba-385d380ce4fc"
}
```

Response: HTTP 200.

5) POST api/protected/rate-course

Casts a vote that counts towards the rating of the course. A rating can be any integer between 1 and 5 (including).

Request body's JSON:

```json
{
  "courseId": "0c2023c3-0f6a-4eb5-a850-dc10c297e4fd",
  "rating": 2
}
```

Response: HTTP 200.

6) POST api/protected/set/course-item-order

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

Requsest JSON:

```json
{
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Which came before: Chicken? Or the egg?",
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

1) GET api/protected/get/course-item

Retrieve all the information related to a course item (video metadata, quiz, coding test and terminal test).

Requsest JSON:

```json
{
  "itemId": "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368"
}
```

Response JSON:

```json
{
  "itemId": "CT_90a22596-4ce0-41c8-94d0-bd3cb2487368",
  "courseId": "cbdc0b4a-6f0b-4964-9964-24a7f4775207",
  "question": "Which came before: Chicken? Or the egg?",
  "expectedStdout": "The big beautiful bill..."
}
```

## Purchase Controller

1) POST api/protected/purchase/course

Get the purchase link for the course. The frontend will redirect to the provided link, on which the user will confirm
the purchase or deny it.
Upon successful purchase, Stripe will redirect the user to the pre-configured link. The backend will add the course to
the learner's account
if the purchase was successful indicated by Stripe's web hook events.

Requsest JSON:

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

Requsest JSON:

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
