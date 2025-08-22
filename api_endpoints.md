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

Logs out the user by removing the JWT refresh token storing HTTP-only cookie. The frontend needs to drop the JWT auth token stored
in memory too.

4) POST api/protected/change-profile-picture

Changes the profile picture with the uploaded picture.

The picture needs to be uploaded as a form-data with the name `new-profile-picture`. We accept PNG for now. Make sure to set the content type
to `image/png`.

5) api/protected/update-password

Updates the password to the newly provided password if the old password matches.

Request body's JSON:
```json
{
    "oldPassword": "12345678",
    "newPassword": "hell0kitty"
}
```
Response: HTTP 200 with confirmation text

6) api/public/update-username

Updates the user to the newly provided username.

Request body's JSON:
```json
{
    "newUsername": "sayonala"
}
```
Response: HTTP 200 with confirmation text

## Course Controller

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
            "id": "8080944b-2699-40fb-acbb-6ca9c56f695c",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #1",
            "description": "Course Description #1",
            "thumbnailImageId": null,
            "courseItemIds": [
                "8080944b-2699-40fb-acbb-6ca9c56f695c"
            ],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.454424",
            "lastModifiedAt": "2025-08-01T20:40:04.493686"
        },
        {
            "id": "4f3a4d25-1681-475b-9f5c-7aad98d6dc6b",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #2",
            "description": "Course Description #2",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.501373",
            "lastModifiedAt": "2025-08-01T20:40:04.507690"
        },
        {
            "id": "eb6ac21c-4810-4841-a3ac-de865fcae025",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #3",
            "description": "Course Description #3",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.511802",
            "lastModifiedAt": "2025-08-01T20:40:04.518002"
        },
        {
            "id": "8e940514-dcb5-4ca9-a88c-a76e31e710bf",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #4",
            "description": "Course Description #4",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.521873",
            "lastModifiedAt": "2025-08-01T20:40:04.527570"
        },
        {
            "id": "0c2023c3-0f6a-4eb5-a850-dc10c297e4fd",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #5",
            "description": "Course Description #5",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.531566",
            "lastModifiedAt": "2025-08-01T20:40:04.537549"
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
            "id": "8080944b-2699-40fb-acbb-6ca9c56f695c",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #1",
            "description": "Course Description #1",
            "thumbnailImageId": null,
            "courseItemIds": [
                "8080944b-2699-40fb-acbb-6ca9c56f695c"
            ],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.454424",
            "lastModifiedAt": "2025-08-01T20:40:04.493686"
        },
        {
            "id": "4f3a4d25-1681-475b-9f5c-7aad98d6dc6b",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #2",
            "description": "Course Description #2",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.501373",
            "lastModifiedAt": "2025-08-01T20:40:04.507690"
        },
        {
            "id": "eb6ac21c-4810-4841-a3ac-de865fcae025",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #3",
            "description": "Course Description #3",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.511802",
            "lastModifiedAt": "2025-08-01T20:40:04.518002"
        },
        {
            "id": "8e940514-dcb5-4ca9-a88c-a76e31e710bf",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #4",
            "description": "Course Description #4",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.521873",
            "lastModifiedAt": "2025-08-01T20:40:04.527570"
        },
        {
            "id": "0c2023c3-0f6a-4eb5-a850-dc10c297e4fd",
            "instructorId": "6f15dfbe-279b-480b-bb13-f88f4ea81899",
            "title": "Course Title #5",
            "description": "Course Description #5",
            "thumbnailImageId": null,
            "courseItemIds": [],
            "price": 4.2,
            "rating": 0.0,
            "createdAt": "2025-08-01T20:40:04.531566",
            "lastModifiedAt": "2025-08-01T20:40:04.537549"
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
    "courseId": "8080944b-2699-40fb-acbb-6ca9c56f695c",
    "orderOfItemIds": [
        "bf142c50-3c25-478c-9b6e-f494f6de64ea",
        "6942e466-5170-4799-b9c6-25c51ddcae86",
        "a37a46b6-433e-49fa-90f0-3a781a46aa1f"
    ]
}
```

Response: HTTP 200.

In the above request, the 3 course items have been reordered in the sequence provided.

