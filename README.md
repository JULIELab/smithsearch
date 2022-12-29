# Search Engine for Medical German

This search engine was developed in the context of the [SMITH](https://www.smith.care/de/) project. It aims to specifically offer search capabilities for medical text in German. Its main feature is the seamless integration of semantic concepts, called named entities, into the search index. This allows to search for canonical IDs of diseases, medications or possibly other entity types. Thus, instead of providing the search engine with synonyms and writing variants of the same concept in order to retrieve as much relevant documents as possible, these steps are handled in the preprocessing step and woven into the index. The engine offers faceting and highlighting capabilities that work with normal text queries as well as entity IDs. Entity IDs and normal words can be used in arbitrary combinations since the entity IDs are just words from the perspective of the search index.

The code for the search engine consists of two parts, the indexing pipeline and this Web application. The indexing pipeline, that is used to read documents, detect entities and create index documents, is found in this Git repository in the `smithsearch-indexing-pipeline` directory.

This application is built with [Spring Boot](https://spring.io/projects/spring-boot) and relies on [ElasticSearch](https://www.elastic.co/) for its search capabilities. The indexing pipeline is built with [UIMA](https://uima.apache.org/) using [JCoRe](https://github.com/JULIELab/jcore-base) components.

An ElasticSearch instance can be quickly provided using Docker. See the directory `../es-docker` for instructions.

## Quickstart

The quickest way to start up the pipeline application is to use the official Docker image like this:
```
docker run --rm -p 8080:8080 -v julielab/smithsearch:1.0.0
```

Then, the web service will be available at `http://localhost:8080/search`.

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
docker run --rm -p 8080:8080 mypsearchwebapp:1.0.0-SNAPSHOT
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