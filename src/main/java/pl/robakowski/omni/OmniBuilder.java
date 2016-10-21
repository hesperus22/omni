package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.lang.invoke.SerializedLambda;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class OmniBuilder<T>
{
    private Kryo kryo = new Kryo();
    private String path;
    private Supplier<T> instanceCreator;

    private OmniBuilder( Supplier<T> instanceCreator )
    {
        kryo.setInstantiatorStrategy( new Kryo.DefaultInstantiatorStrategy( new StdInstantiatorStrategy() ) );
        kryo.register( SerializedLambda.class );
        kryo.register( ClosureSerializer.Closure.class, new ClosureSerializer() );
        this.instanceCreator = instanceCreator;
    }

    public OmniBuilder<T> configureKryo( Consumer<Kryo> configurator )
    {
        configurator.accept( kryo );
        return this;
    }

    public OmniBuilder<T> path( String path )
    {
        this.path = path;
        return this;
    }

    public Omni<T> build()
    {
        return new Omni<>( kryo, path, instanceCreator );
    }

    public static <T> OmniBuilder<T> builder( Supplier<T> instanceCreator )
    {
        return new OmniBuilder<>( instanceCreator );
    }
}
