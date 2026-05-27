package demo;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

import demo.Person;

@ProtoSchema(
    includeClasses = { Person.class },
    schemaPackageName = "demo"
)
public interface DemoSchema extends SerializationContextInitializer {
}