# SMITH Indexing Pipeline

This pipeline is supposed to index medical German text into an ElasticSearch index. The file `../smithsearch/src/main/resources/esMappingFile.json` should be used to create the index.

Within this pipeline, a Gazetteer component matched strings given in a dictionary in the input documents and assigns an ID given in the dictionary. The dictionary could be derived from ICD-10 codes, for example. The dictionary must be provided by the user.

The ElasticSearch Consumer sends the data to the index, including the entity IDs to enable semantic search capabilities.

The pipeline is built with the [JCoRe Pipeline Modules](https://github.com/JULIELab/jcore-pipeline-modules) and can be edited and run using them.

An ElasticSearch instance can be quickly provided using Docker. See the directory `../es-docker` for instructions.

To successfully run this pipeline, a dictionary must be provided to the Gazetteer component in the pipeline. Use the `JCoRe Pipeline Modules` the appropriate configuration. The dictionary must be a file with two tab-separated columns. The first column must be entity text strings, the second column the corresponding entity ID that should be added to the ElasticSearch index. Additionally, the ElasticSearch instance must be online and the `ElasticSearch Consumer` in the pipeline must be given the correct URL, again through the `JCoRe Pipeline Modules`.

The pipeline configuration is basically just [UIMA](uima.apache.org/). That means that people familiar with UIMA may also edit the descriptors in the `descAll` and `desc` directories directly. The `descAll` directory holds all descriptors available to the pipeline while the `desc` directory is filled by first deleting it upon saving the `JCoRe Pipeline` and then copying all the descriptors of the enabled components from `descAll`. This mechanism allows the `JCoRe Pipeline Modules` to disable or enable pipeline components. The important thing is that the descriptors in `desc` are overridden upon saving the pipeline when using the `JCoRe Pipeline Modules`. Editing the descriptors in `descAll` will thus have only effect after saving the pipeline, editing the descriptors in `desc` will be overridden when using the `JCoRe Pipeline Modules` for editing the pipeline. 