import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serializers.ClosureSerializer;

import java.lang.invoke.SerializedLambda;
import java.util.function.Supplier;

public class OmniBuilder<T>
{
    private Kryo kryo = new Kryo();
    private String path;
    private Supplier<T> instanceCreator;

    private OmniBuilder( Supplier<T> instanceCreator )
    {
        kryo.register( SerializedLambda.class, new ClosureSerializer() );
        kryo.register( ClosureSerializer.Closure.class, new ClosureSerializer() );
        this.instanceCreator = instanceCreator;
    }

    public OmniBuilder<T> register( Class<?> clazz )
    {
        kryo.register( clazz );
        return this;
    }

    public <E> OmniBuilder<T> register( Class<E> clazz, Serializer<E> serializer )
    {
        kryo.register( clazz, serializer );
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
