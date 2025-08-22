# How to run

First, copy the file located at `./src/main/resources/sample_application.yaml` to `./src/main/resources/application.yaml`.

Then, setup the Postgres database with the name `jwt_security` which should be accessible with the username `username` and the password `password`.
It's recommended to use the Postgres docker image for this. (The DB name, username, password can be changed in the `./src/main/resources/application.yaml` file)

Then, acquire/generate the things marked `<<REPLACE-ME>>` with the appropriate values.

(For the Stripe secret keys, you'll have to create a free Stripe account. No credit card required.)
(For the secret key we use to sign our JWT tokens, you can use)

Then, for forwarding the webhook events in the local development environment, you'll have to run `stripe login` to log into Stripe and then keep the following command running:
`stripe listen --forward-to http://localhost:8080/webhook --skip-verify`.

After that, run the project using `gradle bootRun`. This will start the server and start listening for requests at `localhost:8080`.

NOTE: The video upload related API endpoints are in a separate microservice. So, you'll need to run that microservice for those endpoints if you want to use them.

# API endpoints 

The available API endpoints are listed in the [api_endpoints.md](./api_endpoints.md) file.
