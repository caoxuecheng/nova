nifi-core-bundle
==========

### Overview

The NiFi core bundle is a NiFi extension providing standard ingest components and integrations with the metadata server

### Component Descriptions


#### Ingest Support

| Component        | Description           |
| ------------- |-------------|
| MergeTable | Merges data from a source table and partition into the target table (inserting and adding)
| GetTableData | Polls a database table with the ability to do snapshots or incremental fetch with high-water recording
| RegisterNflowTables | Creates standard tables in support of ingest workflow. This is referred to as "table registration"
| RouteOnRegistration | Routes through an alternate flow path based on the status of table registration
| UpdateRegistration | Updates state of a nflow in metadata repository to note that table registration has been performed

#### Ingest Support

| Component        | Description           |
| ------------- |-------------|
| BeginNflow | Denotes the start of a nflow with capability to receive JMS messages to trigger the flow. 
| TerminateDirectoryNflow | Denotes the termination of a nflow that resulted in data written to a directory. Evicts a JMS event based on the data change.
| TerminateHiveTableNflow | Denotes the termination of a nflow that resulted in data written to a Hive take. Evicts a JMS event based on the data change.

### Dependencies

Use of this bundle requires the nifi-hadoop-services-nar
