### Build

Run maven for build this plugin:
```
mvn clean package
```

### Usage
Move plugin into gitblit plugins folder:

```
mv target/autobranch-XX.zip gitblit/data/plugins/
```

Edit gitblit/data/xxx.properties and add **autobranches.default=develop,master,testing** option.
