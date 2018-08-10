# Introduction
Simple [gitblit](https://github.com/gitblit/gitblit) plugin that create a list of specific branches for every new repository.

# Build
Run maven for build this plugin:
```
mvn clean package
```

# Usage
Move plugin into gitblit plugins folder:

mv target/autobranch-XX.zip gitblit/data/plugins/

Edit gitblit/data/xxx.properties and add list of branches for create:

```
autobranches.default=develop,master,testing option.
```
