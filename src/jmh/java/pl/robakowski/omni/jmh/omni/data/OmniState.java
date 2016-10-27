package pl.robakowski.omni.jmh.omni.data;

import com.esotericsoftware.kryo.Kryo;
import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.*;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

public abstract class OmniState {
    public Omni<List<Person>> o;

    protected abstract String getPath();

    protected void configureKryo(Kryo kryo) {
        kryo.register(Update.class);
        kryo.register(AddAndSum.class);
        kryo.register(AddWithPets.class);
    }

    protected Omni<List<Person>> getOmni() {
        return OmniBuilder.builder(this::getPeople).configureKryo(this::configureKryo).path(getPath()).build();
    }

    protected List<Person> getPeople() {
        List<Person> list = new LinkedList<>();
        list.add(new Person());
        return list;
    }

    @Setup
    public void setup() {
        o = getOmni();
    }

    @TearDown
    public void tearDown() {
        File dir = new File(getPath());

        Stream.of(Optional.ofNullable(dir.listFiles()).orElse(new File[0])).forEach(File::delete);
        dir.delete();
    }
}
