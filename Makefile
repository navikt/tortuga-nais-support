DOCKER  := docker
NAIS    := nais
VERSION := $(shell cat ./VERSION)
REGISTRY:= repo.adeo.no:5443

.PHONY: all build test bump-version release maven-deploy

all: build
release: tag maven-deploy

build:
	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven clean package -DskipTests=true -B -V

test:
	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven verify -B -e

bump-version:
	@echo $$(($$(cat ./VERSION) + 1)) > ./VERSION

	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven versions:set -DnewVersion=$$(cat ./VERSION) -DgenerateBackupPoms=false -B -e

maven-deploy:
	$(DOCKER) run --rm -t \
		-v ${PWD}:/usr/src \
		-w /usr/src \
		-u $(shell id -u) \
		-v ${HOME}/.m2:/var/maven/.m2 \
		-e MAVEN_CONFIG=/var/maven/.m2 \
		maven:3.5-jdk-11 mvn -Duser.home=/var/maven deploy -B -e

tag:
	git add VERSION pom.xml
	git commit -m "Bump version to $(VERSION) [skip ci]"
	git tag -a $(VERSION) -m "auto-tag from Makefile"
