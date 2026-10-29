Some rules / tips on writing Proto3 protocol buffer messages.buffer


## SQLite vs Proto3

SQLite has "columns" which can only be primitive types (not complex structures).
Protobufs are stored as Base64 strings in SQLite and are thus opaque.
In an Android Room Database, a change a schema requires a database update wrapper.

What should be a column vs a protobuf:
- It is easier to upgrade a protobuf (just change it) than an SQL table
  (since changing the schema requires a database update wrapper).
- In a field needs to be in a SELECT statement, it must be an SQL column.
- Anything that does NOT need to be in a SELECT or index is better expressed in a protobuf.

Each protobuf requires a Room Converter to encode/decode the proto to a Base64 string.
Since we cannot guarantee that the string stored in the SQL entity is a proper protobuf encoding,
that means the decoding can _always_ fail. To make this explicit, I always place the protobufs
entries as optional/nullable SQL columns.


## Proto3 Enums

Always have an "unspecified" first entry with value 0.
That will be the value if the fields is omitted, or new.


## Proto3 Optional

Every single field in a Proto3 is _optional_ and can be missing.
In that case, the field has an implied default value (0, empty string, first enum value 0, etc.)

The `optional` keywords in a proto3 is thus useful _only_ to get a generated "hasField()" method
that can indicate whether the field was missing. In either case, the field can always be read.
The `optional` keywords is useful if we _need_ to differentiate the default value from the
absence of a field. In other words, if the code is never going to use the "hasField()" method,
then the field needs not be marked optional.


~~
