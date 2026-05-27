package demo;
import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.Proto;

@Proto
@Indexed
public record Person(
    @Basic String name,
    @Basic int age,
    @Basic String city
) {}