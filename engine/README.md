## Quickstart

The quickest way to start up the Web application is to use the official Docker image like this:
```
docker run --rm -p 8080:8080 julielab/smithsearch:1.0.0
```

Then, the web service will be available at `http://localhost:8080/search`. An ElasticSearch instance will be expected at `http://localhost:9200`.

## Web service Usage

The Web service offers a REST interface to the `/search` endpoint. Search requests are sent there using the `POST` HTTP method. The request body must be a JSON object in the following format:

```json
{
  "query": "...",
  "from": 0,
  "size": 10,
  "doHighlighting": true,
  "doFaceting": true
}
```

Where
* `query` is an ElasticSearch [Simple Query String Query](https://www.elastic.co/guide/en/elasticsearch/reference/7.17/query-dsl-simple-query-string-query.html) with flags set to `ALL`. This query allows boolean expression using `+` as AND, `|` as OR and `-` as negation. Refer to the documentation to find all query possibilities.
* `from` is a number that specifies the result offset from which the result documents should be returned. This can be used for result paging.
* `size` is a number that specifies the number of results to return beginning from `from`.
* `doHighlighting` is a boolean value, `true` or `false`. It toggles the creation of snippets that use HTML tags to mark query matches in the document text.
* `doFaceting` is a boolean value, `true` or `false`. It toggles the calculation of the top 10 entity IDs for the query result.

The following sections describe how to use the Web service in different scenarios.

### As a development version with Maven

Use Maven to quickly run the application without the need to build JAR files:

`./mvnw spring-boot:run`

### As a Java application

Compile the application into an executable JAR with the Maven command `mvn clean package`. The application can be run with a command like
```
java -jar target/smithsearch-1.0.0.jar
```

### As a Docker container

A Docker container with the search application has been published to Docker Hub named `julielab/smithsearch:1.0.0`. Alternatively, this GitHub repository contains a Dockerfile that can be used to create a local Docker image. The next sections show how to use a Docker container as a Web service. A running Docker installation is required.

All commands specified in this README specify the `--rm` option that will remove the container after it is stopped. Since the application does not have an internal state, it is not necessary to keep the container. The `-p 8080:8080` option maps the container-internal port 8080 to the host port 8080. The second number can be changed to use another host port.

#### Run the official Docker container from Docker Hub

On the command line, type
```
docker run --rm -p 8080:8080  julielab/smithsearch:1.0.0
```

This will download the official image, create and run a container. The web application is then available under port 8080 with path `/search`.

#### With a Docker image built from the repository code

The `Dockerfile` in the repository allows to create a new, local Docker image from scratch. Run
```
docker build . -t mypsearchwebapp:1.0.0
```
to create a new image named `mypsearchwebapp` with version `1.0.0.`. Create and run a container using
```
docker run --rm -p 8080:8080 mypsearchwebapp:1.0.0
```
just as with the official image.


### Testing the web service
On *nix-based systems, use cURL to send a test document:
```
curl -XPOST http://localhost:8080/search -H 'Content-Type: application/json' -d '{"query":"R05","from":0,"size":5,"doHighlighting":true,"doFaceting":true}'
```
the response will return matched documents to the query term `R05` (the ICD10 code for "Husten") if any are found in the index. Otherwise, the response will indicate that no documents were found.

On Windows, use the PowerShell like this:
```
PS> $inputText=ConvertTo-Json @(@{query="R05";from=0;size=5;doHighlighting=true;doFaceting:true})
PS> Invoke-RestMethod -Method POST -ContentType "application/json" -uri http://localhost:8080/search -Body $inputText
```

## Web Application configuration

The Web application needs to know the URL of the ElasticSearch instance to connect to. The file `src/main/resources/application.properties` lists the properties

* `spring.elasticsearch.uris`
* `spring.elasticsearch.socket-timeout`
* `spring.elasticsearch.username`
* `spring.elasticsearch.password`

where username and password are required when ElasticSearch security is enabled. The ElasticSearch Docker setup provided in this repository does not enable security. The minimal required property is `spring.elasticsearch.uris` and must be set to at least one ElasticSearch instance URL. When running the application in a Docker container, the host `host.docker.internal` can be used to connect to an ElasticSearch running on the host, including in another Docker container. Note, however, that for all-Docker setups, a Docker network should be created for more direct communication.

To use an external configuration file, use `java -jar smithsearch.jar --spring.config.location=classpath:/another-location.properties`. If you use the Docker container, edit the Dockerfile `CMD` line accordingly and mount the configuration file into the container, e.g. by using the `-v` switch.
