# Search Engine for Medical German

This search engine was developed in the context of the [SMITH](https://www.smith.care/de/) project. It aims to specifically offer search capabilities for medical text in German. Its main feature is the seamless integration of semantic concepts, called named entities, into the search index. This allows to search for canonical IDs of diseases, medications or possibly other entity types. Thus, instead of providing the search engine with synonyms and writing variants of the same concept in order to retrieve as much relevant documents as possible, these steps are handled in the preprocessing step and woven into the index. The engine offers faceting and highlighting capabilities that work with normal text queries as well as entity IDs. Entity IDs and normal words can be used in arbitrary combinations since the entity IDs are just words from the perspective of the search index.

Consider this example request:
```
curl -XPOST http://localhost:8080/search -H 'Content-Type: application/json' -d '{"query":"R05","from":0,"size":1,"doHighlighting":true}'
```

Given that matching documents exist in the search index, the response looks like this:

```json
{
  "hits":
  [
    {
      "docId": "1234",
      "highlights":
      [
        "Eine 76-jährige Patientin meldet sich in der Sprechstunde an, weil sie seit einiger Zeit an <em>Husten</em> leidet",
        "einem Atemwegsinfekt mit Schnupfen, Gliederschmerzen, Abgeschlagenheit, leichtem Fieber und leichtem <em>Husten</em>"
      ],
      "text": "Eine 76-jährige Patientin meldet sich in der Sprechstunde an, weil sie seit einiger Zeit an Husten leidet. [...]"
    }
  ],
  "numHits": 61,
  "numHitsRelation": "Eq",
  "entityIdCounts":
  [
    {
      "count": 61,
      "entityId": "R05"
    },
    {
      "count": 14,
      "entityId": "R06.0"
    },
    {
      "count": 7,
      "entityId": "Z01.7"
    },
    {
      "count": 5,
      "entityId": "R07.0"
    },
    {
      "count": 4,
      "entityId": "E66.-"
    },
    {
      "count": 4,
      "entityId": "I50.-"
    },
    {
      "count": 4,
      "entityId": "R29.1"
    },
    {
      "count": 3,
      "entityId": "B05.-"
    },
    {
      "count": 3,
      "entityId": "B26.-"
    },
    {
      "count": 3,
      "entityId": "G93.6"
    }
  ]
}
```

Note how `Husten` is highlighted upon a search for `R05`. Also note the `entityId` counts where `R05` has the highest count, because it was the search query.
The code for the search engine consists of two parts, the indexing pipeline and a Web application. The Web application code is located at the `engine`directory. The indexing pipeline, that is used to read documents, detect entities and create index documents, is found in this Git repository in the `smithsearch-indexing-pipeline` directory.

This application is built with [Spring Boot](https://spring.io/projects/spring-boot) and relies on [ElasticSearch](https://www.elastic.co/) for its search capabilities. The indexing pipeline is built with [UIMA](https://uima.apache.org/) using [JCoRe](https://github.com/JULIELab/jcore-base) components.

An ElasticSearch instance can be quickly provided using Docker. See the directory `../es-docker` for instructions.

