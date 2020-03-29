# Neural Network Database API [DEPRECATED]

This repository implements a GraphQL endpoint to query the Neural Network Database. The database itself is an Apache TinkerPop compatible graph database.

## Endpoint

The GraphQL endpoint is exposed on the `/graphql` path and supports `GET` requests. The query should be provided in the `query` URL parameter and optional query variables can be provided in the `variables` URL parameter as a JSON-encoded string. The GraphQL schema is in the [resources/schema.edn](resources/schema.edn) file.

The production service endpoint is: https://nndb-api.aughie.org/graphql. There is a 10 requests/second rate limiting enforced on a per IP basis. If this limit is exceeded, requests will return an `HTTP 429 Too Many Requests` error until a minute after the request rate returns to the allowed level.

It is recommended to access the service with the [Aughie Python Client Library](https://github.com/aughie/aughie-py).

## Run locally

The service is implemented in Clojure 1.9. To run it locally, install:

- JDK 1.8 or later
- [Leiningen 2.8](https://leiningen.org/) or later

To run the server at `localhost:8000` execute the following command from the root of the repository:

`lein run -m nndb.system`

This will use the testing dataset committed into the repository.
