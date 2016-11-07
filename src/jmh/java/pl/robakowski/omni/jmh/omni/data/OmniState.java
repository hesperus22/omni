package pl.robakowski.omni.jmh.omni.data;

import com.esotericsoftware.kryo.Kryo;
import org.apache.commons.io.FileUtils;
import org.openjdk.jmh.annotations.*;
import pl.robakowski.omni.*;

import java.io.*;
import java.util.*;

public abstract class OmniState {
    public Omni<List<Person>> o;

    protected abstract String getPath();

    private void configureKryo(Kryo kryo) {
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

    @Setup(Level.Iteration)
    public void setup() {
        o = getOmni();
    }

    @TearDown(Level.Iteration)
    public void tearDown() throws IOException {
        o.close();
        File dir = new File(getPath());
        FileUtils.deleteDirectory(dir);
    }
}
