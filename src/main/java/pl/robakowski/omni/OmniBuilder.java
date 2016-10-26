package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.File;
import java.lang.invoke.SerializedLambda;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OmniBuilder<T> {
    private Kryo kryo = new Kryo();
    private File path;
    private Supplier<T> instanceCreator;

    private OmniBuilder(Supplier<T> instanceCreator) {
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        kryo.register(SerializedLambda.class);
        kryo.register(ClosureSerializer.Closure.class, new ClosureSerializer());
        this.instanceCreator = instanceCreator;
    }

    public OmniBuilder<T> configureKryo(Consumer<Kryo> configurator) {
        configurator.accept(kryo);
        return this;
    }

    public OmniBuilder<T> path(String path) {
        this.path = new File(path);
        return this;
    }

    public OmniBuilder<T> path(File path) {
        this.path = path;
        return this;
    }

    public Omni<T> build() {
        if (path.exists() && !path.isDirectory()) {
            throw new IllegalStateException(path.getAbsolutePath() + " is not direcotry");
        }
        if (path.exists() || path.mkdirs()) {
            return new Omni<>(kryo, path, instanceCreator);
        }

        throw new IllegalStateException("Cannot create directory " + path.getAbsolutePath());
    }

    public static <T> OmniBuilder<T> builder(Supplier<T> instanceCreator) {
        return new OmniBuilder<>(instanceCreator);
    }
}
