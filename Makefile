IMAGE_NAME = yii2-support-plugin
CONTAINER_NAME = yii2-support-plugin-tmp
DIST_DIR = build

.PHONY: build docker-build extract clean

build: clean docker-build extract

docker-build:
	docker build -t $(IMAGE_NAME) .

extract:
	mkdir -p $(DIST_DIR)
	docker create --name $(CONTAINER_NAME) $(IMAGE_NAME)
	docker cp $(CONTAINER_NAME):/app/build/distributions/. $(DIST_DIR)
	docker rm $(CONTAINER_NAME)

clean:
	rm -rf $(DIST_DIR)/*
