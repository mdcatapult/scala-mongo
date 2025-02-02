# Scala Mongo

MongoDB Abstraction for ease of connection and access.

## Usage
When connecting to a replica set you can either:
1. specify a comma delimited list of hosts, or
2. use an srv record, by setting `MONGO_SRV=true`. If you supplied more than one host in this case, only the first host is used in DNS resolution.

Any codecs must be declared implicitly.

The config from which connection details are pulled must be declared implicitly.

## Config
The default is to read from the secondary with default replication lag of 90 seconds in `mongo.connection.maxStaleness`.
You can change `mongo.connnection.readPreference` to `primaryPreferred` if you want which then ignores the staleness setting.  
`maxStaleness` cannot be less than [90](https://docs.mongodb.com/manual/reference/connection-string/#mongodb-urioption-urioption.maxStalenessSeconds).

## Testing
```bash
docker-compose up -d
sbt clean it:test
```

### License
This project is licensed under the terms of the Apache 2 license, which can be found in the repository as `LICENSE.txt`
