# postgres-complex-types-jdbc-dialect

This adds support for `ARRAY` and `STRUCT` types in Postgres Sinks. 
They are converted to json and added as `jsonb` fields.

# To use. 
  * Download the [jar file](https://github.com/Mercell/complex-types-postgres-jdbc-dialect/releases) and place it on the classpath of your PostgresSink.
  * When configuring your sink add the following to your config
      * `dialect.name=ComplexTypesPostgresDatabaseDialect`
    
      