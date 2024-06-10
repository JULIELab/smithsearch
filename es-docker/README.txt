This creates a docker image for a development ElasticSearch
instance that has the preanalyzed plugin installed:
docker build . -t elasticsearch-preanalyzed-smith:<tag>

Create and run a container based on the image:
docker run -d --name elasticsearch-preanalyzed-smith -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch-preanalyzed-smith:<tag>

And, finally, create an index:
curl -XPUT http://localhost:9200/smith -H 'Content-Type: application/json' -d @../engine/src/main/resources/esMappingFile.json

