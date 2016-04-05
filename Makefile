SHELL=/bin/bash

help: ## This help dialog.
	@IFS=$$'\n' ; \
	help_lines=(`fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##/:/'`); \
	printf "%-30s %s\n" "target" "help" ; \
	printf "%-30s %s\n" "------" "----" ; \
	for help_line in $${help_lines[@]}; do \
		IFS=$$':' ; \
		help_split=($$help_line) ; \
		help_command=`echo $${help_split[0]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
		help_info=`echo $${help_split[2]} | sed -e 's/^ *//' -e 's/ *$$//'` ; \
		printf '\033[36m'; \
		printf "%-30s %s" $$help_command ; \
		printf '\033[0m'; \
		printf "%s\n" $$help_info; \
	done

all: help

run: ## start server with mode DEV on port 9002
	sbt ~run -Dhttp.port=9000 # -Dconfig.file=/FULL PATH TO YOUR LOCAL DEV CONF FILE/dev.conf

console: ## start console
	sbt console

package: ## create Debian package
	sbt debian:packageBin

secret: ## Get a new secret key (not saved)
	sbt playGenerateSecret

check: ## check Scala style
	sbt scalastyle

scalastyleconfig: ## Generate configuration for scalastyle
	sbt scalastyleGenerateConfig
