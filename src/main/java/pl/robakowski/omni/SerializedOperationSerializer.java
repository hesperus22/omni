package pl.robakowski.omni;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.Instant;

/**
 * Created by probakowski on 2016-10-20.
 */
public class SerializedOperationSerializer<T, E> extends Serializer<SerializedOperation<T, E>>
{
    @Override
    public void write( Kryo kryo, Output output, SerializedOperation<T, E> object )
    {
        output.writeVarLong( object.getClock().toEpochMilli(), true );
        kryo.writeClassAndObject( output, object.getOperation() );
    }

    @Override
    public SerializedOperation<T, E> read( Kryo kryo, Input input, Class<SerializedOperation<T, E>> type )
    {
        return new SerializedOperation<>( Instant.ofEpochMilli( input.readVarLong( true ) ),
            (Operation<T, E>)kryo.readClassAndObject( input ) );
    }
}
